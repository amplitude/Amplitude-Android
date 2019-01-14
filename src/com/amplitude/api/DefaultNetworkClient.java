package com.amplitude.api;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class DefaultNetworkClient implements NetworkClient {

    @Override
    public Response uploadEvents(EventUploadRequest eventUploadRequest, OkHttpClient client) throws IOException {

        String apiVersionString = "" + eventUploadRequest.getApiVersion();
        String timestampString = "" + eventUploadRequest.getUploadTime();

        FormBody body = new FormBody.Builder()
                .add("v", apiVersionString)
                .add("client", eventUploadRequest.getApiKey())
                .add("e", eventUploadRequest.getEvents())
                .add("upload_time", timestampString)
                .add("checksum", eventUploadRequest.getChecksum())
                .build();

        Request request = new Request.Builder()
                .url(eventUploadRequest.getUrl())
                .post(body)
                .build();

        return client.newCall(request).execute();
    }
}
