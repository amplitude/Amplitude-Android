package com.amplitude.api;

public class Response {

    private final int code;
    private final String body;

    public Response(int code, String body) {
        this.body = body;
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getBody() {
        return body;
    }
}
