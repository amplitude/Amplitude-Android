package com.amplitude.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

public class Amplitude {

    public static final String TAG = "com.amplitude.api.Amplitude";

    private static Context context;
    private static String apiKey;
    private static String userId;
    private static String deviceId;
    private static boolean newDeviceIdPerInstall = false;
    private static boolean useAdvertisingIdForDeviceId = false;
    private static boolean initialized = false;

    private static DeviceInfo deviceInfo;
    private static String advertisingId;
    private static int versionCode;
    private static String versionName;
    private static int buildVersionSdk;
    private static String buildVersionRelease;
    private static String phoneBrand;
    private static String phoneManufacturer;
    private static String phoneModel;
    private static String phoneCarrier;
    private static String country;
    private static String language;

    private static JSONObject userProperties;

    private static long sessionId = -1;
    private static boolean sessionOpen = false;
    private static long sessionTimeoutMillis = Constants.SESSION_TIMEOUT_MILLIS;
    private static Runnable endSessionRunnable;

    private static AtomicBoolean updateScheduled = new AtomicBoolean(false);
    private static AtomicBoolean uploadingCurrently = new AtomicBoolean(false);

    public static final String START_SESSION_EVENT = "session_start";
    public static final String END_SESSION_EVENT = "session_end";
    public static final String REVENUE_EVENT = "revenue_amount";

    static WorkerThread logThread = new WorkerThread("logThread");
    static WorkerThread httpThread = new WorkerThread("httpThread");

    static {
        logThread.start();
        httpThread.start();
    }

    private Amplitude() {}

    public static void initialize(Context context, String apiKey) {
        initialize(context, apiKey, null);
    }

    public synchronized static void initialize(Context context, String apiKey, String userId) {
        if (context == null) {
            Log.e(TAG, "Argument context cannot be null in initialize()");
            return;
        }
        if (TextUtils.isEmpty(apiKey)) {
            Log.e(TAG, "Argument apiKey cannot be null or blank in initialize()");
            return;
        }
        if (!initialized) {
            Amplitude.context = context.getApplicationContext();
            Amplitude.apiKey = apiKey;
            initializeDeviceInfo();
            SharedPreferences preferences = context.getSharedPreferences(
                    getSharedPreferencesName(), Context.MODE_PRIVATE);
            if (userId != null) {
                Amplitude.userId = userId;
                preferences.edit().putString(Constants.PREFKEY_USER_ID, userId).commit();
            } else {
                Amplitude.userId = preferences.getString(Constants.PREFKEY_USER_ID, null);
            }
            initialized = true;
        }
    }

    private static void initializeDeviceInfo() {
        deviceInfo = new DeviceInfo(context);
        runOnLogThread(new Runnable() {

            @Override
            public void run() {
                deviceId = initializeDeviceId();
                advertisingId = deviceInfo.getAdvertisingId();
                versionCode = deviceInfo.getVersionCode();
                versionName = deviceInfo.getVersionName();
                buildVersionSdk = deviceInfo.getBuildVersionSdk();
                buildVersionRelease = deviceInfo.getBuildVersionRelease();
                phoneBrand = deviceInfo.getPhoneBrand();
                phoneManufacturer = deviceInfo.getPhoneManufacturer();
                phoneModel = deviceInfo.getPhoneModel();
                phoneCarrier = deviceInfo.getPhoneCarrier();
                country = deviceInfo.getCountry();
                language = deviceInfo.getLanguage();
            }
        });
    }

    public static void enableNewDeviceIdPerInstall(boolean newDeviceIdPerInstall) {
        Amplitude.newDeviceIdPerInstall = newDeviceIdPerInstall;
    }

    public static void useAdvertisingIdForDeviceId() {
        Amplitude.useAdvertisingIdForDeviceId = true;
    }

    public static void setSessionTimeoutMillis(long sessionTimeoutMillis) {
        Amplitude.sessionTimeoutMillis = sessionTimeoutMillis;
    }

    public static void logEvent(String eventType) {
        logEvent(eventType, null);
    }

