package com.project.wishify.fragments;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.wishify.R;
import com.project.wishify.adapters.ContactsAdapter;
import com.project.wishify.classes.Birthday;
import com.project.wishify.workers.MessageSenderWorker;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ContactsFragment extends Fragment implements ContactsAdapter.OnCustomizeClickListener {
    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private List<Birthday> birthdayList;
    private DatabaseReference databaseReference;
    private static final String PREFS_NAME = "MessageSchedules";
    private static final String MESSAGE_KEY = "message_";
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Birthday pendingBirthday;
    private String pendingMessage;
    private String pendingAppName;
    private String pendingCelebrity;

    private void fetchBirthdays() {
        databaseReference = FirebaseDatabase.getInstance().getReference("birthdays");
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
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show();
                        if (pendingBirthday != null && pendingMessage != null && pendingAppName != null && pendingCelebrity != null) {
                            scheduleMessage(pendingBirthday, pendingMessage, pendingAppName, pendingCelebrity);
                            Toast.makeText(requireContext(), "Message scheduled for " + pendingBirthday.getName() + " on " + pendingBirthday.getDate(), Toast.LENGTH_SHORT).show();
                            pendingBirthday = null;
                            pendingMessage = null;
                            pendingAppName = null;
                            pendingCelebrity = null;
                        }
                    } else {
                        Toast.makeText(requireContext(), "Notification permission denied. Cannot schedule message.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {Log.d(TAG, "onCreateView: Initializing ContactsFragment");
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

        AutoCompleteTextView searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchAutoComplete != null) {
            searchAutoComplete.setBackgroundResource(R.drawable.rounded_search_background);
        } else {
            Log.w(TAG, "searchAutoComplete is null in SearchView");
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

        Log.d(TAG, "onCreateView: ContactsFragment setup complete");
        return rootView;
    }

    @Override
    public void onCustomizeClicked(Birthday birthday) {
        showScheduleMessageDialog(birthday);
    }

    private String generateAIMessage(String name) {
        return "Happy Birthday, " + name + "! Wishing you a fantastic day filled with joy, laughter, and all your favorite things. Have an amazing year ahead!";
    }

    private void showScheduleMessageDialog(Birthday birthday) {
        if (birthday == null || birthday.getName() == null || birthday.getPhoneNumber() == null) {
            Toast.makeText(requireContext(), "Invalid contact data", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_send_message_dialog_no_spinner, null);
        builder.setView(dialogView);

        EditText etMessage = dialogView.findViewById(R.id.et_message);
        Spinner spinnerCelebrity = dialogView.findViewById(R.id.spinner_celebrity);
        RadioGroup radioGroupApps = dialogView.findViewById(R.id.radio_group_apps);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button sendButton = dialogView.findViewById(R.id.send_button);

        String aiMessage = generateAIMessage(birthday.getName());
        etMessage.setText(aiMessage);

        ArrayAdapter<CharSequence> celebrityAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.celebrity_list,
                android.R.layout.simple_spinner_item
        );
        celebrityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCelebrity.setAdapter(celebrityAdapter);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        sendButton.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            String selectedCelebrity = spinnerCelebrity.getSelectedItem().toString();
            int selectedAppId = radioGroupApps.getCheckedRadioButtonId();

            if (message.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedCelebrity.equals("Select a celebrity")) {
                Toast.makeText(requireContext(), "Please select a celebrity", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedAppId == -1) {
                Toast.makeText(requireContext(), "Please select a messaging app", Toast.LENGTH_SHORT).show();
                return;
            }

            String appName;
            if (selectedAppId == R.id.radio_whatsapp) {
                appName = "whatsapp";
            } else if (selectedAppId == R.id.radio_telegram) {
                appName = "telegram";
            } else if (selectedAppId == R.id.radio_viber) {
                appName = "viber";
            } else {
                Toast.makeText(requireContext(), "Invalid app selection", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    pendingBirthday = birthday;
                    pendingMessage = message;
                    pendingAppName = appName;
                    pendingCelebrity = selectedCelebrity;
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    scheduleMessage(birthday, message, appName, selectedCelebrity);
                    Toast.makeText(requireContext(), "Message scheduled for " + birthday.getName() + " on " + birthday.getDate(), Toast.LENGTH_SHORT).show();
                }
            } else {
                scheduleMessage(birthday, message, appName, selectedCelebrity);
                Toast.makeText(requireContext(), "Message scheduled for " + birthday.getName() + " on " + birthday.getDate(), Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void scheduleMessage(Birthday birthday, String message, String appName, String celebrity) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = MESSAGE_KEY + birthday.getName() + "_" + birthday.getDate();
        String value = birthday.getPhoneNumber() + "|" + message + "|" + appName + "|" + celebrity;
        editor.putString(key, value);
        editor.apply();
        Log.d(TAG, "Scheduled message for " + birthday.getName() + " on " + birthday.getDate() + " via " + appName + " with celebrity " + celebrity);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());

        try {
            calendar.setTime(sdf.parse(birthday.getDate()));
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            calendar.set(Calendar.YEAR, currentYear);

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.YEAR, 1);
            }

            calendar.set(Calendar.HOUR_OF_DAY, 22);
            calendar.set(Calendar.MINUTE, 20);
            calendar.set(Calendar.SECOND, 0);

//            long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
//            if (delay < 0) {
//                delay += TimeUnit.DAYS.toMillis(365);
//            }

            long delay = 60_000; // 1 minute for testing (originally: calendar.getTimeInMillis() - System.currentTimeMillis())
            if (delay < 0) {
                delay = 60_000; // Ensure delay is positive for testing
            }

            Data inputData = new Data.Builder()
                    .putString(MessageSenderWorker.KEY_NAME, birthday.getName())
                    .putString(MessageSenderWorker.KEY_PHONE, birthday.getPhoneNumber())
                    .putString(MessageSenderWorker.KEY_MESSAGE, message)
                    .putString(MessageSenderWorker.KEY_APP, appName)
                    .putString(MessageSenderWorker.KEY_CELEBRITY, celebrity)
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MessageSenderWorker.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(requireContext()).enqueue(workRequest);
            Log.d(TAG, "WorkManager scheduled for " + birthday.getName() + " with delay " + delay + "ms");
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to parse date for " + birthday.getName() + ": " + e.getMessage());
            Toast.makeText(requireContext(), "Failed to schedule message: Invalid date format", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        adapter = null;
        birthdayList = null;
        databaseReference = null;
        Log.d(TAG, "onDestroyView: Cleaned up ContactsFragment resources");
    }
}