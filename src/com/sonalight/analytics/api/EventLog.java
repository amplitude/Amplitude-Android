package com.sonalight.analytics.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

public class EventLog {

  public static final String TAG = "com.sonalight.analytics.api.EventLog";

  private static Context context;
  private static String ApiKey;
  private static String userId;
  private static String deviceId;

  private static int versionCode;
  private static String versionName;
  private static int buildVersionSdk;
  private static String buildVersionRelease;
  private static String phoneBrand;
  private static String phoneManufacturer;
  private static String phoneModel;

  private static long sessionId = -1;
  private static boolean sessionStarted = false;

  private static boolean updateScheduled = false;

  public static void initialize(Context context, String ApiKey) {
    initialize(context, ApiKey, null);
  }

  public static void initialize(Context context, String ApiKey, String userId) {
    if (context == null) {
      throw new IllegalArgumentException("Context cannot be null");
    }
    if (TextUtils.isEmpty(ApiKey)) {
      throw new IllegalArgumentException("ApiKey cannot be null or blank");
    }

    EventLog.context = context.getApplicationContext();
    EventLog.ApiKey = ApiKey;
    EventLog.userId = userId;
    EventLog.deviceId = getDeviceId();

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
  }

  public static void logEvent(String eventType) {
    logEvent(eventType, null);
  }

  public static void logEvent(String eventType, JSONObject customProperties) {
    logEvent(eventType, customProperties, null);
  }

