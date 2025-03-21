package com.project.wishify.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.project.wishify.R;
import com.project.wishify.classes.Birthday;

import java.util.ArrayList;
import java.util.List;

public class DialogUtils {

    private static final String TAG = "DialogUtils";

    public static void showSendMessageDialog(Context context, List<Birthday> birthdayList) {
        if (birthdayList == null || birthdayList.isEmpty()) {
            Toast.makeText(context, "No contacts available to send a message", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_send_message_dialog, null);
        builder.setView(dialogView);

        Spinner spinnerContacts = dialogView.findViewById(R.id.spinner_contacts);
        EditText etMessage = dialogView.findViewById(R.id.et_message);
        RadioGroup radioGroupApps = dialogView.findViewById(R.id.radio_group_apps);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button sendButton = dialogView.findViewById(R.id.send_button);

        List<String> contactNames = new ArrayList<>();
        for (Birthday birthday : birthdayList) {
            if (birthday != null && birthday.getName() != null) {
                contactNames.add(birthday.getName());
            }
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, contactNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContacts.setAdapter(spinnerAdapter);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        sendButton.setOnClickListener(v -> {
            int selectedContactPosition = spinnerContacts.getSelectedItemPosition();
            String message = etMessage.getText().toString().trim();
            int selectedAppId = radioGroupApps.getCheckedRadioButtonId();

            if (message.isEmpty()) {
                Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedAppId == -1) {
                Toast.makeText(context, "Please select a messaging app", Toast.LENGTH_SHORT).show();
                return;
            }

            Birthday selectedBirthday = birthdayList.get(selectedContactPosition);
            if (selectedBirthday == null) {
                Toast.makeText(context, "Selected contact is invalid", Toast.LENGTH_SHORT).show();
                return;
            }

            String phoneNumber = selectedBirthday.getPhoneNumber();
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(context, "Phone number not available for " + selectedBirthday.getName(), Toast.LENGTH_SHORT).show();
                return;
            }

            String appScheme;
            if (selectedAppId == R.id.radio_whatsapp) {
                appScheme = "whatsapp://send?phone=" + phoneNumber + "&text=" + Uri.encode(message);
            } else if (selectedAppId == R.id.radio_telegram) {
                appScheme = "tg://msg?to=" + phoneNumber + "&text=" + Uri.encode(message);
            } else if (selectedAppId == R.id.radio_viber) {
                appScheme = "viber://chat?number=" + phoneNumber + "&draft=" + Uri.encode(message);
            } else {
                Toast.makeText(context, "Invalid app selection", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(appScheme));
                context.startActivity(intent);
                Toast.makeText(context, "Opening " + ((RadioButton) dialogView.findViewById(selectedAppId)).getText() + " to send message", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "Failed to open app. Please ensure it is installed.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error opening app: " + e.getMessage());
            }

            dialog.dismiss();
        });

        dialog.show();
    }
}