package com.amplitude.identitymanager;

public class Identity {
    private String _userId;
    private String _deviceId;

    public String initializeDeviceId(String deviceId) {
        if (_deviceId == null) {
            String deviceIdToUse = "";
            if (deviceId.length() > 0) {
                deviceIdToUse = deviceId;
            } else {
                deviceIdToUse = IdentityUtils.generateBase36Id();
            }
            _deviceId = deviceIdToUse;
        } else {
            IdentityUtils.logIdentityWarning("Cannot set device ID twice for same identity. Skipping operation.");
        }

        return deviceId;
    }

    public String initializeDeviceId() {
        if (_deviceId == null) {
            _deviceId = IdentityUtils.generateBase36Id();
        } else {
            IdentityUtils.logIdentityWarning("Cannot set device ID twice for same identity. Skipping operation.");
        }

        return _deviceId;
    }

    public String getDeviceId() {
        if (_deviceId == null) {
            IdentityUtils.logIdentityWarning("Did not detect device ID; generating one for this instance.");
            return initializeDeviceId();
        } else {
            return _deviceId;
        }
    }

    public void setUserId(String userId) {
        _userId = userId;
    }

    public void setUserId(Integer userId) {
        _userId = userId.toString();
    }

    public String getUserId() {
        return _userId;
    }

    public void addIdentityChangedListener(){}

    public void useAdvertisingIdForDeviceId() {}
}
