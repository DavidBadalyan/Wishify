package com.project.wishify.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.project.wishify.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Button notificationSettingsButton = findViewById(R.id.notificationSettingsButton);
        Button accountSettingsButton = findViewById(R.id.accountSettingsButton);

        notificationSettingsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Notification Settings clicked", Toast.LENGTH_SHORT).show();
        });

        accountSettingsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Account Settings clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}