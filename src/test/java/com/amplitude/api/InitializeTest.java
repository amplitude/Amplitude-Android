package com.amplitude.api;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class InitializeTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testInitializeUserId() {
        // the userId passed to initialize should override any existing values

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyValue(AmplitudeClient.USER_ID_KEY, "oldUserId");

        String userId = "newUserId";
        amplitude.initialize(context, apiKey, userId);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        // Test that the user id is set.
        assertEquals(userId, amplitude.userId);
        assertEquals(userId, dbHelper.getValue(AmplitudeClient.USER_ID_KEY));

        // Test that events are logged.
        RecordedRequest request = sendEvent(amplitude, "init_test_event", null);
        assertNotNull(request);
    }

    @Test
    public void testInitializeUserIdFromSharedPrefs() {
        String userId = "testUserId";

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertNull(dbHelper.getValue(AmplitudeClient.USER_ID_KEY));

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        amplitude.setUserId(userId);

        // Test that the user id is set.
        assertEquals(amplitude.userId, userId);
        assertEquals(userId, dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
    }

    @Test
    public void testInitializeUserIdFromDb() {
        // since user id already exists in database, ignore old value in shared prefs
        String userId = "testUserId";

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyValue(AmplitudeClient.USER_ID_KEY, userId);

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        // Test that the user id is set.
        assertEquals(amplitude.userId, userId);
        assertEquals(userId, dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
    }

    @Test
    public void testInitializeOptOut() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertNull(dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY));

        amplitude.initialize(context, apiKey);
        looper.runOneTask();

        assertFalse(amplitude.isOptedOut());

        amplitude.setOptOut(true);
        looper.runOneTask();

        assertTrue(amplitude.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY), 1L);
    }

    @Test
    public void testInitializeOptOutFromDB() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.OPT_OUT_KEY, 0L);

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        assertFalse(amplitude.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY), 0L);
    }


    @Test
    public void testInitializeLastEventId() throws JSONException {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        assertEquals(amplitude.lastEventId, 3L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY), 3L);

        amplitude.logEvent("testEvent");
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);

        assertEquals(events.getJSONObject(0).getLong("event_id"), 1L);

        assertEquals(amplitude.lastEventId, 1L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY), 1L);
    }

    @Test
    public void testInitializePreviousSessionId() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY), 4000L);
    }

    @Test
    public void testInitializeLastEventTime() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY, 5000L);

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        assertEquals(amplitude.lastEventTime, 5000L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY), 5000L);
    }

    @Test
    public void testSkipSharedPrefsToDb() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyValue(AmplitudeClient.DEVICE_ID_KEY, "testDeviceId");
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY, 1000L);
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY, 2000L);

        assertNull(dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY));

        amplitude.initialize(context, apiKey);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runOneTask();
        looper.runToEndOfTasks();

        assertEquals(dbHelper.getValue(AmplitudeClient.DEVICE_ID_KEY), "testDeviceId");
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY), 1000L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY), 2000L);
        assertNull(dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY));

        // after upgrade, pref values still there since they weren't deleted
        assertEquals(amplitude.deviceId, "testDeviceId");
        assertEquals(amplitude.previousSessionId, 1000L);
        assertEquals(amplitude.lastEventTime, 2000L);
        assertNull(amplitude.userId);
    }

    @Test
    public void testInitializePreviousSessionIdLastEventTime() {
        // set a previous session id & last event time
        // log an event with timestamp such that same session is continued
        // log second event with timestamp such that new session is started

        amplitude.setSessionTimeoutMillis(5000); // 5s

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY, 7000L);

        long [] timestamps = {8000, 14000};
        clock.setTimestamps(timestamps);

        amplitude.initialize(context, apiKey);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runOneTask();
        looper.runToEndOfTasks();

        assertNull(amplitude.userId);

        // log first event
        amplitude.logEvent("testEvent1");
        looper.runToEndOfTasks();
        assertEquals(amplitude.previousSessionId, 6000L);
        assertEquals(amplitude.lastEventTime, 8000L);

        // log second event
        amplitude.logEvent("testEvent2");
        looper.runToEndOfTasks();
        assertEquals(amplitude.previousSessionId, 14000L);
        assertEquals(amplitude.lastEventTime, 14000L);
    }

    @Test
    public void testReloadDeviceIdFromDatabase() {
        String deviceId = "test_device_id_from_database";
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        DatabaseHelper.getDatabaseHelper(context).insertOrReplaceKeyValue(
            AmplitudeClient.DEVICE_ID_KEY, deviceId
        );

        amplitude.initialize(context, apiKey);
        looper.runToEndOfTasks();
        assertEquals(deviceId, amplitude.getDeviceId());
    }

    @Test
    public void testInitializeDeviceIdWithRandomUUID() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        amplitude.initialize(context, apiKey);
        looper.runToEndOfTasks();

        String deviceId = amplitude.getDeviceId();
        assertEquals(37, deviceId.length());
        assertTrue(deviceId.endsWith("R"));
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertEquals(deviceId, dbHelper.getValue(AmplitudeClient.DEVICE_ID_KEY));
    }
}
