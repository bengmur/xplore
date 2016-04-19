package edu.umd.cs.xplore;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
