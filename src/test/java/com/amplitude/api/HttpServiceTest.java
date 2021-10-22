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

import java.net.HttpURLConnection;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class HttpServiceTest extends BaseTest {

    private int testMaxEventId = -1000000;
    private int testMaxIdentifyId = -1000001;
    private int[] calledListener; //represents number of success and error calls
    private String events = "[{\"user_id\":\"datamonster@gmail.com\",\"device_id\":\"C8F9E604-F01A-4BD9-95C6-8E5357DF265D\",\"event_type\":\"event1\",\"time\":1396381378123}]";

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        setUpListener();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void setUpListener() {
        assertTrue(amplitude instanceof AmplitudeTestHelperClient);
        AmplitudeTestHelperClient amplitudeTestHelperClient = (AmplitudeTestHelperClient) amplitude;
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
        calledListener = new int[]{0, 0, 0};
    }

    @Test
    public void testResponseOk() {
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpService.getHttpThreadLooper());
        server.enqueueResponse(MockHttpUrlConnection.defaultRes()); //200 OK
        amplitude.httpService.sendEvents(events, testMaxEventId, testMaxIdentifyId);
        httplooper.runToEndOfTasks();
        assertArrayEquals(new int[]{1, 0, 0}, calledListener);
    }

    @Test
    public void testResponseTooLarge() {
        //413 mock response too large (no exception), should retry
        MockHttpUrlConnection mockConn = MockHttpUrlConnection.defaultRes()
                .setResponseCode(HttpURLConnection.HTTP_ENTITY_TOO_LARGE);
        server.enqueueResponse(mockConn);
        amplitude.httpService.sendEvents(events, testMaxEventId, testMaxIdentifyId);
        ShadowLooper httpLooper = Shadows.shadowOf(amplitude.httpService.getHttpThreadLooper());
        httpLooper.runToEndOfTasks();
        assertArrayEquals(new int[]{0, 1, 0}, calledListener);
    }

    @Test
    public void testBadResponse() {
        //400 mock response (no exception), should not retry immediately
        MockHttpUrlConnection mockConn = MockHttpUrlConnection.defaultRes()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
        server.enqueueResponse(mockConn);
        amplitude.httpService.sendEvents(events, testMaxEventId, testMaxIdentifyId);
        ShadowLooper httpLooper = Shadows.shadowOf(amplitude.httpService.getHttpThreadLooper());
        httpLooper.runToEndOfTasks();
        assertArrayEquals(new int[]{0, 0, 1}, calledListener);
    }

    @Test
    public void testExceptionInRequest() {
        //exception during response, should not retry immediately
        MockHttpUrlConnection mockErrorConn = Mockito.spy(MockHttpUrlConnection.defaultRes());
        Mockito.when(mockErrorConn.getOutputStream()).thenThrow(new RuntimeException());
        server.enqueueResponse(mockErrorConn);
        amplitude.httpService.sendEvents(events, testMaxEventId, testMaxIdentifyId);
        ShadowLooper httpLooper = Shadows.shadowOf(amplitude.httpService.getHttpThreadLooper());
        httpLooper.runToEndOfTasks();
        assertArrayEquals(new int[]{0, 0, 1}, calledListener);
    }

    @Test
    public void testSetServerUrl() {
        String testUrl = "amplitude.com";
        amplitude.setServerUrl(testUrl);
        assertEquals(testUrl, amplitude.httpService.messageHandler.httpClient.url);
    }

    @Test
    public void testSetApiKey() {
        String testApiKey = "fake-api-key";
        amplitude.httpService.setApiKey(testApiKey);
        assertEquals(testApiKey, amplitude.httpService.messageHandler.httpClient.apiKey);
    }

    @Test
    public void testSetBearerToken() {
        String testBearerToken = "fake-bearer-token";
        amplitude.setBearerToken(testBearerToken);
        assertEquals(testBearerToken, amplitude.httpService.messageHandler.httpClient.bearerToken);
    }
}
