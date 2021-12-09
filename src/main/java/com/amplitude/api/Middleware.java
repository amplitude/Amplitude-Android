package com.amplitude.api;

public interface Middleware {
	void run(MiddlewarePayload payload, MiddlewareNext next);
}