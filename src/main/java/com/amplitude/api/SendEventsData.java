package com.amplitude.api;

public class SendEventsData {
    public String events;
    public long maxEventId, maxIdentifyId;
    public SendEventsData(String events, long maxEventId, long maxIdentifyId) {
        this.events = events;
        this.maxEventId = maxEventId;
        this.maxIdentifyId = maxIdentifyId;
    }
}
