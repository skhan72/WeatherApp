package org.khan.weatherapp;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Volley RequestQueue singleton.
 * Use: NetworkClient.getInstance(context).addToRequestQueue(request);
 */
public class NetworkClient {
    private static NetworkClient instance;
    private RequestQueue requestQueue;
    private static Context ctx;

    private NetworkClient(Context context) {
        ctx = context.getApplicationContext();
        requestQueue = getRequestQueue();
    }

    public static synchronized NetworkClient getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkClient(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // ApplicationContext used to avoid leaking an Activity
            requestQueue = Volley.newRequestQueue(ctx);
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
