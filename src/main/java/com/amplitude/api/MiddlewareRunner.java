package com.amplitude.api;

import java.util.ArrayList;
import java.util.List;

public class MiddlewareRunner {
    private final List<Middleware> middlewares;

    public MiddlewareRunner() {
        middlewares = new ArrayList<>();
    }

    public void add(Middleware middleware) {
        this.middlewares.add(middleware);
    }

    private void runMiddlewares(List<Middleware> middlewares, MiddlewarePayload payload, MiddlewareNext next) {
        if (middlewares.size() == 0 ){
            next.run(payload);
            return;
        }
        middlewares.get(0).run(payload, new MiddlewareNext() {
            @Override
            public void run(MiddlewarePayload curPayload) {
                runMiddlewares(middlewares.subList(1, middlewares.size()), curPayload, next);
            }
        });
    }

    public void run(MiddlewarePayload payload, MiddlewareNext next) {
        runMiddlewares(this.middlewares, payload, next);
    }
}