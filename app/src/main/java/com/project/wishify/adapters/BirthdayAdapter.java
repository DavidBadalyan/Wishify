package com.project.wishify.adapters;

import static android.content.ContentValues.TAG;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.wishify.R;
import com.project.wishify.classes.Birthday;

import java.util.List;

public class BirthdayAdapter extends RecyclerView.Adapter<BirthdayAdapter.BirthdayViewHolder> {

    private List<Birthday> birthdayList;
    private BirthdayViewHolder holder;
    private int position;

    public BirthdayAdapter(List<Birthday> birthdayList) {
        this.birthdayList = birthdayList;
    }


    @NonNull
    @Override
    public BirthdayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_birthday, parent, false);
        return new BirthdayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BirthdayViewHolder holder, int position) {
        Log.d(TAG, "Binding data for position " + position + ": " + birthdayList.get(position).getName());

        holder.birthdayListContainer.removeAllViews();

        Birthday birthday = birthdayList.get(position);
        LinearLayout birthdayRow = new LinearLayout(holder.itemView.getContext());
        birthdayRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        birthdayRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvName = new TextView(holder.itemView.getContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvName.setText(birthday.getName());
        tvName.setTextSize(18);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);

        TextView tvDate = new TextView(holder.itemView.getContext());
        tvDate.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvDate.setText(birthday.getDate());
        tvDate.setTextSize(16);

        birthdayRow.addView(tvName);
        birthdayRow.addView(tvDate);

        holder.birthdayListContainer.addView(birthdayRow);
    }




    @Override
    public int getItemCount() {
        return birthdayList.size();
    }

    static class BirthdayViewHolder extends RecyclerView.ViewHolder {
        LinearLayout birthdayListContainer;

        public BirthdayViewHolder(@NonNull View itemView) {
            super(itemView);
            birthdayListContainer = itemView.findViewById(R.id.birthday_list_container);
        }
    }




}

