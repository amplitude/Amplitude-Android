package com.amplitude.api;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
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
        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_USER_ID, "oldestUserId").commit();

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

        // verified shared prefs not deleted
        assertEquals(
            prefs.getString(Constants.PREFKEY_USER_ID, null),
            "oldestUserId"
        );
    }

    @Test
    public void testInitializeUserIdFromSharedPrefs() {
        String userId = "testUserId";
        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_USER_ID, userId).commit();

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertNull(dbHelper.getValue(AmplitudeClient.USER_ID_KEY));

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        // Test that the user id is set.
        assertEquals(amplitude.userId, userId);
        assertEquals(userId, dbHelper.getValue(AmplitudeClient.USER_ID_KEY));

        // verify shared prefs deleted
        assertNull(prefs.getString(Constants.PREFKEY_USER_ID, null));
    }

    @Test
    public void testInitializeUserIdFromDb() {
        // since user id already exists in database, ignore old value in shared prefs
        String userId = "testUserId";
        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_USER_ID, "oldUserId").commit();

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyValue(AmplitudeClient.USER_ID_KEY, userId);

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        // Test that the user id is set.
        assertEquals(amplitude.userId, userId);
        assertEquals(userId, dbHelper.getValue(AmplitudeClient.USER_ID_KEY));

        // verify that shared prefs not deleted
        assertEquals("oldUserId", prefs.getString(Constants.PREFKEY_USER_ID, null));
    }

    @Test
    public void testInitializeOptOut() {
        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Constants.PREFKEY_OPT_OUT, true).commit();

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertNull(dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY));

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        assertTrue(amplitude.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY), 1L);

        amplitude.setOptOut(false);
        assertFalse(amplitude.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY), 0L);

        // verify shared prefs deleted
        assertFalse(prefs.getBoolean(Constants.PREFKEY_OPT_OUT, false));
    }

    @Test
    public void testInitializeOptOutFromDB() {
        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Constants.PREFKEY_OPT_OUT, true).commit();

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.OPT_OUT_KEY, 0L);

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        assertFalse(amplitude.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY), 0L);

        // verify shared prefs not deleted
        assertTrue(prefs.getBoolean(Constants.PREFKEY_OPT_OUT, false));
    }


    @Test
    public void testInitializeLastEventId() throws JSONException {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);

        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putLong(Constants.PREFKEY_LAST_EVENT_ID, 3L).commit();

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        assertEquals(amplitude.getLastEventId(), 3L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY), 3L);

        amplitude.logEvent("testEvent");
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);

        assertEquals(events.getJSONObject(0).getLong("event_id"), 1L);

        assertEquals(amplitude.getLastEventId(), 1L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY), 1L);

        // verify shared prefs deleted
        assertEquals(prefs.getLong(Constants.PREFKEY_LAST_EVENT_ID, -1), -1);
    }

    @Test
    public void testInitializePreviousSessionId() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);

        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, 4000L).commit();

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        assertEquals(amplitude.sessionId, 4000L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY), 4000L);

        // verify shared prefs deleted
        assertEquals(prefs.getLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, -1), -1);
    }

    @Test
    public void testInitializeLastEventTime() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY, 5000L);

        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putLong(Constants.PREFKEY_LAST_EVENT_TIME, 4000L).commit();

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        assertEquals(amplitude.getLastEventTime(), 5000L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY), 5000L);

        // verify shared prefs deleted
        assertEquals(prefs.getLong(Constants.PREFKEY_LAST_EVENT_TIME, -1), 4000L);
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

        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_DEVICE_ID, "otherDeviceId").commit();
        prefs.edit().putString(Constants.PREFKEY_USER_ID, "testUserId").commit();
        prefs.edit().putBoolean(Constants.PREFKEY_OPT_OUT, true).commit();
        prefs.edit().putLong(Constants.PREFKEY_LAST_IDENTIFY_ID, 3000L).commit();

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

        assertEquals(prefs.getString(Constants.PREFKEY_DEVICE_ID, null), "otherDeviceId");
        assertEquals(prefs.getString(Constants.PREFKEY_USER_ID, null), "testUserId");
        assertTrue(prefs.getBoolean(Constants.PREFKEY_OPT_OUT, false));
        assertEquals(prefs.getLong(Constants.PREFKEY_LAST_IDENTIFY_ID, -1), 3000L);

        // after upgrade, pref values still there since they weren't deleted
        assertEquals(amplitude.deviceId, "testDeviceId");
        assertEquals(amplitude.getPreviousSessionId(), 1000L);
        assertEquals(amplitude.getLastEventTime(), 2000L);
        assertNull(amplitude.userId);
    }

    @Test
    public void testInitializePreviousSessionIdLastEventTime() {
        // set a previous session id & last event time
        // log an event with timestamp such that same session is continued
        // log second event with timestamp such that new session is started

        amplitude.setSessionTimeoutMillis(5000); // 5s

        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_DEVICE_ID, "testDeviceId").commit();
        prefs.edit().putLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, 6000L).commit();
        prefs.edit().putLong(Constants.PREFKEY_LAST_EVENT_TIME, 6000L).commit();

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY, 7000L);

        long [] timestamps = {8000, 14000};
        clock.setTimestamps(timestamps);

        amplitude.initialize(context, apiKey);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runOneTask();
        looper.runToEndOfTasks();

        assertEquals(amplitude.deviceId, "testDeviceId");
        assertEquals(amplitude.getPreviousSessionId(), 6000L);
        assertEquals(amplitude.getLastEventTime(), 7000L);
        assertNull(amplitude.userId);

        // log first event
        amplitude.logEvent("testEvent1");
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), 6000L);
        assertEquals(amplitude.getLastEventTime(), 8000L);

        // log second event
        amplitude.logEvent("testEvent2");
        looper.runToEndOfTasks();
        assertEquals(amplitude.getPreviousSessionId(), 14000L);
        assertEquals(amplitude.getLastEventTime(), 14000L);
    }
}
