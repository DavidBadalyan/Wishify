package com.project.wishify.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.project.wishify.R;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileName;
    private TextView profileEmail;
    private Button editProfileButton;
    private Button changePasswordButton;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        editProfileButton = findViewById(R.id.editProfileButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            profileEmail.setText(currentUser.getEmail());
            profileName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
        } else {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
        }

        editProfileButton.setOnClickListener(v -> showEditProfileDialog());

        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_edit_profile_dialog, null);
        builder.setView(dialogView);

        EditText editUsername = dialogView.findViewById(R.id.username);
        Button cancelButton = dialogView.findViewById(R.id.cancel);
        Button saveButton = dialogView.findViewById(R.id.save);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            editUsername.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
        }

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String newUsername = editUsername.getText().toString().trim();
            if (newUsername.isEmpty()) {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser != null) {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(newUsername)
                        .build();

                currentUser.updateProfile(profileUpdates)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                profileName.setText(newUsername);
                                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(this, "Failed to update profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_change_password_dialog, null);
        builder.setView(dialogView);

        EditText currentPassword = dialogView.findViewById(R.id.curr_pass);
        EditText newPassword = dialogView.findViewById(R.id.new_pass);
        EditText confirmNewPassword = dialogView.findViewById(R.id.conf_new_pass);
        Button cancelButton = dialogView.findViewById(R.id.cancel);
        Button changeButton = dialogView.findViewById(R.id.change);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        changeButton.setOnClickListener(v -> {
            String currentPass = currentPassword.getText().toString().trim();
            String newPass = newPassword.getText().toString().trim();
            String confirmPass = confirmNewPassword.getText().toString().trim();

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "New password and confirmation do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                auth.signInWithEmailAndPassword(user.getEmail(), currentPass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                user.updatePassword(newPass)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                            } else {
                                                Toast.makeText(this, "Failed to update password: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}