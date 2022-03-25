package com.amplitude.api;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.util.Pair;


import com.amplitude.analytics.connector.AnalyticsConnector;
import com.amplitude.analytics.connector.Identity;
import com.amplitude.analytics.connector.util.JSONUtil;
import com.amplitude.eventexplorer.EventExplorer;
import com.amplitude.security.MD5;
import com.amplitude.util.DoubleCheck;
import com.amplitude.util.Provider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import okhttp3.Call;
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
    private static final String TAG = AmplitudeClient.class.getName();

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

    private static final AmplitudeLog logger = AmplitudeLog.getLogger();

    /**
     * The Android App Context.
     */
    protected Context context;
    /**
     * The shared OkHTTPClient instance.
     */
    protected Call.Factory callFactory;
    /**
     * The shared Amplitude database helper instance.
     */
    protected DatabaseHelper dbHelper;
    /**
     * The Amplitude App API key.
     */
    protected String apiKey;
    /**
     * The name for this instance of AmplitudeClient.
     */
    protected String instanceName;
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
    private boolean useAppSetIdForDeviceId = false;
    protected boolean initialized = false;
    private AmplitudeDeviceIdCallback deviceIdCallback;
    private boolean optOut = false;
    private boolean offline = false;
    TrackingOptions inputTrackingOptions = new TrackingOptions();
    TrackingOptions appliedTrackingOptions = TrackingOptions.copyOf(inputTrackingOptions);
    JSONObject apiPropertiesTrackingOptions = appliedTrackingOptions.getApiPropertiesTrackingOptions();
    private boolean coppaControlEnabled = false;
    private boolean locationListening = true;
    private EventExplorer eventExplorer;
    private Plan plan;
    /**
     * Amplitude Server Zone
     */
    private AmplitudeServerZone serverZone = AmplitudeServerZone.US;

    /**
     * The device's Platform value.
     */
    protected String platform;

    /**
     * Event metadata
     */
    long sessionId = -1;
    long sequenceNumber = 0;
    long lastEventId = -1;
    long lastIdentifyId = -1;
    long lastEventTime = -1;
    long previousSessionId = -1;

    protected DeviceInfo deviceInfo;

    /**
     * The current session ID value.
     */
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
    private boolean flushEventsOnClose = true;
    private String libraryName = Constants.LIBRARY;
    private String libraryVersion = Constants.VERSION;
    private boolean useDynamicConfig = false;

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
     * The Bearer Token for authentication
     */
    String bearerToken = null;
    /**
     * The background event logging worker thread instance.
     */
    WorkerThread logThread = new WorkerThread("logThread");
    /**
     * The background event uploading worker thread instance.
     */
    WorkerThread httpThread = new WorkerThread("httpThread");
    /**
     * The core package for integrating with the Experiment SDK.
     */
    final AnalyticsConnector connector;
    /**
     * The runner for middleware
     */
    MiddlewareRunner middlewareRunner = new MiddlewareRunner();

    /**
     * Instantiates a new default instance AmplitudeClient and starts worker threads.
     */
    public AmplitudeClient() {
        this(null);
    }

    /**
     * Instantiates a new AmplitudeClient with instance name and starts worker threads.
     * @param instance
     */
    public AmplitudeClient(String instance) {
        this.instanceName = Utils.normalizeInstanceName(instance);
        logThread.start();
        httpThread.start();
        this.connector = AnalyticsConnector.getInstance(this.instanceName);
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
    public AmplitudeClient initialize(Context context, String apiKey, String userId) {
        return initialize(context, apiKey, userId, null, false);
    }

    /**
     * Initialize the Amplitude SDK with the Android application context, your Amplitude App API
     * key, a user ID for the current user, and a custom platform value.
     * <b>Note:</b> initialization is required before you log events and modify user properties.
     *
     * @param context the Android application context
     * @param apiKey  your Amplitude App API key
     * @param userId  the user id to set
     * @param
     * @return the AmplitudeClient
     */
    public synchronized AmplitudeClient initialize(
            final Context context,
            final String apiKey,
            final String userId,
            final String platform,
            final boolean enableDiagnosticLogging
    ) {
        return this.initializeInternal(
                context,
                apiKey,
                userId,
                platform,
                enableDiagnosticLogging,
                null);
    }

    /**
     * Initialize the Amplitude SDK with the Android application context, your Amplitude App API
     * key, a user ID for the current user, and a custom platform value.
     * <b>Note:</b> initialization is required before you log events and modify user properties.
     *
     * @param context the Android application context
     * @param apiKey  your Amplitude App API key
     * @param userId  the user id to set
     * @param callFactory the call factory that used by Amplitude to make http request
     * @return the AmplitudeClient
     */
    public synchronized AmplitudeClient initialize(
            final Context context,
            final String apiKey,
            final String userId,
            final String platform,
            final boolean enableDiagnosticLogging,
            final Call.Factory callFactory
    ) {
        return this.initializeInternal(
                context,
                apiKey,
                userId,
                platform,
                enableDiagnosticLogging,
                callFactory);
    }

    /**
     * Initialize the Amplitude SDK with the Android application context, your Amplitude App API
     * key, a user ID for the current user, and a custom platform value.
     * <b>Note:</b> initialization is required before you log events and modify user properties.
     *
     * @param context the Android application context
     * @param apiKey  your Amplitude App API key
     * @param userId  the user id to set
     * @param
     * @return the AmplitudeClient
     */
    public synchronized AmplitudeClient initializeInternal(
            final Context context,
            final String apiKey,
            final String userId,
            final String platform,
            final boolean enableDiagnosticLogging,
            final Call.Factory callFactory
    ) {
        if (context == null) {
            logger.e(TAG, "Argument context cannot be null in initialize()");
            return this;
        }

        if (Utils.isEmptyString(apiKey)) {
            logger.e(TAG, "Argument apiKey cannot be null or blank in initialize()");
            return this;
        }

        this.context = context.getApplicationContext();
        this.apiKey = apiKey;
        this.dbHelper = DatabaseHelper.getDatabaseHelper(this.context, this.instanceName);
        this.platform = Utils.isEmptyString(platform) ? Constants.PLATFORM : platform;

        final AmplitudeClient client = this;
        runOnLogThread(() -> {
            if (!initialized) {
                // this try block is idempotent, so it's safe to retry initialize if failed
                try {
                    if (callFactory == null) {
                        // defer OkHttp client to first call
                        final Provider<Call.Factory> callProvider
                                = DoubleCheck.provider(OkHttpClient::new);
                        this.callFactory = request -> callProvider.get().newCall(request);
                    } else {
                        this.callFactory = callFactory;
                    }

                    if (useDynamicConfig) {
                        ConfigManager.getInstance().refresh(new ConfigManager.RefreshListener() {
                            @Override
                            public void onFinished() {
                                url = ConfigManager.getInstance().getIngestionEndpoint();
                            }
                        }, serverZone);
                    }

                    deviceInfo = initializeDeviceInfo();
                    deviceId = initializeDeviceId();
                    if (this.deviceIdCallback != null) {
                        this.deviceIdCallback.onDeviceIdReady(deviceId);
                    }
                    deviceInfo.prefetch();

                    if (userId != null) {
                        client.userId = userId;
                        dbHelper.insertOrReplaceKeyValue(USER_ID_KEY, userId);
                    } else {
                        client.userId = dbHelper.getValue(USER_ID_KEY);
                    }
                    final Long optOutLong = dbHelper.getLongValue(OPT_OUT_KEY);
                    optOut = optOutLong != null && optOutLong == 1;

                    // try to restore previous session id
                    previousSessionId = getLongvalue(PREVIOUS_SESSION_ID_KEY, -1);
                    if (previousSessionId >= 0) {
                        sessionId = previousSessionId;
                    }

                    // reload event meta data
                    sequenceNumber = getLongvalue(SEQUENCE_NUMBER_KEY, 0);
                    lastEventId = getLongvalue(LAST_EVENT_ID_KEY, -1);
                    lastIdentifyId = getLongvalue(LAST_IDENTIFY_ID_KEY, -1);
                    lastEventTime = getLongvalue(LAST_EVENT_TIME_KEY, -1);

                    // install database reset listener to re-insert metadata in memory
                    dbHelper.setDatabaseResetListener(new DatabaseResetListener() {
                        @Override
                        public void onDatabaseReset(SQLiteDatabase db) {
                            dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.STORE_TABLE_NAME, DEVICE_ID_KEY, client.deviceId);
                            dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.STORE_TABLE_NAME, USER_ID_KEY, client.userId);
                            dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.LONG_STORE_TABLE_NAME, OPT_OUT_KEY, client.optOut ? 1L : 0L);
                            dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.LONG_STORE_TABLE_NAME, PREVIOUS_SESSION_ID_KEY, client.sessionId);
                            dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.LONG_STORE_TABLE_NAME, LAST_EVENT_TIME_KEY, client.lastEventTime);
                        }
                    });

                    // set up listener to core package to receive exposure events from Experiment
                    connector.getEventBridge().setEventReceiver(analyticsEvent -> {
                        String eventType = analyticsEvent.getEventType();
                        JSONObject eventProperties = JSONUtil.toJSONObject(analyticsEvent.getEventProperties());
                        JSONObject userProperties = JSONUtil.toJSONObject(analyticsEvent.getUserProperties());
                        logEventAsync(eventType, eventProperties, null, userProperties,
                            null, null, getCurrentTimeMillis(), false);
                        return Unit.INSTANCE;
                    });

                    // Set user ID and device ID in core identity store for use in Experiment SDK
                    connector.getIdentityStore().setIdentity(new Identity(userId, deviceId, new HashMap<>()));

                    initialized = true;

                } catch (CursorWindowAllocationException e) {  // treat as uninitialized SDK
                    logger.e(TAG, String.format(
                            "Failed to initialize Amplitude SDK due to: %s", e.getMessage()
                    ));
                    client.apiKey = null;
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

    /**
     * @deprecated - We removed Diagnostics class and this function has no-op.
     * Will completely remove it in the near future.
     */
    public AmplitudeClient enableDiagnosticLogging() {
        return this;
    }

    /**
     * @deprecated - We removed Diagnostics class and this function has no-op.
     * Will completely remove it in the near future.
     */
    public AmplitudeClient disableDiagnosticLogging() {
        return this;
    }

    /**
     * @deprecated - We removed Diagnostics class and this function has no-op.
     * Will completely remove it in the near future.
     */
    public AmplitudeClient setDiagnosticEventMaxCount(int eventMaxCount) {
        return this;
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
        useAdvertisingIdForDeviceId = true;
        return this;
    }

    /**
     * Use Android app set id as the user's device ID.
     *
     * @return the AmplitudeClient
     */
    public AmplitudeClient useAppSetIdForDeviceId() {
        useAppSetIdForDeviceId = true;
        return this;
    }

    /**
     * Enable location listening in the SDK. This will add the user's current lat/lon coordinates
     * to every event logged.
     *
     * This function should be called before SDK initialization, e.g. {@link #initialize(Context, String)}.
     *
     * @return the AmplitudeClient
     */
    public AmplitudeClient enableLocationListening() {
        this.locationListening = true;
        if (this.deviceInfo != null) {
            this.deviceInfo.setLocationListening(true);
        }
        return this;
    }

    /**
     * Disable location listening in the SDK. This will stop the sending of the user's current
     * lat/lon coordinates.
     *
     * This function should be called before SDK initialization, e.g. {@link #initialize(Context, String)}.
     *
     * @return the AmplitudeClient
     */
    public AmplitudeClient disableLocationListening() {
        this.locationListening = false;
        if (this.deviceInfo != null) {
            this.deviceInfo.setLocationListening(false);
        }
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
     * Sets a custom server url for event upload.
     *
     * We now have a new method setServerZone. To send data to Amplitude's EU servers, recommend to
     * use setServerZone method like client.setServerZone(AmplitudeServerZone.EU);
     *
     * @param serverUrl - a string url for event upload.
     * @return the AmplitudeClient
     */
    public AmplitudeClient setServerUrl(String serverUrl) {
        if (!Utils.isEmptyString(serverUrl)) {
            url = serverUrl;
        }
        return this;
    }

    /**
     * Set Bearer Token to be included in request header.
     * @param token
     * @return the AmplitudeClient
     */
    public AmplitudeClient setBearerToken(String token) {
        this.bearerToken = token;
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

    public AmplitudeClient setTrackingOptions(TrackingOptions trackingOptions) {
        inputTrackingOptions = trackingOptions;
        appliedTrackingOptions = TrackingOptions.copyOf(inputTrackingOptions);
        if (coppaControlEnabled) {
            appliedTrackingOptions.mergeIn(TrackingOptions.forCoppaControl());
        }
        apiPropertiesTrackingOptions = appliedTrackingOptions.getApiPropertiesTrackingOptions();
        return this;
    }

    /**
     * Enable COPPA (Children's Online Privacy Protection Act) restrictions on ADID, city, IP address and location tracking.
     * This can be used by any customer that does not want to collect ADID, city, IP address and location tracking.
     */
    public AmplitudeClient enableCoppaControl() {
        coppaControlEnabled = true;
        appliedTrackingOptions.mergeIn(TrackingOptions.forCoppaControl());
        apiPropertiesTrackingOptions = appliedTrackingOptions.getApiPropertiesTrackingOptions();
        return this;
    }

    /**
     * Disable COPPA (Children's Online Privacy Protection Act) restrictions on ADID, city, IP address and location tracking.
     */
    public AmplitudeClient disableCoppaControl() {
        coppaControlEnabled = false;
        appliedTrackingOptions = TrackingOptions.copyOf(inputTrackingOptions);
        apiPropertiesTrackingOptions = appliedTrackingOptions.getApiPropertiesTrackingOptions();
        return this;
    }

    /**
     * Sets opt out. If true then the SDK does not track any events for the user.
     *
     * @param optOut whether or not to opt the user out of tracking
     * @return the AmplitudeClient
     */
    public AmplitudeClient setOptOut(final boolean optOut) {
        if (!contextAndApiKeySet("setOptOut()")) {
            return this;
        }

        final AmplitudeClient client = this;
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                if (Utils.isEmptyString(apiKey)) { // in case initialization failed
                    return;
                }
                client.optOut = optOut;
                dbHelper.insertOrReplaceKeyLongValue(OPT_OUT_KEY, optOut ? 1L : 0L);
            }
        });
        return this;
    }

    /**
     * Library name is default as `amplitude-android`.
     * Notice: You will only want to set it when following conditions are met.
     * 1. You develop your own library which bridges Amplitude Android native library.
     * 2. You want to track your library as one of the data sources.
     */
    public AmplitudeClient setLibraryName(final String libraryName) {
        this.libraryName = libraryName;
        return this;
    }

    /**
     * Library version is default as the latest Amplitude Android SDK version.
     * Notice: You will only want to set it when following conditions are met.
     * 1. You develop your own library which bridges Amplitude Android native library.
     * 2. You want to track your library as one of the data sources.
     */
    public AmplitudeClient setLibraryVersion(final String libraryVersion) {
        this.libraryVersion = libraryVersion;
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
     * Set log callback, it can help read and collect error message from sdk
     *
     * @param callback
     * @return the AmplitudeClient
     */
    public AmplitudeClient setLogCallback(AmplitudeLogCallback callback) {
        logger.setAmplitudeLogCallback(callback);
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
     * Enable/disable flushing of unsent events on app close (enabled by default).
     *
     * @param flushEventsOnClose whether to flush unsent events on app close
     * @return the AmplitudeClient
     */
    public AmplitudeClient setFlushEventsOnClose(boolean flushEventsOnClose) {
        this.flushEventsOnClose = flushEventsOnClose;
        return this;
    }

    /**
     * Track session events amplitude client. If enabled then the SDK will automatically send
     * start and end session events to mark the start and end of the user's sessions.
     *
     * @param trackingSessionEvents whether to enable tracking of session events
     * @return the AmplitudeClient
     */
    public AmplitudeClient trackSessionEvents(boolean trackingSessionEvents) {
        this.trackingSessionEvents = trackingSessionEvents;
        return this;
    }

    /**
     * Turning this flag on will find the best server url automatically based on users' geo location.
     * Note:
     * 1. If you have your own proxy server and use `setServerUrl` API, please leave this off.
     * 2. If you have users in China Mainland, we suggest you turn this on.
     *
     * @param useDynamicConfig whether to enable dynamic config
     * @return the AmplitudeClient
     */
    public AmplitudeClient setUseDynamicConfig(boolean useDynamicConfig) {
        this.useDynamicConfig = useDynamicConfig;
        return this;
    }

    /**
     * Show Amplitude Event Explorer for the given activity.
     *
     * @param activity root activity
     */
    public void showEventExplorer(Activity activity) {
        if (this.eventExplorer == null) {
            this.eventExplorer = new EventExplorer(this.instanceName);
        }
        this.eventExplorer.show(activity);
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
     * Add middleware to the middleware runner
     */
    public void addEventMiddleware(Middleware middleware) {
        middlewareRunner.add(middleware);
    }

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
     * @param extra           the extra unstructured data for middleware
     */
    public void logEvent(String eventType, JSONObject eventProperties, MiddlewareExtra extra) {
        logEvent(eventType, eventProperties, null, getCurrentTimeMillis(), false, extra);
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
     */
    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups, boolean outOfSession) {
        logEvent(eventType, eventProperties, groups, getCurrentTimeMillis(), outOfSession);
    }

    /**
     * Log event with the specified event type, event properties, groups, timestamp, with optional
     * out of session flag. If out of session is true, then the sessionId will be -1 for the event,
     * indicating that it is not part of the current session. Note: this might be useful when
     * logging events for notifications received.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @param timestamp       the timestamp in millisecond since epoch
     * @param outOfSession    the out of session
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Setting Event Properties</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-groups">
     *     Setting Groups</a>
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-sessions">
     *     Tracking Sessions</a>
     */
    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups, long timestamp, boolean outOfSession) {
        logEvent(eventType, eventProperties, groups,
                timestamp, outOfSession, null);
    }

    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups, long timestamp, boolean outOfSession, MiddlewareExtra extra) {
        if (validateLogEvent(eventType)) {
            logEventAsync(
                eventType, eventProperties, null, null, groups, null,
                timestamp, outOfSession, extra);
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
     */
    public void logEventSync(String eventType, JSONObject eventProperties, JSONObject groups) {
        logEventSync(eventType, eventProperties, groups, false);
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
        logEventSync(eventType, eventProperties, groups, getCurrentTimeMillis(), outOfSession);
    }

    /**
     * Log event with the specified event type, event properties, groups, timestamp,  with optional
     * sout of ession flag. If out of session is true, then the sessionId will be -1 for the event,
     * indicating that it is not part of the current session. Note: this might be useful when
     * logging events for notifications received.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @param timestamp       the timestamp in milliseconds since epoch
     * @param outOfSession    the out of session
     */
    public void logEventSync(String eventType, JSONObject eventProperties, JSONObject groups, long timestamp, boolean outOfSession) {
        if (validateLogEvent(eventType)) {
            logEvent(eventType, eventProperties, null, null, groups, null, timestamp, outOfSession);
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
        if (Utils.isEmptyString(eventType)) {
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
           JSONObject apiProperties, JSONObject userProperties, JSONObject groups,
           JSONObject groupProperties, final long timestamp, final boolean outOfSession) {
        logEventAsync(eventType,eventProperties, apiProperties, userProperties, groups,groupProperties, timestamp, outOfSession, null);
    }

    protected void logEventAsync(final String eventType, JSONObject eventProperties,
            JSONObject apiProperties, JSONObject userProperties, JSONObject groups,
            JSONObject groupProperties, final long timestamp, final boolean outOfSession, MiddlewareExtra extra) {
        // Clone the incoming eventProperties object before sending over
        // to the log thread. Helps avoid ConcurrentModificationException
        // if the caller starts mutating the object they passed in.
        // Only does a shallow copy, so it's still possible, though unlikely,
        // to hit concurrent access if the caller mutates deep in the object.
        if (eventProperties != null) {
            eventProperties = Utils.cloneJSONObject(eventProperties);
        }

        if (apiProperties != null) {
            apiProperties = Utils.cloneJSONObject(apiProperties);
        }

        if (userProperties != null) {
            userProperties = Utils.cloneJSONObject(userProperties);
        }

        if (groups != null) {
            groups = Utils.cloneJSONObject(groups);
        }

        if (groupProperties != null) {
            groupProperties = Utils.cloneJSONObject(groupProperties);
        }

        final JSONObject copyEventProperties = eventProperties;
        final JSONObject copyApiProperties = apiProperties;
        final JSONObject copyUserProperties = userProperties;
        final JSONObject copyGroups = groups;
        final JSONObject copyGroupProperties = groupProperties;
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                if (Utils.isEmptyString(apiKey)) {  // in case initialization failed
                    return;
                }
                logEvent(
                    eventType, copyEventProperties, copyApiProperties,
                    copyUserProperties, copyGroups, copyGroupProperties, timestamp, outOfSession, extra
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
                            JSONObject userProperties, JSONObject groups, JSONObject groupProperties,
                            long timestamp, boolean outOfSession) {
        return logEvent(eventType, eventProperties, apiProperties, userProperties, groups, groupProperties, timestamp,outOfSession, null);
    }

    protected long logEvent(String eventType, JSONObject eventProperties, JSONObject apiProperties,
            JSONObject userProperties, JSONObject groups, JSONObject groupProperties,
            long timestamp, boolean outOfSession, MiddlewareExtra extra) {

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

        long result = -1;
        JSONObject event = new JSONObject();
        try {
            event.put("event_type", replaceWithJSONNull(eventType));
            event.put("timestamp", timestamp);
            event.put("user_id", replaceWithJSONNull(userId));
            event.put("device_id", replaceWithJSONNull(deviceId));
            event.put("session_id", outOfSession ? -1 : sessionId);
            event.put("uuid", UUID.randomUUID().toString());
            event.put("sequence_number", getNextSequenceNumber());

            if (appliedTrackingOptions.shouldTrackVersionName()) {
                event.put("version_name", replaceWithJSONNull(deviceInfo.getVersionName()));
            }
            if (appliedTrackingOptions.shouldTrackOsName()) {
                event.put("os_name", replaceWithJSONNull(deviceInfo.getOsName()));
            }
            if (appliedTrackingOptions.shouldTrackOsVersion()) {
                event.put("os_version", replaceWithJSONNull(deviceInfo.getOsVersion()));
            }
            if (appliedTrackingOptions.shouldTrackApiLevel()) {
                event.put("api_level", replaceWithJSONNull(Build.VERSION.SDK_INT));
            }
            if (appliedTrackingOptions.shouldTrackDeviceBrand()) {
                event.put("device_brand", replaceWithJSONNull(deviceInfo.getBrand()));
            }
            if (appliedTrackingOptions.shouldTrackDeviceManufacturer()) {
                event.put("device_manufacturer", replaceWithJSONNull(deviceInfo.getManufacturer()));
            }
            if (appliedTrackingOptions.shouldTrackDeviceModel()) {
                event.put("device_model", replaceWithJSONNull(deviceInfo.getModel()));
            }
            if (appliedTrackingOptions.shouldTrackCarrier()) {
                event.put("carrier", replaceWithJSONNull(deviceInfo.getCarrier()));
            }
            if (appliedTrackingOptions.shouldTrackCountry()) {
                event.put("country", replaceWithJSONNull(deviceInfo.getCountry()));
            }
            if (appliedTrackingOptions.shouldTrackLanguage()) {
                event.put("language", replaceWithJSONNull(deviceInfo.getLanguage()));
            }
            if (appliedTrackingOptions.shouldTrackPlatform()) {
                event.put("platform", platform);
            }

            JSONObject library = new JSONObject();
            library.put("name", this.libraryName == null ? Constants.LIBRARY_UNKNOWN : this.libraryName);
            library.put("version", this.libraryVersion == null ? Constants.VERSION_UNKNOWN : this.libraryVersion);
            event.put("library", library);

            if (plan != null) {
                event.put("plan", plan.toJSONObject());
            }

            apiProperties = (apiProperties == null) ? new JSONObject() : apiProperties;
            if (apiPropertiesTrackingOptions != null && apiPropertiesTrackingOptions.length() > 0) {
                apiProperties.put("tracking_options", apiPropertiesTrackingOptions);
            }

            if (appliedTrackingOptions.shouldTrackLatLng()) {
                Location location = deviceInfo.getMostRecentLocation();
                if (location != null) {
                    JSONObject locationJSON = new JSONObject();
                    locationJSON.put("lat", location.getLatitude());
                    locationJSON.put("lng", location.getLongitude());
                    apiProperties.put("location", locationJSON);
                }
            }
            if (appliedTrackingOptions.shouldTrackAdid() && deviceInfo.getAdvertisingId() != null) {
                apiProperties.put("androidADID", deviceInfo.getAdvertisingId());
            }
            if (appliedTrackingOptions.shouldTrackAppSetId() && deviceInfo.getAppSetId() != null) {
                apiProperties.put("android_app_set_id", deviceInfo.getAppSetId());
            }
            apiProperties.put("limit_ad_tracking", deviceInfo.isLimitAdTrackingEnabled());
            apiProperties.put("gps_enabled", deviceInfo.isGooglePlayServicesEnabled());

            event.put("api_properties", apiProperties);
            event.put("event_properties", (eventProperties == null) ? new JSONObject()
                : truncate(eventProperties));
            event.put("user_properties", (userProperties == null) ? new JSONObject()
                : truncate(userProperties));
            event.put("groups", (groups == null) ? new JSONObject() : truncate(groups));
            event.put("group_properties", (groupProperties == null) ? new JSONObject()
                : truncate(groupProperties));
            result = saveEvent(eventType, event, extra);

            // If the the event is an identify, update the user properties to the core identity
            // for experiment SDK to consume.
            if (eventType.equals(Constants.IDENTIFY_EVENT) && userProperties != null) {
                connector.getIdentityStore().editIdentity()
                    .updateUserProperties(JSONUtil.toUpdateUserPropertiesMap(userProperties))
                    .commit();
            }
        } catch (JSONException e) {
            logger.e(TAG, String.format(
                "JSON Serialization of event type %s failed, skipping: %s", eventType, e.toString()
            ));
        }

        return result;
    }

    /**
     * Save event long. Internal method to save an event to the database.
     *
     * @param eventType the event type
     * @param event     the event
     * @param extra     the extra unstructured data for middleware
     * @return the event ID if succeeded, else -1
     */
    protected long saveEvent(String eventType, JSONObject event, MiddlewareExtra extra) {
        if (!middlewareRunner.run(new MiddlewarePayload(event, extra))) return -1;

        String eventString = event.toString();
        if (Utils.isEmptyString(eventString)) {
            logger.e(TAG, String.format(
                "Detected empty event string for event type %s, skipping", eventType
            ));
            return -1;
        }

        if (eventType.equals(Constants.IDENTIFY_EVENT) || eventType.equals(Constants.GROUP_IDENTIFY_EVENT)) {
            lastIdentifyId = dbHelper.addIdentify(eventString);
            setLastIdentifyId(lastIdentifyId);
        } else {
            lastEventId = dbHelper.addEvent(eventString);
            setLastEventId(lastEventId);
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

        return (
            eventType.equals(Constants.IDENTIFY_EVENT) ||
            eventType.equals(Constants.GROUP_IDENTIFY_EVENT)
        ) ? lastIdentifyId : lastEventId;
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
        sequenceNumber++;
        dbHelper.insertOrReplaceKeyLongValue(SEQUENCE_NUMBER_KEY, sequenceNumber);
        return sequenceNumber;
    }

    /**
     * Internal method to set the last event time.
     *
     * @param timestamp the timestamp
     */
    void setLastEventTime(long timestamp) {
        lastEventTime = timestamp;
        dbHelper.insertOrReplaceKeyLongValue(LAST_EVENT_TIME_KEY, timestamp);
    }

    /**
     * Internal method to set the last event id.
     *
     * @param eventId the event id
     */
    void setLastEventId(long eventId) {
        lastEventId = eventId;
        dbHelper.insertOrReplaceKeyLongValue(LAST_EVENT_ID_KEY, eventId);
    }

    /**
     * Internal method to set the last identify id.
     *
     * @param identifyId the identify id
     */
    void setLastIdentifyId(long identifyId) {
        lastIdentifyId = identifyId;
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
     * Internal method to set the previous session id.
     *
     * @param timestamp the timestamp
     */
    void setPreviousSessionId(long timestamp) {
        previousSessionId = timestamp;
        dbHelper.insertOrReplaceKeyLongValue(PREVIOUS_SESSION_ID_KEY, timestamp);
    }

    /**
     * Public method to start a new session if needed.
     *
     * @param timestamp the timestamp
     * @return whether or not a new session was started
     */
    public boolean startNewSessionIfNeeded(long timestamp) {
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

        logEvent(sessionEvent, null, apiProperties, null, null, null, lastEventTime, false);
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
                if (Utils.isEmptyString(apiKey)) {
                    return;
                }
                refreshSessionTime(timestamp);
                inForeground = false;
                if (flushEventsOnClose) {
                    updateServer();
                }

                // re-persist metadata into database for good measure
                dbHelper.insertOrReplaceKeyValue(DEVICE_ID_KEY, deviceId);
                dbHelper.insertOrReplaceKeyValue(USER_ID_KEY, userId);
                dbHelper.insertOrReplaceKeyLongValue(OPT_OUT_KEY, optOut ? 1L : 0L);
                dbHelper.insertOrReplaceKeyLongValue(PREVIOUS_SESSION_ID_KEY, sessionId);
                dbHelper.insertOrReplaceKeyLongValue(LAST_EVENT_TIME_KEY, lastEventTime);
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
                if (Utils.isEmptyString(apiKey)) {
                    return;
                }
                if (useDynamicConfig) {
                    ConfigManager.getInstance().refresh(new ConfigManager.RefreshListener() {
                        @Override
                        public void onFinished() {
                            url = ConfigManager.getInstance().getIngestionEndpoint();
                        }
                    }, serverZone);
                }
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

    public void logRevenue(String productId, int quantity, double price, String receipt,
                           String receiptSignature) {
        logRevenue(productId, quantity, price, receipt, receiptSignature, null);
    }
    /**
     * Log revenue with a productId, quantity, price, and receipt data for revenue verification.
     *
     * @param productId        the product id
     * @param quantity         the quantity
     * @param price            the price
     * @param receipt          the receipt
     * @param receiptSignature the receipt signature
     * @param extra            the extra unstructured data for middleware
     * @deprecated - use {@code logRevenueV2} instead
     * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-revenue">
     *     Tracking Revenue</a>
     */
    public void logRevenue(String productId, int quantity, double price, String receipt,
            String receiptSignature, MiddlewareExtra extra) {
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
            Constants.AMP_REVENUE_EVENT, null, apiProperties, null, null, null, getCurrentTimeMillis(), false, extra
        );
    }

    /**
     * Log revenue v2. Create a {@link Revenue} object to hold your revenue data and properties,
     * and log it as a revenue event using this method.
     *
     * @param revenue a {@link Revenue} object
     */
    public void logRevenueV2(Revenue revenue) {
        logRevenueV2(revenue, null);
    }

    public void logRevenueV2(Revenue revenue, MiddlewareExtra extra) {
        if (!contextAndApiKeySet("logRevenueV2()") || revenue == null || !revenue.isValidRevenue()) {
            return;
        }

        logEvent(Constants.AMP_REVENUE_EVENT, revenue.toJSONObject(), null, null, null, null, getCurrentTimeMillis(), false, extra);
    }

    /**
     * Sets user properties. This is a convenience wrapper around the
     * {@link Identify} API to set multiple user properties with a single
     * command. <b>Note:</b> the replace parameter is deprecated and has no effect.
     *
     * @param userProperties the user properties
     * @param replace        the replace - has no effect
     * @deprecated
     */
    public void setUserProperties(final JSONObject userProperties, final boolean replace) {
        setUserProperties(userProperties);
    }

    /**
     * Sets user properties. This is a convenience wrapper around the
     * {@link Identify} API to set multiple user properties with a single
     * command.
     *
     * @param userProperties the user properties
     */
    public void setUserProperties(final JSONObject userProperties) {
        setUserProperties(userProperties, null);
    }

    /**
     * Sets user properties. This is a convenience wrapper around the
     * {@link Identify} API to set multiple user properties with a single
     * command.
     *
     * @param userProperties the user properties
     * @param extra          the extra unstructured data for middleware
     */
    public void setUserProperties(final JSONObject userProperties, MiddlewareExtra extra) {
        if (userProperties == null || userProperties.length() == 0 ||
                !contextAndApiKeySet("setUserProperties")) {
            return;
        }

        Identify identify = convertPropertiesToIdentify(userProperties);
        if (identify != null) {
            identify(identify, false, extra);
        }
    }

    private Identify convertPropertiesToIdentify(final JSONObject properties) {
        if (properties == null) {
            return null;
        }

        // sanitize and truncate properties before trying to convert to identify
        JSONObject sanitized = truncate(properties);
        if (sanitized.length() == 0) {
            return null;
        }

        Identify identify = new Identify();
        Iterator<?> keys = sanitized.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {
                identify.setUserProperty(key, sanitized.get(key));
            } catch (JSONException e) {
                logger.e(TAG, e.toString());
            }
        }
        return identify;
    }

    /**
     * Clear user properties. This will clear all user properties at once. <b>Note: the
     * result is irreversible!</b>
     */
    public void clearUserProperties() {
        Identify identify = new Identify().clearAll();
        identify(identify);
    }

    /**
     * Identify. Use this to send an {@link Identify} object containing
     * user property operations to Amplitude server.
     *
     * @param identify an {@link Identify} object
     */
    public void identify(Identify identify) {
        identify(identify, false);
    }

    public void identify(Identify identify, boolean outOfSession) {
        identify(identify, outOfSession, null);
    }

    /**
     * Identify. Use this to send an {@link com.amplitude.api.Identify} object containing
     * user property operations to Amplitude server. If outOfSession is true, then the identify
     * event is sent with a session id of -1, and does not trigger any session-handling logic.
     *
     * @param identify      an {@link Identify} object
     * @param outOfSession  whther to log the identify event out of session
     * @param extra         the extra unstructured data for middleware
     */
    public void identify(Identify identify, boolean outOfSession, MiddlewareExtra extra) {
        if (
            identify == null || identify.userPropertiesOperations.length() == 0 ||
            !contextAndApiKeySet("identify()")
        ) return;
        logEventAsync(
            Constants.IDENTIFY_EVENT, null, null, identify.userPropertiesOperations,
            null, null, getCurrentTimeMillis(), outOfSession, extra
        );
    }

    /**
     * Sets the user's group(s).
     *
     * @param groupType the group type (ex: orgId)
     * @param groupName the group name (ex: 15)
     */
    public void setGroup(String groupType, Object groupName) {
        setGroup(groupType, groupName, null);
    }

    /**
     * Sets the user's group(s).
     *
     * @param groupType the group type (ex: orgId)
     * @param groupName the group name (ex: 15)
     * @param extra     the extra unstructured data for middleware
     */
    public void setGroup(String groupType, Object groupName, MiddlewareExtra extra) {
        if (!contextAndApiKeySet("setGroup()") || Utils.isEmptyString(groupType)) {
            return;
        }

        JSONObject group = null;
        try {
            group = new JSONObject().put(groupType, groupName);
        } catch (JSONException e) {
            logger.e(TAG, e.toString());
        }

        Identify identify = new Identify().setUserProperty(groupType, groupName);
        logEventAsync(Constants.IDENTIFY_EVENT, null, null, identify.userPropertiesOperations,
                group, null, getCurrentTimeMillis(), false, extra);
    }

    public void groupIdentify(String groupType, Object groupName, Identify groupIdentify) {
        groupIdentify(groupType, groupName, groupIdentify, false);
    }

    public void groupIdentify(String groupType, Object groupName, Identify groupIdentify, boolean outOfSession) {
        groupIdentify(groupType, groupName, groupIdentify, outOfSession, null);
    }

    public void groupIdentify(String groupType, Object groupName, JSONObject groupProperties, boolean outOfSession, MiddlewareExtra extra) {
        Identify identify = convertPropertiesToIdentify(groupProperties);
        if (identify != null) {
            groupIdentify(groupType, groupName, identify, outOfSession, extra);
        }
    }

    public void groupIdentify(String groupType, Object groupName, Identify groupIdentify, boolean outOfSession, MiddlewareExtra extra) {
        if (groupIdentify == null || groupIdentify.userPropertiesOperations.length() == 0 ||
            !contextAndApiKeySet("groupIdentify()") || Utils.isEmptyString(groupType)) {

            return;
        }

        JSONObject group = null;
        try {
            group = new JSONObject().put(groupType, groupName);
        } catch (JSONException e) {
            logger.e(TAG, e.toString());
        }

        logEventAsync(
            Constants.GROUP_IDENTIFY_EVENT, null, null, null, group,
            groupIdentify.userPropertiesOperations, getCurrentTimeMillis(), outOfSession, extra
        );
    }

    /**
     * Truncate values in a JSON object. Any string values longer than 1024 characters will be
     * truncated to 1024 characters.
     * Any dictionary with more than 1000 items will be ignored.
     *
     * @param object the object
     * @return the truncated JSON object
     */
    public JSONObject truncate(JSONObject object) {
        if (object == null) {
            return new JSONObject();
        }

        if (object.length() > Constants.MAX_PROPERTY_KEYS) {
            logger.w(TAG, "Warning: too many properties (more than 1000), ignoring");
            return new JSONObject();
        }

        Iterator<?> keys = object.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            try {
                Object value = object.get(key);
                // do not truncate revenue receipt and receipt sig fields
                if (key.equals(Constants.AMP_REVENUE_RECEIPT) ||
                        key.equals(Constants.AMP_REVENUE_RECEIPT_SIG)) {
                    object.put(key, value);
                } else if (value.getClass().equals(String.class)) {
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
            return new JSONArray();
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
    public static String truncate(String value) {
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
    public AmplitudeClient setUserId(final String userId) {
        return setUserId(userId, false);
    }

    /**
     * Sets the user id (can be null).
     * If startNewSession is true, ends the session for the previous user and starts a new
     * session for the new user id.
     *
     * @param userId the user id
     * @return the AmplitudeClient
     */
    public AmplitudeClient setUserId(final String userId, final boolean startNewSession) {
        if (!contextAndApiKeySet("setUserId()")) {
            return this;
        }

        final AmplitudeClient client = this;
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                if (Utils.isEmptyString(client.apiKey)) {  // in case initialization failed
                    return;
                }

                // end previous session
                if (startNewSession && trackingSessionEvents) {
                    sendSessionEvent(END_SESSION_EVENT);
                }

                client.userId = userId;
                dbHelper.insertOrReplaceKeyValue(USER_ID_KEY, userId);

                // start new session
                if (startNewSession) {
                    long timestamp = getCurrentTimeMillis();
                    setSessionId(timestamp);
                    refreshSessionTime(timestamp);
                    if (trackingSessionEvents) {
                        sendSessionEvent(START_SESSION_EVENT);
                    }
                }

                // update the user in the core identity store to notify
                // experiment to re-fetch variants with the new identity
                client.connector.getIdentityStore().editIdentity().setUserId(userId).commit();
            }
        });
        return this;
    }

    /**
     * Sets a custom device id. <b>Note: only do this if you know what you are doing!</b>
     *
     * @param deviceId the device id
     * @return the AmplitudeClient
     */
    public AmplitudeClient setDeviceId(final String deviceId) {
        Set<String> invalidDeviceIds = getInvalidDeviceIds();
        if (!contextAndApiKeySet("setDeviceId()") || Utils.isEmptyString(deviceId) ||
                invalidDeviceIds.contains(deviceId)) {
            return this;
        }

        final AmplitudeClient client = this;
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                if (Utils.isEmptyString(client.apiKey)) {  // in case initialization failed
                    return;
                }
                client.deviceId = deviceId;
                saveDeviceId(deviceId);

                // update the user in the core identity store to notify
                // experiment to re-fetch variants with the new identity
                client.connector.getIdentityStore().editIdentity().setDeviceId(deviceId).commit();
            }
        });
        return this;
    }

    /**
     * Regenerates a new random deviceId for current user. Note: this is not recommended unless you
     * know what you are doing. This can be used in conjunction with setUserId(null) to anonymize
     * users after they log out. With a null userId and a completely new deviceId, the current user
     * would appear as a brand new user in dashboard.
     */
    public AmplitudeClient regenerateDeviceId() {
        if (!contextAndApiKeySet("regenerateDeviceId()")) {
            return this;
        }

        final AmplitudeClient client = this;
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                if (Utils.isEmptyString(client.apiKey)) { // in case initialization failed
                    return;
                }
                String randomId = DeviceInfo.generateUUID() + "R";
                setDeviceId(randomId);
            }
        });
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
                if (Utils.isEmptyString(apiKey)) {  // in case initialization failed
                    return;
                }
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
                List<JSONObject> events = dbHelper.getEvents(lastEventId, batchSize);
                List<JSONObject> identifys = dbHelper.getIdentifys(lastIdentifyId, batchSize);

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
                        makeEventUploadPostRequest(callFactory, mergedEventsString, maxEventId, maxIdentifyId);
                    }
                });
            } catch (JSONException e) {
                uploadingCurrently.set(false);
                logger.e(TAG, e.toString());
            } catch (CursorWindowAllocationException e) {
                // handle CursorWindowAllocationException when fetching events, defer upload
                uploadingCurrently.set(false);
                logger.e(TAG, String.format(
                    "Caught Cursor window exception during event upload, deferring upload: %s",
                    e.getMessage()
                ));
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
    protected void makeEventUploadPostRequest(Call.Factory client, String events, final long maxEventId, final long maxIdentifyId) {
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

        Request request;
        try {
             Request.Builder builder = new Request.Builder()
                     .url(url)
                     .post(body);

             if (!Utils.isEmptyString(bearerToken)) {
                builder.addHeader("Authorization", "Bearer " + bearerToken);
             }

             request = builder.build();
        } catch (IllegalArgumentException e) {
            logger.e(TAG, e.toString());
            uploadingCurrently.set(false);
            return;
        }

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

    protected DeviceInfo initializeDeviceInfo() {
        return new DeviceInfo(context, this.locationListening);
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
        invalidDeviceIds.add("00000000-0000-0000-0000-000000000000");

        return invalidDeviceIds;
    }

    private String initializeDeviceId() {
        Set<String> invalidIds = getInvalidDeviceIds();

        // see if device id already stored in db
        String deviceId = dbHelper.getValue(DEVICE_ID_KEY);
        if (!(Utils.isEmptyString(deviceId) || invalidIds.contains(deviceId) || deviceId.endsWith("S"))) {
            return deviceId;
        }

        if (!newDeviceIdPerInstall && useAdvertisingIdForDeviceId && !deviceInfo.isLimitAdTrackingEnabled()) {
            // Android ID is deprecated by Google.
            // We are required to use Advertising ID, and respect the advertising ID preference

            String advertisingId = deviceInfo.getAdvertisingId();
            if (!(Utils.isEmptyString(advertisingId) || invalidIds.contains(advertisingId))) {
                saveDeviceId(advertisingId);
                return advertisingId;
            }
        }

        if (useAppSetIdForDeviceId) {
            String appSetId = deviceInfo.getAppSetId();
            if (!(Utils.isEmptyString(appSetId) || invalidIds.contains(appSetId))) {
                // Suffix with S for app set id so in future we can tell if device id is from app set id
                String appSetDeviceId = appSetId + "S";
                saveDeviceId(appSetDeviceId);
                return appSetDeviceId;
            }
        }

        // If this still fails, generate random identifier that does not persist
        // across installations. Append R to distinguish as randomly generated
        String randomId = deviceInfo.generateUUID() + "R";
        saveDeviceId(randomId);
        return randomId;
    }

    private void saveDeviceId(String deviceId) {
        dbHelper.insertOrReplaceKeyValue(DEVICE_ID_KEY, deviceId);
    }

    public AmplitudeClient setDeviceIdCallback(AmplitudeDeviceIdCallback callback) {
        this.deviceIdCallback = callback;
        return this;
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
        if (Utils.isEmptyString(apiKey)) {
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
     * Internal method to fetch the current time millis. Used for testing.
     *
     * @return the current time millis
     */
    protected long getCurrentTimeMillis() { return System.currentTimeMillis(); }

    /**
     * Set tracking plan information.
     * @param plan Plan object
     * @return the AmplitudeClient
     */
    public AmplitudeClient setPlan(Plan plan) {
        this.plan = plan;
        return this;
    }

    /**
     * Set Amplitude Server Zone, switch to zone related configuration,
     * including dynamic configuration and server url.
     *
     * To send data to Amplitude's EU servers, you need to configure the serverZone to EU like
     * client.setServerZone(AmplitudeServerZone.EU);
     *
     * @param serverZone AmplitudeServerZone, US or EU, default is US
     * @return the AmplitudeClient
     */
    public AmplitudeClient setServerZone(AmplitudeServerZone serverZone) {
        return setServerZone(serverZone, true);
    }

    /**
     * Set Amplitude Server Zone, switch to zone related configuration,
     * including dynamic configuration. If updateServerUrl is true, including server url as well.
     * Recommend to keep updateServerUrl to be true for alignment.
     *
     * @param serverZone AmplitudeServerZone, US or EU, default is US
     * @param updateServerUrl if update server url when update server zone, recommend setting true
     * @return
     */
    public AmplitudeClient setServerZone(AmplitudeServerZone serverZone, boolean updateServerUrl) {
        if (serverZone == null) {
            return null;
        }
        this.serverZone = serverZone;
        if (updateServerUrl) {
            setServerUrl(AmplitudeServerZone.getEventLogApiForZone(serverZone));
        }
        return this;
    }
}
