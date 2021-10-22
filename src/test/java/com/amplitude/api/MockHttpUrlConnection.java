package com.amplitude.api;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MockHttpUrlConnection extends HttpURLConnection {
    public MockHttpUrlConnection(String str) throws MalformedURLException {
        this(new URL(str));
    }

    protected MockHttpUrlConnection(URL url) {
        super(url);
        this.responseCode = 200;
        this.responseMessage = "";
    }

    public static MockHttpUrlConnection defaultRes() {
        try {
            MockHttpUrlConnection request = new MockHttpUrlConnection(Constants.EVENT_LOG_URL);
            request.setBody("success");
            return request;
        } catch (MalformedURLException e) {
            fail(e.toString());
        }
        return null;
    }

    public MockHttpUrlConnection setBody(String body) {
        responseMessage = body;
        return this;
    }

    public MockHttpUrlConnection setResponseCode(int code) {
        this.responseCode = code;
        return this;
    }

    public OutputStream getOutputStream() {
        return new ByteArrayOutputStream();
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.responseMessage.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() {

    }
}
