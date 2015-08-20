package com.amplitude.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.squareup.okhttp.mockwebserver.MockResponse;
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
import org.robolectric.shadows.ShadowLooper;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AmplitudeTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSetUserId() {
        String sharedPreferences = Constants.SHARED_PREFERENCES_NAME_PREFIX + "."
                + context.getPackageName();
        assertEquals(sharedPreferences, "com.amplitude.api.com.amplitude.test");
        String userId = "user_id";
        amplitude.setUserId(userId);
        assertEquals(
                userId,
                context.getSharedPreferences(sharedPreferences, Context.MODE_PRIVATE).getString(
                        Constants.PREFKEY_USER_ID, null));
    }

    @Test
    public void testSetUserIdTwice() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        String userId1 = "user_id1";
        String userId2 = "user_id2";

        amplitude.setUserId(userId1);
        assertEquals(amplitude.getUserId(), userId1);
        amplitude.logEvent("event1");
        looper.runToEndOfTasks();

        JSONObject event1 = getLastUnsentEvent();
        assertEquals(event1.optString("event_type"), "event1");
        assertEquals(event1.optString("user_id"), userId1);

        amplitude.setUserId(userId2);
        assertEquals(amplitude.getUserId(), userId2);
        amplitude.logEvent("event2");
        looper.runToEndOfTasks();

        JSONObject event2 = getLastUnsentEvent();
        assertEquals(event2.optString("event_type"), "event2");
        assertEquals(event2.optString("user_id"), userId2);
    }

    @Test
    public void testSetUserProperties() throws JSONException {
        amplitude.setUserProperties(null);
        assertNull(amplitude.userProperties);

        JSONObject userProperties;
        JSONObject userProperties2;
        JSONObject expected;

        userProperties = new JSONObject();
        userProperties.put("key1", "value1");
        userProperties.put("key2", "value2");
        amplitude.setUserProperties(userProperties);
        assertEquals(amplitude.userProperties.toString(), userProperties.toString());

        amplitude.setUserProperties(null);
        assertEquals(amplitude.userProperties.toString(), userProperties.toString());

        // modify original input JSONObject, should not modify internal amplitude JSONObject
        userProperties.put("key2", "value3");
        userProperties.put("key3", "value4");

        // test merging on background thread
        userProperties2 = new JSONObject();
        userProperties2.put("key5", "value5");
        amplitude.setUserProperties(userProperties2);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        expected = new JSONObject();
        expected.put("key1", "value1");
        expected.put("key2", "value2");
        expected.put("key5", "value5");
        // JSONObject doesn't have a proper equals method, so we compare strings
        // instead
        assertEquals(expected.toString(), amplitude.userProperties.toString());
    }

    @Test
    public void testReloadDeviceIdFromDatabase() {
        String deviceId = "test_device_id";
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        assertNull(amplitude.getDeviceId());
        DatabaseHelper.getDatabaseHelper(context).insertOrReplaceKeyValue(
                AmplitudeClient.DEVICE_ID_KEY,
                deviceId
        );
        looper.getScheduler().advanceToLastPostedRunnable();
        assertEquals(deviceId, amplitude.getDeviceId());
    }

    @Test
    public void testDoesNotUpgradeDeviceIdFromSharedPrefsToDatabase() {
        assertNull(amplitude.getDeviceId());
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());

        // initializeDeviceId no longer fetches from SharedPrefs, will get advertising ID instead
        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_DEVICE_ID, "test_device_id").commit();

        looper.getScheduler().advanceToLastPostedRunnable();
        String deviceId = amplitude.getDeviceId();
        assertTrue(deviceId.endsWith("R"));
        assertEquals(
                deviceId,
                DatabaseHelper.getDatabaseHelper(context).getValue(AmplitudeClient.DEVICE_ID_KEY)
        );
    }

    @Test
    public void testGetDeviceIdWithoutAdvertisingId() {
        assertNull(amplitude.getDeviceId());
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.getScheduler().advanceToLastPostedRunnable();
        assertNotNull(amplitude.getDeviceId());
        assertEquals(37, amplitude.getDeviceId().length());
        String deviceId = amplitude.getDeviceId();
        assertTrue(deviceId.endsWith("R"));
        assertEquals(
                deviceId,
                DatabaseHelper.getDatabaseHelper(context).getValue(AmplitudeClient.DEVICE_ID_KEY)
        );

    }

    @Test
    public void testOptOut() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());

        amplitude.setOptOut(true);
        RecordedRequest request = sendEvent(amplitude, "test_opt_out", null);
        assertNull(request);

        // Event shouldn't be sent event once opt out is turned off.
        amplitude.setOptOut(false);
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
    public void testLogRevenue() {
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        JSONObject event, apiProps;

        amplitude.logRevenue(10.99);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        event = getLastUnsentEvent();
        apiProps = event.optJSONObject("api_properties");
        assertEquals(AmplitudeClient.REVENUE_EVENT, event.optString("event_type"));
        assertEquals(AmplitudeClient.REVENUE_EVENT, apiProps.optString("special"));
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
        assertEquals(AmplitudeClient.REVENUE_EVENT, event.optString("event_type"));
        assertEquals(AmplitudeClient.REVENUE_EVENT, apiProps.optString("special"));
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
        assertEquals(AmplitudeClient.REVENUE_EVENT, event.optString("event_type"));
        assertEquals(AmplitudeClient.REVENUE_EVENT, apiProps.optString("special"));
        assertEquals(3, apiProps.optInt("quantity"));
        assertEquals("ID2", apiProps.optString("productId"));
        assertEquals(8.99, apiProps.optDouble("price"), .01);
        assertEquals("RECEIPT", apiProps.optString("receipt"));
        assertEquals("SIG", apiProps.optString("receiptSig"));

        assertNotNull(runRequest());
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

        // verify event queue empty
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);

        // 413 error force backoff with 2 events --> new upload limit will be 1
        amplitude.logEvent("test");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2); // 2 events: start session + test
        looper.runToEndOfTasks();
        server.enqueue(new MockResponse().setResponseCode(413));
        ShadowLooper httpLooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httpLooper.runToEndOfTasks();

        // 413 error with upload limit 1 will remove the top (start session) event
        amplitude.logEvent("test");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);
        looper.runToEndOfTasks();
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
        assertEquals(getUnsentEventCount(), 1);

        // verify backoff disabled - queue 2 more events, see that all get uploaded
        amplitude.logEvent("test");
        amplitude.logEvent("test");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);
        server.enqueue(new MockResponse().setBody("success"));
        httpLooper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
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
}
