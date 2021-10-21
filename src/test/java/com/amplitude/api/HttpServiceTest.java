package com.amplitude.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class HttpServiceTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testE2EFlush() {
        assertTrue(amplitude instanceof AmplitudeTestHelperClient);
        AmplitudeTestHelperClient amplitudeTestHelperClient = (AmplitudeTestHelperClient) amplitude;

        int testMaxEventId = -1000000;
        int testMaxIdentifyId = -1000001;

        final int[] calledListener = {0, 0, 0}; //represents number of success and error calls
        amplitudeTestHelperClient.requestListener = new HttpService.RequestListener() {
            @Override
            public void onSuccess(long maxEventId, long maxIdentifyId) {
                calledListener[0]++;
                assertEquals(testMaxEventId, maxEventId);
                assertEquals(testMaxIdentifyId, maxIdentifyId);
            }
            @Override
            public void onErrorRetry(long maxEventId, long maxIdentifyId) {
                calledListener[1]++;
                assertEquals(testMaxEventId, maxEventId);
                assertEquals(testMaxIdentifyId, maxIdentifyId);
            }
            @Override
            public void onError() {
                calledListener[2]++;
            }
        };

        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        String events = "[{\"user_id\":\"datamonster@gmail.com\",\"device_id\":\"C8F9E604-F01A-4BD9-95C6-8E5357DF265D\",\"event_type\":\"event1\",\"time\":1396381378123}]";

        //200 mock response
        server.enqueueResponse(MockHttpUrlConnection.defaultRes());
        amplitude.httpService.submitSendEvents(events, testMaxEventId, testMaxIdentifyId);
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpService.getHttpThreadLooper());
        httplooper.runToEndOfTasks();
        assertArrayEquals(new int[]{1, 0, 0}, calledListener);

        //413 mock response too large (no exception), should retry
        server.enqueueResponse(MockHttpUrlConnection.defaultRes().setResponseCode(413));
        amplitude.httpService.submitSendEvents(events, testMaxEventId, testMaxIdentifyId);
        httplooper.runToEndOfTasks();
        assertArrayEquals(new int[]{1, 1, 0}, calledListener);

        //400 mock response (no exception), should not retry immediately
        server.enqueueResponse(MockHttpUrlConnection.defaultRes().setResponseCode(400));
        amplitude.httpService.submitSendEvents(events, testMaxEventId, testMaxIdentifyId);
        httplooper.runToEndOfTasks();
        assertArrayEquals(new int[]{1, 1, 1}, calledListener);

        //exception during response, should not retry immediately
        MockHttpUrlConnection mockErrorConn = Mockito.spy(MockHttpUrlConnection.defaultRes());
        Mockito.when(mockErrorConn.getOutputStream()).thenThrow(new RuntimeException());
        server.enqueueResponse(mockErrorConn);
        amplitude.httpService.submitSendEvents(events, testMaxEventId, testMaxIdentifyId);
        httplooper.runToEndOfTasks();
        assertArrayEquals(new int[]{1, 1, 2}, calledListener);
    }

}
