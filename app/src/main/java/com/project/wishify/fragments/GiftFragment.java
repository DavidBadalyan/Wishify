package com.project.wishify.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.project.wishify.R;

public class GiftFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gifts_fragment, container, false);

        EditText preferencesEditText = view.findViewById(R.id.preferencesEditText);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView_gift_suggestions);

        preferencesEditText.setOnClickListener(v -> {
            String preferences = preferencesEditText.getText().toString().trim();
            if (!preferences.isEmpty()) {
                Toast.makeText(getContext(), "Preferences entered: " + preferences + " (ChatGPT logic to be added)", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}