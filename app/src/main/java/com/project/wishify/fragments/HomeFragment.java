package com.project.wishify.fragments;

import static android.content.ContentValues.TAG;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.wishify.R;
import com.project.wishify.adapters.BirthdayAdapter;
import com.project.wishify.classes.Birthday;
import com.project.wishify.receivers.BirthdayReminderReceiver;
import com.project.wishify.receivers.MessageNotificationReceiver;
import com.project.wishify.receivers.TextMessageNotificationReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private BirthdayAdapter adapter;
    private List<Birthday> birthdayList;
    private DatabaseReference databaseReference;
    private AppCompatButton customizeButton;

    private void fetchBirthdays() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("birthdays");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                birthdayList.clear();
                Calendar today = Calendar.getInstance();
                today.setTime(new Date());

                List<Birthday> allBirthdays = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Birthday birthday = dataSnapshot.getValue(Birthday.class);
                    if (birthday != null) {
                        allBirthdays.add(birthday);
                    }
                }

                Collections.sort(allBirthdays, new Comparator<Birthday>() {
                    @Override
                    public int compare(Birthday b1, Birthday b2) {
                        return compareDates(b1.getDate(), b2.getDate(), today);
                    }
                });

                birthdayList.addAll(allBirthdays.subList(0, Math.min(4, allBirthdays.size())));
                Log.d(TAG, "Fetched and filtered birthdays: " + birthdayList.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private int compareDates(String date1, String date2, Calendar today) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());

        try {
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();

            Date d1 = sdf.parse(date1);
            Date d2 = sdf.parse(date2);

            cal1.setTime(d1);
            cal2.setTime(d2);

            int currentYear = today.get(Calendar.YEAR);
            cal1.set(Calendar.YEAR, currentYear);
            cal2.set(Calendar.YEAR, currentYear);

            if (cal1.before(today) && !(today.get(Calendar.MONTH) == cal1.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) == cal1.get(Calendar.DAY_OF_MONTH))) {
                cal1.add(Calendar.YEAR, 1);
            }
            if (cal2.before(today) && !(today.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH))) {
                cal2.add(Calendar.YEAR, 1);
            }

            return cal1.compareTo(cal2);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.home_fragment, container, false);

        recyclerView = rootView.findViewById(R.id.recyclerView_birthdays);
        AppCompatButton remindMeButton = rootView.findViewById(R.id.remindMeButton);
        customizeButton = rootView.findViewById(R.id.customizeButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        birthdayList = new ArrayList<>();
        adapter = new BirthdayAdapter(birthdayList);
        recyclerView.setAdapter(adapter);

        fetchBirthdays();

        remindMeButton.setOnClickListener(v -> {
            if (birthdayList.isEmpty()) {
                Toast.makeText(getContext(), "No birthdays available", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = birthdayList.get(0).getName();
            String date = birthdayList.get(0).getDate();

            if (date == null || !date.matches("\\d{2}-\\d{2}")) {
                Toast.makeText(getContext(), "Invalid date format: " + date, Toast.LENGTH_SHORT).show();
                return;
            }

            String[] parts = date.split("-");
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            int year = Calendar.getInstance().get(Calendar.YEAR);
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);

            if (currentMonth > month - 1) {
                year += 1;
            }

            if (getContext() != null) {
                try {
                    setBirthdayReminder(getContext(), name, day, month, year);
                    Toast.makeText(getContext(), "Reminder set for " + name, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Failed to set reminder", Toast.LENGTH_SHORT).show();
                }
            }
        });

        customizeButton.setOnClickListener(v -> showScheduleMessageDialog());

        return rootView;
    }

    private void showScheduleMessageDialog() {
        if (birthdayList.isEmpty()) {
            Toast.makeText(getContext(), "No contacts available to schedule a message", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_send_message_dialog, null);
        builder.setView(dialogView);

        Spinner spinnerContacts = dialogView.findViewById(R.id.spinner_contacts);
        EditText etMessage = dialogView.findViewById(R.id.et_message);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button scheduleButton = dialogView.findViewById(R.id.schedule_button);

        List<String> contactNames = new ArrayList<>();
        for (Birthday birthday : birthdayList) {
            contactNames.add(birthday.getName());
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, contactNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContacts.setAdapter(spinnerAdapter);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        scheduleButton.setOnClickListener(v -> {
            int selectedContactPosition = spinnerContacts.getSelectedItemPosition();
            String message = etMessage.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            Birthday selectedBirthday = birthdayList.get(selectedContactPosition);
            String phoneNumber = selectedBirthday.getPhoneNumber();
            String date = selectedBirthday.getDate();

            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(getContext(), "Phone number not available for " + selectedBirthday.getName(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (date == null || !date.matches("\\d{2}-\\d{2}")) {
                Toast.makeText(getContext(), "Invalid date format for " + selectedBirthday.getName(), Toast.LENGTH_SHORT).show();
                return;
            }

            scheduleMessageNotification(selectedBirthday, message);
            Toast.makeText(getContext(), "Message scheduled for " + selectedBirthday.getName() + " on " + date, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void scheduleMessageNotification(Birthday birthday, String message) {
        Intent intent = new Intent(getContext(), TextMessageNotificationReceiver.class);
        intent.putExtra("name", birthday.getName());
        intent.putExtra("message", message);
        intent.putExtra("phoneNumber", birthday.getPhoneNumber());
        intent.putExtra("notificationId", (birthday.getName() + "_msg").hashCode());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(),
                (birthday.getName() + "_msg").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        String[] dateParts = birthday.getDate().split("-");
        int month = Integer.parseInt(dateParts[0]);
        int day = Integer.parseInt(dateParts[1]);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);

        if (currentMonth > month - 1) {
            year += 1;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1); // 0-based
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (alarmManager != null) {
            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.d(TAG, "Message notification scheduled for " + birthday.getName() + " at " + calendar.getTime());
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Permission denied for scheduling message", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "SecurityException: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "AlarmManager is null");
        }
    }

    public void setBirthdayReminder(Context context, String name, int day, int month, int year) {
        Intent intent = new Intent(context, BirthdayReminderReceiver.class);
        intent.putExtra("name", name);
        intent.putExtra("notificationId", name.hashCode());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (alarmManager != null) {
            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } catch (SecurityException e) {
                Toast.makeText(context, "Permission denied for alarm", Toast.LENGTH_SHORT).show();
            }
        }
    }
}