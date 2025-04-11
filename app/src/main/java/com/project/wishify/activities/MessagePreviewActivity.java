package com.project.wishify.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.project.wishify.R;

public class MessagePreviewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_preview);

        TextView tvTitle = findViewById(R.id.tv_title);
        EditText etMessagePreview = findViewById(R.id.et_message_preview);
        AppCompatButton btnShareVia = findViewById(R.id.btn_share_via);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String message = intent.getStringExtra("message");
        String phoneNumber = intent.getStringExtra("phoneNumber");

        tvTitle.setText("Birthday Message for " + name);
        etMessagePreview.setText(message);

        btnShareVia.setOnClickListener(v -> {
            String finalMessage = etMessagePreview.getText().toString().trim();
            if (finalMessage.isEmpty()) {
                etMessagePreview.setError("Message cannot be empty");
                return;
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, finalMessage);
            shareIntent.putExtra("phoneNumber", phoneNumber); // Optional, some apps might use this
            startActivity(Intent.createChooser(shareIntent, "Share birthday message"));
            finish(); // Close the activity after sharing
        });
    }
}