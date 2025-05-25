package com.project.wishify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.project.wishify.R;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] celebrities;
    private final String[] avatarUrls;

    public CustomSpinnerAdapter(@NonNull Context context, String[] celebrities, String[] avatarUrls) {
        super(context, R.layout.spinner_item_celebrity, celebrities);
        this.context = context;
        this.celebrities = celebrities;
        this.avatarUrls = avatarUrls;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, true);
    }

    private View createView(int position, @Nullable View convertView, @NonNull ViewGroup parent, boolean isDropDown) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.spinner_item_celebrity, parent, false);
        }

        ImageView avatarImage = view.findViewById(R.id.avatar_image);
        TextView celebrityName = view.findViewById(R.id.celebrity_name);

        if (position == 0) {
            // "Select an avatar" item
            avatarImage.setVisibility(View.GONE);
            celebrityName.setText(celebrities[position]);
        } else {
            avatarImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(avatarUrls[position - 1])
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error)
                    .into(avatarImage);
            celebrityName.setText(celebrities[position]);
        }

        return view;
    }

    @Override
    public int getCount() {
        return celebrities.length;
    }
}