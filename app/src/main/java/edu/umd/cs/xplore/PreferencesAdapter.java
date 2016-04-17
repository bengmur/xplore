package edu.umd.cs.xplore;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Created by jrubin on 4/17/16.
 */
public class PreferencesAdapter extends BaseAdapter {

    private Context myContext;

    private Integer[] imageIds = {
            //TODO Get pictures and insert them here
    };

    public PreferencesAdapter(Context c) {
        myContext = c;
    }

    @Override
    public int getCount() {
        return imageIds.length;
    }

    @Override
    public Object getItem(int position) {
        return imageIds[position];
    }

    @Override
    public long getItemId(int position) {
        //TODO make sure this is right
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}
