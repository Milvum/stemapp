package com.milvum.stemapp;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.milvum.stemapp.model.VoteCategory;
import com.milvum.stemapp.model.VoteItem;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.Utils;
import com.milvum.stemapp.utils.VoteItemUtils;

import java.util.ArrayList;

public class ConfirmationDialogFragment extends DialogFragment {
    private ArrayList<Integer> indices;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        indices = getArguments().getIntegerArrayList(Constants.VOTE_ITEMS);

        View confirmationView = inflater.inflate(R.layout.dialog_confirmation_vote, container, false);
        VoteItemUtils.setViewContent(getContext(), confirmationView, indices);

        confirmationView.findViewById(R.id.yesButton)
                .setOnClickListener(onYes);
        confirmationView.findViewById(R.id.noButton)
                .setOnClickListener(onNo);

        return confirmationView;
    }

    /**
     * Go to success activity to cast your vote
     */
    private View.OnClickListener onYes = new View.OnClickListener() {
        public void onClick(View v) {
            Intent i = new Intent(getActivity(), SuccessActivity.class);
            i.putIntegerArrayListExtra(Constants.VOTE_ITEMS, indices);

            dismiss();
            startActivity(i);
        }
    };

    /**
     * Simply close dialog and do nothing else
     */
    private View.OnClickListener onNo = new View.OnClickListener() {
        public void onClick(View v) {
            // Dismiss the dialog
            dismiss();
        }
    };
}

