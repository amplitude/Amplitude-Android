package com.amplitude.api;

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

    public Identify setOnce(String property, Object value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify set(String property, Object value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
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
}
