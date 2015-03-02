package com.amplitude.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.FakeHttp;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import android.content.Context;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AmplitudeTest {

    Context context;
    Amplitude.Lib amplitude;

    @Before
    public void setUp() throws Exception {
        ShadowApplication.getInstance().setPackageName("com.amplitude.test");
        context = ShadowApplication.getInstance().getApplicationContext();
        amplitude = new Amplitude.Lib();
        // this sometimes deadlocks with lock contention by logThread and httpThread for
        // a ShadowWrangler instance and the ShadowLooper class
        // Might be a sign of a bug, or just Robolectric's bug.
        amplitude.initialize(context, "3e1bdafd338d25310d727a394f282a8d");
        FakeHttp.setDefaultHttpResponse(200, "success");
    }

    @After
    public void tearDown() throws Exception {
        amplitude.logThread.getLooper().quit();
        amplitude.httpThread.getLooper().quit();
    }

    @Test
    public void testSetUserId() {
        String sharedPreferences = Constants.SHARED_PREFERENCES_NAME_PREFIX + "."
                + context.getPackageName();
        assertEquals(sharedPreferences, "com.amplitude.api.com.amplitude.test");
        String userId = "user_id";
        amplitude.setUserId(userId);
        assertEquals(
                "user_id",
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
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.getScheduler().advanceToLastPostedRunnable();
        amplitude.setOptOut(true);
        amplitude.logEvent("testOptOut");

        looper.getScheduler().advanceToLastPostedRunnable();
        looper.getScheduler().advanceToLastPostedRunnable();

        FakeHttp.addPendingHttpResponse(200, "success");
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httplooper.getScheduler().advanceToLastPostedRunnable();

        assertEquals(0, looper.getScheduler().size());
        assertFalse(FakeHttp.httpRequestWasMade(Constants.EVENT_LOG_URL));

        amplitude.setOptOut(false);
        amplitude.logEvent("testOptOut");

        looper.getScheduler().advanceToLastPostedRunnable();
        looper.getScheduler().advanceToLastPostedRunnable();

        FakeHttp.addPendingHttpResponse(200, "success");
        httplooper.getScheduler().advanceToLastPostedRunnable();

        assertEquals(1, looper.getScheduler().size());
        assertTrue(FakeHttp.httpRequestWasMade(Constants.EVENT_LOG_URL));
    }

    @Test
    public void testLogEvent() {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.getScheduler().advanceToLastPostedRunnable();
        amplitude.logEvent("test_event");
        looper.getScheduler().advanceToLastPostedRunnable();
        looper.getScheduler().advanceToLastPostedRunnable();

        FakeHttp.addPendingHttpResponse(200, "success");
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httplooper.getScheduler().advanceToLastPostedRunnable();

        assertEquals(1, looper.getScheduler().size());
        assertTrue(FakeHttp.httpRequestWasMade(Constants.EVENT_LOG_URL));
    }
}
