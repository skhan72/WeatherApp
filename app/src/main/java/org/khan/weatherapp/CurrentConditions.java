package org.khan.weatherapp;

/**
 * Represents the "currentConditions" object in the Visual Crossing timeline response.
 */
public class CurrentConditions {
    public String datetime;        // e.g., "15:00:00"
    public long datetimeEpoch;     // epoch seconds

    public double temp;
    public double feelslike;
    public double humidity;
    public double uvindex;

    public String conditions;      // e.g., "Partly Cloudy"
    public String icon;            // e.g., "partly-cloudy-day"

    public double windspeed;
    public double windgust;
    public int winddir;            // degrees
    public double visibility;
    public double cloudcover;

    public long sunriseEpoch;
    public long sunsetEpoch;
}
