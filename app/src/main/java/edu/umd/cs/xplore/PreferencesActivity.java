package edu.umd.cs.xplore;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;

import javax.net.ssl.HttpsURLConnection;

/**
 * Activity for selecting preferences for the user's trip
 */
public class PreferencesActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, GoogleApiClient.OnConnectionFailedListener {

    static final String SELECTED_PREFERENCES = "edu.umd.cs.xplore.SELECTED_PREFERENCES";
    static final String PREFERENCE_TITLE = "What are your interests?";
    private static final String TAG = "PreferencesActivity";
    private PreferencesAdapter prefAdapter;
    private String curDestination;
    private ArrayList<String> destinationList;
    private PreferenceList prefList = PreferenceList.getInstance();
    private HashSet<String> selectedPreferences = new HashSet<String>();
    private int duration;
    private int travelTime;
    private GoogleApiClient mGoogleApiClient; // Connect to Google Places API

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
                //TODO Handle other intents
            }
        } else {
            //TODO Handle other intents
        }

        // set up the toolbar
        Toolbar prefToolbar = (Toolbar) findViewById(R.id.preferences_toolbar);
        prefToolbar.setTitle(curDestination);
        setSupportActionBar(prefToolbar);

        // set up the Spinner (dropdown of destinations)
        Spinner destSpinner = (Spinner) findViewById(R.id.destination_spinner);
        destSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> destAdapter =
                new ArrayAdapter<String>(this, R.layout.spinner_item, destinationList);
        destAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        destSpinner.setAdapter(destAdapter);

        // set up the FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.send_preferences_fab);
        fab.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                createItinerary();
//                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                intent.setAction(Intent.ACTION_SEND);
//                intent.putExtra(SELECTED_PREFERENCES, selectedPreferences);
//                intent.setType("list/preferences");
//                startActivity(intent);
            }
        });

        // set up the preferences question title
        TextView prefTitleView = (TextView) findViewById(R.id.preferences_title);
        prefTitleView.setText(PREFERENCE_TITLE);

        // set up the grid list of preferences
        prefAdapter = new PreferencesAdapter(this.getApplicationContext());
        GridView prefGridView = (GridView) findViewById(R.id.preferences_grid);
        prefGridView.setAdapter(prefAdapter);

        // when a preference item is selected, it should be highlighted and should
        // be added to list of preferences to be passed to the next Activity
        prefGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            private String curPreferenceTag;

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                curPreferenceTag = prefList.getPreferenceTag(position);
                if (selectedPreferences.contains(curPreferenceTag)) {
                    selectedPreferences.remove(curPreferenceTag);
                    view.setBackgroundColor(Color.WHITE);
                } else {
                    selectedPreferences.add(curPreferenceTag);
                    view.setBackgroundColor(Color.parseColor("#2196F3"));
                }
            }
        });

        // Connected to Google Places API
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

    }

    public void createItinerary() {
//        // Get place ID
//        Log.i(TAG, "Converting curr destination string to Place ID...");
//        PendingResult<AutocompletePredictionBuffer> autocompleteResults
//                = Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, curDestination, null, null);
//        AutocompletePredictionBuffer predictionBuffer = autocompleteResults.await();
//        AutocompletePrediction prediction = predictionBuffer.get(0);
//        String placeId = prediction.getPlaceId();
//        Log.i(TAG, String.format("Place ID = %s", placeId));
//
//        // Get place
//        Log.i(TAG, "Getting Place from Place ID...");
//        PendingResult<PlaceBuffer> placeResults = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
//        PlaceBuffer placeBuffer = placeResults.await();
//        Place destination = placeBuffer.get(0);
//        Log.i(TAG, String.format("Place = %s", destination.getName().toString()));

        // TODO: Get actual location
        detectCurrentPlace();

        // TODO: Figure out length of roundtrip travel to destination


        // TODO: Use preference and figure out where to go on leftover time
    }

    private void detectCurrentPlace() {
        Log.i(TAG, "Detecting current place...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        PendingResult<PlaceLikelihoodBuffer> currResults
                = Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, new PlaceFilter());
        currResults.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(@NonNull PlaceLikelihoodBuffer placeLikelihoods) {
                PlaceLikelihood currLikelihood = placeLikelihoods.get(0);
                Place currPlace = currLikelihood.getPlace();
                Log.i(TAG, String.format("Place = %s", currPlace.getName().toString()));
                findTravelLength(currPlace);
            }
        });
    }

    private void findTravelLength(Place currPlace) {
        Log.i(TAG, "Computing roundtrip travel time to destination...");
        DirectionsAsyncTask directions = new DirectionsAsyncTask();
        directions.execute(currPlace.getName().toString(), curDestination);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //TODO determine if you need a menu for this activity
        return true;
    }


    // MENU METHODS

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //TODO determine if you need a menu for this activity
        return true;
    }

    // Save the selected destination from the dropdown as the current
    // destination
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
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
        duration = intent.getIntExtra("duration", 400);
    }


    // HELPER METHODS

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // TODO: Please implement GoogleApiClient.OnConnectionFailedListener to
        // handle connection failures.
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
                urlStringBuilder.append(URLEncoder.encode(params[1], "UTF-8"));

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

                // using first route by default
                JSONObject route = directionsResult.getJSONArray("routes").getJSONObject(0);
                travelTime = route.getInt("duration");
                Log.i(TAG, String.format("Travel time = %d seconds", travelTime));
            } catch (Exception e) {
                // TODO: handle errors
            }
        }
    }
}
