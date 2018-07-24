package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class TrackingOptions {

    private static String[] SERVER_SIDE_PROPERTIES = {
        Constants.AMP_TRACKING_OPTION_CITY,
        Constants.AMP_TRACKING_OPTION_COUNTRY,
        Constants.AMP_TRACKING_OPTION_DMA,
        Constants.AMP_TRACKING_OPTION_IP_ADDRESS,
        Constants.AMP_TRACKING_OPTION_LAT_LNG,
        Constants.AMP_TRACKING_OPTION_REGION,
    };

    public static final String TAG = "com.amplitude.api.TrackingOptions";

    Set<String> disabledFields = new HashSet<String>();

    public TrackingOptions disableAdid() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_ADID);
        return this;
    }

    boolean shouldTrackAdid() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_ADID);
    }

    public TrackingOptions disableCarrier() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_CARRIER);
        return this;
    }

    boolean shouldTrackCarrier() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_CARRIER);
    }

    public TrackingOptions disableCity() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_CITY);
        return this;
    }

    boolean shouldTrackCity() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_CITY);
    }

    public TrackingOptions disableCountry() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_COUNTRY);
        return this;
    }

    boolean shouldTrackCountry() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_COUNTRY);
    }

    public TrackingOptions disableDeviceBrand() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_DEVICE_BRAND);
        return this;
    }

    boolean shouldTrackDeviceBrand() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_DEVICE_BRAND);
    }

    public TrackingOptions disableDeviceManufacturer() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_DEVICE_MANUFACTURER);
        return this;
    }

    boolean shouldTrackDeviceManufacturer() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_DEVICE_MANUFACTURER);
    }

    public TrackingOptions disableDeviceModel() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_DEVICE_MODEL);
        return this;
    }

    boolean shouldTrackDeviceModel() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_DEVICE_MODEL);
    }

    public TrackingOptions disableDma() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_DMA);
        return this;
    }

    boolean shouldTrackDma() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_DMA);
    }

    public TrackingOptions disableIpAddress() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_IP_ADDRESS);
        return this;
    }

    boolean shouldTrackIpAddress() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_IP_ADDRESS);
    }

    public TrackingOptions disableLanguage() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_LANGUAGE);
        return this;
    }

    boolean shouldTrackLanguage() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_LANGUAGE);
    }

    public TrackingOptions disableLatLng() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_LAT_LNG);
        return this;
    }

    boolean shouldTrackLatLng() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_LAT_LNG);
    }

    public TrackingOptions disableOsName() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_OS_NAME);
        return this;
    }

    boolean shouldTrackOsName() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_OS_NAME);
    }

    public TrackingOptions disableOsVersion() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_OS_VERSION);
        return this;
    }

    boolean shouldTrackOsVersion() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_OS_VERSION);
    }

    public TrackingOptions disablePlatform() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_PLATFORM);
        return this;
    }

    boolean shouldTrackPlatform() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_PLATFORM);
    }

    public TrackingOptions disableRegion() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_REGION);
        return this;
    }

    boolean shouldTrackRegion() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_REGION);
    }

    public TrackingOptions disableVersionName() {
        disableTrackingField(Constants.AMP_TRACKING_OPTION_VERSION_NAME);
        return this;
    }

    boolean shouldTrackVersionName() {
        return shouldTrackField(Constants.AMP_TRACKING_OPTION_VERSION_NAME);
    }

    private void disableTrackingField(String field) {
        disabledFields.add(field);
    }

    protected JSONObject getApiPropertiesTrackingOptions() {
        JSONObject apiPropertiesTrackingOptions = new JSONObject();
        if (disabledFields.isEmpty()) {
            return apiPropertiesTrackingOptions;
        }

        for (String key : SERVER_SIDE_PROPERTIES) {
            if (disabledFields.contains(key)) {
                try {
                    apiPropertiesTrackingOptions.put(key, false);
                } catch (JSONException e) {
                    AmplitudeLog.getLogger().e(TAG, e.toString());
                }
            }
        }
        return apiPropertiesTrackingOptions;
    }

    private boolean shouldTrackField(String field) {
        return !disabledFields.contains(field);
    }
}
