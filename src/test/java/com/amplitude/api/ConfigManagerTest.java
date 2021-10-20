package com.amplitude.api;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amplitude.api.util.MockHttpURLConnectionHelper;
import com.amplitude.api.util.MockURLStreamHandler;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@Config(manifest=Config.NONE)
public class ConfigManagerTest {
    private final MockURLStreamHandler mockURLStreamHandler = MockURLStreamHandler.getInstance();

    @Test
    public void testRefreshForEU() throws Exception {
        assertEquals(Constants.EVENT_LOG_URL, ConfigManager.getInstance().getIngestionEndpoint());
        JSONObject responseObject = new JSONObject();
        responseObject.put("code", 200);
        responseObject.put("ingestionEndpoint", "api.eu.amplitude.com");

        URL url = new URL(Constants.DYNAMIC_CONFIG_EU_URL);
        AmplitudeServerZone euZone = AmplitudeServerZone.EU;
        HttpURLConnection connection =
                MockHttpURLConnectionHelper.getMockHttpURLConnection(200, responseObject.toString());
        mockURLStreamHandler.setConnection(url, connection);

        ConfigManager.getInstance().refresh(new ConfigManager.RefreshListener() {
            @Override
            public void onFinished() {
                assertEquals(Constants.EVENT_LOG_EU_URL, ConfigManager.getInstance().getIngestionEndpoint());
            }
        }, euZone);
    }
}