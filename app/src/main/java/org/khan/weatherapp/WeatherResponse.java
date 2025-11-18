package org.khan.weatherapp;

import java.util.List;

/**
 * Top-level model for the Visual Crossing Timeline API response.
 * Useful if you want to deserialize the whole response with Gson,
 * or to pass the full parsed structure between activities.
 */
public class WeatherResponse {
    public String resolvedAddress;
    public String address;
    public String timezone;
    public double queryCost;          // optional, if present
    public CurrentConditions currentConditions;
    public List<Day> days;
    // alerts omitted (not required by assignment)
}
