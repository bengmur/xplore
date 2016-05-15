package edu.umd.cs.xplore;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlanActivity extends AppCompatActivity {

    public static final String DURATION = "edu.umd.cs.xplore.DURATION";

    private static final String TAG = "PlanActivity";
    private static final String HOUR_FIELD = "edu.umd.cs.xplore.HOUR_FIELD";
    private static final String MINUTE_FIELD = "edu.umd.cs.xplore.MINUTE_FIELD";
    private static final String DESTINATION = "edu.umd.cs.xplore.DESTINATION";

    private NumberPicker hourField;
    private NumberPicker minuteField;
    private PlaceAutocompleteFragment autocompleteFragment;
    private String destination;
    private ProgressDialog findLocationsProgressDialog;
    private LatLng lastLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        // Get last location from MainActivity intent
        Intent intent = getIntent();
        lastLoc = intent.getParcelableExtra(MainActivity.LAST_LOC);
        Log.i(TAG, "Got last loc: " + lastLoc.toString());

        // Setup number pickers
        hourField = (NumberPicker) findViewById(R.id.hour_field);
        minuteField = (NumberPicker) findViewById(R.id.minute_field);
        hourField.setMinValue(0);
        hourField.setMaxValue(23);
        minuteField.setMinValue(0);
        minuteField.setMaxValue(59);
        NumberPicker.Formatter formatter = new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format(Locale.ENGLISH, "%02d", value);
            }
        };
        hourField.setFormatter(formatter);
        minuteField.setFormatter(formatter);

        // Setup autocomplete fragment
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                destination = place.getName().toString();
                Log.i(TAG, "Place: " + destination);
            }

            @Override
            public void onError(Status status) {
                Log.e(TAG, "Error with autocomplete fragment: " + status);
            }
        });
        // autocompleteFragment.setBoundsBias(new LatLngBounds(
        //         new LatLng(-33.880490, 151.184363),
        //         new LatLng(-33.858754, 151.229596)));

        // Set values
        if (savedInstanceState == null) {
            hourField.setValue(6);
            minuteField.setValue(30);
            destination = null;
        } else {
            hourField.setValue(savedInstanceState.getInt(HOUR_FIELD, 6));
            minuteField.setValue(savedInstanceState.getInt(MINUTE_FIELD, 30));
            destination = savedInstanceState.getString(DESTINATION, null);
            if (destination != null) {
                autocompleteFragment.setText(destination);
            }
        }

        findLocationsProgressDialog = new ProgressDialog(this);
        findLocationsProgressDialog.setTitle("Finding Locations");
        findLocationsProgressDialog.setMessage("Finding destinations for you to explore");
        findLocationsProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "In onSaveInstanceState");

        outState.putInt(HOUR_FIELD, hourField.getValue());
        outState.putInt(MINUTE_FIELD, minuteField.getValue());
        outState.putString(DESTINATION, destination);
    }

    public void fabClicked(View view) {
        // Get duration
        int hours = hourField.getValue();
        int minutes = minuteField.getValue();

        // Get destination
        String inputDest = (destination == null) ? "" : destination;
        ArrayList<String> destinations = new ArrayList<String>();

        // Find 4 possible destinations for the user. If the user enters something, add that
        //  on to the list of possible destinations.

        // example using Frederick, MD
        // double currLat = 39.4143;
        // double currLong = -77.4105;

        double currLat = lastLoc.latitude;
        double currLong = lastLoc.longitude;

        /* Using duration, limit travel to final destination and back to origin to 40%
           of duration, so 20% for each way, assuming speed of 60mph. Total duration in
           minutes means total possible miles we can drive in this trip. */
        int totalDuration = (60 * hours) + minutes;
        double radius = totalDuration / 5.0;

        /* Using radius, we can figure out how much the lat and long of the origin
           can change. 1 degree change in lat is 69 miles, and 1 degree in long is 53 miles. */
        double latDelta = radius / 69.0;
        double longDelta = radius / 53.0;

        /* Create an array for 4 different LatLng coordinates that the user can go during
           this time. Each LatLng coordinate is 2 consecutive elements, one for Lat and one
           for Long. Use the Geonames service in an AsyncTask to find the name of
           each calculated location. */
        String[] coordinates = {Double.toString(currLat + latDelta), Double.toString(currLong),
                Double.toString(currLat - latDelta), Double.toString(currLong),
                Double.toString(currLat), Double.toString(currLong + longDelta),
                Double.toString(currLat), Double.toString(currLong - longDelta)};

        // Add user destination if it was entered
        if (!inputDest.isEmpty()) {
            destinations.add(inputDest);
        }

        // Start async task to find possible destinations
        FindLocationName findName = new FindLocationName(totalDuration, destinations);
        findName.execute(coordinates);
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
            Log.e(TAG, "Exception reading stream", e);
            return "";
        }
    }

    private class FindLocationName extends AsyncTask<String, Integer, String[]> {

        private int duration;
        private List<String> destinations;

        public FindLocationName(int duration, List<String> destinations) {
            this.duration = duration;
            this.destinations = destinations;
        }

        @Override
        protected void onPreExecute() {
            findLocationsProgressDialog.show();
        }

        @Override
        protected String[] doInBackground(String... params) {
            String[] queryResponses = new String[4];

            for (int i = 0; i < params.length / 2; i++) {
                HttpURLConnection urlConnection = null;
                try {
                    // Construct request URL string
                    StringBuilder urlStringBuilder = new StringBuilder();
                    urlStringBuilder.append("http://api.geonames.org/findNearbyPlaceNameJSON?lat=");
                    urlStringBuilder.append(URLEncoder.encode(params[i], "UTF-8"));
                    urlStringBuilder.append("&lng=");
                    urlStringBuilder.append(URLEncoder.encode(params[i + 1], "UTF-8"));
                    urlStringBuilder.append("&cities="); // location should have min population of 15000
                    urlStringBuilder.append(URLEncoder.encode(getString(R.string.geonames_min_population), "UTF-8"));
                    urlStringBuilder.append("&username=");
                    urlStringBuilder.append(URLEncoder.encode(getString(R.string.geonames_username), "UTF-8"));

                    // Convert to URL and open connection
                    URL reqURL = new URL(urlStringBuilder.toString());
                    urlConnection = (HttpURLConnection) reqURL.openConnection();

                    // Open connection and store response
                    BufferedInputStream stream = new BufferedInputStream(urlConnection.getInputStream());
                    queryResponses[i] = readStream(stream);
                } catch (Exception e) {
                    Log.e(TAG, "Exception in FindLocationName query", e);
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }

            return queryResponses;
        }

        @Override
        protected void onPostExecute(String[] results) {
            // Parse request results into JSON objects
            for (String result : results) {
                try {
                    // Get the name of the location from the JSON object and add it to ArrayList
                    JSONObject nameQueryResult = new JSONObject(result);
                    JSONArray geonamesArray = nameQueryResult.getJSONArray("geonames");
                    if (geonamesArray.length() > 0) {
                        JSONObject location = geonamesArray.getJSONObject(0);
                        destinations.add(location.get("name").toString());
                        Log.i(TAG, location.get("name").toString());
                    } else {
                        Log.e(TAG, "No places returned at given distance");
                    }
                } catch (Exception e) {
                    // Report exception, but continue parsing other results
                    Log.e(TAG, "JSON parsing exception in FindLocationName", e);
                }
            }

            // Close progress bar
            findLocationsProgressDialog.hide();

            // Start preferences activity, while passing down destinations data
            Intent preferencesIntent = new Intent(getApplicationContext(), PreferencesActivity.class);
            preferencesIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            preferencesIntent.putStringArrayListExtra(Intent.EXTRA_STREAM, (ArrayList<String>) destinations);
            preferencesIntent.putExtra(DURATION, duration);
            preferencesIntent.setType("possibleDestinations");
            startActivity(preferencesIntent);
        }
    }
}
