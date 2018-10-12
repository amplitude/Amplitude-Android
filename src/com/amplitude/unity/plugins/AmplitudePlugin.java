package com.amplitude.unity.plugins;

import android.app.Application;
import android.content.Context;

import com.amplitude.api.Amplitude;
import com.amplitude.api.Identify;
import com.amplitude.api.Revenue;
import com.amplitude.api.Utils;

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

    public static void init(String instanceName, Context context, String apiKey) {
        Amplitude.getInstance(instanceName).initialize(context, apiKey);
    }

    public static void init(String instanceName, Context context, String apiKey, String userId) {
        Amplitude.getInstance(instanceName).initialize(context, apiKey, userId);
    }

    public static void enableForegroundTracking(String instanceName, Application app) {
        Amplitude.getInstance(instanceName).enableForegroundTracking(app);
    }

    @Deprecated
    public static void startSession() { return; }

    @Deprecated
    public static void endSession() { return; }

    public static void logEvent(String instanceName, String event) {
        Amplitude.getInstance(instanceName).logEvent(event);
    }

    public static void logEvent(String instanceName, String event, String jsonProperties) {
        Amplitude.getInstance(instanceName).logEvent(event, ToJSONObject(jsonProperties));
    }

    public static void logEvent(String instanceName, String event, String jsonProperties, boolean outOfSession) {
        Amplitude.getInstance(instanceName).logEvent(event, ToJSONObject(jsonProperties), outOfSession);
    }

    public static void setUserId(String instanceName, String userId) {
        Amplitude.getInstance(instanceName).setUserId(userId);
    }

    public static void setOptOut(String instanceName, boolean enabled) {
        Amplitude.getInstance(instanceName).setOptOut(enabled);
    }

    public static void setUserProperties(String instanceName, String jsonProperties) {
        Amplitude.getInstance(instanceName).setUserProperties(ToJSONObject(jsonProperties));
    }

    public static void logRevenue(String instanceName, double amount) {
        Amplitude.getInstance(instanceName).logRevenue(amount);
    }

    public static void logRevenue(String instanceName, String productId, int quantity, double price) {
        Amplitude.getInstance(instanceName).logRevenue(productId, quantity, price);
    }

    public static void logRevenue(String instanceName, String productId, int quantity, double price, String receipt, String receiptSignature) {
        Amplitude.getInstance(instanceName).logRevenue(productId, quantity, price, receipt, receiptSignature);
    }

    public static void logRevenue(String instanceName, String productId, int quantity, double price, String receipt, String receiptSignature, String revenueType, String jsonProperties) {
        Revenue revenue = new Revenue().setQuantity(quantity).setPrice(price);
        if (!Utils.isEmptyString(productId)) {
            revenue.setProductId(productId);
        }
        if (!Utils.isEmptyString(receipt) && !Utils.isEmptyString(receiptSignature)) {
            revenue.setReceipt(receipt, receiptSignature);
        }
        if (!Utils.isEmptyString(revenueType)) {
            revenue.setRevenueType(revenueType);
        }
        if (!Utils.isEmptyString(jsonProperties)) {
            revenue.setEventProperties(ToJSONObject(jsonProperties));
        }
        Amplitude.getInstance(instanceName).logRevenueV2(revenue);
    }

    public static String getDeviceId(String instanceName) {
        return Amplitude.getInstance(instanceName).getDeviceId();
    }

    public static void regenerateDeviceId(String instanceName) { Amplitude.getInstance(instanceName).regenerateDeviceId(); }

    public static void trackSessionEvents(String instanceName, boolean enabled) {
        Amplitude.getInstance(instanceName).trackSessionEvents(enabled);
    }

    public static long getSessionId(String instanceName) { return Amplitude.getInstance(instanceName).getSessionId(); }

    // User Property Operations

    // clear user properties
    public static void clearUserProperties(String instanceName) {
        Amplitude.getInstance(instanceName).clearUserProperties();
    }

    // unset user property
    public static void unsetUserProperty(String instanceName, String property) {
        Amplitude.getInstance(instanceName).identify(new Identify().unset(property));
    }

    // setOnce user property
    public static void setOnceUserProperty(String instanceName, String property, boolean value) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, double value) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, float value) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, int value) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, long value) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, String value) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserPropertyDict(String instanceName, String property, String values) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, ToJSONObject(values)));
    }

    public static void setOnceUserPropertyList(String instanceName, String property, String values) {
        JSONObject properties = ToJSONObject(values);
        if (properties == null) {
            return;
        }
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(
            property, properties.optJSONArray("list")
        ));
    }

    public static void setOnceUserProperty(String instanceName, String property, boolean[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, double[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, float[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, int[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, long[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, String[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    // set user property
    public static void setUserProperty(String instanceName, String property, boolean value) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, double value) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, float value) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, int value) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, long value) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, String value) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserPropertyDict(String instanceName, String property, String values) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, ToJSONObject(values)));
    }

    public static void setUserPropertyList(String instanceName, String property, String values) {
        JSONObject properties = ToJSONObject(values);
        if (properties == null) {
            return;
        }
        Amplitude.getInstance(instanceName).identify(new Identify().set(
                property, properties.optJSONArray("list")
        ));
    }

    public static void setUserProperty(String instanceName, String property, boolean[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, double[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, float[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, int[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, long[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, String[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    // add
    public static void addUserProperty(String instanceName, String property, double value) {
        Amplitude.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String instanceName, String property, float value) {
        Amplitude.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String instanceName, String property, int value) {
        Amplitude.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String instanceName, String property, long value) {
        Amplitude.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String instanceName, String property, String value) {
        Amplitude.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserPropertyDict(String instanceName, String property, String values) {
        Amplitude.getInstance(instanceName).identify(new Identify().add(property, ToJSONObject(values)));
    }

    // append user property
    public static void appendUserProperty(String instanceName, String property, boolean value) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, double value) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, float value) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, int value) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, long value) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, String value) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserPropertyDict(String instanceName, String property, String values) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, ToJSONObject(values)));
    }

    public static void appendUserPropertyList(String instanceName, String property, String values) {
        JSONObject properties = ToJSONObject(values);
        if (properties == null) {
            return;
        }
        Amplitude.getInstance(instanceName).identify(new Identify().append(
                property, properties.optJSONArray("list")
        ));
    }

    public static void appendUserProperty(String instanceName, String property, boolean[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, double[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, float[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, int[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, long[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, String[] values) {
        Amplitude.getInstance(instanceName).identify(new Identify().append(property, values));
    }
}
