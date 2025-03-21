package com.project.wishify.adapters;

import static android.content.ContentValues.TAG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.wishify.R;
import com.project.wishify.classes.Birthday;
import com.project.wishify.receivers.BirthdayReminderReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder> {

    private List<Birthday> originalBirthdayList;
    private List<Birthday> filteredBirthdayList;
    private static final String PREFS_NAME = "BirthdayReminders";
    private static final String REMINDER_KEY = "reminder_";
    private DatabaseReference databaseReference;
    private int selectedPosition = -1;
    private Context context;
    private OnCustomizeClickListener customizeClickListener;

    // Callback interface to notify the fragment when the Customize button is clicked
    public interface OnCustomizeClickListener {
        void onCustomizeClicked(Birthday birthday);
    }

    public ContactsAdapter(Context context, OnCustomizeClickListener listener) {
        this.context = context;
        this.originalBirthdayList = new ArrayList<>();
        this.filteredBirthdayList = new ArrayList<>();
        this.customizeClickListener = listener;
        databaseReference = FirebaseDatabase.getInstance().getReference("birthdays");
    }

    public void updateList(List<Birthday> newList) {
        if (newList == null) {
            Log.e(TAG, "updateList: newList is null");
            return;
        }
        originalBirthdayList.clear();
        originalBirthdayList.addAll(newList);
        filteredBirthdayList.clear();
        filteredBirthdayList.addAll(newList);
        notifyDataSetChanged();
        Log.d(TAG, "Updated list with " + newList.size() + " birthdays");
    }

    public void filter(String query) {
        filteredBirthdayList.clear();
        if (query == null || query.isEmpty()) {
            filteredBirthdayList.addAll(originalBirthdayList);
        } else {
            String searchQuery = query.toLowerCase(Locale.getDefault());
            for (Birthday birthday : originalBirthdayList) {
                if (birthday != null && birthday.getName() != null &&
                        birthday.getName().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                    filteredBirthdayList.add(birthday);
                }
            }
        }
        selectedPosition = -1;
        notifyDataSetChanged();
        Log.d(TAG, "Filtered list to " + filteredBirthdayList.size() + " birthdays with query: " + query);
    }

    @NonNull
    @Override
    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactsViewHolder holder, int position) {
        Log.d(TAG, "Binding data for position " + position);
        if (position >= filteredBirthdayList.size()) {
            Log.e(TAG, "Position " + position + " is out of bounds for filteredBirthdayList size " + filteredBirthdayList.size());
            return;
        }

        Birthday birthday = filteredBirthdayList.get(position);
        if (birthday == null) {
            Log.e(TAG, "Birthday at position " + position + " is null");
            return;
        }

        holder.birthdayListContainer.removeAllViews();

        LinearLayout birthdayRow = new LinearLayout(holder.itemView.getContext());
        birthdayRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        birthdayRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvName = new TextView(holder.itemView.getContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvName.setText(birthday.getName() != null ? birthday.getName() : "Unknown");
        tvName.setTextColor(0xFF000000);
        tvName.setTextSize(18);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);

        TextView tvDate = new TextView(holder.itemView.getContext());
        tvDate.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvDate.setText(birthday.getDate() != null ? birthday.getDate() : "N/A");
        tvDate.setTextSize(16);

        birthdayRow.addView(tvName);
        birthdayRow.addView(tvDate);

        holder.birthdayListContainer.addView(birthdayRow);

        holder.buttonsContainer.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition == position) {
                selectedPosition = -1;
            } else {
                selectedPosition = position;
            }
            notifyDataSetChanged();
            Log.d(TAG, "Toggled buttons visibility for position " + position + ", selectedPosition: " + selectedPosition);
        });

        holder.reminderButton.setOnClickListener(v -> {
            setBirthdayReminder(holder.itemView.getContext(), birthday, position);
            Toast.makeText(holder.itemView.getContext(),
                    "Reminder set for " + birthday.getName() + " on " + birthday.getDate(),
                    Toast.LENGTH_SHORT).show();
        });

        holder.customizeButton.setOnClickListener(v -> {
            if (customizeClickListener != null) {
                customizeClickListener.onCustomizeClicked(birthday);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
            builder.setMessage("Do you want to delete " + (birthday.getName() != null ? birthday.getName() : "this birthday") + "'s birthday?");
            builder.setPositiveButton("DELETE", (dialog, which) -> {
                deleteBirthday(holder.itemView.getContext(), birthday, position);
            });
            builder.setNegativeButton("CANCEL", (dialog, which) -> {
                dialog.dismiss();
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFFF0000);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF0000FF);
        });
    }

    private void setBirthdayReminder(Context context, Birthday birthday, int position) {
        if (birthday == null || birthday.getDate() == null) {
            Log.e(TAG, "Cannot set reminder: birthday or date is null at position " + position);
            Toast.makeText(context, "Failed to set reminder: Invalid birthday data", Toast.LENGTH_SHORT).show();
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            Toast.makeText(context, "Failed to set reminder: System service unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

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

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    Toast.makeText(context, "Please allow exact alarms in settings to set reminders", Toast.LENGTH_LONG).show();
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(settingsIntent);
                    return;
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
            Log.d(TAG, "Alarm set for " + birthday.getName() + " at " + calendar.getTime());

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(REMINDER_KEY + position, birthday.getName() + "|" + birthday.getDate());
            editor.apply();
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to parse date for " + (birthday.getName() != null ? birthday.getName() : "unknown"));
            Toast.makeText(context, "Failed to set reminder: Invalid date format", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteBirthday(Context context, Birthday birthday, int position) {
        if (birthday == null || birthday.getName() == null) {
            Log.e(TAG, "Cannot delete birthday: birthday or name is null at position " + position);
            return;
        }

        databaseReference.orderByChild("name").equalTo(birthday.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean deleted = false;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Birthday dbBirthday = dataSnapshot.getValue(Birthday.class);
                    if (dbBirthday != null && dbBirthday.getDate() != null && dbBirthday.getDate().equals(birthday.getDate())) {
                        dataSnapshot.getRef().removeValue((error, ref) -> {
                            if (error == null) {
                                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                                Intent intent = new Intent(context, BirthdayReminderReceiver.class);
                                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, position, intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                                alarmManager.cancel(pendingIntent);

                                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.remove(REMINDER_KEY + position);
                                editor.apply();

                                filteredBirthdayList.remove(position);
                                notifyItemRemoved(position);
                                notifyDataSetChanged();
                                Toast.makeText(context, "Birthday deleted", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Deleted birthday: " + birthday.getName());
                            } else {
                                Toast.makeText(context, "Failed to delete birthday", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Failed to delete birthday: " + error.getMessage());
                            }
                        });
                        deleted = true;
                        break;
                    }
                }
                if (!deleted) {
                    Log.w(TAG, "No matching birthday found to delete for name: " + birthday.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Database error on delete: " + error.getMessage());
            }
        });
    }

    @Override
    public int getItemCount() {
        int size = filteredBirthdayList != null ? filteredBirthdayList.size() : 0;
        Log.d(TAG, "getItemCount: " + size);
        return size;
    }

    static class ContactsViewHolder extends RecyclerView.ViewHolder {
        LinearLayout birthdayListContainer;
        LinearLayout buttonsContainer;
        Button reminderButton;
        Button customizeButton;
        Button deleteButton;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            birthdayListContainer = itemView.findViewById(R.id.birthday_list_container);
            buttonsContainer = itemView.findViewById(R.id.buttons_container);
            reminderButton = itemView.findViewById(R.id.remindBd);
            customizeButton = itemView.findViewById(R.id.customizeM);
            deleteButton = itemView.findViewById(R.id.deleteBd);

            if (birthdayListContainer == null) {
                Log.e(TAG, "birthdayListContainer is null in item_contact layout");
            }
            if (buttonsContainer == null) {
                Log.e(TAG, "buttonsContainer is null in item_contact layout");
            }
            if (reminderButton == null) {
                Log.e(TAG, "reminderButton is null in item_contact layout");
            }
            if (customizeButton == null) {
                Log.e(TAG, "customizeButton is null in item_contact layout");
            }
            if (deleteButton == null) {
                Log.e(TAG, "deleteButton is null in item_contact layout");
            }
        }
    }
}