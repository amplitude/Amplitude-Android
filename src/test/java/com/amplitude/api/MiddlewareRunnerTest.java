package com.amplitude.api;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest= Config.NONE)
public class MiddlewareRunnerTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testMiddlewareRun() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

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
        amplitude.addEventMiddleware(updateDeviceIdMiddleware);

        JSONObject event = new JSONObject().put("device_model", "sample_device");
        MiddlewareExtra extra = new MiddlewareExtra();
        boolean middlewareCompleted = amplitude.middlewareRunner.run(new MiddlewarePayload(event, (MiddlewareExtra) extra));

        assertTrue(middlewareCompleted);
        assertEquals(event.getString("device_model"), middlewareDevice);
    }

    @Test
    public void testRunWithNotPassMiddleware() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

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
        amplitude.addEventMiddleware(updateDeviceIdMiddleware);
        amplitude.addEventMiddleware(swallowMiddleware);

        JSONObject event = new JSONObject().put("device_model", "sample_device").put("user_id", "sample_user");
        boolean middlewareCompleted = amplitude.middlewareRunner.run(new MiddlewarePayload(event));

        assertFalse(middlewareCompleted);
        assertEquals(event.getString("device_model"), middlewareDevice);
        assertEquals(event.getString("user_id"), middlewareUser);
    }

}
