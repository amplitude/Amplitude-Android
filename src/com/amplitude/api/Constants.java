package com.amplitude.api;

public class Constants {

    public static final String LIBRARY = "amplitude-android";
    public static final String PLATFORM = "Android";
    public static final String VERSION = "2.21.0";

    public static final String EVENT_LOG_URL = "https://api.amplitude.com/";

    public static final String PACKAGE_NAME = "com.amplitude.api";

    public static final int API_VERSION = 2;

    public static final String DATABASE_NAME = PACKAGE_NAME;
    public static final int DATABASE_VERSION = 3;

    public static final String DEFAULT_INSTANCE = "$default_instance";

    public static final int EVENT_UPLOAD_THRESHOLD = 30;
    public static final int EVENT_UPLOAD_MAX_BATCH_SIZE = 50;
    public static final int EVENT_MAX_COUNT = 1000;
    public static final int EVENT_REMOVE_BATCH_SIZE = 20;
    public static final long EVENT_UPLOAD_PERIOD_MILLIS = 30 * 1000; // 30s
    public static final long MIN_TIME_BETWEEN_SESSIONS_MILLIS = 5 * 60 * 1000; // 5m
    public static final long SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000; // 30m
    public static final int MAX_STRING_LENGTH = 1024;
    public static final int MAX_PROPERTY_KEYS = 1000;

    public static final String SHARED_PREFERENCES_NAME_PREFIX = PACKAGE_NAME;
    public static final String PREFKEY_LAST_EVENT_ID = PACKAGE_NAME + ".lastEventId";
    public static final String PREFKEY_LAST_EVENT_TIME = PACKAGE_NAME + ".lastEventTime";
    public static final String PREFKEY_LAST_IDENTIFY_ID = PACKAGE_NAME + ".lastIdentifyId";
    public static final String PREFKEY_PREVIOUS_SESSION_ID = PACKAGE_NAME + ".previousSessionId";
    public static final String PREFKEY_DEVICE_ID = PACKAGE_NAME + ".deviceId";
    public static final String PREFKEY_USER_ID = PACKAGE_NAME + ".userId";
    public static final String PREFKEY_OPT_OUT = PACKAGE_NAME + ".optOut";

    public static final String IDENTIFY_EVENT = "$identify";
    public static final String GROUP_IDENTIFY_EVENT = "$groupidentify";
    public static final String AMP_OP_ADD = "$add";
    public static final String AMP_OP_APPEND = "$append";
    public static final String AMP_OP_CLEAR_ALL = "$clearAll";
    public static final String AMP_OP_PREPEND = "$prepend";
    public static final String AMP_OP_SET = "$set";
    public static final String AMP_OP_SET_ONCE = "$setOnce";
    public static final String AMP_OP_UNSET = "$unset";

    public static final String AMP_REVENUE_EVENT = "revenue_amount";
    public static final String AMP_REVENUE_PRODUCT_ID = "$productId";
    public static final String AMP_REVENUE_QUANTITY = "$quantity";
    public static final String AMP_REVENUE_PRICE = "$price";
    public static final String AMP_REVENUE_REVENUE_TYPE = "$revenueType";
    public static final String AMP_REVENUE_RECEIPT = "$receipt";
    public static final String AMP_REVENUE_RECEIPT_SIG = "$receiptSig";

    public static final String AMP_TRACKING_OPTION_ADID = "adid";
    public static final String AMP_TRACKING_OPTION_CARRIER = "carrier";
    public static final String AMP_TRACKING_OPTION_CITY = "city";
    public static final String AMP_TRACKING_OPTION_COUNTRY = "country";
    public static final String AMP_TRACKING_OPTION_DEVICE_BRAND = "device_brand";
    public static final String AMP_TRACKING_OPTION_DEVICE_MANUFACTURER = "device_manufacturer";
    public static final String AMP_TRACKING_OPTION_DEVICE_MODEL = "device_model";
    public static final String AMP_TRACKING_OPTION_DMA = "dma";
    public static final String AMP_TRACKING_OPTION_IP_ADDRESS = "ip_address";
    public static final String AMP_TRACKING_OPTION_LANGUAGE = "language";
    public static final String AMP_TRACKING_OPTION_LAT_LNG = "lat_lng";
    public static final String AMP_TRACKING_OPTION_OS_NAME = "os_name";
    public static final String AMP_TRACKING_OPTION_OS_VERSION = "os_version";
    public static final String AMP_TRACKING_OPTION_PLATFORM = "platform";
    public static final String AMP_TRACKING_OPTION_REGION = "region";
    public static final String AMP_TRACKING_OPTION_VERSION_NAME = "version_name";
}
