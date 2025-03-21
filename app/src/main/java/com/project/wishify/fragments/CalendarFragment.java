package com.project.wishify.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.wishify.classes.Birthday;
import com.project.wishify.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private Button btnOpenDialog;
    private DatabaseReference databaseReference;
    private TextView tvBirthday;
    private String selectedDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.calendar_fragment, container, false);
        btnOpenDialog = view.findViewById(R.id.addButton);

        databaseReference = FirebaseDatabase.getInstance().getReference("birthdays");

        btnOpenDialog.setOnClickListener(v -> showAddBirthdayDialog());

        return view;
    }

    private void showAddBirthdayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_birthday, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.et_name);
        tvBirthday = dialogView.findViewById(R.id.tv_birthday);
        EditText etPhone = dialogView.findViewById(R.id.et_phone);
        Button btnAddBirthday = dialogView.findViewById(R.id.btn_add_birthday);

        // Set up DatePickerDialog for the birthday TextView
        tvBirthday.setOnClickListener(v -> showDatePickerDialog());

        AlertDialog dialog = builder.create();

        btnAddBirthday.setOnClickListener(v -> {
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

    private void showDatePickerDialog() {
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
        String id = databaseReference.push().getKey();
        Birthday birthdayObj = new Birthday(id, name, birthday, phone);
        databaseReference.child(id).setValue(birthdayObj)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Birthday added!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to add", Toast.LENGTH_SHORT).show());
    }
}