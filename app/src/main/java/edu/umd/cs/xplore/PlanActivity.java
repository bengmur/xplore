package edu.umd.cs.xplore;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;

import java.util.Locale;

public class PlanActivity extends AppCompatActivity {

    private final static String TAG = "PlanActivity";

    private NumberPicker hourField;
    private NumberPicker minuteField;
    private PlaceAutocompleteFragment autocompleteFragment;
    private Place destination = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

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
        hourField.setValue(6);
        minuteField.setValue(30);

        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

//        final TextView txtPlaceDetails = (TextView) findViewById(R.id.txt_place_details);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName());

                destination = place;
//                StringBuilder sb = new StringBuilder();
//                sb.append("<b>").append(place.getName()).append("</b>").append("\n");
//                sb.append(place.getAddress()).append("\n");
//                sb.append(place.getAttributions() == null ? "" : place.getAttributions());
//                txtPlaceDetails.setText(Html.fromHtml(sb.toString()));
//                txtPlaceDetails.setText(autocompleteFragment.toString());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

//        autocompleteFragment.setBoundsBias(new LatLngBounds(
//                new LatLng(-33.880490, 151.184363),
//                new LatLng(-33.858754, 151.229596)));

//        final EditText timeField = (EditText) findViewById(R.id.time_field);
//        timeField.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                TimePickerFragment newFragment = new TimePickerFragment();
//                newFragment.setTime(timeField.getText().toString());
//                newFragment.show(getFragmentManager(), "timePicker");
//                InputMethodManager mgr = (InputMethodManager) PlanActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
//                mgr.hideSoftInputFromWindow(timeField.getWindowToken(), 0);
//            }
//        });

    }

    public void fabClicked(View view) {
        // Get duration
        int hours = hourField.getValue();
        int minutes = minuteField.getValue();

        // Get destination
//        EditText destField = (EditText) findViewById(R.id.destination);
//        String inputDest = destField.getText().toString();
        String inputDest = (destination == null ? "" : destination.getName().toString());

        // If the user didn't input a dest, create a list of possible destinations
        //  Otherwise (if the user did input a dest), the list is just one element (their input)
        ArrayList<String> destinations = new ArrayList<String>();

        if (inputDest.matches("")) {
            // some API call based on their time input and assumptions
        } else {
            destinations.add(inputDest);
        }

        Intent preferencesIntent = new Intent(getApplicationContext(), PreferencesActivity.class);

        preferencesIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        preferencesIntent.putStringArrayListExtra(Intent.EXTRA_STREAM, destinations);
        preferencesIntent.setType("possibleDestinations");

        startActivity(preferencesIntent);

        /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show(); */
    }

//    public static class TimePickerFragment extends DialogFragment
//            implements TimePickerDialog.OnTimeSetListener {
//
//        private String time;
//
//        public void setTime(String time) {
//            this.time = time;
//        }
//
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//            // Use the current time as the default values for the picker
//            String[] splitString = time.split(":");
//            int hours = Integer.parseInt(splitString[0]);
//            int minutes = Integer.parseInt(splitString[1]);
//
//            // Create a new instance of TimePickerDialog and return it
//            return new TimePickerDialog(getActivity(), this, hours, minutes, true);
//        }
//
//        public void onTimeSet(TimePicker view, int hours, int minutes) {
//            // Do something with the time chosen by the user
//            EditText time_field = (EditText) getActivity().findViewById(R.id.time_field);
//            time_field.setText(String.format("%d:%02d", hours, minutes));
//        }
//    }
}
