package com.amplitude.api;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.function.Function;

public class HttpService {

    private String apiKey, url, bearerToken;
    private RequestListenerCallback reqListener; //fire this after HTTP request completes
    HandlerThread httpThread;
    private MessageHandler messageHandler; //pass httpThread.looper() into this handler
    private HttpClient httpClient;

    public HttpService(String apiKey, String url, String bearerToken, RequestListenerCallback reqListener, boolean secure) {
        this.apiKey = apiKey;
        this.url = url;
        this.bearerToken = bearerToken;
        this.reqListener = reqListener;
        this.httpThread = new HandlerThread("httpThread", THREAD_PRIORITY_BACKGROUND);
        httpThread.start();
        this.messageHandler = new MessageHandler(httpThread.getLooper(), this);
        if (secure) {
            httpClient = new SSLHttpsClient();
        } else {
            httpClient = new HttpClient();
        }
    }

    //sends a message to our MessageHandler
    public void submitSendEvents(String events, long maxEventId, long maxIdentifyId) {
        SendEventsMessageData data = new SendEventsMessageData(events, maxEventId, maxIdentifyId);
        messageHandler.sendMessage(messageHandler.obtainMessage(MessageHandler.REQUEST_FLUSH, data));
    }

    //Actually calls HttpClient, the callback
    public void flushEvents(SendEventsMessageData data) {
        messageHandler.post(new Runnable() {
            public void run() {
                HttpResponse response = httpClient.getResponse(apiKey, url, bearerToken, data.events);
                reqListener.onRequestFinished(response, data.maxEventId, data.maxIdentifyId);
            }
        });
    }

    public static class MessageHandler extends Handler {

        static final int REQUEST_FLUSH = 1;
        private final HttpService httpService;

        MessageHandler(Looper looper, HttpService httpService) {
            super(looper);
            this.httpService = httpService;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REQUEST_FLUSH:
                    httpService.flushEvents((SendEventsMessageData) msg.obj);
                    break;
                default:
                    throw new AssertionError("Unknown dispatcher message: " + msg.what);
            }
        }

    }

    public interface RequestListenerCallback {
        void onRequestFinished(HttpResponse response, long maxEventId, long maxIdentifyId);
    }

    public static class SendEventsMessageData {
        public String events;
        public long maxEventId, maxIdentifyId;
        public SendEventsMessageData(String events, long maxEventId, long maxIdentifyId) {
            this.events = events;
            this.maxEventId = maxEventId;
            this.maxIdentifyId = maxIdentifyId;
        }
    }

    public Looper getHttpThreadLooper() {
        return httpThread.getLooper();
    }

    public void shutdown() {
        messageHandler.removeCallbacks(this.httpThread);
        this.httpThread.quit();
    }

}
