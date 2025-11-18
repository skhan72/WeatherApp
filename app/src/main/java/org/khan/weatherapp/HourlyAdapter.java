package org.khan.weatherapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.khan.weatherapp.databinding.ItemHourBinding;

import java.util.List;

public class HourlyAdapter extends RecyclerView.Adapter<HourlyViewHolder> {

    private final List<Hour> hours;
    private final Context ctx;

    public HourlyAdapter(Context ctx, List<Hour> hours) {
        this.ctx = ctx;
        this.hours = hours;
    }

    @NonNull
    @Override
    public HourlyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHourBinding binding = ItemHourBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new HourlyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HourlyViewHolder holder, int position) {
        Hour h = hours.get(position);
        if (h == null) return;

        // --- Day label: "Today" or weekday ---
        String dayLabel;
        long nowEpoch = System.currentTimeMillis() / 1000L;
        if (isSameDayEpoch(nowEpoch, h.datetimeEpoch)) {
            dayLabel = "Today";
        } else {
            Date d = new Date(h.datetimeEpoch * 1000L);
            dayLabel = new SimpleDateFormat("EEE", Locale.getDefault()).format(d); // Mon, Tue, ...
        }
        holder.binding.tvDayLabel.setText(dayLabel);

        // Time label: assume caller set friendly label (e.g., "1 PM"); fallback to raw datetime
        holder.binding.tvHourTime.setText(h.datetime != null ? h.datetime : (h.datetimeEpoch > 0 ? String.valueOf(h.datetimeEpoch) : ""));

        // Temp (no decimals)
        holder.binding.tvHourTemp.setText(String.format(Locale.getDefault(), "%.0f°", h.temp));

        // Description
        holder.binding.tvHourDesc.setText(h.conditions != null ? h.conditions : "");

        // Load icon by name (replace dashes with underscores)
        String iconName = (h.icon == null || h.icon.isEmpty()) ? "ic_launcher" : h.icon.replace("-", "_");
        int iconRes = ctx.getResources().getIdentifier(iconName, "drawable", ctx.getPackageName());
        if (iconRes == 0) {
            // fallback to mipmap launcher if drawable missing
            iconRes = ctx.getResources().getIdentifier("ic_launcher", "mipmap", ctx.getPackageName());
        }

        Glide.with(ctx)
                .load(iconRes)
                .into(holder.binding.ivHourIcon);
    }

    @Override
    public int getItemCount() {
        return hours == null ? 0 : hours.size();
    }

    // helper: compare two epoch-second timestamps for same calendar day in the device locale/timezone
    private boolean isSameDayEpoch(long epochSecondsA, long epochSecondsB) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(epochSecondsA * 1000L);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(epochSecondsB * 1000L);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
