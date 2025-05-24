package com.project.wishify.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.project.wishify.R;

import java.io.File;

public class MessagePreviewActivity extends AppCompatActivity {
    private VideoView videoView;
    private String videoFilePath;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_preview);

        AppCompatButton back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MessagePreviewActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvMessage = findViewById(R.id.tv_message);
        AppCompatButton btnShareVia = findViewById(R.id.btn_share_via);
        videoView = findViewById(R.id.video_view);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        videoFilePath = intent.getStringExtra("videoFilePath");
        message = intent.getStringExtra("message");
        String phoneNumber = intent.getStringExtra("phoneNumber");

        tvTitle.setText("Birthday Wish for " + (name != null ? name : "Friend"));

        if (videoFilePath != null && new File(videoFilePath).exists()) {
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoPath(videoFilePath);
            videoView.setOnPreparedListener(mp -> videoView.start());
            videoView.setOnCompletionListener(mp -> videoView.start()); // Loop video
        } else {
            videoView.setVisibility(View.GONE);
        }

        if (message != null && !message.isEmpty()) {
            tvMessage.setVisibility(View.VISIBLE);
            tvMessage.setText(message);
        } else {
            tvMessage.setVisibility(View.GONE);
        }

        if ((videoFilePath != null && new File(videoFilePath).exists()) || (message != null && !message.isEmpty())) {
            btnShareVia.setVisibility(View.VISIBLE);
            btnShareVia.setOnClickListener(v -> shareWish());
        } else {
            btnShareVia.setVisibility(View.GONE);
            Toast.makeText(this, "No content to share", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareWish() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        if (videoFilePath != null && new File(videoFilePath).exists()) {
            File videoFile = new File(videoFilePath);
            Uri videoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    videoFile
            );
            shareIntent.setType("video/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
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

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null && !videoView.isPlaying()) {
            videoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        if (videoView != null) {
            videoView.stopPlayback();
        }
        super.onDestroy();
    }
}