package edu.umd.cs.xplore;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

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
import java.util.HashSet;

import javax.net.ssl.HttpsURLConnection;

/**
 * Activity for selecting preferences for the user's trip
 */
public class PreferencesActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, GoogleApiClient.OnConnectionFailedListener {

    public static final String SELECTED_PREFERENCES = "edu.umd.cs.xplore.SELECTED_PREFERENCES";
    public static final String MATCHES = "edu.umd.cs.xplore.MATCHES";
    public static final String MATCH_NAMES = "edu.umd.cs.xplore.MATCH_NAMES";
    public static final String MATCH_PREFERENCES = "edu.umd.cs.xplore.MATCH_PREFERENCES";

    private static final String PREFERENCE_TITLE = "What are your interests?";
    private static final String TAG = "PreferencesActivity";

    private PreferencesAdapter prefAdapter;
    private String curDestination;
    private ArrayList<String> destinationList;
    private PreferenceList prefList = PreferenceList.getInstance();
    private HashSet<String> selectedPreferences = new HashSet<String>();
    private int duration;
    private GoogleApiClient mGoogleApiClient; // Connect to Google Places API
    private ProgressDialog findPlacesProgressDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences_layout);

        // Check for an Intent from PlanActivity
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if ("possibleDestinations".equals(type)) {
                handleSendDestinations(intent);
            } else {
                // TODO Handle other intents
            }
        } else {
            // TODO Handle other intents
        }

        // set up the toolbar
        Toolbar prefToolbar = (Toolbar) findViewById(R.id.preferences_toolbar);
        setSupportActionBar(prefToolbar);

        // set up the Spinner (dropdown of destinations)
        Spinner destSpinner = (Spinner) findViewById(R.id.destination_spinner);
        if (destSpinner == null) {
            Log.e(TAG, "Destination spinner is null");
        } else {
            destSpinner.setOnItemSelectedListener(this);
            ArrayAdapter<String> destAdapter =
                    new ArrayAdapter<String>(this, R.layout.spinner_item, destinationList);
            destAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            destSpinner.setAdapter(destAdapter);
        }

        // set up the FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.send_preferences_fab);
        if (fab == null) {
            Log.e(TAG, "FAB is null");
        } else {
            fab.setOnClickListener(new View.OnClickListener() {

                public void onClick(View view) {
                    createAndPassItinerary(curDestination);
                }
            });
        }

        // set up the preferences question title
        TextView prefTitleView = (TextView) findViewById(R.id.preferences_title);
        if (prefTitleView == null) {
            Log.e(TAG, "Preference title view is null");
        } else {
            prefTitleView.setText(PREFERENCE_TITLE);
        }

        // set up the grid list of preferences
        prefAdapter = new PreferencesAdapter(this.getApplicationContext());
        GridView prefGridView = (GridView) findViewById(R.id.preferences_grid);
        if (prefGridView == null) {
            Log.e(TAG, "Preference grid view is null");
        } else {
            prefGridView.setAdapter(prefAdapter);

            // when a preference item is selected, it should be highlighted and should
            // be added to list of preferences to be passed to the next Activity
            prefGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String curPreferenceTag = prefList.getPreferenceTag(position);
                    if (selectedPreferences.contains(curPreferenceTag)) {
                        selectedPreferences.remove(curPreferenceTag);
                        view.setBackgroundColor(Color.WHITE);
                    } else {
                        selectedPreferences.add(curPreferenceTag);
                        view.setBackgroundResource(R.color.colorAccent);
                    }
                }
            });
        }

        // Connected to Google Places API
        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .addApi(AppIndex.API).build();

        // Set up progress dialog
        findPlacesProgressDialog = new ProgressDialog(this);
        findPlacesProgressDialog.setTitle("Finding Locations");
        findPlacesProgressDialog.setMessage("Finding destinations for your preferences");
        findPlacesProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    public void convertPlaceIdToPlace(final String placeId, final String placeName) {
        // Get place
        Log.i(TAG, "Getting Place from Place ID...");
        PendingResult<PlaceBuffer> placeResults = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
        placeResults.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                Place destination = places.get(0);
                Log.i(TAG, String.format("Place = %s", destination.getName().toString()));
                findNearby(placeId, placeName, destination.getLatLng());
            }
        });
    }

    public void convertStringToPlaceId(final String placeName) {
        // Get place ID
        Log.i(TAG, "Converting curr destination string to Place ID...");
        PendingResult<AutocompletePredictionBuffer> autocompleteResults
                = Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, placeName, null, null);
        autocompleteResults.setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
            @Override
            public void onResult(@NonNull AutocompletePredictionBuffer autocompletePredictions) {
                AutocompletePrediction prediction = autocompletePredictions.get(0);
                String placeId = prediction.getPlaceId();
                Log.i(TAG, String.format("Place ID = %s", placeId));
                convertPlaceIdToPlace(placeId, placeName);
            }
        });
    }

    public void findNearby(String placeId, String placeName, LatLng latLng) {
        String position = Double.toString(latLng.latitude) + "," + Double.toString(latLng.longitude);
        Log.i(TAG, "LatLng of dest = " + position);
        SearchNearbyAsyncTask searchTask = new SearchNearbyAsyncTask(placeId, placeName, selectedPreferences);
        searchTask.execute(position);
    }

    public void createAndPassItinerary(String destination) {
        // Find destination coordinates in order to find nearby places
        Log.i(TAG, "Finding destination coordinates...");

        // Use Google Places API
        convertStringToPlaceId(destination);
    }

    private void sendIntent(ArrayList<String> preferences,
                            HashMap<String, ArrayList<String>> matches,
                            HashMap<String, String> matchNames,
                            HashMap<String, String> matchPreferences) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(SELECTED_PREFERENCES, preferences);
        intent.putExtra(MATCHES, matches);
        intent.putExtra(MATCH_NAMES, matchNames);
        intent.putExtra(MATCH_PREFERENCES, matchPreferences);
        intent.putExtra(PlanActivity.DURATION, duration); // Pass along duration
        intent.setType("list/preferences");
        startActivity(intent);
    }

