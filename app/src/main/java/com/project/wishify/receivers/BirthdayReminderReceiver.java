package com.project.wishify.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;

import com.project.wishify.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class BirthdayReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "BirthdayReminders";
    private static final String CHANNEL_NAME = "Birthday Reminders";
    private static final String PREFS_NAME = "BirthdayReminders";
    private static final String REMINDER_KEY = "reminder_";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            rescheduleAlarms(context);
            return;
        }

        String name = intent.getStringExtra("name");
        String date = intent.getStringExtra("date");

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.bell)
                .setContentTitle("Birthday Reminder")
                .setContentText("Today is " + name + "'s birthday! (" + date + ")")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(name.hashCode(), builder.build());
    }

    private void rescheduleAlarms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(REMINDER_KEY)) {
                String[] parts = ((String) entry.getValue()).split("\\|");
                String name = parts[0];
                String date = parts[1];
                int position = Integer.parseInt(entry.getKey().replace(REMINDER_KEY, ""));

                Intent intent = new Intent(context, BirthdayReminderReceiver.class);
                intent.putExtra("name", name);
                intent.putExtra("date", date);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, position, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                Calendar calendar = Calendar.getInstance();
                try {
                    calendar.setTime(sdf.parse(date));
                    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                    calendar.set(Calendar.YEAR, currentYear);

                    if (calendar.before(Calendar.getInstance())) {
                        calendar.add(Calendar.YEAR, 1);
                    }

                    calendar.set(Calendar.HOUR_OF_DAY, 11);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        }
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}