package com.amplitude.api;

public class Constants {

    public static final String LIBRARY = "amplitude-android";
    public static final String PLATFORM = "Android";
    public static final String VERSION = "2.5.1";

    public static final String EVENT_LOG_URL = "https://api.amplitude.com/";

    public static final String PACKAGE_NAME = "com.amplitude.api";

    public static final int API_VERSION = 2;

    public static final String DATABASE_NAME = PACKAGE_NAME;
    public static final int DATABASE_VERSION = 3;

    public static final int EVENT_UPLOAD_THRESHOLD = 30;
    public static final int EVENT_UPLOAD_MAX_BATCH_SIZE = 100;
    public static final int EVENT_MAX_COUNT = 1000;
    public static final int EVENT_REMOVE_BATCH_SIZE = 20;
    public static final long EVENT_UPLOAD_PERIOD_MILLIS = 30 * 1000; // 30s
    public static final long MIN_TIME_BETWEEN_SESSIONS_MILLIS = 5 * 60 * 1000; // 5m
    public static final long SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000; // 30m
    public static final int MAX_STRING_LENGTH = 1024;

    public static final String SHARED_PREFERENCES_NAME_PREFIX = PACKAGE_NAME;
    public static final String PREFKEY_LAST_EVENT_ID = PACKAGE_NAME + ".lastEventId";
    public static final String PREFKEY_LAST_EVENT_TIME = PACKAGE_NAME + ".lastEventTime";
    public static final String PREFKEY_LAST_IDENTIFY_ID = PACKAGE_NAME + ".lastIdentifyId";
    public static final String PREFKEY_PREVIOUS_SESSION_ID = PACKAGE_NAME + ".previousSessionId";
    public static final String PREFKEY_DEVICE_ID = PACKAGE_NAME + ".deviceId";
    public static final String PREFKEY_USER_ID = PACKAGE_NAME + ".userId";
    public static final String PREFKEY_OPT_OUT = PACKAGE_NAME + ".optOut";

    public static final String IDENTIFY_EVENT = "$identify";
    public static final String AMP_OP_ADD = "$add";
    public static final String AMP_OP_APPEND = "$append";
    public static final String AMP_OP_CLEAR_ALL = "$clearAll";
    public static final String AMP_OP_SET = "$set";
    public static final String AMP_OP_SET_ONCE = "$setOnce";
    public static final String AMP_OP_UNSET = "$unset";
}
