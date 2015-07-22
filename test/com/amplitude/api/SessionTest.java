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
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));

        assertEquals(event3.optString("event_type"), "test3");
        assertEquals(event3.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event3.optString("timestamp"), String.valueOf(timestamp3));
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

        // verify order of synchronous events
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
    public void testDefaultTriggerNewSessionWithTracking() {
        amplitude.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        amplitude.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 1st event, initialize first session
        long timestamp1 = System.currentTimeMillis();
        amplitude.logEventAsync("test1", null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        // log 2nd event past timeout, verify new session started
        long timestamp2 = timestamp1 + sessionTimeoutMillis;
        amplitude.logEventAsync("test2", null, null, timestamp2, false);
        looper.runToEndOfTasks();
        // trackSessions is true, end_session and start_session events are added
        assertEquals(getUnsentEventCount(), 5);

        JSONArray events = getUnsentEvents(5);
        JSONObject startSession1 = events.optJSONObject(0);
        JSONObject event1 = events.optJSONObject(1);
        JSONObject endSession = events.optJSONObject(2);
        JSONObject startSession2 = events.optJSONObject(3);
        JSONObject event2 = events.optJSONObject(4);

        assertEquals(startSession1.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                startSession1.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession1.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(endSession.optString("event_type"), AmplitudeClient.END_SESSION_EVENT);
        assertEquals(
                endSession.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.END_SESSION_EVENT
        );
        assertEquals(endSession.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(startSession2.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                startSession2.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession2.optString("session_id"), String.valueOf(timestamp2));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp2));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));
    }

    @Test
    public void testDefaultTriggerNewSessionWithTrackingSynchronous() {
        amplitude.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        amplitude.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 1st event, initialize first session
        long timestamp1 = System.currentTimeMillis();
        amplitude.logEvent("test1", null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        // log 2nd event past timeout, verify new session started
        long timestamp2 = timestamp1 + sessionTimeoutMillis;
        amplitude.logEvent("test2", null, null, timestamp2, false);
        looper.runToEndOfTasks();
        // trackSessions is true, end_session and start_session events are added
        assertEquals(getUnsentEventCount(), 5);

        // verify order of synchronous events
        JSONArray events = getUnsentEvents(5);
        JSONObject startSession1 = events.optJSONObject(0);
        JSONObject event1 = events.optJSONObject(1);
        JSONObject endSession = events.optJSONObject(2);
        JSONObject startSession2 = events.optJSONObject(3);
        JSONObject event2 = events.optJSONObject(4);

        assertEquals(startSession1.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                startSession1.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession1.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(endSession.optString("event_type"), AmplitudeClient.END_SESSION_EVENT);
        assertEquals(
                endSession.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.END_SESSION_EVENT
        );
        assertEquals(endSession.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(startSession2.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                startSession2.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession2.optString("session_id"), String.valueOf(timestamp2));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp2));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));
    }

    @Test
    public void testDefaultExtendSessionWithTracking() {
        amplitude.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        amplitude.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 3 events all just within session expiration window, verify all in same session
        long timestamp1 = System.currentTimeMillis();
        amplitude.logEventAsync("test1", null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test2", null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        long timestamp3 = timestamp2 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test3", null, null, timestamp3, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 4);

        JSONArray events = getUnsentEvents(4);
        JSONObject startSession = events.optJSONObject(0);
        JSONObject event1 = events.optJSONObject(1);
        JSONObject event2 = events.optJSONObject(2);
        JSONObject event3 = events.optJSONObject(3);

        assertEquals(startSession.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                startSession.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));

        assertEquals(event3.optString("event_type"), "test3");
        assertEquals(event3.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event3.optString("timestamp"), String.valueOf(timestamp3));
    }

    @Test
    public void testDefaultExtendSessionWithTrackingSynchronous() {
        amplitude.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        amplitude.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 3 events all just within session expiration window, verify all in same session
        long timestamp1 = System.currentTimeMillis();
        amplitude.logEvent("test1", null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        amplitude.logEvent("test2", null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        long timestamp3 = timestamp2 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test3", null, null, timestamp3, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 4);

        // verify order of synchronous events
        JSONArray events = getUnsentEvents(4);
        JSONObject startSession = events.optJSONObject(0);
        JSONObject event1 = events.optJSONObject(1);
        JSONObject event2 = events.optJSONObject(2);
        JSONObject event3 = events.optJSONObject(3);

        assertEquals(startSession.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                startSession.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));

        assertEquals(event3.optString("event_type"), "test3");
        assertEquals(event3.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event3.optString("timestamp"), String.valueOf(timestamp3));
    }

    @Test
    public void testEnableAccurateTracking() {
        assertFalse(amplitude.isUsingAccurateTracking());
        AmplitudeCallbacks callBacks = new AmplitudeCallbacks(amplitude);
        assertTrue(amplitude.isUsingAccurateTracking());
    }

    @Test
    public void testAccurateStartSession() {
        AmplitudeCallbacks callBacks = new AmplitudeCallbacks(amplitude);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertFalse(amplitude.isInForeground());

        callBacks.onActivityResumed(null);
        assertTrue(amplitude.isInForeground());
        assertTrue(amplitude.getPreviousSessionId() > -1);
        assertTrue(amplitude.getLastEventTime() > -1);
        assertEquals(amplitude.getLastEventId(), -1);
    }

    @Test
    public void testAccurateStartSessionWithTracking() {
        amplitude.trackSessionEvents(true);
        AmplitudeCallbacks callBacks = new AmplitudeCallbacks(amplitude);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertFalse(amplitude.isInForeground());
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertTrue(amplitude.isInForeground());
        assertTrue(amplitude.getPreviousSessionId() > -1);
        assertTrue(amplitude.getLastEventTime() > -1);
        assertTrue(amplitude.getLastEventId() > -1);

        // verify that start session event sent
        assertEquals(getUnsentEventCount(), 1);
        JSONObject startSession = getLastUnsentEvent();
        assertEquals(startSession.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
            startSession.optJSONObject("api_properties").optString("special"),
            AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(
            startSession.optString("session_id"),
            String.valueOf(amplitude.getPreviousSessionId())
        );
        assertEquals(
            startSession.optString("timestamp"),
            String.valueOf(amplitude.getLastEventTime())
        );
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
}
