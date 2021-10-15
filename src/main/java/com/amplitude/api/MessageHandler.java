package com.amplitude.api;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class MessageHandler extends Handler {

    static final int REQUEST_FLUSH = 1;
    private final HttpService httpService;
    private final HttpClient httpClient;
    private final HttpService.RequestListener requestListener;

    MessageHandler(Looper looper, HttpService httpService, HttpClient httpClient, HttpService.RequestListener requestListener) {
        super(looper);
        this.httpService = httpService;
        this.httpClient = httpClient;
        this.requestListener = requestListener;
    }

    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case REQUEST_FLUSH:
                SendEventsMessageData data = (SendEventsMessageData) msg.obj;
                this.post(new Runnable() {
                    public void run() {
                        try {
                            HttpResponse response = httpClient.getSyncHttpResponse(
                                    httpService.apiKey, httpService.url, httpService.bearerToken, data.events);
                            if (response.responseCode == 200 && response.responseMessage.equals("success")) {
                                requestListener.onSuccessFinished(response, data.maxEventId, data.maxIdentifyId);
                            } else {
                                requestListener.onErrorFinished(response, data.maxEventId, data.maxIdentifyId);
                            }
                        } catch (Exception e) {
                            requestListener.onException(e.toString());
                        }
                    }
                });
                break;
            default:
                throw new AssertionError("Unknown dispatcher message: " + msg.what);
        }
    }

}
