package com.amplitude.identitymanager;

import android.util.Log;

public class Identity {
    private String _userId;
    private String _deviceId;

    private void logIdentityWarning(String message) {
        Log.w(Identity.class.getName(), message);
    }

    public String initializeDeviceId(String deviceId) {
        if (deviceId == null) {
            String deviceIdToUse = "";
            if (deviceId.length() > 0) {
                deviceIdToUse = deviceId;
            } else {
                deviceIdToUse = "generateBase36Id";
            }
            _deviceId = deviceIdToUse;
        } else {
            logIdentityWarning("Cannot set device ID twice for same identity. Skipping operation.");
        }

        return deviceId;
    }

    public String initializeDeviceId() {
        if (_deviceId == null) {
            _deviceId = "generateBase36Id";
        } else {
            logIdentityWarning("Cannot set device ID twice for same identity. Skipping operation.");
        }

        return _deviceId;
    }

    public String getDeviceId() {
        if (_deviceId == null) {
            logIdentityWarning("Did not detect device ID; generating one for this instance.");
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
