package edu.umd.cs.xplore;

/**
 * Singleton class that holds the data for the preference grid list
 */
public class PreferenceList {
    private static PreferenceList ourInstance = new PreferenceList();

    public static PreferenceList getInstance() {
        return ourInstance;
    }

    private PreferenceList() {
    }

    private Integer[] imageIds = {
            //TODO Get pictures and insert them here
            R.drawable.sample_2, R.drawable.sample_3,
            R.drawable.sample_4, R.drawable.sample_5,
            R.drawable.sample_6, R.drawable.sample_7,
            R.drawable.sample_0, R.drawable.sample_1,
            R.drawable.sample_2, R.drawable.sample_3
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

    public int getListLength() {
        return imageIds.length;
    }

    public Integer getImageId(int position) {
        return imageIds[position];
    }

    public String getPreferenceTag(int position) {
        return preferenceTags[position];
    }

    public String getPreferenceTitle(int position) {
        return preferenceTitles[position];
    }

}
