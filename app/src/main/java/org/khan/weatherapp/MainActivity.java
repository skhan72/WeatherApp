package org.khan.weatherapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.khan.weatherapp.databinding.ActivityMainBinding;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private String unitGroup = "us";
    private double currentTemp = 68.0;
    private final List<Day> days = new ArrayList<>();
    private final List<Hour> hourlyList = new ArrayList<>();
    private HourlyAdapter hourlyAdapter;

    // store last location query so toggling units reuses it (city name or "lat,lon")
    private String lastQueriedLocation = null;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                    fetchLocationAndWeather();
                } else {
                    // fallback default
                    fetchWeatherByCity("Chicago, IL");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d("API_TEST", "KEY=" + BuildConfig.VC_API_KEY);

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // hourly RecyclerView
            hourlyAdapter = new HourlyAdapter(this, hourlyList);
            binding.rvHourly.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            binding.rvHourly.setAdapter(hourlyAdapter);

            // ensure unit icon initial state
            int initIcon = getDrawableResourceByName(unitGroup.equals("us") ? "units_f" : "units_c");
            if (initIcon != 0) binding.ivToggleUnit.setImageResource(initIcon);

            // Icon bar listeners
            binding.ivToggleUnit.setOnClickListener(v -> {
                // flip units
                unitGroup = unitGroup.equals("us") ? "metric" : "us";

                // update toggle icon immediately (safe)
                int iconRes = getDrawableResourceByName(unitGroup.equals("us") ? "units_f" : "units_c");
                if (iconRes != 0) binding.ivToggleUnit.setImageResource(iconRes);

                // re-fetch weather for the last queried location (fallback to Chicago)
                String toQuery = (lastQueriedLocation != null && !lastQueriedLocation.isEmpty())
                        ? lastQueriedLocation
                        : "Chicago, IL";
                fetchWeatherByCity(toQuery);
            });

            binding.ivSetLocation.setOnClickListener(v -> {
                final android.widget.EditText input = new android.widget.EditText(this);
                new AlertDialog.Builder(this)
                        .setTitle("Set Location")
                        .setMessage("Enter City (e.g., Chicago, IL) or latitude,longitude")
                        .setView(input)
                        .setPositiveButton("OK", (d, w) -> {
                            String text = input.getText().toString().trim();
                            if (!text.isEmpty()) fetchWeatherByCity(text);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            binding.iv15Day.setOnClickListener(v -> {
                if (days.isEmpty()) return;

                DailyForecastActivity.daysList = days;  // send 15-day data
                DailyForecastActivity.unitLetter = unitGroup.equals("us") ? "F" : "C";

                Intent intent = new Intent(MainActivity.this, DailyForecastActivity.class);
                startActivity(intent);
            });

            binding.ivShare.setOnClickListener(v -> {
                try {
                    String text = buildShareText();

                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, text);

                    startActivity(Intent.createChooser(i, "Share Weather"));

                } catch (Exception ex) {
                    Toast.makeText(this, "Unable to share right now", Toast.LENGTH_SHORT).show();
                }
            });


            binding.ivReset.setOnClickListener(v -> {
                // explicit reset should try location fetch
                fetchLocationAndWeather();
            });

            binding.ivMap.setOnClickListener(v -> {
                Toast.makeText(this, "Map Feature Not Implemented", Toast.LENGTH_SHORT).show();
            });

            checkPermissionsAndFetch();
        } catch (Exception ex) {
            // prevent crash during onCreate — log and show a simple alert
            Log.e("MainActivity", "onCreate error", ex);
            new AlertDialog.Builder(this)
                    .setTitle("Startup Error")
                    .setMessage("Unexpected startup error: " + ex.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void checkPermissionsAndFetch() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            fetchLocationAndWeather();
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private String buildShareText() {

        // --- Extract city + optional ZIP code from header ---
        String header = binding.tvHeader.getText().toString();
        // header looks like: "Chicago, Sat Oct 12 12:00 PM"
        String city = header.contains(",") ? header.split(",")[0].trim() : header;

        // Try to extract ZIP from resolvedAddress if present
        String zip = "";
        if (lastQueriedLocation != null && lastQueriedLocation.matches(".*\\d{5}.*")) {
            // if user typed something like “Chicago 60604” or “60604”
            zip = lastQueriedLocation.replaceAll(".*?(\\d{5}).*", "$1");
        }

        // fallback ZIP from days list if VisualCrossing responded with “City, ST, ZIP”
        try {
            JSONObject root = new JSONObject(ApiManager.lastJsonResponse);
            String resolved = root.optString("resolvedAddress", "");
            if (resolved.matches(".*\\d{5}.*")) {
                zip = resolved.replaceAll(".*?(\\d{5}).*", "$1");
            }
        } catch (Exception ignored) {}

        String cityLine = zip.isEmpty() ? city : city + " (" + zip + ")";

        // --- Today's Day object ---
        Day today = days.isEmpty() ? null : days.get(0);

        // --- Build fields safely ---
        String nowTemp = binding.tvTemp.getText().toString();
        String nowFeels = binding.tvFeelsLike.getText().toString().replace("Feels like ", "");
        String nowCond = binding.tvConditionDesc.getText().toString();

        String humidity = binding.tvHumidity.getText().toString().replace("Humidity: ", "");
        String uv = binding.tvUV.getText().toString().replace("UV Index: ", "");
        String sunrise = binding.tvSunrise.getText().toString().replace("Sunrise: ", "");
        String sunset = binding.tvSunset.getText().toString().replace("Sunset: ", "");

        String visibility = binding.tvVisibility.getText().toString().replace("Visibility: ", "");

        // wind line: “Winds: S at 0.6 mph gusting to 15”
        String windLine = binding.tvWind.getText().toString()
                .replace("Winds: ", "");

        String forecastLine = "";
        if (today != null) {
            forecastLine = String.format(
                    Locale.getDefault(),
                    "%s with a high of %.1f°%s and a low of %.1f°%s.",
                    today.description != null ? today.description : "Forecast",
                    today.tempmax,
                    unitGroup.equals("us") ? "F" : "C",
                    today.tempmin,
                    unitGroup.equals("us") ? "F" : "C"
            );
        }

        return "Weather for " + cityLine + ":\n" +
                "Forecast: " + forecastLine + "\n" +
                "Now: " + nowTemp + ", " + nowCond + " (Feels like: " + nowFeels + ")\n" +
                "Humidity: " + humidity + "\n" +
                "Winds: " + windLine + "\n" +
                "UV Index: " + uv + "\n" +
                "Sunrise: " + sunrise + "\n" +
                "Sunset: " + sunset + "\n" +
                "Visibility: " + visibility + "\n";
    }


    private void fetchLocationAndWeather() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    final double lat = location.getLatitude();
                    final double lon = location.getLongitude();

                    // Run reverse-geocoding off the UI thread
                    new Thread(() -> {
                        String cityName = reverseGeocodeToCity(lat, lon);

                        // If reverse geocode fails, fallback to "lat,lon" format
                        final String query = (cityName == null || cityName.isEmpty() || "Unknown".equals(cityName))
                                ? (lat + "," + lon)
                                : cityName;

                        // Call fetch on main thread
                        runOnUiThread(() -> fetchWeatherByCity(query));
                    }).start();

                } else {
                    // No location available — fallback to default city
                    fetchWeatherByCity("Chicago, IL");
                }
            }).addOnFailureListener(e -> fetchWeatherByCity("Chicago, IL"));
        } catch (SecurityException se) {
            fetchWeatherByCity("Chicago, IL");
        } catch (Exception ex) {
            Log.w("MainActivity", "fetchLocationAndWeather unexpected", ex);
            fetchWeatherByCity("Chicago, IL");
        }
    }

    private void fetchWeatherByCity(String location) {
        try {
            if (location == null) location = "Chicago, IL";
            // remember the last requested location so toggling units will re-query the same place
            lastQueriedLocation = location;

            String apiKey = BuildConfig.VC_API_KEY;
            if (apiKey == null || apiKey.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("API Key Missing")
                        .setMessage("Please set VISUAL_CROSSING_API_KEY in local.properties")
                        .setPositiveButton("OK", null).show();
                return;
            }

            ApiManager.fetchWeatherForLocation(this, location, apiKey, unitGroup, new ApiManager.ApiCallback() {
                @Override
                public void onSuccess(String json) {
                    runOnUiThread(() -> parseAndDisplay(json));
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Network Error")
                            .setMessage(error)
                            .setPositiveButton("OK", null)
                            .show());
                }
            });
        } catch (Exception ex) {
            Log.e("MainActivity", "fetchWeatherByCity error", ex);
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Unable to request weather: " + ex.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void parseAndDisplay(String json) {
        try {
            JSONObject root = new JSONObject(json);

            // --- Build header: Location + formatted date/time ---
            String resolvedAddress = root.optString("resolvedAddress", "");
            String displayLocation;
            if (resolvedAddress.contains(",")) {
                String[] parts = resolvedAddress.split(",");
                displayLocation = parts[0].trim();
            } else if (!resolvedAddress.isEmpty()) {
                displayLocation = resolvedAddress;
            } else {
                displayLocation = "Unknown location";
            }

            // We'll format the current time from the currentConditions.datetimeEpoch (fallback to now)
            long headerEpoch = System.currentTimeMillis() / 1000L;
            if (root.has("currentConditions")) {
                JSONObject currentTest = root.optJSONObject("currentConditions");
                if (currentTest != null) {
                    headerEpoch = currentTest.optLong("datetimeEpoch", headerEpoch);
                }
            }

            // Format like: "Sat Oct 12 12:00 PM"
            String formattedDateTime;
            try {
                java.text.SimpleDateFormat headerSdf = new java.text.SimpleDateFormat("EEE, MMM d h:mm a", java.util.Locale.getDefault());
                formattedDateTime = headerSdf.format(new java.util.Date(headerEpoch * 1000L));
            } catch (Exception e) {
                formattedDateTime = "";
            }

            String header = displayLocation;
            if (!formattedDateTime.isEmpty()) header = header + ", " + formattedDateTime;
            // Set combined header TextView (make sure activity_main.xml has tvHeader)
            if (binding != null && binding.tvHeader != null) binding.tvHeader.setText(header);

            // --- Parse current conditions ---
            JSONObject current = root.getJSONObject("currentConditions");
            CurrentConditions cc = new CurrentConditions();
            cc.datetime = current.optString("datetime");
            cc.datetimeEpoch = current.optLong("datetimeEpoch");
            cc.temp = current.optDouble("temp", 0);
            cc.feelslike = current.optDouble("feelslike", 0);
            cc.humidity = current.optDouble("humidity", 0);
            cc.uvindex = current.optDouble("uvindex", 0);
            cc.conditions = current.optString("conditions");
            cc.icon = current.optString("icon");
            cc.windspeed = current.optDouble("windspeed", 0);
            cc.windgust = current.optDouble("windgust", 0);
            cc.winddir = current.optInt("winddir", 0);
            cc.visibility = current.optDouble("visibility", 0);
            cc.sunriseEpoch = current.optLong("sunriseEpoch", 0);
            cc.sunsetEpoch = current.optLong("sunsetEpoch", 0);

            currentTemp = cc.temp;
            String unitLetter = unitGroup.equals("us") ? "F" : "C";
            if (binding != null) {
                binding.tvTemp.setText(String.format(Locale.getDefault(), "%.0f°%s", cc.temp, unitLetter));
                binding.tvFeelsLike.setText(String.format(Locale.getDefault(), "Feels like %.0f°%s", cc.feelslike, unitLetter));
                binding.tvConditionDesc.setText(cc.conditions + (current.has("cloudcover") ? " (" + current.optInt("cloudcover") + "% clouds)" : ""));
                binding.tvWind.setText(String.format(Locale.getDefault(), "Winds: %s at %.1f %s%s",
                        Utils.getDirection(cc.winddir),
                        cc.windspeed,
                        unitGroup.equals("us") ? "mph" : "kph",
                        (current.has("windgust") ? " gusting to " + cc.windgust : "")));
                binding.tvHumidity.setText(String.format(Locale.getDefault(), "Humidity: %.0f%%", cc.humidity));
                binding.tvUV.setText(String.format(Locale.getDefault(), "UV Index: %.0f", cc.uvindex));
                binding.tvVisibility.setText(String.format(Locale.getDefault(), "Visibility: %.1f %s", cc.visibility, unitGroup.equals("us") ? "mi" : "km"));
                binding.tvSunrise.setText("Sunrise: " + Utils.epochToTime(cc.sunriseEpoch));
                binding.tvSunset.setText("Sunset: " + Utils.epochToTime(cc.sunsetEpoch));
            }

            String iconName = (cc.icon != null && !cc.icon.isEmpty()) ? cc.icon.replace("-", "_") : "ic_launcher";
            int iconId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            if (iconId == 0) iconId = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName());
            if (binding != null && binding.ivWeatherIcon != null) binding.ivWeatherIcon.setImageResource(iconId);

            // Parse days
            JSONArray daysArr = root.getJSONArray("days");
            days.clear();
            for (int i = 0; i < daysArr.length(); i++) {
                JSONObject d = daysArr.getJSONObject(i);
                Day day = new Day();
                day.datetime = d.optString("datetime");
                day.datetimeEpoch = d.optLong("datetimeEpoch");
                day.tempmax = d.optDouble("tempmax");
                day.tempmin = d.optDouble("tempmin");
                day.temp = d.optDouble("temp");
                day.uvindex = d.optDouble("uvindex");
                day.description = d.optString("description");
                day.icon = d.optString("icon");
                day.precipprob = d.optDouble("precipprob", 0);

                JSONArray hoursArr = d.getJSONArray("hours");
                List<Hour> hours = new ArrayList<>();
                for (int h = 0; h < hoursArr.length(); h++) {
                    JSONObject hj = hoursArr.getJSONObject(h);
                    Hour hr = new Hour();
                    hr.datetime = hj.optString("datetime");
                    hr.datetimeEpoch = hj.optLong("datetimeEpoch");
                    hr.temp = hj.optDouble("temp", 0);
                    hr.icon = hj.optString("icon");
                    hr.conditions = hj.optString("conditions");
                    hours.add(hr);
                }
                day.hours = hours;
                days.add(day);
            }

            // --- Build continuous upcoming hourly list across days (not just today's hours) ---
            hourlyList.clear();

            long nowEpoch = System.currentTimeMillis() / 1000L;
            List<Hour> allFutureHours = new ArrayList<>();

            // gather hours from all days
            for (Day dayObj : days) {
                if (dayObj == null || dayObj.hours == null) continue;
                for (Hour h : dayObj.hours) {
                    if (h == null) continue;
                    if (h.datetimeEpoch >= nowEpoch) {
                        allFutureHours.add(h);
                    }
                }
            }

            // sort by epoch just in case
            allFutureHours.sort((a, b) -> Long.compare(a.datetimeEpoch, b.datetimeEpoch));

            // Optionally limit how many hours to show (e.g., next 24 hours)
            int maxEntries = Math.min(24, allFutureHours.size());
            for (int i = 0; i < maxEntries; i++) {
                Hour hh = allFutureHours.get(i);
                hh.datetime = Utils.epochToHourLabel(hh.datetimeEpoch);
                hourlyList.add(hh);
            }
            hourlyAdapter.notifyDataSetChanged();

            // Build ordered map for chart
            Map<String, Double> ordered = new LinkedHashMap<>();
            for (Hour hh : hourlyList) {
                String label = Utils.epochToHourLabel(hh.datetimeEpoch);
                ordered.put(label, hh.temp);
            }
            drawChart(ordered, unitGroup.equals("us") ? "°F" : "°C");

            // Set background gradient
            if (binding != null) ColorMaker.setColorGradient(binding.rootMain, currentTemp, unitLetter);

        } catch (Exception ex) {
            Log.e("MainActivity", "parse error", ex);
            new AlertDialog.Builder(this)
                    .setTitle("Parse Error")
                    .setMessage("Error parsing weather data: " + ex.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private int getDrawableResourceByName(String name) {
        try {
            int res = getResources().getIdentifier(name, "drawable", getPackageName());
            if (res == 0) {
                // fallback to mipmap launcher for missing drawable name
                res = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName());
            }
            return res;
        } catch (Exception ex) {
            Log.w("MainActivity", "drawable lookup failed for: " + name, ex);
            return 0;
        }
    }

    /**
     * Reverse-geocodes lat/lon to a friendly city name.
     * Returns "Unknown" if no useful name is found or on error.
     * This method is blocking and must be called from a background thread.
     */
    private String reverseGeocodeToCity(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);

                String locality = addr.getLocality();
                if (locality != null && !locality.isEmpty()) return locality;

                String subAdmin = addr.getSubAdminArea();
                if (subAdmin != null && !subAdmin.isEmpty()) return subAdmin;

                String admin = addr.getAdminArea();
                if (admin != null && !admin.isEmpty()) return admin;

                String feature = addr.getFeatureName();
                if (feature != null && !feature.isEmpty()) return feature;

                String thoroughfare = addr.getThoroughfare();
                if (thoroughfare != null && !thoroughfare.isEmpty()) return thoroughfare;

                String addrLine = addr.getAddressLine(0);
                if (addrLine != null && !addrLine.isEmpty()) return addrLine;
            }
        } catch (IOException io) {
            Log.w("MainActivity", "Geocoder IO error", io);
        } catch (IllegalArgumentException ia) {
            Log.w("MainActivity", "Geocoder invalid lat/lon", ia);
        } catch (Exception ex) {
            Log.w("MainActivity", "Unexpected geocoder error", ex);
        }
        return "Unknown";
    }

    private void drawChart(Map<String, Double> map, String labelUnit) {
        try {
            binding.chartTemp.clear();

            List<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();
            final List<String> xLabels = new ArrayList<>();
            int idx = 0;
            for (Map.Entry<String, Double> e : map.entrySet()) {
                if (e.getValue() == null) continue;
                entries.add(new Entry(idx, e.getValue().floatValue()));
                xLabels.add(e.getKey());
                idx++;
            }

            if (entries.isEmpty()) {
                binding.chartTemp.clear();
                binding.chartTemp.setNoDataText("No chart data available.");
                binding.chartTemp.invalidate();
                return;
            }

            LineDataSet set = new LineDataSet(entries, "Temperature " + labelUnit);
            set.setLineWidth(2f);
            set.setDrawCircles(true);
            set.setCircleRadius(3f);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            LineData data = new LineData(set);
            binding.chartTemp.setData(data);

            XAxis xAxis = binding.chartTemp.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(Math.min(xLabels.size(), 6), true);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));

            YAxis left = binding.chartTemp.getAxisLeft();
            left.setGranularity(1f);
            binding.chartTemp.getAxisRight().setEnabled(false);

            binding.chartTemp.getDescription().setEnabled(false);
            binding.chartTemp.getLegend().setEnabled(false);

            binding.chartTemp.invalidate();
        } catch (Exception ex) {
            Log.w("MainActivity", "drawChart failed", ex);
        }
    }
}
