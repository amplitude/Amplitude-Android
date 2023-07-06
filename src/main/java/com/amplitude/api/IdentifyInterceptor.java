package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IdentifyInterceptor
 * This is the internal class for handling identify events intercept and  optimize identify volumes.
 */
class IdentifyInterceptor {

    private static final String TAG = IdentifyInterceptor.class.getName();

    private final DatabaseHelper dbHelper;

    private final WorkerThread logThread;

    private long identifyBatchIntervalMillis;

    private final AtomicBoolean transferScheduled = new AtomicBoolean(false);

    private long lastIdentifyInterceptorId = -1;

    private final AmplitudeClient client;

    private String userId;
    private String deviceId;
    private final AtomicBoolean identitySet = new AtomicBoolean(false);

    public  IdentifyInterceptor (
            DatabaseHelper dbHelper,
            WorkerThread logThread,
            long identifyBatchIntervalMillis,
            AmplitudeClient client
    ) {
        this.dbHelper = dbHelper;
        this.logThread = logThread;
        this.identifyBatchIntervalMillis = identifyBatchIntervalMillis;
        if (dbHelper.getIdentifyInterceptorCount() > 0) {
            lastIdentifyInterceptorId = dbHelper.getLastIdentifyInterceptorId();
        }
        this.client = client;
    }

    /**
     * Intercept the event if it is identify with set action.
     *
     * @param eventType the event type
     * @param event full event data after middleware run
     * @return event with potentially more information or null if intercepted
     */
    public JSONObject intercept(String eventType, JSONObject event) {
        if (isIdentityUpdated(event)) {
            // if userId or deviceId is updated, send out the identify for older identity
            transferInterceptedIdentify();
        }
        if (eventType.equals(Constants.IDENTIFY_EVENT)) {
            if (isSetOnly(event) && !isSetGroups(event)) {
                // intercept and  save user properties
                lastIdentifyInterceptorId = saveIdentifyProperties(event);
                scheduleTransfer();
                return null;
            } else if(isClearAll(event)){
                // clear existing and return event
                dbHelper.removeIdentifyInterceptors(lastIdentifyInterceptorId);
                return event;
            } else {
                // send out the identify for older identity and event
                transferInterceptedIdentify();
                return event;
            }
        } else if (eventType.equals(Constants.GROUP_IDENTIFY_EVENT)) {
            // no op
            return event;
        } else {
            // send out the identify for older identity and event
            transferInterceptedIdentify();
            return event;
        }
    }

    /**
     * Sets min time for identify batch millis.
     *
     * @param identifyBatchIntervalMillis the time interval for identify batch interval
     */
    public void setIdentifyBatchIntervalMillis(long identifyBatchIntervalMillis) {
        this.identifyBatchIntervalMillis = identifyBatchIntervalMillis;
    }

    private JSONObject getTransferIdentifyEvent() {
        try {
            List<JSONObject> identifys = dbHelper.getIdentifyInterceptors(lastIdentifyInterceptorId, -1);
            if (identifys.isEmpty()) {
                return null;
            }
            JSONObject identifyEvent = identifys.get(0);
            JSONObject identifyEventUserProperties = identifyEvent.getJSONObject("user_properties").getJSONObject(Constants.AMP_OP_SET);
            JSONObject userProperties = mergeIdentifyInterceptList(identifys.subList(1, identifys.size()));
            mergeUserProperties(identifyEventUserProperties, userProperties);
            identifyEvent.getJSONObject("user_properties").put(Constants.AMP_OP_SET, identifyEventUserProperties);
            dbHelper.removeIdentifyInterceptors(lastIdentifyInterceptorId);
            return identifyEvent;
        } catch (JSONException e) {
            AmplitudeLog.getLogger().w(TAG, "Identify Merge error: " + e.getMessage());
        }
        return null;
    }

    private void scheduleTransfer() {
        if (transferScheduled.getAndSet(true)) {
            return;
        }

        logThread.postDelayed(new Runnable() {
            @Override
            public void run() {
                transferScheduled.set(false);
                transferInterceptedIdentify();
            }
        }, identifyBatchIntervalMillis);
    }

    public void transferInterceptedIdentify() {
        JSONObject identifyEvent = getTransferIdentifyEvent();
        if (identifyEvent == null) {
            return;
        }
        client.saveEvent(Constants.IDENTIFY_EVENT, identifyEvent);
    }

    private JSONObject mergeIdentifyInterceptList(List<JSONObject> identifys) throws JSONException {
        JSONObject userProperties = new JSONObject();
        for (JSONObject identify : identifys) {
            JSONObject setUserProperties = identify.getJSONObject("user_properties")
                    .getJSONObject(Constants.AMP_OP_SET);
            mergeUserProperties(userProperties, setUserProperties);
        }
        return userProperties;
    }

    private void mergeUserProperties(JSONObject userProperties, JSONObject userPropertiesToMerge) throws JSONException {
        Iterator<?> keys = userPropertiesToMerge.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (userPropertiesToMerge.get(key) != null && userPropertiesToMerge.get(key) != JSONObject.NULL) {
                userProperties.put(key, userPropertiesToMerge.get(key));
            }
        }
    }

    private boolean isSetOnly(JSONObject event) {
        return isActionOnly(event, Constants.AMP_OP_SET);
    }

    private boolean isClearAll(JSONObject event) {
        return isActionOnly(event, Constants.AMP_OP_CLEAR_ALL);
    }

    private boolean isSetGroups(JSONObject event) {
        try {
            return event.getJSONObject("groups").length() > 0;
        } catch (JSONException e) {
            return false;
        }
    }

    private boolean isActionOnly(JSONObject event, String action) {
        try {
            JSONObject userProperties = event.getJSONObject("user_properties");
            return userProperties.length() == 1 && userProperties.has(action);
        } catch (JSONException e) {
            return false;
        }
    }

    private long saveIdentifyProperties(JSONObject event) {
        return dbHelper.addIdentifyInterceptor(event.toString());
    }

    private boolean isIdentityUpdated(JSONObject event) {
        try {
            if (!identitySet.getAndSet(true)) {
                userId = event.getString("user_id");
                deviceId = event.getString("device_id");
                return true;
            }
            boolean isUpdated = false;
            if (isIdUpdated(userId, event.getString("user_id"))) {
                userId = event.getString("user_id");
                isUpdated = true;
            }
            if (isIdUpdated(deviceId, event.getString("device_id"))) {
                deviceId = event.getString("device_id");
                isUpdated = true;
            }
            return isUpdated;
        } catch (JSONException e) {
            return true;
        }
    }

    private boolean isIdUpdated(String id, String updateId) {
        if (id == null && updateId == null) {
            return false;
        }
        if (id == null || updateId == null) {
            return true;
        }
        return !id.equals(updateId);
    }
}
