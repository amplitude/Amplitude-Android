package com.amplitude.api;

import org.json.JSONArray;
import org.json.JSONException;
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
        assertTrue(logger.unsentEvents.isEmpty());
    }

    @Test
    public void testEnableLogging() {
        logger.enableLogging(httpClient, apiKey);
        assertTrue(logger.enabled);
    }

    @Test
    public void testResize() {
        logger.enableLogging(httpClient, apiKey);
        for (int i = 0; i < Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT + 1; i++) {
            logger.logError("test " + String.valueOf(i));
        }
        looper.runToEndOfTasks();
        assertEquals(logger.unsentEvents.size(), Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT + 1);

        // resize and verify that we enforce minimum value
        logger.setDiagnosticEventMaxCount(Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT - 1);
        assertEquals(logger.diagnosticEventMaxCount, Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT);
        assertEquals(logger.unsentEvents.size(), Diagnostics.DIAGNOSTIC_EVENT_MIN_COUNT);

        // verify we truncated from start of list
        assertEquals(logger.unsentEvents.get(0).optString("error"), "java.lang.Exception: test 1");

        // verify that on next log error, we truncate
        logger.logError("test");
        looper.runToEndOfTasks();
        assertEquals(logger.unsentEvents.size(), 1);
        assertEquals(logger.unsentEvents.get(0).optString("error"), "java.lang.Exception: test");

        // verify safe to resize to greater
        logger.setDiagnosticEventMaxCount(Diagnostics.DIAGNOSTIC_EVENT_MAX_COUNT + 1);
        assertEquals(logger.diagnosticEventMaxCount, Diagnostics.DIAGNOSTIC_EVENT_MAX_COUNT);
        assertEquals(logger.unsentEvents.size(), 1);
        assertEquals(logger.unsentEvents.get(0).optString("error"), "java.lang.Exception: test");
    }

    @Test
    public void testLogError() {
        logger.enableLogging(httpClient, apiKey);

        long timestamp = System.currentTimeMillis();
        logger.logError("test_error");
        logger.logError("test_error1");
        logger.logError("test_error2");
        looper.runToEndOfTasks();
        assertEquals(logger.unsentEvents.size(), 3);

        assertEquals(logger.unsentEvents.get(0).optString("error"), "java.lang.Exception: test_error");
        assertTrue(logger.unsentEvents.get(0).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentEvents.get(1).optString("error"), "java.lang.Exception: test_error1");
        assertTrue(logger.unsentEvents.get(1).optLong("timestamp") >= timestamp);
        assertEquals(logger.unsentEvents.get(2).optString("error"), "java.lang.Exception: test_error2");
        assertTrue(logger.unsentEvents.get(2).optLong("timestamp") >= timestamp);

        // test truncation
        logger.setDiagnosticEventMaxCount(7);
        logger.logError("test_error3");
        logger.logError("test_error4");
        logger.logError("test_error5");
        logger.logError("test_error6");
        logger.logError("test_error7");
        looper.runToEndOfTasks();

        // logged 8 events, but removed 5, so 3 left
        assertEquals(logger.unsentEvents.size(), 3);
        assertEquals(logger.unsentEvents.get(0).optString("error"), "java.lang.Exception: test_error5");
        assertEquals(logger.unsentEvents.get(1).optString("error"), "java.lang.Exception: test_error6");
        assertEquals(logger.unsentEvents.get(2).optString("error"), "java.lang.Exception: test_error7");
    }

    @Test
    public void testDisabled() {
        logger.enableLogging(httpClient, apiKey).disableLogging();
        logger.logError("test_error");
        logger.logError("test_error1");
        logger.logError("test_error2");
        looper.runToEndOfTasks();

        assertEquals(logger.unsentEvents.size(), 0);
    }

    @Test
    public void testUploadEvents() throws JSONException {
        logger.enableLogging(httpClient, apiKey);

        long timestamp = System.currentTimeMillis();
        logger.logError("test_error");
        logger.logError("test_error1");
        logger.logError("test_error2");
        looper.runToEndOfTasks();

        logger.flushEvents();
        RecordedRequest request = runRequest();
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.optJSONObject(0).optString("error"), "java.lang.Exception: test_error");
        assertTrue(events.optJSONObject(0).optLong("timestamp") >= timestamp);
        assertEquals(events.optJSONObject(1).optString("error"), "java.lang.Exception: test_error1");
        assertTrue(events.optJSONObject(1).optLong("timestamp") >= timestamp);
        assertEquals(events.optJSONObject(2).optString("error"), "java.lang.Exception: test_error2");
        assertTrue(events.optJSONObject(2).optLong("timestamp") >= timestamp);

        // verify flushing
        assertEquals(logger.unsentEvents.size(), 0);
    }

    @Test
    public void testLoggingException() {
        logger.enableLogging(httpClient, apiKey);

        try {
            testStackTraceMethod();
        } catch (Exception e) {
            logger.logException(e);
        }
        looper.runToEndOfTasks();
    }
}
