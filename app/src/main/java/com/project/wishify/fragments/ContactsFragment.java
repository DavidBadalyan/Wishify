package com.project.wishify.fragments;

import static android.content.ContentValues.TAG;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.wishify.R;
import com.project.wishify.adapters.ContactsAdapter;
import com.project.wishify.classes.Birthday;
import com.project.wishify.receivers.MessageNotificationReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ContactsFragment extends Fragment implements ContactsAdapter.OnCustomizeClickListener {
    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private List<Birthday> birthdayList;
    private DatabaseReference databaseReference;
    private TextToSpeech tts;
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
    private static final String GEMINI_API_KEY = "AIzaSyA134vyOKrkVZf6Q_gW05G_YmYrNYenDhY";
    private static final int MAX_RETRIES = 3;

    private void fetchBirthdays() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("birthdays");
        if (databaseReference == null) {
            Log.e(TAG, "DatabaseReference is null");
            return;
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                birthdayList.clear();

                List<Birthday> allBirthdays = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Birthday birthday = dataSnapshot.getValue(Birthday.class);
                    if (birthday != null && birthday.getName() != null && birthday.getDate() != null) {
                        allBirthdays.add(birthday);
                    } else {
                        Log.w(TAG, "Invalid birthday data in snapshot: " + dataSnapshot.toString());
                    }
                }

                Collections.sort(allBirthdays, new Comparator<Birthday>() {
                    @Override
                    public int compare(Birthday b1, Birthday b2) {
                        if (b1 == null || b1.getName() == null || b2 == null || b2.getName() == null) {
                            Log.w(TAG, "Null birthday or name during sorting");
                            return 0;
                        }
                        return b1.getName().compareToIgnoreCase(b2.getName());
                    }
                });

                birthdayList.addAll(allBirthdays);
                Log.d(TAG, "Fetched and sorted birthdays: " + birthdayList.size());
                if (adapter != null) {
                    adapter.updateList(birthdayList);
                } else {
                    Log.e(TAG, "Adapter is null when updating list");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value: " + error.getMessage());
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.contacts_fragment, container, false);

        recyclerView = rootView.findViewById(R.id.recyclerView_birthdays);
        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView is null in contacts_fragment layout");
            return rootView;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        birthdayList = new ArrayList<>();
        adapter = new ContactsAdapter(requireContext(), this);
        recyclerView.setAdapter(adapter);

        SearchView searchView = rootView.findViewById(R.id.searchView);
        if (searchView == null) {
            Log.e(TAG, "SearchView is null in contacts_fragment layout");
            return rootView;
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filter(newText);
                } else {
                    Log.e(TAG, "Adapter is null when filtering");
                }
                return true;
            }
        });

        fetchBirthdays();

        return rootView;
    }

    @Override
    public void onCustomizeClicked(Birthday birthday) {
        showScheduleMessageDialog(birthday);
    }

    private void showScheduleMessageDialog(Birthday birthday) {
        if (birthday == null || birthday.getName() == null || birthday.getPhoneNumber() == null || birthday.getDate() == null) {
            Toast.makeText(getContext(), "Invalid contact data", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_send_message_dialog_no_spinner, null);
        builder.setView(dialogView);

        EditText etMessage = dialogView.findViewById(R.id.et_message);
        Spinner spinnerCelebrity = dialogView.findViewById(R.id.spinner_celebrity);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button scheduleButton = dialogView.findViewById(R.id.schedule_button);

        etMessage.setText("Generating wish...");

        generateAIWish(birthday.getName(), new WishCallback() {
            @Override
            public void onWishGenerated(String wish) {
                requireActivity().runOnUiThread(() -> etMessage.setText(wish));
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    etMessage.setText(getFallbackWish(birthday.getName()));
                    Toast.makeText(getContext(), "Failed to generate wish: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });

        ArrayAdapter<CharSequence> celebrityAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.celebrity_list,
                android.R.layout.simple_spinner_item
        );
        celebrityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCelebrity.setAdapter(celebrityAdapter);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        scheduleButton.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            String selectedCelebrity = spinnerCelebrity.getSelectedItem().toString();

            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedCelebrity.equals("Select a celebrity")) {
                Toast.makeText(getContext(), "Please select a celebrity", Toast.LENGTH_SHORT).show();
                return;
            }

            File audioFile = generateAudioFile(message, selectedCelebrity);
            if (audioFile != null) {
                scheduleMessageNotification(birthday, audioFile.getAbsolutePath());
                Toast.makeText(getContext(), "Audio wish scheduled for " + birthday.getName() + " on " + birthday.getDate(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to generate audio wish", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private interface WishCallback {
        void onWishGenerated(String wish);
        void onError(String error);
    }

    private void generateAIWish(String name, WishCallback callback) {
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No network connection available");
            requireActivity().runOnUiThread(() -> callback.onError("No internet connection"));
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        String prompt = "Write a short, heartfelt birthday wish (20-50 words) for a friend named " + name + ". Keep it positive, concise, and avoid advertisements.";

        JSONObject json = new JSONObject();
        try {
            json.put("contents", new JSONArray()
                    .put(new JSONObject()
                            .put("parts", new JSONArray()
                                    .put(new JSONObject()
                                            .put("text", prompt)))));
        } catch (JSONException e) {
            Log.e(TAG, "JSON creation failed: " + e.getMessage());
            callback.onError("JSON error");
            return;
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(GEMINI_API_URL + "?key=" + GEMINI_API_KEY)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        makeApiCallWithRetry(client, request, callback, name, prompt, 0);
    }

    private void makeApiCallWithRetry(OkHttpClient client, Request request, WishCallback callback, String name, String prompt, int attempt) {
        if (attempt >= MAX_RETRIES) {
            Log.e(TAG, "Max retries reached for API call");
            requireActivity().runOnUiThread(() -> callback.onError("Max retries exceeded"));
            return;
        }

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API call failed (attempt " + (attempt + 1) + "): " + e.getMessage());
                if (e.getMessage().contains("timeout") || e.getMessage().contains("network")) {
                    makeApiCallWithRetry(client, request, callback, name, prompt, attempt + 1);
                } else {
                    requireActivity().runOnUiThread(() -> callback.onError("Network error: " + e.getMessage()));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e(TAG, "API response unsuccessful (code: " + response.code() + "): " + errorBody);
                    if (response.code() == 503) {
                        makeApiCallWithRetry(client, request, callback, name, prompt, attempt + 1);
                    } else {
                        requireActivity().runOnUiThread(() -> callback.onError("API error: " + response.code() + " - " + errorBody));
                    }
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Raw API response: " + responseBody);

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String generatedText = jsonResponse
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    generatedText = generatedText.replaceAll("\n", " ").trim();
                    if (!generatedText.endsWith(".")) {
                        generatedText += ".";
                    }

                    if (isValidBirthdayWish(generatedText)) {
                        final String finalText = generatedText;
                        requireActivity().runOnUiThread(() -> callback.onWishGenerated(finalText));
                    } else {
                        Log.w(TAG, "Invalid or ad-like response: " + generatedText);
                        requireActivity().runOnUiThread(() -> callback.onWishGenerated(getFallbackWish(name)));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing failed: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> callback.onError("Parsing error: " + e.getMessage()));
                }
            }
        });
    }

    private boolean isValidBirthdayWish(String text) {
        String[] adKeywords = {"buy now", "click here", "subscribe", "free trial", "discount", "offer"};
        for (String keyword : adKeywords) {
            if (text.toLowerCase().contains(keyword)) {
                return false;
            }
        }
        return text.length() <= 200;
    }

    private String getFallbackWish(String name) {
        String[] wishes = {
                "Happy Birthday, " + name + "! Wishing you a day full of joy and love!",
                "Have an amazing birthday, " + name + "! May all your dreams come true!",
                "Happy Birthday, " + name + "! Here's to a fantastic year ahead!"
        };
        return wishes[new Random().nextInt(wishes.length)];
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private File generateAudioFile(String message, String celebrity) {
        if (tts == null) {
            Log.e(TAG, "TTS not initialized");
            return null;
        }

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

        File audioFile = new File(getContext().getCacheDir(), "birthday_wish_" + System.currentTimeMillis() + ".wav");
        CountDownLatch latch = new CountDownLatch(1);

        int result = tts.synthesizeToFile(message, null, audioFile, "birthday_wish");
        if (result == TextToSpeech.SUCCESS) {
            try {
                latch.await(5, TimeUnit.SECONDS);
                if (audioFile.exists() && audioFile.length() > 0) {
                    return audioFile;
                } else {
                    Log.e(TAG, "Audio file not created or empty");
                    return null;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for audio synthesis", e);
                return null;
            }
        } else {
            Log.e(TAG, "TTS synthesis failed with result: " + result);
            return null;
        }
    }

    private void scheduleMessageNotification(Birthday birthday, String audioFilePath) {
        Intent intent = new Intent(getContext(), MessageNotificationReceiver.class);
        intent.putExtra("name", birthday.getName());
        intent.putExtra("audioFilePath", audioFilePath);
        intent.putExtra("phoneNumber", birthday.getPhoneNumber());
        intent.putExtra("notificationId", (birthday.getName() + "_msg").hashCode());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(),
                (birthday.getName() + "_msg").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        String[] dateParts = birthday.getDate().split("-");
        int month = Integer.parseInt(dateParts[0]);
        int day = Integer.parseInt(dateParts[1]);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);

        if (currentMonth > month - 1) {
            year += 1;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (alarmManager != null) {
            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.d(TAG, "Audio wish scheduled for " + birthday.getName() + " at " + calendar.getTime());
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Permission denied for scheduling", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "SecurityException: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        adapter = null;
        birthdayList = null;
        databaseReference = null;
    }
}