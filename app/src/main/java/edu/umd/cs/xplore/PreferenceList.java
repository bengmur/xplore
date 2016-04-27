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
