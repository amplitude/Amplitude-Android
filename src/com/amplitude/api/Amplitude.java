package com.amplitude.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

public class Amplitude {

  public static final String TAG = "com.amplitude.api.Amplitude";

  private static Context context;
  private static String apiKey;
  private static String userId;
  private static String deviceId;

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
  private static boolean sessionStarted = false;
  private static Runnable setSessionIdRunnable;

  private static AtomicBoolean updateScheduled = new AtomicBoolean(false);
  private static AtomicBoolean uploadingCurrently = new AtomicBoolean(false);

  private Amplitude() {
  }

  public static void initialize(Context context, String apiKey) {
    initialize(context, apiKey, null);
  }

  public static void initialize(Context context, String apiKey, String userId) {
    if (context == null) {
      Log.e(TAG, "Argument context cannot be null in initialize()");
      return;
    }
    if (TextUtils.isEmpty(apiKey)) {
      Log.e(TAG, "Argument apiKey cannot be null or blank in initialize()");
      return;
    }

    Amplitude.context = context.getApplicationContext();
    Amplitude.apiKey = apiKey;
    SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
        Context.MODE_PRIVATE);
    if (userId != null) {
      Amplitude.userId = userId;
      preferences.edit().putString(Constants.PREFKEY_USER_ID, userId).commit();
    } else {
      Amplitude.userId = preferences.getString(Constants.PREFKEY_USER_ID, null);
    }
    Amplitude.deviceId = getDeviceId();

