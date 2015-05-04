package com.amplitude.api;

import org.json.JSONObject;

public interface EventListeningInterface {
    void trackEvent(JSONObject jsonObject);
}