package com.project.wishify.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.project.wishify.fragments.HomeFragment;

public class BirthdayReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "birthday_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra("name");
        int notificationId = intent.getIntExtra("notificationId", 0);

        createNotificationChannel(context);

        Intent activityIntent = new Intent(context, HomeFragment.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Birthday Reminder 🎉")
                .setContentText("It's " + name + "'s birthday today! Don't forget to wish them!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Birthday Reminders";
            String description = "Channel for birthday reminder notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
