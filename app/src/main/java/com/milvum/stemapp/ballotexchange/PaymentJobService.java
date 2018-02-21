package com.milvum.stemapp.ballotexchange;

import android.app.job.JobParameters;
import android.content.Context;
import android.util.Log;

import com.milvum.stemapp.contracts.BallotDispenser;
import com.milvum.stemapp.geth.implementation.AddressValue;
import com.milvum.stemapp.geth.implementation.BlockchainConnection;
import com.milvum.stemapp.geth.implementation.MixingClient;
import com.milvum.stemapp.geth.implementation.MixingObserver;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.model.Notification;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.model.exceptions.EthereumClientCreationException;
import com.milvum.stemapp.model.exceptions.SendTransactionException;
import com.milvum.stemapp.utils.WalletUtil;

import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.concurrent.Future;

import rx.Subscriber;

/**
 * .
 * The job for providing the Payment of a Voting Pass once a mixing (requested by Requestjob)
 * has been accepted through the Ballot Dispenser Contract
 */

public class PaymentJobService extends MixJobService {
    private static final String TAG = "PaymentJobService";

    private JobParameters params;

    @Override
    public boolean onStartJob(final JobParameters params) {
        Notification.castNotification(this, Notification.PAY);
        MixState.setMixState(this, MixState.PAY);

        this.params = params;
        Context context = getApplicationContext();

        Wallet wallet = WalletUtil.fromKey(context, WalletRole.PASS);
        final AddressValue filterAddress = new AddressValue(wallet.getAddressHex());
        final MixingObserver observer = new MixingObserver(context);

        new Thread(new Runnable() {
            @Override
            public void run() {
                observer.getJoinAcceptedObservable(
                        filterAddress,
                        BlockchainConnection.getLatestBlock(getApplicationContext()),
                        DefaultBlockParameterName.LATEST)
                        .first()
                        .subscribe(new AcceptedSubscriber());
                observer.getJoinRejectedObservable(
                        filterAddress,
                        BlockchainConnection.getLatestBlock(getApplicationContext()),
                        DefaultBlockParameterName.LATEST)
                        .first()
                        .subscribe(new RejectedSubscriber());
            }
        }).start();

        Log.v("Mixing", "Listening for accept/reject mix: " + filterAddress.toString());

        return true;
    }

    private class AcceptedSubscriber extends Subscriber<BallotDispenser.JoinAcceptedEventResponse> {
        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            reactToError(e, "Error occured when listening for acceptance of mix");
            jobFinished(params, false);
        }

        @Override
        public void onNext(BallotDispenser.JoinAcceptedEventResponse joinAcceptedEventResponse) {
            Log.v("Mixing", "Received accept, starting payment");

            Context context = getApplicationContext();
            MixingClient client = MixingClient.getInstance(context);

            Future<TransactionReceipt> paymentTask;
            try {
                paymentTask = client.payDeposit(PaymentJobService.this);
            } catch (EthereumClientCreationException | SendTransactionException e) {
                onError(e);
                return;
            }

            reportTransaction(paymentTask, "Mixing-Payment", (success) -> {
                if (success) {
                    ExchangeScheduler.scheduleJob(context, MixJob.WHISPER);
                }

                jobFinished(params, !success);
            });
        }
    }

    private class RejectedSubscriber extends Subscriber<BallotDispenser.JoinRejectedEventResponse> {
        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            reactToError(e, "Error occured when listening for rejection of mix");
            jobFinished(params, false);
        }

        @Override
        public void onNext(BallotDispenser.JoinRejectedEventResponse joinRejectedEventResponse) {
            Log.v("Mixing", "Received reject, retrying REQUEST");
            ExchangeScheduler.scheduleJob(getApplicationContext(), MixJob.REQUEST);

            jobFinished(params, false);
        }
    }
}
