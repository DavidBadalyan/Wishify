package com.project.wishify.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.project.wishify.R;

public class StartActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "WishifyPrefs";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                    setupButtons();
                } else {
                    Toast.makeText(this, "Notification permission denied. Birthday reminders may not work.", Toast.LENGTH_LONG).show();
                    setupButtons();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Check if user is logged in and "Remember Me" is enabled
        if (mAuth.getCurrentUser() != null && sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)) {
            startActivity(new Intent(StartActivity.this, MainActivity.class));
            finish();
        } else {
            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                showNotificationPermissionDialog();
            } else {
                setupButtons();
            }
        }
    }

    private void showNotificationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Notification Permission")
                .setMessage("This app needs notification permission to send birthday reminders. Please grant the permission to enable this feature.")
                .setPositiveButton("Allow", (dialog, which) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .setNegativeButton("Deny", (dialog, which) -> {
                    Toast.makeText(this, "Notification permission denied. Birthday reminders may not work.", Toast.LENGTH_LONG).show();
                    setupButtons();
                })
                .setCancelable(false) // Prevent dismissing dialog without a choice
                .show();
    }

    private void setupButtons() {
        Button login = findViewById(R.id.loginButton);
        Button signup = findViewById(R.id.signupButton);
        Button quickLogin = findViewById(R.id.quickLoginButton);

        login.setOnClickListener(view -> {
            Intent intent = new Intent(StartActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        signup.setOnClickListener(view -> {
            Intent intent = new Intent(StartActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        quickLogin.setOnClickListener(view -> {
            String email = "individualproject2025@gmail.com";
            String password = "YourSecurePassword123";

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(StartActivity.this, task -> {
                        if (task.isSuccessful()) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(KEY_REMEMBER_ME, true);
                            editor.apply();

                            Intent intent = new Intent(StartActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(StartActivity.this,
                                    "Quick login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}