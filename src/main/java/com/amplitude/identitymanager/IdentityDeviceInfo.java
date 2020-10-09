package com.amplitude.identitymanager;

import android.os.Build;

public class IdentityDeviceInfo {
    public static String getAdvertisingId() {
        // This should not be called on the main thread.
        if ("Amazon".equals(getManufacturer())) {
            return getAndCacheAmazonAdvertisingId();
        } else {
            return getAndCacheGoogleAdvertisingId();
        }
    }

    private static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    private static String getAndCacheAmazonAdvertisingId() {
        return "amazonadvertisingid";
    }

    private static String getAndCacheGoogleAdvertisingId() {
        return "advertisingid";
    }
}

