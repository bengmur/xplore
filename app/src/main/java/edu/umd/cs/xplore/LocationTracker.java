package edu.umd.cs.xplore;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class LocationTracker extends Service implements LocationListener {

    private final static String TAG = "LocationTracker";

    private static final int LOCATION_INTERVAL = 3000; //milliseconds
    private static final float LOCATION_DISTANCE = 20; // meters
    private static final int PROXIMITY_ALERT_RADIUS = 150; // meters

    IBinder locBinder = new LocationTrackerBinder();
    boolean allowRebind = true;

    PendingIntent currProximityAlertPendingIntent = null;

    public class LocationTrackerBinder extends Binder {
        LocationTracker getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationTracker.this;
        }
    }

    // Stores locations until they are able to be sent to the Activity (i.e. it is not paused)
    private ArrayList<LatLng> locationUpdates = new ArrayList<LatLng>();
    private ArrayList<LatLng> allLocs = new ArrayList<LatLng>();

    private boolean activityDestroyed = true;
    private boolean resendAllLocs = false;
    private boolean resendProximityAlert = false;

    private BroadcastReceiver endTripReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(endTripReceiver);
            unregisterReceiver(proximityAlertReceiver);

            Intent openHome = new Intent(Intent.ACTION_MAIN);
            openHome.addCategory(Intent.CATEGORY_HOME);
            openHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(openHome);

            stopForeground(true);
            stopSelf();
        }
    };

    private BroadcastReceiver proximityAlertReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)) {
                Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
                mainActivityIntent.setAction(Intent.ACTION_MAIN);
                mainActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mainActivityIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                startActivity(mainActivityIntent);

                if (activityDestroyed) {
                    resendProximityAlert = true;
                }
            }
        }
    };

    LocationManager locManager;

    public LocationTracker() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        activityDestroyed = false;
        Log.i(TAG, "Location updates active..");

        if (resendProximityAlert) {
            resendProximityAlert = false;
            Intent proximityAlertIntent = new Intent("edu.umd.cs.xplore.PROXIMITY_ALERT");
            proximityAlertIntent.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
            sendBroadcast(proximityAlertIntent);
        }

        return locBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        activityDestroyed = true;
        resendAllLocs = true;
        Log.i(TAG, "Location updates inactive..");
        return allowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        activityDestroyed = false;
        Log.i(TAG, "Location updates active..");

        if (resendProximityAlert) {
            resendProximityAlert = false;
            Intent proximityAlertIntent = new Intent("edu.umd.cs.xplore.PROXIMITY_ALERT");
            proximityAlertIntent.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
            sendBroadcast(proximityAlertIntent);
        }
    }

    @Override
    public void onCreate() {
        subscribeToLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(endTripReceiver, new IntentFilter("edu.umd.cs.xplore.END_TRIP"));
        registerReceiver(proximityAlertReceiver, new IntentFilter("edu.umd.cs.xplore.PROXIMITY_ALERT"));

        Intent endTripIntent = new Intent("edu.umd.cs.xplore.END_TRIP");
        PendingIntent endTripPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, endTripIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle("Xplore")
                .setContentText("Mapping your trip progress...")
                .addAction(new Notification.Action.Builder(R.drawable.ic_close_black_24dp, "End trip", endTripPendingIntent).build())
                .build();

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.contentIntent = pendingIntent;
        startForeground(1, notification);

        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    public void onLocationChanged(Location loc) {
        Log.d(TAG, "New loc: " + loc.getLatitude() + ", " + loc.getLongitude());
        locationUpdates.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        allLocs.add(new LatLng(loc.getLatitude(), loc.getLongitude()));

        if (!activityDestroyed) {
            if (resendAllLocs) {
                Log.d(TAG, "Resending all locations..");
                Intent locBrdIntent = new Intent("edu.umd.cs.xplore.LOC_UPDATE");
                locBrdIntent.putExtra("locs", allLocs);
                sendBroadcast(locBrdIntent);

                locationUpdates.clear(); // clear updates since they're included in the "allLocs" list
                resendAllLocs = false;
            } else {
                Log.d(TAG, "Sending location updates..");
                Intent locBrdIntent = new Intent("edu.umd.cs.xplore.LOC_UPDATE");
                locBrdIntent.putExtra("locs", locationUpdates);
                sendBroadcast(locBrdIntent);

                locationUpdates.clear();
            }
        }
    }
    public void onProviderEnabled(String s){
    }
    public void onProviderDisabled(String s){
    }
    public void onStatusChanged(String s, int i, Bundle b){
    }

    public LatLng getCurrentLocation() {
        try {
            Location lastLoc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastLoc == null) {
                lastLoc = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastLoc == null) {
                lastLoc = locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            return new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());
        } catch (SecurityException ex) {
            // TODO: req permission
        }
        if (allLocs.size() > 1) {
            return allLocs.get(allLocs.size() - 1);
        } else {
            return null;
        }
    }

    public void subscribeToLocationUpdates() {
        this.locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        try {
            this.locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL, LOCATION_DISTANCE, this);
        } catch (SecurityException ex) {
            // TODO: req permission
        }
    }

    public void clearProximityAlert() {
        if (currProximityAlertPendingIntent != null) {
            try {
                locManager.removeProximityAlert(currProximityAlertPendingIntent);
                currProximityAlertPendingIntent = null;
            } catch (SecurityException e) {
            }
        }
    }

    public void addTripProximityAlert(LatLng loc) {
        try {
            Intent proximityAlertIntent = new Intent("edu.umd.cs.xplore.PROXIMITY_ALERT");
            currProximityAlertPendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(), 0, proximityAlertIntent, 0);
            locManager.addProximityAlert(loc.latitude, loc.longitude, PROXIMITY_ALERT_RADIUS, -1,
                    currProximityAlertPendingIntent);
        } catch (SecurityException e) {
        }
    }

}