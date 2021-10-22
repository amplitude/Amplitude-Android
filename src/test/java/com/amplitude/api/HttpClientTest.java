package com.amplitude.api;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class HttpClientTest extends BaseTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetSyncHttpResponse() {
        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        //mock 200 response
        MockHttpUrlConnection mockRes = MockHttpUrlConnection.defaultRes();
        server.enqueueResponse(mockRes);
        String events = "[{\"user_id\":\"datamonster@gmail.com\",\"device_id\":\"C8F9E604-F01A-4BD9-95C6-8E5357DF265D\",\"event_type\":\"event1\",\"time\":1396381378123}]";
        try {
            amplitude.httpService.messageHandler.httpClient.makeRequest(events);
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testBadUrlConnection() throws IOException {
        String badUrl = "malformedurl";
        HttpClient client = new HttpClient("fake-key", badUrl, "fake-bearer-token");
        exception.expect(MalformedURLException.class);
        client.getNewConnection(badUrl);
    }
}
