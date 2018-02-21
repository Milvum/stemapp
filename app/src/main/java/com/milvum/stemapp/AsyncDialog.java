package com.milvum.stemapp;

import android.app.Dialog;
import android.os.AsyncTask;
import android.util.Log;

import java.net.ConnectException;
import java.util.concurrent.Callable;


public class AsyncDialog extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "AsyncDialog";

    private Dialog dialog;
    private Callable<Boolean> task;

    public AsyncDialog(Dialog dialog, Callable<Boolean> task) {
        this.dialog = dialog;
        this.task = task;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        boolean result = false;
        try {
            result = task.call();
        }
        catch(ConnectException e) {
            Log.w(TAG, "Connect exception while showing dialog", e);
        }
        catch(Exception e) {
            Log.e("AsyncDialog", "Exception when wanting to show dialog", e);
        }

        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if(result) {
            dialog.show();
            Log.d(TAG, "Task was done, dialog is being shown");
        } else { Log.d(TAG, "Task was done, but dialog did not have to be shown"); }
    }
}
