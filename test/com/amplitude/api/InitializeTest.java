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
public class InitializeTest {

    Context context;
    Amplitude.Lib amplitude;

    @Before
    public void setUp() throws Exception {
        ShadowApplication.getInstance().setPackageName("com.amplitude.test");
        context = ShadowApplication.getInstance().getApplicationContext();
        amplitude = Amplitude.getInstance();
        FakeHttp.setDefaultHttpResponse(200, "success");
    }

    @After
    public void tearDown() throws Exception {
        amplitude.logThread.getLooper().quit();
        amplitude.httpThread.getLooper().quit();
    }

    @Test
    public void testInitializeUserId() {
        String userId = "user_id";

        Amplitude.initialize(context, "3e1bdafd338d25310d727a394f282a8d", userId);

        // Test that the user id is set.
        String sharedPreferences = Constants.SHARED_PREFERENCES_NAME_PREFIX + "."
                + context.getPackageName();
        assertEquals(sharedPreferences, "com.amplitude.api.com.amplitude.test");
        assertEquals(
                userId,
                context.getSharedPreferences(sharedPreferences, Context.MODE_PRIVATE).getString(
                        Constants.PREFKEY_USER_ID, null));

        // Test that events are logged.
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.getScheduler().advanceToLastPostedRunnable();
        Amplitude.logEvent("test_event");
        looper.getScheduler().advanceToLastPostedRunnable();
        looper.getScheduler().advanceToLastPostedRunnable();

        FakeHttp.addPendingHttpResponse(200, "success");
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httplooper.getScheduler().advanceToLastPostedRunnable();

        assertEquals(1, looper.getScheduler().size());
        assertTrue(FakeHttp.httpRequestWasMade(Constants.EVENT_LOG_URL));
    }
}
