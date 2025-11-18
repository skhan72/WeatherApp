package org.khan.weatherapp;


 // Represents a single hourly entry inside a day's "hours" array.

public class Hour {
    public String datetime;       // raw string like "13:00:00"
    public long datetimeEpoch;    // epoch seconds
    public double temp;           // temperature for the hour

    public String icon;           // "partly-cloudy-day"
    public String conditions;     // "Partly Cloudy"

    public double windspeed;
    public int winddir;
    public double precipprob;
    public double visibility;
    public double cloudcover;
}
