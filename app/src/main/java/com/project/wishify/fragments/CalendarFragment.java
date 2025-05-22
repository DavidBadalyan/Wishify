package com.project.wishify.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.wishify.classes.Birthday;
import com.project.wishify.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private Button btnOpenDialog;
    private DatabaseReference databaseReference;
    private CalendarView calendarView;
    private TextView birthdayName;
    private String selectedDate;
    private List<Birthday> birthdayList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calendar_fragment, container, false);
        btnOpenDialog = view.findViewById(R.id.addButton);
        calendarView = view.findViewById(R.id.calendarView);
        birthdayName = view.findViewById(R.id.birthdayName);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to add birthdays", Toast.LENGTH_SHORT).show();
            return view;
        }
        String userId = auth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("birthdays");

        btnOpenDialog.setOnClickListener(v -> showAddBirthdayDialog());
        fetchBirthdays();
        setupCalendarClickListener();

        return view;
    }

    private void fetchBirthdays() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                birthdayList.clear();
                List<EventDay> events = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Birthday birthday = dataSnapshot.getValue(Birthday.class);
                    if (birthday != null && birthday.getName() != null && birthday.getDate() != null) {
                        birthdayList.add(birthday);
                        try {
                            String[] dateParts = birthday.getDate().split("-");
                            int month = Integer.parseInt(dateParts[0]) - 1; // Months are 0-based
                            int day = Integer.parseInt(dateParts[1]);
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.MONTH, month);
                            calendar.set(Calendar.DAY_OF_MONTH, day);
                            events.add(new EventDay(calendar, R.drawable.balloon));
                        } catch (Exception e) {
                            // Skip invalid date formats
                        }
                    }
                }
                try {
                    calendarView.setEvents(events);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error showing events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load birthdays: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCalendarClickListener() {
        calendarView.setOnDayClickListener(eventDay -> {
            Calendar clickedDay = eventDay.getCalendar();
            String dateStr = String.format(Locale.getDefault(), "%02d-%02d", clickedDay.get(Calendar.MONTH) + 1, clickedDay.get(Calendar.DAY_OF_MONTH));
            List<String> names = new ArrayList<>();
            for (Birthday birthday : birthdayList) {
                if (birthday.getDate().equals(dateStr)) {
                    names.add(birthday.getName());
                }
            }
            if (!names.isEmpty()) {
                String message = "It is " + String.join(" and ", names) + "'s birthday, don't forget to congratulate them!";
                birthdayName.setText(message);
            } else {
                birthdayName.setText("");
            }
        });
    }

    private void showAddBirthdayDialog() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to add birthdays", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset selected date to avoid leftover value from previous dialog
        selectedDate = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_birthday, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.et_name);
        TextView tvBirthday = dialogView.findViewById(R.id.tv_birthday); // Now local
        EditText etPhone = dialogView.findViewById(R.id.et_phone);
        Button btnAddBirthday = dialogView.findViewById(R.id.btn_add_birthday);

        tvBirthday.setOnClickListener(v -> showDatePickerDialog(tvBirthday));

        AlertDialog dialog = builder.create();

        btnAddBirthday.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Please log in to add birthdays", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (!name.isEmpty() && selectedDate != null && !phone.isEmpty()) {
                if (!phone.matches("\\+\\d{10,15}")) {
                    Toast.makeText(requireContext(), "Phone number must be in +1234567890 format", Toast.LENGTH_SHORT).show();
                    return;
                }
                addBirthdayToFirebase(name, selectedDate, phone);
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showDatePickerDialog(TextView tvBirthday) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(selectedCalendar.getTime());
                    tvBirthday.setText(selectedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void addBirthdayToFirebase(String name, String birthday, String phone) {
        Birthday birthdayObj = new Birthday(null, name, birthday, phone);
        databaseReference.push().setValue(birthdayObj)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Birthday added!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to add: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
