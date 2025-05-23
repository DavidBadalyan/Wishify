package com.project.wishify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.project.wishify.R;

public class TextMessagePreviewActivity extends AppCompatActivity {
    private EditText etMessage;
    private Button btnShare;
    private String name;
    private String phoneNumber;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_message_preview);

        etMessage = findViewById(R.id.et_message_preview);
        btnShare = findViewById(R.id.btn_share);

        // Retrieve data from intent
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        message = intent.getStringExtra("message");
        phoneNumber = intent.getStringExtra("phoneNumber");

        // Set the message in EditText
        if (message != null) {
            etMessage.setText(message);
        } else {
            etMessage.setText("");
            Toast.makeText(this, "No message found", Toast.LENGTH_SHORT).show();
        }

        // Set the activity title
        if (name != null) {
            setTitle("Birthday Wish for " + name);
        }

        btnShare.setOnClickListener(v -> {
            String updatedMessage = etMessage.getText().toString().trim();
            if (updatedMessage.isEmpty()) {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, updatedMessage);
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                shareIntent.putExtra("address", phoneNumber);
            }
            startActivity(Intent.createChooser(shareIntent, "Share Birthday Wish"));
        });
    }
}