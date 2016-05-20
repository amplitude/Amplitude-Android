package com.amplitude.api;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SessionTest extends BaseTest {

    // allows for control of System.currentTimeMillis
    private class AmplitudeCallbacksWithTime extends AmplitudeCallbacks {

        private int index;
        private long [] timestamps = null;

        public AmplitudeCallbacksWithTime(AmplitudeClient client, long [] timestamps) {
            super(client);
            this.index = 0;
            this.timestamps = timestamps;
        }

        @Override
        protected long getCurrentTimeMillis() {
            return timestamps[index++ % timestamps.length];
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testDefaultStartSession() {
        long timestamp = System.currentTimeMillis();
        amplitude.logEventAsync("test", null, null, null, null, timestamp, false);
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
        amplitude.logEventAsync("test1", null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        // log 2nd event past timeout, verify new session started
        long timestamp2 = timestamp1 + sessionTimeoutMillis;
        amplitude.logEventAsync("test2", null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);

        JSONArray events = getUnsentEvents(2);
        JSONObject event1 = events.optJSONObject(0);
        JSONObject event2 = events.optJSONObject(1);

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp2));

        // also test getSessionId
        assertEquals(amplitude.getSessionId(), timestamp2);
    }

    @Test
    public void testDefaultExtendSession() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        amplitude.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 3 events all just within session expiration window, verify all in same session
        long timestamp1 = System.currentTimeMillis();
        amplitude.logEventAsync("test1", null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test2", null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);

        long timestamp3 = timestamp2 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test3", null, null, null, null, timestamp3, false);
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
        amplitude.logEventAsync("test", null, null, null, null, timestamp, false);
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
        amplitude.logEvent("test", null, null, null, null, timestamp, false);
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
        amplitude.logEventAsync("test1", null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        // log 2nd event past timeout, verify new session started
        long timestamp2 = timestamp1 + sessionTimeoutMillis;
        amplitude.logEventAsync("test2", null, null, null, null, timestamp2, false);
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
        amplitude.logEvent("test1", null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        // log 2nd event past timeout, verify new session started
        long timestamp2 = timestamp1 + sessionTimeoutMillis;
        amplitude.logEvent("test2", null, null, null, null, timestamp2, false);
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
        amplitude.logEventAsync("test1", null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test2", null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        long timestamp3 = timestamp2 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test3", null, null, null, null, timestamp3, false);
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
        amplitude.logEvent("test1", null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        amplitude.logEvent("test2", null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        long timestamp3 = timestamp2 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("test3", null, null, null, null, timestamp3, false);
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
        assertFalse(amplitude.isUsingForegroundTracking());
        AmplitudeCallbacks callBacks = new AmplitudeCallbacks(amplitude);
        assertTrue(amplitude.isUsingForegroundTracking());
    }

    @Test
    public void testAccurateOnResumeStartSession() {
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp};
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertFalse(amplitude.isInForeground());

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertTrue(amplitude.isInForeground());
        assertEquals(amplitude.getPreviousSessionId(), timestamp);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), timestamp);
    }

    @Test
    public void testAccurateOnResumeStartSessionWithTracking() {
        amplitude.trackSessionEvents(true);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp};
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertFalse(amplitude.isInForeground());
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertTrue(amplitude.isInForeground());
        assertEquals(amplitude.getPreviousSessionId(), timestamp);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamp);

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
                String.valueOf(timestamp)
        );
        assertEquals(
            startSession.optString("timestamp"),
            String.valueOf(timestamp)
        );
    }

    @Test
    public void testAccurateOnPauseRefreshTimestamp() {
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp, timestamp + minTimeBetweenSessionsMillis};
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), timestamps[0]);

        callBacks.onActivityPaused(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), timestamps[1]);
        assertFalse(amplitude.isInForeground());
    }

    @Test
    public void testAccurateOnPauseRefreshTimestampWithTracking() {
        amplitude.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        amplitude.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp, timestamp + minTimeBetweenSessionsMillis};
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamps[0]);
        assertEquals(getUnsentEventCount(), 1);

        // only refresh time, no session checking
        callBacks.onActivityPaused(null);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamps[1]);
        assertEquals(getUnsentEventCount(), 1);
    }

    @Test
    public void testAccurateOnResumeTriggerNewSession() {
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        amplitude.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
                timestamp,
                timestamp + 1,
                timestamp + 1 + minTimeBetweenSessionsMillis
        };
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), timestamps[0]);
        assertEquals(getUnsentEventCount(), 0);
        assertTrue(amplitude.isInForeground());

        // only refresh time, no session checking
        callBacks.onActivityPaused(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), timestamps[1]);
        assertEquals(getUnsentEventCount(), 0);
        assertFalse(amplitude.isInForeground());

        // resume after min session expired window, verify new session started
        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[2]);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), timestamps[2]);
        assertEquals(getUnsentEventCount(), 0);
        assertTrue(amplitude.isInForeground());
    }

    @Test
    public void testAccurateOnResumeTriggerNewSessionWithTracking() {
        amplitude.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        amplitude.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
                timestamp,
                timestamp + 1,
                timestamp + 1 + minTimeBetweenSessionsMillis
        };
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamps[0]);
        assertEquals(getUnsentEventCount(), 1);
        assertTrue(amplitude.isInForeground());

        // only refresh time, no session checking
        callBacks.onActivityPaused(null);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamps[1]);
        assertEquals(getUnsentEventCount(), 1);
        assertFalse(amplitude.isInForeground());

        // resume after min session expired window, verify new session started
        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[2]);
        assertEquals(amplitude.getLastEventId(), 3);
        assertEquals(amplitude.getLastEventTime(), timestamps[2]);
        assertEquals(getUnsentEventCount(), 3);
        assertTrue(amplitude.isInForeground());

        JSONArray events = getUnsentEvents(3);
        JSONObject startSession1 = events.optJSONObject(0);
        JSONObject endSession = events.optJSONObject(1);
        JSONObject startSession2 = events.optJSONObject(2);

        assertEquals(startSession1.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
            startSession1.optJSONObject("api_properties").optString("special"),
            AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession1.optString("session_id"), String.valueOf(timestamps[0]));
        assertEquals(startSession1.optString("timestamp"), String.valueOf(timestamps[0]));

        assertEquals(endSession.optString("event_type"), AmplitudeClient.END_SESSION_EVENT);
        assertEquals(
                endSession.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.END_SESSION_EVENT
        );
        assertEquals(endSession.optString("session_id"), String.valueOf(timestamps[0]));
        assertEquals(endSession.optString("timestamp"), String.valueOf(timestamps[1]));

        assertEquals(startSession2.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                startSession2.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession2.optString("session_id"), String.valueOf(timestamps[2]));
        assertEquals(startSession2.optString("timestamp"), String.valueOf(timestamps[2]));
    }

    @Test
    public void testAccurateOnResumeExtendSession() {
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        amplitude.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
                timestamp,
                timestamp + 1,
                timestamp + 1 + minTimeBetweenSessionsMillis - 1  // just inside session exp window
        };
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), timestamps[0]);

        callBacks.onActivityPaused(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), timestamps[1]);
        assertFalse(amplitude.isInForeground());

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), timestamps[2]);
        assertTrue(amplitude.isInForeground());
    }

    @Test
    public void testAccurateOnResumeExtendSessionWithTracking() {
        amplitude.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        amplitude.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
                timestamp,
                timestamp + 1,
                timestamp + 1 + minTimeBetweenSessionsMillis - 1  // just inside session exp window
        };
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamps[0]);
        assertEquals(getUnsentEventCount(), 1);

        callBacks.onActivityPaused(null);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamps[1]);
        assertFalse(amplitude.isInForeground());
        assertEquals(getUnsentEventCount(), 1);

        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamps[2]);
        assertTrue(amplitude.isInForeground());
        assertEquals(getUnsentEventCount(), 1);

        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
            event.optJSONObject("api_properties").optString("special"),
            AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(event.optString("session_id"), String.valueOf(timestamps[0]));
        assertEquals(event.optString("timestamp"), String.valueOf(timestamps[0]));
    }

    @Test
    public void testAccurateLogAsyncEvent() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        amplitude.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp + minTimeBetweenSessionsMillis - 1};
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertEquals(getUnsentEventCount(), 0);
        assertFalse(amplitude.isInForeground());

        // logging an event before onResume will force a session check
        amplitude.logEventAsync("test", null, null, null, null, timestamp, false);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamp);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamp);
        assertEquals(getUnsentEventCount(), 1);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamp);
        assertEquals(amplitude.getLastEventId(), 1);
        assertEquals(amplitude.getLastEventTime(), timestamps[0]);
        assertEquals(getUnsentEventCount(), 1);
        assertTrue(amplitude.isInForeground());

        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test");
        assertEquals(event.optString("session_id"), String.valueOf(timestamp));
        assertEquals(event.optString("timestamp"), String.valueOf(timestamp));
    }

    @Test
    public void testAccurateLogAsyncEventWithTracking() {
        amplitude.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        amplitude.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp + minTimeBetweenSessionsMillis};
        AmplitudeCallbacks callBacks = new AmplitudeCallbacksWithTime(amplitude, timestamps);

        assertEquals(amplitude.getPreviousSessionId(), -1);
        assertEquals(amplitude.getLastEventId(), -1);
        assertEquals(amplitude.getLastEventTime(), -1);
        assertEquals(getUnsentEventCount(), 0);
        assertFalse(amplitude.isInForeground());

        // logging an event before onResume will force a session check
        amplitude.logEventAsync("test", null, null, null, null, timestamp, false);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamp);
        assertEquals(amplitude.getLastEventId(), 2);
        assertEquals(amplitude.getLastEventTime(), timestamp);
        assertEquals(getUnsentEventCount(), 2);

        // onResume after session expires will start new session
        callBacks.onActivityResumed(null);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), timestamps[0]);
        assertEquals(amplitude.getLastEventId(), 4);
        assertEquals(amplitude.getLastEventTime(), timestamps[0]);
        assertEquals(getUnsentEventCount(), 4);
        assertTrue(amplitude.isInForeground());

        JSONArray events = getUnsentEvents(4);
        JSONObject startSession1 = events.optJSONObject(0);
        JSONObject event = events.optJSONObject(1);
        JSONObject endSession = events.optJSONObject(2);
        JSONObject startSession2 = events.optJSONObject(3);

        assertEquals(startSession1.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
            startSession1.optJSONObject("api_properties").optString("special"),
            AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession1.optString("session_id"), String.valueOf(timestamp));
        assertEquals(startSession1.optString("timestamp"), String.valueOf(timestamp));

        assertEquals(event.optString("event_type"), "test");
        assertEquals(event.optString("session_id"), String.valueOf(timestamp));
        assertEquals(event.optString("timestamp"), String.valueOf(timestamp));

        assertEquals(endSession.optString("event_type"), AmplitudeClient.END_SESSION_EVENT);
        assertEquals(
                endSession.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.END_SESSION_EVENT
        );
        assertEquals(endSession.optString("session_id"), String.valueOf(timestamp));
        assertEquals(endSession.optString("timestamp"), String.valueOf(timestamp));

        assertEquals(startSession2.optString("event_type"), AmplitudeClient.START_SESSION_EVENT);
        assertEquals(
                startSession2.optJSONObject("api_properties").optString("special"),
                AmplitudeClient.START_SESSION_EVENT
        );
        assertEquals(startSession2.optString("session_id"), String.valueOf(timestamps[0]));
        assertEquals(startSession2.optString("timestamp"), String.valueOf(timestamps[0]));
    }


    @Test
    public void testLogOutOfSessionEvent() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        long sessionTimeoutMillis = 5*1000; //1s
        amplitude.setSessionTimeoutMillis(sessionTimeoutMillis);

        long timestamp1 = System.currentTimeMillis();
        amplitude.logEventAsync("test1", null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        // log out of session event just within session expiration window
        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        amplitude.logEventAsync("outOfSession", null, null, null, null, timestamp2, true);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);

        // out of session events do not extend session, 2nd event will start new session
        long timestamp3 = timestamp1 + sessionTimeoutMillis;
        amplitude.logEventAsync("test2", null, null, null, null, timestamp3, false);
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
