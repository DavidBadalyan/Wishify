package com.project.wishify.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.project.wishify.activities.MessagePreviewActivity;

public class MessageNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "message_notification_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MessageNotificationReceiver", "Notification received: " + intent.getExtras());
        String name = intent.getStringExtra("name");
        String videoFilePath = intent.getStringExtra("videoFilePath");
        String phoneNumber = intent.getStringExtra("phoneNumber");
        int notificationId = intent.getIntExtra("notificationId", 0);

        Intent activityIntent = new Intent(context, MessagePreviewActivity.class);
        activityIntent.putExtra("name", name);
        activityIntent.putExtra("videoFilePath", videoFilePath);
        activityIntent.putExtra("phoneNumber", phoneNumber);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Log.d("MessageNotificationReceiver", "Intent extras: name=" + name + ", videoFilePath=" + videoFilePath + ", phoneNumber=" + phoneNumber);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Log.d("MessageNotificationReceiver", "PendingIntent created with notificationId: " + notificationId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Message Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManagerCompat.from(context).createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Birthday Video Wish for " + name)
                .setContentText("Tap to review and send your video wish")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }
}