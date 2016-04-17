package edu.umd.cs.xplore;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

/**
 * Created by jrubin on 4/17/16.
 */
public class PreferencesActivity extends FragmentActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences_layout);

        GridView gridview = (GridView) findViewById(R.id.preferencesGrid);

        gridview.setAdapter(new PreferencesAdapter(this));

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Toast.makeText(PreferencesActivity.this, "Clicked: " + position,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

}
