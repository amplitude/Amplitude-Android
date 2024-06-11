package com.amplitude.api;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
@Config(manifest= Config.NONE)
public class MiddlewareRunnerTest {
    MiddlewareRunner middlewareRunner = new MiddlewareRunner();

    @Test
    public void testMiddlewareRun() throws JSONException {
        String middlewareDevice = "middleware_device";
        Middleware updateDeviceIdMiddleware = new Middleware() {
            @Override
            public void run(MiddlewarePayload payload, MiddlewareNext next) {
                try {
                    payload.event.put("device_model", middlewareDevice);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                next.run(payload);
            }
        };
        middlewareRunner.add(updateDeviceIdMiddleware);

        JSONObject event = new JSONObject().put("device_model", "sample_device");
        boolean middlewareCompleted = middlewareRunner.run(new MiddlewarePayload(event, new MiddlewareExtra()));

        assertTrue(middlewareCompleted);
        assertEquals(event.getString("device_model"), middlewareDevice);
    }

    @Test
    public void testRunWithNotPassMiddleware() throws JSONException {
        // first middleware
        String middlewareDevice = "middleware_device";
        Middleware updateDeviceIdMiddleware = new Middleware() {
            @Override
            public void run(MiddlewarePayload payload, MiddlewareNext next) {
                try {
                    payload.event.put("device_model", middlewareDevice);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                next.run(payload);
            }
        };

        // swallow middleware
        String middlewareUser = "middleware_user";
        Middleware swallowMiddleware = new Middleware() {
            @Override
            public void run(MiddlewarePayload payload, MiddlewareNext next) {
                try {
                    payload.event.put("user_id", middlewareUser);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        middlewareRunner.add(updateDeviceIdMiddleware);
        middlewareRunner.add(swallowMiddleware);

        JSONObject event = new JSONObject().put("device_model", "sample_device").put("user_id", "sample_user");
        boolean middlewareCompleted = middlewareRunner.run(new MiddlewarePayload(event));

        assertFalse(middlewareCompleted);
        assertEquals(event.getString("device_model"), middlewareDevice);
        assertEquals(event.getString("user_id"), middlewareUser);
    }

    @Test
    public void testMiddlewareFlush() throws JSONException {
        AtomicInteger runCount = new AtomicInteger(0);
        AtomicInteger flushCount = new AtomicInteger(0);

        MiddlewareExtended flushMiddleware = new MiddlewareExtended() {
            @Override
            public void run(MiddlewarePayload payload, MiddlewareNext next) {
                runCount.incrementAndGet();
            }

            @Override
            public void flush() {
                flushCount.incrementAndGet();
            }
        };

        middlewareRunner.add(flushMiddleware);

        middlewareRunner.flush();

        assertEquals(flushCount.get(), 1);
        assertEquals(runCount.get(), 0);
    }

}
