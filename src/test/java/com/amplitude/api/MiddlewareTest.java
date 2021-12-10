package com.amplitude.api;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

@RunWith(AndroidJUnit4.class)
@Config(manifest= Config.NONE)
public class MiddlewareTest extends BaseTest {

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

    private MiddlewareNext next = new MiddlewareNext() {
        @Override
        public void run(MiddlewarePayload curPayload) {

        }
    };

    @Test
    public void testWithNoMiddleware() throws JSONException {
        //should call next() with no middleware
        MiddlewareRunner middllewareRunner = new MiddlewareRunner();
        JSONObject event = new JSONObject();
        MiddlewareExtra extra = new MiddlewareExtra();
        extra.put("description", "extra description");
        event.put("user_id", "test_user");
        MiddlewarePayload paylod = new MiddlewarePayload(event, extra);
        final boolean[] middlewareCompleted = {false};
        middllewareRunner.run(paylod, new MiddlewareNext() {
            @Override
            public void run(MiddlewarePayload curPayload) {
                middlewareCompleted[0] = true;
            }
        });
        assertTrue(middlewareCompleted[0]);
    }

    @Test
    public void testWithSingleMiddleware() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        MiddlewareExtra extra = new MiddlewareExtra();
        extra.put("description", "extra description");
        Middleware middleware = new Middleware() {
            @Override
            public void run(MiddlewarePayload payload, MiddlewareNext next) {
                try {
                    payload.event.optJSONObject("event_properties").put("description", "extra description");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                next.run(payload);
            }
        };
        amplitude.addEventMiddleware(middleware);
        amplitude.logEvent("middleware_event_type", new JSONObject().put("user_id", "middleware_user"), null, System.currentTimeMillis(), false, extra);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);
        JSONArray eventObject = getUnsentEvents(1);;
        assertEquals(eventObject.optJSONObject(0).optString("event_type"), "middleware_event_type");
        assertEquals(eventObject.optJSONObject(0).optJSONObject("event_properties").getString("description"), "extra description");
        assertEquals(eventObject.optJSONObject(0).optJSONObject("event_properties").optString("user_id"), "middleware_user");
    }

    @Test
    public void testWithSwallowMiddleware() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        Middleware middleware = new Middleware() {
            @Override
            public void run(MiddlewarePayload payload, MiddlewareNext next) {
            }
        };
        amplitude.addEventMiddleware(middleware);
        amplitude.logEvent("middleware_event_type", new JSONObject().put("user_id", "middleware_user"), null, System.currentTimeMillis(), false, null);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
    }

    @Test
    public void testWithMultipleMiddleware() throws JSONException {
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

        // second middleware
        String middlewareUser = "middleware_user";
        Middleware updateUserIdMiddleware = new Middleware() {
            @Override
            public void run(MiddlewarePayload payload, MiddlewareNext next) {
                try {
                    payload.event.put("user_id", middlewareUser);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                next.run(payload);
            }
        };

        amplitude.addEventMiddleware(updateDeviceIdMiddleware);
        amplitude.addEventMiddleware(updateUserIdMiddleware);

        JSONObject event = new JSONObject().put("device_model", "sample_device").put("user_id", "sample_user");
        amplitude.logEvent("middleware_event_type", event, null, System.currentTimeMillis(), false, null);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);
        JSONArray eventObject = getUnsentEvents(1);
        assertEquals(eventObject.optJSONObject(0).getString("device_model"), middlewareDevice);
        assertEquals(eventObject.optJSONObject(0).getString("user_id"), middlewareUser);
    }

}
