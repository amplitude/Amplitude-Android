package com.amplitude.api;

import com.amplitude.security.MD5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

class HttpClient {

    protected String apiKey;
    protected String url;
    protected String bearerToken;

    public HttpClient(String apiKey, String url, String bearerToken) {
        this.apiKey = apiKey;
        this.url = url;
        this.bearerToken = bearerToken;
    }

    private long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    protected HttpResponse makeRequest(String events)
            throws IllegalArgumentException, IOException {
        String bodyString = generateBodyString(events);

        HttpURLConnection connection = getNewConnection(url);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");

        if (!Utils.isEmptyString(bearerToken)) {
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }

        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        byte[] input = bodyString.getBytes("UTF-8");
        os.write(input, 0, input.length);

        InputStream inputStream;
        if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        BufferedReader br = null;
        if (inputStream != null) {
            br = new BufferedReader(new InputStreamReader(inputStream));
        }

        StringBuilder sb = new StringBuilder();
        String output;
        if (br != null) {
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
        }
        String stringResponse = sb.toString();
        return new HttpResponse(stringResponse, connection.getResponseCode());
    }

    public String generateBodyString(String events) {
        String apiVersionString = "" + Constants.API_VERSION;
        String timestampString = "" + getCurrentTimeMillis();

        String checksumString = "";
        try {
            String preimage = apiVersionString + apiKey + events + timestampString;

            // MessageDigest.getInstance(String) is not threadsafe on Android.
            // See https://code.google.com/p/android/issues/detail?id=37937
            // Use MD5 implementation from http://org.rodage.com/pub/java/security/MD5.java
            // This implementation does not throw NoSuchAlgorithm exceptions.
            MessageDigest messageDigest = new MD5();
            checksumString = Utils.bytesToHexString(messageDigest.digest(preimage.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            // According to
            // http://stackoverflow.com/questions/5049524/is-java-utf-8-charset-exception-possible,
            // this will never be thrown
        }

        return String.format(
                "v=%1$s&client=%2$s&e=%3$s&upload_time=%4$s&checksum=%5$s",
                apiVersionString,
                apiKey,
                events,
                timestampString,
                checksumString
        );
    }

    protected HttpURLConnection getNewConnection(String url) throws IOException {
        URL urlObject = new URL(url);
        return (HttpURLConnection) urlObject.openConnection();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }
}
