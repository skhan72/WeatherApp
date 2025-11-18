package org.khan.weatherapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.khan.weatherapp.R;
import org.khan.weatherapp.databinding.ItemDayBinding;
import org.khan.weatherapp.Day;
import org.khan.weatherapp.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyAdapter extends RecyclerView.Adapter<DailyViewHolder> {

    private final Context context;
    private final List<Day> dayList;
    private final String unitLetter;

    public DailyAdapter(Context ctx, List<Day> days, String unitLetter) {
        this.context = ctx;
        this.dayList = days;
        this.unitLetter = unitLetter;
    }

    @NonNull
    @Override
    public DailyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDayBinding binding = ItemDayBinding.inflate(
                LayoutInflater.from(context), parent, false
        );
        return new DailyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyViewHolder holder, int position) {
        Day d = dayList.get(position);
        if (d == null) return;

        // --- Format date ---
        Date date = new Date(d.datetimeEpoch * 1000L);
        String dayName = new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
        String monthDay = new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);

        // --- Set text fields (null-safe) ---
        holder.binding.tvDayName.setText(dayName);
        holder.binding.tvDate.setText(monthDay);
        holder.binding.tvDescription.setText(d.description != null ? d.description : "");

        holder.binding.tvPrecip.setText("Precip: " + (int) d.precipprob + "%");
        holder.binding.tvUV.setText("UV Index: " + (int) d.uvindex);

        holder.binding.tvTempHigh.setText(String.format(Locale.getDefault(), "%.0f°%s", d.tempmax, unitLetter));
        holder.binding.tvTempLow.setText(String.format(Locale.getDefault(), "%.0f°%s", d.tempmin, unitLetter));

        // --- Icon (safe) ---
        String iconName = (d.icon == null) ? "" : d.icon.replace("-", "_");
        int iconId = 0;
        if (!iconName.isEmpty()) {
            iconId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        }
        if (iconId == 0) {
            // fallback icon
            iconId = R.drawable.ic_launcher_foreground;
        }
        holder.binding.ivDayIcon.setImageResource(iconId);

        // --- Apply dynamic temperature-based background gradient ---
        try {
            double avgTemp = (d.tempmax + d.tempmin) / 2.0;
            ColorMaker.setColorGradient(holder.binding.itemDayRoot, avgTemp, unitLetter);
        } catch (Exception ex) {
            // fail-safe: don't crash item binding if ColorMaker throws
            Log.w("DailyAdapter", "Failed to apply gradient", ex);
        }
    }


    @Override
    public int getItemCount() {
        return dayList.size();
    }


}
