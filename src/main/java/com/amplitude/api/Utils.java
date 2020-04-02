package com.amplitude.api;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Utils {
    private static final String TAG = Utils.class.getName();

    private static AmplitudeLog logger = AmplitudeLog.getLogger();

    /**
     * Do a shallow copy of a JSONObject. Takes a bit of code to avoid
     * stringify and reparse given the API.
     */
    static JSONObject cloneJSONObject(final JSONObject obj) {
        if (obj == null) {
            return null;
        }

        if (obj.length() == 0) {
            return new JSONObject();
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

    static boolean compareJSONObjects(JSONObject o1, JSONObject o2) {
        try {

            if (o1 == o2) {
                return true;
            }

            if ((o1 != null && o2 == null) || (o1 == null && o2 != null)) {
                return false;
            }

            if (o1.length() != o2.length()) {
                return false;
            }

            Iterator<?> keys = o1.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (!o2.has(key)) {
                    return false;
                }

                Object value1 = o1.get(key);
                Object value2 = o2.get(key);

                if (!value1.getClass().equals(value2.getClass())) {
                    return false;
                }

                if (value1.getClass() == JSONObject.class) {
                    if (!compareJSONObjects((JSONObject) value1, (JSONObject) value2)) {
                        return false;
                    }
                } else if (!value1.equals(value2)) {
                    return false;
                }
            }

            return true;
        } catch (JSONException e) {}
        return false;
    }

    public static boolean isEmptyString(String s) {
        return (s == null || s.length() == 0);
    }

    static String normalizeInstanceName(String instance) {
        if (isEmptyString(instance)) {
            instance = Constants.DEFAULT_INSTANCE;
        }
        return instance.toLowerCase();
    }

    static SharedPreferences getAmplitudeSharedPreferences(Context context, String instanceName) {
        assert !isEmptyString(instanceName);
        String prefName = Constants.PACKAGE_NAME + "." + instanceName + "." + context.getPackageName();
        return context.getSharedPreferences(prefName, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    static void writeStringToSharedPreferences(Context context, String instanceName, String key, String value) {
        SharedPreferences.Editor editor = getAmplitudeSharedPreferences(context, instanceName).edit();
        editor.putString(key, value);
        editor.apply();
    }

    static String getStringFromSharedPreferences(Context context, String instanceName, String key) {
        return getAmplitudeSharedPreferences(context, instanceName).getString(key, null);
    }

    static boolean checkLocationPermissionAllowed(Context context) {
        return checkPermissionAllowed(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                checkPermissionAllowed(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    static boolean checkPermissionAllowed(Context context, String permission) {
        // ANDROID 6.0 AND UP!
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            boolean hasPermission = false;
            try {
                // Invoke checkSelfPermission method from Android 6 (API 23 and UP)
                java.lang.reflect.Method methodCheckPermission = Activity.class.getMethod("checkSelfPermission", java.lang.String.class);
                Object resultObj = methodCheckPermission.invoke(context, permission);
                int result = Integer.parseInt(resultObj.toString());
                hasPermission = (result == PackageManager.PERMISSION_GRANTED);
            } catch (Exception ex) {

            }

            return hasPermission;
        } else {
            return true;
        }
    }
}
