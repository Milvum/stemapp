package com.milvum.stemapp.utils;

import android.content.Context;
import android.content.Intent;

import com.milvum.stemapp.HomeActivity;

/**
 * .
 */

public class RedirectUtil {
    public static void goHome(Context context) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public static void redirect(Context context, Class<?> target) {
        if (context == null) {
            return;
        }

        if (target == null) {
            return;
        }

        Intent intent = new Intent(context, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}
