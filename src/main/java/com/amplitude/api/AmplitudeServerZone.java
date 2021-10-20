package com.amplitude.api;

import java.util.HashMap;
import java.util.Map;

public enum AmplitudeServerZone {
    US, EU;

    private static Map<AmplitudeServerZone, String> amplitudeServerZoneEventLogApiMap =
        new HashMap<AmplitudeServerZone, String>() {{
            put(AmplitudeServerZone.US, Constants.EVENT_LOG_URL);
            put(AmplitudeServerZone.EU, Constants.EVENT_LOG_EU_URL);
        }};

    private static Map<AmplitudeServerZone, String> amplitudeServerZoneDynamicConfigMap =
        new HashMap<AmplitudeServerZone, String>() {{
            put(AmplitudeServerZone.US, Constants.DYNAMIC_CONFIG_URL);
            put(AmplitudeServerZone.EU, Constants.DYNAMIC_CONFIG_EU_URL);
        }};


    protected static String getEventLogApiForZone(AmplitudeServerZone serverZone) {
        if (amplitudeServerZoneEventLogApiMap.containsKey(serverZone)) {
            return amplitudeServerZoneEventLogApiMap.get(serverZone);
        }
        return Constants.EVENT_LOG_URL;
    }

    protected static String getDynamicConfigApi(AmplitudeServerZone serverZone) {
        if (amplitudeServerZoneDynamicConfigMap.containsKey(serverZone)) {
            return amplitudeServerZoneDynamicConfigMap.get(serverZone);
        }
        return Constants.DYNAMIC_CONFIG_URL;
    }
}
