package com.amplitude.api;

class HttpResponse {
    public String responseMessage;
    public int responseCode;

    public HttpResponse(String responseMessage, int responseCode) {
        this.responseMessage = responseMessage;
        this.responseCode = responseCode;
    }
}
