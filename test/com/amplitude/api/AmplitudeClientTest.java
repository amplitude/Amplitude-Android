package com.amplitude.api;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AmplitudeClientTest extends BaseTest {

    private String generateStringWithLength(int length, char c) {
        if (length < 0) return "";
        char [] array = new char[length];
        Arrays.fill(array, c);
        return new String(array);
    }

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
    public void testConstructor() {
        // verify that the constructor lowercases the instance name
        AmplitudeClient a = new AmplitudeClient("APP1");
        AmplitudeClient b = new AmplitudeClient("New_App_2");

        assertEquals(a.instanceName, "app1");
        assertEquals(b.instanceName, "new_app_2");
    }

    @Test
    public void testSetUserId() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        String userId = "user_id";
        amplitude.setUserId(userId);
        looper.runToEndOfTasks();
        assertEquals(userId, dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
        assertEquals(userId, amplitude.getUserId());

        // try setting to null
        amplitude.setUserId(null);
        looper.runToEndOfTasks();
        assertNull(dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
        assertNull(amplitude.getUserId());
    }

    @Test
    public void testSetUserIdTwice() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        String userId1 = "user_id1";
        String userId2 = "user_id2";

        amplitude.setUserId(userId1);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getUserId(), userId1);
        amplitude.logEvent("event1");
        looper.runToEndOfTasks();

        JSONObject event1 = getLastUnsentEvent();
        assertEquals(event1.optString("event_type"), "event1");
        assertEquals(event1.optString("user_id"), userId1);

        amplitude.setUserId(userId2);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getUserId(), userId2);
        amplitude.logEvent("event2");
        looper.runToEndOfTasks();

        JSONObject event2 = getLastUnsentEvent();
        assertEquals(event2.optString("event_type"), "event2");
        assertEquals(event2.optString("user_id"), userId2);
    }

    @Test
    public void testSetDeviceId() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        String deviceId = amplitude.getDeviceId(); // Randomly generated device ID
        assertNotNull(deviceId);
        assertEquals(deviceId.length(), 36 + 1); // 36 for UUID, + 1 for appended R
        assertEquals(deviceId.charAt(36), 'R');
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), deviceId);


        // test setting invalid device ids
        amplitude.setDeviceId(null);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), deviceId);

        amplitude.setDeviceId("");
        looper.runToEndOfTasks();
        assertEquals(amplitude.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), deviceId);

        amplitude.setDeviceId("9774d56d682e549c");
        looper.runToEndOfTasks();
        assertEquals(amplitude.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), deviceId);

        amplitude.setDeviceId("unknown");
        looper.runToEndOfTasks();
        assertEquals(amplitude.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), deviceId);

        amplitude.setDeviceId("000000000000000");
        looper.runToEndOfTasks();
        assertEquals(amplitude.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), deviceId);

        amplitude.setDeviceId("Android");
        looper.runToEndOfTasks();
        assertEquals(amplitude.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), deviceId);

        amplitude.setDeviceId("DEFACE");
        looper.runToEndOfTasks();
        assertEquals(amplitude.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), deviceId);

        amplitude.setDeviceId("00000000-0000-0000-0000-000000000000");
        assertEquals(amplitude.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), deviceId);

        // set valid device id
        String newDeviceId = UUID.randomUUID().toString();
        amplitude.setDeviceId(newDeviceId);
        looper.runToEndOfTasks();
        assertEquals(amplitude.getDeviceId(), newDeviceId);
        assertEquals(dbHelper.getValue(amplitude.DEVICE_ID_KEY), newDeviceId);

        amplitude.logEvent("test");
        looper.runToEndOfTasks();
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test");
        assertEquals(event.optString("device_id"), newDeviceId);
    }

    @Test
    public void testSetUserProperties() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        // setting null or empty user properties does nothing
        amplitude.setUserProperties(null);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
        amplitude.setUserProperties(new JSONObject());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        JSONObject userProperties = new JSONObject().put("key1", "value1").put("key2", "value2");
        amplitude.setUserProperties(userProperties);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 1);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.IDENTIFY_EVENT, event.optString("event_type"));
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));

        JSONObject userPropertiesOperations = event.optJSONObject("user_properties");
        assertEquals(userPropertiesOperations.length(), 1);
        assertTrue(userPropertiesOperations.has(Constants.AMP_OP_SET));

        JSONObject setOperations = userPropertiesOperations.optJSONObject(Constants.AMP_OP_SET);
        assertTrue(Utils.compareJSONObjects(userProperties, setOperations));
    }

    @Test
    public void testIdentifyMultipleOperations() throws JSONException {
        String property1 = "string value";
        String value1 = "testValue";

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "boolean value";
        boolean value3 = true;

        String property4 = "json value";

        Identify identify = new Identify().setOnce(property1, value1).add(property2, value2);
        identify.set(property3, value3).unset(property4);

        // identify should ignore this since duplicate key
        identify.set(property4, value3);

        amplitude.identify(identify);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(getUnsentIdentifyCount(), 1);
        assertEquals(getUnsentEventCount(), 0);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.IDENTIFY_EVENT, event.optString("event_type"));

        JSONObject userProperties = event.optJSONObject("user_properties");
        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_SET_ONCE, new JSONObject().put(property1, value1));
        expected.put(Constants.AMP_OP_ADD, new JSONObject().put(property2, value2));
        expected.put(Constants.AMP_OP_SET, new JSONObject().put(property3, value3));
        expected.put(Constants.AMP_OP_UNSET, new JSONObject().put(property4, "-"));
        assertTrue(Utils.compareJSONObjects(userProperties, expected));
    }

    @Test
    public void testReloadDeviceIdFromDatabase() {
        String deviceId = "test_device_id";
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        DatabaseHelper.getDatabaseHelper(context).insertOrReplaceKeyValue(
                AmplitudeClient.DEVICE_ID_KEY,
                deviceId
        );

        // force re-initialize to re-load deviceId from DB
        amplitude.initialized = false;
        amplitude.initialize(context, apiKey);
        looper.runToEndOfTasks();
        assertEquals(deviceId, amplitude.getDeviceId());
    }

    @Test
    public void testDoesNotUpgradeDeviceIdFromSharedPrefsToDatabase() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        // initializeDeviceId no longer fetches from SharedPrefs, will get advertising ID instead
        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_DEVICE_ID, "test_device_id").commit();

        looper.getScheduler().advanceToLastPostedRunnable();
        String deviceId = amplitude.getDeviceId();
        assertTrue(deviceId.endsWith("R"));
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertEquals(
                deviceId,
                dbHelper.getValue(AmplitudeClient.DEVICE_ID_KEY)
        );
    }

    @Test
    public void testGetDeviceIdWithoutAdvertisingId() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.getScheduler().advanceToLastPostedRunnable();
        assertNotNull(amplitude.getDeviceId());
        assertEquals(37, amplitude.getDeviceId().length());
        String deviceId = amplitude.getDeviceId();
        assertTrue(deviceId.endsWith("R"));
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertEquals(
                deviceId,
                dbHelper.getValue(AmplitudeClient.DEVICE_ID_KEY)
        );
    }

    @Test
    public void testOptOut() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertFalse(amplitude.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY), 0L);

        amplitude.setOptOut(true);
        looper.runToEndOfTasks();
        assertTrue(amplitude.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY), 1L);
        RecordedRequest request = sendEvent(amplitude, "test_opt_out", null);
        assertNull(request);

        // Event shouldn't be sent event once opt out is turned off.
        amplitude.setOptOut(false);
        looper.runToEndOfTasks();
        assertFalse(amplitude.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY), 0L);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        httplooper.runToEndOfTasks();
        assertNull(request);

        request = sendEvent(amplitude, "test_opt_out", null);
        assertNotNull(request);
    }

    @Test
    public void testOffline() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());

        amplitude.setOffline(true);
        RecordedRequest request = sendEvent(amplitude, "test_offline", null);
        assertNull(request);

        // Events should be sent after offline is turned off.
        amplitude.setOffline(false);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        httplooper.runToEndOfTasks();

        try {
            request = server.takeRequest(1, SECONDS);
        } catch (InterruptedException e) {
        }
        assertNotNull(request);
    }

    @Test
    public void testLogEvent() {
        RecordedRequest request = sendEvent(amplitude, "test_event", null);
        assertNotNull(request);
    }

    @Test
    public void testIdentify() throws JSONException {
        long [] timestamps = {1000, 1001};
        clock.setTimestamps(timestamps);

        RecordedRequest request = sendIdentify(amplitude, new Identify().set("key", "value"));
        assertNotNull(request);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 1);
        JSONObject identify = events.getJSONObject(0);
        assertEquals(identify.getString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(identify.getLong("event_id"), 1);
        assertEquals(identify.getLong("timestamp"), timestamps[0]);
        assertEquals(identify.getLong("sequence_number"), 1);
        JSONObject userProperties = identify.getJSONObject("user_properties");
        assertEquals(userProperties.length(), 1);
        assertTrue(userProperties.has(Constants.AMP_OP_SET));

        JSONObject expected = new JSONObject();
        expected.put("key", "value");
        assertTrue(Utils.compareJSONObjects(userProperties.getJSONObject(Constants.AMP_OP_SET), expected));

        // verify db state
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertNull(dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
        assertEquals((long)dbHelper.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY), 1L);
        assertEquals((long)dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY), -1L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY), 1L);
        assertEquals((long)dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY), timestamps[0]);
    }

    @Test
    public void testNullIdentify() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        amplitude.identify(null);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testLog3Events() throws InterruptedException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();


        amplitude.logEvent("test_event1");
        amplitude.logEvent("test_event2");
        amplitude.logEvent("test_event3");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 3);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONArray events = getUnsentEvents(3);
        for (int i = 0; i < 3; i++) {
            assertEquals(events.optJSONObject(i).optString("event_type"), "test_event" + (i+1));
            assertEquals(events.optJSONObject(i).optLong("timestamp"), timestamps[i]);
            assertEquals(events.optJSONObject(i).optLong("sequence_number"), i+1);
        }

        // send response and check that remove events works properly
        runRequest(amplitude);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testLog3Identifys() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        Robolectric.getForegroundThreadScheduler().advanceTo(1);
        amplitude.identify(new Identify().set("photo_count", 1));
        amplitude.identify(new Identify().add("karma", 2));
        amplitude.identify(new Identify().unset("gender"));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 3);
        JSONArray events = getUnsentIdentifys(3);

        JSONObject expectedIdentify1 = new JSONObject();
        expectedIdentify1.put(Constants.AMP_OP_SET, new JSONObject().put("photo_count", 1));
        JSONObject expectedIdentify2 = new JSONObject();
        expectedIdentify2.put(Constants.AMP_OP_ADD, new JSONObject().put("karma", 2));
        JSONObject expectedIdentify3 = new JSONObject();
        expectedIdentify3.put(Constants.AMP_OP_UNSET, new JSONObject().put("gender", "-"));

        assertEquals(events.optJSONObject(0).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(0).optLong("timestamp"), timestamps[0]);
        assertEquals(events.optJSONObject(0).optLong("sequence_number"), 1);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(0).optJSONObject("user_properties"), expectedIdentify1
        ));
        assertEquals(events.optJSONObject(1).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(1).optLong("timestamp"), timestamps[1]);
        assertEquals(events.optJSONObject(1).optLong("sequence_number"), 2);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(1).optJSONObject("user_properties"), expectedIdentify2
        ));
        assertEquals(events.optJSONObject(2).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(2).optLong("timestamp"), timestamps[2]);
        assertEquals(events.optJSONObject(2).optLong("sequence_number"), 3);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(2).optJSONObject("user_properties"), expectedIdentify3
        ));

        // send response and check that remove events works properly
        runRequest(amplitude);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testLogEventAndIdentify() throws JSONException {
        long [] timestamps = {1, 1, 2};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        amplitude.logEvent("test_event");
        amplitude.identify(new Identify().add("photo_count", 1));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // verify some internal counters
        assertEquals(getUnsentEventCount(), 1);
        assertEquals(amplitude.lastEventId, 1);
        assertEquals(getUnsentIdentifyCount(), 1);
        assertEquals(amplitude.lastIdentifyId, 1);

        JSONArray unsentEvents = getUnsentEvents(1);
        assertEquals(unsentEvents.optJSONObject(0).optString("event_type"), "test_event");
        assertEquals(unsentEvents.optJSONObject(0).optLong("sequence_number"), 1);

        JSONObject expectedIdentify = new JSONObject();
        expectedIdentify.put(Constants.AMP_OP_ADD, new JSONObject().put("photo_count", 1));

        JSONArray unsentIdentifys = getUnsentIdentifys(1);
        assertEquals(unsentIdentifys.optJSONObject(0).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(unsentIdentifys.optJSONObject(0).optLong("sequence_number"), 2);
        assertTrue(Utils.compareJSONObjects(
            unsentIdentifys.optJSONObject(0).optJSONObject("user_properties"), expectedIdentify
        ));

        // send response and check that remove events works properly
        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 2);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test_event");
        assertEquals(events.optJSONObject(1).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertTrue(Utils.compareJSONObjects(
            events.optJSONObject(1).optJSONObject("user_properties"), expectedIdentify
        ));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testMergeEventsAndIdentifys() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 5, 6, 7, 8, 9, 10};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        amplitude.logEvent("test_event1");
        amplitude.identify(new Identify().add("photo_count", 1));
        amplitude.logEvent("test_event2");
        amplitude.logEvent("test_event3");
        amplitude.logEvent("test_event4");
        amplitude.identify(new Identify().set("gender", "male"));
        amplitude.identify(new Identify().unset("karma"));

        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // verify some internal counters
        assertEquals(getUnsentEventCount(), 4);
        assertEquals(amplitude.lastEventId, 4);
        assertEquals(getUnsentIdentifyCount(), 3);
        assertEquals(amplitude.lastIdentifyId, 3);

        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 7);

        JSONObject expectedIdentify1 = new JSONObject();
        expectedIdentify1.put(Constants.AMP_OP_ADD, new JSONObject().put("photo_count", 1));
        JSONObject expectedIdentify2 = new JSONObject();
        expectedIdentify2.put(Constants.AMP_OP_SET, new JSONObject().put("gender", "male"));
        JSONObject expectedIdentify3 = new JSONObject();
        expectedIdentify3.put(Constants.AMP_OP_UNSET, new JSONObject().put("karma", "-"));

        assertEquals(events.getJSONObject(0).getString("event_type"), "test_event1");
        assertEquals(events.getJSONObject(0).getLong("event_id"), 1);
        assertEquals(events.getJSONObject(0).getLong("timestamp"), timestamps[0]);
        assertEquals(events.getJSONObject(0).getLong("sequence_number"), 1);

        assertEquals(events.getJSONObject(1).getString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.getJSONObject(1).getLong("event_id"), 1);
        assertEquals(events.getJSONObject(1).getLong("timestamp"), timestamps[1]);
        assertEquals(events.getJSONObject(1).getLong("sequence_number"), 2);
        assertTrue(Utils.compareJSONObjects(
                events.getJSONObject(1).getJSONObject("user_properties"), expectedIdentify1
        ));

        assertEquals(events.getJSONObject(2).getString("event_type"), "test_event2");
        assertEquals(events.getJSONObject(2).getLong("event_id"), 2);
        assertEquals(events.getJSONObject(2).getLong("timestamp"), timestamps[2]);
        assertEquals(events.getJSONObject(2).getLong("sequence_number"), 3);

        assertEquals(events.getJSONObject(3).getString("event_type"), "test_event3");
        assertEquals(events.getJSONObject(3).getLong("event_id"), 3);
        assertEquals(events.getJSONObject(3).getLong("timestamp"), timestamps[3]);
        assertEquals(events.getJSONObject(3).getLong("sequence_number"), 4);

        // sequence number guarantees strict ordering regardless of timestamp
        assertEquals(events.getJSONObject(4).getString("event_type"), "test_event4");
        assertEquals(events.getJSONObject(4).getLong("event_id"), 4);
        assertEquals(events.getJSONObject(4).getLong("timestamp"), timestamps[4]);
        assertEquals(events.getJSONObject(4).getLong("sequence_number"), 5);

        assertEquals(events.getJSONObject(5).getString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.getJSONObject(5).getLong("event_id"), 2);
        assertEquals(events.getJSONObject(5).getLong("timestamp"), timestamps[5]);
        assertEquals(events.getJSONObject(5).getLong("sequence_number"), 6);
        assertTrue(Utils.compareJSONObjects(
                events.getJSONObject(5).getJSONObject("user_properties"), expectedIdentify2
        ));

        assertEquals(events.getJSONObject(6).getString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.getJSONObject(6).getLong("event_id"), 3);
        assertEquals(events.getJSONObject(6).getLong("timestamp"), timestamps[6]);
        assertEquals(events.getJSONObject(6).getLong("sequence_number"), 7);
        assertTrue(Utils.compareJSONObjects(
                events.getJSONObject(6).getJSONObject("user_properties"), expectedIdentify3
        ));

        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        // verify db state
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertNull(dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY), 3L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY), 4L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY), 7L);
        assertEquals((long)dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY), timestamps[6]);
    }

    @Test
    public void testMergeEventBackwardsCompatible() throws JSONException {
        amplitude.setEventUploadThreshold(4);
        // eventst logged before v2.1.0 won't have a sequence number, should get priority
        long [] timestamps = {1, 1, 2, 3};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        amplitude.uploadingCurrently.set(true);
        amplitude.identify(new Identify().add("photo_count", 1));
        amplitude.logEvent("test_event1");
        amplitude.identify(new Identify().add("photo_count", 2));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // need to delete sequence number from test event
        JSONObject event = getUnsentEvents(1).getJSONObject(0);
        assertEquals(event.getLong("event_id"), 1);
        event.remove("sequence_number");
        event.remove("event_id");
        // delete event from db and reinsert modified event
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.removeEvent(1);
        dbHelper.addEvent(event.toString());
        amplitude.uploadingCurrently.set(false);

        // log another event to trigger upload
        amplitude.logEvent("test_event2");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // verify some internal counters
        assertEquals(getUnsentEventCount(), 2);
        assertEquals(amplitude.lastEventId, 3);
        assertEquals(getUnsentIdentifyCount(), 2);
        assertEquals(amplitude.lastIdentifyId, 2);

        JSONObject expectedIdentify1 = new JSONObject();
        expectedIdentify1.put(Constants.AMP_OP_ADD, new JSONObject().put("photo_count", 1));
        JSONObject expectedIdentify2 = new JSONObject();
        expectedIdentify2.put(Constants.AMP_OP_ADD, new JSONObject().put("photo_count", 2));

        // send response and check that merging events correctly ordered events
        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 4);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test_event1");
        assertFalse(events.optJSONObject(0).has("sequence_number"));
        assertEquals(events.optJSONObject(1).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(1).optLong("sequence_number"), 1);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(1).optJSONObject("user_properties"), expectedIdentify1
        ));
        assertEquals(events.optJSONObject(2).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(2).optLong("sequence_number"), 3);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(2).optJSONObject("user_properties"), expectedIdentify2
        ));
        assertEquals(events.optJSONObject(3).optString("event_type"), "test_event2");
        assertEquals(events.optJSONObject(3).optLong("sequence_number"), 4);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testRemoveAfterSuccessfulUpload() throws JSONException {
        long [] timestamps = new long[Constants.EVENT_UPLOAD_MAX_BATCH_SIZE + 4];
        for (int i = 0; i < timestamps.length; i++) timestamps[i] = i;
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        for (int i = 0; i < Constants.EVENT_UPLOAD_THRESHOLD; i++) {
            amplitude.logEvent("test_event" + i);
        }
        amplitude.identify(new Identify().add("photo_count", 1));
        amplitude.identify(new Identify().add("photo_count", 2));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), Constants.EVENT_UPLOAD_THRESHOLD);
        assertEquals(getUnsentIdentifyCount(), 2);

        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);
        for (int i = 0; i < events.length(); i++) {
            assertEquals(events.optJSONObject(i).optString("event_type"), "test_event" + i);
        }

        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 2); // should have 2 identifys left
    }

    @Test
    public void testLogEventHasUUID() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        amplitude.logEvent("test_event");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        JSONObject event = getLastUnsentEvent();
        assertTrue(event.has("uuid"));
        assertNotNull(event.optString("uuid"));
        assertTrue(event.optString("uuid").length() > 0);
    }

    @Test
    public void testLogRevenue() {
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        JSONObject event, apiProps;

        amplitude.logRevenue(10.99);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        event = getLastUnsentEvent();
        apiProps = event.optJSONObject("api_properties");
        assertEquals(Constants.AMP_REVENUE_EVENT, event.optString("event_type"));
        assertEquals(Constants.AMP_REVENUE_EVENT, apiProps.optString("special"));
        assertEquals(1, apiProps.optInt("quantity"));
        assertNull(apiProps.optString("productId", null));
        assertEquals(10.99, apiProps.optDouble("price"), .01);
        assertNull(apiProps.optString("receipt", null));
        assertNull(apiProps.optString("receiptSig", null));

        amplitude.logRevenue("ID1", 2, 9.99);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        event = getLastUnsentEvent();
        apiProps = event.optJSONObject("api_properties");;
        assertEquals(Constants.AMP_REVENUE_EVENT, event.optString("event_type"));
        assertEquals(Constants.AMP_REVENUE_EVENT, apiProps.optString("special"));
        assertEquals(2, apiProps.optInt("quantity"));
        assertEquals("ID1", apiProps.optString("productId"));
        assertEquals(9.99, apiProps.optDouble("price"), .01);
        assertNull(apiProps.optString("receipt", null));
        assertNull(apiProps.optString("receiptSig", null));

        amplitude.logRevenue("ID2", 3, 8.99, "RECEIPT", "SIG");
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        event = getLastUnsentEvent();
        apiProps = event.optJSONObject("api_properties");
        assertEquals(Constants.AMP_REVENUE_EVENT, event.optString("event_type"));
        assertEquals(Constants.AMP_REVENUE_EVENT, apiProps.optString("special"));
        assertEquals(3, apiProps.optInt("quantity"));
        assertEquals("ID2", apiProps.optString("productId"));
        assertEquals(8.99, apiProps.optDouble("price"), .01);
        assertEquals("RECEIPT", apiProps.optString("receipt"));
        assertEquals("SIG", apiProps.optString("receiptSig"));

        assertNotNull(runRequest(amplitude));
    }

    @Test
    public void testLogRevenueV2() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        // ignore invalid revenue objects
        amplitude.logRevenueV2(null);
        looper.runToEndOfTasks();
        amplitude.logRevenueV2(new Revenue());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);

        // log valid revenue object
        double price = 10.99;
        int quantity = 15;
        String productId = "testProductId";
        String receipt = "testReceipt";
        String receiptSig = "testReceiptSig";
        String revenueType = "testRevenueType";
        JSONObject props = new JSONObject().put("city", "Boston");

        Revenue revenue = new Revenue().setProductId(productId).setPrice(price);
        revenue.setQuantity(quantity).setReceipt(receipt, receiptSig);
        revenue.setRevenueType(revenueType).setRevenueProperties(props);

        amplitude.logRevenueV2(revenue);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "revenue_amount");

        JSONObject obj = event.optJSONObject("event_properties");
        assertEquals(obj.optDouble("$price"), price, 0);
        assertEquals(obj.optInt("$quantity"), 15);
        assertEquals(obj.optString("$productId"), productId);
        assertEquals(obj.optString("$receipt"), receipt);
        assertEquals(obj.optString("$receiptSig"), receiptSig);
        assertEquals(obj.optString("$revenueType"), revenueType);
        assertEquals(obj.optString("city"), "Boston");

        // user properties should be empty
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("user_properties"), new JSONObject()
        ));

        // api properties should not have any revenue info
        JSONObject apiProps = event.optJSONObject("api_properties");
        assertTrue(apiProps.length() > 0);
        assertFalse(apiProps.has("special"));
        assertFalse(apiProps.has("productId"));
        assertFalse(apiProps.has("quantity"));
        assertFalse(apiProps.has("price"));
        assertFalse(apiProps.has("receipt"));
        assertFalse(apiProps.has("receiptSig"));
    }

    @Test
    public void testLogEventSync() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        amplitude.logEventSync("test_event_sync", null);

        // Event should be in the database synchronously.
        JSONObject event = getLastEvent();
        assertEquals("test_event_sync", event.optString("event_type"));

        looper.runToEndOfTasks();

        server.enqueue(new MockResponse().setBody("success"));
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httplooper.runToEndOfTasks();

        try {
            assertNotNull(server.takeRequest(1, SECONDS));
        } catch (InterruptedException e) {
            fail(e.toString());
        }
    }

    /**
     * Test for not excepting on empty event properties.
     * See https://github.com/amplitude/Amplitude-Android/issues/35
     */
    @Test
    public void testEmptyEventProps() {
        RecordedRequest request = sendEvent(amplitude, "test_event", new JSONObject());
        assertNotNull(request);
    }

    /**
     * Test that resend failed events only occurs every 30 events.
     */
    @Test
    public void testSaveEventLogic() {
        amplitude.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);

        for (int i = 0; i < Constants.EVENT_UPLOAD_THRESHOLD; i++) {
            amplitude.logEvent("test");
        }
        looper.runToEndOfTasks();
        // unsent events will be threshold (+1 for start session)
        assertEquals(getUnsentEventCount(), Constants.EVENT_UPLOAD_THRESHOLD + 1);

        server.enqueue(new MockResponse().setBody("invalid_api_key"));
        server.enqueue(new MockResponse().setBody("bad_checksum"));
        ShadowLooper httpLooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httpLooper.runToEndOfTasks();

        // no events sent, queue should be same size
        assertEquals(getUnsentEventCount(), Constants.EVENT_UPLOAD_THRESHOLD + 1);

        for (int i = 0; i < Constants.EVENT_UPLOAD_THRESHOLD; i++) {
            amplitude.logEvent("test");
        }
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), Constants.EVENT_UPLOAD_THRESHOLD * 2 + 1);
        httpLooper.runToEndOfTasks();

        // sent 61 events, should have only made 2 requests
        assertEquals(server.getRequestCount(), 2);
    }

    @Test
    public void testRequestTooLargeBackoffLogic() {
        amplitude.trackSessionEvents(true);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        // verify event queue empty
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);

        // 413 error force backoff with 2 events --> new upload limit will be 1
        amplitude.logEvent("test");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2); // 2 events: start session + test
        server.enqueue(new MockResponse().setResponseCode(413));
        ShadowLooper httpLooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httpLooper.runToEndOfTasks();

        // 413 error with upload limit 1 will remove the top (start session) event
        amplitude.logEvent("test");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);
        server.enqueue(new MockResponse().setResponseCode(413));
        httpLooper.runToEndOfTasks();

        // verify only start session event removed
        assertEquals(getUnsentEventCount(), 2);
        JSONArray events = getUnsentEvents(2);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test");
        assertEquals(events.optJSONObject(1).optString("event_type"), "test");

        // upload limit persists until event count below threshold
        server.enqueue(new MockResponse().setBody("success"));
        looper.runToEndOfTasks(); // retry uploading after removing large event
        httpLooper.runToEndOfTasks(); // send success --> 1 event sent
        looper.runToEndOfTasks(); // event count below threshold --> disable backoff
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        // verify backoff disabled - queue 2 more events, see that all get uploaded
        amplitude.logEvent("test");
        amplitude.logEvent("test");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);
        server.enqueue(new MockResponse().setBody("success"));
        httpLooper.runToEndOfTasks();
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
    }

    @Test
    public void testUploadRemainingEvents() {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        amplitude.setEventUploadMaxBatchSize(2);
        amplitude.setEventUploadThreshold(2);
        amplitude.uploadingCurrently.set(true); // block uploading until we queue up enough events
        for (int i = 0; i < 6; i++) {
            amplitude.logEvent(String.format("test%d", i));
            looper.runToEndOfTasks();
            looper.runToEndOfTasks();
            assertEquals(dbHelper.getTotalEventCount(), i+1);
        }
        amplitude.uploadingCurrently.set(false);

        // allow event uploads
        // 7 events in queue, should upload 2, and then 2, and then 2, and then 2
        amplitude.logEvent("test7");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(dbHelper.getEventCount(), 7);
        assertEquals(dbHelper.getIdentifyCount(), 0);
        assertEquals(dbHelper.getTotalEventCount(), 7);

        // server response
        server.enqueue(new MockResponse().setBody("success"));
        ShadowLooper httpLooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httpLooper.runToEndOfTasks();

        // when receive success response, continue uploading
        looper.runToEndOfTasks();
        looper.runToEndOfTasks(); // remove uploaded events
        assertEquals(dbHelper.getEventCount(), 5);
        assertEquals(dbHelper.getIdentifyCount(), 0);
        assertEquals(dbHelper.getTotalEventCount(), 5);

        // 2nd server response
        server.enqueue(new MockResponse().setBody("success"));
        httpLooper.runToEndOfTasks();
        looper.runToEndOfTasks(); // remove uploaded events
        assertEquals(dbHelper.getEventCount(), 3);
        assertEquals(dbHelper.getIdentifyCount(), 0);
        assertEquals(dbHelper.getTotalEventCount(), 3);

        // 3rd server response
        server.enqueue(new MockResponse().setBody("success"));
        httpLooper.runToEndOfTasks();
        looper.runToEndOfTasks(); // remove uploaded events
        looper.runToEndOfTasks();
        assertEquals(dbHelper.getEventCount(), 1);
        assertEquals(dbHelper.getIdentifyCount(), 0);
        assertEquals(dbHelper.getTotalEventCount(), 1);
    }

    @Test
    public void testBackoffRemoveIdentify() {
        long [] timestamps = {1, 1, 2, 3, 4, 5};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        // 413 error force backoff with 2 events --> new upload limit will be 1
        amplitude.identify(new Identify().add("photo_count", 1));
        amplitude.logEvent("test1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentIdentifyCount(), 1);
        assertEquals(getUnsentEventCount(), 1);

        server.enqueue(new MockResponse().setResponseCode(413));
        ShadowLooper httpLooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httpLooper.runToEndOfTasks();

        // 413 error with upload limit 1 will remove the top identify
        amplitude.logEvent("test2");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);
        assertEquals(getUnsentIdentifyCount(), 1);
        server.enqueue(new MockResponse().setResponseCode(413));
        httpLooper.runToEndOfTasks();

        // verify only identify removed
        assertEquals(getUnsentEventCount(), 2);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONArray events = getUnsentEvents(2);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test1");
        assertEquals(events.optJSONObject(1).optString("event_type"), "test2");
    }

    @Test
    public void testLimitTrackingEnabled() {
        amplitude.logEvent("test");
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        JSONObject apiProperties = getLastUnsentEvent().optJSONObject("api_properties");
        assertTrue(apiProperties.has("limit_ad_tracking"));
        assertFalse(apiProperties.optBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.has("androidADID"));
    }

    @Test
    public void testTruncateString() {
        String longString = generateStringWithLength(Constants.MAX_STRING_LENGTH * 2, 'c');
        assertEquals(longString.length(), Constants.MAX_STRING_LENGTH * 2);
        String truncatedString = amplitude.truncate(longString);
        assertEquals(truncatedString.length(), Constants.MAX_STRING_LENGTH);
        assertEquals(truncatedString, generateStringWithLength(Constants.MAX_STRING_LENGTH, 'c'));
    }

    @Test
    public void testTruncateJSONObject() throws JSONException {
        String longString = generateStringWithLength(Constants.MAX_STRING_LENGTH * 2, 'c');
        String truncString = generateStringWithLength(Constants.MAX_STRING_LENGTH, 'c');
        JSONObject object = new JSONObject();
        object.put("int value", 10);
        object.put("bool value", false);
        object.put("long string", longString);
        object.put("array", new JSONArray().put(longString).put(10));
        object.put("jsonobject", new JSONObject().put("long string", longString));
        object.put(Constants.AMP_REVENUE_RECEIPT, longString);
        object.put(Constants.AMP_REVENUE_RECEIPT_SIG, longString);

        object = amplitude.truncate(object);
        assertEquals(object.optInt("int value"), 10);
        assertEquals(object.optBoolean("bool value"), false);
        assertEquals(object.optString("long string"), truncString);
        assertEquals(object.optJSONArray("array").length(), 2);
        assertEquals(object.optJSONArray("array").getString(0), truncString);
        assertEquals(object.optJSONArray("array").getInt(1), 10);
        assertEquals(object.optJSONObject("jsonobject").length(), 1);
        assertEquals(object.optJSONObject("jsonobject").optString("long string"), truncString);

        // receipt and receipt sig should not be truncated
        assertEquals(object.optString(Constants.AMP_REVENUE_RECEIPT), longString);
        assertEquals(object.optString(Constants.AMP_REVENUE_RECEIPT_SIG), longString);
    }

    @Test
    public void testTruncateNullJSONObject() throws JSONException {
        assertTrue(Utils.compareJSONObjects(
            amplitude.truncate((JSONObject) null), new JSONObject()
        ));
        assertEquals(amplitude.truncate((JSONArray) null).length(), 0);
    }

    @Test
    public void testTruncateEventAndIdentify() throws JSONException {
        String longString = generateStringWithLength(Constants.MAX_STRING_LENGTH * 2, 'c');
        String truncString = generateStringWithLength(Constants.MAX_STRING_LENGTH, 'c');

        long [] timestamps = {1, 1, 2, 3};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        amplitude.logEvent("test", new JSONObject().put("long_string", longString));
        amplitude.identify(new Identify().set("long_string", longString));

        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);

        assertEquals(events.optJSONObject(0).optString("event_type"), "test");
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(0).optJSONObject("event_properties"),
                new JSONObject().put("long_string", truncString)
        ));
        assertEquals(events.optJSONObject(1).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(1).optJSONObject("user_properties"),
                new JSONObject().put(Constants.AMP_OP_SET, new JSONObject().put("long_string", truncString))
        ));
    }

    @Test
    public void testAutoIncrementSequenceNumber() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        int limit = 10;
        for (int i = 0; i < limit; i++) {
            assertEquals(amplitude.getNextSequenceNumber(), i+1);
            assertEquals(dbHelper.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY), Long.valueOf(i+1));
        }
    }

    @Test
    public void testSetOffline() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        amplitude.setOffline(true);

        amplitude.logEvent("test1");
        amplitude.logEvent("test2");
        amplitude.identify(new Identify().unset("key1"));
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);
        assertEquals(getUnsentIdentifyCount(), 1);

        amplitude.setOffline(false);
        looper.runToEndOfTasks();
        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);
        looper.runToEndOfTasks();

        assertEquals(events.length(), 3);
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testSetOfflineTruncate() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        int eventMaxCount = 3;
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        amplitude.setEventMaxCount(eventMaxCount).setOffline(true);

        amplitude.logEvent("test1");
        amplitude.logEvent("test2");
        amplitude.logEvent("test3");
        amplitude.identify(new Identify().unset("key1"));
        amplitude.identify(new Identify().unset("key2"));
        amplitude.identify(new Identify().unset("key3"));
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);
        assertEquals(getUnsentIdentifyCount(), eventMaxCount);

        amplitude.logEvent("test4");
        amplitude.identify(new Identify().unset("key4"));
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);
        assertEquals(getUnsentIdentifyCount(), eventMaxCount);

        List<JSONObject> events = dbHelper.getEvents(-1, -1);
        assertEquals(events.size(), eventMaxCount);
        assertEquals(events.get(0).optString("event_type"), "test2");
        assertEquals(events.get(1).optString("event_type"), "test3");
        assertEquals(events.get(2).optString("event_type"), "test4");

        List<JSONObject> identifys = dbHelper.getIdentifys(-1, -1);
        assertEquals(identifys.size(), eventMaxCount);
        assertEquals(identifys.get(0).optJSONObject("user_properties").optJSONObject("$unset").optString("key2"), "-");
        assertEquals(identifys.get(1).optJSONObject("user_properties").optJSONObject("$unset").optString("key3"), "-");
        assertEquals(identifys.get(2).optJSONObject("user_properties").optJSONObject("$unset").optString("key4"), "-");
    }

    @Test
    public void testTruncateEventsQueues() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        int eventMaxCount = 50;
        assertTrue(eventMaxCount > Constants.EVENT_REMOVE_BATCH_SIZE);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        amplitude.setEventMaxCount(eventMaxCount).setOffline(true);

        for (int i = 0; i < eventMaxCount; i++) {
            amplitude.logEvent("test");
        }
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);

        amplitude.logEvent("test");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount - (eventMaxCount/10) + 1);
    }

    @Test
    public void testTruncateEventsQueuesWithOneEvent() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        int eventMaxCount = 1;
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        amplitude.setEventMaxCount(eventMaxCount).setOffline(true);

        amplitude.logEvent("test1");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);

        amplitude.logEvent("test2");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);

        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test2");
    }

    @Test
    public void testClearUserProperties() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        amplitude.clearUserProperties();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 1);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.IDENTIFY_EVENT, event.optString("event_type"));
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));

        JSONObject userPropertiesOperations = event.optJSONObject("user_properties");
        assertEquals(userPropertiesOperations.length(), 1);
        assertTrue(userPropertiesOperations.has(Constants.AMP_OP_CLEAR_ALL));

        assertEquals(
            "-", userPropertiesOperations.optString(Constants.AMP_OP_CLEAR_ALL)
        );
    }

    @Test
    public void testSetGroup() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        amplitude.setGroup("orgId", new JSONArray().put(10).put(15));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 1);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.IDENTIFY_EVENT, event.optString("event_type"));
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));

        JSONObject userPropertiesOperations = event.optJSONObject("user_properties");
        assertEquals(userPropertiesOperations.length(), 1);
        assertTrue(userPropertiesOperations.has(Constants.AMP_OP_SET));

        JSONObject groups = event.optJSONObject("groups");
        assertEquals(groups.length(), 1);
        assertEquals(groups.optJSONArray("orgId"), new JSONArray().put(10).put(15));

        JSONObject setOperations = userPropertiesOperations.optJSONObject(Constants.AMP_OP_SET);
        assertEquals(setOperations.length(), 1);
        assertEquals(setOperations.optJSONArray("orgId"), new JSONArray().put(10).put(15));
    }

    @Test
    public void testLogEventWithGroups() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        JSONObject groups = new JSONObject().put("orgId", 10).put("sport", "tennis");
        amplitude.logEvent("test", null, groups);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test");
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("user_properties"), new JSONObject()
        ));

        JSONObject eventGroups = event.optJSONObject("groups");
        assertEquals(eventGroups.length(), 2);
        assertEquals(eventGroups.optInt("orgId"), 10);
        assertEquals(eventGroups.optString("sport"), "tennis");
    }

    @Test
    public void testMergeEventsArrayIndexOutOfBounds() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        amplitude.setOffline(true);

        amplitude.logEvent("testEvent1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // force failure case
        amplitude.setLastEventId(0);

        amplitude.setOffline(false);
        looper.runToEndOfTasks();

        // make sure next upload succeeds
        amplitude.setLastEventId(1);
        amplitude.logEvent("testEvent2");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 2);

        assertEquals(events.getJSONObject(0).optString("event_type"), "testEvent1");
        assertEquals(events.getJSONObject(0).optLong("event_id"), 1);

        assertEquals(events.getJSONObject(1).optString("event_type"), "testEvent2");
        assertEquals(events.getJSONObject(1).optLong("event_id"), 2);
    }

    @Test
    public void testCursorWindowAllocationException() {
        Robolectric.getForegroundThreadScheduler().advanceTo(1);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        // log an event successfully
        amplitude.logEvent("testEvent1");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);

        // mock out database helper to force CursorWindowAllocationExceptions
        DatabaseHelper.instances.put(Constants.DEFAULT_INSTANCE, new MockDatabaseHelper(context));

        // force an upload and verify no request sent
        // make sure we catch it during sending of events and defer sending
        RecordedRequest request = runRequest(amplitude);
        assertNull(request);
        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);

        // make sure we catch it during initialization and treat as uninitialized
        amplitude.initialized = false;
        amplitude.initialize(context, apiKey);
        looper.runToEndOfTasks();
        assertNull(amplitude.apiKey);

        // since event meta data is loaded during initialize, in theory we should
        // be able to log an event even if we can't query from it
        amplitude.context = context;
        amplitude.apiKey = apiKey;
        Identify identify = new Identify().set("car", "blue");
        amplitude.identify(identify);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 1);
    }

    @Test
    public void testBlockTooManyEventUserProperties() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        JSONObject eventProperties = new JSONObject();
        JSONObject userProperties = new JSONObject();
        Identify identify = new Identify();

        for (int i = 0; i < Constants.MAX_PROPERTY_KEYS + 1; i++) {
            eventProperties.put(String.valueOf(i), i);
            userProperties.put(String.valueOf(i*2), i*2);
            identify.setOnce(String.valueOf(i), i);
        }

        // verify user properties is filtered out
        amplitude.setUserProperties(userProperties);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentIdentifyCount(), 0);

        // verify scrubbed from events
        amplitude.logEvent("test event", eventProperties);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test event");
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));

        // verify scrubbed from identifys - but leaves an empty JSONObject
        amplitude.identify(identify);
        looper.runToEndOfTasks();
        assertEquals(getUnsentIdentifyCount(), 1);
        JSONObject identifyEvent = getLastUnsentIdentify();
        assertEquals(identifyEvent.optString("event_type"), "$identify");
        assertTrue(Utils.compareJSONObjects(
            identifyEvent.optJSONObject("user_properties"),
            new JSONObject().put("$setOnce", new JSONObject())
        ));
    }

    @Test
    public void testLogEventWithTimestamp() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        amplitude.logEvent("test", null, null, 1000, false);
        looper.runToEndOfTasks();
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optLong("timestamp"), 1000);

        amplitude.logEventSync("test", null, null, 2000, false);
        looper.runToEndOfTasks();
        event = getLastUnsentEvent();
        assertEquals(event.optLong("timestamp"), 2000);
    }

    @Test
    public void testRegenerateDeviceId() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        String oldDeviceId = amplitude.getDeviceId();
        assertEquals(oldDeviceId, dbHelper.getValue("device_id"));

        amplitude.regenerateDeviceId();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        String newDeviceId = amplitude.getDeviceId();
        assertNotEquals(oldDeviceId, newDeviceId);
        assertEquals(newDeviceId, dbHelper.getValue("device_id"));
        assertTrue(newDeviceId.endsWith("R"));
    }

    @Test
    public void testSendNullEvents() throws JSONException {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        dbHelper.addEvent(null);
        amplitude.setLastEventId(1);
        amplitude.getNextSequenceNumber();
        assertEquals(getUnsentEventCount(), 1);

        amplitude.logEvent("test event");
        looper.runToEndOfTasks();

        amplitude.updateServer();
        RecordedRequest request = runRequest(amplitude);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 1);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test event");
    }

    @Test
    @PrepareForTest(OkHttpClient.class)
    public void testHandleUploadExceptions() throws Exception {
        ShadowLooper logLooper = Shadows.shadowOf(amplitude.logThread.getLooper());
        ShadowLooper httpLooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        IOException error = new IOException("test IO Exception");

        // mock out client
        OkHttpClient oldClient = amplitude.httpClient;
        OkHttpClient mockClient = PowerMockito.mock(OkHttpClient.class);

        // need to have mock client return mock call that throws exception
        Call mockCall = PowerMockito.mock(Call.class);
        PowerMockito.when(mockCall.execute()).thenThrow(error);
        PowerMockito.when(mockClient.newCall(Matchers.any(Request.class))).thenReturn(mockCall);

        // attach mock client to amplitude
        amplitude.httpClient = mockClient;
        amplitude.logEvent("test event");
        logLooper.runToEndOfTasks();
        logLooper.runToEndOfTasks();
        httpLooper.runToEndOfTasks();

        assertEquals(amplitude.lastError, error);

        // restore old client
        amplitude.httpClient = oldClient;
    }

    @Test
    public void testDefaultPlatform() throws InterruptedException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        assertEquals(amplitude.platform, Constants.PLATFORM);

        amplitude.logEvent("test_event1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONArray events = getUnsentEvents(1);
        for (int i = 0; i < 1; i++) {
            assertEquals(events.optJSONObject(i).optString("event_type"), "test_event" + (i+1));
            assertEquals(events.optJSONObject(i).optLong("timestamp"), timestamps[i]);
            assertEquals(events.optJSONObject(i).optString("platform"), Constants.PLATFORM);
        }
        runRequest(amplitude);
    }

    @Test
    public void testOverridePlatform() throws InterruptedException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        String customPlatform = "test_custom_platform";

        // force re-initialize to override platform
        amplitude.initialized = false;
        amplitude.initialize(context, apiKey, null, customPlatform, false);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(amplitude.platform, customPlatform);

        amplitude.logEvent("test_event1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONArray events = getUnsentEvents(1);
        for (int i = 0; i < 1; i++) {
            assertEquals(events.optJSONObject(i).optString("event_type"), "test_event" + (i+1));
            assertEquals(events.optJSONObject(i).optLong("timestamp"), timestamps[i]);
            assertEquals(events.optJSONObject(i).optString("platform"), customPlatform);
        }
        runRequest(amplitude);
    }

    @Test
    public void testSetTrackingConfig() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        TrackingOptions options = new TrackingOptions().disableCity().disableCountry().disableIpAddress().disableLanguage().disableLatLng();
        amplitude.setTrackingOptions(options);

        assertEquals(amplitude.trackingOptions, options);
        assertTrue(Utils.compareJSONObjects(amplitude.apiPropertiesTrackingOptions, options.getApiPropertiesTrackingOptions()));
        assertFalse(amplitude.trackingOptions.shouldTrackCity());
        assertFalse(amplitude.trackingOptions.shouldTrackCountry());
        assertFalse(amplitude.trackingOptions.shouldTrackIpAddress());
        assertFalse(amplitude.trackingOptions.shouldTrackLanguage());
        assertFalse(amplitude.trackingOptions.shouldTrackLatLng());

        amplitude.logEvent("test event");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        JSONArray events = getUnsentEvents(1);
        assertEquals(events.length(), 1);
        JSONObject event = events.getJSONObject(0);

        // verify we do have platform and carrier since those were not filtered out
        assertTrue(event.has("carrier"));
        assertTrue(event.has("platform"));

        // verify we do not have any of the filtered out fields
        assertFalse(event.has("city"));
        assertFalse(event.has("country"));
        assertFalse(event.has("language"));

        // verify api properties contains tracking options for location filtering
        JSONObject apiProperties = event.getJSONObject("api_properties");
        assertFalse(apiProperties.getBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.getBoolean("gps_enabled"));
        assertTrue(apiProperties.has("tracking_options"));

        JSONObject trackingOptions = apiProperties.getJSONObject("tracking_options");
        assertEquals(trackingOptions.length(), 4);
        assertFalse(trackingOptions.getBoolean("city"));
        assertFalse(trackingOptions.getBoolean("country"));
        assertFalse(trackingOptions.getBoolean("ip_address"));
        assertFalse(trackingOptions.getBoolean("lat_lng"));
    }

    @Test
    public void testGroupIdentifyMultipleOperations() throws JSONException {
        String groupType = "test group type";
        String groupName = "test group name";

        String property1 = "string value";
        String value1 = "testValue";

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "boolean value";
        boolean value3 = true;

        String property4 = "json value";

        Identify identify = new Identify().setOnce(property1, value1).add(property2, value2);
        identify.set(property3, value3).unset(property4);

        // identify should ignore this since duplicate key
        identify.set(property4, value3);

        amplitude.groupIdentify(groupType, groupName, identify);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        assertEquals(getUnsentIdentifyCount(), 1);
        assertEquals(getUnsentEventCount(), 0);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.GROUP_IDENTIFY_EVENT, event.optString("event_type"));

        assertTrue(Utils.compareJSONObjects(event.optJSONObject("event_properties"), new JSONObject()));
        assertTrue(Utils.compareJSONObjects(event.optJSONObject("user_properties"), new JSONObject()));

        JSONObject groups = event.optJSONObject("groups");
        JSONObject expectedGroups = new JSONObject();
        expectedGroups.put(groupType, groupName);
        assertTrue(Utils.compareJSONObjects(groups, expectedGroups));

        JSONObject groupProperties = event.optJSONObject("group_properties");
        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_SET_ONCE, new JSONObject().put(property1, value1));
        expected.put(Constants.AMP_OP_ADD, new JSONObject().put(property2, value2));
        expected.put(Constants.AMP_OP_SET, new JSONObject().put(property3, value3));
        expected.put(Constants.AMP_OP_UNSET, new JSONObject().put(property4, "-"));
        assertTrue(Utils.compareJSONObjects(groupProperties, expected));
    }
}
