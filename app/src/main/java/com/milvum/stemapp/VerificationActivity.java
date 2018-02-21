package com.milvum.stemapp;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.milvum.stemapp.model.VoteStorage;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.RedirectUtil;
import com.milvum.stemapp.utils.Utils;
import com.milvum.stemapp.utils.VoteItemUtils;
import com.milvum.stemapp.utils.VoteUtil;

public class VerificationActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);
        setTitle(getString(R.string.verificationTitle));

        fillTextViews();

        initialize();
    }


    private void initialize() {
        final Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        final View loadingIcon = findViewById(R.id.loading_icon);
        loadingIcon.startAnimation(rotation);

        final Button homeButton = (Button) findViewById(R.id.homeButton);
        homeButton.setOnClickListener(
                v -> RedirectUtil.goHome(VerificationActivity.this));
        homeButton.setEnabled(false);

        final Handler handler = new Handler(getMainLooper());

        Runnable showSuccess = () -> onSuccess();

        handler.postDelayed(showSuccess, Constants.SECRET_WAIT_TIME);
    }

    private void onSuccess() {
        final Button homeButton = (Button) findViewById(R.id.homeButton);
        homeButton.setEnabled(true);
        Utils.setViewVisibility(this.getDelegate(), R.id.loading_state, View.INVISIBLE);
        Utils.setViewVisibility(this.getDelegate(), R.id.success_state, View.VISIBLE);
        final View loadingIcon = findViewById(R.id.loading_icon);
        loadingIcon.clearAnimation();

    }
    private void fillTextViews() {
        Intent intent = getIntent();
        Context context = getApplicationContext();

        String voteKey = intent.getStringExtra(Constants.VOTE_BUNDLE);
        VoteStorage vote = VoteUtil.getInstance(context).getVote(context, voteKey);

        VoteItemUtils.setViewContent(findViewById(R.id.vote_info), vote.getLastBreadCrumb(), vote.voteItem.getName());
    }
}
