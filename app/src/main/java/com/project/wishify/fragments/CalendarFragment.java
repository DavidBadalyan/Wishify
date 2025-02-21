package com.project.wishify.fragments;

import android.app.AlertDialog;
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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.wishify.classes.Birthday;
import com.project.wishify.R;

public class CalendarFragment extends Fragment {

    private TextView birthdayNameTextView;
    private Button btnOpenDialog;
    private DatabaseReference databaseReference;

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
        EditText etBirthday = dialogView.findViewById(R.id.et_birthday);
        Button btnAddBirthday = dialogView.findViewById(R.id.btn_add_birthday);

        AlertDialog dialog = builder.create();

        btnAddBirthday.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String birthday = etBirthday.getText().toString().trim();

            if (!name.isEmpty() && !birthday.isEmpty()) {
                addBirthdayToFirebase(name, birthday);
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void addBirthdayToFirebase(String name, String birthday) {
        String id = databaseReference.push().getKey();
        Birthday birthdayObj = new Birthday(id, name, birthday);
        databaseReference.child(id).setValue(birthdayObj)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Birthday added!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to add", Toast.LENGTH_SHORT).show());
    }
}

