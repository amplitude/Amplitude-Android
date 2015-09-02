package com.amplitude.api;

import android.util.Log;

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

    public Identify() { return; }

    protected Identify(Identify other) throws JSONException {
        this.userPropertiesOperations = new JSONObject(other.userPropertiesOperations.toString());
        this.userProperties = new HashSet<String>(other.userProperties);
    }

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
            Log.e(TAG, String.format("Already used property %s in previous operation, ignoring", property));
            return;
        }

        try {
            if (userPropertiesOperations.has(operation)) {
                userPropertiesOperations.getJSONObject(operation).put(property, value);
            } else {
                JSONObject operations = new JSONObject().put(property, value);
                userPropertiesOperations.put(operation, operations);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
        }

        userProperties.add(property);
    }

    /*
    public boolean mergeIdentifyEvents(Identify toMerge) {
        Set<String> currentProperties = new HashSet<String>(this.properties);
        currentProperties.retainAll(toMerge.properties);
        if (currentProperties.size() > 0) {
            return false;
        }

        try {
            Iterator<?> operations = toMerge.userProperties.keys();
            while (operations.hasNext()) {
                String operation = (String) operations.next();
                if (this.userProperties.has(operation)) {
                    Iterator<?> actions =
                } else {
                    this.userProperties.put(operation, toMerge.userProperties.get(operation));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return false;
        }

        return true;
    }
    */
}
