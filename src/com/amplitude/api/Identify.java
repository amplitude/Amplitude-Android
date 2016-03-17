package com.amplitude.api;

import android.text.TextUtils;

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
    public Identify setOnce(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify setOnce(String property, double value) {
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

    public Identify setOnce(String property, String value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    public Identify setOnce(String property, JSONArray values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, values);
        return this;
    }

    public Identify setOnce(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, values);
        return this;
    }

    public Identify setOnce(String property, boolean[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, booleanArrayToJSONArray(values));
        return this;
    }

    public Identify setOnce(String property, double[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, doubleArrayToJSONArray(values));
        return this;
    }

    public Identify setOnce(String property, float[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, floatArrayToJSONArray(values));
        return this;
    }

    public Identify setOnce(String property, int[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, intArrayToJSONArray(values));
        return this;
    }

    public Identify setOnce(String property, long[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, longArrayToJSONArray(values));
        return this;
    }

    public Identify setOnce(String property, String[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, stringArrayToJSONArray(values));
        return this;
    }


    // SET
    public Identify set(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    public Identify set(String property, double value) {
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

    public Identify set(String property, String value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    public Identify set(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_SET, property, values);
        return this;
    }

    public Identify set(String property, JSONArray values) {
        addToUserProperties(Constants.AMP_OP_SET, property, values);
        return this;
    }

    public Identify set(String property, boolean[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, booleanArrayToJSONArray(values));
        return this;
    }

    public Identify set(String property, double[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, doubleArrayToJSONArray(values));
        return this;
    }

    public Identify set(String property, float[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, floatArrayToJSONArray(values));
        return this;
    }

    public Identify set(String property, int[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, intArrayToJSONArray(values));
        return this;
    }

    public Identify set(String property, long[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, longArrayToJSONArray(values));
        return this;
    }

    public Identify set(String property, String[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, stringArrayToJSONArray(values));
        return this;
    }


    // ADD
    public Identify add(String property, double value) {
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

    // Server-side converts string numbers to numbers
    public Identify add(String property, String value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    // Server-side we flatten dictionaries and apply add to each flattened proeprty
    public Identify add(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_ADD, property, values);
        return this;
    }


    // APPEND
    public Identify append(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    public Identify append(String property, double value) {
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

    public Identify append(String property, String value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    public Identify append(String property, JSONArray values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, values);
        return this;
    }

    // Server-side we flatten dictionaries and apply append to each flattened property
    public Identify append(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, values);
        return this;
    }

    public Identify append(String property, boolean[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, booleanArrayToJSONArray(values));
        return this;
    }

    public Identify append(String property, double[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, doubleArrayToJSONArray(values));
        return this;
    }

    public Identify append(String property, float[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, floatArrayToJSONArray(values));
        return this;
    }

    public Identify append(String property, int[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, intArrayToJSONArray(values));
        return this;
    }

    public Identify append(String property, long[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, longArrayToJSONArray(values));
        return this;
    }

    public Identify append(String property, String[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, stringArrayToJSONArray(values));
        return this;
    }


    // PREPEND
    public Identify prepend(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    public Identify prepend(String property, double value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    public Identify prepend(String property, float value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    public Identify prepend(String property, int value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    public Identify prepend(String property, long value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    public Identify prepend(String property, String value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    public Identify prepend(String property, JSONArray values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, values);
        return this;
    }

    // Server-side we flatten dictionaries and apply prepend to each flattened property
    public Identify prepend(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, values);
        return this;
    }

    public Identify prepend(String property, boolean[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, booleanArrayToJSONArray(values));
        return this;
    }

    public Identify prepend(String property, double[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, doubleArrayToJSONArray(values));
        return this;
    }

    public Identify prepend(String property, float[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, floatArrayToJSONArray(values));
        return this;
    }

    public Identify prepend(String property, int[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, intArrayToJSONArray(values));
        return this;
    }

    public Identify prepend(String property, long[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, longArrayToJSONArray(values));
        return this;
    }

    public Identify prepend(String property, String[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, stringArrayToJSONArray(values));
        return this;
    }


    // UNSET
    public Identify unset(String property) {
        addToUserProperties(Constants.AMP_OP_UNSET, property, "-");
        return this;
    }


    // CLEAR ALL
    public Identify clearAll() {
        if (userPropertiesOperations.length() > 0) {
            if (!userProperties.contains(Constants.AMP_OP_CLEAR_ALL)) {
                AmplitudeLog.getLogger().w(TAG, String.format(
                   "Need to send $clearAll on its own Identify object without any other " +
                   "operations, ignoring $clearAll"
                ));
            }
            return this;
        }

        try {
            userPropertiesOperations.put(Constants.AMP_OP_CLEAR_ALL, "-");
        } catch (JSONException e) {
            AmplitudeLog.getLogger().e(TAG, e.toString());
        }
        return this;
    }


    private void addToUserProperties(String operation, String property, Object value) {
        if (TextUtils.isEmpty(property)) {
            AmplitudeLog.getLogger().w(TAG, String.format(
               "Attempting to perform operation %s with a null or empty string property, ignoring",
                operation
            ));
            return;
        }

        if (value == null) {
            AmplitudeLog.getLogger().w(TAG, String.format(
                "Attempting to perform operation %s with null value for property %s, ignoring",
                operation, property
            ));
            return;
        }

        // check that clearAll wasn't already used in this Identify
        if (userPropertiesOperations.has(Constants.AMP_OP_CLEAR_ALL)) {
            AmplitudeLog.getLogger().w(TAG, String.format(
                "This Identify already contains a $clearAll operation, ignoring operation %s",
                operation
            ));
            return;
        }

        // check if property already used in previous operation
        if (userProperties.contains(property)) {
            AmplitudeLog.getLogger().w(TAG, String.format(
                "Already used property %s in previous operation, ignoring operation %s",
                property, operation
            ));
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

    private JSONArray booleanArrayToJSONArray(boolean[] values) {
        JSONArray array = new JSONArray();
        for (boolean value : values) array.put(value);
        return array;
    }

    private JSONArray floatArrayToJSONArray(float[] values) {
        JSONArray array = new JSONArray();
        for (float value : values) {
            try {
                array.put(value);
            } catch (JSONException e) {
                AmplitudeLog.getLogger().e(TAG, String.format(
                    "Error converting float %f to JSON: %s", value, e.toString()
                ));
            }
        }
        return array;
    }

    private JSONArray doubleArrayToJSONArray(double[] values) {
        JSONArray array = new JSONArray();
        for (double value : values) {
            try {
                array.put(value);
            } catch (JSONException e) {
                AmplitudeLog.getLogger().e(TAG, String.format(
                    "Error converting double %d to JSON: %s", value, e.toString()
                ));
            }
        }
        return array;
    }

    private JSONArray intArrayToJSONArray(int[] values) {
        JSONArray array = new JSONArray();
        for (int value : values) array.put(value);
        return array;
    }

    private JSONArray longArrayToJSONArray(long[] values) {
        JSONArray array = new JSONArray();
        for (long value : values) array.put(value);
        return array;
    }

    private JSONArray stringArrayToJSONArray(String[] values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        return array;
    }

    // Package private method - used by AmplitudeClient to convert setUserProperties to identify
    Identify setUserProperty(String property, Object value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    // DEPRECATED - Signature is too general
    public Identify setOnce(String property, Object value) {
        AmplitudeLog.getLogger().w(
            TAG,
            "This version of setOnce is deprecated. Please use one with a different signature."
        );
        return this;
    }

    // DEPRECATED - Signature is too general
    public Identify set(String property, Object value) {
        AmplitudeLog.getLogger().w(
            TAG,
            "This version of set is deprecated. Please use one with a different signature."
        );
        return this;
    }
}
