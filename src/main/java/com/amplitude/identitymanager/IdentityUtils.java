package com.amplitude.identitymanager;

import java.util.Random;

public class IdentityUtils {
    static private final int device_id_length = 25;
    static private final String base_36_char_set = "abcdefghijklmnopqrstuvwxyz0123456789";
    static private final int base_36_radix = 36;

    static String generateBase36Id() {
        String stringBuilder = "";
        Random rand = new Random();
        for (int idx = 0; idx < device_id_length; idx++) {
            int randomIdx = rand.nextInt(base_36_radix);
            char nextChar = base_36_char_set.charAt(randomIdx);
            stringBuilder += nextChar;
        }

        return stringBuilder;
    }
}
