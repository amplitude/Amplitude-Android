package com.amplitude.api;

public class EventsPayload {
    public String events;
    public long maxEventId, maxIdentifyId;
    public EventsPayload(String events, long maxEventId, long maxIdentifyId) {
        this.events = events;
        this.maxEventId = maxEventId;
        this.maxIdentifyId = maxIdentifyId;
    }
}
