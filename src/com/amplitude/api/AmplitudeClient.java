package com.amplitude.api;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import android.util.Pair;

import com.amplitude.security.MD5;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AmplitudeClient {

    public static final String TAG = "com.amplitude.api.AmplitudeClient";

    public static final String START_SESSION_EVENT = "session_start";
    public static final String END_SESSION_EVENT = "session_end";
    public static final String REVENUE_EVENT = "revenue_amount";

    // database valueStore keys
    public static final String DEVICE_ID_KEY = "device_id";
    public static final String USER_ID_KEY = "user_id";
    public static final String OPT_OUT_KEY = "opt_out";
    public static final String SEQUENCE_NUMBER_KEY = "sequence_number";
    public static final String LAST_EVENT_TIME_KEY = "last_event_time";
    public static final String LAST_EVENT_ID_KEY = "last_event_id";
    public static final String LAST_IDENTIFY_ID_KEY = "last_identify_id";
    public static final String PREVIOUS_SESSION_ID_KEY = "previous_session_id";


    protected static AmplitudeClient instance = new AmplitudeClient();

    public static AmplitudeClient getInstance() {
        return instance;
    }

    private static final AmplitudeLog logger = AmplitudeLog.getLogger();

    protected Context context;
    protected OkHttpClient httpClient;
    protected String apiKey;
    protected String userId;
    protected String deviceId;
    protected String sharedPreferencesName;
    private boolean newDeviceIdPerInstall = false;
    private boolean useAdvertisingIdForDeviceId = false;
    private boolean initialized = false;
    private boolean optOut = false;
    private boolean offline = false;

    private DeviceInfo deviceInfo;

    long sessionId = -1;
    private int eventUploadThreshold = Constants.EVENT_UPLOAD_THRESHOLD;
    private int eventUploadMaxBatchSize = Constants.EVENT_UPLOAD_MAX_BATCH_SIZE;
    private int eventMaxCount = Constants.EVENT_MAX_COUNT;
    private long eventUploadPeriodMillis = Constants.EVENT_UPLOAD_PERIOD_MILLIS;
    private long minTimeBetweenSessionsMillis = Constants.MIN_TIME_BETWEEN_SESSIONS_MILLIS;
    private long sessionTimeoutMillis = Constants.SESSION_TIMEOUT_MILLIS;
    private boolean backoffUpload = false;
    private int backoffUploadBatchSize = eventUploadMaxBatchSize;
    private boolean usingForegroundTracking = false;
    private boolean trackingSessionEvents = false;
    private boolean inForeground = false;

    private AtomicBoolean updateScheduled = new AtomicBoolean(false);
    AtomicBoolean uploadingCurrently = new AtomicBoolean(false);

    // Let test classes have access to these properties.
    Throwable lastError;
    String url = Constants.EVENT_LOG_URL;
    WorkerThread logThread = new WorkerThread("logThread");
    WorkerThread httpThread = new WorkerThread("httpThread");

    public AmplitudeClient() {
        logThread.start();
        httpThread.start();
    }

    public AmplitudeClient initialize(Context context, String apiKey) {
        return initialize(context, apiKey, null);
    }

    public synchronized AmplitudeClient initialize(Context context, String apiKey, String userId) {
        if (context == null) {
            logger.e(TAG, "Argument context cannot be null in initialize()");
            return instance;
        }

        AmplitudeClient.upgradePrefs(context);
        AmplitudeClient.upgradeSharedPrefsToDB(context);

        if (TextUtils.isEmpty(apiKey)) {
            logger.e(TAG, "Argument apiKey cannot be null or blank in initialize()");
            return instance;
        }
        if (!initialized) {
            this.context = context.getApplicationContext();
            this.httpClient = new OkHttpClient();
            this.apiKey = apiKey;
            this.sharedPreferencesName = getSharedPreferencesName();
            initializeDeviceInfo();

            DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(this.context);
            if (userId != null) {
                dbHelper.insertOrReplaceKeyValue(USER_ID_KEY, userId);
            } else {
                this.userId = dbHelper.getValue(USER_ID_KEY);
            }
            Long optOut = dbHelper.getLongValue(OPT_OUT_KEY);
            this.optOut = optOut != null && optOut == 1;

            // try to restore previous session id
            long previousSessionId = getPreviousSessionId();
            if (previousSessionId >= 0) {
                sessionId = previousSessionId;
            }

            initialized = true;
        }

        return instance;
    }

    public AmplitudeClient enableForegroundTracking(Application app) {
        if (usingForegroundTracking || !contextAndApiKeySet("enableForegroundTracking()")) {
            return instance;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            app.registerActivityLifecycleCallbacks(new AmplitudeCallbacks(instance));
        }

        return instance;
    }

    private void initializeDeviceInfo() {
        deviceInfo = new DeviceInfo(context);
        runOnLogThread(new Runnable() {

            @Override
            public void run() {
                deviceId = initializeDeviceId();
                deviceInfo.prefetch();
            }
        });
    }

    public AmplitudeClient enableNewDeviceIdPerInstall(boolean newDeviceIdPerInstall) {
        this.newDeviceIdPerInstall = newDeviceIdPerInstall;
        return instance;
    }

    public AmplitudeClient useAdvertisingIdForDeviceId() {
        this.useAdvertisingIdForDeviceId = true;
        return instance;
    }

    public AmplitudeClient enableLocationListening() {
        if (deviceInfo == null) {
            throw new IllegalStateException(
                    "Must initialize before acting on location listening.");
        }
        deviceInfo.setLocationListening(true);
        return instance;
    }

    public AmplitudeClient disableLocationListening() {
        if (deviceInfo == null) {
            throw new IllegalStateException(
                    "Must initialize before acting on location listening.");
        }
        deviceInfo.setLocationListening(false);
        return instance;
    }

    public AmplitudeClient setEventUploadThreshold(int eventUploadThreshold) {
        this.eventUploadThreshold = eventUploadThreshold;
        return instance;
    }

    public AmplitudeClient setEventUploadMaxBatchSize(int eventUploadMaxBatchSize) {
        this.eventUploadMaxBatchSize = eventUploadMaxBatchSize;
        this.backoffUploadBatchSize = eventUploadMaxBatchSize;
        return instance;
    }

    public AmplitudeClient setEventMaxCount(int eventMaxCount) {
        this.eventMaxCount = eventMaxCount;
        return instance;
    }

    public AmplitudeClient setEventUploadPeriodMillis(int eventUploadPeriodMillis) {
        this.eventUploadPeriodMillis = eventUploadPeriodMillis;
        return instance;
    }

    public AmplitudeClient setMinTimeBetweenSessionsMillis(long minTimeBetweenSessionsMillis) {
        this.minTimeBetweenSessionsMillis = minTimeBetweenSessionsMillis;
        return instance;
    }

    public AmplitudeClient setSessionTimeoutMillis(long sessionTimeoutMillis) {
        this.sessionTimeoutMillis = sessionTimeoutMillis;
        return instance;
    }

    public AmplitudeClient setOptOut(boolean optOut) {
        if (!contextAndApiKeySet("setOptOut()")) {
            return instance;
        }

        this.optOut = optOut;
        DatabaseHelper.getDatabaseHelper(context).insertOrReplaceKeyLongValue(
            OPT_OUT_KEY, optOut ? 1L : 0L
        );

        return instance;
    }

    public boolean isOptOut() {
        return optOut;
    }

    public AmplitudeClient enableLogging(boolean enableLogging) {
        logger.setEnableLogging(enableLogging);
        return instance;
    }

    public AmplitudeClient setLogLevel(int logLevel) {
        logger.setLogLevel(logLevel);
        return instance;
    }

    public AmplitudeClient setOffline(boolean offline) {
        this.offline = offline;

        // Try to update to the server once offline mode is disabled.
        if (!offline) {
            uploadEvents();
        }

        return instance;
    }

    public AmplitudeClient trackSessionEvents(boolean trackingSessionEvents) {
        this.trackingSessionEvents = trackingSessionEvents;
        return instance;
    }

    void useForegroundTracking() {
        usingForegroundTracking = true;
    }

    boolean isUsingForegroundTracking() { return usingForegroundTracking; }

    boolean isInForeground() { return inForeground; }

    public void logEvent(String eventType) {
        logEvent(eventType, null);
    }

    public void logEvent(String eventType, JSONObject eventProperties) {
        logEvent(eventType, eventProperties, false);
    }

    public void logEvent(String eventType, JSONObject eventProperties, boolean outOfSession) {
        logEvent(eventType, eventProperties, null, outOfSession);
    }

    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups) {
        logEvent(eventType, eventProperties, groups, false);
    }

    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups, boolean outOfSession) {
        if (validateLogEvent(eventType)) {
            logEventAsync(
                eventType, eventProperties, null, null, groups, getCurrentTimeMillis(), outOfSession
            );
        }
    }

    public void logEventSync(String eventType) {
        logEventSync(eventType, null);
    }

    public void logEventSync(String eventType, JSONObject eventProperties) {
        logEventSync(eventType, eventProperties, false);
    }

    public void logEventSync(String eventType, JSONObject eventProperties, boolean outOfSession) {
        logEventSync(eventType, eventProperties, null, outOfSession);
    }

    public void logEventSync(String eventType, JSONObject eventProperties, JSONObject group) {
        logEventSync(eventType, eventProperties, group, false);
    }

    public void logEventSync(String eventType, JSONObject eventProperties, JSONObject groups, boolean outOfSession) {
        if (validateLogEvent(eventType)) {
            logEvent(
                eventType, eventProperties, null, null, groups, getCurrentTimeMillis(), outOfSession
            );
        }
    }

    protected boolean validateLogEvent(String eventType) {
        if (TextUtils.isEmpty(eventType)) {
            logger.e(TAG, "Argument eventType cannot be null or blank in logEvent()");
            return false;
        }

        if (!contextAndApiKeySet("logEvent()")) {
            return false;
        }

        return true;
    }

    protected void logEventAsync(final String eventType, JSONObject eventProperties,
            final JSONObject apiProperties, JSONObject userProperties,
            JSONObject groups, final long timestamp, final boolean outOfSession) {
        // Clone the incoming eventProperties object before sending over
        // to the log thread. Helps avoid ConcurrentModificationException
        // if the caller starts mutating the object they passed in.
        // Only does a shallow copy, so it's still possible, though unlikely,
        // to hit concurrent access if the caller mutates deep in the object.
        if (eventProperties != null) {
            eventProperties = cloneJSONObject(eventProperties);
        }

        if (userProperties != null) {
            userProperties = cloneJSONObject(userProperties);
        }

        if (groups != null) {
            groups = cloneJSONObject(groups);
        }

        final JSONObject copyEventProperties = eventProperties;
        final JSONObject copyUserProperties = userProperties;
        final JSONObject copyGroups = groups;
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                logEvent(
                    eventType, copyEventProperties, apiProperties,
                    copyUserProperties, copyGroups, timestamp, outOfSession
                );
            }
        });
    }

    protected long logEvent(String eventType, JSONObject eventProperties, JSONObject apiProperties,
            JSONObject userProperties, JSONObject groups, long timestamp, boolean outOfSession) {
        logger.d(TAG, "Logged event to Amplitude: " + eventType);

        if (optOut) {
            return -1;
        }

        // skip session check if logging start_session or end_session events
        boolean loggingSessionEvent = trackingSessionEvents &&
                (eventType.equals(START_SESSION_EVENT) || eventType.equals(END_SESSION_EVENT));

        if (!loggingSessionEvent && !outOfSession) {
            // default case + corner case when async logEvent between onPause and onResume
            if (!inForeground){
                startNewSessionIfNeeded(timestamp);
            } else {
                refreshSessionTime(timestamp);
            }
        }

        JSONObject event = new JSONObject();
        try {
            event.put("event_type", replaceWithJSONNull(eventType));
            event.put("timestamp", timestamp);
            event.put("user_id", replaceWithJSONNull(userId));
            event.put("device_id", replaceWithJSONNull(deviceId));
            event.put("session_id", outOfSession ? -1 : sessionId);
            event.put("version_name", replaceWithJSONNull(deviceInfo.getVersionName()));
            event.put("os_name", replaceWithJSONNull(deviceInfo.getOsName()));
            event.put("os_version", replaceWithJSONNull(deviceInfo.getOsVersion()));
            event.put("device_brand", replaceWithJSONNull(deviceInfo.getBrand()));
            event.put("device_manufacturer", replaceWithJSONNull(deviceInfo.getManufacturer()));
            event.put("device_model", replaceWithJSONNull(deviceInfo.getModel()));
            event.put("carrier", replaceWithJSONNull(deviceInfo.getCarrier()));
            event.put("country", replaceWithJSONNull(deviceInfo.getCountry()));
            event.put("language", replaceWithJSONNull(deviceInfo.getLanguage()));
            event.put("platform", Constants.PLATFORM);
            event.put("uuid", UUID.randomUUID().toString());
            event.put("sequence_number", getNextSequenceNumber());

            JSONObject library = new JSONObject();
            library.put("name", Constants.LIBRARY);
            library.put("version", Constants.VERSION);
            event.put("library", library);

            apiProperties = (apiProperties == null) ? new JSONObject() : apiProperties;
            Location location = deviceInfo.getMostRecentLocation();
            if (location != null) {
                JSONObject locationJSON = new JSONObject();
                locationJSON.put("lat", location.getLatitude());
                locationJSON.put("lng", location.getLongitude());
                apiProperties.put("location", locationJSON);
            }
            if (deviceInfo.getAdvertisingId() != null) {
                apiProperties.put("androidADID", deviceInfo.getAdvertisingId());
            }
            apiProperties.put("limit_ad_tracking", deviceInfo.isLimitAdTrackingEnabled());
            apiProperties.put("gps_enabled", deviceInfo.isGooglePlayServicesEnabled());

            event.put("api_properties", apiProperties);
            event.put("event_properties", (eventProperties == null) ? new JSONObject()
                    : truncate(eventProperties));
            event.put("user_properties", (userProperties == null) ? new JSONObject()
                    : truncate(userProperties));
            event.put("groups", (groups == null) ? new JSONObject() : truncate(groups));
        } catch (JSONException e) {
            logger.e(TAG, e.toString());
        }

        return saveEvent(eventType, event);
    }

    protected long saveEvent(String eventType, JSONObject event) {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        long eventId;
        if (eventType.equals(Constants.IDENTIFY_EVENT)) {
            eventId = dbHelper.addIdentify(event.toString());
            setLastIdentifyId(eventId);
        } else {
            eventId = dbHelper.addEvent(event.toString());
            setLastEventId(eventId);
        }

        int numEventsToRemove = Math.min(
                Math.max(1, eventMaxCount/10),
                Constants.EVENT_REMOVE_BATCH_SIZE
        );
        if (dbHelper.getEventCount() > eventMaxCount) {
            dbHelper.removeEvents(dbHelper.getNthEventId(numEventsToRemove));
        }
        if (dbHelper.getIdentifyCount() > eventMaxCount) {
            dbHelper.removeIdentifys(dbHelper.getNthIdentifyId(numEventsToRemove));
        }

        long totalEventCount = dbHelper.getTotalEventCount(); // counts may have changed, refetch
        if ((totalEventCount % eventUploadThreshold) == 0 &&
                totalEventCount >= eventUploadThreshold) {
            updateServer();
        } else {
            updateServerLater(eventUploadPeriodMillis);
        }

        return eventId;
    }

    // fetches key from dbHelper longValueStore
    // if key does not exist, return defaultValue instead
    private long getLongvalue(String key, long defaultValue) {
        Long value = DatabaseHelper.getDatabaseHelper(context).getLongValue(key);
        return value == null ? defaultValue : value;
    }

    private void setLongValue(String key, long value) {
        DatabaseHelper.getDatabaseHelper(context).insertOrReplaceKeyLongValue(key, value);
    }

    // shared sequence number for ordering events and identifys
    long getNextSequenceNumber() {
        long sequenceNumber = getLongvalue(SEQUENCE_NUMBER_KEY, 0);
        sequenceNumber++;
        setLongValue(SEQUENCE_NUMBER_KEY, sequenceNumber);
        return sequenceNumber;
    }

    long getLastEventTime() {
        return getLongvalue(LAST_EVENT_TIME_KEY, -1);
    }

    void setLastEventTime(long timestamp) {
        setLongValue(LAST_EVENT_TIME_KEY, timestamp);
    }

    long getLastEventId() {
        return getLongvalue(LAST_EVENT_ID_KEY, -1);
    }

    void setLastEventId(long eventId) {
        setLongValue(LAST_EVENT_ID_KEY, eventId);
    }

    long getLastIdentifyId() {
        return getLongvalue(LAST_IDENTIFY_ID_KEY, -1);
    }

    void setLastIdentifyId(long identifyId) {
        setLongValue(LAST_IDENTIFY_ID_KEY, identifyId);
    }

    long getPreviousSessionId() {
        return getLongvalue(PREVIOUS_SESSION_ID_KEY, -1);
    }

    void setPreviousSessionId(long timestamp) {
        setLongValue(PREVIOUS_SESSION_ID_KEY, timestamp);
    }

    boolean startNewSessionIfNeeded(long timestamp) {
        if (inSession()) {

            if (isWithinMinTimeBetweenSessions(timestamp)) {
                refreshSessionTime(timestamp);
                return false;
            }

            startNewSession(timestamp);
            return true;
        }

        // no current session - check for previous session
        if (isWithinMinTimeBetweenSessions(timestamp)) {
            long previousSessionId = getPreviousSessionId();
            if (previousSessionId == -1) {
                startNewSession(timestamp);
                return true;
            }

            // extend previous session
            setSessionId(previousSessionId);
            refreshSessionTime(timestamp);
            return false;
        }

        startNewSession(timestamp);
        return true;
    }

    private void startNewSession(long timestamp) {
        // end previous session
        if (trackingSessionEvents) {
            sendSessionEvent(END_SESSION_EVENT);
        }

        // start new session
        setSessionId(timestamp);
        refreshSessionTime(timestamp);
        if (trackingSessionEvents) {
            sendSessionEvent(START_SESSION_EVENT);
        }
    }

    private boolean inSession() {
        return sessionId >= 0;
    }

    private boolean isWithinMinTimeBetweenSessions(long timestamp) {
        long lastEventTime = getLastEventTime();
        long sessionLimit = usingForegroundTracking ?
                minTimeBetweenSessionsMillis : sessionTimeoutMillis;
        return (timestamp - lastEventTime) < sessionLimit;
    }

    private void setSessionId(long timestamp) {
        sessionId = timestamp;
        setPreviousSessionId(timestamp);
    }

    void refreshSessionTime(long timestamp) {
        if (!inSession()) {
            return;
        }

        setLastEventTime(timestamp);
    }

    private void sendSessionEvent(final String sessionEvent) {
        if (!contextAndApiKeySet(String.format("sendSessionEvent('%s')", sessionEvent))) {
            return;
        }

        if (!inSession()) {
            return;
        }

        JSONObject apiProperties = new JSONObject();
        try {
            apiProperties.put("special", sessionEvent);
        } catch (JSONException e) {
            return;
        }

        long timestamp = getLastEventTime();
        logEvent(sessionEvent, null, apiProperties, null, null, timestamp, false);
    }

    void onExitForeground(final long timestamp) {
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                refreshSessionTime(timestamp);
                inForeground = false;
            }
        });
    }

    void onEnterForeground(final long timestamp) {
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                startNewSessionIfNeeded(timestamp);
                inForeground = true;
            }
        });
    }

    public void logRevenue(double amount) {
        // Amount is in dollars
        // ex. $3.99 would be pass as logRevenue(3.99)
        logRevenue(null, 1, amount);
    }

    public void logRevenue(String productId, int quantity, double price) {
        logRevenue(productId, quantity, price, null, null);
    }

    public void logRevenue(String productId, int quantity, double price, String receipt,
            String receiptSignature) {
        if (!contextAndApiKeySet("logRevenue()")) {
            return;
        }

        // Log revenue in events
        JSONObject apiProperties = new JSONObject();
        try {
            apiProperties.put("special", REVENUE_EVENT);
            apiProperties.put("productId", productId);
            apiProperties.put("quantity", quantity);
            apiProperties.put("price", price);
            apiProperties.put("receipt", receipt);
            apiProperties.put("receiptSig", receiptSignature);
        } catch (JSONException e) {
        }

        logEventAsync(
                REVENUE_EVENT, null, apiProperties, null, null, getCurrentTimeMillis(), false
        );
    }

    // maintain for backwards compatibility
    public void setUserProperties(final JSONObject userProperties, final boolean replace) {
        setUserProperties(userProperties);
    }

    public void setUserProperties(final JSONObject userProperties) {
        if (userProperties == null || userProperties.length() == 0 ||
                !contextAndApiKeySet("setUserProperties")) {
            return;
        }

        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                // Create deep copy to try and prevent ConcurrentModificationException
                JSONObject copy;
                try {
                    copy = new JSONObject(userProperties.toString());
                } catch (JSONException e) {
                    logger.e(TAG, e.toString());
                    return; // could not create copy
                }

                Identify identify = new Identify();
                Iterator<?> keys = copy.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    try {
                        identify.setUserProperty(key, copy.get(key));
                    } catch (JSONException e) {
                        logger.e(TAG, e.toString());
                    }
                }
                identify(identify);
            }
        });
    }

    public void clearUserProperties() {
        Identify identify = new Identify().clearAll();
        identify(identify);
    }

    public void identify(Identify identify) {
        if (identify == null || identify.userPropertiesOperations.length() == 0
                || !contextAndApiKeySet("identify()")) {
            return;
        }
        logEventAsync(Constants.IDENTIFY_EVENT, null, null,
                identify.userPropertiesOperations, null, getCurrentTimeMillis(), false);
    }

    public void setGroup(String groupType, Object groupName) {
        if (TextUtils.isEmpty(groupType) || !contextAndApiKeySet("setGroup()")) {
            return;
        }
        JSONObject group = null;
        try {
            group = new JSONObject().put(groupType, groupName);
        }
        catch (JSONException e) {
            logger.e(TAG, e.toString());
        }
        Identify identify = new Identify().setUserProperty(groupType, groupName);
        logEventAsync(Constants.IDENTIFY_EVENT, null, null, identify.userPropertiesOperations,
                group, getCurrentTimeMillis(), false);
    }

    public JSONObject truncate(JSONObject object) {
        if (object == null) {
            return null;
        }

        Iterator<?> keys = object.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {
                Object value = object.get(key);
                if (value.getClass().equals(String.class)) {
                    object.put(key, truncate((String) value));
                } else if (value.getClass().equals(JSONObject.class)) {
                    object.put(key, truncate((JSONObject) value));
                } else if (value.getClass().equals(JSONArray.class)) {
                    object.put(key, truncate((JSONArray) value));
                }
            } catch (JSONException e) {
                logger.e(TAG, e.toString());
            }
        }

        return object;
    }

    public JSONArray truncate(JSONArray array) throws JSONException {
        if (array == null) {
            return null;
        }

        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value.getClass().equals(String.class)) {
                array.put(i, truncate((String) value));
            } else if (value.getClass().equals(JSONObject.class)) {
                array.put(i, truncate((JSONObject) value));
            } else if (value.getClass().equals(JSONArray.class)) {
                array.put(i, truncate((JSONArray) value));
            }
        }
        return array;
    }

    public String truncate(String value) {
        return value.length() <= Constants.MAX_STRING_LENGTH ? value :
                value.substring(0, Constants.MAX_STRING_LENGTH);
    }


    /**
     * @return The developer specified identifier for tracking within the analytics system.
     *         Can be null.
     */
    public String getUserId() {
        return userId;
    }

    public AmplitudeClient setUserId(String userId) {
        if (!contextAndApiKeySet("setUserId()")) {
            return instance;
        }

        this.userId = userId;
        DatabaseHelper.getDatabaseHelper(context).insertOrReplaceKeyValue(USER_ID_KEY, userId);
        return instance;
    }

    public AmplitudeClient setDeviceId(final String deviceId) {
        Set<String> invalidDeviceIds = getInvalidDeviceIds();
        if (!contextAndApiKeySet("setDeviceId()") || TextUtils.isEmpty(deviceId) ||
                invalidDeviceIds.contains(deviceId)) {
            return instance;
        }

        this.deviceId = deviceId;
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyValue(DEVICE_ID_KEY, deviceId);
        return instance;
    }

    public void uploadEvents() {
        if (!contextAndApiKeySet("uploadEvents()")) {
            return;
        }

        logThread.post(new Runnable() {
            @Override
            public void run() {
                updateServer();
            }
        });
    }

    private void updateServerLater(long delayMillis) {
        if (updateScheduled.getAndSet(true)) {
            return;
        }

        logThread.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateScheduled.set(false);
                updateServer();
            }
        }, delayMillis);
    }

    protected void updateServer() {
        updateServer(false);
    }

    // Always call this from logThread
    protected void updateServer(boolean limit) {
        if (optOut || offline) {
            return;
        }

        // if returning out of this block, always be sure to set uploadingCurrently to false!!
        if (!uploadingCurrently.getAndSet(true)) {
            DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
            long totalEventCount = dbHelper.getTotalEventCount();
            long batchSize = Math.min(
                limit ? backoffUploadBatchSize : eventUploadMaxBatchSize,
                totalEventCount
            );

            if (batchSize <= 0) {
                uploadingCurrently.set(false);
                return;
            }

            try {
                List<JSONObject> events = dbHelper.getEvents(getLastEventId(), batchSize);
                List<JSONObject> identifys = dbHelper.getIdentifys(getLastIdentifyId(), batchSize);

                final Pair<Pair<Long, Long>, JSONArray> merged = mergeEventsAndIdentifys(
                        events, identifys, batchSize);
                final JSONArray mergedEvents = merged.second;
                if (mergedEvents.length() == 0) {
                    uploadingCurrently.set(false);
                    return;
                }
                final long maxEventId = merged.first.first;
                final long maxIdentifyId = merged.first.second;
                final String mergedEventsString = merged.second.toString();

                httpThread.post(new Runnable() {
                    @Override
                    public void run() {
                        makeEventUploadPostRequest(httpClient, mergedEventsString, maxEventId, maxIdentifyId);
                    }
                });
            } catch (JSONException e) {
                uploadingCurrently.set(false);
                logger.e(TAG, e.toString());
            }
        }
    }

    protected Pair<Pair<Long,Long>, JSONArray> mergeEventsAndIdentifys(List<JSONObject> events,
                            List<JSONObject> identifys, long numEvents) throws JSONException {
        JSONArray merged = new JSONArray();
        long maxEventId = -1;
        long maxIdentifyId = -1;

        while (merged.length() < numEvents) {
            boolean noEvents = events.isEmpty();
            boolean noIdentifys = identifys.isEmpty();

            // case 0: no events or identifys, nothing to grab
            if (noEvents && noIdentifys) {
                break;

            // case 1: no identifys, grab from events
            } else if (noIdentifys) {
                JSONObject event = events.remove(0);
                maxEventId = event.getLong("event_id");
                merged.put(event);

            // case 2: no events, grab from identifys
            } else if (noEvents) {
                JSONObject identify = identifys.remove(0);
                maxIdentifyId = identify.getLong("event_id");
                merged.put(identify);

            // case 3: need to compare sequence numbers
            } else {
                // events logged before v2.1.0 won't have a sequence number, put those first
                if (!events.get(0).has("sequence_number") ||
                        events.get(0).getLong("sequence_number") <
                        identifys.get(0).getLong("sequence_number")) {
                    JSONObject event = events.remove(0);
                    maxEventId = event.getLong("event_id");
                    merged.put(event);
                } else {
                    JSONObject identify = identifys.remove(0);
                    maxIdentifyId = identify.getLong("event_id");
                    merged.put(identify);
                }
            }
        }

        return new Pair<Pair<Long, Long>, JSONArray>(new Pair<Long,Long>(maxEventId, maxIdentifyId), merged);
    }

    protected void makeEventUploadPostRequest(OkHttpClient client, String events, final long maxEventId, final long maxIdentifyId) {
        String apiVersionString = "" + Constants.API_VERSION;
        String timestampString = "" + getCurrentTimeMillis();

        String checksumString = "";
        try {
            String preimage = apiVersionString + apiKey + events + timestampString;

            // MessageDigest.getInstance(String) is not threadsafe on Android.
            // See https://code.google.com/p/android/issues/detail?id=37937
            // Use MD5 implementation from http://org.rodage.com/pub/java/security/MD5.java
            // This implementation does not throw NoSuchAlgorithm exceptions.
            MessageDigest messageDigest = new MD5();
            checksumString = bytesToHexString(messageDigest.digest(preimage.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            // According to
            // http://stackoverflow.com/questions/5049524/is-java-utf-8-charset-exception-possible,
            // this will never be thrown
            logger.e(TAG, e.toString());
        }

        FormBody body = new FormBody.Builder()
            .add("v", apiVersionString)
            .add("client", apiKey)
            .add("e", events)
            .add("upload_time", timestampString)
            .add("checksum", checksumString)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();

        boolean uploadSuccess = false;

        try {
            Response response = client.newCall(request).execute();
            String stringResponse = response.body().string();
            if (stringResponse.equals("success")) {
                uploadSuccess = true;
                logThread.post(new Runnable() {
                    @Override
                    public void run() {
                        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
                        if (maxEventId >= 0) dbHelper.removeEvents(maxEventId);
                        if (maxIdentifyId >= 0) dbHelper.removeIdentifys(maxIdentifyId);
                        uploadingCurrently.set(false);
                        if (dbHelper.getTotalEventCount() > eventUploadThreshold) {
                            logThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    updateServer(backoffUpload);
                                }
                            });
                        }
                        else {
                            backoffUpload = false;
                            backoffUploadBatchSize = eventUploadMaxBatchSize;
                        }
                    }
                });
            } else if (stringResponse.equals("invalid_api_key")) {
                logger.e(TAG, "Invalid API key, make sure your API key is correct in initialize()");
            } else if (stringResponse.equals("bad_checksum")) {
                logger.w(TAG,
                        "Bad checksum, post request was mangled in transit, will attempt to reupload later");
            } else if (stringResponse.equals("request_db_write_failed")) {
                logger.w(TAG,
                        "Couldn't write to request database on server, will attempt to reupload later");
            } else if (response.code() == 413) {

                // If blocked by one massive event, drop it
                DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
                if (backoffUpload && backoffUploadBatchSize == 1) {
                    if (maxEventId >= 0) dbHelper.removeEvent(maxEventId);
                    if (maxIdentifyId >= 0) dbHelper.removeIdentify(maxIdentifyId);
                    // maybe we want to reset backoffUploadBatchSize after dropping massive event
                }

                // Server complained about length of request, backoff and try again
                backoffUpload = true;
                int numEvents = Math.min((int)dbHelper.getEventCount(), backoffUploadBatchSize);
                backoffUploadBatchSize = (int)Math.ceil(numEvents / 2.0);
                logger.w(TAG, "Request too large, will decrease size and attempt to reupload");
                logThread.post(new Runnable() {
                   @Override
                    public void run() {
                       uploadingCurrently.set(false);
                       updateServer(true);
                   }
                });
            } else {
                logger.w(TAG, "Upload failed, " + stringResponse
                        + ", will attempt to reupload later");
            }
        } catch (java.net.ConnectException e) {
            // logger.w(TAG,
            // "No internet connection found, unable to upload events");
            lastError = e;
        } catch (java.net.UnknownHostException e) {
            // logger.w(TAG,
            // "No internet connection found, unable to upload events");
            lastError = e;
        } catch (IOException e) {
            logger.e(TAG, e.toString());
            lastError = e;
        } catch (AssertionError e) {
            // This can be caused by a NoSuchAlgorithmException thrown by DefaultHttpClient
            logger.e(TAG, "Exception:", e);
            lastError = e;
        } catch (Exception e) {
            // Just log any other exception so things don't crash on upload
            logger.e(TAG, "Exception:", e);
            lastError = e;
        }

        if (!uploadSuccess) {
            uploadingCurrently.set(false);
        }

    }

    /**
     * @return A unique identifier for tracking within the analytics system. Can be null if
     *         deviceId hasn't been initialized yet;
     */
    public String getDeviceId() {
        return deviceId;
    }

    // don't need to keep this in memory, if only using it at most 1 or 2 times
    private Set<String> getInvalidDeviceIds() {
        Set<String> invalidDeviceIds = new HashSet<String>();
        invalidDeviceIds.add("");
        invalidDeviceIds.add("9774d56d682e549c");
        invalidDeviceIds.add("unknown");
        invalidDeviceIds.add("000000000000000"); // Common Serial Number
        invalidDeviceIds.add("Android");
        invalidDeviceIds.add("DEFACE");

        return invalidDeviceIds;
    }

    private String initializeDeviceId() {
        Set<String> invalidIds = getInvalidDeviceIds();

        // see if device id already stored in db
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        String deviceId = dbHelper.getValue(DEVICE_ID_KEY);
        if (!(TextUtils.isEmpty(deviceId) || invalidIds.contains(deviceId))) {
            return deviceId;
        }

        if (!newDeviceIdPerInstall && useAdvertisingIdForDeviceId) {
            // Android ID is deprecated by Google.
            // We are required to use Advertising ID, and respect the advertising ID preference

            String advertisingId = deviceInfo.getAdvertisingId();
            if (!(TextUtils.isEmpty(advertisingId) || invalidIds.contains(advertisingId))) {
                dbHelper.insertOrReplaceKeyValue(DEVICE_ID_KEY, advertisingId);
                return advertisingId;
            }
        }

        // If this still fails, generate random identifier that does not persist
        // across installations. Append R to distinguish as randomly generated
        String randomId = deviceInfo.generateUUID() + "R";
        dbHelper.insertOrReplaceKeyValue(DEVICE_ID_KEY, randomId);
        return randomId;
    }

    private void runOnLogThread(Runnable r) {
        if (Thread.currentThread() != logThread) {
            logThread.post(r);
        } else {
            r.run();
        }
    }

    protected Object replaceWithJSONNull(Object obj) {
        return obj == null ? JSONObject.NULL : obj;
    }

    protected synchronized boolean contextAndApiKeySet(String methodName) {
        if (context == null) {
            logger.e(TAG, "context cannot be null, set context with initialize() before calling "
                    + methodName);
            return false;
        }
        if (TextUtils.isEmpty(apiKey)) {
            logger.e(TAG,
                    "apiKey cannot be null or empty, set apiKey with initialize() before calling "
                            + methodName);
            return false;
        }
        return true;
    }

    protected String getSharedPreferencesName() {
        return Constants.SHARED_PREFERENCES_NAME_PREFIX + "." + context.getPackageName();
    }

    protected String bytesToHexString(byte[] bytes) {
        final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
                'c', 'd', 'e', 'f' };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Do a shallow copy of a JSONObject. Takes a bit of code to avoid
     * stringify and reparse given the API.
     */
    private JSONObject cloneJSONObject(final JSONObject obj) {
        if (obj == null) {
            return null;
        }

        // obj.names returns null if the json obj is empty.
        JSONArray nameArray = null;
        try {
            nameArray = obj.names();
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.e(TAG, e.toString());
        }
        int len = (nameArray != null ? nameArray.length() : 0);

        String[] names = new String[len];
        for (int i = 0; i < len; i++) {
            names[i] = nameArray.optString(i);
        }

        try {
            return new JSONObject(obj, names);
        } catch (JSONException e) {
            logger.e(TAG, e.toString());
            return null;
        }
    }

    /**
     * Move all preference data from the legacy name to the new, static name if needed.
     *
     * Constants.PACKAGE_NAME used to be set using "Constants.class.getPackage().getName()"
     * Some aggressive proguard optimizations broke the reflection and caused apps
     * to crash on startup.
     *
     * Now that Constants.PACKAGE_NAME is changed, old data on devices needs to be
     * moved over to the new location so that device ids remain consistent.
     *
     * This should only happen once -- the first time a user loads the app after updating.
     * This logic needs to remain in place for quite a long time. It was first introduced in
     * April 2015 in version 1.6.0.
     */
    static boolean upgradePrefs(Context context) {
        return upgradePrefs(context, null, null);
    }

    static boolean upgradePrefs(Context context, String sourcePkgName, String targetPkgName) {
        try {
            if (sourcePkgName == null) {
                // Try to load the package name using the old reflection strategy.
                sourcePkgName = Constants.PACKAGE_NAME;
                try {
                    sourcePkgName = Constants.class.getPackage().getName();
                } catch (Exception e) { }
            }

            if (targetPkgName == null) {
                targetPkgName = Constants.PACKAGE_NAME;
            }

            // No need to copy if the source and target are the same.
            if (targetPkgName.equals(sourcePkgName)) {
                return false;
            }

            // Copy over any preferences that may exist in a source preference store.
            String sourcePrefsName = sourcePkgName + "." + context.getPackageName();
            SharedPreferences source =
                    context.getSharedPreferences(sourcePrefsName, Context.MODE_PRIVATE);

            // Nothing left in the source store to copy
            if (source.getAll().size() == 0) {
                return false;
            }

            String prefsName = targetPkgName + "." + context.getPackageName();
            SharedPreferences targetPrefs =
                    context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
            SharedPreferences.Editor target = targetPrefs.edit();

            // Copy over all existing data.
            if (source.contains(sourcePkgName + ".previousSessionId")) {
                target.putLong(Constants.PREFKEY_PREVIOUS_SESSION_ID,
                        source.getLong(sourcePkgName + ".previousSessionId", -1));
            }
            if (source.contains(sourcePkgName + ".deviceId")) {
                target.putString(Constants.PREFKEY_DEVICE_ID,
                        source.getString(sourcePkgName + ".deviceId", null));
            }
            if (source.contains(sourcePkgName + ".userId")) {
                target.putString(Constants.PREFKEY_USER_ID,
                        source.getString(sourcePkgName + ".userId", null));
            }
            if (source.contains(sourcePkgName + ".optOut")) {
                target.putBoolean(Constants.PREFKEY_OPT_OUT,
                        source.getBoolean(sourcePkgName + ".optOut", false));
            }

            // Commit the changes and clear the source store so we don't recopy.
            target.apply();
            source.edit().clear().apply();

            logger.i(TAG, "Upgraded shared preferences from " + sourcePrefsName + " to " + prefsName);
            return true;

        } catch (Exception e) {
            logger.e(TAG, "Error upgrading shared preferences", e);
            return false;
        }
    }

    /*
     * Move all data from sharedPrefs to sqlite key value store to support multi-process apps.
     * sharedPrefs is known to not be process-safe.
     */
    static boolean upgradeSharedPrefsToDB(Context context) {
        return upgradeSharedPrefsToDB(context, null);
    }

    static boolean upgradeSharedPrefsToDB(Context context, String sourcePkgName) {
        if (sourcePkgName == null) {
            sourcePkgName = Constants.PACKAGE_NAME;
        }

        // only perform upgrade if deviceId and previous_session_id not yet in database
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        String deviceId = dbHelper.getValue(DEVICE_ID_KEY);
        Long previousSessionId = dbHelper.getLongValue(PREVIOUS_SESSION_ID_KEY);
        Long lastEventTime = dbHelper.getLongValue(LAST_EVENT_TIME_KEY);
        if (!TextUtils.isEmpty(deviceId) && previousSessionId != null && lastEventTime != null) {
            return true;
        }

        String prefsName = sourcePkgName + "." + context.getPackageName();
        SharedPreferences preferences =
                context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);

        // migrate deviceId
        migrateStringValue(
            preferences, Constants.PREFKEY_DEVICE_ID, null, dbHelper, DEVICE_ID_KEY
        );

        // migrate lastEventTime
        migrateLongValue(
            preferences, Constants.PREFKEY_LAST_EVENT_TIME, -1, dbHelper, LAST_EVENT_TIME_KEY
        );

        // migrate lastEventId
        migrateLongValue(
            preferences, Constants.PREFKEY_LAST_EVENT_ID, -1, dbHelper, LAST_EVENT_ID_KEY
        );

        // migrate lastIdentifyId
        migrateLongValue(
            preferences, Constants.PREFKEY_LAST_IDENTIFY_ID, -1, dbHelper, LAST_IDENTIFY_ID_KEY
        );

        // migrate previousSessionId
        migrateLongValue(
            preferences, Constants.PREFKEY_PREVIOUS_SESSION_ID, -1,
            dbHelper, PREVIOUS_SESSION_ID_KEY
        );

        // migrate userId
        migrateStringValue(
            preferences, Constants.PREFKEY_USER_ID, null, dbHelper, USER_ID_KEY
        );

        // migrate optOut
        migrateBooleanValue(
            preferences, Constants.PREFKEY_OPT_OUT, false, dbHelper, OPT_OUT_KEY
        );

        return true;
    }

    private static void migrateLongValue(SharedPreferences prefs, String prefKey, long defValue, DatabaseHelper dbHelper, String dbKey) {
        Long value = dbHelper.getLongValue(dbKey);
        if (value != null) { // if value already exists don't need to migrate
            return;
        }
        long oldValue = prefs.getLong(prefKey, defValue);
        dbHelper.insertOrReplaceKeyLongValue(dbKey, oldValue);
        prefs.edit().remove(prefKey).apply();
    }

    private static void migrateStringValue(SharedPreferences prefs, String prefKey, String defValue, DatabaseHelper dbHelper, String dbKey) {
        String value = dbHelper.getValue(dbKey);
        if (!TextUtils.isEmpty(value)) {
            return;
        }
        String oldValue = prefs.getString(prefKey, defValue);
        if (!TextUtils.isEmpty(oldValue)) {
            dbHelper.insertOrReplaceKeyValue(dbKey, oldValue);
            prefs.edit().remove(prefKey).apply();
        }
    }

    private static void migrateBooleanValue(SharedPreferences prefs, String prefKey, boolean defValue, DatabaseHelper dbHelper, String dbKey) {
        Long value = dbHelper.getLongValue(dbKey);
        if (value != null) {
            return;
        }
        boolean oldValue = prefs.getBoolean(prefKey, defValue);
        dbHelper.insertOrReplaceKeyLongValue(dbKey, oldValue ? 1L : 0L);
        prefs.edit().remove(prefKey).apply();
    }

    protected long getCurrentTimeMillis() { return System.currentTimeMillis(); }
}
