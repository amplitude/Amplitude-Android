package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class IdentifyInterceptor {

    private static final String TAG = IdentifyInterceptor.class.getName();

    private final DatabaseHelper dbHelper;

    private final WorkerThread logThread;

    private long identifyBatchIntervalMillis;

    private final AtomicBoolean transferScheduled = new AtomicBoolean(false);

    private long lastIdentifyInterceptorId = -1;

    private final AmplitudeClient client;

    public  IdentifyInterceptor (
            DatabaseHelper dbHelper,
            WorkerThread logThread,
            long identifyBatchIntervalMillis,
            AmplitudeClient client
    ) {
        this.dbHelper = dbHelper;
        this.logThread = logThread;
        this.identifyBatchIntervalMillis = identifyBatchIntervalMillis;
        lastIdentifyInterceptorId = dbHelper.getLastIdentifyInterceptorId();
        this.client = client;
    }

    public JSONObject intercept(String eventType, JSONObject event) {
        if (eventType.equals(Constants.IDENTIFY_EVENT)) {
            if (isSetOnly(event)) {
                // intercept and  save user properties
                lastIdentifyInterceptorId = saveIdentifyProperties(event);
                scheduleTransfer();
                return null;
            } else if(isClearAll(event)){
                // clear existing and return event
                dbHelper.removeIdentifyInterceptors(lastIdentifyInterceptorId);
                return event;
            } else {
                // Fetch and merge event
                return fetchAndMergeToIdentifyEvent(event);
            }
        } else if (eventType.equals(Constants.GROUP_IDENTIFY_EVENT)) {
            // no op
            return event;
        } else {
            // fetch, merge and attach user properties
            return fetchAndMergeToNormalEvent(event);
        }
    }

    public void setIdentifyBatchIntervalMillis(long identifyBatchIntervalMillis) {
        this.identifyBatchIntervalMillis = identifyBatchIntervalMillis;
    }

    private JSONObject fetchAndMergeToIdentifyEvent(JSONObject event) {
        try {
            List<JSONObject> identifys = dbHelper.getIdentifyInterceptors(lastIdentifyInterceptorId, -1);
            if (identifys.isEmpty()) {
                return event;
            }
            JSONObject identifyEventUserProperties = event.getJSONObject("user_properties");
            JSONObject userProperties = mergeIdentifyInterceptList(identifys);
            if (identifyEventUserProperties.has(Constants.AMP_OP_SET)) {
                mergeUserProperties(userProperties, identifyEventUserProperties.getJSONObject(Constants.AMP_OP_SET));
            }
            identifyEventUserProperties.put(Constants.AMP_OP_SET, userProperties);
            event.put("user_properties", identifyEventUserProperties);
            dbHelper.removeIdentifyInterceptors(lastIdentifyInterceptorId);
            return event;
        } catch (JSONException e) {
            AmplitudeLog.getLogger().w(TAG, "Identify Merge error: " + e.getMessage());
        }
        return event;
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

    private JSONObject fetchAndMergeToNormalEvent(JSONObject event) {
        try {
            List<JSONObject> identifys = dbHelper.getIdentifyInterceptors(lastIdentifyInterceptorId, -1);
            if (identifys.isEmpty()) {
                return event;
            }
            JSONObject userProperties = mergeIdentifyInterceptList(identifys);
            mergeUserProperties(userProperties, event.getJSONObject("user_properties"));
            event.put("user_properties", userProperties);
            dbHelper.removeIdentifyInterceptors(lastIdentifyInterceptorId);
        } catch (JSONException e) {
            AmplitudeLog.getLogger().w(TAG, "Identify Merge error: " + e.getMessage());
        }
        return event;
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
            userProperties.put(key, userPropertiesToMerge.get(key));
        }
    }

    private boolean isSetOnly(JSONObject event) {
        return isActionOnly(event, Constants.AMP_OP_SET);
    }

    private boolean isClearAll(JSONObject event) {
        return isActionOnly(event, Constants.AMP_OP_CLEAR_ALL);
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
}
