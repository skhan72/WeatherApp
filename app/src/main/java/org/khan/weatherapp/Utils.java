package org.khan.weatherapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    /**
     * Convert epoch seconds to "h:mm a" (e.g., 7:12 AM)
     * Uses device default timezone. If you need API timezone, set TimeZone on SimpleDateFormat.
     */
    public static String epochToTime(long epochSeconds) {
        if (epochSeconds <= 0) return "";
        Date date = new Date(epochSeconds * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Convert epoch seconds to a short hour label like "1 PM", "2 AM"
     */
    public static String epochToHourLabel(long epochSeconds) {
        if (epochSeconds <= 0) return "";
        Date date = new Date(epochSeconds * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("h a", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Convert wind degrees to compass direction (N, NE, E, SE, S, SW, W, NW)
     */
    public static String getDirection(double degrees) {
        if (degrees >= 337.5 || degrees < 22.5)
            return "N";
        if (degrees >= 22.5 && degrees < 67.5)
            return "NE";
        if (degrees >= 67.5 && degrees < 112.5)
            return "E";
        if (degrees >= 112.5 && degrees < 157.5)
            return "SE";
        if (degrees >= 157.5 && degrees < 202.5)
            return "S";
        if (degrees >= 202.5 && degrees < 247.5)
            return "SW";
        if (degrees >= 247.5 && degrees < 292.5)
            return "W";
        if (degrees >= 292.5 && degrees < 337.5)
            return "NW";
        return "X"; // We'll use 'X' as the default if we get a bad value
    }

    /**
     * Optional helper: safe formatting for temperature display
     */
    public static String formatTemp(double temp, boolean isUS) {
        String unit = isUS ? "°F" : "°C";
        return String.format(Locale.getDefault(), "%.0f%s", temp, unit);
    }
}
