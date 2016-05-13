package edu.umd.cs.xplore;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends FragmentActivity implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";

    private GoogleMap mMap;
    private ArrayList<String> selectedPreferences;
    private HashMap<String, ArrayList<String>> matches;
    private int duration;
    private ArrayList<String> itinerary;
    private int preferenceIdx;
    private String destination;

    private BottomSheetBehavior mBottomSheetBehavior;
    private int peekHeight;
    private RecyclerView recyclerView;
    private ArrayList<LatLng> actualLocations = new ArrayList<LatLng>();
    private ArrayList<LatLng> newLocs;

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                //TODO: This can probably be optimized
                newLocs = new ArrayList<LatLng>();

                if (actualLocations.size() > 0) {
                    newLocs.add(actualLocations.get(actualLocations.size() - 1));
                }
                newLocs.addAll((ArrayList<LatLng>) bundle.get("locs"));
                actualLocations.addAll((ArrayList<LatLng>) bundle.get("locs"));

                drawMovingLoc();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PlanActivity.class);
                // TODO: change this from hardcoded to actual last location
                intent.putExtra("lastLoc", new LatLng(38.988205, -76.943566));
                startActivity(intent);
            }
        });

        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setHideable(false);
        peekHeight = mBottomSheetBehavior.getPeekHeight();
        mBottomSheetBehavior.setPeekHeight(0);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                final int position = viewHolder.getAdapterPosition();
                final String place = itinerary.get(position);
                final String newPlace = putNewPlaceInItinerary();
                int newPlacePos = itinerary.indexOf(newPlace);
                recyclerView.getAdapter().notifyItemInserted(newPlacePos);
                Snackbar snackbar = Snackbar
                        .make(recyclerView, "PLACE REMOVED", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (position > -1) {
                                    itinerary.add(position, place);
                                    recyclerView.getAdapter().notifyItemInserted(position);
                                    int newPos = itinerary.indexOf(newPlace);
                                    itinerary.remove(newPos);
                                    recyclerView.getAdapter().notifyItemRemoved(newPos);
                                    recyclerView.scrollToPosition(newPos);
                                }
                            }
                        });
                snackbar.show();
                itinerary.remove(position);
                recyclerView.getAdapter().notifyItemRemoved(position);
                recyclerView.scrollToPosition(position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Check for an Intent from PreferencesActivity
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("list/preferences".equals(type)) {
                handleSendPreferences(intent);
            } else {
                //TODO Handle other intents
            }
        } else {
            //TODO Handle other intents
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Send broadcast that activity is starting and needs ALL locations from a previously started
        // service (if Activity was started earlier)
        Intent activityStatusIntent = new Intent("edu.umd.cs.xplore.MAIN_STATUS");
        activityStatusIntent.putExtra("stopStatus", false);
        sendBroadcast(activityStatusIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register broadcast receiver for location updates
        registerReceiver(locationReceiver, new IntentFilter("edu.umd.cs.xplore.LOC_UPDATE"));

        // Send broadcast that activity is ready to receive location updates
        Intent activityStatusIntent = new Intent("edu.umd.cs.xplore.MAIN_STATUS");
        activityStatusIntent.putExtra("pauseStatus", false);
        sendBroadcast(activityStatusIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Send broadcast that activity is not able to receive location updates
        Intent activityStatusIntent = new Intent("edu.umd.cs.xplore.MAIN_STATUS");
        activityStatusIntent.putExtra("pauseStatus", true);
        sendBroadcast(activityStatusIntent);

        // Unregister broadcast receiver for location updates (in case Activity is stopped)
        unregisterReceiver(locationReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Send broadcast that activity is stopping and will need ALL locations re-sent when started
        Intent activityStatusIntent = new Intent("edu.umd.cs.xplore.MAIN_STATUS");
        activityStatusIntent.putExtra("stopStatus", true);
        sendBroadcast(activityStatusIntent);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable showing user's location on map (blue dot/center on current location control)
        // Use Android Device Monitor (Tools>Android>Android Device Monitor), "emulator control" tab, then manually send
        // coordinates of current location to the device under "Location Controls".
        enableMyLocation();

        // Register broadcast receiver
        registerReceiver(locationReceiver, new IntentFilter("edu.umd.cs.xplore.LOC_UPDATE"));

        // Start loc tracking service if not yet running
        if (!isMyServiceRunning(LocationTracker.class)) {
            Intent serviceIntent = new Intent(this, LocationTracker.class);
            startService(serviceIntent);
        }

        // Sample addresses for testing prior to integration with actual initial places
        // These can be addresses, precise location names, etc. (anything that Google Maps can find the *correct* coordinates for)
        // TODO: use current location for start/end points
        String[] addrSamples = {"12060 Cherry Hill Rd, Silver Spring, MD 20904",
                "10135 Colesville Rd, Silver Spring, MD 20901",
                "10161 New Hampshire Ave, Silver Spring, MD 20903",
                "907 Ellsworth Dr, Silver Spring, MD 20910",
                "5506 Cherrywood Ln, Greenbelt, MD 20770"};

        // Draw polyline connecting places (up to 23 places allowed by the API for the single request)
        DirectionsAsyncTask routePolylineDrawer = new DirectionsAsyncTask();
        routePolylineDrawer.execute(addrSamples);

    }

    private String putNewPlaceInItinerary(){
        // No places to add
        if (selectedPreferences.isEmpty()) {
            Log.e(TAG, "No places found for any preferences");
            preferenceIdx = 0;
            return null;
        }

        // Get current preference and list of places
        String currPreference = selectedPreferences.get(preferenceIdx);
        ArrayList<String> places = matches.get(currPreference);

        // Handle no places found for preference
        while (places.isEmpty()) {
            Log.e(TAG, "No places found for preference " + currPreference);
            selectedPreferences.remove(currPreference);
            if (selectedPreferences.isEmpty()) {
                Log.e(TAG, "No places found for any preferences");
                preferenceIdx = 0;
                return null;
            }
            preferenceIdx = preferenceIdx % selectedPreferences.size();
            currPreference = selectedPreferences.get(preferenceIdx);
            places = matches.get(currPreference);
        }

        // Remove place without replacement
        String place = places.remove(0);

        // Iterate to next preference and add to itinerary
        preferenceIdx = (preferenceIdx + 1) % selectedPreferences.size();
        if (itinerary.contains(place)) {
            Log.e(TAG, place + " already in itinerary");
            return null; // Nothing to add?
        } else {
            itinerary.add(place);
        }
        return place;
    }

    private void createItinerary() {
        double tripDuration = Math.ceil(0.6 * duration);
        int numPlaces = (int) Math.ceil(tripDuration / 120.0);
        Log.i(TAG, "Num places = " + numPlaces);
        Log.i(TAG, "Number of preferences = " + selectedPreferences.size());
        for (int i = 0; i < numPlaces; i++) {
            String place = putNewPlaceInItinerary();
            Log.i(TAG, "Found place = " + place);
        }
        Log.i(TAG, "Itinerary = " + itinerary.toString());
    }

    // Store HashSet containing preferences sent from PreferencesActivity
    private void handleSendPreferences(Intent intent) {
        // Retrieve data passed through intent
        selectedPreferences = intent.getStringArrayListExtra(PreferencesActivity.SELECTED_PREFERENCES);
        matches = (HashMap<String, ArrayList<String>>) intent.getSerializableExtra(PreferencesActivity.ITINERARY);
        duration = intent.getIntExtra(PlanActivity.DURATION, 400);

        // Get destination
        if (matches == null || !matches.containsKey("destination")) {
            Log.e(TAG, "Destination not passed");
            throw new IllegalArgumentException("Destination not passed"); // TODO: Handle differently?
        }
        ArrayList<String> destinations = matches.remove("destination");
        if (destinations.isEmpty()) {
            Log.e(TAG, "Destination not passed");
            throw new IllegalArgumentException("Destination not passed"); // TODO: Handle differently?
        }
        destination = destinations.get(0);
        Log.i(TAG, "Destination = " + destination);

        // Make sure preferences were selected
        if (selectedPreferences == null || selectedPreferences.isEmpty()) {
            selectedPreferences = new ArrayList<String>(matches.keySet());
        }

        // Save matches
        HashMap<String, ArrayList<String>> initialMatches = new HashMap<String, ArrayList<String>>();
        for(String key:matches.keySet()){
            ArrayList<String> tPlaces = new ArrayList<String>();
            for(String tPlace:matches.get(key)){
                tPlaces.add(tPlace);
            }
            initialMatches.put(key, tPlaces);
        }

        // Fill itinerary
        preferenceIdx = 0;
        itinerary = new ArrayList<String>();
        createItinerary();

        // Log preferences for debugging
        for (String preference : matches.keySet()) {
            Log.i(TAG, preference + " -> " + matches.get(preference));
        }

        // Set PeekHeight
        mBottomSheetBehavior.setPeekHeight(peekHeight);

        // Load RecycleView
        recyclerView.setAdapter(new RecyclerViewStringListAdapter(itinerary, initialMatches));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private class DirectionsAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                StringBuilder urlStringBuilder = new StringBuilder();
                urlStringBuilder.append("https://maps.googleapis.com/maps/api/directions/json?origin=");
                urlStringBuilder.append(URLEncoder.encode(params[0], "UTF-8"));
                urlStringBuilder.append("&destination=");
                urlStringBuilder.append(URLEncoder.encode(params[0], "UTF-8"));
                urlStringBuilder.append("&waypoints=");

                // ignore initial origin, which is also final destination, so start iterating at i = 1
                // ignore final waypoint to omit final separating "|" character, so end at prior to last element
                for (int i = 1; i < params.length - 1; i++) {
                    urlStringBuilder.append(URLEncoder.encode(params[i] + "|", "UTF-8"));
                }
                urlStringBuilder.append(URLEncoder.encode(params[params.length - 1], "UTF-8"));

                URL reqURL = new URL(urlStringBuilder.toString());
                HttpsURLConnection urlConnection = (HttpsURLConnection) reqURL.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String queryResponse = readStream(in);
                urlConnection.disconnect(); // TODO: put this in a "finally" block

                return queryResponse;
            } catch (Exception e) {
                // TODO: handle errors; particularly an error resulting from no internet access
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                // parse request result into JSON object
                JSONObject directionsResult = new JSONObject(result);

                // using first route by default, since Google's navigation intents don't allow specifying waypoints
                JSONArray routeLegs = directionsResult.getJSONArray("routes").getJSONObject(0).getJSONArray("legs");

                for (int i = 0; i < routeLegs.length(); i++) {
                    JSONObject currLeg = routeLegs.getJSONObject(i);

                    // Add a marker on start location of current leg
                    JSONObject startLocJSON = currLeg.getJSONObject("start_location");
                    LatLng startLocLatLng = new LatLng(startLocJSON.getDouble("lat"), startLocJSON.getDouble("lng"));
                    //TODO: Change marker name to actual location name, add icon property too maybe?
                    mMap.addMarker(new MarkerOptions().position(startLocLatLng).title("Chipotle").snippet(currLeg.getString("start_address")));

                    // Draw PolyLine for each step in the leg
                    // TODO: Store PolyLines so color can be modified as user progresses along journey
                    JSONArray currLegSteps = currLeg.getJSONArray("steps");
                    for (int j = 0; j < currLegSteps.length(); j++) {
                        String encodedPolyline = currLegSteps.getJSONObject(j).getJSONObject("polyline").getString("points");
                        List<LatLng> pointsList = PolyUtil.decode(encodedPolyline);

                        Polyline line = mMap.addPolyline(new PolylineOptions()
                                .addAll(pointsList)
                                .width(20)
                                .color(Color.CYAN));
                    }
                }

                // center camera on entire route bounds (position & zoom)
                JSONObject routeBounds = directionsResult.getJSONArray("routes").getJSONObject(0).getJSONObject("bounds");
                LatLng southwestBound = new LatLng(routeBounds.getJSONObject("southwest").getDouble("lat"),
                        routeBounds.getJSONObject("southwest").getDouble("lng"));
                LatLng northeastBound = new LatLng(routeBounds.getJSONObject("northeast").getDouble("lat"),
                        routeBounds.getJSONObject("northeast").getDouble("lng"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southwestBound, northeastBound), 50));
            } catch (Exception e) {
                // TODO: handle errors
            }
        }
    }

    private void drawMovingLoc() {
        Polyline line = mMap.addPolyline(new PolylineOptions()
                .addAll(newLocs)
                .width(20)
                .color(Color.RED));
    }

    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while (i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: { // ACCESS_FINE_LOCATION case
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    enableMyLocation();
                } else {
                    // permission denied: quit app (location is vital to usage..)
                    // TODO: handle this more cleanly instead of just exiting out
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    // Source: http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