    public static void logEvent(String eventType, JSONObject eventProperties) {
        checkedLogEvent(eventType, eventProperties, null, System.currentTimeMillis(), true);
    }

    private static void checkedLogEvent(final String eventType, final JSONObject eventProperties,
            final JSONObject apiProperties, final long timestamp, final boolean checkSession) {
        if (TextUtils.isEmpty(eventType)) {
            Log.e(TAG, "Argument eventType cannot be null or blank in logEvent()");
            return;
        }
        if (!contextAndApiKeySet("logEvent()")) {
            return;
        }
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                logEvent(eventType, eventProperties, apiProperties, timestamp, checkSession);
            }
        });
    }

    private static long logEvent(String eventType, JSONObject eventProperties,
            JSONObject apiProperties, long timestamp, boolean checkSession) {
        if (checkSession) {
            startNewSessionIfNeeded(timestamp);
        }
        setLastEventTime(timestamp);

        JSONObject event = new JSONObject();
        try {
            event.put("event_type", replaceWithJSONNull(eventType));

            event.put("timestamp", timestamp);
            event.put("user_id", (userId == null) ? replaceWithJSONNull(deviceId)
                    : replaceWithJSONNull(userId));
            event.put("device_id", replaceWithJSONNull(deviceId));
            event.put("session_id", sessionId);
            event.put("version_code", versionCode);
            event.put("version_name", replaceWithJSONNull(versionName));
            event.put("build_version_sdk", buildVersionSdk);
            event.put("build_version_release", replaceWithJSONNull(buildVersionRelease));
            event.put("phone_brand", replaceWithJSONNull(phoneBrand));
            event.put("phone_manufacturer", replaceWithJSONNull(phoneManufacturer));
            event.put("phone_model", replaceWithJSONNull(phoneModel));
            event.put("phone_carrier", replaceWithJSONNull(phoneCarrier));
            event.put("country", replaceWithJSONNull(country));
            event.put("language", replaceWithJSONNull(language));
            event.put("client", "android");

            apiProperties = (apiProperties == null) ? new JSONObject() : apiProperties;
            Location location = DeviceInfo.getMostRecentLocation(context);
            if (location != null) {
                JSONObject locationJSON = new JSONObject();
                locationJSON.put("lat", location.getLatitude());
                locationJSON.put("lng", location.getLongitude());
                apiProperties.put("location", locationJSON);
            }
            if (advertisingId != null) {
                apiProperties.put("androidADID", advertisingId);
            }

            event.put("api_properties", apiProperties);
            event.put("custom_properties", (eventProperties == null) ? new JSONObject()
                    : eventProperties);
            event.put("global_properties", (userProperties == null) ? new JSONObject()
                    : userProperties);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }

        return logEvent(event);
    }

    private static long logEvent(JSONObject event) {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        long eventId = dbHelper.addEvent(event.toString());

        if (dbHelper.getEventCount() >= Constants.EVENT_MAX_COUNT) {
            dbHelper.removeEvents(dbHelper.getNthEventId(Constants.EVENT_REMOVE_BATCH_SIZE));
        }

        if (dbHelper.getEventCount() >= Constants.EVENT_UPLOAD_THRESHOLD) {
            updateServer();
        } else {
            updateServerLater(Constants.EVENT_UPLOAD_PERIOD_MILLIS);
        }
        return eventId;
    }

    private static void runOnLogThread(Runnable r) {
        if (Thread.currentThread() != logThread) {
            logThread.post(r);
        } else {
            r.run();
        }
    }

    public static void uploadEvents() {
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

    private static void updateServerLater(long delayMillis) {
        if (!updateScheduled.getAndSet(true)) {

            logThread.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateScheduled.set(false);
                    updateServer();
                }
            }, delayMillis);
        }
    }

    private static long getLastEventTime() {
        SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
                Context.MODE_PRIVATE);
        return preferences.getLong(Constants.PREFKEY_PREVIOUS_SESSION_TIME, -1);
    }

    private static void setLastEventTime(long timestamp) {
        SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
                Context.MODE_PRIVATE);
        preferences.edit().putLong(Constants.PREFKEY_PREVIOUS_SESSION_TIME, timestamp).commit();
    }

    private static void clearEndSession() {
        SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
                Context.MODE_PRIVATE);
        preferences.edit().remove(Constants.PREFKEY_PREVIOUS_END_SESSION_TIME)
                .remove(Constants.PREFKEY_PREVIOUS_END_SESSION_ID).commit();
    }

    private static long getEndSessionTime() {
        SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
                Context.MODE_PRIVATE);
        return preferences.getLong(Constants.PREFKEY_PREVIOUS_END_SESSION_TIME, -1);
    }

    private static long getEndSessionId() {
        SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
                Context.MODE_PRIVATE);
        return preferences.getLong(Constants.PREFKEY_PREVIOUS_END_SESSION_ID, -1);
    }

    private static void openSession() {
        clearEndSession();
        sessionOpen = true;
    }

    private static void closeSession() {
        // Close the session. Events within the next MIN_TIME_BETWEEN_SESSIONS_MILLIS seconds
        // will stay in the session.
        // A startSession call within the next MIN_TIME_BETWEEN_SESSIONS_MILLIS seconds
        // will reopen the session.
        sessionOpen = false;
    }

    private static void startNewSession(long timestamp) {
        // Log session start in events
        openSession();
        sessionId = timestamp;
        SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
                Context.MODE_PRIVATE);
        preferences.edit().putLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, sessionId).commit();
        JSONObject apiProperties = new JSONObject();
        try {
            apiProperties.put("special", START_SESSION_EVENT);
        } catch (JSONException e) {
        }
        logEvent(START_SESSION_EVENT, null, apiProperties, timestamp, false);
    }

    private static void startNewSessionIfNeeded(long timestamp) {
        if (!sessionOpen) {
            long lastEndSessionTime = getEndSessionTime();
            if (timestamp - lastEndSessionTime < Constants.MIN_TIME_BETWEEN_SESSIONS_MILLIS) {
                // Sessions close enough, set sessionId to previous sessionId

                SharedPreferences preferences = context.getSharedPreferences(
                        getSharedPreferencesName(), Context.MODE_PRIVATE);
                long previousSessionId = preferences.getLong(Constants.PREFKEY_PREVIOUS_SESSION_ID,
                        -1);

                if (previousSessionId == -1) {
                    // Invalid session Id, create new sessionId
                    startNewSession(timestamp);
                } else {
                    sessionId = previousSessionId;
                }
            } else {
                // Sessions not close enough, create new sessionId
                startNewSession(timestamp);
            }
        } else {
            long lastEventTime = getLastEventTime();
            if (timestamp - lastEventTime > sessionTimeoutMillis || sessionId == -1) {
                startNewSession(timestamp);
            }
        }
    }

    public static void startSession() {
        if (!contextAndApiKeySet("startSession()")) {
            return;
        }
        final long now = System.currentTimeMillis();

        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                logThread.removeCallbacks(endSessionRunnable);
                long previousEndSessionId = getEndSessionId();
                long lastEndSessionTime = getEndSessionTime();
                if (previousEndSessionId != -1
                        && now - lastEndSessionTime < Constants.MIN_TIME_BETWEEN_SESSIONS_MILLIS) {
                    DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
                    dbHelper.removeEvent(previousEndSessionId);
                }
                startNewSessionIfNeeded(now);
                openSession();

                // Update last event time
                setLastEventTime(now);

                uploadEvents();
            }
        });
    }

    public static void endSession() {
        if (!contextAndApiKeySet("endSession()")) {
            return;
        }
        final long timestamp = System.currentTimeMillis();
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                JSONObject apiProperties = new JSONObject();
                try {
                    apiProperties.put("special", END_SESSION_EVENT);
                } catch (JSONException e) {
                }
                if (sessionOpen) {
                    long eventId = logEvent(END_SESSION_EVENT, null, apiProperties, timestamp,
                            false);

                    SharedPreferences preferences = context.getSharedPreferences(
                            getSharedPreferencesName(), Context.MODE_PRIVATE);
                    preferences.edit().putLong(Constants.PREFKEY_PREVIOUS_END_SESSION_ID, eventId)
                            .putLong(Constants.PREFKEY_PREVIOUS_END_SESSION_TIME, timestamp)
                            .commit();
                }
                closeSession();
            }
        });
        // Queue up upload events 16 seconds later
        logThread.removeCallbacks(endSessionRunnable);
        endSessionRunnable = new Runnable() {
            @Override
            public void run() {
                clearEndSession();
                uploadEvents();
            }
        };
        logThread
                .postDelayed(endSessionRunnable, Constants.MIN_TIME_BETWEEN_SESSIONS_MILLIS + 1000);
    }

    public static void logRevenue(double amount) {
        // Amount is in dollars
        // ex. $3.99 would be pass as logRevenue(3.99)
        logRevenue(null, 1, amount);
    }

    public static void logRevenue(String productId, int quantity, double price) {
        logRevenue(productId, quantity, price, null, null);
    }

    public static void logRevenue(String productId, int quantity, double price, String receipt,
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
        checkedLogEvent(REVENUE_EVENT, null, apiProperties, System.currentTimeMillis(), true);
    }

    public static void setUserProperties(JSONObject userProperties) {
        Amplitude.userProperties = userProperties;
    }

    public static void setUserId(String userId) {
        if (!contextAndApiKeySet("setUserId()")) {
            return;
        }

        Amplitude.userId = userId;
        SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
                Context.MODE_PRIVATE);
        preferences.edit().putString(Constants.PREFKEY_USER_ID, userId).commit();
    }

    private static void updateServer() {
        updateServer(true);
    }

    // Always call this from logThread
    private static void updateServer(boolean limit) {
        if (!uploadingCurrently.getAndSet(true)) {
            DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
            try {
                long endSessionId = getEndSessionId();
                Pair<Long, JSONArray> pair = dbHelper.getEvents(endSessionId,
                        limit ? Constants.EVENT_UPLOAD_MAX_BATCH_SIZE : -1);
                final long maxId = pair.first;
                final JSONArray events = pair.second;
                httpThread.post(new Runnable() {
                    @Override
                    public void run() {
                        makeEventUploadPostRequest(Constants.EVENT_LOG_URL, events.toString(),
                                maxId);
                    }
                });
            } catch (JSONException e) {
                uploadingCurrently.set(false);
                Log.e(TAG, e.toString());
            }
        }
    }

    private static void makeEventUploadPostRequest(String url, String events, final long maxId) {
        HttpPost postRequest = new HttpPost(url);
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();

        String apiVersionString = "" + Constants.API_VERSION;
        String timestampString = "" + System.currentTimeMillis();

        String checksumString = "";
        try {
            String preimage = apiVersionString + apiKey + events + timestampString;
            checksumString = bytesToHexString(MessageDigest.getInstance("MD5").digest(
                    preimage.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            // According to
            // http://stackoverflow.com/questions/5049524/is-java-utf-8-charset-exception-possible,
            // this will never be thrown
            Log.e(TAG, e.toString());
        } catch (UnsupportedEncodingException e) {
            // According to
            // http://stackoverflow.com/questions/5049524/is-java-utf-8-charset-exception-possible,
            // this will never be thrown
            Log.e(TAG, e.toString());
        }

        postParams.add(new BasicNameValuePair("v", apiVersionString));
        postParams.add(new BasicNameValuePair("client", apiKey));
        postParams.add(new BasicNameValuePair("e", events));
        postParams.add(new BasicNameValuePair("upload_time", timestampString));
        postParams.add(new BasicNameValuePair("checksum", checksumString));

        try {
            postRequest.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));
        } catch (UnsupportedEncodingException e) {
            // According to
            // http://stackoverflow.com/questions/5049524/is-java-utf-8-charset-exception-possible,
            // this will never be thrown
            Log.e(TAG, e.toString());
        }

        boolean uploadSuccess = false;
        HttpClient client = new DefaultHttpClient();
        try {
            HttpResponse response = client.execute(postRequest);
            String stringResponse = EntityUtils.toString(response.getEntity());
            if (stringResponse.equals("success")) {
                uploadSuccess = true;
                logThread.post(new Runnable() {
                    @Override
                    public void run() {
                        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
                        dbHelper.removeEvents(maxId);
                        uploadingCurrently.set(false);
                        if (dbHelper.getEventCount() > Constants.EVENT_UPLOAD_THRESHOLD) {
                            logThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    updateServer(false);
                                }
                            });
                        }
                    }
                });
            } else if (stringResponse.equals("invalid_api_key")) {
                Log.e(TAG, "Invalid API key, make sure your API key is correct in initialize()");
            } else if (stringResponse.equals("bad_checksum")) {
                Log.w(TAG,
                        "Bad checksum, post request was mangled in transit, will attempt to reupload later");
            } else if (stringResponse.equals("request_db_write_failed")) {
                Log.w(TAG,
                        "Couldn't write to request database on server, will attempt to reupload later");
            } else {
                Log.w(TAG, "Upload failed, " + stringResponse + ", will attempt to reupload later");
            }
        } catch (org.apache.http.conn.HttpHostConnectException e) {
            // Log.w(TAG,
            // "No internet connection found, unable to upload events");
        } catch (java.net.UnknownHostException e) {
            // Log.w(TAG,
            // "No internet connection found, unable to upload events");
        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (AssertionError e) {
            // This can be caused by a NoSuchAlgorithmException thrown by DefaultHttpClient
            Log.e(TAG, "Exception:", e);
        } catch (Exception e) {
            // Just log any other exception so things don't crash on upload
            Log.e(TAG, "Exception:", e);
        } finally {
            if (client.getConnectionManager() != null) {
                client.getConnectionManager().shutdown();
            }
        }

        if (!uploadSuccess) {
            uploadingCurrently.set(false);
        }

    }

    /**
     * @return A unique identifier for tracking within the analytics system. Can be null if deviceId
     *         hasn't been initialized yet;
     */
    public static String getDeviceId() {
        return deviceId;
    }

    private static String initializeDeviceId() {
        Set<String> invalidIds = new HashSet<String>();
        invalidIds.add("");
        invalidIds.add("9774d56d682e549c");
        invalidIds.add("unknown");
        invalidIds.add("000000000000000"); // Common Serial Number
        invalidIds.add("Android");
        invalidIds.add("DEFACE");

        SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
                Context.MODE_PRIVATE);
        String deviceId = preferences.getString(Constants.PREFKEY_DEVICE_ID, null);
        if (!(TextUtils.isEmpty(deviceId) || invalidIds.contains(deviceId))) {
            return deviceId;
        }

        if (!newDeviceIdPerInstall && useAdvertisingIdForDeviceId) {
            // Android ID is deprecated by Google.
            // We are required to use Advertising ID, and respect the advertising ID preference

            String advertisingId = deviceInfo.getAdvertisingId();
            if (!(TextUtils.isEmpty(advertisingId) || invalidIds.contains(advertisingId))) {
                preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, advertisingId).commit();
                return advertisingId;
            }
        }

        // If this still fails, generate random identifier that does not persist
        // across installations. Append R to distinguish as randomly generated
        String randomId = deviceInfo.generateUUID() + "R";
        preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, randomId).commit();
        return randomId;

    }

    private static Object replaceWithJSONNull(Object obj) {
        return obj == null ? JSONObject.NULL : obj;
    }

    private synchronized static boolean contextAndApiKeySet(String methodName) {
        if (context == null) {
            Log.e(TAG, "context cannot be null, set context with initialize() before calling "
                    + methodName);
            return false;
        }
        if (TextUtils.isEmpty(apiKey)) {
            Log.e(TAG,
                    "apiKey cannot be null or empty, set apiKey with initialize() before calling "
                            + methodName);
            return false;
        }
        return true;
    }

    private static String getSharedPreferencesName() {
        return Constants.SHARED_PREFERENCES_NAME_PREFIX + "." + context.getPackageName();
    }

    private static String bytesToHexString(byte[] bytes) {
        final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c',
                'd', 'e', 'f' };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
