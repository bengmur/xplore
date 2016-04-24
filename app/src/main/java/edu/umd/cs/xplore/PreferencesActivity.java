package edu.umd.cs.xplore;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Activity for selecting preferences for the user's trip
 */
public class PreferencesActivity extends AppCompatActivity {

    private PreferencesAdapter prefAdapter;
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
        setSupportActionBar(prefToolbar);

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

            private String curPreferenceTitle;
            private String curPreferenceTag;

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                curPreferenceTitle = prefList.getPreferenceTitle(position);
                curPreferenceTag = prefList.getPreferenceTag(position);
                Toast.makeText(PreferencesActivity.this,
                        "Clicked: " + curPreferenceTitle,
                        Toast.LENGTH_SHORT).show();
                if (selectedPreferences.contains(curPreferenceTag)) {
                    selectedPreferences.remove(curPreferenceTag);
                } else {
                    selectedPreferences.add(curPreferenceTag);
                }
                Toast.makeText(PreferencesActivity.this,
                        selectedPreferences.toString(),
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void handleSendDestinations(Intent intent) {
        destinationList = intent.getStringArrayListExtra(Intent.EXTRA_STREAM);
        Toast.makeText(this, destinationList.toString(), Toast.LENGTH_LONG).show();
    }

}
