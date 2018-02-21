package com.milvum.stemapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.milvum.stemapp.ballotexchange.ExchangeScheduler;
import com.milvum.stemapp.ballotexchange.MixJob;
import com.milvum.stemapp.contracts.BallotDispenser;
import com.milvum.stemapp.geth.implementation.VoteCaster;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.contract.BallotDispenserUtil;
import com.milvum.stemapp.model.Notification;
import com.milvum.stemapp.model.VoteCandidate;
import com.milvum.stemapp.model.VoteStorage;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.MaskSettings;
import com.milvum.stemapp.utils.RedirectUtil;
import com.milvum.stemapp.utils.ToastUtil;
import com.milvum.stemapp.utils.Utils;
import com.milvum.stemapp.utils.VoteItemUtils;
import com.milvum.stemapp.utils.VoteUtil;
import com.milvum.stemapp.utils.WalletUtil;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SuccessActivity extends AppCompatActivity {

    private Handler handler;
    private boolean activityActive = false;
    private ArrayList<Integer> indices;
    private boolean backButtonState;
    private String voteTxHash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Notification.clearNotification(this);
        indices = getIntent().getIntegerArrayListExtra(Constants.VOTE_ITEMS);
        handler = new Handler(getMainLooper());

        setContentView(R.layout.activity_success);
        setTitle(getString(R.string.successTitle));
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);

        VoteItemUtils.setViewContent(getApplicationContext(), findViewById(R.id.vote_info), indices);

        initialize();
    }

    private void initialize() {
        final Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        final View loadingIcon = findViewById(R.id.loading_icon);
        loadingIcon.startAnimation(rotation);

        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.grow);
        final Dialog dialog = new Dialog(this, R.style.SecretPopupDialogStyle);
        dialog.setContentView(R.layout.popup_secret_token);
        final View countdown = dialog.findViewById(R.id.secret_countdown);

        final Runnable hideSecretToken = new Runnable() {
            @Override
            public void run() {
                countdown.clearAnimation();
                dialog.dismiss();
                backButtonState = true;

            }
        };

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                // Prevent dialog close on back press button
                return keyCode == KeyEvent.KEYCODE_BACK;
            }
        });

        final Runnable showSecretToken = new Runnable() {
            @Override
            public void run() {
                if(!activityActive) return;

                dialog.show();

                handler.postDelayed(hideSecretToken, animation.getDuration());
                countdown.startAnimation(animation);
            }
        };

        final Runnable showSuccess = new Runnable() {
            @Override
            public void run() {
                generateVotes(dialog);
                onSuccess();
                handler.postDelayed(showSecretToken, Constants.SECRET_WAIT_TIME);
            }
        };

        final Button homeButton = (Button) findViewById(R.id.homeButton);
        homeButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        goHome();
                    }
                });
        homeButton.setEnabled(false);

        Activity activity = SuccessActivity.this;

        final Context context = getApplicationContext();
        VoteCandidate candidate = (VoteCandidate) VoteItemUtils.getMainVoteItem(context)
                .getDeepItem(indices);

        Wallet wallet = WalletUtil.fromKey(context, WalletRole.BALLOT);

        Log.v("Voting", "Starting voting transaction");
        VoteCaster voteCaster = new VoteCaster(context);


        new Thread(new Runnable() {
            @Override
            public void run() {
                int tokenAmount;

                try {
                    // Check how many votes we should cast in total (real + dummy)
                    BallotDispenser ballotDispenser = BallotDispenserUtil.getContract(context, wallet);
                    tokenAmount = ballotDispenser.tokenAmount().send().intValue();
                } catch (Exception e) {
                    ToastUtil.showToast(activity, R.string.errorNoBlockchain, Toast.LENGTH_SHORT);
                    Log.e("Voting", "Can't read tokenAmount from BallotDispenser, " +
                            "cause of the error: ", e.getCause());
                    goHome();
                    return;
                }

                // Make a list of dummy votes, and insert the real vote in a random spot
                List<VoteCandidate> candidates =
                        VoteItemUtils.getRandomVoteItems(context, tokenAmount - 1);
                int realVoteIndex = new Random().nextInt(candidates.size() + 1);
                candidates.add(realVoteIndex, candidate);
                TransactionReceipt realVoteReceipt = null;

                try {
                    // Cast real and dummy votes
                    List<Future<TransactionReceipt>> receiptFutures =
                            voteCaster.run(wallet, candidates);

                    for (int i = 0; i < receiptFutures.size(); i++) {
                       TransactionReceipt receipt = receiptFutures.get(i).get();

                        if (receipt.getGasUsed().equals(BigInteger.valueOf(Constants.LOAD_GAS_LIMIT))) {
                            ToastUtil.showToast(activity, R.string.errorNoVotingBallot, Toast.LENGTH_SHORT);
                            Log.e("Voting", "No voting ballot when voting");
                            goHome();
                            return;
                        }

                        if (i == realVoteIndex) {
                            realVoteReceipt = receipt;
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    ToastUtil.showToast(activity, R.string.errorNoBlockchain, Toast.LENGTH_SHORT);
                    Log.e("Voting", "Insufficient ether when voting");
                    Log.e("Voting", "Cause of the error: ", e.getCause());
                    goHome();
                    return;
                }


                voteTxHash = realVoteReceipt.getTransactionHash();

                Log.v("Voting", "Successful vote");

                Log.d("Masking", "Stopping MaskVoteJob");
                stopService(new Intent(context, MixJob.MASK.getClass()));

                Log.d("Masking", "Clearing MaskVoteJob");
                MaskSettings.init(context, voteTxHash, Constants.AMOUNT_VOTES);

                Log.d("Masking", "Scheduling MaskVoteJob");
                ExchangeScheduler.scheduleJob(context, MixJob.MASK);

                handler.post(showSuccess);
            }
        }).start();
        backButtonState = false;
    }

    private void goHome() {
        handler.removeCallbacksAndMessages(null);

        RedirectUtil.goHome(this);
    }

    private void generateVotes(Dialog dialog) {
        Context context = getApplicationContext();
        VoteUtil.clearVotes(context);
        VoteUtil voteUtil = VoteUtil.getInstance(context);

        // User vote
        VoteStorage vs = VoteItemUtils.getMainVoteItem(context).getVoteStorage(indices, voteTxHash);

        String userKey = voteUtil.addVote(vs);
        voteUtil.storeVotes(context);

        // Inflate Dialog with custom layout.
        setDialogSecretToken(dialog, userKey);
    }

    private void onSuccess() {
        final Button homeButton = (Button) findViewById(R.id.homeButton);
        homeButton.setEnabled(true);
        Utils.setViewVisibility(this.getDelegate(), R.id.loading_state, View.INVISIBLE);
        Utils.setViewVisibility(this.getDelegate(), R.id.success_state, View.VISIBLE);
        final View loadingIcon = findViewById(R.id.loading_icon);
        loadingIcon.clearAnimation();
    }


    private void setDialogSecretToken(Dialog dialog, String token) {
        final TextView secretTokenTextView = (TextView) dialog.findViewById(R.id.secret_token);
        secretTokenTextView.setText(token);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityActive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityActive = false;
    }

    @Override
    public void onBackPressed() {
        if (backButtonState) {
            RedirectUtil.goHome(this);
        } else {
            Toast.makeText(getApplicationContext(), R.string.backButtonDisabledMessage, Toast.LENGTH_SHORT).show();
        }
    }
}
