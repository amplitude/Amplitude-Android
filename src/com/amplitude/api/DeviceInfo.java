package com.amplitude.api;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class DeviceInfo {

    public static final String TAG = "com.amplitude.api.DeviceInfo";

    public static final String OS_NAME = "android";

    private boolean locationListening = true;

    private Context context;

    // Cached properties, since fetching these take time
    private String advertisingId;
    private String country;
    private String versionName;
    private String osName;
    private String osVersion;
    private String brand;
    private String manufacturer;
    private String model;
    private String carrier;
    private String language;

    public DeviceInfo(Context context) {
        this.context = context;
    }

    public void init() {
        advertisingId = getAdvertisingId(context);
        versionName = getVersionName(context);
        osName = getOsName(context);
        osVersion = getOsVersion(context);
        brand = getBrand(context);
        manufacturer = getManufacturer(context);
        model = getModel(context);
        carrier = getCarrier(context);
        country = getCountry(context);
        language = getLanguage(context);
    }

    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public String getVersionName() {
        return versionName;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getBrand() {
        return brand;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getCountry() {
        return country;
    }

    public String getLanguage() {
        return language;
    }

    public String getAdvertisingId() {
        return advertisingId;
    }

    public Location getMostRecentLocation() {
        if (!isLocationListening()) {
            return null;
        }

        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        // Don't crash if the device does not have location services.
        if (locationManager == null) {
            return null;
        }

        List<String> providers = locationManager.getProviders(true);

        // It's possible that the location service is running out of process
        // and the remote getProviders call fails. Handle null provider lists.
        if (providers == null) {
            return null;
        }

        List<Location> locations = new ArrayList<Location>();
        for (String provider : providers) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                locations.add(location);
            }
        }

        long maximumTimestamp = -1;
        Location bestLocation = null;
        for (Location location : locations) {
            if (location.getTime() > maximumTimestamp) {
                maximumTimestamp = location.getTime();
                bestLocation = location;
            }
        }

        return bestLocation;
    }

    public boolean isLocationListening() {
        return locationListening;
    }

    public void setLocationListening(boolean locationListening) {
        this.locationListening = locationListening;
    }

    /**
     * Internal methods for getting raw information
     */

    private String getVersionName(Context context) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (NameNotFoundException e) {
        }
        return null;
    }

    private String getOsName(Context context) {
        return OS_NAME;
    }

    private String getOsVersion(Context context) {
        return Build.VERSION.RELEASE;
    }

    private String getBrand(Context context) {
        return Build.BRAND;
    }

    private String getManufacturer(Context context) {
        return Build.MANUFACTURER;
    }

    private String getModel(Context context) {
        return Build.MODEL;
    }

    private String getCarrier(Context context) {
        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getNetworkOperatorName();
    }

    private String getCountry(Context context) {
        // This should not be called on the main thread.

        // Prioritize reverse geocode, but until we have a result from that,
        // we try to grab the country from the network, and finally the locale
        String country = getCountryFromLocation();
        if (!TextUtils.isEmpty(country)) {
            return country;
        }

        country = getCountryFromNetwork();
        if (!TextUtils.isEmpty(country)) {
            return country;
        }
        return getCountryFromLocale();
    }

    // @VisibleForTesting
    protected Geocoder getGeocoder() {
        return new Geocoder(context, Locale.ENGLISH);
    }

    private String getCountryFromLocation() {
        if (!isLocationListening()) {
            return null;
        }

        Location recent = getMostRecentLocation();
        if (recent != null) {
            try {
                Geocoder geocoder = getGeocoder();
                List<Address> addresses = geocoder.getFromLocation(recent.getLatitude(),
                        recent.getLongitude(), 1);
                if (addresses != null) {
                    for (Address address : addresses) {
                        if (address != null) {
                            return address.getCountryCode();
                        }
                    }
                }
            } catch (IOException e) {
                // Failed to reverse geocode location
            }
        }
        return null;
    }

    private String getCountryFromNetwork() {
        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
            String country = manager.getNetworkCountryIso();
            if (country != null) {
                return country.toUpperCase(Locale.US);
            }
        }
        return null;
    }

    private String getCountryFromLocale() {
        return Locale.getDefault().getCountry();
    }

    private String getLanguage(Context context) {
        return Locale.getDefault().getLanguage();
    }

    private String getAdvertisingId(Context context) {
        // This should not be called on the main thread.
        try {
            Class AdvertisingIdClient = Class
                    .forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            Method getAdvertisingInfo = AdvertisingIdClient.getMethod("getAdvertisingIdInfo",
                    Context.class);
            Object advertisingInfo = getAdvertisingInfo.invoke(null, context);
            Method isLimitAdTrackingEnabled = advertisingInfo.getClass().getMethod(
                    "isLimitAdTrackingEnabled");
            Boolean limitAdTrackingEnabled = (Boolean) isLimitAdTrackingEnabled
                    .invoke(advertisingInfo);

            if (limitAdTrackingEnabled) {
                return null;
            }
            Method getId = advertisingInfo.getClass().getMethod("getId");
            advertisingId = (String) getId.invoke(advertisingInfo);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Google Play Services SDK not found!");
        } catch (Exception e) {
            Log.e(TAG, "Encountered an error connecting to Google Play Services", e);
        }
        return null;
    }

}
