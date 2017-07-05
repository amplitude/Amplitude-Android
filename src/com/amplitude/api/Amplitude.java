package com.amplitude.api;

import android.content.Context;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * <h1>Amplitude</h1>
 * This is the main Amplitude class that manages SDK instances. <br><br>
 * <b>NOTE:</b> All of the methods except {@code getInstance()} have been deprecated.
 * Please call those methods on the AmplitudeClient instance instead, for example:
 * {@code Amplitude.getInstance().logEvent();}
 *
 * @see com.amplitude.api.AmplitudeClient AmplitudeClient
 */
public class Amplitude {

    static final Map<String, AmplitudeClient> instances = new HashMap<String, AmplitudeClient>();

    /**
     * Gets the default instance.
     *
     * @return the default instance
     */
    public static AmplitudeClient getInstance() {
        return getInstance(null);
    }

    /**
     * Gets the specified instance. If instance is null or empty string, fetches the default
     * instance instead.
     *
     * @param instance name to get "ex app 1"
     * @return the specified instance
     */
    public static synchronized AmplitudeClient getInstance(String instance) {
        instance = Utils.normalizeInstanceName(instance);
        AmplitudeClient client = instances.get(instance);
        if (client == null) {
            client = new AmplitudeClient(instance);
            instances.put(instance, client);
        }
        return client;
    }

    /**
     * Initialize the SDK with the Android app context and Amplitude API key.
     * Initializing is required before calling other methods such as {@code logEvent();}.
     *
     * @param context the context
     * @param apiKey  the api key
     */
    @Deprecated
    public static void initialize(Context context, String apiKey) {
        getInstance().initialize(context, apiKey);
    }

    /**
     * Initialize the SDK with the Android app context, Amplitude API key, and a user Id.
     * Initializing is required before calling other methods such as {@code logEvent();}.
     *
     * @param context the context
     * @param apiKey  the api key
     * @param userId  the user id
     */
    @Deprecated
    public static void initialize(Context context, String apiKey, String userId) {
        getInstance().initialize(context, apiKey, userId);
    }

    /**
     * Enable new device id per install.
     *
     * @param newDeviceIdPerInstall the new device id per install
     */
    @Deprecated
    public static void enableNewDeviceIdPerInstall(boolean newDeviceIdPerInstall) {
        getInstance().enableNewDeviceIdPerInstall(newDeviceIdPerInstall);
    }

    /**
     * Use advertising id for device id.
     */
    @Deprecated
    public static void useAdvertisingIdForDeviceId() {
        getInstance().useAdvertisingIdForDeviceId();
    }

    /**
     * Enable location listening.
     */
    @Deprecated
    public static void enableLocationListening() {
        getInstance().enableLocationListening();
    }

    /**
     * Disable location listening.
     */
    @Deprecated
    public static void disableLocationListening() {
        getInstance().disableLocationListening();
    }

    /**
     * Sets session timeout millis.
     *
     * @param sessionTimeoutMillis the session timeout millis
     */
    @Deprecated
    public static void setSessionTimeoutMillis(long sessionTimeoutMillis) {
        getInstance().setSessionTimeoutMillis(sessionTimeoutMillis);
    }

    /**
     * Sets opt out.
     *
     * @param optOut the opt out
     */
    @Deprecated
    public static void setOptOut(boolean optOut) {
        getInstance().setOptOut(optOut);
    }

    /**
     * Log event.
     *
     * @param eventType the event type
     */
    @Deprecated
    public static void logEvent(String eventType) {
        getInstance().logEvent(eventType);
    }

    /**
     * Log event.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     */
    @Deprecated
    public static void logEvent(String eventType, JSONObject eventProperties) {
        getInstance().logEvent(eventType, eventProperties);
    }

    /**
     * Upload events.
     */
    @Deprecated
    public static void uploadEvents() {
        getInstance().uploadEvents();
    }

    /**
     * Start session.
     */
    @Deprecated
    public static void startSession() { return; }

    /**
     * End session.
     */
    @Deprecated
    public static void endSession() { return; }

    /**
     * Log revenue.
     *
     * @param amount the amount
     */
    @Deprecated
    public static void logRevenue(double amount) {
        getInstance().logRevenue(amount);
    }

    /**
     * Log revenue.
     *
     * @param productId the product id
     * @param quantity  the quantity
     * @param price     the price
     */
    @Deprecated
    public static void logRevenue(String productId, int quantity, double price) {
        getInstance().logRevenue(productId, quantity, price);
    }

    /**
     * Log revenue.
     *
     * @param productId        the product id
     * @param quantity         the quantity
     * @param price            the price
     * @param receipt          the receipt
     * @param receiptSignature the receipt signature
     */
    @Deprecated
    public static void logRevenue(String productId, int quantity, double price, String receipt,
            String receiptSignature) {
        getInstance().logRevenue(productId, quantity, price, receipt, receiptSignature);
    }

    /**
     * Sets user properties.
     *
     * @param userProperties the user properties
     */
    @Deprecated
    public static void setUserProperties(JSONObject userProperties) {
        getInstance().setUserProperties(userProperties);
    }

    /**
     * Sets user properties.
     *
     * @param userProperties the user properties
     * @param replace        the replace
     */
    @Deprecated
    public static void setUserProperties(JSONObject userProperties, boolean replace) {
        getInstance().setUserProperties(userProperties, replace);
    }

    /**
     * Sets user id.
     *
     * @param userId the user id
     */
    @Deprecated
    public static void setUserId(String userId) {
        getInstance().setUserId(userId);
    }

    /**
     * Gets device id.
     *
     * @return the device id
     */
    @Deprecated
    public static String getDeviceId() {
        return getInstance().getDeviceId();
    }
}
