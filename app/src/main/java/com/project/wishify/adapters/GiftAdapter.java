package com.project.wishify.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.wishify.R;

import java.util.List;

public class GiftAdapter extends RecyclerView.Adapter<GiftAdapter.GiftViewHolder> {

    private List<String> giftSuggestions;

    public GiftAdapter(List<String> giftSuggestions) {
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
        holder.giftTextView.setText(giftSuggestions.get(position));
    }

    @Override
    public int getItemCount() {
        return giftSuggestions.size();
    }

    static class GiftViewHolder extends RecyclerView.ViewHolder {
        TextView giftTextView;

        GiftViewHolder(@NonNull View itemView) {
            super(itemView);
            giftTextView = itemView.findViewById(R.id.giftTextView);
        }
    }
}