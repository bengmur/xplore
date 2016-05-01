package edu.umd.cs.xplore;

import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Activity for selecting preferences for the user's trip
 */
public class PreferencesActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private PreferencesAdapter prefAdapter;
    private String curDestination;
    private ArrayList<String> destinationList;
    private PreferenceList prefList = PreferenceList.getInstance();
    private HashSet<String> selectedPreferences = new HashSet<String>();
    static final String SELECTED_PREFERENCES = "edu.umd.cs.xplore.SELECTED_PREFERENCES";
    static final String PREFERENCE_TITLE = "What are your interests?";

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
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(SELECTED_PREFERENCES, selectedPreferences);
                intent.setType("list/preferences");
                startActivity(intent);
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

    }

    // MENU METHODS

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //TODO determine if you need a menu for this activity
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //TODO determine if you need a menu for this activity
        return true;
    }

    // SPINNER METHODS

    // Save the selected destination from the dropdown as the current
    // destination
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        curDestination = (String) parent.getItemAtPosition(pos);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }


    // HELPER METHODS

    // Save the list of destinations sent from the PlanActivity and
    // set the current destination to be the first destination in the
    // list
    private void handleSendDestinations(Intent intent) {
        destinationList = intent.getStringArrayListExtra(Intent.EXTRA_STREAM);
        curDestination = destinationList.get(0);
    }

}
