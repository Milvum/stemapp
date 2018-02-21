package com.milvum.stemapp.ballotexchange;

import android.app.job.JobParameters;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.milvum.stemapp.R;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.geth.implementation.contract.VotingBallotUtil;
import com.milvum.stemapp.model.VoteCategory;
import com.milvum.stemapp.model.VoteStorage;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.MaskSettings;
import com.milvum.stemapp.utils.Utils;
import com.milvum.stemapp.utils.VoteItemUtils;
import com.milvum.stemapp.utils.VoteUtil;
import com.milvum.stemapp.utils.WalletUtil;

import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.utils.Async;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;

/**
 * Created by Tim de Jong on 13 Nov 2017.
 * A job for collecting other votes that came before and after the user's vote,
 *   to mask which vote was actually cast by the user.
 * post: preferences storage contains maskingVotes, a set
 *   of (Constants.AMOUNT_VOTES) contiguous votes containing the vote with the given voteHash
 */
public class MaskVoteJobService extends MixJobService {
    private static final String TAG = "Masking";

    private JobParameters params;
    private Subscription subscription = null;
    private MaskSettings settings;
    private ScheduledFuture deadlineTask;

    // Set to true if the job is being destroyed, in which case it should
    //   stop functioning until deletion is complete.
    private boolean destroyed = false;

    @Override
    public boolean onStartJob(final JobParameters params) {
        this.params = params;

        final String voteHash = params.getExtras().getString("voteHash");
        Log.d(TAG, "Started MaskVoteJob with voteHash param: " + voteHash);

        final Wallet ballotWallet = WalletUtil.fromKey(this, WalletRole.BALLOT);
        settings = MaskSettings.get(this);

        if (settings.isDone()) {
            Log.d(TAG, "ALREADY DONE MaskVoteJob");

            jobFinished(params, false);
            return false;
        }

        Log.d(TAG, "RESUMING/STARTING MaskVoteJob");

        Utils.getContractInfo(this);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        new Thread(() -> {
            deadlineTask = scheduler.schedule(() -> {
                Log.d(TAG, "Deadline has passed, writing found votes to storage");
                finishMasking();
            }, Constants.MAX_MASKING_TIME, TimeUnit.MILLISECONDS);
            subscription = VotingBallotUtil.onVote(
                    MaskVoteJobService.this,
                    ballotWallet,
                    new DefaultBlockParameterNumber(settings.getLastProcessedBlock()),
                    DefaultBlockParameterName.LATEST)
                    .subscribe(new VotesSubscriber());
        }).start();

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DESTROYED MaskJobService");

        destroyed = true;
        Utils.safeUnsubscribe(subscription);
    }

    /**
     * Executes all cleanup tasks for this job.
     * @param reschedule - Whether the job should be restarted.
     */
    private void endJobSafely(boolean reschedule) {
        Log.d(TAG, "ending MaskVoteJob safely");

        deadlineTask.cancel(false);
        destroyed = true;
        Utils.safeUnsubscribe(subscription);
        jobFinished(params, reschedule);
    }

    /**
     * Write found masking votes to VoteUtil and tear down the job
     */
    private void finishMasking() {
        Context context = this;

        // write maskingVotes to keys
        VoteUtil util = VoteUtil.getInstance(context);
        for (VoteStorage vs : settings.getMaskingVotes()) {
            util.addVote(vs);
        }
        util.storeVotes(context);
        MaskSettings.clear(context);

        endJobSafely(false);
    }

    /**
     * Subscriber that listens to votes on the BlockChain. Found votes are added to the MaskingSettings.
     * When the masking is considered done, finishMasking is called.
     */
    private class VotesSubscriber extends Subscriber<TransferEventResponseWithLog> {
        @Override
        public void onStart() {
            Log.d(TAG, "onStart VotesSubscriber");
        }

        @Override
        public void onCompleted() {
            Log.e(TAG, "onCompleted in VotesSubscriber ---- !!!" +
                    "Didn't expect this to be called !!!");
        }

        @Override
        public void onError(Throwable e) {
            reactToError(e, "onError in VotesSubscriber");

            endJobSafely(true);
        }

        @Override
        public void onNext(TransferEventResponseWithLog event) {
            if (destroyed) {
                return;
            }
            Context context = MaskVoteJobService.this;

            // Create a vote storage object from the event info
            String transHash = event.getLog().getTransactionHash();
            VoteStorage voteStorage = VoteItemUtils.getMainVoteItem(context)
                    .getVoteStorage(event.to.replaceAll("0x", ""), transHash);

            // add vote to the list of masking votes
            settings.addMaskVote(voteStorage);
            MaskSettings.set(context, settings);

            Log.v(TAG, "Added another masking vote, current amount: " + settings.getMaskingVotes().size());

            // When done, store all gathered masking votes using the VoteUtil.
            if(settings.isDone()) {
                Log.d(TAG, "Finished gathering masking votes, linking them to keys");
                finishMasking();
            } else {
                Log.v(TAG, "Found voteHash: " + settings.isMyVoteFound() + " post votes left: " + settings.getNumberOfPostVotes());
            }
        }
    }
}
