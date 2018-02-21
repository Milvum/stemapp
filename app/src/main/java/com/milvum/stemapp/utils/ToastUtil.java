package com.milvum.stemapp.utils;

import android.app.Activity;
import android.widget.Toast;

/**
 * .
 */

public class ToastUtil {
    /**
     * Show a simple text toast on the UIThread (Thread safe)
     * @param activity - The Activity which should show the toast. Used to find the UIThread
     * @param text - The text that will be shown in the toast
     * @param duration - Toast duration (e.g. Toast.LENGTH_SHORT)
     */
    public static void showToast(final Activity activity, final String text, final int duration) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(activity.getApplicationContext(), text, duration);
                toast.show();
            }
        });
    }

    /**
     * Show a simple text toast on the UIThread (Thread safe)
     * @param activity - The activity which should show the toast. Used to find the UIThread
     * @param stringId - an integer referring to an id of the strings from R.string
     * @param duration - Toast duration (e.g. Toast.LENGTH_SHORT)
     */
    public static void showToast(Activity activity, int stringId, int duration) {
        String text = activity.getApplicationContext().getString(stringId);

        showToast(activity, text, duration);
    }
}
