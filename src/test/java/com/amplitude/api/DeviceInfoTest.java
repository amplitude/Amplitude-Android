package com.amplitude.api;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLocationManager;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowTelephonyManager;
import org.robolectric.shadows.maps.ShadowGeocoder;
import org.robolectric.util.ReflectionHelpers;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@RunWith(AndroidJUnit4.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "javax.net.ssl.*" })
@PrepareForTest({AdvertisingIdClient.class, GooglePlayServicesUtil.class})
@Config(manifest = Config.NONE)
public class DeviceInfoTest extends BaseTest {
    private DeviceInfo deviceInfo;
    private static final String TEST_VERSION_NAME = "test_version";
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

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {
        super.setUp(false);

        ReflectionHelpers.setStaticField(Build.class, "BRAND", TEST_BRAND);
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", TEST_MANUFACTURER);
        ReflectionHelpers.setStaticField(Build.class, "MODEL", TEST_MODEL);

        Configuration c = context.getResources().getConfiguration();
        Locale.setDefault(TEST_LOCALE);

        ShadowTelephonyManager manager = Shadows.shadowOf((TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE));
        manager.setNetworkOperatorName(TEST_CARRIER);
        deviceInfo = new DeviceInfo(context);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

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

        DeviceInfo deviceInfo = new DeviceInfo(context);
        assertEquals(TEST_NETWORK_COUNTRY, deviceInfo.getCountry());
    }

    // TODO: Consider move this test to android specific tests.
//    @Test
//    @Config(shadows = {MockGeocoder.class})
//    public void testGetCountryFromLocation() {
//        ShadowTelephonyManager telephonyManager = Shadows.shadowOf((TelephonyManager) context
//                .getSystemService(Context.TELEPHONY_SERVICE));
//        telephonyManager.setNetworkCountryIso(TEST_NETWORK_COUNTRY);
//        ShadowLocationManager locationManager = Shadows.shadowOf((LocationManager) context
//                .getSystemService(Context.LOCATION_SERVICE));
//        locationManager.simulateLocation(makeLocation(LocationManager.NETWORK_PROVIDER,
//                TEST_LOCATION_LAT, TEST_LOCATION_LNG));
//        locationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
//
//        DeviceInfo deviceInfo = new DeviceInfo(context) {
//            @Override
//            protected Geocoder getGeocoder() {
//                Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
//                ShadowGeocoder shadowGeocoder = Shadow.extract(geocoder);
//                shadowGeocoder.setSimulatedResponse("1 Dr Carlton B Goodlett Pl", "San Francisco",
//                        "CA", "94506", TEST_GEO_COUNTRY);
//                return geocoder;
//            }
//        };
//
//        assertEquals(TEST_GEO_COUNTRY, deviceInfo.getCountry());
//    }

    @Test
    public void testGetLanguage() {
        assertEquals(TEST_LANGUAGE, deviceInfo.getLanguage());
    }

    @Test
    public void testGetAdvertisingIdFromGoogleDevice() {
        PowerMockito.mockStatic(AdvertisingIdClient.class);
        String advertisingId = "advertisingId";
        AdvertisingIdClient.Info info = new AdvertisingIdClient.Info(
                advertisingId,
                false
        );

        try {
            Mockito.when(AdvertisingIdClient.getAdvertisingIdInfo(context)).thenReturn(info);
        } catch (Exception e) {
            fail(e.toString());
        }
        DeviceInfo deviceInfo = new DeviceInfo(context);

        // still get advertisingId even if limit ad tracking disabled
        assertEquals(advertisingId, deviceInfo.getAdvertisingId());
        assertFalse(deviceInfo.isLimitAdTrackingEnabled());
    }

    @Test
    public void testGetAdvertisingIdFromAmazonDevice() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Amazon");

        String advertisingId = "advertisingId";
        ContentResolver cr = context.getContentResolver();

        Secure.putInt(cr, "limit_ad_tracking", 1);
        Secure.putString(cr, "advertising_id", advertisingId);

        DeviceInfo deviceInfo = new DeviceInfo(context);

