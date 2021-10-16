package com.amplitude.api;

public class SSLHttpsClient extends HttpClient {

    public SSLHttpsClient(String apiKey, String url, String bearerToken) {
        super(apiKey, url, bearerToken);
    }

}
