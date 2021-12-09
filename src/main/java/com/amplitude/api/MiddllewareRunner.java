package com.amplitude.api;

import java.util.ArrayList;
import java.util.List;

public class MiddllewareRunner {
    private final List<Middleware> middlewares;

    public MiddllewareRunner() {
        middlewares = new ArrayList<>();
    }

    public void add(Middleware middleware) {
        this.middlewares.add(middleware);
    }

    public void runMiddlewares(List<Middleware> middlewares, MiddlewarePayload payload, MiddlewareNext next) {
        if (middlewares.size() == 0 ){
            next.middlewareNext(payload);
            return;
        }
        middlewares.get(0).run(payload, new MiddlewareNext() {
            @Override
            public void middlewareNext(MiddlewarePayload curPayload) {
                runMiddlewares(middlewares.subList(1, middlewares.size()), curPayload, next);
            }
        });
    }

    public void run(MiddlewarePayload payload, MiddlewareNext next) {
        runMiddlewares(this.middlewares, payload, next);
    }
}