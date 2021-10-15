package com.amplitude.api;

import com.amplitude.security.MD5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;

public class HttpClient {

    private String apiKey, url, bearerToken;

    public HttpClient(String apiKey, String url, String bearerToken) {
        this.apiKey = apiKey;
        this.url = url;
        this.bearerToken = bearerToken;
    }

    protected static String bytesToHexString(byte[] bytes) {
        final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
                'c', 'd', 'e', 'f' };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    protected static long getCurrentTimeMillis() { return System.currentTimeMillis(); }

    public HttpResponse getSyncHttpResponse(String events)
            throws IllegalArgumentException, IOException {
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
            checksumString = bytesToHexString(messageDigest.digest(preimage.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            // According to
            // http://stackoverflow.com/questions/5049524/is-java-utf-8-charset-exception-possible,
            // this will never be thrown
        }

        StringBuilder sb = new StringBuilder();
        sb.append("v=" + apiVersionString + "&");
        sb.append("client=" + apiKey + "&");
        sb.append("e=" + events + "&");
        sb.append("upload_time=" + timestampString + "&");
        sb.append("checksum=" + checksumString);
        String bodyString = sb.toString();

        HttpURLConnection connection = getNewConnection(url);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");

        if (!Utils.isEmptyString(bearerToken)) {
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }

        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        byte[] input = bodyString.getBytes("UTF-8"); //bodyJson.toString().getBytes("UTF-8");
        os.write(input, 0, input.length);

        InputStream inputStream;
        if (100 <= connection.getResponseCode() && connection.getResponseCode() <= 399) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        BufferedReader br = null;
        if (inputStream != null) {
            br = new BufferedReader(new InputStreamReader(inputStream));
        }

        sb = new StringBuilder();
        String output = "";
        if (br != null) {
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
        }
        String stringResponse = sb.toString();
        return new HttpResponse(stringResponse, connection.getResponseCode());
    }

    public static HttpURLConnection getNewConnection(String url) throws IOException {
        URL urlObject = new URL(url);
        return (HttpURLConnection) urlObject.openConnection();
    }

}
