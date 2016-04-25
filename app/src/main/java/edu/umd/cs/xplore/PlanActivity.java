package edu.umd.cs.xplore;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

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

import javax.net.ssl.HttpsURLConnection;

public class PlanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.plan_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // Create intent for the PreferencesActivity
                // get text field inputs
                EditText timeField = (EditText) findViewById(R.id.time_field);
                EditText destField = (EditText) findViewById(R.id.destination);

                String time = timeField.getText().toString();
                String inputDest = destField.getText().toString();

                // If the user didn't input a dest, create a list of possible destinations
                //  Otherwise (if the user did input a dest), the list is just one element (their input)
                if (inputDest.matches("")) {
                    // TODO: get user's current location
                    // TODO: also handle situation where user doesn't give permission

                    // example using Frederick, MD
                    double currLat = 39.4143;
                    double currLong = -77.4105;

                    /* Using duration, limit travel to final destination and back to origin to 20%
                        of duration, so 10% for each way, assuming speed of 60mph. Total duration in
                        minutes means total possible miles we can drive in this trip. */
                    String[] durationTextParts = time.split(":");
                    int totalDuration = (Integer.parseInt(durationTextParts[0]) * 60) +
                                            (Integer.parseInt(durationTextParts[1]));
                    double radius = totalDuration/10;

                    /* Using radius, we can figure out how much the lat and long of the origin
                      can change. 1 degree change in lat is 69 miles, and 1 degree in long is 53 miles. */
                    double latDelta = radius/69;
                    double longDelta = radius/53;

                    /* Create an array for 4 different LatLng coordinates that the user can go during
                        this time. Each LatLng coordinate is 2 consecutive elements, one for Lat and one
                        for Long. Use the Geonames service in an AsyncTask to find the name of
                        each calculated location. */
                    Double[] coordinates = {currLat + latDelta, currLong,
                                            currLat - latDelta, currLong,
                                            currLat, currLong + longDelta,
                                            currLat, currLong - longDelta};

                    // List to store results from Geonames.
                    ArrayList<String> destinations = new ArrayList<String>();

                    for (int i = 0; i < coordinates.length; i++) {
                        // TODO: should we wait for these tasks to finish before going on to the prerences activity?
                        FindLocationName findName = new FindLocationName(destinations);
                        findName.execute(Double.toString(coordinates[i]), Double.toString(coordinates[i++]));
                    }

                    // Start preferences activity, while passing down destinations data
                    Intent preferencesIntent = new Intent(getApplicationContext(), PreferencesActivity.class);

                    preferencesIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    preferencesIntent.putStringArrayListExtra(Intent.EXTRA_STREAM, destinations);
                    preferencesIntent.setType("possibleDestinations");

                    startActivity(preferencesIntent);

                } else {
                    // Verify that input destination is a valid location and create list
                    DestInputVerifyTask verifyTask = new DestInputVerifyTask();
                    verifyTask.execute(inputDest);
                }
            }
        });

        final EditText timeField = (EditText) findViewById(R.id.time_field);
        timeField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerFragment newFragment = new TimePickerFragment();
                newFragment.setTime(timeField.getText().toString());
                newFragment.show(getFragmentManager(), "timePicker");
            }
        });
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        private String time;

        public void setTime(String time) {
            this.time = time;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            String[] splitString = time.split(":");
            int hours = Integer.parseInt(splitString[0]);
            int minutes = Integer.parseInt(splitString[1]);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hours, minutes, true);
        }

        public void onTimeSet(TimePicker view, int hours, int minutes) {
            // Do something with the time chosen by the user
            EditText time_field = (EditText) getActivity().findViewById(R.id.time_field);

            String hoursString = Integer.toString(hours);
            if (hoursString.matches("0")) {
                hoursString = "00";
            }

            String minutesString = Integer.toString(minutes);
            if (minutesString.matches("0")) {
                minutesString = "00";
            }
            time_field.setText(hoursString + ":" + minutesString);
        }
    }

    private class DestInputVerifyTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                StringBuilder urlStringBuilder = new StringBuilder();
                urlStringBuilder.append("http://api.geonames.org/postalCodeSearchJSON?placename=");
                urlStringBuilder.append(URLEncoder.encode(params[0], "UTF-8"));
                urlStringBuilder.append("&username=");
                urlStringBuilder.append(URLEncoder.encode(getString(R.string.geonames_username), "UTF-8"));

                URL reqURL = new URL(urlStringBuilder.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) reqURL.openConnection();
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
                JSONObject postalCodeQueryResult = new JSONObject(result);

                // get array of postal codes from the query
                JSONArray postalCodes = postalCodeQueryResult.getJSONArray("postalCodes");

                // If the array is empty, the place isn't valid. Otherwise, we're fine, so
                //  go to next activity.
                if (postalCodes.length() > 0) {
                    // Create an ArrayList to pass to Preferences
                    ArrayList<String> destinations = new ArrayList<String>();
                    String userDest = ((EditText) findViewById(R.id.destination)).getText().toString();
                    destinations.add(userDest);

                    Intent preferencesIntent = new Intent(getApplicationContext(), PreferencesActivity.class);

                    preferencesIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    preferencesIntent.putStringArrayListExtra(Intent.EXTRA_STREAM, destinations);
                    preferencesIntent.setType("possibleDestinations");

                    startActivity(preferencesIntent);
                } else { // stay on this screen and make user correct text
                    // TODO: make this a proper error message that follows android design principles
                    Toast.makeText(PlanActivity.this,
                            "NOT a valid place",
                            Toast.LENGTH_LONG).show();
                }


            } catch (Exception e) {
                // TODO: handle errors
            }
        }
    }

    private class FindLocationName extends AsyncTask<String, Void, String> {

        private ArrayList<String> destinations;

        public FindLocationName(ArrayList<String> destinations) {
            this.destinations = destinations;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                //
                StringBuilder urlStringBuilder = new StringBuilder();
                urlStringBuilder.append("http://api.geonames.org/findNearbyPlaceNameJSON?lat=");
                urlStringBuilder.append(URLEncoder.encode(params[0], "UTF-8"));
                urlStringBuilder.append("&lng=");
                urlStringBuilder.append(URLEncoder.encode(params[1], "UTF-8"));
                urlStringBuilder.append("&username=");
                urlStringBuilder.append(URLEncoder.encode(getString(R.string.geonames_username), "UTF-8"));

                URL reqURL = new URL(urlStringBuilder.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) reqURL.openConnection();
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
                JSONObject nameQueryResult = new JSONObject(result);

                // get the name of the location from the JSON object and add it to ArrayList
                JSONObject location = nameQueryResult.getJSONArray("geonames").getJSONObject(0);
                destinations.add(location.get("name").toString());
            } catch (Exception e) {
                // TODO: handle errors
            }
        }
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
}
