package com.amplitude.api;

import okhttp3.OkHttpClient;

import java.io.IOException;

public interface NetworkClient {

    Response uploadEvents(EventUploadRequest eventUploadRequest, OkHttpClient client) throws IOException, IllegalArgumentException;

}
