package com.amplitude.api;

import org.json.JSONObject;

public class MiddlewarePayload {
    public JSONObject event;
    public MiddlewareExtra extra;

    public MiddlewarePayload(JSONObject event, MiddlewareExtra extra) {
        this.event = event;
        this.extra = extra;
    }
}