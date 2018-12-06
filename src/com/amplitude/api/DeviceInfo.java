package com.amplitude.api;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DeviceInfo {

    public static final String TAG = "com.amplitude.api.DeviceInfo";

    public static final String OS_NAME = "android";

    private static final String SETTING_LIMIT_AD_TRACKING = "limit_ad_tracking";
    private static final String SETTING_ADVERTISING_ID = "advertising_id";

    private boolean locationListening = true;

    private Context context;

    private CachedInfo cachedInfo;

    /**
     * Internal class serves as a cache
     */
    private class CachedInfo {
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
        private boolean limitAdTrackingEnabled;
        private boolean gpsEnabled; // google play services

        private CachedInfo() {
            advertisingId = getAdvertisingId();
            versionName = getVersionName();
            osName = getOsName();
            osVersion = getOsVersion();
            brand = getBrand();
            manufacturer = getManufacturer();
            model = getModel();
            carrier = getCarrier();
            country = getCountry();
            language = getLanguage();
            gpsEnabled = checkGPSEnabled();
        }

        /**
         * Internal methods for getting raw information
         */

        private String getVersionName() {
            PackageInfo packageInfo;
            try {
                packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return packageInfo.versionName;
            } catch (NameNotFoundException e) {
                Diagnostics.getLogger().logError("Failed to get version name", e);
            }
            return null;
        }

        private String getOsName() {
            return OS_NAME;
        }

        private String getOsVersion() {
            return Build.VERSION.RELEASE;
        }

        private String getBrand() {
            return Build.BRAND;
        }

        private String getManufacturer() {
            return Build.MANUFACTURER;
        }

        private String getModel() {
            return Build.MODEL;
        }

        private String getCarrier() {
            try {
                TelephonyManager manager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                return manager.getNetworkOperatorName();
            } catch (Exception e) {
                // Failed to get network operator name from network
                Diagnostics.getLogger().logError("Failed to get carrier", e);
            }
            return null;
        }

        private String getCountry() {
            // This should not be called on the main thread.

            // Prioritize reverse geocode, but until we have a result from that,
            // we try to grab the country from the network, and finally the locale
            String country = getCountryFromLocation();
            if (!Utils.isEmptyString(country)) {
                return country;
            }

            country = getCountryFromNetwork();
            if (!Utils.isEmptyString(country)) {
                return country;
            }
            return getCountryFromLocale();
        }

        private String getCountryFromLocation() {
            if (!isLocationListening()) {
                return null;
            }

            Location recent = getMostRecentLocation();
            if (recent != null) {
                try {
                    if (Geocoder.isPresent()) {
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
                    }
                } catch (IOException e) {
                    // Failed to reverse geocode location
                    Diagnostics.getLogger().logError("Failed to get country from location", e);
                } catch (NullPointerException e) {
                    // Failed to reverse geocode location
                    Diagnostics.getLogger().logError("Failed to get country from location", e);
                } catch (NoSuchMethodError e) {
                    // failed to fetch geocoder
                    Diagnostics.getLogger().logError("Failed to get country from location", e);
                } catch (IllegalArgumentException e) {
                    // Bad lat / lon values can cause Geocoder to throw IllegalArgumentExceptions
                    Diagnostics.getLogger().logError("Failed to get country from location", e);
                } catch (IllegalStateException e) {
                    // sometimes the location manager is unavailable
                    Diagnostics.getLogger().logError("Failed to get country from location", e);
                }
            }
            return null;
        }

        private String getCountryFromNetwork() {
            try {
                TelephonyManager manager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                if (manager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
                    String country = manager.getNetworkCountryIso();
                    if (country != null) {
                        return country.toUpperCase(Locale.US);
                    }
                }
            } catch (Exception e) {
                // Failed to get country from network
                Diagnostics.getLogger().logError("Failed to get country from network", e);
            }
            return null;
        }

        private String getCountryFromLocale() {
            return Locale.getDefault().getCountry();
        }

        private String getLanguage() {
            return Locale.getDefault().getLanguage();
        }

        private String getAdvertisingId() {
            // This should not be called on the main thread.
            if ("Amazon".equals(getManufacturer())) {
                return getAndCacheAmazonAdvertisingId();
            } else {
                return getAndCacheGoogleAdvertisingId();
            }
        }

        private String getAndCacheAmazonAdvertisingId() {
            ContentResolver cr = context.getContentResolver();

            limitAdTrackingEnabled = Secure.getInt(cr, SETTING_LIMIT_AD_TRACKING, 0) == 1;
            advertisingId = Secure.getString(cr, SETTING_ADVERTISING_ID);

            return advertisingId;
        }

        private String getAndCacheGoogleAdvertisingId() {
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
                this.limitAdTrackingEnabled =
                        limitAdTrackingEnabled != null && limitAdTrackingEnabled;
                Method getId = advertisingInfo.getClass().getMethod("getId");
                advertisingId = (String) getId.invoke(advertisingInfo);
            } catch (ClassNotFoundException e) {
                AmplitudeLog.getLogger().w(TAG, "Google Play Services SDK not found!");
                Diagnostics.getLogger().logError("Failed to get ADID", e);
            } catch (InvocationTargetException e) {
                AmplitudeLog.getLogger().w(TAG, "Google Play Services not available");
                Diagnostics.getLogger().logError("Failed to get ADID", e);
            } catch (Exception e) {
                AmplitudeLog.getLogger().e(TAG, "Encountered an error connecting to Google Play Services", e);
                Diagnostics.getLogger().logError("Failed to get ADID", e);
            }

            return advertisingId;
        }

        private boolean checkGPSEnabled() {
            // This should not be called on the main thread.
            try {
                Class GPSUtil = Class
                        .forName("com.google.android.gms.common.GooglePlayServicesUtil");
                Method getGPSAvailable = GPSUtil.getMethod("isGooglePlayServicesAvailable",
                        Context.class);
                Integer status = (Integer) getGPSAvailable.invoke(null, context);
                // status 0 corresponds to com.google.android.gms.common.ConnectionResult.SUCCESS;
                return status != null && status.intValue() == 0;
            } catch (NoClassDefFoundError e) {
                AmplitudeLog.getLogger().w(TAG, "Google Play Services Util not found!");
                Diagnostics.getLogger().logError("Failed to check GPS enabled", e);
            } catch (ClassNotFoundException e) {
                AmplitudeLog.getLogger().w(TAG, "Google Play Services Util not found!");
                Diagnostics.getLogger().logError("Failed to check GPS enabled", e);
            } catch (NoSuchMethodException e) {
                AmplitudeLog.getLogger().w(TAG, "Google Play Services not available");
                Diagnostics.getLogger().logError("Failed to check GPS enabled", e);
            } catch (InvocationTargetException e) {
                AmplitudeLog.getLogger().w(TAG, "Google Play Services not available");
                Diagnostics.getLogger().logError("Failed to check GPS enabled", e);
            } catch (IllegalAccessException e) {
                AmplitudeLog.getLogger().w(TAG, "Google Play Services not available");
                Diagnostics.getLogger().logError("Failed to check GPS enabled", e);
            } catch (Exception e) {
                AmplitudeLog.getLogger().w(TAG,
                        "Error when checking for Google Play Services: " + e);
                Diagnostics.getLogger().logError("Failed to check GPS enabled", e);
            }
            return false;
        }
    }

    public DeviceInfo(Context context) {
        this.context = context;
    }

    private CachedInfo getCachedInfo() {
        if (cachedInfo == null) {
            cachedInfo = new CachedInfo();
        }
        return cachedInfo;
    }

    public void prefetch() {
        getCachedInfo();
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public String getVersionName() {
        return getCachedInfo().versionName;
    }

    public String getOsName() {
        return getCachedInfo().osName;
    }

    public String getOsVersion() {
        return getCachedInfo().osVersion;
    }

    public String getBrand() {
        return getCachedInfo().brand;
    }

    public String getManufacturer() {
        return getCachedInfo().manufacturer;
    }

    public String getModel() {
        return getCachedInfo().model;
    }

    public String getCarrier() {
        return getCachedInfo().carrier;
    }

    public String getCountry() {
        return getCachedInfo().country;
    }

    public String getLanguage() {
        return getCachedInfo().language;
    }

    public String getAdvertisingId() {
        return getCachedInfo().advertisingId;
    }

    public boolean isLimitAdTrackingEnabled() {
        return getCachedInfo().limitAdTrackingEnabled;
    }

    public boolean isGooglePlayServicesEnabled() { return getCachedInfo().gpsEnabled; }

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

        // It's possible that the location service is running out of process
        // and the remote getProviders call fails. Handle null provider lists.
        List<String> providers = null;
        try {
            providers = locationManager.getProviders(true);
        } catch (SecurityException e) {
            // failed to get providers list
            Diagnostics.getLogger().logError("Failed to get most recent location", e);
        }
        if (providers == null) {
            return null;
        }

        List<Location> locations = new ArrayList<Location>();
        for (String provider : providers) {
            Location location = null;
            try {
                location = locationManager.getLastKnownLocation(provider);
            } catch (IllegalArgumentException e) {
                // failed to get last known location from provider
                Diagnostics.getLogger().logError("Failed to get most recent location", e);
            } catch (SecurityException e) {
                // failed to get last known location from provider
                Diagnostics.getLogger().logError("Failed to get most recent location", e);
            }
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

    // @VisibleForTesting
    protected Geocoder getGeocoder() {
        return new Geocoder(context, Locale.ENGLISH);
    }

}
