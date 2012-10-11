package com.giraffegraph.api;

public class GGConstants {
  public static final String EVENT_LOG_URL = "http://api.giraffegraph.com/";
  public static final String CAMPAIGN_TRACKING_URL = "http://ref.giraffegraph.com/install";

  public static final String PACKAGE_NAME = GGConstants.class.getPackage().getName();

  public static final String DATABASE_NAME = PACKAGE_NAME;
  public static final int DATABASE_VERSION = 1;

  public static final String EVENT_TABLE_NAME = "events";

  public static final String ID_FIELD = "id";
  public static final String EVENT_FIELD = "event";
  public static final String[] TABLE_FIELD_NAMES = { ID_FIELD, EVENT_FIELD };

  public static final int EVENT_BATCH_SIZE = 10;
  // ten seconds
  public static final long EVENT_UPLOAD_PERIOD_MILLIS = 10 * 1000;

  public static final String SHARED_PREFERENCES_NAME_PREFIX = PACKAGE_NAME;
  public static final String PREFKEY_HAS_TRACKED_CAMPAIGN = PACKAGE_NAME + ".hasTrackedCampaign";
  public static final String PREFKEY_CAMPAIGN_INFORMATION = PACKAGE_NAME + ".campaignInformation";
  public static final String PREFKEY_PREVIOUS_SESSION_TIME = PACKAGE_NAME + ".previousSessionTime";
  public static final String PREFKEY_PREVIOUS_SESSION_ID = PACKAGE_NAME + ".previousSessionId";
  public static final String PREFKEY_DEVICE_ID = PACKAGE_NAME + ".deviceId";
  public static final String PREFKEY_USER_ID = PACKAGE_NAME + ".userId";

  // ten seconds
  public static final long MIN_TIME_BETWEEN_SESSIONS_MILLIS = 10 * 1000;

  public static final String PERMISSION_GET_ACCOUNTS = "android.permission.GET_ACCOUNTS";
  public static final String PERMISSION_READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
  public static final String PERMISSION_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
  public static final String PERMISSION_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";

}
