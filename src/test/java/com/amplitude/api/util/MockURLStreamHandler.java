package com.amplitude.api.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class MockURLStreamHandler extends URLStreamHandler implements URLStreamHandlerFactory {

    private final Map<URL, URLConnection> connections = new HashMap<>();

    private static final MockURLStreamHandler instance = new MockURLStreamHandler();

    public static MockURLStreamHandler getInstance() {
        return instance;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return connections.get(url);
    }

    public void resetConnections() {
        connections.clear();
    }

    public MockURLStreamHandler setConnection(URL url, URLConnection urlConnection) {
        connections.put(url, urlConnection);
        return this;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return getInstance();
    }
}
