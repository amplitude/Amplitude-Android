package com.amplitude.api;

public class EventUploadRequest {

    final private int apiVersion;
    final private String apiKey;
    final private String events;
    final private long uploadTime;
    final private String checksum;
    final private String url;

    public EventUploadRequest(int apiVersion, String apiKey, String events, long uploadTime, String checksum, String url) {
        this.apiVersion = apiVersion;
        this.apiKey = apiKey;
        this.events = events;
        this.uploadTime = uploadTime;
        this.checksum = checksum;
        this.url = url;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getEvents() {
        return events;
    }

    public long getUploadTime() {
        return uploadTime;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getUrl() {
        return url;
    }
}
