package com.amplitude.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;
import org.json.JSONObject;
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
        assertEquals(amplitude.userProperties, userProperties);

        amplitude.setUserProperties(null);
        assertEquals(amplitude.userProperties, userProperties);

        userProperties2 = new JSONObject();
        userProperties.put("key2", "value3");
        userProperties.put("key3", "value4");
        amplitude.setUserProperties(userProperties2);
        expected = new JSONObject();
        expected.put("key1", "value1");
        expected.put("key2", "value3");
        expected.put("key3", "value4");
        // JSONObject doesn't have a proper equals method, so we compare strings
        // instead
        assertEquals(expected.toString(), amplitude.userProperties.toString());
    }

    @Test
    public void testGetDeviceIdWithoutAdvertisingId() {
        assertNull(amplitude.getDeviceId());
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.getScheduler().advanceToLastPostedRunnable();
        assertNotNull(amplitude.getDeviceId());
        assertEquals(37, amplitude.getDeviceId().length());
        assertTrue(amplitude.getDeviceId().endsWith("R"));
    }

    @Test
    public void testOptOut() {
        amplitude.setOptOut(true);
        RecordedRequest request = sendEvent(amplitude, "testOptOut", null);
        assertNull(request);

        amplitude.setOptOut(false);
        request = sendEvent(amplitude, "testOptOut", null);
        assertNotNull(request);
    }

    @Test
    public void testLogEvent() {
        RecordedRequest request = sendEvent(amplitude, "test_event", null);
        assertNotNull(request);
    }
}
