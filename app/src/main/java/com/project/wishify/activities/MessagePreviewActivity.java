package com.project.wishify.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.FileProvider;

import com.project.wishify.R;

import java.io.File;
import java.io.IOException;

public class MessagePreviewActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private String audioFilePath;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_preview);

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvMessage = findViewById(R.id.tv_message);
        AppCompatButton btnShareVia = findViewById(R.id.btn_share_via);
        AppCompatButton btnPlayWish = findViewById(R.id.btn_play_wish);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        audioFilePath = intent.getStringExtra("audioFilePath");
        message = intent.getStringExtra("message");
        String phoneNumber = intent.getStringExtra("phoneNumber");

        tvTitle.setText("Birthday Wish for " + (name != null ? name : "Friend"));

        if (audioFilePath != null && new File(audioFilePath).exists()) {
            btnPlayWish.setVisibility(View.VISIBLE);
            btnPlayWish.setOnClickListener(v -> playAudioWish());
        } else {
            btnPlayWish.setVisibility(View.GONE);
        }

        if (message != null && !message.isEmpty()) {
            tvMessage.setVisibility(View.VISIBLE);
            tvMessage.setText(message);
        } else {
            tvMessage.setVisibility(View.GONE);
        }

        if ((audioFilePath != null && new File(audioFilePath).exists()) || (message != null && !message.isEmpty())) {
            btnShareVia.setVisibility(View.VISIBLE);
            btnShareVia.setOnClickListener(v -> shareWish());
        } else {
            btnShareVia.setVisibility(View.GONE);
            Toast.makeText(this, "No content to share", Toast.LENGTH_SHORT).show();
        }
    }

    private void playAudioWish() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> releaseMediaPlayer());
        } catch (IOException e) {
            Toast.makeText(this, "Error playing audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            releaseMediaPlayer();
        }
    }

    private void shareWish() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        if (audioFilePath != null && new File(audioFilePath).exists()) {
            File audioFile = new File(audioFilePath);
            Uri audioUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    audioFile
            );
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
        } else if (message != null && !message.isEmpty()) {
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        } else {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
            return;
        }

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share birthday wish"));
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        releaseMediaPlayer();
        super.onDestroy();
    }
}