package com.amplitude.api.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

public class MockHttpURLConnectionHelper {

    public static HttpURLConnection getMockHttpURLConnection(int code, String response)
            throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(connection.getOutputStream()).thenReturn(outputStream);
        when(connection.getResponseCode()).thenReturn(code);
        InputStream inputStream = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        if (code == 200) {
            when(connection.getInputStream()).thenReturn(inputStream);
        } else {
            when(connection.getErrorStream()).thenReturn(inputStream);
        }
        return connection;
    }
}
