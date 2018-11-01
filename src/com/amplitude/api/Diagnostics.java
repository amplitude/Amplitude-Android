package com.amplitude.api;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by djih on 11/1/18.
 */

public class Diagnostics {

    public static final String DIAGNOSTIC_EVENT_ENDPOINT = "https://api.amplitude.com/diagnostic";

    public static final int DIAGNOSTIC_EVENT_API_VERSION = 1;

    public static final int DIAGNOSTIC_EVENT_MAX_COUNT = 50; // limit memory footprint
    public static final int DIAGNOSTIC_EVENT_MIN_COUNT = 5;

    private volatile boolean enabled;
    private volatile String apiKey;
    private volatile OkHttpClient httpClient;
    private int diagnosticEventMaxCount;
    String url;
    WorkerThread diagnosticThread = new WorkerThread("diagnosticThread");
    private List<JSONObject> unsentEvents;

    protected static Diagnostics instance;

    static synchronized Diagnostics getLogger() {
        if (instance == null) {
            instance = new Diagnostics();
        }
        return instance;
    }

    private Diagnostics() {
        enabled = false;
        diagnosticEventMaxCount = DIAGNOSTIC_EVENT_MAX_COUNT;
        url = DIAGNOSTIC_EVENT_ENDPOINT;
        unsentEvents = new ArrayList<JSONObject>(diagnosticEventMaxCount);
        diagnosticThread.start();
    }

    Diagnostics enableLogging(OkHttpClient httpClient, String apiKey) {
        this.enabled = true;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        return this;
    }

    Diagnostics disableLogging() {
        this.enabled = false;
        return this;
    }

    Diagnostics setDiagnosticEventMaxCount(int diagnosticEventMaxCount) {
        // only allow overrides between 5 and 50
        this.diagnosticEventMaxCount = Math.max(diagnosticEventMaxCount, DIAGNOSTIC_EVENT_MIN_COUNT);
        this.diagnosticEventMaxCount = Math.min(this.diagnosticEventMaxCount, DIAGNOSTIC_EVENT_MAX_COUNT);

        // resize unsent list and transfer any unsent events over
        List<JSONObject> newUnsentEvents = new ArrayList<JSONObject>(this.diagnosticEventMaxCount);
        int toTransfer = Math.min(unsentEvents.size(), this.diagnosticEventMaxCount);
        for (int i = 0; i < toTransfer; i++) {
            newUnsentEvents.add(unsentEvents.get(i));
        }

        this.unsentEvents = newUnsentEvents;
        return this;
    }

    Diagnostics logError(final String error, final String stackTrace) {
        if (!enabled || TextUtils.isEmpty(error)) {
            return this;
        }

        runOnBgThread(new Runnable() {
            @Override
            public void run() {
                JSONObject event = new JSONObject();
                try {
                    event.put("error", error);
                    event.put("timestamp", System.currentTimeMillis());
                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("stackTrace", stackTrace);
                    event.put("event_properties", eventProperties);

                    if (unsentEvents.size() >= diagnosticEventMaxCount) {
                        for (int i = 0; i < DIAGNOSTIC_EVENT_MIN_COUNT; i++) {
                            unsentEvents.remove(0);
                        }
                    }
                    unsentEvents.add(event);
                } catch (JSONException e) {}
            }
        });

        return this;
    }

    // call this manually to upload unsent events
    Diagnostics flushEvents() {
        if (!enabled || TextUtils.isEmpty(apiKey) || httpClient == null) {
            return this;
        }

        runOnBgThread(new Runnable() {
            @Override
            public void run() {
                if (unsentEvents.isEmpty()) {
                    return;
                }
                String eventJson = null;
                eventJson = new JSONArray(unsentEvents).toString();

                if (!TextUtils.isEmpty(eventJson)) {
                    makeEventUploadPostRequest(eventJson);
                }
            }
        });

        return this;
    }

    protected void makeEventUploadPostRequest(String events) {
        FormBody body = new FormBody.Builder()
                .add("v", "" + DIAGNOSTIC_EVENT_API_VERSION)
                .add("client", apiKey)
                .add("e", events)
                .add("upload_time", "" + System.currentTimeMillis())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            String stringResponse = response.body().string();
            if (stringResponse.equals("success")) {
                unsentEvents.clear();
            }
        } catch (IOException e) {
        } catch (AssertionError e) {
        } catch (Exception e) {
        }
    }


    protected void runOnBgThread(Runnable r) {
        if (Thread.currentThread() != diagnosticThread) {
            diagnosticThread.post(r);
        } else {
            r.run();
        }
    }
}