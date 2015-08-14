package com.amplitude.security;

import com.amplitude.api.Constants;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;


/*
    Regression tests for alternative MD5 implementation.
 */

public class MD5Test {

    @Before
    public void setUp() throws Exception { return; }

    @After
    public void tearDown() throws Exception { return; }

    private static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "x", bi);
    }

    private void compareMD5(String input, String truth) {
        try {
            MessageDigest androidMD5 = MessageDigest.getInstance("MD5");
            byte[] androidBytes = androidMD5.digest(input.getBytes("UTF-8"));

            MessageDigest alternateMD5 = new MD5();
            byte[] altBytes = alternateMD5.digest(input.getBytes("UTF-8"));

            assertArrayEquals(androidBytes, altBytes);

            if (truth != null) {
                assertTrue(truth.equals(toHex(androidBytes)));
                assertTrue(truth.equals(toHex(altBytes)));
            }

        } catch (NoSuchAlgorithmException e) {
            fail (e.toString());
        } catch (UnsupportedEncodingException e) {
            fail (e.toString());
        }
    }

    @Test
    public void testMD5() {
        compareMD5("", "d41d8cd98f00b204e9800998ecf8427e");
        compareMD5("foobar", "3858f62230ac3c915f300c664312c63f");
    }

    @Test
    public void testMD5WithAmplitudeData() {

        String apiVersionString = "" + Constants.API_VERSION;
        String apiKey = "1cc2c1978ebab0f6451112a8f5df4f4e";
        String timestampString = "" + System.currentTimeMillis();
        String events = "[{\"version_name\":null,\"device_manufacturer\":\"unknown\",\"user_" +
                "properties\":{},\"platform\":\"Android\",\"api_properties\":{\"special\":\"" +
                "session_start\",\"limit_ad_tracking\":false},\"session_id\":1439421597509,\"" +
                "event_type\":\"session_start\",\"event_id\":1,\"event_properties\":{},\"devi" +
                "ce_id\":\"610e21eb-2f27-48ca-bf4e-f977ce6391d1R\",\"device_brand\":\"unknown" +
                "\",\"country\":\"US\",\"os_version\":\"unknown\",\"timestamp\":1439421597509" +
                ",\"device_model\":\"unknown\",\"os_name\":\"android\",\"library\":{\"name\":" +
                "\"amplitude-android\",\"version\":\"1.7.0\"},\"carrier\":null,\"user_id\":\"" +
                "610e21eb-2f27-48ca-bf4e-f977ce6391d1R\",\"language\":\"en\"},{\"version_name" +
                "\":null,\"device_manufacturer\":\"unknown\",\"user_properties\":{},\"platfor" +
                "m\":\"Android\",\"api_properties\":{\"limit_ad_tracking\":false},\"session_i" +
                "d\":1439421597509,\"event_type\":\"test\",\"event_id\":2,\"event_properties" +
                "\":{},\"device_id\":\"610e21eb-2f27-48ca-bf4e-f977ce6391d1R\",\"device_brand" +
                "\":\"unknown\",\"country\":\"US\",\"os_version\":\"unknown\",\"timestamp\":1" +
                "439421597509,\"device_model\":\"unknown\",\"os_name\":\"android\",\"library" +
                "\":{\"name\":\"amplitude-android\",\"version\":\"1.7.0\"},\"carrier\":null," +
                "\"user_id\":\"610e21eb-2f27-48ca-bf4e-f977ce6391d1R\",\"language\":\"en\"}]";

        String preImage = apiVersionString + apiKey + events + timestampString;
        compareMD5(preImage, null);
    }

    @Test
    public void testMD5WithRandomStrings() {
        for (int i = 0; i < 5; i++) {
            compareMD5(UUID.randomUUID().toString(), null);
        }
    }

    @Test
    public void testMD5WithUnicodeStrings() {
        compareMD5("\u2661", "db36e9b42b9fa2863f94280206fb4d74");
        compareMD5("\uD83D\uDE1C", "8fb34591f1a56cf3ca9837774f4b7bd7");
    }
}
