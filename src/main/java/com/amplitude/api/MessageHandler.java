package com.amplitude.api;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

class MessageHandler extends Handler {

    private static final String TAG = MessageHandler.class.getName();
    private static final AmplitudeLog logger = AmplitudeLog.getLogger();

    static final int REQUEST_FLUSH = 1;
    HttpClient httpClient;
    private HttpService.RequestListener requestListener;

    public MessageHandler(Looper looper, boolean secure, String apiKey, String url, String bearerToken,
                          HttpService.RequestListener requestListener) {
        super(looper);
        if (secure) {
            httpClient = new SSLHttpsClient(apiKey, url, bearerToken);
        } else {
            httpClient = new HttpClient(apiKey, url, bearerToken);
        }
        this.requestListener = requestListener;
    }

    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case REQUEST_FLUSH:
                EventsPayload data = (EventsPayload) msg.obj;
                flushEvents(data);
                break;
            default:
                logger.e(TAG, "Unknown dispatcher message: " + msg.what);
                break;
        }
    }

    private void flushEvents(EventsPayload data) {
        try {
            HttpResponse response = httpClient.getSyncHttpResponse(data.events);
            if (response.responseCode == 200 && response.responseMessage.equals("success")) {
                requestListener.onSuccess(data.maxEventId, data.maxIdentifyId);
            } else {
                String stringResponse = response.responseMessage;
                if (response.responseCode == 413) {
                    requestListener.onErrorRetry(data.maxEventId, data.maxIdentifyId);
                } else {
                    if (stringResponse.equals("invalid_api_key")) {
                        logger.e(TAG, "Invalid API key, make sure your API key is correct in initialize()");
                    } else if (stringResponse.equals("bad_checksum")) {
                        logger.w(TAG,
                                "Bad checksum, post request was mangled in transit, will attempt to reupload later");
                    } else if (stringResponse.equals("request_db_write_failed")) {
                        logger.w(TAG,
                                "Couldn't write to request database on server, will attempt to reupload later");
                    } else {
                        logger.w(TAG, "Upload failed, " + stringResponse
                                + ", will attempt to reupload later");
                    }
                    requestListener.onError();
                }
            }
        } catch (Exception e) {
            logger.e(TAG, e.toString());
            requestListener.onError();
        }
    }

    public void setApiKey(String apiKey) { httpClient.setApiKey(apiKey); }
    public void setUrl(String url) { httpClient.setUrl(url); }
    public void setBearerToken(String bearerToken) { httpClient.setBearerToken(bearerToken); }

}
