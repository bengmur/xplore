package edu.umd.cs.xplore;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class RecyclerViewStringListAdapter extends RecyclerView.Adapter {

    private static String TAG = "RecyclerViewStringListAdapter";

    private PreferenceList prefList = PreferenceList.getInstance();
    private ArrayList<String> itinerary;
    private HashMap<String, String> matchNames;
    private HashMap<String, String> matchPreferences;

    public RecyclerViewStringListAdapter(ArrayList<String> itinerary,
                                         HashMap<String, String> matchNames,
                                         HashMap<String, String> matchPreferences) {
        this.itinerary = itinerary;
        this.matchNames = matchNames;
        this.matchPreferences = matchPreferences;
    }

    @Override
    public int getItemCount() {
        return itinerary.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // Get item info
        String id = itinerary.get(position);
        String name = matchNames.get(id);
        String preference = matchPreferences.get(id);

        // Create view holder
        ViewHolder myViewHolder = (ViewHolder) holder;
        myViewHolder.setText(name);
        int imagePos = prefList.getImageId(preference);
        if (imagePos < 0) {
            imagePos = 0;
        }
        myViewHolder.setImageResource(prefList.getImageId(imagePos));
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivImage;
        private TextView tvText;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = (ImageView) itemView.findViewById(R.id.ivImage);
            tvText = (TextView) itemView.findViewById(R.id.tvText);
        }

        public void setImageResource(int resId) {
            this.ivImage.setImageResource(resId);
        }

        public void setText(String text) {
            this.tvText.setText(text);
        }
    }
}