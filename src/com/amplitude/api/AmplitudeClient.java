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

/**
 * <h1>AmplitudeClient</h1>
 * This is the SDK instance class that contains all of the SDK functionality.<br><br>
 * <b>Note:</b> call the methods on the default shared instance in the Amplitude class,
 * for example: {@code Amplitude.getInstance().logEvent();}<br><br>
 * Many of the SDK functions return the SDK instance back, allowing you to chain multiple method
 * calls together, for example: {@code Amplitude.getInstance().initialize(this, "APIKEY").enableForegroundTracking(getApplication())}
 */
public class AmplitudeClient {

    /**
     * The class identifier tag used in logging. TAG = {@code "com.amplitude.api.AmplitudeClient";}
     */
    public static final String TAG = "com.amplitude.api.AmplitudeClient";

    /**
     * The event type for start session events.
     */
    public static final String START_SESSION_EVENT = "session_start";
    /**
     * The event type for end session events.
     */
    public static final String END_SESSION_EVENT = "session_end";

    /**
     * The pref/database key for the device ID value.
     */
    public static final String DEVICE_ID_KEY = "device_id";
    /**
     * The pref/database key for the user ID value.
     */
    public static final String USER_ID_KEY = "user_id";
    /**
     * The pref/database key for the opt out flag.
     */
    public static final String OPT_OUT_KEY = "opt_out";
    /**
     * The pref/database key for the sequence number.
     */
    public static final String SEQUENCE_NUMBER_KEY = "sequence_number";
    /**
     * The pref/database key for the last event time.
     */
    public static final String LAST_EVENT_TIME_KEY = "last_event_time";
    /**
     * The pref/database key for the last event ID value.
     */
    public static final String LAST_EVENT_ID_KEY = "last_event_id";
    /**
     * The pref/database key for the last identify ID value.
     */
    public static final String LAST_IDENTIFY_ID_KEY = "last_identify_id";
    /**
     * The pref/database key for the previous session ID value.
     */
    public static final String PREVIOUS_SESSION_ID_KEY = "previous_session_id";


    /**
     * The default shared instance. This is fetched by {@code Amplitude.getInstance()}
     */
    protected static AmplitudeClient instance = new AmplitudeClient();

    /**
     * Gets the default AmplitudeClient instance.
     *
     * @return the default instance
     */
    public static AmplitudeClient getInstance() {
        return instance;
    }

    private static final AmplitudeLog logger = AmplitudeLog.getLogger();

    /**
     * The Android App Context.
     */
    protected Context context;
    /**
     * The shared OkHTTPClient instance.
     */
    protected OkHttpClient httpClient;
    /**
     * The shared Amplitude database helper instance.
     */
    protected DatabaseHelper dbHelper;
    /**
     * The Amplitude App API key.
     */
    protected String apiKey;
    /**
     * The user's ID value.
     */
    protected String userId;
    /**
     * The user's Device ID value.
     */
    protected String deviceId;
    private boolean newDeviceIdPerInstall = false;
    private boolean useAdvertisingIdForDeviceId = false;
    private boolean initialized = false;
    private boolean optOut = false;
    private boolean offline = false;

    private DeviceInfo deviceInfo;

    /**
     * The current session ID value.
     */
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
    /**
     * Whether or not the SDK is in the process of uploading events.
     */
    AtomicBoolean uploadingCurrently = new AtomicBoolean(false);

    /**
     * The last SDK error - used for testing.
     */
    Throwable lastError;
    /**
     * The url for Amplitude API endpoint
     */
    String url = Constants.EVENT_LOG_URL;
    /**
     * The background event logging worker thread instance.
     */
    WorkerThread logThread = new WorkerThread("logThread");
    /**
     * The background event uploading worker thread instance.
     */
    WorkerThread httpThread = new WorkerThread("httpThread");

    /**
     * Instantiates a new AmplitudeClient and starts worker threads.
     */
    public AmplitudeClient() {
        logThread.start();
        httpThread.start();
    }

    /**
     * Initialize the Amplitude SDK with the Android application context and your Amplitude
     * App API key. <b>Note:</b> initialization is required before you log events and modify
     * user properties.
     *
     * @param context the Android application context
     * @param apiKey  your Amplitude App API key
     * @return the AmplitudeClient
     */
    public AmplitudeClient initialize(Context context, String apiKey) {
        return initialize(context, apiKey, null);
    }

