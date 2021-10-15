package com.amplitude.api;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.os.HandlerThread;
import android.os.Looper;

public class HttpService {

    HandlerThread httpThread;
    private MessageHandler messageHandler;

    public HttpService(String apiKey, String url, String bearerToken, RequestListener requestListener,
                        boolean secure) {
        this.httpThread = new HandlerThread("httpThread", THREAD_PRIORITY_BACKGROUND);
        httpThread.start();

        HttpClient httpClient;
        if (secure) {
            httpClient = new SSLHttpsClient(apiKey, url, bearerToken);
        } else {
            httpClient = new HttpClient(apiKey, url, bearerToken);
        }
        this.messageHandler = new MessageHandler(httpThread.getLooper(), httpClient, requestListener);
    }

    //sends a message to our MessageHandler
    public void submitSendEvents(String events, long maxEventId, long maxIdentifyId) {
        SendEventsData data = new SendEventsData(events, maxEventId, maxIdentifyId);
        messageHandler.sendMessage(messageHandler.obtainMessage(MessageHandler.REQUEST_FLUSH, data));
    }

    public static class RequestListener {
        public void onSuccess(long maxEventId, long maxIdentifyId) {}
        public void onError(long maxEventId, long maxIdentifyId) {}
    }

    public Looper getHttpThreadLooper() {
        return httpThread.getLooper();
    }

    public void shutdown() {
        messageHandler.removeCallbacks(this.httpThread);
        this.httpThread.quit();
    }

}
