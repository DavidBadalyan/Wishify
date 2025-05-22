package com.project.wishify.adapters;

import static android.content.ContentValues.TAG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
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
    private Context context;
    private OnCustomizeClickListener customizeClickListener;
    private int swipedPosition = -1;

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
        resetSwipe();
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
        resetSwipe();
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
        tvName.setTextColor(0xFFFFFFFF);
        tvName.setTextSize(18);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);

        TextView tvDate = new TextView(holder.itemView.getContext());
        tvDate.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        String formattedDate = formatDate(birthday.getDate());
        tvDate.setText(formattedDate);
        tvDate.setTextColor(0xFFFFFFFF);
        tvDate.setTextSize(16);
        tvDate.setTypeface(Typeface.DEFAULT);

        birthdayRow.addView(tvName);
        birthdayRow.addView(tvDate);

        holder.birthdayListContainer.addView(birthdayRow);

        boolean isSwiped = position == swipedPosition;
        holder.buttonsContainer.setVisibility(isSwiped ? View.VISIBLE : View.GONE);
        holder.birthdayListContainer.setTranslationX(isSwiped ? -getSwipeDistance() : 0f);

        holder.birthdayListContainer.setOnClickListener(v -> {
            if (swipedPosition == holder.getAdapterPosition()) {
                resetSwipe();
            }
        });

        holder.reminderButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                Log.w(TAG, "Reminder button clicked but adapter position is NO_POSITION");
                return;
            }

            Birthday currentBirthday = filteredBirthdayList.get(adapterPosition);
            String name = currentBirthday.getName();
            String date = currentBirthday.getDate();
            Log.d("Reminder", "Name: " + name + ", Date: " + date);

            if (date == null || !date.matches("\\d{2}-\\d{2}")) {
                Toast.makeText(context, "Invalid date format: " + date, Toast.LENGTH_SHORT).show();
                Log.d("Reminder", "Invalid date format");
                return;
            }

            String[] parts = date.split("-");
            String month = parts[0];
            String day = parts[1];
            int year = Calendar.getInstance().get(Calendar.YEAR);
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);

            int parsedMonth = Integer.parseInt(month);
            if (currentMonth > parsedMonth - 1) {
                year += 1;
            }

            if (context != null) {
                try {
                    setBirthdayReminder(context, name, Integer.parseInt(day), parsedMonth, year, currentBirthday.getId());
                    Toast.makeText(context, "Reminder set for " + name, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("Reminder", "Error setting reminder: " + e.getMessage(), e);
                    Toast.makeText(context, "Failed to set reminder", Toast.LENGTH_SHORT).show();
                }
            }
            resetSwipe();
        });

        holder.customizeButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                Log.w(TAG, "Customize button clicked but adapter position is NO_POSITION");
                return;
            }
            if (customizeClickListener != null) {
                customizeClickListener.onCustomizeClicked(filteredBirthdayList.get(adapterPosition));
            }
            resetSwipe();
        });

        holder.deleteButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                Log.w(TAG, "Delete button clicked but adapter position is NO_POSITION");
                return;
            }
            Birthday currentBirthday = filteredBirthdayList.get(adapterPosition);
            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
            builder.setMessage("Do you want to delete " + (currentBirthday.getName() != null ? currentBirthday.getName() : "this birthday") + "'s birthday?");
            builder.setPositiveButton("DELETE", (dialog, which) -> {
                deleteBirthday(holder.itemView.getContext(), currentBirthday, adapterPosition);
            });
            builder.setNegativeButton("CANCEL", (dialog, which) -> {
                dialog.dismiss();
                resetSwipe();
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            dialog.setOnShowListener(d -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFFF0000);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF0000FF);
            });
        });
    }

    private String formatDate(String date) {
        if (date == null || !date.matches("\\d{2}-\\d{2}")) {
            return "N/A";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
            Calendar inputCalendar = Calendar.getInstance();
            inputCalendar.setTime(inputFormat.parse(date));

            Calendar today = Calendar.getInstance();
            int todayMonth = today.get(Calendar.MONTH);
            int todayDay = today.get(Calendar.DAY_OF_MONTH);

            if (inputCalendar.get(Calendar.MONTH) == todayMonth &&
                    inputCalendar.get(Calendar.DAY_OF_MONTH) == todayDay) {
                return "Today";
            }

            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
            return outputFormat.format(inputCalendar.getTime());
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + date, e);
            return "N/A";
        }
    }

    public void setBirthdayReminder(Context context, String name, int day, int month, int year, String birthdayId) {
        Log.d("Reminder", "setBirthdayReminder called with: " + name + ", " + day + "-" + month + "-" + year);

        Intent intent = new Intent(context, BirthdayReminderReceiver.class);
        intent.putExtra("name", name);
        intent.putExtra("notificationId", birthdayId.hashCode());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                birthdayId.hashCode(),
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
        Log.d("Reminder", "Alarm set for: " + calendar.getTime().toString());

        if (alarmManager != null) {
            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.d("Reminder", "Alarm successfully scheduled");
            } catch (SecurityException e) {
                Log.e("Reminder", "SecurityException: " + e.getMessage(), e);
                Toast.makeText(context, "Permission denied for alarm", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("Reminder", "AlarmManager is null");
        }
    }

    private void deleteBirthday(Context context, Birthday birthday, int position) {
        if (birthday == null || birthday.getName() == null || birthday.getId() == null) {
            Log.e(TAG, "Cannot delete birthday: birthday, name, or id is null at position " + position);
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("birthdays");
        databaseReference.orderByChild("name").equalTo(birthday.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean deleted = false;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Birthday dbBirthday = dataSnapshot.getValue(Birthday.class);
                    if (dbBirthday != null && dbBirthday.getDate() != null && dbBirthday.getDate().equals(birthday.getDate()) && dbBirthday.getId().equals(birthday.getId())) {
                        dataSnapshot.getRef().removeValue((error, ref) -> {
                            if (error == null) {
                                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                                Intent intent = new Intent(context, BirthdayReminderReceiver.class);
                                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, birthday.getId().hashCode(), intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                                if (alarmManager != null) {
                                    alarmManager.cancel(pendingIntent);
                                }

                                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.remove(REMINDER_KEY + birthday.getId().hashCode());
                                editor.apply();

                                filteredBirthdayList.remove(position);
                                notifyItemRemoved(position);
                                notifyDataSetChanged();
                                Toast.makeText(context, "Birthday deleted", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Deleted birthday: " + birthday.getName());
                                resetSwipe();
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
                    Log.w(TAG, "No matching birthday found to delete for name: " + birthday.getName() + ", id: " + birthday.getId());
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

    private float getSwipeDistance() {
        LinearLayout buttonsContainer = new LinearLayout(context);
        buttonsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonsContainer.setPadding(
                (int) (8 * context.getResources().getDisplayMetrics().density),
                (int) (8 * context.getResources().getDisplayMetrics().density),
                (int) (8 * context.getResources().getDisplayMetrics().density),
                (int) (8 * context.getResources().getDisplayMetrics().density));

        for (int i = 0; i < 3; i++) {
            Button button = new Button(context);
            button.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) (40 * context.getResources().getDisplayMetrics().density),
                    (int) (40 * context.getResources().getDisplayMetrics().density)));
            if (i < 2) {
                button.setPadding(0, 0, (int) (8 * context.getResources().getDisplayMetrics().density), 0);
            }
            buttonsContainer.addView(button);
        }

        buttonsContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        float swipeDistance = buttonsContainer.getMeasuredWidth();
        // Add a 24dp buffer to ensure full visibility of the remindBd button
        float buffer = 24 * context.getResources().getDisplayMetrics().density;
        swipeDistance += buffer;
        Log.d(TAG, "Calculated swipe distance: " + swipeDistance + " pixels (including " + buffer + "px buffer)");
        return swipeDistance;
    }

    public void resetSwipe() {
        int oldSwipedPosition = swipedPosition;
        swipedPosition = -1;
        if (oldSwipedPosition != -1) {
            notifyItemChanged(oldSwipedPosition);
        }
    }

    public void attachSwipeHelper(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                if (direction == ItemTouchHelper.LEFT) {
                    if (swipedPosition != position) {
                        int oldSwipedPosition = swipedPosition;
                        swipedPosition = position;
                        if (oldSwipedPosition != -1) {
                            notifyItemChanged(oldSwipedPosition);
                        }
                        notifyItemChanged(position);
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    if (swipedPosition == position) {
                        resetSwipe();
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View foregroundView = ((ContactsViewHolder) viewHolder).birthdayListContainer;
                View buttonsView = ((ContactsViewHolder) viewHolder).buttonsContainer;
                float maxSwipe = -getSwipeDistance();

                float clampedDX = Math.max(maxSwipe, Math.min(dX, 0f));

                if (clampedDX < 0) {
                    foregroundView.setTranslationX(clampedDX);
                    buttonsView.setVisibility(View.VISIBLE);
                } else {
                    foregroundView.setTranslationX(clampedDX);
                    buttonsView.setVisibility(View.GONE);
                }

                getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, clampedDX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                View foregroundView = ((ContactsViewHolder) viewHolder).birthdayListContainer;
                View buttonsView = ((ContactsViewHolder) viewHolder).buttonsContainer;
                if (viewHolder.getAdapterPosition() != swipedPosition) {
                    foregroundView.setTranslationX(0f);
                    buttonsView.setVisibility(View.GONE);
                }
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.5f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 8f;
            }

            @Override
            public float getSwipeVelocityThreshold(float defaultValue) {
                return defaultValue * 0.5f;
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
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