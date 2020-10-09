package com.amplitude.identitymanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdentityManager {
    static final Map<String, Identity> instanceMap = new ConcurrentHashMap<String, Identity>();
    private final static String DEFAULT_INSTANCE = "$default_instance";

    public Identity getInstance(String instanceName) {
        if (instanceMap.containsKey(instanceName)) {
            return instanceMap.get(instanceName);
        } else {
            Identity identityToSet = new Identity();
            instanceMap.put(instanceName, identityToSet);
            return identityToSet;
        }
    }

    public Identity getInstance() {
        return getInstance(DEFAULT_INSTANCE);
    }
}
