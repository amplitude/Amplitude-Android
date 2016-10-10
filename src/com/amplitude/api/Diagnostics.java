package com.amplitude.api;

import android.text.TextUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by danieljih on 10/10/16.
 */
public class Diagnostics {

    public static final int MAX_UNSENT_DIAGNOSTIC_QUEUE_SIZE = 10; // limit memory footprint

    private volatile boolean enabled = false;
    private int maxKeys;
    private Map<String, Integer> unsentErrorCounts;

    protected static Diagnostics instance = new Diagnostics();

    public static Diagnostics getLogger() {
        return instance;
    }

    private Diagnostics() {
        maxKeys = MAX_UNSENT_DIAGNOSTIC_QUEUE_SIZE;
        unsentErrorCounts = new HashMap<String, Integer>(maxKeys);
    }

    Diagnostics setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    Diagnostics setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
        return this;
    }

    Diagnostics logError(String error) {
        if (!enabled || TextUtils.isEmpty(error)) {
            return this;
        }

        // aggregate counts for unsent events
        if (unsentErrorCounts.containsKey(error)) {
            unsentErrorCounts.put(error, unsentErrorCounts.get(error) + 1);
        } else if (unsentErrorCounts.size() < maxKeys) {
            unsentErrorCounts.put(error, 1);
        }

        return this;
    }

    Diagnostics clear() {
        unsentErrorCounts = new HashMap<String, Integer>(maxKeys);
        return this;
    }

    JSONObject getUnsentErrorsJSON() {
        return new JSONObject(unsentErrorCounts);
    }
}
