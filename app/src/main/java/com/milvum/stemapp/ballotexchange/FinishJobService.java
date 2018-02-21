package com.milvum.stemapp.ballotexchange;

import android.app.job.JobParameters;
import android.util.Log;

import com.milvum.stemapp.contracts.VotingBallot;
import com.milvum.stemapp.geth.implementation.BlockchainConnection;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.contract.VotingBallotUtil;
import com.milvum.stemapp.model.Notification;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.utils.WalletUtil;

import org.web3j.protocol.core.DefaultBlockParameterName;

import rx.Observable;
import rx.Subscriber;

/**
 * .
 * A job for notifying the user that a Voting Ballot is available in his wallet once this has been
 * provided through the BallotDispenser Contract.
 */

public class FinishJobService extends MixJobService {
    private JobParameters params;

    @Override
    public boolean onStartJob(final JobParameters params) {
        Notification.castNotification(this, Notification.FINISH);
        MixState.setMixState(this, MixState.FINISH);

        Log.v("Mixing", "Listening for Ballot Give Event");
        this.params = params;

        Wallet ballotWallet = WalletUtil.fromKey(this, WalletRole.BALLOT);
        final Observable<VotingBallot.GiveEventResponse> ballotObservable =
                VotingBallotUtil.getBallotObservable(
                        this,
                        ballotWallet,
                        BlockchainConnection.getLatestBlock(getApplicationContext()),
                        DefaultBlockParameterName.LATEST);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ballotObservable.subscribe(new BallotSubscriber());
            }
        }).start();

        return true;
    }

    private class BallotSubscriber extends Subscriber<VotingBallot.GiveEventResponse> {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            reactToError(e, "Error while waiting for ballot");
            jobFinished(params, false);
        }

        @Override
        public void onNext(VotingBallot.GiveEventResponse giveEventResponse) {
            Log.v("Mixing", "Received Ballot, pushing notification");
            // TODO: open app on touch notification
            Notification.castNotification(FinishJobService.this, Notification.BALLOT_READY);

            MixState.setMixState(FinishJobService.this, MixState.IDLE);
            jobFinished(params, false);
        }
    }
}