//    private void detectCurrentPlace() {
//        Log.i(TAG, "Detecting current place...");
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        PendingResult<PlaceLikelihoodBuffer> currResults
//                = Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, new PlaceFilter());
//        currResults.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
//            @Override
//            public void onResult(@NonNull PlaceLikelihoodBuffer placeLikelihoods) {
//                PlaceLikelihood currLikelihood = placeLikelihoods.get(0);
//                Place currPlace = currLikelihood.getPlace();
//                Log.i(TAG, String.format("Place = %s", currPlace.getName().toString()));
//                findTravelLength(currPlace);
//                placeLikelihoods.release();
//            }
//        });
//    }

//    private void findTravelLength(Place currPlace) {
//        Log.i(TAG, "Computing roundtrip travel time to destination...");
//        DirectionsAsyncTask directions = new DirectionsAsyncTask();
//        directions.execute(currPlace.getName().toString(), curDestination);
//    }

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
            Log.e(TAG, "Error reading stream", e);
            return "";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO determine if you need a menu for this activity
        return true;
    }


    // MENU METHODS

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO determine if you need a menu for this activity
        return true;
    }

    // Save the selected destination from the dropdown as the current
    // destination
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        curDestination = (String) parent.getItemAtPosition(pos);
    }

    // SPINNER METHODS

    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    // Save the list of destinations sent from the PlanActivity and
    // set the current destination to be the first destination in the
    // list
    private void handleSendDestinations(Intent intent) {
        destinationList = intent.getStringArrayListExtra(Intent.EXTRA_STREAM);
        curDestination = destinationList.get(0);
        duration = intent.getIntExtra(PlanActivity.DURATION, 400);
    }


    // HELPER METHODS

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // TODO: Please implement GoogleApiClient.OnConnectionFailedListener to handle connection failures.
        Log.e(TAG, "Connection failure not handled");
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Preferences Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://edu.umd.cs.xplore/http/host/path")
        );
        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Preferences Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://edu.umd.cs.xplore/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
        mGoogleApiClient.disconnect();
    }

    private class SearchNearbyAsyncTask extends AsyncTask<String, Void, String[]> {

        // Match names and IDs and list of preferences
        private ArrayList<String> preferences;
        private HashMap<String, ArrayList<String>> matches;
        private HashMap<String, String> matchNames;
        private HashMap<String, String> matchPreferences;

        public SearchNearbyAsyncTask(String placeId, String placeName, HashSet<String> selectedPreferences) {
            // Initialize preferences
            if (!selectedPreferences.isEmpty()) {
                preferences = new ArrayList<String>(selectedPreferences);
            } else {
                preferences = new ArrayList<String>(PreferenceList.getInstance().getPreferenceTags());
            }

            // Initialize fields
            matches = new HashMap<String, ArrayList<String>>();
            matchNames = new HashMap<String, String>();
            matchPreferences = new HashMap<String, String>();

            // Add place ID to matches
            ArrayList<String> destinationList = new ArrayList<String>();
            destinationList.add(placeId);
            matches.put("destination", destinationList);
            matchNames.put(placeId, placeName);
        }

        @Override
        protected void onPreExecute() {
            findPlacesProgressDialog.show();
        }

        @Override
        protected String[] doInBackground(String... params) {
            try {
                ArrayList<InputStream> inputStreams = new ArrayList<InputStream>();
                ArrayList<HttpsURLConnection> urlConnections = new ArrayList<HttpsURLConnection>();
                for (String preference : preferences) {
                    StringBuilder urlStringBuilder = new StringBuilder();
                    urlStringBuilder.append("https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=");
                    urlStringBuilder.append(getString(R.string.google_service_key));
                    urlStringBuilder.append("&location=");
                    urlStringBuilder.append(URLEncoder.encode(params[0], "UTF-8"));
//                    urlStringBuilder.append("&rankby=distance");
                    urlStringBuilder.append("&radius=10000");
                    urlStringBuilder.append("&keyword=");
                    urlStringBuilder.append(URLEncoder.encode(PreferenceList.getInstance().getTitleFromTag(preference), "UTF-8"));

                    URL reqURL = new URL(urlStringBuilder.toString());
                    HttpsURLConnection urlConnection = (HttpsURLConnection) reqURL.openConnection();
                    urlConnections.add(urlConnection);
                    inputStreams.add(new BufferedInputStream(urlConnection.getInputStream()));
                }

                String[] queryResponses = new String[inputStreams.size()];

                // Get results from each URL connection
                for (int i = 0; i < inputStreams.size(); i++) {
                    queryResponses[i] = readStream(inputStreams.get(i));
                    // Disconnect each url as well
                    urlConnections.get(i).disconnect(); // TODO: put this in a "finally" block
                }

                return queryResponses;
            } catch (Exception e) {
                // TODO: handle errors; particularly an error resulting from no internet access
                Log.e(TAG, "Exception during API calls.", e);
                return new String[]{""};
            }
        }

        @Override
        protected void onPostExecute(String[] results) {
            try {
                for (int i = 0; i < preferences.size(); i++) {
                    // parse request result into JSON object
                    JSONObject directionsResult = new JSONObject(results[i]);

                    // Construct list of place names for preference
                    ArrayList<String> placeIds = new ArrayList<String>();

                    // using first route by default
                    JSONArray places = directionsResult.getJSONArray("results");
                    for (int j = 0; j < places.length(); j++) {
                        JSONObject place = places.getJSONObject(j);
                        String placeId = place.getString("place_id");
                        String placeName = place.getString("name");
                        placeIds.add(placeId);
                        matchNames.put(placeId, placeName);
                        matchPreferences.put(placeId, preferences.get(i));
                    }
                    matches.put(preferences.get(i), placeIds);
                }
            } catch (Exception e) {
                // TODO: handle errors
                Log.e(TAG, "Exception parsing responses.", e);
            }

            findPlacesProgressDialog.hide();
            sendIntent(preferences, matches, matchNames, matchPreferences);
        }
    }

}
