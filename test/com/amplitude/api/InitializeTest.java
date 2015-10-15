package com.amplitude.api;

import android.content.Context;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class InitializeTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        amplitude = Amplitude.getInstance();
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testInitializeUserId() {
        String userId = "user_id";

        Amplitude.getInstance().initialize(context, "1cc2c1978ebab0f6451112a8f5df4f4e", userId);

        // Test that the user id is set.
        String sharedPreferences = Constants.SHARED_PREFERENCES_NAME_PREFIX + "."
                + context.getPackageName();
        assertEquals(sharedPreferences, "com.amplitude.api.com.amplitude.test");
        assertEquals(
                userId,
                context.getSharedPreferences(sharedPreferences, Context.MODE_PRIVATE).getString(
                        Constants.PREFKEY_USER_ID, null));

        // Test that events are logged.
        RecordedRequest request = sendEvent(amplitude, "init_test_event", null);
        assertNotNull(request);
    }
}
