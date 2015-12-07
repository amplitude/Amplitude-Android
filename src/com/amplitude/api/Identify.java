package com.amplitude.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/*
    Wrapper class for identify events and user properties operations
 */
public class Identify {

    public static final String TAG = "com.amplitude.api.Identify";

    protected JSONObject userPropertiesOperations = new JSONObject();
    protected Set<String> userProperties = new HashSet<String>();

    // SETONCE
    public Identify setOnce(String property, String value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify setOnce(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify setOnce(String property, float value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify setOnce(String property, int value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify setOnce(String property, long value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify setOnce(String property, double value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify setOnce(String property, JSONArray value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify setOnce(String property, JSONObject value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    // SET
    public Identify set(String property, String value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    public Identify set(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    public Identify set(String property, float value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    public Identify set(String property, int value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    public Identify set(String property, long value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    public Identify set(String property, double value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    public Identify set(String property, JSONObject value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    public Identify set(String property, JSONArray value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }


    // ADD
    public Identify add(String property, String value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    public Identify add(String property, float value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    public Identify add(String property, int value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    public Identify add(String property, long value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    public Identify add(String property, double value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    // APPEND
    public Identify append(String property, String value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    public Identify append(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    public Identify append(String property, float value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    public Identify append(String property, int value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    public Identify append(String property, long value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    public Identify append(String property, double value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    public Identify append(String property, JSONArray value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    public Identify append(String property, JSONObject value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    // UNSET
    public Identify unset(String property) {
        addToUserProperties(Constants.AMP_OP_UNSET, property, "-");
        return this;
    }

    private void addToUserProperties(String operation, String property, Object value) {
        // check if property already used in previous operation
        if (userProperties.contains(property)) {
            AmplitudeLog.getLogger().w(TAG, String.format("Already used property %s in previous operation, ignoring operation %s", property, operation));
            return;
        }

        try {
            if (!userPropertiesOperations.has(operation)) {
                userPropertiesOperations.put(operation, new JSONObject());
            }
            userPropertiesOperations.getJSONObject(operation).put(property, value);
            userProperties.add(property);
        } catch (JSONException e) {
            AmplitudeLog.getLogger().e(TAG, e.toString());
        }
    }

    // Package private method - used by AmplitudeClient to convert setUserProperties to identify
    Identify setUserProperty(String property, Object value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    // DEPRECATED - Object class is too general
    public Identify setOnce(String property, Object value) {
        AmplitudeLog.getLogger().w(TAG, String.format("This version of setOnce is deprecated. Please use one with a different signature."));
        return this;
    }

    // DEPRECATED - Object class is too general
    public Identify set(String property, Object value) {
        AmplitudeLog.getLogger().w(TAG, String.format("This version of set is deprecated. Please use one with a different signature."));
        return this;
    }
}
