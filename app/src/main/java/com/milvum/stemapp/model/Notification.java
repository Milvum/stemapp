package com.milvum.stemapp.model;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.NotificationCompat;

import com.milvum.stemapp.CandidateListActivity;
import com.milvum.stemapp.HomeActivity;
import com.milvum.stemapp.R;

/**
 * Several different notification settings. Each notification contains 3 settings: a title,
 * subtext and an icon. Can be used in combination with the Utils.castNotification function.
 */
public enum Notification {
    BALLOT_READY("StemApp", "Uw stembiljet ligt klaar.", R.drawable.ic_check),
    START("StemApp", "Bezig met opstarten van het inwissel proces", R.drawable.ic_hourglass),
    PAY("StemApp", "Op 25% van het inwissel proces", R.drawable.ic_hourglass),
    WHISPER("StemApp", "Op 50% van het inwissel proces", R.drawable.ic_hourglass),
    FINISH("StemApp", "Op 75% van het inwissel proces. Uw Stembiljet ligt bijna klaar.", R.drawable.ic_hourglass);

    private static final int notificationID = 1234567;
    private String title;
    private String text;
    private int icon;
    Notification(String title, String text, int icon) {
        this.title = title;
        this.text = text;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public int getIcon() {
        return icon;
    }

    /**
     * Cast a notification based on the provided enum. Replaces/updates any notification cast
     * earlier by this function.
     * @param context -
     * @param notification - A notification containing the title, subtext and icon
     */
    public static void castNotification(Context context, Notification notification) {
        // Build intent that navigates to HomeActivity on back
        Intent candidateIntent = new Intent(context, CandidateListActivity.class);
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(candidateIntent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        android.app.Notification not = new NotificationCompat.Builder(context)
            .setSmallIcon(notification.getIcon())
            .setContentTitle(notification.getTitle())
            .setContentText(notification.getText())
            .setContentIntent(pendingIntent)
            .build();

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // use a constant notificationID to replace/update any previous notifications
        notificationManager.notify(notificationID, not);
    }

    public static void clearNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(notificationID);
    }
}
