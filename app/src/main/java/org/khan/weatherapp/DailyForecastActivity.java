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

    // city name passed from MainActivity (e.g., "Chicago")
    public static String cityName = "";

    private ActivityDailyForecastBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDailyForecastBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the page title to "<City> 15-Day Forecast"
        String title;
        if (cityName != null && !cityName.trim().isEmpty()) {
            // remove any trailing comma or extra text
            String cleanCity = cityName.split(",")[0].trim();
            title = cleanCity + " 15-Day Forecast";
        } else {
            title = "15-Day Forecast";
        }
        binding.cityForecastPageTitle.setText(title);

        // Setup RecyclerView
        binding.rvDaily.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDaily.setAdapter(new DailyAdapter(this, daysList, unitLetter));
    }
}
