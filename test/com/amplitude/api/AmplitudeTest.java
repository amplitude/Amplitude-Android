package com.amplitude.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import android.content.Context;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AmplitudeTest {

    Context context;
    Amplitude.Lib amplitude;

    @Before
    public void setUp() throws Exception {
        Robolectric.getShadowApplication().setPackageName("com.amplitude.test");
        context = Robolectric.getShadowApplication().getApplicationContext();
        amplitude = new Amplitude.Lib();
        amplitude.initialize(context, "3e1bdafd338d25310d727a394f282a8d");
        Robolectric.setDefaultHttpResponse(200, "success");
    }

    @After
    public void tearDown() throws Exception {}

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
    public void testGetDeviceIdWithoutAdvertisingId() {
        assertNull(amplitude.getDeviceId());
        ShadowLooper looper = Robolectric.shadowOf(amplitude.logThread.getLooper());
        looper.getScheduler().advanceToLastPostedRunnable();
        assertNotNull(amplitude.getDeviceId());
        assertEquals(37, amplitude.getDeviceId().length());
        assertTrue(amplitude.getDeviceId().endsWith("R"));
    }

    @Test
    public void testLogEvent() {
        ShadowLooper looper = Robolectric.shadowOf(amplitude.logThread.getLooper());
        looper.getScheduler().advanceToLastPostedRunnable();
        amplitude.logEvent("test_event");
        looper.getScheduler().advanceToLastPostedRunnable();
        looper.getScheduler().advanceToLastPostedRunnable();

        Robolectric.addPendingHttpResponse(200, "success");
        ShadowLooper httplooper = Robolectric.shadowOf(amplitude.httpThread.getLooper());
        httplooper.getScheduler().advanceToLastPostedRunnable();

        assertEquals(1, looper.getScheduler().enqueuedTaskCount());
        assertTrue(Robolectric.httpRequestWasMade(Constants.EVENT_LOG_URL));
    }
}
