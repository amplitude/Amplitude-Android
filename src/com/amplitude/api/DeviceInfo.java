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
import android.util.Log;

public class DeviceInfo {

    public static final String TAG = "com.amplitude.api.DeviceInfo";

    private Context context;
    
    // Cached properties, since fetching these take time
    private String advertisingId;
    private String country;
    
    public DeviceInfo(Context context) {
        this.context = context;
    }
    
    public int getVersionCode() {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
        }
        return 0;
    }

    public String getVersionName() {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (NameNotFoundException e) {
        }
        return null;
    }

    public int getBuildVersionSdk() {
        return Build.VERSION.SDK_INT;
    }

    public String getBuildVersionRelease() {
        return Build.VERSION.RELEASE;
    }

    public String getPhoneBrand() {
        return Build.BRAND;
    }

    public String getPhoneManufacturer() {
        return Build.MANUFACTURER;
    }

    public String getPhoneModel() {
        return Build.MODEL;
    }

    public String getPhoneCarrier() {
        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getNetworkOperatorName();
    }
    
    public String getCountry() {
        if (country == null) {
            country = getCountryUncached();
        }
        return country;
    }

    private String getCountryUncached() {
        // This should not be called on the main thread.

        // Prioritize reverse geocode, but until we have a result from that,
        // we try to grab the country from the network, and finally the locale
        Location recent = getMostRecentLocation(context);
        if (recent != null) {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        recent.getLatitude(), recent.getLongitude(), 1);
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

        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
            String country = manager.getNetworkCountryIso();
            if (country != null) {
                return country.toUpperCase(Locale.US);
            }
        }
        return Locale.getDefault().getCountry();
    }

    public String getLanguage() {
        return Locale.getDefault().getLanguage();
    }
    
    public String getAdvertisingId() {
        // This should not be called on the main thread.
        if (advertisingId == null) {
            try {
                Class AdvertisingIdClient = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                Method getAdvertisingInfo = AdvertisingIdClient.getMethod("getAdvertisingIdInfo", Context.class);
                Object advertisingInfo = getAdvertisingInfo.invoke(null, context);
                Method isLimitAdTrackingEnabled = advertisingInfo.getClass().getMethod("isLimitAdTrackingEnabled");
                Boolean limitAdTrackingEnabled = (Boolean) isLimitAdTrackingEnabled.invoke(advertisingInfo);

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
        }
        return advertisingId;
    }
    
    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static Location getMostRecentLocation(Context context) {
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
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

}
