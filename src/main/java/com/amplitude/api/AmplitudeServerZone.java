package com.amplitude.api;

import java.util.HashMap;
import java.util.Map;

/**
 * AmplitudeServerZone is for Data Residency and handling server zone related properties.
 * The server zones now are US and EU.
 *
 * For usage like sending data to Amplitude's EU servers, you need to configure the serverZone
 * property after initializing the client with setServerZone method.
 */
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

    public static AmplitudeServerZone getServerZone(String serverZone) {
        AmplitudeServerZone amplitudeServerZone = AmplitudeServerZone.US;
        switch (serverZone) {
            case "EU":
                amplitudeServerZone = AmplitudeServerZone.EU;
                break;
            case "US":
                amplitudeServerZone = AmplitudeServerZone.US;
                break;
            default:
                break;
        }
        return amplitudeServerZone;
    }
}
