package com.amplitude.identitymanager;

import com.amplitude.api.AmplitudeLog;

public class Identity {
    private String userId;
    private String deviceId;
    private boolean shouldUseAdvertisingIdForDeviceId = false;
    private IdentityListener listener;

    public interface IdentityListener {
        void onUserIdChanged(String userId);
        void onDeviceIdChanged(String deviceId);
    }

    public Identity() {
        this(false, null);
    }

    public Identity(boolean useAdvertisingId) {
        this(useAdvertisingId, null);
    }

    public Identity(boolean useAdvertisingId, String userId) {
        this.userId = userId;
        this.shouldUseAdvertisingIdForDeviceId = useAdvertisingId;

        initializeDeviceId();
    }

    public String initializeDeviceId(String deviceId) {
        if (this.deviceId == null) {
            String deviceIdToUse = "";
            if (shouldUseAdvertisingIdForDeviceId) {
                deviceIdToUse = IdentityDeviceInfo.getAdvertisingId();
            } else {
                if (deviceId.length() > 0) {
                    deviceIdToUse = deviceId;
                } else {
                    deviceIdToUse = IdentityUtils.generateBase36Id();
                }
            }
            this.deviceId = deviceIdToUse;
        } else {
            AmplitudeLog.getLogger().w(Identity.class.getName(), "Cannot set device ID twice for same identity. Skipping operation.");
        }
        return this.deviceId;
    }

    public String initializeDeviceId() {
        return initializeDeviceId(null);
    }

    public String getDeviceId() {
        return deviceId;
    }

    // Should this exist? Otherwise, when would we call the onDeviceIdChanged listener?
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        if (listener != null) {
            listener.onDeviceIdChanged(deviceId);
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        if (listener != null) {
            listener.onUserIdChanged(userId);
        }
    }

    public void addIdentityChangedListener(IdentityListener listener){
        this.listener = listener;
    }

    public void useAdvertisingIdForDeviceId() {
        shouldUseAdvertisingIdForDeviceId = true;
    }
}