    /**
     * Initialize the Amplitude SDK with the Android application context, your Amplitude App API
     * key, and a user ID for the current user. <b>Note:</b> initialization is required before
     * you log events and modify user properties.
     *
     * @param context the Android application context
     * @param apiKey  your Amplitude App API key
     * @param userId  the user id to set
     * @return the AmplitudeClient
     */
    public synchronized AmplitudeClient initialize(final Context context, final String apiKey, final String userId) {
        if (context == null) {
            logger.e(TAG, "Argument context cannot be null in initialize()");
            return this;
        }

        if (TextUtils.isEmpty(apiKey)) {
            logger.e(TAG, "Argument apiKey cannot be null or blank in initialize()");
            return this;
        }

        final AmplitudeClient client = this;
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                if (!initialized) {
                    AmplitudeClient.upgradePrefs(context);
                    AmplitudeClient.upgradeSharedPrefsToDB(context);
                    client.context = context.getApplicationContext();
                    client.httpClient = new OkHttpClient();
                    client.dbHelper = DatabaseHelper.getDatabaseHelper(client.context);
                    client.apiKey = apiKey;
                    initializeDeviceInfo();

                    if (userId != null) {
                        client.userId = userId;
                        dbHelper.insertOrReplaceKeyValue(USER_ID_KEY, userId);
                    } else {
                        client.userId = dbHelper.getValue(USER_ID_KEY);
                    }
                    Long optOut = dbHelper.getLongValue(OPT_OUT_KEY);
                    client.optOut = optOut != null && optOut == 1;

                    // try to restore previous session id
                    long previousSessionId = getPreviousSessionId();
                    if (previousSessionId >= 0) {
                        sessionId = previousSessionId;
                    }

                    initialized = true;
                }
            }
        });



        return this;
    }

    /**
     * Enable foreground tracking for the SDK. This is <b>HIGHLY RECOMMENDED</b>, and will allow
     * for accurate session tracking.
     *
     * @param app the Android application
     * @return the AmplitudeClient
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-sessions">
     *     Tracking Sessions</a>
     */
    public AmplitudeClient enableForegroundTracking(Application app) {
        if (usingForegroundTracking || !contextAndApiKeySet("enableForegroundTracking()")) {
            return this;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            app.registerActivityLifecycleCallbacks(new AmplitudeCallbacks(this));
        }

        return this;
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

    /**
     * Whether to set a new device ID per install. If true, then the SDK will always generate a new
     * device ID on app install (as opposed to re-using an existing value like ADID).
     *
     * @param newDeviceIdPerInstall whether to set a new device ID on app install.
     * @return the AmplitudeClient
     * @deprecated
     */
    public AmplitudeClient enableNewDeviceIdPerInstall(boolean newDeviceIdPerInstall) {
        this.newDeviceIdPerInstall = newDeviceIdPerInstall;
        return this;
    }

    /**
     * Whether to use the Android advertising ID (ADID) as the user's device ID.
     *
     * @return the AmplitudeClient
     */
    public AmplitudeClient useAdvertisingIdForDeviceId() {
        this.useAdvertisingIdForDeviceId = true;
        return this;
    }

    /**
     * Enable location listening in the SDK. This will add the user's current lat/lon coordinates
     * to every event logged.
     *
     * @return the AmplitudeClient
     */
    public AmplitudeClient enableLocationListening() {
        if (deviceInfo == null) {
            throw new IllegalStateException(
                    "Must initialize before acting on location listening.");
        }
        deviceInfo.setLocationListening(true);
        return this;
    }

    /**
     * Disable location listening in the SDK. This will stop the sending of the user's current
     * lat/lon coordinates.
     *
     * @return the AmplitudeClient
     */
    public AmplitudeClient disableLocationListening() {
        if (deviceInfo == null) {
            throw new IllegalStateException(
                    "Must initialize before acting on location listening.");
        }
        deviceInfo.setLocationListening(false);
        return this;
    }

    /**
     * Sets event upload threshold. The SDK will attempt to batch upload unsent events
     * every eventUploadPeriodMillis milliseconds, or if the unsent event count exceeds the
     * event upload threshold.
     *
     * @param eventUploadThreshold the event upload threshold
     * @return the AmplitudeClient
     */
    public AmplitudeClient setEventUploadThreshold(int eventUploadThreshold) {
        this.eventUploadThreshold = eventUploadThreshold;
        return this;
    }

    /**
     * Sets event upload max batch size. This controls the maximum number of events sent with
     * each upload request.
     *
     * @param eventUploadMaxBatchSize the event upload max batch size
     * @return the AmplitudeClient
     */
    public AmplitudeClient setEventUploadMaxBatchSize(int eventUploadMaxBatchSize) {
        this.eventUploadMaxBatchSize = eventUploadMaxBatchSize;
        this.backoffUploadBatchSize = eventUploadMaxBatchSize;
        return this;
    }

    /**
     * Sets event max count. This is the maximum number of unsent events to keep on the device
     * (for example if the device does not have internet connectivity and cannot upload events).
     * If the number of unsent events exceeds the max count, then the SDK begins dropping events,
     * starting from the earliest logged.
     *
     * @param eventMaxCount the event max count
     * @return the AmplitudeClient
     */
    public AmplitudeClient setEventMaxCount(int eventMaxCount) {
        this.eventMaxCount = eventMaxCount;
        return this;
    }

    /**
     * Sets event upload period millis. The SDK will attempt to batch upload unsent events
     * every eventUploadPeriodMillis milliseconds, or if the unsent event count exceeds the
     * event upload threshold.
     *
     * @param eventUploadPeriodMillis the event upload period millis
     * @return the AmplitudeClient
     */
    public AmplitudeClient setEventUploadPeriodMillis(int eventUploadPeriodMillis) {
        this.eventUploadPeriodMillis = eventUploadPeriodMillis;
        return this;
    }

    /**
     * Sets min time between sessions millis.
     *
     * @param minTimeBetweenSessionsMillis the min time between sessions millis
     * @return the min time between sessions millis
     */
    public AmplitudeClient setMinTimeBetweenSessionsMillis(long minTimeBetweenSessionsMillis) {
        this.minTimeBetweenSessionsMillis = minTimeBetweenSessionsMillis;
        return this;
    }

    /**
     * Sets session timeout millis. If foreground tracking has not been enabled with
     * @{code enableForegroundTracking()}, then new sessions will be started after
     * sessionTimeoutMillis milliseconds have passed since the last event logged.
     *
     * @param sessionTimeoutMillis the session timeout millis
     * @return the AmplitudeClient
     */
    public AmplitudeClient setSessionTimeoutMillis(long sessionTimeoutMillis) {
        this.sessionTimeoutMillis = sessionTimeoutMillis;
        return this;
    }

    /**
     * Sets opt out. If true then the SDK does not track any events for the user.
     *
     * @param optOut whether or not to opt the user out of tracking
     * @return the AmplitudeClient
     */
    public AmplitudeClient setOptOut(boolean optOut) {
        if (!contextAndApiKeySet("setOptOut()")) {
            return this;
        }

        this.optOut = optOut;
        dbHelper.insertOrReplaceKeyLongValue(OPT_OUT_KEY, optOut ? 1L : 0L);
        return this;
    }

    /**
     * Returns whether or not the user is opted out of tracking.
     *
     * @return the optOut flag value
     */
    public boolean isOptedOut() {
        return optOut;
    }

    /**
     * Enable/disable message logging by the SDK.
     *
     * @param enableLogging whether to enable message logging by the SDK.
     * @return the AmplitudeClient
     */
    public AmplitudeClient enableLogging(boolean enableLogging) {
        logger.setEnableLogging(enableLogging);
        return this;
    }

    /**
     * Sets the logging level. Logging messages will only appear if they are the same severity
     * level or higher than the set log level.
     *
     * @param logLevel the log level
     * @return the AmplitudeClient
     */
    public AmplitudeClient setLogLevel(int logLevel) {
        logger.setLogLevel(logLevel);
        return this;
    }

    /**
     * Sets offline. If offline is true, then the SDK will not upload events to Amplitude servers;
     * however, it will still log events.
     *
     * @param offline whether or not the SDK should be offline
     * @return the AmplitudeClient
     */
    public AmplitudeClient setOffline(boolean offline) {
        this.offline = offline;

        // Try to update to the server once offline mode is disabled.
        if (!offline) {
            uploadEvents();
        }

        return this;
    }

    /**
     * Track session events amplitude client. If enabled then the SDK will automatically send
     * start and end session events to mark the start and end of the user's sessions.
     *
     * @param trackingSessionEvents whether to enable tracking of session events
     * @return the AmplitudeClient
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-sessions">
     *     Tracking Sessions</a>
     */
    public AmplitudeClient trackSessionEvents(boolean trackingSessionEvents) {
        this.trackingSessionEvents = trackingSessionEvents;
        return this;
    }

    /**
     * Set foreground tracking to true.
     */
    void useForegroundTracking() {
        usingForegroundTracking = true;
    }

    /**
     * Whether foreground tracking is enabled.
     *
     * @return whether foreground tracking is enabled
     */
    boolean isUsingForegroundTracking() { return usingForegroundTracking; }

    /**
     * Whether app is in the foreground.
     *
     * @return whether app is in the foreground
     */
    boolean isInForeground() { return inForeground; }

    /**
     * Log an event with the specified event type.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType the event type
     */
    public void logEvent(String eventType) {
        logEvent(eventType, null);
    }

    /**
     * Log an event with the specified event type and event properties.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Setting Event Properties</a>
     */
    public void logEvent(String eventType, JSONObject eventProperties) {
        logEvent(eventType, eventProperties, false);
    }

    /**
     * Log an event with the specified event type, event properties, with optional out of session
     * flag. If out of session is true, then the sessionId will be -1 for the event, indicating
     * that it is not part of the current session. Note: this might be useful when logging events
     * for notifications received.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param outOfSession    the out of session
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Setting Event Properties</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-sessions">
     *     Tracking Sessions</a>
     */
    public void logEvent(String eventType, JSONObject eventProperties, boolean outOfSession) {
        logEvent(eventType, eventProperties, null, outOfSession);
    }

    /**
     * Log an event with the specified event type, event properties, and groups. Use this to set
     * event-level groups, meaning the group(s) set only apply for this specific event and does
     * not persist on the user.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Setting Event Properties</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-groups">
     *     Setting Groups</a>
     */
    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups) {
        logEvent(eventType, eventProperties, groups, false);
    }

    /**
     * Log event with the specified event type, event properties, groups, with optional out of
     * session flag. If out of session is true, then the sessionId will be -1 for the event,
     * indicating that it is not part of the current session. Note: this might be useful when
     * logging events for notifications received.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @param outOfSession    the out of session
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Setting Event Properties</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-groups">
     *     Setting Groups</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-sessions">
     *     Tracking Sessions</a>
     */
    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups, boolean outOfSession) {
        if (validateLogEvent(eventType)) {
            logEventAsync(
                eventType, eventProperties, null, null, groups, getCurrentTimeMillis(), outOfSession
            );
        }
    }

    /**
     * Log an event with the specified event type.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType the event type
     */
    public void logEventSync(String eventType) {
        logEventSync(eventType, null);
    }

    /**
     * Log an event with the specified event type and event properties.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Setting Event Properties</a>
     */
    public void logEventSync(String eventType, JSONObject eventProperties) {
        logEventSync(eventType, eventProperties, false);
    }

    /**
     * Log an event with the specified event type, event properties, with optional out of session
     * flag. If out of session is true, then the sessionId will be -1 for the event, indicating
     * that it is not part of the current session. Note: this might be useful when logging events
     * for notifications received.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param outOfSession    the out of session
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Setting Event Properties</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-sessions">
     *     Tracking Sessions</a>
     */
    public void logEventSync(String eventType, JSONObject eventProperties, boolean outOfSession) {
        logEventSync(eventType, eventProperties, null, outOfSession);
    }

    /**
     * Log an event with the specified event type, event properties, and groups. Use this to set
     * event-level groups, meaning the group(s) set only apply for this specific event and does
     * not persist on the user.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Setting Event Properties</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-groups">
     *     Setting Groups</a>
     */
    public void logEventSync(String eventType, JSONObject eventProperties, JSONObject group) {
        logEventSync(eventType, eventProperties, group, false);
    }

    /**
     * Log event with the specified event type, event properties, groups, with optional out of
     * session flag. If out of session is true, then the sessionId will be -1 for the event,
     * indicating that it is not part of the current session. Note: this might be useful when
     * logging events for notifications received.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @param outOfSession    the out of session
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Setting Event Properties</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-groups">
     *     Setting Groups</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-sessions">
     *     Tracking Sessions</a>
     */
    public void logEventSync(String eventType, JSONObject eventProperties, JSONObject groups, boolean outOfSession) {
        if (validateLogEvent(eventType)) {
            logEvent(
                eventType, eventProperties, null, null, groups, getCurrentTimeMillis(), outOfSession
            );
        }
    }

    /**
     * Validate the event type being logged. Also verifies that the context and API key
     * have been set already with an initialize call.
     *
     * @param eventType the event type
     * @return true if the event type is valid
     */
    protected boolean validateLogEvent(String eventType) {
        if (TextUtils.isEmpty(eventType)) {
            logger.e(TAG, "Argument eventType cannot be null or blank in logEvent()");
            return false;
        }

        return contextAndApiKeySet("logEvent()");
    }

    /**
     * Log event async. Internal method to handle the synchronous logging of events.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param apiProperties   the api properties
     * @param userProperties  the user properties
     * @param groups          the groups
     * @param timestamp       the timestamp
     * @param outOfSession    the out of session
     */
    protected void logEventAsync(final String eventType, JSONObject eventProperties,
            final JSONObject apiProperties, JSONObject userProperties,
            JSONObject groups, final long timestamp, final boolean outOfSession) {
        // Clone the incoming eventProperties object before sending over
        // to the log thread. Helps avoid ConcurrentModificationException
        // if the caller starts mutating the object they passed in.
        // Only does a shallow copy, so it's still possible, though unlikely,
        // to hit concurrent access if the caller mutates deep in the object.
        if (eventProperties != null) {
            eventProperties = Utils.cloneJSONObject(eventProperties);
        }

        if (userProperties != null) {
            userProperties = Utils.cloneJSONObject(userProperties);
        }

        if (groups != null) {
            groups = Utils.cloneJSONObject(groups);
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

    /**
     * Log event. Internal method to handle the asynchronous logging of events on background
     * thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param apiProperties   the api properties
     * @param userProperties  the user properties
     * @param groups          the groups
     * @param timestamp       the timestamp
     * @param outOfSession    the out of session
     * @return the event ID if succeeded, else -1.
     */
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

    /**
     * Save event long. Internal method to save an event to the database.
     *
     * @param eventType the event type
     * @param event     the event
     * @return the event ID if succeeded, else -1
     */
    protected long saveEvent(String eventType, JSONObject event) {
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
        Long value = dbHelper.getLongValue(key);
        return value == null ? defaultValue : value;
    }

    /**
     * Internal method to increment and fetch the next event sequence number.
     *
     * @return the next sequence number
     */
    long getNextSequenceNumber() {
        long sequenceNumber = getLongvalue(SEQUENCE_NUMBER_KEY, 0);
        sequenceNumber++;
        dbHelper.insertOrReplaceKeyLongValue(SEQUENCE_NUMBER_KEY, sequenceNumber);
        return sequenceNumber;
    }

    /**
     * Internal method to get the last event time.
     *
     * @return the last event time
     */
    long getLastEventTime() {
        return getLongvalue(LAST_EVENT_TIME_KEY, -1);
    }

    /**
     * Internal method to set the last event time.
     *
     * @param timestamp the timestamp
     */
    void setLastEventTime(long timestamp) {
        dbHelper.insertOrReplaceKeyLongValue(LAST_EVENT_TIME_KEY, timestamp);
    }

    /**
     * Internal method to get the last event id.
     *
     * @return the last event id
     */
    long getLastEventId() {
        return getLongvalue(LAST_EVENT_ID_KEY, -1);
    }

    /**
     * Internal method to set the last event id.
     *
     * @param eventId the event id
     */
    void setLastEventId(long eventId) {
        dbHelper.insertOrReplaceKeyLongValue(LAST_EVENT_ID_KEY, eventId);
    }

    /**
     * Internal method to get the last identify id.
     *
     * @return the last identify id
     */
    long getLastIdentifyId() {
        return getLongvalue(LAST_IDENTIFY_ID_KEY, -1);
    }

    /**
     * Internal method to set the last identify id.
     *
     * @param identifyId the identify id
     */
    void setLastIdentifyId(long identifyId) {
        dbHelper.insertOrReplaceKeyLongValue(LAST_IDENTIFY_ID_KEY, identifyId);
    }

    /**
     * Gets the current session id.
     *
     * @return The current sessionId value.
     */
    public long getSessionId() {
        return sessionId;
    }

    /**
     * Internal method to get the previous session id.
     *
     * @return the previous session id
     */
    long getPreviousSessionId() {
        return getLongvalue(PREVIOUS_SESSION_ID_KEY, -1);
    }

    /**
     * Internal method to set the previous session id.
     *
     * @param timestamp the timestamp
     */
    void setPreviousSessionId(long timestamp) {
        dbHelper.insertOrReplaceKeyLongValue(PREVIOUS_SESSION_ID_KEY, timestamp);
    }

    /**
     * Internal method to start a new session if needed.
     *
     * @param timestamp the timestamp
     * @return whether or not a new session was started
     */
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

    /**
     * Internal method to refresh the current session time.
     *
     * @param timestamp the timestamp
     */
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

    /**
     * Internal method to handle on app exit foreground behavior.
     *
     * @param timestamp the timestamp
     */
    void onExitForeground(final long timestamp) {
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                refreshSessionTime(timestamp);
                inForeground = false;
            }
        });
    }

    /**
     * Internal method to handle on app enter foreground behavior.
     *
     * @param timestamp the timestamp
     */
    void onEnterForeground(final long timestamp) {
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                startNewSessionIfNeeded(timestamp);
                inForeground = true;
            }
        });
    }

    /**
     * Log revenue amount via a revenue event.
     *
     * @param amount the amount
     * @deprecated - use {@code logRevenueV2} instead
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-revenue">
     *     Tracking Revenue</a>
     */
    public void logRevenue(double amount) {
        // Amount is in dollars
        // ex. $3.99 would be pass as logRevenue(3.99)
        logRevenue(null, 1, amount);
    }

    /**
     * Log revenue with a productId, quantity, and price.
     *
     * @param productId the product id
     * @param quantity  the quantity
     * @param price     the price
     * @deprecated - use {@code logRevenueV2} instead
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-revenue">
     *     Tracking Revenue</a>
     */
    public void logRevenue(String productId, int quantity, double price) {
        logRevenue(productId, quantity, price, null, null);
    }

    /**
     * Log revenue with a productId, quantity, price, and receipt data for revenue verification.
     *
     * @param productId        the product id
     * @param quantity         the quantity
     * @param price            the price
     * @param receipt          the receipt
     * @param receiptSignature the receipt signature
     * @deprecated - use {@code logRevenueV2} instead
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-revenue">
     *     Tracking Revenue</a>
     */
    public void logRevenue(String productId, int quantity, double price, String receipt,
            String receiptSignature) {
        if (!contextAndApiKeySet("logRevenue()")) {
            return;
        }

        // Log revenue in events
        JSONObject apiProperties = new JSONObject();
        try {
            apiProperties.put("special", Constants.AMP_REVENUE_EVENT);
            apiProperties.put("productId", productId);
            apiProperties.put("quantity", quantity);
            apiProperties.put("price", price);
            apiProperties.put("receipt", receipt);
            apiProperties.put("receiptSig", receiptSignature);
        } catch (JSONException e) {
        }

        logEventAsync(
            Constants.AMP_REVENUE_EVENT, null, apiProperties, null, null, getCurrentTimeMillis(), false
        );
    }

    /**
     * Log revenue v2. Create a {@link com.amplitude.api.Revenue} object to hold your revenue data and
     * properties, and log it as a revenue event using {@code logRevenueV2}.
     *
     * @param revenue a {@link com.amplitude.api.Revenue} object
     * @see com.amplitude.api.Revenue
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-revenue">
     *     Tracking Revenue</a>
     */
    public void logRevenueV2(Revenue revenue) {
        if (!contextAndApiKeySet("logRevenueV2()") || revenue == null || !revenue.isValidRevenue()) {
            return;
        }

        logEvent(Constants.AMP_REVENUE_EVENT, revenue.toJSONObject());
    }

    /**
     * Sets user properties. This is a convenience wrapper around the
     * {@link com.amplitude.api.Identify} API to set multiple user properties with a single
     * command. <b>Note:</b> the replace parameter is deprecated and has no effect.
     *
     * @param userProperties the user properties
     * @param replace        the replace - has no effect
     * @deprecated
     * @see <a href="https://github.com/amplitude/Amplitude-Android#user-properties-and-user-property-operations">
     *     User Properties</a>
     */
    public void setUserProperties(final JSONObject userProperties, final boolean replace) {
        setUserProperties(userProperties);
    }

    /**
     * Sets user properties. This is a convenience wrapper around the
     * {@link com.amplitude.api.Identify} API to set multiple user properties with a single
     * command.
     *
     * @param userProperties the user properties
     * @see <a href="https://github.com/amplitude/Amplitude-Android#user-properties-and-user-property-operations">
     *     User Properties</a>
     */
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

    /**
     * Clear user properties. This will clear all user properties at once. <b>Note: the
     * result is irreversible!</b>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#user-properties-and-user-property-operations">
     *     User Properties</a>
     */
    public void clearUserProperties() {
        Identify identify = new Identify().clearAll();
        identify(identify);
    }

    /**
     * Identify. Use this to send an {@link com.amplitude.api.Identify} object containing
     * user property operations to Amplitude server.
     *
     * @param identify an {@link com.amplitude.api.Identify} object
     * @see com.amplitude.api.Identify
     * @see <a href="https://github.com/amplitude/Amplitude-Android#user-properties-and-user-property-operations">
     *     User Properties</a>
     */
    public void identify(Identify identify) {
        if (identify == null || identify.userPropertiesOperations.length() == 0
                || !contextAndApiKeySet("identify()")) {
            return;
        }
        logEventAsync(Constants.IDENTIFY_EVENT, null, null,
                identify.userPropertiesOperations, null, getCurrentTimeMillis(), false);
    }

    /**
     * Sets the user's group(s).
     *
     * @param groupType the group type (ex: orgId)
     * @param groupName the group name (ex: 15)
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-groups">
     *     Setting Groups</a>
     */
    public void setGroup(String groupType, Object groupName) {
        if (!contextAndApiKeySet("setGroup()") || TextUtils.isEmpty(groupType)) {
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

    /**
     * Truncate values in a JSON object. Any string values longer than 1024 characters will be
     * truncated to 1024 characters.
     *
     * @param object the object
     * @return the truncated JSON object
     */
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

    /**
     * Truncate values in a JSON array. Any string values longer than 1024 characters will be
     * truncated to 1024 characters.
     *
     * @param array the array
     * @return the truncated JSON array
     * @throws JSONException the json exception
     */
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

    /**
     * Truncate a string to 1024 characters.
     *
     * @param value the value
     * @return the truncated string
     */
    public String truncate(String value) {
        return value.length() <= Constants.MAX_STRING_LENGTH ? value :
                value.substring(0, Constants.MAX_STRING_LENGTH);
    }


    /**
     * Gets the user's id. Can be null.
     *
     * @return The developer specified identifier for tracking within the analytics system.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user id (can be null).
     *
     * @param userId the user id
     * @return the AmplitudeClient
     */
    public AmplitudeClient setUserId(String userId) {
        if (!contextAndApiKeySet("setUserId()")) {
            return this;
        }

        this.userId = userId;
        dbHelper.insertOrReplaceKeyValue(USER_ID_KEY, userId);
        return this;
    }

    /**
     * Sets a custom device id. <b>Note: only do this if you know what you are doing!</b>
     *
     * @param deviceId the device id
     * @return the AmplitudeClient
     * @see <a href="https://github.com/amplitude/Amplitude-Android#custom-device-ids">
     *     Custom Device Ids</a>
     */
    public AmplitudeClient setDeviceId(final String deviceId) {
        Set<String> invalidDeviceIds = getInvalidDeviceIds();
        if (!contextAndApiKeySet("setDeviceId()") || TextUtils.isEmpty(deviceId) ||
                invalidDeviceIds.contains(deviceId)) {
            return this;
        }

        this.deviceId = deviceId;
        dbHelper.insertOrReplaceKeyValue(DEVICE_ID_KEY, deviceId);
        return this;
    }

    /**
     * Force SDK to upload any unsent events.
     */
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

    /**
     * Internal method to upload unsent events.
     */
    protected void updateServer() {
        updateServer(false);
    }

    /**
     * Internal method to upload unsent events. Limit controls whether to use event upload max
     * batch size or backoff upload batch size. <b>Note: </b> always call this on logThread
     *
     * @param limit the limit
     */
    protected void updateServer(boolean limit) {
        if (optOut || offline) {
            return;
        }

        // if returning out of this block, always be sure to set uploadingCurrently to false!!
        if (!uploadingCurrently.getAndSet(true)) {
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

    /**
     * Internal method to merge unsent events and identifies into a single array by sequence number.
     *
     * @param events    the events
     * @param identifys the identifys
     * @param numEvents the num events
     * @return the merged array, max event id, and max identify id
     * @throws JSONException the json exception
     */
    protected Pair<Pair<Long,Long>, JSONArray> mergeEventsAndIdentifys(List<JSONObject> events,
                            List<JSONObject> identifys, long numEvents) throws JSONException {
        JSONArray merged = new JSONArray();
        long maxEventId = -1;
        long maxIdentifyId = -1;

        while (merged.length() < numEvents) {
            boolean noEvents = events.isEmpty();
            boolean noIdentifys = identifys.isEmpty();

            // case 0: no events or identifys, nothing to grab
            // this case should never happen, as it means there are less identifys and events
            // than expected
            if (noEvents && noIdentifys) {
                logger.w(TAG, String.format(
                    "mergeEventsAndIdentifys: number of events and identifys " +
                    "less than expected by %d", numEvents - merged.length())
                );
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

    /**
     * Internal method to generate the event upload post request.
     *
     * @param client        the client
     * @param events        the events
     * @param maxEventId    the max event id
     * @param maxIdentifyId the max identify id
     */
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
     * Get the current device id. Can be null if deviceId hasn't been initialized yet.
     *
     * @return A unique identifier for tracking within the analytics system.
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

    protected void runOnLogThread(Runnable r) {
        if (Thread.currentThread() != logThread) {
            logThread.post(r);
        } else {
            r.run();
        }
    }

    /**
     * Internal method to replace null event fields with JSON null object.
     *
     * @param obj the obj
     * @return the object
     */
    protected Object replaceWithJSONNull(Object obj) {
        return obj == null ? JSONObject.NULL : obj;
    }

    /**
     * Internal method to check whether application context and api key are set
     *
     * @param methodName the parent method name to print in error message
     * @return whether application context and api key are set
     */
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

    /**
     * Internal method to convert bytes to hex string
     *
     * @param bytes the bytes
     * @return the string
     */
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
     * Move all preference data from the legacy name to the new, static name if needed.
     * <p/>
     * Constants.PACKAGE_NAME used to be set using "Constants.class.getPackage().getName()"
     * Some aggressive proguard optimizations broke the reflection and caused apps
     * to crash on startup.
     * <p/>
     * Now that Constants.PACKAGE_NAME is changed, old data on devices needs to be
     * moved over to the new location so that device ids remain consistent.
     * <p/>
     * This should only happen once -- the first time a user loads the app after updating.
     * This logic needs to remain in place for quite a long time. It was first introduced in
     * April 2015 in version 1.6.0.
     *
     * @param context the context
     * @return the boolean
     */
    static boolean upgradePrefs(Context context) {
        return upgradePrefs(context, null, null);
    }

    /**
     * Upgrade prefs boolean.
     *
     * @param context       the context
     * @param sourcePkgName the source pkg name
     * @param targetPkgName the target pkg name
     * @return the boolean
     */
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

    /**
     * Upgrade shared prefs to db boolean.
     *
     * @param context the context
     * @return the boolean
     */
/*
     * Move all data from sharedPrefs to sqlite key value store to support multi-process apps.
     * sharedPrefs is known to not be process-safe.
     */
    static boolean upgradeSharedPrefsToDB(Context context) {
        return upgradeSharedPrefsToDB(context, null);
    }

    /**
     * Upgrade shared prefs to db boolean.
     *
     * @param context       the context
     * @param sourcePkgName the source pkg name
     * @return the boolean
     */
    static boolean upgradeSharedPrefsToDB(Context context, String sourcePkgName) {
        if (sourcePkgName == null) {
            sourcePkgName = Constants.PACKAGE_NAME;
        }

        // check if upgrade needed
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

        migrateStringValue(
            preferences, Constants.PREFKEY_DEVICE_ID, null, dbHelper, DEVICE_ID_KEY
        );

        migrateLongValue(
            preferences, Constants.PREFKEY_LAST_EVENT_TIME, -1, dbHelper, LAST_EVENT_TIME_KEY
        );

        migrateLongValue(
            preferences, Constants.PREFKEY_LAST_EVENT_ID, -1, dbHelper, LAST_EVENT_ID_KEY
        );

        migrateLongValue(
            preferences, Constants.PREFKEY_LAST_IDENTIFY_ID, -1, dbHelper, LAST_IDENTIFY_ID_KEY
        );

        migrateLongValue(
            preferences, Constants.PREFKEY_PREVIOUS_SESSION_ID, -1,
            dbHelper, PREVIOUS_SESSION_ID_KEY
        );

        migrateStringValue(
            preferences, Constants.PREFKEY_USER_ID, null, dbHelper, USER_ID_KEY
        );

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

    /**
     * Internal method to fetch the current time millis. Used for testing.
     *
     * @return the current time millis
     */
    protected long getCurrentTimeMillis() { return System.currentTimeMillis(); }
}
