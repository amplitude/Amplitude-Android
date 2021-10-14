package com.amplitude.api;

public class HttpResponse {

    public String responseMessage;
    public int responseCode;
    public Exception error;

    public HttpResponse(String responseMessage, int responseCode, Exception error) {
        this.responseMessage = responseMessage;
        this.responseCode = responseCode;
        this.error = error;
    }

}
