package edu.umd.cs.xplore;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;

/**
 * Adapter for creating the preferences grid list
 */
public class PreferencesAdapter extends BaseAdapter {

    private CardView[] cards = new CardView[10];
    private Context prefContext;
    private PreferenceList prefList = PreferenceList.getInstance();
    private HashSet<Integer> selectedPositionIds;

    public PreferencesAdapter(Context context, HashSet<Integer> positionIds) {
        prefContext = context;
        selectedPositionIds = positionIds;
    }

    @Override
    public int getCount() {
        return prefList.getListLength();
    }

    @Override
    public Object getItem(int position) {
        return cards[position];
    }

    @Override
    public long getItemId(int position) {
        return cards[position].getId();
    }

    // set up each individual grid item, which is a card made up of an
    // image and text representing the preference
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CardView prefCardView;

        if (convertView == null) {
            // if it is not recycled, initialize some attributes
            prefCardView = new CardView(prefContext);
            prefCardView.setLayoutParams(new GridView.LayoutParams(300, 300));
            prefCardView.setCardBackgroundColor(Color.WHITE);

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
        curTextView.setTextColor(Color.BLACK);
        cards[position] = prefCardView;
        if (selectedPositionIds.contains(position)) {
            prefCardView.setBackgroundResource(R.color.colorAccent);
        }
        return prefCardView;
    }

}
