package com.milvum.stemapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.milvum.stemapp.adapters.ListItemClickedListener;
import com.milvum.stemapp.contracts.VotingBallot;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.geth.implementation.contract.VotingBallotUtil;
import com.milvum.stemapp.model.VoteCandidate;
import com.milvum.stemapp.model.VoteCategory;
import com.milvum.stemapp.model.VoteItem;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.Utils;
import com.milvum.stemapp.utils.VoteItemUtils;
import com.milvum.stemapp.utils.WalletUtil;
import com.milvum.stemapp.view.VoteItemAdapter;

import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;

import java.math.BigInteger;
import java.net.ConnectException;
import java.util.ArrayList;

import rx.Subscriber;
import rx.Subscription;

public class CandidateListActivity extends AppCompatActivity implements ListItemClickedListener<VoteItem> {

    private static final String TAG = "CandidateListActivity";

    private boolean hasBallot = false;
    private int selectedIndex = -1;
    private ArrayList<Integer> indices;
    private Subscription subscription;
    private Button voteButton;
    private ListView candidateList;
    private View selectedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Activity", "Creating candidatelist activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate_list);

        Intent intent = getIntent();
        indices = intent.getIntegerArrayListExtra(Constants.VOTE_ITEMS);
        if (indices == null) {
            indices = new ArrayList<>();
        }
        VoteCategory item = (VoteCategory) VoteItemUtils.getMainVoteItem(this)
                .getDeepItem(indices);

        setTitle(item.getName());

        candidateList = (ListView) findViewById(R.id.candidate_list);

        VoteItemAdapter adapter = new VoteItemAdapter(this, R.layout.vote_item, item.getItems(), this);
        candidateList.setAdapter(adapter);

        voteButton = (Button) this.findViewById(R.id.voteButton);
        voteButton.setOnClickListener(onVoteClick);

        Log.v(TAG, "Created candidateList ativity");
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Thread(checkBallotRunnable).start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Utils.safeUnsubscribe(subscription);
    }

    /**
     * Checks the Ballot token balance of the BallotWallet. Calls onReceiveBallot if a ballot token
     * is present else listens until a ballot is received.
     */
    private Runnable checkBallotRunnable = new Runnable() {
        @Override
        public void run() {
            Context context = getApplicationContext();
            Wallet ballotWallet = WalletUtil.fromKey(CandidateListActivity.this, WalletRole.BALLOT);
            VotingBallot ballotContract = VotingBallotUtil.getContract(context, ballotWallet);

            BigInteger currentBlock;
            try {
                currentBlock =
                        Web3jProvider.getWeb3j().ethBlockNumber().send().getBlockNumber();

                BigInteger ballotBalance = ballotContract.balanceOf(ballotWallet.getAddressHex()).send();
                hasBallot = ballotBalance.compareTo(BigInteger.ZERO) == 1;
            }
            catch (ConnectException e) {
                return;
            }
            catch (Exception e) {
                throw new AssertionError("Failure during request for Ballot Balance", e);
            }

            if (hasBallot) {
                CandidateListActivity.this.onReceivedBallot();
            } else {
                subscription = VotingBallotUtil.getBallotObservable(
                        context,
                        ballotWallet,
                        new DefaultBlockParameterNumber(currentBlock),
                        DefaultBlockParameterName.LATEST)
                    .subscribe(new BallotSubscriber());
            }
        }
    };

    /**
     * Show confirmation dialog for selected candidate (if any candidate is selected)
     */
    private View.OnClickListener onVoteClick = new View.OnClickListener() {
        public void onClick(View v) {
            if (selectedIndex > -1) {
                ArrayList<Integer> newIndices = new ArrayList<>(indices);
                newIndices.add(selectedIndex);
                Utils.showConfirmationDialog(CandidateListActivity.this, newIndices);
            }
        }
    };

    /**
     * Enable or disable the vote button depending the activity state. When enabled = true, the
     * button will only be enabled iff hasBallot is true. When enabled = false, the button will
     * always be disabled.
     *
     * @param enabled - Whether we should attempt to enabled the button or not
     * @return - Whether the button is enabled or not.
     */
    protected boolean setVoteButtonState(boolean enabled) {
        if (selectedIndex >= 0 && enabled && hasBallot) {
            voteButton.setEnabled(true);
        } else {
            voteButton.setEnabled(false);
        }

        return voteButton.isEnabled();
    }

    private void onReceivedBallot() {
        hasBallot = true;
        final Activity activity = CandidateListActivity.this;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button voteButton = (Button) activity.findViewById(R.id.voteButton);
                voteButton.setText(activity.getString(R.string.voteButtonText));

                setVoteButtonState(true);
            }
        });
    }

    @Override
    public void onListItemClicked(View view, VoteItem item, int position) {
        if (item instanceof VoteCandidate) {
            if (selectedView != null) {
                // Should really fix this with databinding in the future.
                selectedView.setSelected(false);
                ImageView image = (ImageView) selectedView.findViewById(R.id.vote_indicator);
                image.setImageResource(R.drawable.vote);
            }

            if (position == selectedIndex) {
                selectedIndex = -1;
                selectedView = null;
                setVoteButtonState(false);
            } else {
                selectedIndex = position;
                selectedView = view;
                selectedView.setSelected(true);
                setVoteButtonState(true);
                ImageView image = (ImageView) view.findViewById(R.id.vote_indicator);
                image.setImageResource(R.drawable.voted);
            }
        } else if (item instanceof VoteCategory) {
            Intent intent = new Intent(CandidateListActivity.this, CandidateListActivity.class);
            ArrayList<Integer> newIndices = new ArrayList<>(indices);
            newIndices.add(position);
            intent.putIntegerArrayListExtra(Constants.VOTE_ITEMS, newIndices);

            startActivity(intent);
        }
    }

    class BallotSubscriber extends Subscriber<VotingBallot.GiveEventResponse> {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            Log.e("Mixing", "Error while listening for Ballot: ", e);
        }

        @Override
        public void onNext(VotingBallot.GiveEventResponse giveEventResponse) {
            Log.v("Mixing", "Ballot received, we can vote");
            onReceivedBallot();
        }
    }
}
