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

import com.project.wishify.activities.TextMessagePreviewActivity;

public class TextMessageNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "text_message_notification_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TextMessageNotificationReceiver", "Notification received: " + intent.getExtras());
        String name = intent.getStringExtra("name");
        String message = intent.getStringExtra("message");
        String phoneNumber = intent.getStringExtra("phoneNumber");
        int notificationId = intent.getIntExtra("notificationId", 0);

        Intent activityIntent = new Intent(context, TextMessagePreviewActivity.class);
        activityIntent.putExtra("name", name);
        activityIntent.putExtra("message", message);
        activityIntent.putExtra("phoneNumber", phoneNumber);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Log.d("TextMessageNotificationReceiver", "Intent extras: name=" + name + ", message=" + message + ", phoneNumber=" + phoneNumber);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Log.d("TextMessageNotificationReceiver", "PendingIntent created with notificationId: " + notificationId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Text Message Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManagerCompat.from(context).createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Birthday Text Wish for " + name)
                .setContentText("Tap to review and send your text wish")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }
}