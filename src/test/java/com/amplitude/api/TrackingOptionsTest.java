package com.amplitude.api;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest= Config.NONE)
public class TrackingOptionsTest extends BaseTest {

    @Before
    public void setUp() throws Exception { setUp(false); }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testDisableFields() {
        TrackingOptions options = new TrackingOptions().disableCity().disableCountry().disableIpAddress().disableLanguage().disableLatLng();

        Set<String> expectedDisabledFields = new HashSet<String>();
        expectedDisabledFields.add("city");
        expectedDisabledFields.add("country");
        expectedDisabledFields.add("ip_address");
        expectedDisabledFields.add("language");
        expectedDisabledFields.add("lat_lng");

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
        assertFalse(options.shouldTrackLatLng());
        assertTrue(options.shouldTrackOsName());
        assertTrue(options.shouldTrackOsVersion());
        assertTrue(options.shouldTrackPlatform());
        assertTrue(options.shouldTrackRegion());
        assertTrue(options.shouldTrackVersionName());
    }

    @Test
    public void testGetApiPropertiesTrackingOptions() throws JSONException {
        TrackingOptions options = new TrackingOptions().disableCity().disableCountry().disableIpAddress().disableLanguage().disableLatLng();

        JSONObject expectedOptions = new JSONObject();
        expectedOptions.put("city", false);
        expectedOptions.put("country", false);
        expectedOptions.put("ip_address", false);
        expectedOptions.put("lat_lng", false);

        assertTrue(Utils.compareJSONObjects(options.getApiPropertiesTrackingOptions(), expectedOptions));
    }

    @Test
    public void testGetCoppaControlTrackingOptions() {
        TrackingOptions options = TrackingOptions.forCoppaControl();
        assertFalse(options.shouldTrackAdid());
        assertFalse(options.shouldTrackCity());
        assertFalse(options.shouldTrackIpAddress());
    }

    @Test
    public void testMerging() {
        TrackingOptions options1 = TrackingOptions.forCoppaControl();
        TrackingOptions options2 = new TrackingOptions().disableCountry().disableLanguage();
        options1.mergeIn(options2);
        assertFalse(options1.shouldTrackAdid());
        assertFalse(options1.shouldTrackCity());
        assertFalse(options1.shouldTrackIpAddress());
        assertFalse(options1.shouldTrackCountry());
        assertFalse(options1.shouldTrackLanguage());
    }

    @Test
    public void testCopyOf() {
        TrackingOptions options = TrackingOptions.copyOf(TrackingOptions.forCoppaControl());
        assertFalse(options.shouldTrackAdid());
        assertFalse(options.shouldTrackCity());
        assertFalse(options.shouldTrackIpAddress());
    }

    @Test
    public void testEquals() {
        TrackingOptions options1 = new TrackingOptions();
        options1.disableAdid().disableCarrier();
        TrackingOptions options2 = new TrackingOptions();
        options2.disableAdid().disableCarrier();
        assertEquals(options1, options2);
    }
}
