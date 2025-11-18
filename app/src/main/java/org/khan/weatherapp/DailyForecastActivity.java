package org.khan.weatherapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.khan.weatherapp.DailyAdapter;
import org.khan.weatherapp.databinding.ActivityDailyForecastBinding;
import org.khan.weatherapp.Day;

import java.util.List;

public class DailyForecastActivity extends AppCompatActivity {

    public static List<Day> daysList;
    public static String unitLetter = "F";   // Passed from MainActivity

    private ActivityDailyForecastBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDailyForecastBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup RecyclerView
        binding.rvDaily.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDaily.setAdapter(new DailyAdapter(this, daysList, unitLetter));
    }
}
