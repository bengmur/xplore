package edu.umd.cs.xplore;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.TimePicker;

import java.lang.reflect.GenericArrayType;
import java.util.ArrayList;

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
                ArrayList<String> destinations = new ArrayList<String>();

                if (inputDest.matches("")) {
                    // some API call based on their time input and assumptions
                } else {
                    destinations.add(inputDest);
                }

                Intent preferencesIntent = new Intent();

                preferencesIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                preferencesIntent.putStringArrayListExtra(Intent.EXTRA_STREAM, destinations);
                preferencesIntent.setType("possibleDestinations");

                startActivity(preferencesIntent);

                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show(); */
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
            time_field.setText(Integer.toString(hours) + ":" + Integer.toString(minutes));
        }
    }
}
