package edu.umd.cs.xplore;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
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
import android.view.WindowManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
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

    // TODO: GoogleMap?
    // TODO: BottomSheetBehavior
    // TODO: Peek Height?
    public static final String SELECTED_PREFS = "edu.umd.cs.xplore.SELECTED_PREFS";
    public static final String MATCHES = "edu.umd.cs.xplore.MATCHES";
    public static final String MATCH_NAMES = "edu.umd.cs.xplore.MATCH_NAMES";
    public static final String MATCH_PREFS = "edu.umd.cs.xplore.MATCH_PREFS";
    public static final String DURATION = "edu.umd.cs.xplore.DURATION";
    public static final String ITINERARY = "edu.umd.cs.xplore.ITINERARY";
    public static final String ITINERARY_ADDRESSES = "edu.umd.cs.xplore.ITINERARY_ADDRESSES";
    public static final String ITINERARY_LATLNGS = "edu.umd.cs.xplore.ITINERARY_LATLNGS";
    public static final String PREF_IDX = "edu.umd.cs.xplore.PREF_IDX";
    public static final String DESTINATION = "edu.umd.cs.xplore.DESTINATION";
    public static final String INIT_LOC = "edu.umd.cs.xplore.INIT_LOC";
    public static final String TRIP_ACTIVE = "edu.umd.cs.xplore.TRIP_ACTIVE";
    public static final String SHARE_TRIP = "edu.umd.cs.xplore.SHARE_TRIP";
    public static final String PENDING_DRAW_MAP = "edu.umd.cs.xplore.PENDING_DRAW_MAP";
    public static final String NAV_STARTED = "edu.umd.cs.xplore.NAV_STARTED";
    public static final String ITINERARY_CURSOR = "edu.umd.cs.xplore.ITINERARY_CURSOR";
    public static final String ITINERARY_BOUNDS = "edu.umd.cs.xplore.ITINERARY_BOUNDS";
    public static final String LAST_LOC = "edu.umd.cs.xplore.LAST_LOC";

    private static final String TAG = "MainActivity";

    private GoogleMap mMap;
    private ArrayList<String> selectedPreferences;
    private HashMap<String, ArrayList<String>> matches;
    private HashMap<String, String> matchNames;
    private HashMap<String, String> matchPreferences;
    private int duration;
    private ArrayList<String> itinerary;
    private ArrayList<String> itineraryAddresses; // populated by drawRoute(), incl. origin appended to end for nav
    private ArrayList<LatLng> itineraryLatLngs; // populated by drawRoute(), incl. origin appended to end for nav
    private int preferenceIdx;
    private String destination;

    private BottomSheetBehavior mBottomSheetBehavior;
    private int peekHeight;
    private RecyclerView recyclerView;
    private ArrayList<LatLng> actualLocations = new ArrayList<LatLng>();
    private ArrayList<LatLng> newLocs;
    private LatLng initLoc;

    private ArrayList<Marker> mapMarkers = new ArrayList<Marker>();
    private ArrayList<Polyline> mapLegs = new ArrayList<Polyline>();

    private boolean tripActive = false;
    private boolean shareTrip = false;
    private LocationTracker locService;
    private boolean pendingDrawMap = false;
    private boolean pendingLocService = false;
    private boolean navStarted = false;

    private int itineraryCursor;
    private int itineraryBounds;

    private ServiceConnection locTrackerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            locService = ((LocationTracker.LocationTrackerBinder) service).getService();

            if (pendingLocService) {
                pendingLocService = false;
                drawRoute();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            locService = null;
        }
    };

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

                if (tripActive) {
                    drawMovingLoc();
                }
            }
        }
    };

    private BroadcastReceiver proximityAlertReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)) {

                Log.i("LocationTracker", "received TRUE KEY_PROXIMITY_ENTERING intent");

                locService.clearProximityAlert();

                // if at end of trip, change fab action to sharing
                if (++itineraryCursor >= itineraryBounds) {
                    tripActive = false;
                    shareTrip = true;

                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);
                    fab.setImageDrawable(getResources().getDrawable(
                            R.drawable.ic_share_white_24dp, getTheme()));
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get fields
        boolean saved = false;
        if (savedInstanceState == null) {
            Log.i(TAG, "No saved instance state to load from");
        } else {
            Log.i(TAG, "Loading from saved instance state");
            saved = true;
            selectedPreferences = savedInstanceState.getStringArrayList(SELECTED_PREFS);
            matches = (HashMap<String, ArrayList<String>>) savedInstanceState.getSerializable(MATCHES);
            matchNames = (HashMap<String, String>) savedInstanceState.getSerializable(MATCH_NAMES);
            matchPreferences = (HashMap<String, String>) savedInstanceState.getSerializable(MATCH_PREFS);
            duration = savedInstanceState.getInt(DURATION);
            itinerary = savedInstanceState.getStringArrayList(ITINERARY);
            itineraryAddresses = savedInstanceState.getStringArrayList(ITINERARY_ADDRESSES);
            itineraryLatLngs = savedInstanceState.getParcelableArrayList(ITINERARY_LATLNGS);
            preferenceIdx = savedInstanceState.getInt(PREF_IDX);
            destination = savedInstanceState.getString(DESTINATION);
            initLoc = savedInstanceState.getParcelable(INIT_LOC);
            tripActive = savedInstanceState.getBoolean(TRIP_ACTIVE);
            shareTrip = savedInstanceState.getBoolean(SHARE_TRIP);
            navStarted = savedInstanceState.getBoolean(NAV_STARTED);
            itineraryCursor = savedInstanceState.getInt(ITINERARY_CURSOR);
            itineraryBounds = savedInstanceState.getInt(ITINERARY_BOUNDS);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tripActive) {
                    locService.addTripProximityAlert(itineraryLatLngs.get(itineraryCursor));
                    navigateToAddress(itineraryAddresses.get(itineraryCursor));
                    navStarted = true;
                    mBottomSheetBehavior.setHideable(true);
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else if (shareTrip) {
                    String toShare = "I just travelled to " + matchNames.get(destination) +
                            " using the Xplore App!";
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, toShare);
                    shareIntent.setType("text/plain");
                    startActivity(Intent.createChooser(shareIntent,
                            getResources().getText(R.string.share_chooser_title)));
                } else {
                    Intent intent = new Intent(MainActivity.this, PlanActivity.class);
                    intent.putExtra(LAST_LOC, locService.getCurrentLocation());
                    startActivity(intent);
                }
            }
        });
        if (tripActive) {
            pendingDrawMap = true;
            fab.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_navigation_white_24dp, getTheme()));
        } else if (shareTrip) {
            fab.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_share_white_24dp, getTheme()));
        }

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
                if (!navStarted) {
                    // Retrieve detected position in list
                    final int position = viewHolder.getAdapterPosition();
                    if (position < 0) {
                        Log.e(TAG, "Cannot remove position " + position + " from list");
                    } else {
                        // Remove place from list
                        final String place = itinerary.remove(position);
                        if (place == null) {
                            Log.e(TAG, "Removed place is null");
                        }
                        recyclerView.getAdapter().notifyItemRemoved(position);

                        // Put new place in list
                        final String newPlace = putNewPlaceInItinerary();
                        if (newPlace == null) {
                            Log.i(TAG, "No more places to add");
                        } else {
                            int newPosition = itinerary.indexOf(newPlace);
                            if (newPosition < 0) {
                                Log.e(TAG, "Position of just added place not found");
                            }
                            recyclerView.getAdapter().notifyItemInserted(newPosition);
                        }
                        drawRoute();

                        // Allow for undo action
                        Snackbar snackbar = Snackbar
                                .make(recyclerView, "PLACE REMOVED", Snackbar.LENGTH_LONG)
                                .setAction("UNDO", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        // Remove new place
                                        if (newPlace != null) {
                                            int newPosition = itinerary.indexOf(newPlace);
                                            if (newPosition < 0) {
                                                Log.e(TAG, "Position of just added place not found");
                                            } else {
                                                itinerary.remove(newPosition);
                                                recyclerView.getAdapter().notifyItemRemoved(newPosition);
                                            }
                                        }

                                        // Add old place
                                        if (place != null) {
                                            itinerary.add(position, place);
                                            recyclerView.getAdapter().notifyItemInserted(position);
                                            recyclerView.scrollToPosition(position);
                                        }

                                        drawRoute();
                                    }
                                });
                        snackbar.show();

                        // Scroll to position
                        recyclerView.scrollToPosition(position);
                    }
                }

                if (itinerary == null || itinerary.isEmpty()) {
                    mBottomSheetBehavior.setHideable(true);
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Check for an Intent from PreferencesActivity
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (!saved && type != null && Intent.ACTION_SEND.equals(action)) {
            if ("list/preferences".equals(type)) {
                Log.i(TAG, "Handling send preferences");
                handleSendPreferences(intent);

                tripActive = true;
                itineraryCursor = 0;
                fab.setImageDrawable(getResources().getDrawable(
                        R.drawable.ic_navigation_white_24dp, getTheme()));
            } else {
                //TODO Handle other intents
            }
        } else {
            //TODO Handle other intents
        }

        if (itinerary != null) {
            // Set PeekHeight
            mBottomSheetBehavior.setPeekHeight(peekHeight);

            // Load RecycleView
            recyclerView.setAdapter(new RecyclerViewStringListAdapter(itinerary, matchNames, matchPreferences));
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        // Register broadcast receiver for location updates
        registerReceiver(locationReceiver, new IntentFilter("edu.umd.cs.xplore.LOC_UPDATE"));

        // Register broadcast receiver for proximity alerts
        registerReceiver(proximityAlertReceiver, new IntentFilter("edu.umd.cs.xplore.PROXIMITY_ALERT"));

        // Start loc tracking service if not yet running
        Intent serviceIntent = new Intent(this, LocationTracker.class);
        if (!isMyServiceRunning(LocationTracker.class)) {
            startService(serviceIntent);
        }
        // Bind to loc tracking service
        bindService(serviceIntent, locTrackerConnection, 0);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.i(TAG, "Saving instance state");
        outState.putStringArrayList(SELECTED_PREFS, selectedPreferences);
        outState.putSerializable(MATCHES, matches);
        outState.putSerializable(MATCH_NAMES, matchNames);
        outState.putSerializable(MATCH_PREFS, matchPreferences);
        outState.putInt(DURATION, duration);
        outState.putStringArrayList(ITINERARY, itinerary);
        outState.putStringArrayList(ITINERARY_ADDRESSES, itineraryAddresses);
        outState.putParcelableArrayList(ITINERARY_LATLNGS, itineraryLatLngs);
        outState.putInt(PREF_IDX, preferenceIdx);
        outState.putString(DESTINATION, destination);
        outState.putParcelable(INIT_LOC, initLoc);
        outState.putBoolean(TRIP_ACTIVE, tripActive);
        outState.putBoolean(SHARE_TRIP, shareTrip);
        outState.putBoolean(PENDING_DRAW_MAP, pendingDrawMap);
        outState.putBoolean(NAV_STARTED, navStarted);
        outState.putInt(ITINERARY_CURSOR, itineraryCursor);
        outState.putInt(ITINERARY_BOUNDS, itineraryBounds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(locTrackerConnection);
        // Unregister broadcast receivers
        unregisterReceiver(locationReceiver);
        unregisterReceiver(proximityAlertReceiver);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        CameraPosition initPos = new CameraPosition.Builder()
                .target(new LatLng(38.904679, -77.036021))
                .zoom(10)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(initPos));

        if (pendingDrawMap) {
            pendingDrawMap = false;
            drawRoute();
        }
    }

    // Clear old routing on any updates to itinerary
    private void clearRoute() {
        for (Marker m : mapMarkers) {
            m.remove();
        }

        for (Polyline l : mapLegs) {
            l.remove();
        }
    }

    private void drawRoute() {
        if (mMap == null) {
            pendingDrawMap = true;
            return;
        }

        if (locService == null) {
            pendingLocService = true;
            return;
        }

        clearRoute();

        ArrayList<String> modItinerary = new ArrayList<String>(itinerary);
        if (initLoc == null) {
            initLoc = locService.getCurrentLocation();
        }
        String initLocStr = initLoc.latitude + ", " + initLoc.longitude;
        modItinerary.add(0, initLocStr);

        String[] modItineraryArray = new String[modItinerary.size()];
        modItinerary.toArray(modItineraryArray);

        itineraryBounds = modItinerary.size(); // TODO: this should not have to be here; clean this up

        // Draw polyline connecting places (up to 23 places allowed by the API for the single request)
        DirectionsAsyncTask routePolylineDrawer = new DirectionsAsyncTask();
        routePolylineDrawer.execute(modItineraryArray);
    }

    private String putNewPlaceInItinerary() {
        // No places to add
        if (selectedPreferences == null || selectedPreferences.isEmpty()) {
            Log.e(TAG, "No places found for any preferences");
            preferenceIdx = 0;
            return null;
        }

        // Get current preference and list of places
        String currPreference = selectedPreferences.get(preferenceIdx);
        ArrayList<String> places = matches.get(currPreference);

        // Handle no places found for preference
        while (places == null || places.isEmpty()) {
            Log.e(TAG, "No places found for preference " + currPreference);
            selectedPreferences.remove(currPreference);
            if (selectedPreferences == null || selectedPreferences.isEmpty()) {
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
        double tripDuration = 0.6 * duration;
        int numPlaces = (int) (tripDuration / 120.0);
        Log.i(TAG, "Num places = " + Integer.toString(numPlaces + 1));
        Log.i(TAG, "Number of preferences = " + selectedPreferences.size());
        for (int i = 0; i < numPlaces + 1; i++) {
            String place = putNewPlaceInItinerary();
            Log.i(TAG, "Found place = " + convertIdToName(place));
        }
        Log.i(TAG, "Itinerary = " + convertIdsToNames(itinerary));

        drawRoute();
    }

    private String convertIdToName(String id) {
        if (matchNames.containsKey(id)) {
            return matchNames.get(id);
        }
        Log.e(TAG, "Could not find place ID: " + id);
        return null;
    }

    private ArrayList<String> convertIdsToNames(ArrayList<String> ids) {
        ArrayList<String> names = new ArrayList<String>(ids.size());
        for (String id : ids) {
            names.add(convertIdToName(id));
        }
        return names;
    }

    // Store HashSet containing preferences sent from PreferencesActivity
    private void handleSendPreferences(Intent intent) {
        // Retrieve data passed through intent
        selectedPreferences = intent.getStringArrayListExtra(PreferencesActivity.SELECTED_PREFERENCES);
        matches = (HashMap<String, ArrayList<String>>) intent.getSerializableExtra(PreferencesActivity.MATCHES);
        matchNames = (HashMap<String, String>) intent.getSerializableExtra(PreferencesActivity.MATCH_NAMES);
        matchPreferences = (HashMap<String, String>) intent.getSerializableExtra(PreferencesActivity.MATCH_PREFERENCES);
        duration = intent.getIntExtra(PlanActivity.DURATION, 400);

        // Make sure preferences were selected
        if (selectedPreferences == null || selectedPreferences.isEmpty()) {
            selectedPreferences = new ArrayList<String>(matches.keySet());
        }

        // Get destination ID
        if (matches == null || !matches.containsKey("destination")) {
            Log.e(TAG, "Destination not passed");
        }
        ArrayList<String> destinations = matches.remove("destination");
        if (destinations == null || destinations.isEmpty()) {
            Log.e(TAG, "Destination not passed");
        } else {
            destination = destinations.get(0);
        }
        Log.i(TAG, "Destination = " + destination);

        // Make sure match names and preferences are stored
        if (matchNames == null) {
            Log.e(TAG, "Match names not passed");
        }
        if (matchPreferences == null) {
            Log.e(TAG, "Match preferences not passed");
        }

        // Fill itinerary
        preferenceIdx = 0;
        itinerary = new ArrayList<String>();
        createItinerary();

        // Log preferences for debugging
        for (String preference : matches.keySet()) {
            Log.i(TAG, preference + " -> " + convertIdsToNames(matches.get(preference)));
        }
    }

    private void drawMovingLoc() {
        Polyline line = mMap.addPolyline(new PolylineOptions()
                .addAll(newLocs)
                .width(20)
                .zIndex(101)
                .color(Color.argb(255, 0, 191, 255)));
    }

    private void navigateToAddress(String address) {
        Uri navIntentURI = Uri.parse("google.navigation:q=" + address);
        Intent navIntent = new Intent(Intent.ACTION_VIEW, navIntentURI);
        navIntent.setPackage("com.google.android.apps.maps");
        startActivity(navIntent);
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
                    urlStringBuilder.append(URLEncoder.encode("place_id:" + params[i] + "|", "UTF-8"));
                }
                urlStringBuilder.append(URLEncoder.encode("place_id:" + params[params.length - 1], "UTF-8"));
                urlStringBuilder.append("&key=AIzaSyAOzEWOLOBTMvslQTtB4zBQOWwe2t88mAI");

                URL reqURL = new URL(urlStringBuilder.toString());

                Log.i("URL", urlStringBuilder.toString());

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

                itineraryAddresses = new ArrayList<>();
                itineraryLatLngs = new ArrayList<>();
                for (int i = 0; i < routeLegs.length(); i++) {
                    JSONObject currLeg = routeLegs.getJSONObject(i);

                    // Add a marker on end location of current leg
                    JSONObject endLocJSON = currLeg.getJSONObject("end_location");
                    LatLng endLocLatLng = new LatLng(endLocJSON.getDouble("lat"), endLocJSON.getDouble("lng"));

                    if (i < routeLegs.length() - 1) {
                        Marker m = mMap.addMarker(new MarkerOptions().position(endLocLatLng).title(currLeg.getString("end_address")));
                        mapMarkers.add(m);
                    }

                    itineraryAddresses.add(currLeg.getString("end_address"));
                    itineraryLatLngs.add(endLocLatLng);

                    // Draw PolyLine for each step in the leg
                    // TODO: Store PolyLines so color can be modified as user progresses along journey
                    JSONArray currLegSteps = currLeg.getJSONArray("steps");
                    for (int j = 0; j < currLegSteps.length(); j++) {
                        String encodedPolyline = currLegSteps.getJSONObject(j).getJSONObject("polyline").getString("points");
                        List<LatLng> pointsList = PolyUtil.decode(encodedPolyline);

                        Polyline line = mMap.addPolyline(new PolylineOptions()
                                .addAll(pointsList)
                                .width(20)
                                .zIndex(100)
                                .color(Color.argb(255, 170, 170, 170)));

                        mapLegs.add(line);
                    }
                }

                // center camera on entire route bounds (position & zoom)
                JSONObject routeBounds = directionsResult.getJSONArray("routes").getJSONObject(0).getJSONObject("bounds");
                LatLng southwestBound = new LatLng(routeBounds.getJSONObject("southwest").getDouble("lat"),
                        routeBounds.getJSONObject("southwest").getDouble("lng"));
                LatLng northeastBound = new LatLng(routeBounds.getJSONObject("northeast").getDouble("lat"),
                        routeBounds.getJSONObject("northeast").getDouble("lng"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southwestBound, northeastBound), 400));
            } catch (Exception e) {
                // TODO: handle errors
            }
        }
    }
}
