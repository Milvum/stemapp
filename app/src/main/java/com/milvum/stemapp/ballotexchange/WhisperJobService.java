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
import com.milvum.stemapp.model.Notification;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.utils.WalletUtil;

import org.json.JSONObject;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import rx.Subscriber;

/**
 * .
 * A job for whispering the targetted output address once a signed token has been provided through
 * the BallotDispenser Contract.
 */

public class WhisperJobService extends MixJobService {
    private JobParameters params;

    @Override
    public boolean onStartJob(final JobParameters params) {
        Notification .castNotification(this, Notification.WHISPER);
        MixState.setMixState(this, MixState.WHISPER);

        Log.v("Mixing", "Listening for WarrantyEvent");
        this.params = params;

        Wallet passWallet = WalletUtil.fromKey(this, WalletRole.PASS);
        final AddressValue filterAddress = new AddressValue(passWallet.getAddressHex());
        final MixingObserver observer = new MixingObserver(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                observer.getWarrantyProvidedObservable(
                        filterAddress,
                        BlockchainConnection.getLatestBlock(getApplicationContext()),
                        DefaultBlockParameterName.LATEST)
                        .first()
                        .subscribe(new WarrantySubscriber());
            }
        }).start();

        return true;
    }

    private class WarrantySubscriber extends Subscriber<BallotDispenser.WarrantyProvidedEventResponse> {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            reactToError(e, "Error occured during whispering");
            jobFinished(params, false);
        }

        @Override
        public void onNext(BallotDispenser.WarrantyProvidedEventResponse warrantyProvidedEventResponse) {
            Log.v("Mixing", "Received warranty, starting whisper");

            Context context = WhisperJobService.this.getApplicationContext();

            String warranty = warrantyProvidedEventResponse.warranty;
            BigInteger blindedSignature = new BigInteger(warranty, 16);

            Future<JSONObject> future = MixingClient.getInstance(context)
                    .whisperAddress(context, blindedSignature);

            try {
                future.get();
            } catch (ExecutionException e) {
                // Server has declared your signature to be invalid
                WhisperJobService.this.reactToError(e, "Invalid signature provided");
                return;
            } catch (InterruptedException e) {
                Log.wtf("Mixing", e);
                // An interrupted exception should not occur, but if it does happen, we need to
                // assume that the signature was valid. Resending the same request will not help
                // the server will not accept the same warranty twice.
            }

            ExchangeScheduler.scheduleJob(context, MixJob.FINISH);
            jobFinished(params, false);
        }
    }

}