    PackageInfo packageInfo;
    try {
      packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      versionCode = packageInfo.versionCode;
      versionName = packageInfo.versionName;
    } catch (NameNotFoundException e) {
    }
    buildVersionSdk = Build.VERSION.SDK_INT;
    buildVersionRelease = Build.VERSION.RELEASE;
    phoneBrand = Build.BRAND;
    phoneManufacturer = Build.MANUFACTURER;
    phoneModel = Build.MODEL;
    TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    phoneCarrier = manager.getNetworkOperatorName();
    country = Locale.getDefault().getDisplayCountry();
    language = Locale.getDefault().getDisplayLanguage();

  }

  public static void logEvent(String eventType) {
    logEvent(eventType, null);
  }

  public static void logEvent(String eventType, JSONObject eventProperties) {
    logEvent(eventType, eventProperties, null);
  }

  private static void logEvent(String eventType, JSONObject eventProperties,
      JSONObject apiProperties) {
    logEvent(eventType, eventProperties, apiProperties, false, 0);
  }
  
  private static void logEvent(String eventType, JSONObject eventProperties,
      JSONObject apiProperties, boolean useTimestamp, long timestamp) {
    if (TextUtils.isEmpty(eventType)) {
      Log.e(TAG, "Argument eventType cannot be null or blank in logEvent()");
      return;
    }
    if (!contextAndApiKeySet("logEvent()")) {
      return;
    }

    if (!useTimestamp) {
      timestamp = System.currentTimeMillis();
    }

    final JSONObject event = new JSONObject();
    try {
      event.put("event_type", replaceWithJSONNull(eventType));
      event.put("custom_properties", (eventProperties == null) ? new JSONObject() : eventProperties);
      event.put("api_properties", (apiProperties == null) ? new JSONObject() : apiProperties);
      event.put("global_properties", (userProperties == null) ? new JSONObject() : userProperties);
      addBoilerplate(event, timestamp);
    } catch (JSONException e) {
      Log.e(TAG, e.toString());
    }

    DatabaseThread.post(new Runnable() {
      public void run() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.addEvent(event.toString());

        if (dbHelper.getNumberRows() >= Constants.EVENT_MAX_COUNT) {
          dbHelper.removeEvents(dbHelper.getNthEventId(Constants.EVENT_REMOVE_BATCH_SIZE));
        }

        if (dbHelper.getNumberRows() >= Constants.EVENT_UPLOAD_THRESHOLD) {
          updateServer();
        } else {
          updateServerLater();
        }
      }
    });
  }

  private static void addBoilerplate(JSONObject event, long timestamp) throws JSONException {
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

    JSONObject apiProperties = event.getJSONObject("api_properties");
    Location location = getMostRecentLocation();
    if (location != null) {
      JSONObject JSONLocation = new JSONObject();
      JSONLocation.put("lat", location.getLatitude());
      JSONLocation.put("lng", location.getLongitude());
      apiProperties.put("location", JSONLocation);
    }

    if (sessionStarted) {
      refreshSessionTime();
    }
  }

  public static void uploadEvents() {
    if (!contextAndApiKeySet("uploadEvents()")) {
      return;
    }

    DatabaseThread.post(new Runnable() {
      public void run() {
        updateServer();
      }
    });
  }

  private static void updateServerLater() {
    if (!updateScheduled.getAndSet(true)) {

      DatabaseThread.postDelayed(new Runnable() {
        public void run() {
          updateScheduled.set(false);
          updateServer();
        }
      }, Constants.EVENT_UPLOAD_PERIOD_MILLIS);
    }
  }

  public static void startSession() {
    if (!contextAndApiKeySet("startSession()")) {
      return;
    }

    // Remove setSessionId callback
    DatabaseThread.removeCallbacks(setSessionIdRunnable);
    long now = 0;
    boolean useTimestamp = false;

    if (!sessionStarted) {
      // Session has not been started yet, check overlap

      now = System.currentTimeMillis();
      useTimestamp = true;

      SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
          Context.MODE_PRIVATE);
      long previousSessionTime = preferences.getLong(Constants.PREFKEY_PREVIOUS_SESSION_TIME, -1);

      if (now - previousSessionTime < Constants.MIN_TIME_BETWEEN_SESSIONS_MILLIS) {
        // Sessions close enough, set sessionId to previous sessionId
        sessionId = preferences.getLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, now);
      } else {
        // Sessions not close enough, create new sessionId
        sessionId = now;
        preferences.edit().putLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, sessionId).commit();
      }

      // Session now started
      sessionStarted = true;
    }

    // Log session start in events
    JSONObject apiProperties = new JSONObject();
    try {
      apiProperties.put("special", "session_start");
    } catch (JSONException e) {
    }
    logEvent("session_start", null, apiProperties, useTimestamp, now);

    uploadEvents();
  }

  public static void endSession() {
    if (!contextAndApiKeySet("endSession()")) {
      return;
    }

    // Log session end in events
    JSONObject apiProperties = new JSONObject();
    try {
      apiProperties.put("special", "session_end");
    } catch (JSONException e) {
    }
    logEvent("session_end", null, apiProperties);

    // Session stopped
    sessionStarted = false;
    turnOffSessionLater();

    uploadEvents();
  }

  private static void refreshSessionTime() {
    long now = System.currentTimeMillis();
    SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
        Context.MODE_PRIVATE);
    preferences.edit().putLong(Constants.PREFKEY_PREVIOUS_SESSION_TIME, now).commit();
  }

  private static void turnOffSessionLater() {
    setSessionIdRunnable = new Runnable() {
      public void run() {
        if (!sessionStarted) {
          sessionId = -1;
        }
      }
    };
    DatabaseThread.postDelayed(setSessionIdRunnable, Constants.MIN_TIME_BETWEEN_SESSIONS_MILLIS);
  }

  public static void logRevenue(double amount) {
    // Amount is in dollars
    // ex. $3.99 would be pass as logRevenue(3.99)
    if (!contextAndApiKeySet("logRevenue()")) {
      return;
    }

    // Log revenue in events
    JSONObject apiProperties = new JSONObject();
    try {
      apiProperties.put("special", "revenue_amount");
      apiProperties.put("revenue", amount);
    } catch (JSONException e) {
    }
    logEvent("revenue_amount", null, apiProperties);
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

  // Always call this from DatabaseThread
  private static void updateServer(boolean limit) {
    if (!uploadingCurrently.getAndSet(true)) {
      DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
      try {
        Pair<Long, JSONArray> pair = dbHelper.getEvents(limit);
        final long maxId = pair.first;
        final JSONArray events = pair.second;
        HTTPThread.post(new Runnable() {
          public void run() {
            makeEventUploadPostRequest(Constants.EVENT_LOG_URL, events.toString(), maxId);
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
        DatabaseThread.post(new Runnable() {
          public void run() {
            DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
            dbHelper.removeEvents(maxId);
            uploadingCurrently.set(false);
            if (dbHelper.getNumberRows() > Constants.EVENT_UPLOAD_THRESHOLD) {
              DatabaseThread.post(new Runnable() {
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
        Log.w(TAG, "Couldn't write to request database on server, will attempt to reupload later");
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
    } finally {
        if (client.getConnectionManager() != null) {
            client.getConnectionManager().shutdown();
        }
    }

    if (!uploadSuccess) {
      uploadingCurrently.set(false);
    }

  }

  // Returns a unique identifier for tracking within the analytics system
  private static String getDeviceId() {
    Set<String> invalidIds = new HashSet<String>();
    invalidIds.add("");
    invalidIds.add("9774d56d682e549c");
    invalidIds.add("unknown");

    SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesName(),
        Context.MODE_PRIVATE);
    String deviceId = preferences.getString(Constants.PREFKEY_DEVICE_ID, null);
    if (!(TextUtils.isEmpty(deviceId) || invalidIds.contains(deviceId))) {
      return deviceId;
    }

    // Android ID
    // Issues on 2.2, some phones have same Android ID due to manufacturer error
    String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(),
        android.provider.Settings.Secure.ANDROID_ID);
    if (!(TextUtils.isEmpty(androidId) || invalidIds.contains(androidId))) {
      preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, androidId).commit();
      return androidId;
    }

    // Serial number
    // Guaranteed to be on all non phones in 2.3+
    try {
      String serialNumber = (String) Build.class.getField("SERIAL").get(null);
      if (!(TextUtils.isEmpty(serialNumber) || invalidIds.contains(serialNumber))) {
        preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, serialNumber).commit();
        return serialNumber;
      }
    } catch (Exception e) {
    }

    // Telephony ID
    // Guaranteed to be on all phones, requires READ_PHONE_STATE permission
    if (permissionGranted(Constants.PERMISSION_READ_PHONE_STATE)
        && context.getPackageManager().hasSystemFeature("android.hardware.telephony")) {
      String telephonyId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
      if (!(TextUtils.isEmpty(telephonyId) || invalidIds.contains(telephonyId))) {
        preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, telephonyId).commit();
        return telephonyId;
      }
    }

    // If this still fails, generate random identifier that does not persist
    // across installations
    String randomId = UUID.randomUUID().toString();
    preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, randomId).commit();
    return randomId;

  }

  private static Location getMostRecentLocation() {
    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    List<String> providers = locationManager.getProviders(true);
    List<Location> locations = new ArrayList<Location>();
    for (String provider : providers) {
      Location location = locationManager.getLastKnownLocation(provider);
      if (location != null) {
        locations.add(location);
      }
    }

    long maximumTimestamp = -1;
    Location bestLocation = null;
    for (Location location : locations) {
      if (location.getTime() > maximumTimestamp) {
        maximumTimestamp = location.getTime();
        bestLocation = location;
      }
    }

    return bestLocation;
  }

  private static boolean permissionGranted(String permission) {
    return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
  }

  private static Object replaceWithJSONNull(Object obj) {
    return obj == null ? JSONObject.NULL : obj;
  }

  private static boolean contextAndApiKeySet(String methodName) {
    if (context == null) {
      Log.e(TAG, "context cannot be null, set context with initialize() before calling "
          + methodName);
      return false;
    }
    if (TextUtils.isEmpty(apiKey)) {
      Log.e(TAG, "apiKey cannot be null or empty, set apiKey with initialize() before calling "
          + methodName);
      return false;
    }
    return true;
  }

  private static String getSharedPreferencesName() {
    return Constants.SHARED_PREFERENCES_NAME_PREFIX + "." + context.getPackageName();
  }

  public static String bytesToHexString(byte[] bytes) {
    final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
        'e', 'f' };
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
