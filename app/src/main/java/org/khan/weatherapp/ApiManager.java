package org.khan.weatherapp;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

public class ApiManager {

    public static String lastJsonResponse = "";

    public interface ApiCallback {
        void onSuccess(String json);
        void onFailure(String error);
    }

    public static void fetchWeatherForLocation(
            Context context,
            String location,
            String apiKey,
            String unitGroup,
            ApiCallback callback
    ) {
        try {
            String encoded = location.replace(" ", "%20");

            String url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
                    + encoded
                    + "?unitGroup=" + unitGroup
                    + "&key=" + apiKey
                    + "&contentType=json";

            StringRequest request = new StringRequest(
                    Request.Method.GET,
                    url,
                    response -> {

                        ApiManager.lastJsonResponse = response;


                        callback.onSuccess(response);
                    },
                    error -> callback.onFailure(error.getMessage())
            );

            NetworkClient.getInstance(context).addToRequestQueue(request);

        } catch (Exception ex) {
            callback.onFailure(ex.getMessage());
        }
    }
}
