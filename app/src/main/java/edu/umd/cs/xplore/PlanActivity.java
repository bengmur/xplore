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
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TimePicker;

import java.util.ArrayList;

import java.util.Locale;

public class PlanActivity extends AppCompatActivity {

    private NumberPicker hourField;
    private NumberPicker minuteField;

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
        EditText destField = (EditText) findViewById(R.id.destination);
        String inputDest = destField.getText().toString();

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
