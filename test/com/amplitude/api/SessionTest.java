package com.amplitude.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    // ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
    @Test
    public void testDefaultStartSession() {
        long timestamp = System.currentTimeMillis();
        amplitude.logEventAsync("test", null, null, timestamp, false);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        // trackSessionEvents is false, no start_session event added
        assertEquals(getUnsentEventCount(), 1);
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test");
        assertEquals(event.optString("session_id"), String.valueOf(timestamp));
    }

    @Test
    public void testDefaultTriggerNewSession() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        amplitude.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 1st event, initialize first session
        long timestamp1 = System.currentTimeMillis();
        amplitude.logEventAsync("test1", null, null, timestamp1, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        // log 2nd event past timeout, verify new session started
        long timestamp2 = timestamp1 + sessionTimeoutMillis;
        amplitude.logEventAsync("test2", null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);

        JSONArray events = getUnsentEvents(2);
        JSONObject event1 = events.optJSONObject(0);
        JSONObject event2 = events.optJSONObject(1);

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp2));
    }

    @Test
    public void testDefaultExtendSession() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        amplitude.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 3 events all just within session expiration window, verify all in same session
        long timestamp1 = System.currentTimeMillis();
        amplitude.logEventAsync("test1", null, null, timestamp1, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test2", null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);

        long timestamp3 = timestamp2 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test3", null, null, timestamp3, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        JSONArray events = getUnsentEvents(3);
        JSONObject event1 = events.optJSONObject(0);
        JSONObject event2 = events.optJSONObject(1);
        JSONObject event3 = events.optJSONObject(2);

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event3.optString("event_type"), "test3");
        assertEquals(event3.optString("session_id"), String.valueOf(timestamp1));
    }

    @Test
    public void testDefaultStartSessionWithTracking() {
        amplitude.trackSessionEvents(true);

        long timestamp = System.currentTimeMillis();
        amplitude.logEventAsync("test", null, null, timestamp, false);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);
        JSONArray events = getUnsentEvents(2);
        JSONObject session_event = events.optJSONObject(0);
        JSONObject test_event = events.optJSONObject(1);

        assertEquals(session_event.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                session_event.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(session_event.optString("session_id"), String.valueOf(timestamp));

        assertEquals(test_event.optString("event_type"), "test");
        assertEquals(test_event.optString("session_id"), String.valueOf(timestamp));
    }

    @Test
    public void testDefaultStartSessionWithTrackingSynchronous() {
        amplitude.trackSessionEvents(true);

        long timestamp = System.currentTimeMillis();
        amplitude.logEvent("test", null, null, timestamp, false);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);
        JSONArray events = getUnsentEvents(2);
        JSONObject session_event = events.optJSONObject(0);
        JSONObject test_event = events.optJSONObject(1);

        // verify order of synchronous events
        assertEquals(session_event.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                session_event.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(session_event.optString("session_id"), String.valueOf(timestamp));

        assertEquals(test_event.optString("event_type"), "test");
        assertEquals(test_event.optString("session_id"), String.valueOf(timestamp));
    }

    @Test
    public void testLogOutOfSessionEvent() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long sessionTimeoutMillis = 5*1000; //1s
        amplitude.setSessionTimeoutMillis(sessionTimeoutMillis);

        long timestamp1 = System.currentTimeMillis();
        amplitude.logEventAsync("test1", null, null, timestamp1, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        // log out of session event just within session expiration window
        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("outOfSession", null, null, timestamp2, true);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);

        // out of session events do not extend session, 2nd event will start new session
        long timestamp3 = timestamp1 + sessionTimeoutMillis;
        amplitude.logEventAsync("test2", null, null, timestamp3, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        JSONArray events = getUnsentEvents(3);
        JSONObject event1 = events.optJSONObject(0);
        JSONObject outOfSessionEvent = events.optJSONObject(1);
        JSONObject event2 = events.optJSONObject(2);

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(outOfSessionEvent.optString("event_type"), "outOfSession");
        assertEquals(outOfSessionEvent.optString("session_id"), String.valueOf(-1));
        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp3));
    }


    /*
    @Test
    public void testStartSession() {
//        amplitude.startSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//
//        JSONObject event = getLastUnsentEvent();
//        assertEquals(AmplitudeClient.START_SESSION_EVENT, event.optString("event_type"));
//        assertEquals(AmplitudeClient.START_SESSION_EVENT, event.optJSONObject("api_properties").optString("special"));
//
//        assertEquals(1, getUnsentEventCount());
//        assertNotNull(runRequest());
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
//        amplitude.endSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//        assertEquals(0, getUnsentEventCount());
//
//        amplitude.startSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//        amplitude.endSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//
//        JSONObject event = getLastUnsentEvent();
//        assertEquals(AmplitudeClient.END_SESSION_EVENT, event.optString("event_type"));
//        assertEquals(AmplitudeClient.END_SESSION_EVENT, event.optJSONObject("api_properties").optString("special"));
//
//        assertEquals(2, getUnsentEventCount());
//        assertNotNull(runRequest());
    }

    @Test
    public void testContinueSession() {
//        amplitude.startSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//
//        JSONObject event = getLastUnsentEvent();
//        long session_id = event.optLong("session_id");
//
//        amplitude.endSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();
//
//        amplitude.startSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//        assertEquals(1, getUnsentEventCount());
//
//        amplitude.logEvent("event");
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//
//        event = getLastUnsentEvent();
//        assertEquals(session_id, event.optLong("session_id"));
    }

    @Test
    public void testExpiredSession() {
//        amplitude.startSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//
//        JSONObject event = getLastUnsentEvent();
//        long session_id = event.optLong("session_id");
//
//        amplitude.endSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//
//        amplitude.startSession();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//        assertEquals(3, getUnsentEventCount());
//
//        amplitude.logEvent("event");
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
//
//        event = getLastUnsentEvent();
//        assertFalse(session_id == event.optLong("session_id"));
    }
    */
}
