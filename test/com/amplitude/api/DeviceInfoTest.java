package com.amplitude.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowGeocoder;
import org.robolectric.shadows.ShadowLocation;
import org.robolectric.shadows.ShadowLocationManager;
import org.robolectric.shadows.ShadowTelephonyManager;
import org.robolectric.util.ReflectionHelpers;

import android.content.Context;
import android.content.res.Configuration;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.TelephonyManager;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DeviceInfoTest {

    private Context context;
    private DeviceInfo deviceInfo;
    private static final String TEST_VERSION_NAME = "com.amplitude.test";
    private static final String TEST_BRAND = "brand";
    private static final String TEST_MANUFACTURER = "manufacturer";
    private static final String TEST_MODEL = "model";
    private static final String TEST_CARRIER = "carrier";
    private static final Locale TEST_LOCALE = Locale.FRANCE;
    private static final String TEST_COUNTRY = "FR";
    private static final String TEST_LANGUAGE = "fr";
    private static final String TEST_NETWORK_COUNTRY = "GB";
    private static final double TEST_LOCATION_LAT = 37.7749295;
    private static final double TEST_LOCATION_LNG = -122.4194155;
    private static final String TEST_GEO_COUNTRY = "US";

    private static Location makeLocation(String provider, double lat, double lng) {
        Location l = new Location(provider);
        l.setLatitude(lat);
        l.setLongitude(lng);
        l.setTime(System.currentTimeMillis());
        return l;
    }

    @Before
    public void setUp() throws Exception {
        context = ShadowApplication.getInstance().getApplicationContext();
        ShadowApplication.getInstance().getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName = TEST_VERSION_NAME;
        ReflectionHelpers.setStaticField(Build.class, "BRAND", TEST_BRAND);
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", TEST_MANUFACTURER);
        ReflectionHelpers.setStaticField(Build.class, "MODEL", TEST_MODEL);

        Configuration c = context.getResources().getConfiguration();
        Shadows.shadowOf(c).setLocale(TEST_LOCALE);
        Locale.setDefault(TEST_LOCALE);

        ShadowTelephonyManager manager = Shadows.shadowOf((TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE));
        manager.setNetworkOperatorName(TEST_CARRIER);
        deviceInfo = new DeviceInfo(context);
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testGetVersionName() {
        assertEquals(TEST_VERSION_NAME, deviceInfo.getVersionName());
    }

    @Test
    public void testGetBrand() {
        assertEquals(TEST_BRAND, deviceInfo.getBrand());
    }

    @Test
    public void testGetManufacturer() {
        assertEquals(TEST_MANUFACTURER, deviceInfo.getManufacturer());
    }

    @Test
    public void testGetModel() {
        assertEquals(TEST_MODEL, deviceInfo.getModel());
    }

    @Test
    public void testGetCarrier() {
        assertEquals(TEST_CARRIER, deviceInfo.getCarrier());
    }

    @Test
    public void testGetCountry() {
        assertEquals(TEST_COUNTRY, deviceInfo.getCountry());
    }

    @Test
    public void testGetCountryFromNetwork() {
        ShadowTelephonyManager manager = Shadows.shadowOf((TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE));
        manager.setNetworkCountryIso(TEST_NETWORK_COUNTRY);
        assertEquals(TEST_NETWORK_COUNTRY, deviceInfo.getCountry());
    }

    @Test
    public void testGetCountryFromLocation() {
        DeviceInfo deviceInfo = new DeviceInfo(context) {
            @Override
            protected Geocoder getGeocoder() {
                Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
                ShadowGeocoder shadowGeocoder = (ShadowGeocoder) ShadowExtractor.extract(geocoder);
                shadowGeocoder.setSimulatedResponse("1 Dr Carlton B Goodlett Pl", "San Francisco",
                        "CA", "94506", TEST_GEO_COUNTRY);
                return geocoder;
            };
        };
        ShadowTelephonyManager telephonyManager = Shadows.shadowOf((TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE));
        telephonyManager.setNetworkCountryIso(TEST_NETWORK_COUNTRY);
        ShadowLocationManager locationManager = Shadows.shadowOf((LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE));
        locationManager.simulateLocation(makeLocation(LocationManager.NETWORK_PROVIDER,
                TEST_LOCATION_LAT, TEST_LOCATION_LNG));
        locationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        assertEquals(TEST_GEO_COUNTRY, deviceInfo.getCountry());
    }

    @Test
    public void testGetLanguage() {
        assertEquals(TEST_LANGUAGE, deviceInfo.getLanguage());
    }

    @Test
    public void testGetAdvertisingId() {
        // TODO (curtis): Not sure how to do this yet.
    }

    @Test
    public void testGetMostRecentLocation() {
        DeviceInfo deviceInfo = new DeviceInfo(context);
        ShadowLocationManager locationManager = Shadows.shadowOf((LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE));
        Location loc = makeLocation(LocationManager.NETWORK_PROVIDER, TEST_LOCATION_LAT,
                TEST_LOCATION_LNG);
        locationManager.simulateLocation(loc);
        locationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        assertEquals(loc, deviceInfo.getMostRecentLocation());
    }

    @Test
    public void testNoLocation() {
        DeviceInfo deviceInfo = new DeviceInfo(context);
        Location recent = deviceInfo.getMostRecentLocation();
        assertNull(recent);
    }

}
