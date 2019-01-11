package com.amplitude.api;

import okhttp3.Response;

public interface NetworkClient {

    Response uploadEvents();

}