  private static void logEvent(String eventType, JSONObject customProperties, JSONObject properties) {

    final JSONObject event = new JSONObject();
    try {
      event.put("event_type", (eventType == null) ? JSONObject.NULL : eventType);
      event.put("custom_properties", (customProperties == null) ? new JSONObject()
          : customProperties);
      event.put("properties", (properties == null) ? new JSONObject() : properties);
      addBoilerplate(event);
    } catch (JSONException e) {
      Log.e(TAG, e.toString());
    }

    LogThread.post(new Runnable() {
      public void run() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);

        dbHelper.addEvent(event.toString());

        if (dbHelper.getNumberRows() >= Constants.EVENT_BATCH_SIZE) {
          updateServer();
        } else {
          updateServerLater();
        }
      }
    });
  }

  public static void uploadEvents() {
    LogThread.post(new Runnable() {
      public void run() {
        updateServer();
      }
    });
  }

  public static void startSession() {
    if (!sessionStarted) {
      // Session has not been started yet, check overlap

      long now = System.currentTimeMillis();

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
      long previousSessionTime = preferences.getLong(Constants.PREFKEY_LAST_SESSION_TIME, -1);

      if (now - previousSessionTime < Constants.MIN_TIME_BETWEEN_SESSIONS_MILLIS) {
        // Sessions close enough, set sessionId to previous sessionId
        sessionId = preferences.getLong(Constants.PREFKEY_LAST_SESSION_ID, now);
      } else {
        // Sessions not close enough, create new sessionId
        sessionId = now;
        preferences.edit().putLong(Constants.PREFKEY_LAST_SESSION_ID, sessionId).commit();
      }

      // Session now started
      sessionStarted = true;
    }

    // Log session start in events
    JSONObject properties = new JSONObject();
    try {
      properties.put("special", "session_start");
    } catch (JSONException e) {
    }
    logEvent("session_start", null, properties);
  }

  public static void endSession() {
    // Log session end in events
    JSONObject properties = new JSONObject();
    try {
      properties.put("special", "session_end");
    } catch (JSONException e) {
    }
    logEvent("session_end", null, properties);

    // Session stopped
    sessionStarted = false;
    sessionId = -1;
  }

  private static void refreshSessionTime() {
    long now = System.currentTimeMillis();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    preferences.edit().putLong(Constants.PREFKEY_LAST_SESSION_TIME, now).commit();
  }

  private static void addBoilerplate(JSONObject event) throws JSONException {
    long timestamp = System.currentTimeMillis();
    event.put("timestamp", timestamp);
    event.put("user_id", userId);
    event.put("device_id", deviceId);
    event.put("session_id", sessionId);
    event.put("version_code", versionCode);
    event.put("version_name", versionName);
    event.put("build_version_sdk", buildVersionSdk);
    event.put("build_version_release", buildVersionRelease);
    event.put("phone_brand", phoneBrand);
    event.put("phone_manufacturer", phoneManufacturer);
    event.put("phone_model", phoneModel);

    JSONObject properties = event.getJSONObject("properties");

    Location location = getMostRecentLocation();
    if (location != null) {
      JSONObject JSONLocation = new JSONObject();
      JSONLocation.put("lat", location.getLatitude());
      JSONLocation.put("lng", location.getLongitude());
      properties.put("location", JSONLocation);
    }

    if (sessionStarted) {
      refreshSessionTime();
    }
  }

  private static void updateServer() {
    boolean success = false;
    long maxId = 0;
    DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
    try {
      Pair<Long, JSONArray> pair = dbHelper.getEvents();
      maxId = pair.first;
      JSONArray events = pair.second;

      success = makePostRequest(Constants.EVENT_LOG_URL, events.toString(), events.length());
    } catch (Exception e) {
      Log.e(TAG, e.toString());
    }

    if (success) {
      dbHelper.removeEvents(maxId);
    } else {
      Log.w(TAG, "Upload failed, post request not successful");
    }
  }

  private static void updateServerLater() {
    if (!updateScheduled) {
      updateScheduled = true;

      LogThread.postDelayed(new Runnable() {
        public void run() {
          updateScheduled = false;
          updateServer();
        }
      }, Constants.EVENT_UPLOAD_PERIOD_MILLIS);
    }
  }

  private static boolean makePostRequest(String url, String events, long numEvents)
      throws ClientProtocolException, IOException, JSONException {
    HttpPost postRequest = new HttpPost(url);
    List<NameValuePair> postParams = new ArrayList<NameValuePair>();
    postParams.add(new BasicNameValuePair("e", events));
    postParams.add(new BasicNameValuePair("client", ApiKey));

    postRequest.setEntity(new UrlEncodedFormEntity(postParams));

    HttpClient client = new DefaultHttpClient();
    HttpResponse response = client.execute(postRequest);
    String stringResult = EntityUtils.toString(response.getEntity());

    JSONObject result = new JSONObject(stringResult);

    return result.optLong("added", 0) == numEvents;
  }

  public static void setUserId(String userId) {
    EventLog.userId = userId;
  }

  // Returns a unique identifier for tracking within the analytics system
  private static String getDeviceId() {

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    String deviceId = preferences.getString(Constants.PREFKEY_DEVICE_ID, null);
    if (!TextUtils.isEmpty(deviceId)) {
      return deviceId;
    }

    // Android ID
    // Issues on 2.2, some phones have same Android ID due to manufacturer error
    String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(),
        android.provider.Settings.Secure.ANDROID_ID);
    if (!(TextUtils.isEmpty(androidId) || androidId.equals("9774d56d682e549c"))) {
      preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, androidId).commit();
      return androidId;
    }

    // Serial number
    // Guaranteed to be on all non phones in 2.3+
    try {
      String serialNumber = (String) Build.class.getField("SERIAL").get(null);
      if (!TextUtils.isEmpty(serialNumber)) {
        preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, serialNumber).commit();
        return serialNumber;
      }
    } catch (Exception e) {
    }

    // Telephony ID
    // Guaranteed to be on all phones, requires READ_PHONE_STATE permission
    if (permissionGranted(Constants.PERMISSION_READ_PHONE_STATE)
        && context.getPackageManager().hasSystemFeature("android.hardware.telephony")) {
      String telephonyId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
          .getDeviceId();
      if (!TextUtils.isEmpty(telephonyId)) {
        preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, telephonyId).commit();
        return telephonyId;
      }
    }

    // Account name
    // Requires GET_ACCOUNTS permission
    /*if (permissionGranted(Constants.PERMISSION_GET_ACCOUNTS)) {
      AccountManager accountManager = (AccountManager) context
          .getSystemService(Context.ACCOUNT_SERVICE);
      Account[] accounts = accountManager.getAccountsByType("com.google");
      for (Account account : accounts) {
        String accountName = account.name;
        if (!TextUtils.isEmpty(accountName)) {
          preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, accountName).commit();
          return accountName;
        }
      }
    }*/

    // If this still fails, generate random identifier that does not persist
    // across installations
    String randomId = UUID.randomUUID().toString();
    preferences.edit().putString(Constants.PREFKEY_DEVICE_ID, randomId).commit();
    return randomId;

  }

  private static Location getMostRecentLocation() {
    LocationManager locationManager = (LocationManager) context
        .getSystemService(Context.LOCATION_SERVICE);
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

}
