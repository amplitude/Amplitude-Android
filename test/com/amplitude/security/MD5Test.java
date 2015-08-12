package com.amplitude.security;

import com.amplitude.api.Constants;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;


/*
    Regression tests for alternative MD5 implementation.
 */

public class MD5Test {

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    void compareMD5(String input) {
        try {
            MessageDigest androidMD5 = MessageDigest.getInstance("MD5");
            byte[] androidBytes = androidMD5.digest(input.getBytes("UTF-8"));

            MessageDigest alternativeMD5 = new MD5();
            byte[] altBytes = alternativeMD5.digest(input.getBytes("UTF-8"));

            assertArrayEquals(androidBytes, altBytes);

        } catch (NoSuchAlgorithmException e) {
            fail (e.toString());
        } catch (UnsupportedEncodingException e) {
            fail (e.toString());
        }
    }

    @Test
    public void testMD5() {

        String apiKey = "1cc2c1978ebab0f6451112a8f5df4f4e";
        String apiVersionString = "" + Constants.API_VERSION;
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

        String preimage = apiVersionString + apiKey + events + timestampString;
        compareMD5(preimage);
    }

    @Test
    public void testMD5WithRandomString() {
        String uuid = UUID.randomUUID().toString();
        compareMD5(uuid);
    }
}
