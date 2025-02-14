package com.project.wishify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CalendarFragment extends Fragment {

    private TextView birthdayNameTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.calendar_fragment, container, false);

        CalendarView calendarView = rootView.findViewById(R.id.calendarView);
        birthdayNameTextView = rootView.findViewById(R.id.birthdayName);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String birthdayMessage = "";

            if (month == 1 && dayOfMonth == 10) {
                birthdayMessage = "John Doe's Birthday!";
            } else if (month == 2 && dayOfMonth == 3) {
                birthdayMessage = "Jane Smith's Birthday!";
            } else if (month == 3 && dayOfMonth == 15) {
                birthdayMessage = "Michael Brown's Birthday!";
            } else if (month == 4 && dayOfMonth == 8) {
                birthdayMessage = "Emma Wilson's Birthday!";
            } else if (month == 5 && dayOfMonth == 12) {
                birthdayMessage = "David Lee's Birthday!";
            } else {
                birthdayMessage = "No events today.";
            }

            birthdayNameTextView.setText(birthdayMessage);
        });

        return rootView;
    }
}

