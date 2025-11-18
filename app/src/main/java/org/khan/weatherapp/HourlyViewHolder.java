package org.khan.weatherapp;

import androidx.recyclerview.widget.RecyclerView;

import org.khan.weatherapp.databinding.ItemHourBinding;

public class HourlyViewHolder extends RecyclerView.ViewHolder {
    public final ItemHourBinding binding;

    public HourlyViewHolder(ItemHourBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}