        // still get advertisingID even if limit ad tracking enabled
        assertEquals(advertisingId, deviceInfo.getAdvertisingId());
        assertTrue(deviceInfo.isLimitAdTrackingEnabled());
    }

    @Test
    public void testGPSDisabled() {
        // GPS not enabled
        DeviceInfo deviceInfo = new DeviceInfo(context);
        assertFalse(deviceInfo.isGooglePlayServicesEnabled());

        // GPS bundled but not enabled, GooglePlayUtils.isAvailable returns non-0 value
        PowerMockito.mockStatic(GooglePlayServicesUtil.class);
        try {
            Mockito.when(GooglePlayServicesUtil.isGooglePlayServicesAvailable(context))
                    .thenReturn(1);
        } catch (Exception e) {
            fail(e.toString());
        }
        assertFalse(deviceInfo.isGooglePlayServicesEnabled());
    }

    @Test
    public void testGPSEnabled() {
        PowerMockito.mockStatic(GooglePlayServicesUtil.class);
        try {
            Mockito.when(GooglePlayServicesUtil.isGooglePlayServicesAvailable(context))
                    .thenReturn(ConnectionResult.SUCCESS);
        } catch (Exception e) {
            fail(e.toString());
        }
        assert(deviceInfo.isGooglePlayServicesEnabled());
    }

    // TODO: Consider move this test to android specific tests.
//    @Test
//    public void testGetMostRecentLocation() {
//        DeviceInfo deviceInfo = new DeviceInfo(context);
//        ShadowLocationManager locationManager = Shadows.shadowOf((LocationManager) context
//                .getSystemService(Context.LOCATION_SERVICE));
//        Location loc = makeLocation(LocationManager.NETWORK_PROVIDER, TEST_LOCATION_LAT,
//                TEST_LOCATION_LNG);
//        locationManager.simulateLocation(loc);
//        locationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
//        assertEquals(loc, deviceInfo.getMostRecentLocation());
//    }

    @Test
    public void testNoLocation() {
        DeviceInfo deviceInfo = new DeviceInfo(context);
        Location recent = deviceInfo.getMostRecentLocation();
        assertNull(recent);
    }

    @Test
    public void testUseAdvertisingIdAsDeviceId() {
        PowerMockito.mockStatic(AdvertisingIdClient.class);
        String advertisingId = "advertisingId";
        AdvertisingIdClient.Info info = new AdvertisingIdClient.Info(
            advertisingId,
            false
        );

        try {
            Mockito.when(AdvertisingIdClient.getAdvertisingIdInfo(context)).thenReturn(info);
        } catch (Exception e) {
            fail(e.toString());
        }

        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        AmplitudeClient client = Amplitude.getInstance("ADID");
        client.useAdvertisingIdForDeviceId();
        client.initialize(context, "1cc2c1978ebab0f6451112a8f5df4f4e");
        ShadowLooper looper = Shadows.shadowOf(client.logThread.getLooper());
        looper.runToEndOfTasks();

        assertEquals(advertisingId, client.getDeviceId());
    }

    @Test
    public void testDontUseAdvertisingIdAsDeviceId() {
        PowerMockito.mockStatic(AdvertisingIdClient.class);
        String advertisingId = "advertisingId";
        AdvertisingIdClient.Info info = new AdvertisingIdClient.Info(
            advertisingId,
            true
        );

        try {
            Mockito.when(AdvertisingIdClient.getAdvertisingIdInfo(context)).thenReturn(info);
        } catch (Exception e) {
            fail(e.toString());
        }

        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        AmplitudeClient client = Amplitude.getInstance("NoADID");
        client.useAdvertisingIdForDeviceId();
        client.initialize(context, "1cc2c1978ebab0f6451112a8f5df4f4e");
        ShadowLooper looper = Shadows.shadowOf(client.logThread.getLooper());
        looper.runToEndOfTasks();

        assertNotEquals(advertisingId, client.getDeviceId());
        assertTrue(client.getDeviceId().endsWith("R"));
    }
}
