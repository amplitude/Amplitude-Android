package com.amplitude.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import android.content.Context;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SessionTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testStartSession() {
        amplitude.startSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        JSONObject event = getLastUnsentEvent();
        assertEquals(AmplitudeClient.START_SESSION_EVENT, event.optString("event_type"));
        assertEquals(AmplitudeClient.START_SESSION_EVENT, event.optJSONObject("api_properties").optString("special"));

        assertEquals(1, getUnsentEventCount());
        assertNotNull(runRequest());
    }

    @Test
    public void testImplicitStartSession() {
        amplitude.logEvent("implicit_start_session");
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        JSONArray events = getUnsentEvents(2);
        
        JSONObject event = events.optJSONObject(0);
        assertEquals(AmplitudeClient.START_SESSION_EVENT, event.optString("event_type"));
        assertEquals(AmplitudeClient.START_SESSION_EVENT, event.optJSONObject("api_properties").optString("special"));

        event = events.optJSONObject(1);
        assertEquals("implicit_start_session", event.optString("event_type"));

        assertEquals(2, getUnsentEventCount());
        assertNotNull(runRequest());
    }

    @Test
    public void testEndSession() {
        amplitude.endSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(0, getUnsentEventCount());

        amplitude.startSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        amplitude.endSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        JSONObject event = getLastUnsentEvent();
        assertEquals(AmplitudeClient.END_SESSION_EVENT, event.optString("event_type"));
        assertEquals(AmplitudeClient.END_SESSION_EVENT, event.optJSONObject("api_properties").optString("special"));

        assertEquals(2, getUnsentEventCount());
        assertNotNull(runRequest());
    }

    @Test
    public void testContinueSession() {
        amplitude.startSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        JSONObject event = getLastUnsentEvent();
        long session_id = event.optLong("session_id");

        amplitude.endSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        amplitude.startSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(1, getUnsentEventCount());

        amplitude.logEvent("event");
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        event = getLastUnsentEvent();
        assertEquals(session_id, event.optLong("session_id"));
    }

    @Test
    public void testExpiredSession() {
        amplitude.startSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        JSONObject event = getLastUnsentEvent();
        long session_id = event.optLong("session_id");

        amplitude.endSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        amplitude.startSession();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(3, getUnsentEventCount());

        amplitude.logEvent("event");
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        event = getLastUnsentEvent();
        assertFalse(session_id == event.optLong("session_id"));
    }
}
