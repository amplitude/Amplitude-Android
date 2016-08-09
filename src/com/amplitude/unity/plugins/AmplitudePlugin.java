package com.amplitude.unity.plugins;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.amplitude.api.Amplitude;
import com.amplitude.api.Identify;
import com.amplitude.api.Revenue;

import org.json.JSONException;
import org.json.JSONObject;

public class AmplitudePlugin {

    public static JSONObject ToJSONObject(String jsonString) {
        JSONObject properties = null;
        try {
            properties = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return properties;
    }

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
        Amplitude.getInstance().logEvent(event, ToJSONObject(jsonProperties));
    }

    public static void logEvent(String event, String jsonProperties, boolean outOfSession) {
        Amplitude.getInstance().logEvent(event, ToJSONObject(jsonProperties), outOfSession);
    }

    public static void setUserId(String userId) {
        Amplitude.getInstance().setUserId(userId);
    }

    public static void setOptOut(boolean enabled) {
        Amplitude.getInstance().setOptOut(enabled);
    }

    public static void setUserProperties(String jsonProperties) {
        Amplitude.getInstance().setUserProperties(ToJSONObject(jsonProperties));
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

    public static void logRevenue(String productId, int quantity, double price, String receipt, String receiptSignature, String revenueType, String jsonProperties) {
        Revenue revenue = new Revenue().setQuantity(quantity).setPrice(price);
        if (!TextUtils.isEmpty(productId)) {
            revenue.setProductId(productId);
        }
        if (!TextUtils.isEmpty(receipt) && !TextUtils.isEmpty(receiptSignature)) {
            revenue.setReceipt(receipt, receiptSignature);
        }
        if (!TextUtils.isEmpty(revenueType)) {
            revenue.setRevenueType(revenueType);
        }
        if (!TextUtils.isEmpty(jsonProperties)) {
            revenue.setEventProperties(ToJSONObject(jsonProperties));
        }
        Amplitude.getInstance().logRevenueV2(revenue);
    }

    public static String getDeviceId() {
        return Amplitude.getInstance().getDeviceId();
    }

    public static void trackSessionEvents(boolean enabled) {
        Amplitude.getInstance().trackSessionEvents(enabled);
    }

    // User Property Operations

    // clear user properties
    public static void clearUserProperties() {
        Amplitude.getInstance().clearUserProperties();
    }

    // unset user property
    public static void unsetUserProperty(String property) {
        Amplitude.getInstance().identify(new Identify().unset(property));
    }

    // setOnce user property
    public static void setOnceUserProperty(String property, boolean value) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String property, double value) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String property, float value) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String property, int value) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String property, long value) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String property, String value) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserPropertyDict(String property, String values) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, ToJSONObject(values)));
    }

    public static void setOnceUserPropertyList(String property, String values) {
        JSONObject properties = ToJSONObject(values);
        if (properties == null) {
            return;
        }
        Amplitude.getInstance().identify(new Identify().setOnce(
            property, properties.optJSONArray("list")
        ));
    }

    public static void setOnceUserProperty(String property, boolean[] values) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String property, double[] values) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String property, float[] values) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String property, int[] values) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String property, long[] values) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String property, String[] values) {
        Amplitude.getInstance().identify(new Identify().setOnce(property, values));
    }

    // set user property
    public static void setUserProperty(String property, boolean value) {
        Amplitude.getInstance().identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String property, double value) {
        Amplitude.getInstance().identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String property, float value) {
        Amplitude.getInstance().identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String property, int value) {
        Amplitude.getInstance().identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String property, long value) {
        Amplitude.getInstance().identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String property, String value) {
        Amplitude.getInstance().identify(new Identify().set(property, value));
    }

    public static void setUserPropertyDict(String property, String values) {
        Amplitude.getInstance().identify(new Identify().set(property, ToJSONObject(values)));
    }

    public static void setUserPropertyList(String property, String values) {
        JSONObject properties = ToJSONObject(values);
        if (properties == null) {
            return;
        }
        Amplitude.getInstance().identify(new Identify().set(
                property, properties.optJSONArray("list")
        ));
    }

    public static void setUserProperty(String property, boolean[] values) {
        Amplitude.getInstance().identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String property, double[] values) {
        Amplitude.getInstance().identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String property, float[] values) {
        Amplitude.getInstance().identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String property, int[] values) {
        Amplitude.getInstance().identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String property, long[] values) {
        Amplitude.getInstance().identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String property, String[] values) {
        Amplitude.getInstance().identify(new Identify().set(property, values));
    }

    // add
    public static void addUserProperty(String property, double value) {
        Amplitude.getInstance().identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String property, float value) {
        Amplitude.getInstance().identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String property, int value) {
        Amplitude.getInstance().identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String property, long value) {
        Amplitude.getInstance().identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String property, String value) {
        Amplitude.getInstance().identify(new Identify().add(property, value));
    }

    public static void addUserPropertyDict(String property, String values) {
        Amplitude.getInstance().identify(new Identify().add(property, ToJSONObject(values)));
    }

    // append user property
    public static void appendUserProperty(String property, boolean value) {
        Amplitude.getInstance().identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String property, double value) {
        Amplitude.getInstance().identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String property, float value) {
        Amplitude.getInstance().identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String property, int value) {
        Amplitude.getInstance().identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String property, long value) {
        Amplitude.getInstance().identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String property, String value) {
        Amplitude.getInstance().identify(new Identify().append(property, value));
    }

    public static void appendUserPropertyDict(String property, String values) {
        Amplitude.getInstance().identify(new Identify().append(property, ToJSONObject(values)));
    }

    public static void appendUserPropertyList(String property, String values) {
        JSONObject properties = ToJSONObject(values);
        if (properties == null) {
            return;
        }
        Amplitude.getInstance().identify(new Identify().append(
                property, properties.optJSONArray("list")
        ));
    }

    public static void appendUserProperty(String property, boolean[] values) {
        Amplitude.getInstance().identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String property, double[] values) {
        Amplitude.getInstance().identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String property, float[] values) {
        Amplitude.getInstance().identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String property, int[] values) {
        Amplitude.getInstance().identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String property, long[] values) {
        Amplitude.getInstance().identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String property, String[] values) {
        Amplitude.getInstance().identify(new Identify().append(property, values));
    }
}
