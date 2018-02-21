package com.milvum.stemapp.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.milvum.stemapp.R;
import com.milvum.stemapp.adapters.ListItemClickedListener;
import com.milvum.stemapp.model.VoteCandidate;
import com.milvum.stemapp.model.VoteCategory;
import com.milvum.stemapp.model.VoteItem;

import java.util.List;

/**
 * .
 */

public class VoteItemAdapter extends ArrayAdapter<VoteItem> {
    private int resource;
    private final ListItemClickedListener<VoteItem> listener;

    public VoteItemAdapter(Context context, int resource, List<VoteItem> objects, ListItemClickedListener<VoteItem> listener) {
        super(context, resource, objects);
        this.resource = resource;
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        // Get the data item for this position
        final VoteItem item = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(this.resource, parent, false);
        }

        if (item instanceof VoteCandidate) {
            populateVoteCandidate(convertView, (VoteCandidate) item);
        } else if (item instanceof VoteCategory) {
            populateVoteCategory(convertView, (VoteCategory) item);
        }

        convertView.setOnClickListener(view -> this.listener.onListItemClicked(view, item, position));

        return convertView;
    }

    private void populateVoteCandidate(View view, VoteCandidate candidate) {
        TextView nameView = (TextView) view.findViewById(R.id.vote_name);
        nameView.setText(candidate.getName());

        view.findViewById(R.id.vote_indicator).setVisibility(View.VISIBLE);
    }

    private void populateVoteCategory(View view, VoteCategory category) {
        TextView nameView = (TextView) view.findViewById(R.id.vote_name);
        nameView.setText(category.getName());

        view.findViewById(R.id.vote_icon).setVisibility(View.VISIBLE);
    }
}
