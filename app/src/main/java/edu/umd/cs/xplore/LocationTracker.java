package edu.umd.cs.xplore;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class LocationTracker extends Service implements LocationListener {

    private final static String TAG = "LocationTracker";

    private static final int LOCATION_INTERVAL = 10000; //milliseconds
    private static final float LOCATION_DISTANCE = 25; // meters

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
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle("Xplore (Location Listener)")
                .setContentText("Tracking your current location...")
                .build();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.contentIntent = pendingIntent;
        startForeground(1, notification);

        return START_STICKY;
    }

    public void onLocationChanged(Location loc) {
        Log.d(TAG, "New loc: " + loc.getLatitude() + ", " + loc.getLongitude());

        Intent locBrdIntent = new Intent("edu.umd.cs.xplore.LOC_UPDATE");
        locBrdIntent.putExtra("lat", loc.getLatitude());
        locBrdIntent.putExtra("lng", loc.getLongitude());
        sendBroadcast(locBrdIntent);
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