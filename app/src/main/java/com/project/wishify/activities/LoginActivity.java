package com.project.wishify.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.project.wishify.R;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "WishifyPrefs";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        EditText email = findViewById(R.id.emailET);
        EditText password = findViewById(R.id.passwordET);
        Button login = findViewById(R.id.loginButton);
        Button goToReg = findViewById(R.id.goToRegister);
        CheckBox rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);

        // Check if user is already signed in and "Remember Me" is enabled
        if (mAuth.getCurrentUser() != null && sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        goToReg.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        login.setOnClickListener(v -> {
            String emailStr = email.getText().toString().trim();
            String passwordStr = password.getText().toString().trim();

            if (!emailStr.isEmpty() && !passwordStr.isEmpty()) {
                loginUser(emailStr, passwordStr, rememberMeCheckBox.isChecked());
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser(String emailStr, String passwordStr, boolean rememberMe) {
        mAuth.signInWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save "Remember Me" state
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
                        editor.apply();

                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}