package edu.umd.cs.xplore;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Singleton class that holds the data for the preference grid list
 */
public class PreferenceList {
    private static PreferenceList ourInstance = new PreferenceList();
    private Integer[] imageIds = {
            R.mipmap.museums, R.mipmap.landmarks,
            R.mipmap.food_drink, R.mipmap.nightlife,
            R.mipmap.nature_parks, R.mipmap.live_shows,
            R.mipmap.tours, R.mipmap.zoos_aquariums,
            R.mipmap.shopping, R.mipmap.events
    };
    private String[] preferenceTags = {
            "museums", "landmarks", "food_drink",
            "nightlife", "nature_parks", "live_shows",
            "tours", "zoos_aquariums", "shopping",
            "events"
    };
    private String[] preferenceTitles = {
            "Museums", "Landmarks", "Food & Drink",
            "Nightlife", "Nature & Parks", "Live Shows",
            "Tours", "Zoos & Aquariums", "Shopping",
            "Events"
    };

    private PreferenceList() {
    }

    public static PreferenceList getInstance() {
        return ourInstance;
    }

    public int getListLength() {
        return imageIds.length;
    }

    public Integer getImageId(int position) {
        return imageIds[position];
    }

    public Integer getImageId(String pref) {
        for(int i = 0; i < preferenceTags.length; i++) {
            if(preferenceTags[i].equals(pref)) {
                return i;
            }
        }
        return -1;
    }

    public String getPreferenceTag(int position) {
        return preferenceTags[position];
    }

    public String getPreferenceTitle(int position) {
        return preferenceTitles[position];
    }

    public ArrayList<String> getPreferenceTags() {
        return new ArrayList<String>(Arrays.asList(preferenceTags));
    }

    public String getTitleFromTag(String tag) {
        for (int i = 0; i < preferenceTags.length; i++) {
            if (preferenceTags[i].equals(tag)) {
                return preferenceTitles[i];
            }
        }
        throw new IllegalArgumentException("This should not happen.");
    }

}
