package com.project.wishify.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.project.wishify.R;

public class StartActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "WishifyPrefs";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        if (mAuth.getCurrentUser() != null && sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)) {
            startActivity(new Intent(StartActivity.this, MainActivity.class));
            finish();
        } else {
            Button login = findViewById(R.id.loginButton);
            Button signup = findViewById(R.id.signupButton);
            Button quickLogin = findViewById(R.id.quickLoginButton);

            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(StartActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });

            signup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(StartActivity.this, SignupActivity.class);
                    startActivity(intent);
                }
            });

            quickLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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
                }
            });
        }
    }
}