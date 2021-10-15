package com.amplitude.api;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.os.HandlerThread;
import android.os.Looper;

public class HttpService {

    String apiKey, url, bearerToken;
    HandlerThread httpThread;
    private MessageHandler messageHandler;

    public HttpService(String apiKey, String url, String bearerToken, RequestListener requestListener,
                        boolean secure) {
        this.apiKey = apiKey;
        this.url = url;
        this.bearerToken = bearerToken;
        this.httpThread = new HandlerThread("httpThread", THREAD_PRIORITY_BACKGROUND);
        httpThread.start();

        HttpClient httpClient;
        if (secure) {
            httpClient = new SSLHttpsClient();
        } else {
            httpClient = new HttpClient();
        }
        this.messageHandler = new MessageHandler(httpThread.getLooper(), this, httpClient, requestListener);
    }

    //sends a message to our MessageHandler
    public void submitSendEvents(String events, long maxEventId, long maxIdentifyId) {
        SendEventsMessageData data = new SendEventsMessageData(events, maxEventId, maxIdentifyId);
        messageHandler.sendMessage(messageHandler.obtainMessage(MessageHandler.REQUEST_FLUSH, data));
    }

    public static class RequestListener {
        public void onSuccessFinished(HttpResponse response, long maxEventId, long maxIdentifyId) {}
        public void onErrorFinished(HttpResponse response, long maxEventId, long maxIdentifyId) {}
        public void onException(String exceptionString) {}
    }

    public Looper getHttpThreadLooper() {
        return httpThread.getLooper();
    }

    public void shutdown() {
        messageHandler.removeCallbacks(this.httpThread);
        this.httpThread.quit();
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
