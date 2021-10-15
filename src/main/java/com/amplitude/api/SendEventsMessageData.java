package com.amplitude.api;

public class SendEventsMessageData {
    public String events;
    public long maxEventId, maxIdentifyId;
    public SendEventsMessageData(String events, long maxEventId, long maxIdentifyId) {
        this.events = events;
        this.maxEventId = maxEventId;
        this.maxIdentifyId = maxIdentifyId;
    }
}
