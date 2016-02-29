package com.amplitude.unity.plugins;

import android.app.Application;
import android.content.Context;

import com.amplitude.api.Amplitude;

import org.json.JSONException;
import org.json.JSONObject;

public class AmplitudePlugin {

    public static void init(Context context, String apiKey) {
        Amplitude.getInstance().initialize(context, apiKey);
    }

    public static void init(Context context, String apiKey, String userId) {
        Amplitude.getInstance().initialize(context, apiKey, userId);
    }

    public static void enableForegroundTracking(Application app) {
        Amplitude.getInstance().enableForegroundTracking(app);
    }

    @Deprecated
    public static void startSession() { return; }

    @Deprecated
    public static void endSession() { return; }

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

    public static void logEvent(String event, String jsonProperties, boolean outOfSession) {
        JSONObject properties = null;
        try {
            properties = new JSONObject(jsonProperties);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Amplitude.getInstance().logEvent(event, properties, outOfSession);
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

    public static void unsetUserProperties(String propertiesList) {
        Amplitude.getInstance().unsetUserProperties(propertiesList.split(","));
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

    public static void trackSessionEvents(boolean enabled) {
        Amplitude.getInstance().trackSessionEvents(enabled);
    }
}
