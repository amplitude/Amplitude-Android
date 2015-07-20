package com.amplitude.unity.plugins;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.amplitude.api.Amplitude;

public class AmplitudePlugin {

    public static void init(Context context, String apiKey) {
        Amplitude.getInstance().initialize(context, apiKey);
    }

    public static void init(Context context, String apiKey, String userId) {
        Amplitude.getInstance().initialize(context, apiKey, userId, null);
    }

    public static void startSession() {
        Amplitude.getInstance().startSession();
    }

    public static void endSession() {
        Amplitude.getInstance().endSession();
    }

    public static void logEvent(String event) {
        Amplitude.getInstance().logEvent(event);
    }

    public static void logEvent(String event, String jsonProperties) {
        JSONObject properties = null;
        try {
            properties = new JSONObject(jsonProperties);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Amplitude.getInstance().logEvent(event, properties);
    }

    public static void setUserId(String userId) {
        Amplitude.getInstance().setUserId(userId);
    }

    public static void setOptOut(boolean enabled) {
        Amplitude.getInstance().setOptOut(enabled);
    }

    public static void setUserProperties(String jsonProperties) {
        JSONObject properties = null;
        try {
            properties = new JSONObject(jsonProperties);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Amplitude.getInstance().setUserProperties(properties);
    }

    public static void logRevenue(double amount) {
        Amplitude.getInstance().logRevenue(amount);
    }

    public static void logRevenue(String productId, int quantity, double price) {
        Amplitude.getInstance().logRevenue(productId, quantity, price);
    }

    public static void logRevenue(String productId, int quantity, double price, String receipt, String receiptSignature) {
        Amplitude.getInstance().logRevenue(productId, quantity, price, receipt, receiptSignature);
    }

    public static String getDeviceId() {
        return Amplitude.getInstance().getDeviceId();
    }
}
