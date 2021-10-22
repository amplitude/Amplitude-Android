package com.amplitude.api;

import java.util.LinkedList;
import java.util.Queue;

public class MockWebServer {
    private Queue<RecordedRequest> requests;
    private Queue<MockHttpUrlConnection> queue;
    private int numRequestsMade = 0;

    public MockWebServer() {
        this.requests = new LinkedList<>();
        this.queue = new LinkedList<>();
    }

    public void sendRequest(RecordedRequest request) {
        requests.add(request);
        numRequestsMade++;
    }

    public void enqueueResponse(MockHttpUrlConnection res) {
        queue.add(res);
    }

    public MockHttpUrlConnection getNextResponse() {
        return queue.poll();
    }

    public RecordedRequest takeRequest() {
        return requests.poll();
    }

    public int getRequestCount() {
        return numRequestsMade;
    }
}
