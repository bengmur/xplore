package edu.umd.cs.xplore;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.TimePicker;

public class PlanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.plan_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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
