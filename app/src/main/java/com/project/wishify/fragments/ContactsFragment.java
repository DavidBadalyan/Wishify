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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.wishify.R;
import com.project.wishify.adapters.ContactsAdapter;
import com.project.wishify.classes.Birthday;
import com.project.wishify.receivers.MessageNotificationReceiver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ContactsFragment extends Fragment implements ContactsAdapter.OnCustomizeClickListener {
    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private List<Birthday> birthdayList;
    private DatabaseReference databaseReference;

    private void fetchBirthdays() {
        databaseReference = FirebaseDatabase.getInstance().getReference("birthdays");
        if (databaseReference == null) {
            Log.e(TAG, "DatabaseReference is null");
            return;
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                birthdayList.clear();

                List<Birthday> allBirthdays = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Birthday birthday = dataSnapshot.getValue(Birthday.class);
                    if (birthday != null && birthday.getName() != null && birthday.getDate() != null) {
                        allBirthdays.add(birthday);
                    } else {
                        Log.w(TAG, "Invalid birthday data in snapshot: " + dataSnapshot.toString());
                    }
                }

                Collections.sort(allBirthdays, new Comparator<Birthday>() {
                    @Override
                    public int compare(Birthday b1, Birthday b2) {
                        if (b1 == null || b1.getName() == null || b2 == null || b2.getName() == null) {
                            Log.w(TAG, "Null birthday or name during sorting");
                            return 0;
                        }
                        return b1.getName().compareToIgnoreCase(b2.getName());
                    }
                });

                birthdayList.addAll(allBirthdays);
                Log.d(TAG, "Fetched and sorted birthdays: " + birthdayList.size());
                if (adapter != null) {
                    adapter.updateList(birthdayList);
                } else {
                    Log.e(TAG, "Adapter is null when updating list");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value: " + error.getMessage());
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.contacts_fragment, container, false);

        recyclerView = rootView.findViewById(R.id.recyclerView_birthdays);
        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView is null in contacts_fragment layout");
            return rootView;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        birthdayList = new ArrayList<>();
        adapter = new ContactsAdapter(requireContext(), this);
        recyclerView.setAdapter(adapter);

        SearchView searchView = rootView.findViewById(R.id.searchView);
        if (searchView == null) {
            Log.e(TAG, "SearchView is null in contacts_fragment layout");
            return rootView;
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filter(newText);
                } else {
                    Log.e(TAG, "Adapter is null when filtering");
                }
                return true;
            }
        });

        fetchBirthdays();

        return rootView;
    }

    @Override
    public void onCustomizeClicked(Birthday birthday) {
        showScheduleMessageDialog(birthday);
    }

    private void showScheduleMessageDialog(Birthday birthday) {
        if (birthday == null || birthday.getName() == null || birthday.getPhoneNumber() == null || birthday.getDate() == null) {
            Toast.makeText(getContext(), "Invalid contact data", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_send_message_dialog_no_spinner, null);
        builder.setView(dialogView);

        EditText etMessage = dialogView.findViewById(R.id.et_message);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button scheduleButton = dialogView.findViewById(R.id.schedule_button);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        scheduleButton.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            scheduleMessageNotification(birthday, message);
            Toast.makeText(getContext(), "Message scheduled for " + birthday.getName() + " on " + birthday.getDate(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void scheduleMessageNotification(Birthday birthday, String message) {
        Intent intent = new Intent(getContext(), MessageNotificationReceiver.class);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        adapter = null;
        birthdayList = null;
        databaseReference = null;
    }
}