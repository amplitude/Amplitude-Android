package com.amplitude.api;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * <h1>PinnedAmplitudeClient</h1>
 * This is a version of the AmplitudeClient that supports SSL pinning for encrypted requests.
 * Please contact <a href="mailto:support@amplitude.com">Amplitude Support</a> before you ship any
 * products with SSL pinning enabled so that we are aware and can provide documentation
 * and implementation help.
 */
public class PinnedAmplitudeClient extends AmplitudeClient {

    static Map<String, PinnedAmplitudeClient> instances = new HashMap<String, PinnedAmplitudeClient>();

    /**
     * Gets the default instance.
     *
     * @return the default instance
     */
    public static PinnedAmplitudeClient getInstance() {
        return getInstance(null);
    }

    /**
     * Gets the specified instance. If instance is null or empty string, fetches the default
     * instance instead.
     *
     * @param instance name to get "ex app 1"
     * @return the specified instance
     */
    public static synchronized PinnedAmplitudeClient getInstance(String instance) {
        instance = Utils.normalizeInstanceName(instance);
        PinnedAmplitudeClient client = instances.get(instance);
        if (client == null) {
            client = new PinnedAmplitudeClient(instance);
            instances.put(instance, client);
        }
        return client;
    }

    /**
     * Instantiates a new Pinned amplitude client.
     */
    public PinnedAmplitudeClient(String instance) {
        super(instance);
    }

    public synchronized AmplitudeClient initializeInternal(
            Context context,
            String apiKey,
            String userId
    ) {
        super.initialize(context, apiKey, userId);
        return this;
    }

    public synchronized AmplitudeClient initialize(
            Context context,
            String apiKey,
            String userId) {
        return initializeInternal(context, apiKey, userId);
    }

    protected HttpService initHttpService() {
        return new HttpService(apiKey, url, bearerToken, this.getRequestListener(), true);
    }

}
