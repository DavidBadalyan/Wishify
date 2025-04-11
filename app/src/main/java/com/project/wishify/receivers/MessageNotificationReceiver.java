package com.project.wishify.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.project.wishify.R;
import com.project.wishify.fragments.MessagePreviewActivity;

public class MessageNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "message_notification_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra("name");
        String message = intent.getStringExtra("message");
        String phoneNumber = intent.getStringExtra("phoneNumber");
        int notificationId = intent.getIntExtra("notificationId", 0);

        Intent activityIntent = new Intent(context, MessagePreviewActivity.class);
        activityIntent.putExtra("name", name);
        activityIntent.putExtra("message", message);
        activityIntent.putExtra("phoneNumber", phoneNumber);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

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
                .setContentTitle("Birthday Message for " + name)
                .setContentText("Tap to review and send your message")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }
}