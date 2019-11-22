package com.amplitude.api;

import android.location.Geocoder;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.maps.ShadowGeocoder;

// mock for static Geocoder method
@Implements(Geocoder.class)
public class MockGeocoder extends ShadowGeocoder {
    @Implementation
    public static boolean isPresent() {
        return true;
    }
}
