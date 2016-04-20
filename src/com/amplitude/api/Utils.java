package com.amplitude.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by danieljih on 4/18/16.
 */
public class Utils {

    public static final String TAG = "com.amplitude.api.Utils";
    private static AmplitudeLog logger = AmplitudeLog.getLogger();


    /**
     * Do a shallow copy of a JSONObject. Takes a bit of code to avoid
     * stringify and reparse given the API.
     */
    static JSONObject cloneJSONObject(final JSONObject obj) {
        if (obj == null) {
            return null;
        }

        // obj.names returns null if the json obj is empty.
        JSONArray nameArray = null;
        try {
            nameArray = obj.names();
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.e(TAG, e.toString());
        }
        int len = (nameArray != null ? nameArray.length() : 0);

        String[] names = new String[len];
        for (int i = 0; i < len; i++) {
            names[i] = nameArray.optString(i);
        }

        try {
            return new JSONObject(obj, names);
        } catch (JSONException e) {
            logger.e(TAG, e.toString());
            return null;
        }
    }

}
