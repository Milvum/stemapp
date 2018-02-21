package com.milvum.stemapp.ballotexchange;

import android.app.job.JobParameters;
import android.content.Context;
import android.util.Log;

import com.milvum.stemapp.geth.implementation.BlockchainConnection;
import com.milvum.stemapp.geth.implementation.MixingClient;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.model.Notification;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.concurrent.Future;

/**
 * .
 * The Job for requesting a mixing through the BallotDispenser Contract
 */

public class RequestJobService extends MixJobService {

    @Override
    public boolean onStartJob(final JobParameters params) {
        Notification.castNotification(this, Notification.START);
        MixState.setMixState(this, MixState.REQUEST);

        Log.v("Mixing", "Requesting mix");
        Context context = getApplicationContext();
        MixingClient client = MixingClient.getInstance(context);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Future<TransactionReceipt> requestTask = client.requestMix();
                    
                    BigInteger currentBlock =
                            Web3jProvider.getWeb3j().ethBlockNumber().send().getBlockNumber();
                    BlockchainConnection.setLatestBlock(getApplicationContext(), currentBlock);

                    reportTransaction(requestTask, "Mixing-Request", (success) -> {
                        if (success) {
                            ExchangeScheduler.scheduleJob(context, MixJob.PAYMENT);
                        }

                        jobFinished(params, !success);
                    });
                } catch (IllegalArgumentException e) {
                    Log.e("RequestJobService", e.getMessage(), e);
                } catch (Exception e) {
                    RequestJobService.this.reactToError(e, "Unable to request Mix");
                }
            }
        }).start();

        return true;
    }
}
