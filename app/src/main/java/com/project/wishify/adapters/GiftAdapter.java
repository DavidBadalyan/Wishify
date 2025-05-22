package com.project.wishify.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.wishify.R;
import com.project.wishify.classes.GiftSuggestion;

import java.util.List;

public class GiftAdapter extends RecyclerView.Adapter<GiftAdapter.GiftViewHolder> {

    private List<GiftSuggestion> giftSuggestions;

    public GiftAdapter(List<GiftSuggestion> giftSuggestions) {
        this.giftSuggestions = giftSuggestions;
    }

    @NonNull
    @Override
    public GiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gift_item, parent, false);
        return new GiftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GiftViewHolder holder, int position) {
        GiftSuggestion suggestion = giftSuggestions.get(position);
        holder.giftTitleTextView.setText(suggestion.getTitle());
        holder.giftDescriptionTextView.setText(suggestion.getDescription());
    }

    @Override
    public int getItemCount() {
        return giftSuggestions.size();
    }

    static class GiftViewHolder extends RecyclerView.ViewHolder {
        TextView giftTitleTextView;
        TextView giftDescriptionTextView;

        GiftViewHolder(@NonNull View itemView) {
            super(itemView);
            giftTitleTextView = itemView.findViewById(R.id.giftTitleTextView);
            giftDescriptionTextView = itemView.findViewById(R.id.giftDescriptionTextView);
        }
    }
}