package com.amplitude.identitymanager;

import android.util.Log;

public class IdentityUtils {
    static private final int device_id_length = 25;
    static private final String base_36_char_set = "abcdefghijklmnopqrstuvwxyz0123456789";
    static private final int base_36_radix = 36;

    static void logIdentityWarning(String message) {
        Log.w(Identity.class.getName(), message);
    }

    static String generateBase36Id() {
        String stringBuilder = "";
        for (int idx = 0; idx < device_id_length; idx++) {
            double randomIdx = Math.floor(Math.random() * base_36_radix);
            char nextChar = base_36_char_set.charAt((int)randomIdx);
            stringBuilder += nextChar;
        }

        return stringBuilder;
    }
}
