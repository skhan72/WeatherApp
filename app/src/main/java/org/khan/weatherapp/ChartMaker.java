package org.khan.weatherapp;

import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.khan.weatherapp.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Objects;

public class ChartMaker {

    private final MainActivity mainActivity;
    private final ActivityMainBinding binding;

    private static final SimpleDateFormat inputFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private static final SimpleDateFormat dayPrefixFormat =
            new SimpleDateFormat("yyyy-MM-dd ", Locale.US);

    public ChartMaker(MainActivity mainActivity, ActivityMainBinding binding) {
        this.mainActivity = mainActivity;
        this.binding = binding;
    }

    public void makeChart(TreeMap<String, Double> temperatureData,
                          long timeMillisIn) {

        LineChart chart = binding.chartTemp;

        setupChart(chart);
        setupXAxis(chart);
        setupYAxis(chart);
        setData(chart, temperatureData);

        chart.setVisibility(View.VISIBLE);
    }


    private void setData(LineChart mChart, TreeMap<String, Double> fullResults) {

        ArrayList<Entry> values = new ArrayList<>();

        for (String time : fullResults.keySet()) {
            try {
                // Create a full datetime string like: "2025-01-01 13:00:00"
                String fullDate = dayPrefixFormat.format(new Date()) + time;

                Date dateValue = inputFormat.parse(fullDate);
                long timeMs = Objects.requireNonNull(dateValue).getTime();

                float temp = Objects.requireNonNull(fullResults.get(time)).floatValue();

                values.add(new Entry(timeMs, temp));

            } catch (Exception e) {

            }
        }

        LineDataSet set = new LineDataSet(values, "Temperatures");
        set.setDrawIcons(false);
        set.setColor(Color.WHITE);
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(3f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(set);

        mChart.clear();
        mChart.setData(data);
        mChart.invalidate();

        // Add a vertical "current time" line
        LimitLine ll = new LimitLine(System.currentTimeMillis());
        ll.setLineWidth(1f);
        ll.setLineColor(Color.WHITE);

        mChart.getXAxis().removeAllLimitLines();
        mChart.getXAxis().addLimitLine(ll);
    }

    private void setupChart(LineChart mChart) {
        mChart.setDrawGridBackground(false);
        mChart.getDescription().setEnabled(false);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setAutoScaleMinMaxEnabled(true);
        mChart.getAxisRight().setEnabled(false);
        mChart.animateX(500);

        Legend legend = mChart.getLegend();
        legend.setEnabled(false);
    }

    private void setupXAxis(LineChart mChart) {
        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setGridColor(Color.parseColor("#DDFFFFFF"));
        xAxis.setValueFormatter(new MyCustomXAxisFormatter());
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(14);
        xAxis.setLabelRotationAngle(90);
        xAxis.setLabelCount(8, true);
    }

    private void setupYAxis(LineChart mChart) {
        YAxis yAxis = mChart.getAxisLeft();
        yAxis.removeAllLimitLines();
        yAxis.enableGridDashedLine(10f, 10f, 0f);
        yAxis.setValueFormatter(new MyCustomYAxisFormatter());
        yAxis.setGridColor(Color.parseColor("#DDFFFFFF"));

        int orientation = mainActivity.getResources().getConfiguration().orientation;
        yAxis.setLabelCount(orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 6, true);

        yAxis.setTextColor(Color.WHITE);
        yAxis.setTextSize(14);
    }

    public static class MyCustomXAxisFormatter extends ValueFormatter {
        private final SimpleDateFormat formatter =
                new SimpleDateFormat("h a", Locale.US);

        @Override
        public String getFormattedValue(float value) {
            return formatter.format(new Date((long) value)).toLowerCase();
        }
    }

    public static class MyCustomYAxisFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format(Locale.getDefault(), "%.0f°", value);
        }
    }
}
