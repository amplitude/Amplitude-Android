package com.amplitude.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.sql.SQLException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DiagnosticsTest extends BaseTest {

    private Diagnostics logger;
    private ShadowLooper looper;
    private OkHttpClient httpClient;
    private String deviceId;

    public RecordedRequest runRequest() {
        server.enqueue(new MockResponse().setBody("success"));
        looper.runToEndOfTasks();

        try {
            return server.takeRequest(1, SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void testStackTraceMethod() throws Exception {
        throw new SQLException("this is an exception inside test stack trace");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        httpClient = new OkHttpClient();
        deviceId = "test device id";
        logger = Diagnostics.getLogger();
        looper = Shadows.shadowOf(logger.diagnosticThread.getLooper());
        logger.url = server.url("/").toString();
        Robolectric.getForegroundThreadScheduler().advanceTo(1);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Diagnostics.instance = null;
    }

    @Test
    public void testInitialize() {
        assertFalse(logger.enabled);
        assertTrue(logger.unsentErrors.isEmpty());
    }

    @Test
    public void testEnableLogging() {
        logger.enableLogging(httpClient, apiKey, deviceId);
        assertTrue(logger.enabled);
    }

    @Test
    public void testResize() {
        logger.enableLogging(httpClient, apiKey, deviceId);
        for (int i = 0; i < Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT + 1; i++) {
            logger.logError("test " + String.valueOf(i));
        }
        looper.runToEndOfTasks();
        assertEquals(logger.unsentErrors.size(), Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT + 1);

        // resize and verify that we enforce minimum value
        logger.setDiagnosticEventMaxCount(Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT - 1);
        looper.runToEndOfTasks();
        assertEquals(logger.diagnosticEventMaxCount, Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT);
        assertEquals(logger.unsentErrors.size(), Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT);

        // verify we truncated from start of list
        assertEquals(logger.unsentErrorStrings.get(0), "test 1");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optString("error"), "test 1");

        // verify that on next log error, we truncate
        logger.logError("test");
        looper.runToEndOfTasks();
        assertEquals(logger.unsentErrors.size(), 1);
        assertEquals(logger.unsentErrorStrings.get(0), "test");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optString("error"), "test");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optInt("count"), 1);

        // verify safe to resize to greater
        logger.setDiagnosticEventMaxCount(Diagnostics.DIAGNOSTIC_EVENT_MAX_COUNT + 1);
        looper.runToEndOfTasks();
        assertEquals(logger.diagnosticEventMaxCount, Diagnostics.DIAGNOSTIC_EVENT_MAX_COUNT);
        assertEquals(logger.unsentErrors.size(), 1);
        assertEquals(logger.unsentErrorStrings.get(0), "test");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optString("error"), "test");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optInt("count"), 1);
    }

    @Test
    public void testLogError() {
        logger.enableLogging(httpClient, apiKey, deviceId);

        long timestamp = System.currentTimeMillis();
        logger.logError("test_error");
        logger.logError("test_error1");
        logger.logError("test_error2");
        looper.runToEndOfTasks();
        assertEquals(logger.unsentErrors.size(), 3);

        assertEquals(logger.unsentErrorStrings.get(0), "test_error");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optString("error"), "test_error");
        assertTrue(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optInt("count"), 1);
        assertEquals(logger.unsentErrorStrings.get(1), "test_error1");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(1)).optString("error"), "test_error1");
        assertTrue(logger.unsentErrors.get(logger.unsentErrorStrings.get(1)).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(1)).optInt("count"), 1);
        assertEquals(logger.unsentErrorStrings.get(2), "test_error2");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(2)).optString("error"), "test_error2");
        assertTrue(logger.unsentErrors.get(logger.unsentErrorStrings.get(2)).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(2)).optInt("count"), 1);

        // test truncation
        logger.setDiagnosticEventMaxCount(7);
        looper.runToEndOfTasks();
        logger.logError("test_error3");
        logger.logError("test_error4");
        logger.logError("test_error5");
        logger.logError("test_error6");
        logger.logError("test_error7");
        looper.runToEndOfTasks();

        // logged 8 events, but removed 5, so 3 left
        assertEquals(logger.unsentErrors.size(), 3);
        assertEquals(logger.unsentErrorStrings.get(0), "test_error5");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optString("error"), "test_error5");
        assertTrue(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optInt("count"), 1);
        assertEquals(logger.unsentErrorStrings.get(1), "test_error6");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(1)).optString("error"), "test_error6");
        assertTrue(logger.unsentErrors.get(logger.unsentErrorStrings.get(1)).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(1)).optInt("count"), 1);
        assertEquals(logger.unsentErrorStrings.get(2), "test_error7");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(2)).optString("error"), "test_error7");
        assertTrue(logger.unsentErrors.get(logger.unsentErrorStrings.get(2)).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(2)).optInt("count"), 1);
    }

    @Test
    public void testLogDuplicateError() {
        logger.enableLogging(httpClient, apiKey, deviceId);

        long timestamp = System.currentTimeMillis();
        logger.logError("test_error");
        logger.logError("test_error1");
        logger.logError("test_error2");
        logger.logError("test_error");
        logger.logError("test_error1");
        logger.logError("test_error");
        looper.runToEndOfTasks();
        assertEquals(logger.unsentErrors.size(), 3);

        assertEquals(logger.unsentErrorStrings.get(0), "test_error");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optString("error"), "test_error");
        assertTrue(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(0)).optInt("count"), 3);
        assertEquals(logger.unsentErrorStrings.get(1), "test_error1");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(1)).optString("error"), "test_error1");
        assertTrue(logger.unsentErrors.get(logger.unsentErrorStrings.get(1)).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(1)).optInt("count"), 2);
        assertEquals(logger.unsentErrorStrings.get(2), "test_error2");
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(2)).optString("error"), "test_error2");
        assertTrue(logger.unsentErrors.get(logger.unsentErrorStrings.get(2)).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentErrors.get(logger.unsentErrorStrings.get(2)).optInt("count"), 1);
    }

        @Test
    public void testDisabled() {
        logger.enableLogging(httpClient, apiKey, deviceId).disableLogging();
        logger.logError("test_error");
        logger.logError("test_error1");
        logger.logError("test_error2");
        looper.runToEndOfTasks();

        assertEquals(logger.unsentErrors.size(), 0);
    }

    @Test
    public void testUploadEvents() throws JSONException {
        logger.enableLogging(httpClient, apiKey, deviceId);

        long timestamp = System.currentTimeMillis();
        logger.logError("test_error");
        logger.logError("test_error1");
        logger.logError("test_error2");
        looper.runToEndOfTasks();

        logger.flushEvents();
        RecordedRequest request = runRequest();
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.optJSONObject(0).optString("error"), "test_error");
        assertTrue(events.optJSONObject(0).optLong("timestamp") >= timestamp);
        assertEquals(events.optJSONObject(0).optInt("count"), 1);
        assertEquals(events.optJSONObject(1).optString("error"), "test_error1");
        assertTrue(events.optJSONObject(1).optLong("timestamp") >= timestamp);
        assertEquals(events.optJSONObject(1).optInt("count"), 1);
        assertEquals(events.optJSONObject(2).optString("error"), "test_error2");
        assertTrue(events.optJSONObject(2).optLong("timestamp") >= timestamp);
        assertEquals(events.optJSONObject(2).optInt("count"), 1);

        // verify flushing
        assertEquals(logger.unsentErrors.size(), 0);
        assertEquals(logger.unsentErrorStrings.size(), 0);
    }

    @Test
    public void testLoggingException() {
        logger.enableLogging(httpClient, apiKey, deviceId);

        try {
            testStackTraceMethod();
        } catch (Exception e) {
            logger.logError("failed to run method", e);
        }
        try {
            testStackTraceMethod();
        } catch (Exception e) {
            logger.logError("failed to run method", e);
        }
        looper.runToEndOfTasks();

        assertEquals(logger.unsentErrors.size(), 1);
        assertEquals(logger.unsentErrorStrings.get(0), "failed to run method");
        JSONObject event = logger.unsentErrors.get("failed to run method");
        assertEquals(event.optString("error"), "failed to run method");
        assertTrue(event.optString("stack_trace").startsWith("java.sql.SQLException: this is an exception inside test stack trace"));
        assertEquals(event.optInt("count"), 2);
    }
}
