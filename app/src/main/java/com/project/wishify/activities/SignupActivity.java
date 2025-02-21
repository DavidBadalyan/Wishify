package com.project.wishify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.wishify.R;

public class SignupActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        EditText email = findViewById(R.id.rEmailET);
        EditText password = findViewById(R.id.rPasswordET);
        EditText confPassword = findViewById(R.id.confirmPasswordET);
        Button signUpButton = findViewById(R.id.signupButton);

        signUpButton.setOnClickListener(v -> {
            String emailStr = email.getText().toString().trim();
            String passwordStr = password.getText().toString().trim();
            String confirmPasswordStr = confPassword.getText().toString().trim();

            if (!emailStr.isEmpty() && !passwordStr.isEmpty() && !confirmPasswordStr.isEmpty()) {
                if (confirmPasswordStr.equals(passwordStr)) {
                    signupUser(emailStr, passwordStr);
                } else {
                    Toast.makeText(this, "Password Doesn't Match", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signupUser(String email, String password) {
        TextView errorMessage = findViewById(R.id.errorMessage);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(SignupActivity.this,
                                            "Verification email sent. Please check your inbox.", Toast.LENGTH_SHORT).show();
                                    if (user.isEmailVerified()) {
                                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(SignupActivity.this, "Please verify your email first.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(SignupActivity.this,
                                            "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } else {
                    errorMessage.setText(task.getException().getMessage());
                    errorMessage.setVisibility(View.VISIBLE);
                    Toast.makeText(SignupActivity.this, "Authentication failed: " +
                            task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
