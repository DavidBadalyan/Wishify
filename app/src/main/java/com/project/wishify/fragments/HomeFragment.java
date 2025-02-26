package com.project.wishify.fragments;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.wishify.R;
import com.project.wishify.adapters.BirthdayAdapter;
import com.project.wishify.classes.Birthday;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private BirthdayAdapter adapter;
    private List<Birthday> birthdayList;
    private DatabaseReference databaseReference;

    private void fetchBirthdays() {
        databaseReference = FirebaseDatabase.getInstance().getReference("birthdays");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                birthdayList.clear();
                Calendar today = Calendar.getInstance();
                today.setTime(new Date());

                List<Birthday> allBirthdays = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Birthday birthday = dataSnapshot.getValue(Birthday.class);
                    if (birthday != null) {
                        allBirthdays.add(birthday);
                    }
                }

                Collections.sort(allBirthdays, new Comparator<Birthday>() {
                    @Override
                    public int compare(Birthday b1, Birthday b2) {
                        return compareDates(b1.getDate(), b2.getDate(), today);
                    }
                });

                birthdayList.addAll(allBirthdays.subList(0, Math.min(7, allBirthdays.size())));
                Log.d(TAG, "Fetched and filtered birthdays: " + birthdayList.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private int compareDates(String date1, String date2, Calendar today) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());

        try {
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();

            Date d1 = sdf.parse(date1);
            Date d2 = sdf.parse(date2);

            cal1.setTime(d1);
            cal2.setTime(d2);

            int currentYear = today.get(Calendar.YEAR);
            cal1.set(Calendar.YEAR, currentYear);
            cal2.set(Calendar.YEAR, currentYear);

            if (cal1.before(today) && !(today.get(Calendar.MONTH) == cal1.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) == cal1.get(Calendar.DAY_OF_MONTH))) {
                cal1.add(Calendar.YEAR, 1);
            }
            if (cal2.before(today) && !(today.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH))) {
                cal2.add(Calendar.YEAR, 1);
            }

            return cal1.compareTo(cal2);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.home_fragment, container, false);

        recyclerView = rootView.findViewById(R.id.recyclerView_birthdays);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        birthdayList = new ArrayList<>();
        adapter = new BirthdayAdapter(birthdayList);
        recyclerView.setAdapter(adapter);

        fetchBirthdays();

        return rootView;
    }
}
