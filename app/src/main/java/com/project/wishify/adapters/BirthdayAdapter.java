package com.project.wishify.adapters;

import android.graphics.Color;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BirthdayAdapter extends RecyclerView.Adapter<BirthdayAdapter.BirthdayViewHolder> {

    private List<Birthday> birthdayList;
    private static final String TAG = BirthdayAdapter.class.getSimpleName();

    public BirthdayAdapter(List<Birthday> birthdayList) {
        this.birthdayList = birthdayList;
    }

    @NonNull
    @Override
    public BirthdayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_birthday, parent, false);
        return new BirthdayViewHolder(view);
    }

    public String getEmoji() {
        String[] emojis = {"🎉", "🎈", "🎆", "🎊", "🎀", "🎁", "🎂"};
        int randomIndex = (int) (Math.random() * emojis.length);
        return emojis[randomIndex];
    }

    @Override
    public void onBindViewHolder(@NonNull BirthdayViewHolder holder, int position) {
        Log.d(TAG, "Binding data for position " + position + ": " + birthdayList.get(position).getName());

        Birthday birthday = birthdayList.get(position);
        holder.tvIcon.setText(getEmoji());
        holder.tvName.setText(birthday.getName());

        String formattedDate = formatDate(birthday.getDate());
        holder.tvDate.setText(formattedDate);
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || !dateStr.matches("\\d{2}-\\d{2}")) {
            return dateStr;
        }

        SimpleDateFormat inputFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        today.setTime(new Date());

        try {
            Date date = inputFormat.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            int currentYear = today.get(Calendar.YEAR);
            cal.set(Calendar.YEAR, currentYear);

            if (cal.before(today) && !(today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH))) {
                cal.add(Calendar.YEAR, 1);
            }

            if (today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH)) {
                return "Today";
            }

            return outputFormat.format(cal.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return dateStr;
        }
    }

    @Override
    public int getItemCount() {
        return birthdayList.size();
    }

    static class BirthdayViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon;
        TextView tvName;
        TextView tvDate;

        public BirthdayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.icon);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}