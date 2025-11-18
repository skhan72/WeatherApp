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

        // Format date components
        Date date = new Date(d.datetimeEpoch * 1000L);

        // Full weekday (Saturday, Sunday...)
        String fullDayName = new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);

        // MM/dd format
        String monthDay = new SimpleDateFormat("MM/dd", Locale.getDefault()).format(date);

        // Final combined label "Saturday, 10/12"
        String fullLabel = fullDayName + ", " + monthDay;
        holder.binding.tvDayDate.setText(fullLabel);


        holder.binding.tvDescription.setText(d.description != null ? d.description : "");

        holder.binding.tvPrecip.setText("(" + (int) d.precipprob + "%" + " precip.)");
        holder.binding.tvUV.setText("UV Index: " + (int) d.uvindex);

        // High/Low: 71°F/60°F
        String mergedTemp = String.format(Locale.getDefault(), "%.0f°%s/%.0f°%s",
                d.tempmax, unitLetter,
                d.tempmin, unitLetter
        );

        holder.binding.tvTempHigh.setText(mergedTemp);

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

        // temperature-based background gradient
        try {
            double avgTemp = (d.tempmax + d.tempmin) / 2.0;
            ColorMaker.setColorGradient(holder.binding.itemDayRoot, avgTemp, unitLetter);
        } catch (Exception ex) {
            // fail-safe: don't crash item binding if ColorMaker throws
            Log.w("DailyAdapter", "Failed to apply gradient", ex);
        }

        try {
            fillTemperaturePeriods(holder, d);
        } catch (Exception ex) {
            Log.w("DailyAdapter", "Failed to fill period temps", ex);
        }
    }

    @Override
    public int getItemCount() {
        return dayList == null ? 0 : dayList.size();
    }

    private void fillTemperaturePeriods(DailyViewHolder holder, Day d) {

        Double tM = null, tA = null, tE = null, tN = null;

        if (d.hours != null && (!allTempsAvailable(tM, tA, tE, tN))) {

            for (Hour h : d.hours) {
                if (h == null) continue;
                int hour = getHourOfDay(h.datetimeEpoch);

                if (hour >= 6 && hour <= 11 && tM == null)
                    tM = h.temp;

                if (hour >= 12 && hour <= 16 && tA == null)
                    tA = h.temp;

                if (hour >= 17 && hour <= 20 && tE == null)
                    tE = h.temp;

                if ((hour >= 21 && hour <= 23 || hour >= 0 && hour <= 5) && tN == null)
                    tN = h.temp;
            }
        }

        if (tM == null) tM = d.temp;      // Morning fallback
        if (tA == null) tA = d.tempmax;   // Afternoon fallback
        if (tE == null) tE = d.temp;      // Evening fallback
        if (tN == null) tN = d.tempmin;   // Night fallback

        holder.binding.tvTempMorning.setText(String.format(Locale.getDefault(), "%.0f°%s", tM, unitLetter));
        holder.binding.tvTempAfternoon.setText(String.format(Locale.getDefault(), "%.0f°%s", tA, unitLetter));
        holder.binding.tvTempEvening.setText(String.format(Locale.getDefault(), "%.0f°%s", tE, unitLetter));
        holder.binding.tvTempNight.setText(String.format(Locale.getDefault(), "%.0f°%s", tN, unitLetter));
    }

    private boolean allTempsAvailable(Double a, Double b, Double c, Double d) {
        return a != null && b != null && c != null && d != null;
    }

    private int getHourOfDay(long epochSec) {
        return Integer.parseInt(new SimpleDateFormat("H", Locale.getDefault())
                .format(new Date(epochSec * 1000L)));
    }


     //Simple field-existence guard. Returns true if Day class has a non-null field by name.
    private boolean hasField(Day d, String fieldName) {
        try {

            java.lang.reflect.Field f = d.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object val = f.get(d);
            return val != null;
        } catch (Exception ex) {
            return false;
        }
    }
}
