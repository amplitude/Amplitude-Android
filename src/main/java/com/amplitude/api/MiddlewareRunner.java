package com.amplitude.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MiddlewareRunner {
    private final ConcurrentLinkedQueue<Middleware> middlewares;

    public MiddlewareRunner() {
        middlewares = new ConcurrentLinkedQueue<>();
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
                runMiddlewares((middlewares.subList(1, middlewares.size())), curPayload, next);
            }
        });
    }

    public boolean run(MiddlewarePayload payload) {
        AtomicBoolean middlewareCompleted = new AtomicBoolean(false);
        this.run(payload, new MiddlewareNext() {
            @Override
            public void run(MiddlewarePayload curPayload) {
                middlewareCompleted.set(true);
            }
        });
        return middlewareCompleted.get();
    }

    public void run(MiddlewarePayload payload, MiddlewareNext next) {
        List<Middleware> middlewareList = new ArrayList<>(this.middlewares);
        runMiddlewares(middlewareList, payload, next);
    }
}