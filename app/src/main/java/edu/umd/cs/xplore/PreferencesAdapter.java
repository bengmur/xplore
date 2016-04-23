package edu.umd.cs.xplore;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Adapter for creating the preferences grid list
 */
public class PreferencesAdapter extends BaseAdapter {

    private Context prefContext;
    private PreferenceList prefList = PreferenceList.getInstance();

    public PreferencesAdapter(Context context) {
        prefContext = context;
    }

    @Override
    public int getCount() {
        return prefList.getListLength();
    }

    @Override
    public Object getItem(int position) {
        //TODO make sure this is right
        return 0;
    }

    @Override
    public long getItemId(int position) {
        //TODO make sure this is right
        return 0;
    }

    // set up each individual grid item, which is a card made up of an
    // image and text representing the preference
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CardView prefCardView;

        if (convertView == null) {
            // if it is not recycled, initialize some attributes
            prefCardView = new CardView(prefContext);
            prefCardView.setLayoutParams(new GridView.LayoutParams(650, 650));
            prefCardView.setContentPadding(10, 10, 10, 10);

            LayoutInflater layoutInflater =
                    (LayoutInflater) prefContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout linearLayout
                    = (LinearLayout) layoutInflater.inflate(R.layout.preference_item, parent, false);
            prefCardView.addView(linearLayout);
        } else {
            prefCardView = (CardView) convertView;
        }

        ImageView curImageView =
                (ImageView) ((LinearLayout) prefCardView.getChildAt(0)).getChildAt(0);
        TextView curTextView =
                (TextView) ((LinearLayout) prefCardView.getChildAt(0)).getChildAt(1);

        curImageView.setImageResource(prefList.getImageId(position));
        curTextView.setText(prefList.getPreferenceTitle(position));

        return prefCardView;
    }

}
