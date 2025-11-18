package org.khan.weatherapp;

import java.util.List;


 //Represents a daily summary object in the "days" array.
public class Day {
    public String datetime;       // e.g., "2025-11-16"
    public long datetimeEpoch;    // epoch seconds

    public double tempmax;
    public double tempmin;
    public double temp;           // average temp for the day

    public double uvindex;
    public String description;
    public String icon;
    public double precipprob;

    public List<Hour> hours;      // list of 24 Hour objects for the day
}
