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
import java.util.Base64;

import android.os.Looper;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
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
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
    private static final String GEMINI_API_KEY = "AIzaSyA134vyOKrkVZf6Q_gW05G_YmYrNYenDhY";
    private static final String D_ID_API_URL = "https://api.d-id.com/talks";
    private static final String D_ID_API_KEY = "ZGF2aWQxY29kemlsbGFAZ21haWwuY29t:vKlIdDhtwpCv0-HF-6vf_";
    private static final int MAX_RETRIES = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        adapter.attachSwipeHelper(recyclerView);
        EditText searchEditText = rootView.findViewById(R.id.searchEditText);
        if (searchEditText == null) {
            Log.e(TAG, "searchEditText is null in contacts_fragment layout");
            return rootView;
        }
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                } else {
                    Log.e(TAG, "Adapter is null when filtering");
                }
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
            Toast.makeText(getContext(), "Generating video...", Toast.LENGTH_SHORT).show();
            generateVideoFile(message, selectedCelebrity, videoFile -> {
                requireActivity().runOnUiThread(() -> {
                    if (videoFile != null) {
                        scheduleMessageNotification(birthday, videoFile.getAbsolutePath());
                        Toast.makeText(getContext(), "Video wish scheduled for " + birthday.getName() + " on " + birthday.getDate(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Failed to generate video wish", Toast.LENGTH_SHORT).show();
                    }
                });
            });
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

    private interface VideoCallback {
        void onVideoGenerated(File videoFile);
    }

    private void generateVideoFile(String message, String celebrity, VideoCallback callback) {
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No network connection available");
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
                callback.onVideoGenerated(null);
            });
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        // Debug flag to skip POST request and use hardcoded talkId (set to false for production)
        boolean useHardcodedTalkId = false;
        if (useHardcodedTalkId) {
            String talkId = "tlk_aV5Cz4GkA7T8n0XJtpCH8";
            Log.d(TAG, "Using hardcoded talkId: " + talkId);
            pollTalkStatus(client, talkId, callback);
            return;
        }

        // Map celebrities to different source_url images
        String sourceUrl;
        switch (celebrity.toLowerCase()) {
            case "noelle":
                sourceUrl = "https://create-images-results.d-id.com/api_docs/assets/noelle.jpeg";
                break;
            case "bull":
                sourceUrl = "https://create-images-results.d-id.com/DefaultPresenters/Bull_m/image.png";
                break;
            case "emma":
                sourceUrl = "https://create-images-results.d-id.com/DefaultPresenters/Emma_f/image.png";
                break;
            case "william":
                sourceUrl = "https://create-images-results.d-id.com/DefaultPresenters/William_m/image.png";
                break;
            case "santa":
                sourceUrl = "https://create-images-results.d-id.com/DefaultPresenters/FriendlySanta/image.jpg";
                break;
            case "sara":
                sourceUrl = "https://create-images-results.d-id.com/DefaultPresenters/Sara_f/image.png";
                break;
            default:
                sourceUrl = "https://create-images-results.d-id.com/api_docs/assets/noelle.jpeg";
                break;
        }
        Log.d(TAG, "Selected celebrity: " + celebrity + ", source_url: " + sourceUrl);

        JSONObject json = new JSONObject();
        try {
            json.put("script", new JSONObject()
                    .put("type", "text")
                    .put("input", message)
                    .put("provider", new JSONObject()
                            .put("type", "microsoft")
                            .put("voice_id", "en-US-JennyNeural")));
            json.put("source_url", sourceUrl);
            Log.d(TAG, "Video Request JSON: " + json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Request JSON failed: " + e.getMessage(), e);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Error preparing video request", Toast.LENGTH_SHORT).show();
                callback.onVideoGenerated(null);
            });
            return;
        }

        String[] credentials = D_ID_API_KEY.split(":");
        if (credentials.length != 2) {
            Log.e(TAG, "Invalid API key format: " + D_ID_API_KEY);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Invalid API key format", Toast.LENGTH_SHORT).show();
                callback.onVideoGenerated(null);
            });
            return;
        }
        String username = credentials[0];
        String password = credentials[1];
        String authString = username + ":" + password;
        Log.d(TAG, "Authorization Header: Basic " + authString);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(D_ID_API_URL)
                .post(body)
                .addHeader("Authorization", "Basic " + authString)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "D-ID API call failed: " + e.getMessage(), e);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Video generation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        callback.onVideoGenerated(null);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "No error body";
                            Log.e(TAG, "D-ID API failed (code: " + response.code() + "): " + errorBody);
                            handleApiError(response.code(), errorBody, callback);
                            return;
                        }

                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (responseBody.isEmpty()) {
                            Log.e(TAG, "Empty response body received");
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Empty response from server", Toast.LENGTH_SHORT).show();
                                callback.onVideoGenerated(null);
                            });
                            return;
                        }

                        Log.d(TAG, "D-ID API Response: " + responseBody);
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String talkId = jsonResponse.optString("id", "");
                        if (talkId.isEmpty()) {
                            Log.e(TAG, "No talkId found in response");
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Invalid video response: missing talkId", Toast.LENGTH_SHORT).show();
                                callback.onVideoGenerated(null);
                            });
                            return;
                        }
                        pollTalkStatus(client, talkId, callback);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing D-ID API response: " + e.getMessage(), e);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Error processing video response: " + e.toString(), Toast.LENGTH_LONG).show();
                            callback.onVideoGenerated(null);
                        });
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue D-ID API call: " + e.getMessage(), e);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Failed to start video generation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                callback.onVideoGenerated(null);
            });
        }
    }

    private void pollTalkStatus(OkHttpClient client, String talkId, VideoCallback callback) {
        String[] credentials = D_ID_API_KEY.split(":");
        if (credentials.length != 2) {
            Log.e(TAG, "Invalid API key format: " + D_ID_API_KEY);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Invalid API key format", Toast.LENGTH_SHORT).show();
                callback.onVideoGenerated(null);
            });
            return;
        }
        String username = credentials[0];
        String password = credentials[1];
        String authString = username + ":" + password;
        Log.d(TAG, "Authorization Header for pollTalkStatus: Basic " + authString);

        Request request = new Request.Builder()
                .url("https://api.d-id.com/talks/" + talkId)
                .get()
                .addHeader("Authorization", "Basic " + authString)
                .addHeader("Accept", "application/json")
                .build();

        try {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Talk status check failed: " + e.getMessage(), e);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Status check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        callback.onVideoGenerated(null);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        Log.d(TAG, "Talk Status HTTP Code: " + response.code() + ", Headers: " + response.headers());
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "No error body";
                            Log.e(TAG, "Talk status check failed (code: " + response.code() + "): " + errorBody);
                            handleApiError(response.code(), errorBody, callback);
                            return;
                        }

                        String responseBody = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "Talk Status Raw Response: " + responseBody);
                        if (responseBody.isEmpty()) {
                            Log.e(TAG, "Empty status response body received");
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Empty status response", Toast.LENGTH_SHORT).show();
                                callback.onVideoGenerated(null);
                            });
                            return;
                        }

                        // Sanitize response to handle null values in arrays
                        String sanitizedResponse = responseBody.replace(",null", "");
                        Log.d(TAG, "Talk Status Sanitized Response: " + sanitizedResponse);

                        JSONObject jsonResponse;
                        try {
                            jsonResponse = new JSONObject(sanitizedResponse);
                        } catch (JSONException e) {
                            Log.e(TAG, "Invalid JSON in status response: " + sanitizedResponse, e);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Invalid video status response: " + e.toString(), Toast.LENGTH_LONG).show();
                                callback.onVideoGenerated(null);
                            });
                            return;
                        }

                        String status = jsonResponse.optString("status", "");
                        if (status.isEmpty()) {
                            Log.e(TAG, "No status field in response: " + sanitizedResponse);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Invalid video status: missing status", Toast.LENGTH_SHORT).show();
                                callback.onVideoGenerated(null);
                            });
                            return;
                        }

                        Log.d(TAG, "Talk Status: " + status);
                        if ("done".equals(status)) {
                            String videoUrl = jsonResponse.optString("result_url", "");
                            if (videoUrl.isEmpty()) {
                                Log.e(TAG, "No result_url found in response: " + sanitizedResponse);
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Invalid video response: missing result_url", Toast.LENGTH_SHORT).show();
                                    callback.onVideoGenerated(null);
                                });
                                return;
                            }

                            Request videoRequest = new Request.Builder()
                                    .url(videoUrl)
                                    .get()
                                    .build();

                            client.newCall(videoRequest).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                    Log.e(TAG, "Video download failed: " + e.getMessage(), e);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Video download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        callback.onVideoGenerated(null);
                                    });
                                }

                                @Override
                                public void onResponse(@NonNull Call call, @NonNull Response videoResponse) throws IOException {
                                    try {
                                        if (!videoResponse.isSuccessful()) {
                                            String errorBody = videoResponse.body() != null ? videoResponse.body().string() : "No error body";
                                            Log.e(TAG, "Video download failed (code: " + videoResponse.code() + "): " + errorBody);
                                            requireActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "Video download failed: API error " + videoResponse.code(), Toast.LENGTH_SHORT).show();
                                                callback.onVideoGenerated(null);
                                            });
                                            return;
                                        }

                                        byte[] videoData = videoResponse.body() != null ? videoResponse.body().bytes() : null;
                                        if (videoData == null || videoData.length == 0) {
                                            Log.e(TAG, "Empty video data received");
                                            requireActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "No video data received", Toast.LENGTH_SHORT).show();
                                                callback.onVideoGenerated(null);
                                            });
                                            return;
                                        }

                                        File videoFile = new File(getContext().getCacheDir(), "birthday_wish_" + System.currentTimeMillis() + ".mp4");
                                        try (FileOutputStream out = new FileOutputStream(videoFile)) {
                                            out.write(videoData);
                                        }

                                        if (videoFile.exists() && videoFile.length() > 0) {
                                            Log.d(TAG, "Video file created: " + videoFile.getAbsolutePath());
                                            requireActivity().runOnUiThread(() -> callback.onVideoGenerated(videoFile));
                                        } else {
                                            Log.e(TAG, "Video file not created or empty");
                                            requireActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "Failed to save video file", Toast.LENGTH_SHORT).show();
                                                callback.onVideoGenerated(null);
                                            });
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error processing video download: " + e.getMessage(), e);
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Error saving video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            callback.onVideoGenerated(null);
                                        });
                                    } finally {
                                        if (videoResponse.body() != null) {
                                            videoResponse.body().close();
                                        }
                                    }
                                }
                            });
                        } else if (!"error".equals(status) && !"rejected".equals(status)) {
                            new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> pollTalkStatus(client, talkId, callback), 2000);
                        } else {
                            Log.e(TAG, "Talk failed with status: " + status);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Video generation failed: " + status, Toast.LENGTH_SHORT).show();
                                callback.onVideoGenerated(null);
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing talk status response: " + e.getMessage(), e);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Error parsing status response: " + e.toString(), Toast.LENGTH_LONG).show();
                            callback.onVideoGenerated(null);
                        });
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue talk status call: " + e.getMessage(), e);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Failed to check video status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                callback.onVideoGenerated(null);
            });
        }
    }

    private void handleApiError(int code, String errorBody, VideoCallback callback) {
        Log.e(TAG, "API Error - Code: " + code + ", Body: " + errorBody);
        requireActivity().runOnUiThread(() -> {
            switch (code) {
                case 400:
                    Toast.makeText(getContext(), "Bad request: " + errorBody, Toast.LENGTH_LONG).show();
                    break;
                case 401:
                    Toast.makeText(getContext(), "Authorization error: " + errorBody, Toast.LENGTH_LONG).show();
                    break;
                case 402:
                    Toast.makeText(getContext(), "Insufficient credits: " + errorBody, Toast.LENGTH_LONG).show();
                    break;
                case 403:
                    Toast.makeText(getContext(), "Permission error: " + errorBody, Toast.LENGTH_LONG).show();
                    break;
                case 451:
                    Toast.makeText(getContext(), "Moderation error: " + errorBody, Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(getContext(), "API error " + code + ": " + errorBody, Toast.LENGTH_LONG).show();
            }
            callback.onVideoGenerated(null);
        });
    }

    private void scheduleMessageNotification(Birthday birthday, String videoFilePath) {
        Intent intent = new Intent(getContext(), MessageNotificationReceiver.class);
        intent.putExtra("name", birthday.getName());
        intent.putExtra("videoFilePath", videoFilePath);
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
                Log.d(TAG, "Video wish scheduled for " + birthday.getName() + " at " + calendar.getTime());
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Permission denied for scheduling", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "SecurityException: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
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