package com.project.wishify.fragments;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_preview);

        TextView tvTitle = findViewById(R.id.tv_title);
        AppCompatButton btnShareVia = findViewById(R.id.btn_share_via);
        AppCompatButton btnPlayWish = findViewById(R.id.btn_play_wish);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        audioFilePath = intent.getStringExtra("audioFilePath");
        String phoneNumber = intent.getStringExtra("phoneNumber");

        tvTitle.setText("Birthday Audio Wish for " + name);

        btnPlayWish.setOnClickListener(v -> playAudioWish());
        btnShareVia.setOnClickListener(v -> shareAudioWish());
    }

    private void playAudioWish() {
        if (audioFilePath == null || !new File(audioFilePath).exists()) {
            Toast.makeText(this, "Audio file not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Toast.makeText(this, "Error playing audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareAudioWish() {
        if (audioFilePath == null || !new File(audioFilePath).exists()) {
            Toast.makeText(this, "Audio file not available", Toast.LENGTH_SHORT).show();
            return;
        }

        File audioFile = new File(audioFilePath);
        Uri audioUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                audioFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share audio birthday wish"));
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}