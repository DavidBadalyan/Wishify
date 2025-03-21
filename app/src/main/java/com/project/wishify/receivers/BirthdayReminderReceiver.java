package com.project.wishify.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class BirthdayReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "birthday_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra("name");
        String date = intent.getStringExtra("date");

        if (name == null || date == null) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Birthday Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Birthday Reminder")
                .setContentText("Today is " + name + "'s birthday! (" + date + ")")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}