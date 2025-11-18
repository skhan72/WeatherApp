package org.khan.weatherapp;

import androidx.recyclerview.widget.RecyclerView;

import org.khan.weatherapp.databinding.ItemDayBinding;

public class DailyViewHolder extends RecyclerView.ViewHolder {
    public ItemDayBinding binding;

    public DailyViewHolder(ItemDayBinding b) {
        super(b.getRoot());
        this.binding = b;
    }
}
