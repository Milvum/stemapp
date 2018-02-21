package com.milvum.stemapp.utils;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.milvum.stemapp.BuildConfig;
import com.milvum.stemapp.ConfirmationDialogFragment;
import com.milvum.stemapp.R;
import com.milvum.stemapp.geth.implementation.ContractInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import rx.Subscription;

/**
 * .
 */

public class Utils {
    private Utils() {

    }
    private static final String TAG = "Utils";

    public static void showConfirmationDialog(Activity activity, ArrayList<Integer> indices) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        Fragment prev = activity.getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        Bundle args = new Bundle();
        args.putIntegerArrayList(Constants.VOTE_ITEMS, indices);

        // Create and show the dialog.
        DialogFragment newFragment = new ConfirmationDialogFragment();
        newFragment.setArguments(args);
        newFragment.show(ft, Constants.CONFIRMATION_DIALOG);
    }


    public static void setViewVisibility(AppCompatDelegate delegate, int viewId, int visibility) {
        final View childView = delegate.findViewById(viewId);
        switch (visibility) {
            case View.VISIBLE:
                childView.setVisibility(visibility);
                break;
            case View.INVISIBLE:
                childView.setVisibility(visibility);
                break;
            case View.GONE:
                childView.setVisibility(visibility);
                break;
            default:
                break;
        }
    }

    public static void setTextViewText(AppCompatDelegate delegate, int viewId, String text) {
        TextView partyNameTextView = (TextView) delegate.findViewById(viewId);
        partyNameTextView.setText(text);
    }

    public static void setTextViewText(View parentView, int viewId, String text) {
        TextView textView = (TextView) parentView.findViewById(viewId);
        textView.setText(text);
    }


    @NonNull
    public static String getRandomToken() {
        Random random = new Random();
        return String.valueOf(Constants.TOKENS.charAt(random.nextInt(Constants.TOKENS.length())));
    }

    public static String loadJSONFromAsset(Context context, String filename) throws IOException {
        try (InputStream is = context.getAssets().open(filename)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);

            return new String(buffer, "UTF-8");
        }
    }

    private static ContractInfo info = null;
    public static ContractInfo getContractInfo(Context context) {
        if(info != null) {
            return info;
        }

        if(BuildConfig.DEBUG && context == null) {
            throw new AssertionError("Context cannot be null on first use of ContractInfo");
        }

        try {
            String rawJson = Utils.loadJSONFromAsset(context, "ContractInfo." + BuildConfig.BUILD_TYPE + ".json");
            JSONObject jsonObject = new JSONObject(rawJson);

            info = new ContractInfo(
                    jsonObject.getString("VotingPass"),
                    jsonObject.getString("BallotDispenser"),
                    jsonObject.getString("VotingBallot"));

            return info;
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Translate a Bitmatrix (ZXing) into a Bitmap (Android)
     * @param matrix - the bitmatrix (e.q. obtained by MultiFormatWriter.encode(...))
     * @return - the resulting bitmap
     */
    public static Bitmap translateBitMatrix(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++ ) {
                bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }

        return bmp;
    }

    public static Dialog createQRCodeDialog(Context context, String content) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200);
            Bitmap bmp = Utils.translateBitMatrix(bitMatrix);

            final Dialog dialog = new Dialog(context, R.style.SecretPopupDialogStyle);
            dialog.setContentView(R.layout.popup_wallet);
            ImageView imageView = (ImageView) dialog.findViewById(R.id.qr_code);
            imageView.setImageDrawable(new BitmapDrawable(context.getResources(), bmp));

            return dialog;
        } catch (WriterException e) {
            Log.wtf("QRCode", e);
            return null;
        }
    }

    /**
     * Safely unsubscribe from a web3j event observer.
     * This works a round a bug (probably the one fixed in
     *   https://github.com/web3j/web3j/commit/001c1cdbe0c7de9a81b927f6a7df9c0792b4770d).
     * WARNING: the unsubscription is asynchronous,
     *   so your subscriber may still fire a couple of times before it is completed.
     */
    public static void safeUnsubscribe(final Subscription subscription) {
        if(subscription == null || subscription.isUnsubscribed()) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    subscription.unsubscribe();
                    Log.w("Unsubcribe", "No NullpointerException was thrown. " +
                            "Check if we can remove the try/catch.");
                } catch (NullPointerException e) {
                    // This is caused by a web3j bug: the filter is unsubscribed twice
                } catch (Exception e) {
                    // Should also catch other exceptions
                }
            }
        }).start();
    }

}
