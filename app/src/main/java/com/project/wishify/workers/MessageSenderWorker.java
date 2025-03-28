package com.project.wishify.workers;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessageSenderWorker extends Worker {

    public static final String KEY_NAME = "name";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_APP = "app";
    public static final String KEY_CELEBRITY = "celebrity";
    private static final String CHANNEL_ID = "message_sender_channel";
    private static final String TAG = "MessageSenderWorker";
    private TextToSpeech tts;
    private File audioFile;

    public MessageSenderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: Starting WorkManager task");

        String name = getInputData().getString(KEY_NAME);
        String phoneNumber = getInputData().getString(KEY_PHONE);
        String message = getInputData().getString(KEY_MESSAGE);
        String appName = getInputData().getString(KEY_APP);
        String celebrity = getInputData().getString(KEY_CELEBRITY);

        if (name == null || phoneNumber == null || message == null || appName == null || celebrity == null) {
            Log.e(TAG, "doWork: Missing input data");
            return Result.failure();
        }

        Log.d(TAG, "doWork: Input data - Name: " + name + ", Phone: " + phoneNumber + ", Message: " + message + ", App: " + appName + ", Celebrity: " + celebrity);

        // Generate the audio file
        audioFile = generateAudioFile(message, celebrity);
        if (audioFile == null) {
            Log.e(TAG, "doWork: Failed to generate audio file");
            return Result.failure();
        }

        Log.d(TAG, "doWork: Audio file generated at: " + audioFile.getAbsolutePath());

        showNotification(name, phoneNumber, appName, audioFile);
        Log.d(TAG, "doWork: Notification shown, task completed");
        return Result.success();
    }

    private File generateAudioFile(String message, String celebrity) {
        Log.d(TAG, "generateAudioFile: Starting audio generation for celebrity: " + celebrity);
        final CountDownLatch latch = new CountDownLatch(1);
        final File[] outputFile = new File[1];

        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "generateAudioFile: TextToSpeech initialized successfully");
                tts.setLanguage(Locale.US);
                switch (celebrity.toLowerCase()) {
                    case "morgan freeman":
                        tts.setPitch(0.8f);
                        tts.setSpeechRate(0.9f);
                        break;
                    case "scarlett johansson":
                        tts.setPitch(1.2f);
                        tts.setSpeechRate(1.0f);
                        break;
                    case "chris hemsworth":
                        tts.setPitch(0.9f);
                        tts.setSpeechRate(1.0f);
                        break;
                    case "beyoncé":
                        tts.setPitch(1.1f);
                        tts.setSpeechRate(1.0f);
                        break;
                    case "tom hanks":
                        tts.setPitch(1.0f);
                        tts.setSpeechRate(0.95f);
                        break;
                    default:
                        tts.setPitch(1.0f);
                        tts.setSpeechRate(1.0f);
                }

                try {
                    File cacheDir = getApplicationContext().getCacheDir();
                    outputFile[0] = new File(cacheDir, "birthday_message_" + System.currentTimeMillis() + ".wav");
                    int result = tts.synthesizeToFile(message, null, outputFile[0], "birthday_message");
                    if (result == TextToSpeech.SUCCESS) {
                        Log.d(TAG, "generateAudioFile: Audio synthesis successful");
                    } else {
                        Log.e(TAG, "generateAudioFile: Audio synthesis failed with result: " + result);
                    }
                    latch.countDown();
                } catch (Exception e) {
                    Log.e(TAG, "generateAudioFile: Error generating audio: " + e.getMessage(), e);
                    latch.countDown();
                }
            } else {
                Log.e(TAG, "generateAudioFile: TextToSpeech initialization failed with status: " + status);
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                Log.e(TAG, "generateAudioFile: TTS timed out after 10 seconds");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "generateAudioFile: Interrupted while waiting for TTS: " + e.getMessage(), e);
            return null;
        }

        if (tts != null) {
            tts.stop(); // Ensure synthesis is stopped
            tts.shutdown();
            Log.d(TAG, "generateAudioFile: TextToSpeech shut down");
        }

        if (outputFile[0] != null && outputFile[0].exists() && outputFile[0].length() > 0) {
            Log.d(TAG, "generateAudioFile: Audio file exists at: " + outputFile[0].getAbsolutePath() + ", size: " + outputFile[0].length() + " bytes");
            return outputFile[0];
        } else {
            Log.e(TAG, "generateAudioFile: Audio file does not exist or is empty");
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    private void showNotification(String name, String phoneNumber, String appName, File audioFile) {
        Log.d(TAG, "showNotification: Creating notification for " + name);

        String appScheme;
        switch (appName.toLowerCase()) {
            case "whatsapp":
                appScheme = "whatsapp://send?phone=" + phoneNumber;
                break;
            case "telegram":
                appScheme = "tg://msg?to=" + phoneNumber;
                break;
            case "viber":
                appScheme = "viber://chat?number=" + phoneNumber;
                break;
            default:
                Log.e(TAG, "showNotification: Invalid app name: " + appName);
                return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setData(Uri.parse(appScheme));
        intent.setType("audio/*");

        // Use FileProvider to generate a content URI
        Uri audioUri = FileProvider.getUriForFile(
                getApplicationContext(),
                getApplicationContext().getPackageName() + ".fileprovider",
                audioFile
        );
        intent.putExtra(Intent.EXTRA_STREAM, audioUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant permission to the receiving app

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Message Sender",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManagerCompat.from(getApplicationContext()).createNotificationChannel(channel);
            Log.d(TAG, "showNotification: Notification channel created");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Send Birthday Message")
                .setContentText("Tap to send a birthday message to " + name)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(getApplicationContext()).notify((int) System.currentTimeMillis(), builder.build());
        Log.d(TAG, "showNotification: Notification sent");
    }
}