package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class TrackingOptionsTest extends BaseTest {

    @Before
    public void setUp() throws Exception { setUp(false); }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testDisableFields() {
        TrackingOptions options = new TrackingOptions().disableCity().disableCountry().disableIpAddress().disableLanguage();

        Set<String> expectedDisabledFields = new HashSet<String>();
        expectedDisabledFields.add("city");
        expectedDisabledFields.add("country");
        expectedDisabledFields.add("ip_address");
        expectedDisabledFields.add("language");

        assertEquals(options.disabledFields, expectedDisabledFields);
        assertTrue(options.shouldTrackCarrier());
        assertFalse(options.shouldTrackCity());
        assertFalse(options.shouldTrackCountry());
        assertTrue(options.shouldTrackDeviceBrand());
        assertTrue(options.shouldTrackDeviceManufacturer());
        assertTrue(options.shouldTrackDeviceModel());
        assertTrue(options.shouldTrackDma());
        assertFalse(options.shouldTrackIpAddress());
        assertFalse(options.shouldTrackLanguage());
        assertTrue(options.shouldTrackOsName());
        assertTrue(options.shouldTrackOsVersion());
        assertTrue(options.shouldTrackPlatform());
        assertTrue(options.shouldTrackRegion());
        assertTrue(options.shouldTrackVersionName());
    }

    @Test
    public void testGetApiPropertiesTrackingOptions() throws JSONException {
        TrackingOptions options = new TrackingOptions().disableCity().disableCountry().disableIpAddress().disableLanguage();

        JSONObject expectedOptions = new JSONObject();
        expectedOptions.put("city", false);
        expectedOptions.put("country", false);
        expectedOptions.put("ip_address", false);

        assertTrue(Utils.compareJSONObjects(options.getApiPropertiesTrackingOptions(), expectedOptions));
    }
}
