package com.project.wishify.adapters;

import static android.content.ContentValues.TAG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.wishify.R;
import com.project.wishify.classes.Birthday;
import com.project.wishify.receivers.BirthdayReminderReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.BirthdayViewHolder> {

    private List<Birthday> birthdayList;
    private BirthdayViewHolder holder;
    private int position;

    public ContactsAdapter(List<Birthday> birthdayList) {
        this.birthdayList = birthdayList;
    }


    @NonNull
    @Override
    public BirthdayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new BirthdayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BirthdayViewHolder holder, int position) {
        Log.d(TAG, "Binding data for position " + position + ": " + birthdayList.get(position).getName());

        holder.birthdayListContainer.removeAllViews();

        Birthday birthday = birthdayList.get(position);
        LinearLayout birthdayRow = new LinearLayout(holder.itemView.getContext());
        birthdayRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        birthdayRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvName = new TextView(holder.itemView.getContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvName.setText(birthday.getName());
        tvName.setTextSize(18);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);

        TextView tvDate = new TextView(holder.itemView.getContext());
        tvDate.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvDate.setText(birthday.getDate());
        tvDate.setTextSize(16);

        birthdayRow.addView(tvName);
        birthdayRow.addView(tvDate);

        holder.birthdayListContainer.addView(birthdayRow);

        holder.reminderButton.setOnClickListener(v -> {
            setBirthdayReminder(holder.itemView.getContext(), birthday, position);
            Toast.makeText(holder.itemView.getContext(),
                    "Reminder set for " + birthday.getName() + " on " + birthday.getDate(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void setBirthdayReminder(Context context, Birthday birthday, int position) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BirthdayReminderReceiver.class);
        intent.putExtra("name", birthday.getName());
        intent.putExtra("date", birthday.getDate());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, position, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());

        try {
            calendar.setTime(sdf.parse(birthday.getDate()));
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            calendar.set(Calendar.YEAR, currentYear);

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.YEAR, 1);
            }

            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            Log.d(TAG, "Alarm set for " + birthday.getName() + " at " + calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to parse date for " + birthday.getName());
        }
    }


    @Override
    public int getItemCount() {
        return birthdayList.size();
    }

    static class BirthdayViewHolder extends RecyclerView.ViewHolder {
        LinearLayout birthdayListContainer;
        Button reminderButton;

        public BirthdayViewHolder(@NonNull View itemView) {
            super(itemView);
            birthdayListContainer = itemView.findViewById(R.id.birthday_list_container);
            reminderButton = itemView.findViewById(R.id.remindBd);
        }
    }




}

