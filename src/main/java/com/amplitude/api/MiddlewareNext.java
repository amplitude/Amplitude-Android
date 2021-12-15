package com.amplitude.api;

public interface MiddlewareNext {
    public void run(MiddlewarePayload curPayload);
}
