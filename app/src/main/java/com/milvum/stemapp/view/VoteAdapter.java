package com.milvum.stemapp.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.milvum.stemapp.R;

import java.util.List;

/**
 * .
 */

public class VoteAdapter extends ArrayAdapter<String> {

    private int mResource;

    public VoteAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);

        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        String token = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
        }
        // Lookup view for data population
        TextView tokenView = (TextView) convertView.findViewById(R.id.vote_token);
        // Populate the data into the template view using the data object
        tokenView.setText(token);
        // Return the completed view to render on screen
        return convertView;

    }
}
