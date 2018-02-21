package com.milvum.stemapp.ballotexchange;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.util.Log;

import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.RedirectUtil;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.concurrent.Future;

import rx.functions.Action1;
import rx.functions.Func1;

/**
 * .
 */

public abstract class MixJobService extends JobService {
    private AsyncTask currentTask = null;

    @Override
    public boolean onStopJob(JobParameters params) {
        if(currentTask != null)
            currentTask.cancel(true);

        return true; // restart service, simple as that
    }

    /**
     * Handles exceptions that occur during the mixing
     * @param e - The occuring error
     * @param message - Message for logging
     */
    protected void reactToError(Throwable e, String message) {
        Log.e("Mixing", message, e);
        MixState.setMixState(this, MixState.IDLE);
    }

    /** Check whether a transaction was successfull by monitoring the amountof gas used
     * Maximum gas used is an indication that a requirement wasn't met or an error occured during
     * the transaction or contract call
     * @param task - A future task that returns a TransactionReceipt
     * @param taskTag - The tag used to identify the task with in the log description
     * @param callback - Callback function to react to the answer of the report (called with true if transaction was successful)
     */
    public void reportTransaction(
            final Future<TransactionReceipt> task,
            final String taskTag,
            Action1<Boolean> callback) {

        if (callback == null) {
            throw new IllegalArgumentException("Missing callback argument.");
        }

        new Thread(() -> {
                boolean success;
                try {
                    TransactionReceipt receipt = task.get();

                    BigInteger gas = receipt.getGasUsed();
                    if (gas.equals(BigInteger.valueOf(Constants.LOAD_GAS_LIMIT))) {
                        Log.e(taskTag, "All gas was used, one of the requires" +
                                " in the contract function is not fulfilled");

                        success = false;
                    } else {
                        Log.v(taskTag, "Succesful transaction");

                        success= true;
                    }
                } catch (Exception e) {
                    Log.e("taskTag", "Error during transaction, likely not enough ether", e);

                    success = false;
                    RedirectUtil.goHome(MixJobService.this);
                }

                callback.call(success);
        }).start();
    }
}
