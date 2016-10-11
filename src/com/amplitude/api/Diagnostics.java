package com.amplitude.api;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by danieljih on 10/10/16.
 */
public class Diagnostics {

    public static final String DIAGNOSTIC_EVENT_ENDPOINT = "https://api.amplitude.com/diagnostic";

    public static final int DIAGNOSTIC_EVENT_API_VERSION = 1;

    public static final int DIAGNOSTIC_EVENT_MAX_COUNT = 50; // limit memory footprint

    public static final String LAST_DIAGNOSTIC_EVENT_ID = "last_diagnostic_event_id";

    private volatile boolean enabled;
    private volatile String apiKey;
    private volatile OkHttpClient httpClient;
    private DatabaseHelper dbHelper;
    private int diagnosticEventMaxCount;
    long lastDiagnosticEventId;
    String url;
    WorkerThread diagnosticThread = new WorkerThread("diagnosticThread");

    protected static Diagnostics instance;

    static synchronized Diagnostics getLogger(Context context) {
        if (instance == null) {
            instance = new Diagnostics(context);
        }
        return instance;
    }

    private Diagnostics(Context context) {
        enabled = false;
        diagnosticEventMaxCount = DIAGNOSTIC_EVENT_MAX_COUNT;
        url = DIAGNOSTIC_EVENT_ENDPOINT;
        dbHelper = DatabaseHelper.getDatabaseHelper(context);
        diagnosticThread.start();

        // load diagnostic event meta data
        runOnBgThread(new Runnable() {
            @Override
            public void run() {
                lastDiagnosticEventId = dbHelper.getLongValueSafe(LAST_DIAGNOSTIC_EVENT_ID, 0);
            }
        });
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
        this.diagnosticEventMaxCount = diagnosticEventMaxCount;
        return this;
    }

    Diagnostics logError(final String error) {
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
                    lastDiagnosticEventId = dbHelper.addDiagnosticEvent(event.toString());
                    dbHelper.insertOrReplaceKeyLongValue(
                        LAST_DIAGNOSTIC_EVENT_ID, lastDiagnosticEventId
                    );
                    if (dbHelper.getDiagnosticEventCount() > diagnosticEventMaxCount) {
                        dbHelper.removeDiagnosticEvents(dbHelper.getNthDiagnosticEventId(5));
                    }
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
                if (dbHelper.getDiagnosticEventCount() == 0) {
                    return;
                }
                String eventJson = null;
                try {
                    List<JSONObject> events = dbHelper.getDiagnosticEvents(
                            lastDiagnosticEventId, diagnosticEventMaxCount
                    );
                    if (events == null || events.size() == 0) {
                        return;
                    }
                    eventJson = new JSONArray(events).toString();
                } catch (JSONException e) {
                } catch (RuntimeException e) {  // cursorWindowAllocationException
                } catch (Exception e) {}

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
                dbHelper.removeDiagnosticEvents(lastDiagnosticEventId);
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
