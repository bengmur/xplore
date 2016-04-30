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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class LocationTracker extends Service implements LocationListener {

    private final static String TAG = "LocationTracker";

    private static final int LOCATION_INTERVAL = 3000; //milliseconds
    private static final float LOCATION_DISTANCE = 20; // meters

    // Stores locations until they are able to be sent to the Activity (i.e. it is not paused)
    private ArrayList<LatLng> locationUpdates = new ArrayList<LatLng>();
    private ArrayList<LatLng> allLocs = new ArrayList<LatLng>();

    private boolean activityPaused = false;
    private boolean activityStopped = false;
    private boolean resendAllLocs = false;
    private BroadcastReceiver activityStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("pauseStatus")) {
                activityPaused = intent.getExtras().getBoolean("pauseStatus");
            }

            if (intent.hasExtra("stopStatus")) {
                activityStopped = intent.getExtras().getBoolean("stopStatus");

                if (activityStopped == false) {
                    resendAllLocs = true;
                }
            }
        }
    };

    LocationManager locManager;

    public LocationTracker() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        subscribeToLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(activityStateReceiver, new IntentFilter("edu.umd.cs.xplore.MAIN_STATUS"));

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle("Xplore")
                .setContentText("Mapping your trip progress...")
                .build();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.contentIntent = pendingIntent;
        startForeground(1, notification);

        return START_STICKY;
    }

    public void onLocationChanged(Location loc) {
        Log.d(TAG, "New loc: " + loc.getLatitude() + ", " + loc.getLongitude());
        locationUpdates.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        allLocs.add(new LatLng(loc.getLatitude(), loc.getLongitude()));

        if (resendAllLocs) {
            Intent locBrdIntent = new Intent("edu.umd.cs.xplore.LOC_UPDATE");
            locBrdIntent.putExtra("locs", allLocs);
            sendBroadcast(locBrdIntent);

            locationUpdates.clear(); // clear updates since they're included in the "allLocs" list
            resendAllLocs = false;
        } else if (!activityPaused && !activityStopped) {
            Intent locBrdIntent = new Intent("edu.umd.cs.xplore.LOC_UPDATE");
            locBrdIntent.putExtra("locs", locationUpdates);
            sendBroadcast(locBrdIntent);

            locationUpdates.clear();
        }
    }
    public void onProviderEnabled(String s){
    }
    public void onProviderDisabled(String s){
    }
    public void onStatusChanged(String s, int i, Bundle b){
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
}