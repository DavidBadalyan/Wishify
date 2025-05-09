package com.project.wishify.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.wishify.R;
import com.project.wishify.adapters.GiftAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GiftFragment extends Fragment {

    private EditText preferencesEditText;
    private RecyclerView recyclerView;
    private GiftAdapter giftAdapter;
    private List<String> giftSuggestions;
    private static final String API_KEY = "AIzaSyA134vyOKrkVZf6Q_gW05G_YmYrNYenDhY";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gifts_fragment, container, false);

        preferencesEditText = view.findViewById(R.id.preferencesEditText);
        recyclerView = view.findViewById(R.id.recyclerView_gift_suggestions);

        giftSuggestions = new ArrayList<>();
        giftAdapter = new GiftAdapter(giftSuggestions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(giftAdapter);

        preferencesEditText.setOnEditorActionListener((v, actionId, event) -> {
            String preferences = preferencesEditText.getText().toString().trim();
            if (!preferences.isEmpty()) {
                fetchGiftSuggestions(preferences);
            } else {
                Toast.makeText(getContext(), "Please enter preferences", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        return view;
    }

    private void fetchGiftSuggestions(String preferences) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        String prompt = "Generate a list of 5 personalized gift ideas based on the following preferences: " + preferences + ". Each suggestion should be concise and suitable for a general audience.";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("contents", new JSONArray()
                    .put(new JSONObject()
                            .put("parts", new JSONArray()
                                    .put(new JSONObject()
                                            .put("text", prompt)))));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error preparing request", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(GEMINI_API_URL + "?key=" + API_KEY)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "API call failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String generatedText = jsonResponse
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        List<String> suggestions = parseGiftSuggestions(generatedText);
                        getActivity().runOnUiThread(() -> {
                            giftSuggestions.clear();
                            giftSuggestions.addAll(suggestions);
                            giftAdapter.notifyDataSetChanged();
                        });
                    } catch (Exception e) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "API error: " + response.message(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private List<String> parseGiftSuggestions(String text) {
        List<String> suggestions = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.trim().matches("\\d+\\..*")) {
                suggestions.add(line.trim().substring(line.indexOf(".") + 1).trim());
            }
        }
        return suggestions;
    }
}