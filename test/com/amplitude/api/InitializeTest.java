package com.amplitude.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        String userId = "user_id";

        Amplitude.getInstance().initialize(context, "1cc2c1978ebab0f6451112a8f5df4f4e", userId);

        // Test that the user id is set.
        assertEquals(
            userId,
            DatabaseHelper.getDatabaseHelper(context).getValue(AmplitudeClient.USER_ID_KEY)
        );

        // Test that events are logged.
        RecordedRequest request = sendEvent(amplitude, "init_test_event", null);
        assertNotNull(request);
    }
}
