package com.amplitude.unity.plugins;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.amplitude.api.Amplitude;

public class AmplitudePlugin {
    
    public static void init(Context context, String apiKey) {
        Amplitude.initialize(context, apiKey);
    }
    
    public static void init(Context context, String apiKey, String userId) {
        Amplitude.initialize(context, apiKey, userId);
    }

    public static void startSession() {
        Amplitude.startSession();
    }

    public static void endSession() {
        Amplitude.endSession();
    }
    
    public static void logEvent(String event) {
        Amplitude.logEvent(event);
    }
    
    public static void logEvent(String event, String jsonProperties) {
        JSONObject properties = null;
        try {
            properties = new JSONObject(jsonProperties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        Amplitude.logEvent(event, properties);
    }
    
    public static void setUserId(String userId) {
        Amplitude.setUserId(userId);
    }
    
    public static void setUserProperties(String jsonProperties) {
        JSONObject properties = null;
        try {
            properties = new JSONObject(jsonProperties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        Amplitude.setUserProperties(properties);
    }
    
    public static void logRevenue(double amount) {
        Amplitude.logRevenue(amount);
    }
}
