package edu.umd.cs.xplore;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RecyclerViewStringListAdapter extends RecyclerView.Adapter {

    private PreferenceList prefList = PreferenceList.getInstance();
    private static String TAG = "RecyclerViewString";

    protected class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvText;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = (ImageView) itemView.findViewById(R.id.ivImage);
            tvText = (TextView) itemView.findViewById(R.id.tvText);
        }
    }

    private List strings;
    HashMap<String, ArrayList<String>> matches;

    public RecyclerViewStringListAdapter(List strings, HashMap<String, ArrayList<String>> matches) {
        this.strings = strings;
        this.matches = matches;
    }

    @Override
    public int getItemCount() {
        return strings.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(itemView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder myViewHolder = (ViewHolder) holder;
        myViewHolder.tvText.setText((String) strings.get(position));
        int imagePos = 0;
        for (String key : this.matches.keySet()) {
            if (this.matches.get(key).contains(this.strings.get(position))) {
                imagePos = prefList.getImageId(key);
            }
        }
        if(imagePos<0){imagePos = 0;}
        myViewHolder.ivImage.setImageResource(prefList.getImageId(imagePos));
    }
}