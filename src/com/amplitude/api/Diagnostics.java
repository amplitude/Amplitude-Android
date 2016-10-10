package com.amplitude.api;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by danieljih on 10/10/16.
 */
public class Diagnostics {

    public static final int DIAGNOSTIC_EVENT_MAX_COUNT = 50; // limit memory footprint

    public static final String LAST_DIAGNOSTIC_EVENT_ID = "last_diagnostic_event_id";

    private volatile boolean enabled;
    private int diagnosticEventMaxCount;
    long lastDiagnosticEventId;
    private DatabaseHelper dbHelper;
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

    Diagnostics setEnabled(boolean enabled) {
        this.enabled = enabled;
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
                    dbHelper.addDiagnosticEvent(event.toString());
                    if (dbHelper.getDiagnosticEventCount() > diagnosticEventMaxCount) {
                        dbHelper.removeDiagnosticEvents(dbHelper.getNthDiagnosticEventId(5));
                    }
                } catch (JSONException e) {}
            }
        });

        return this;
    }

    Diagnostics uploadDiagnosticEvents() {
        if (!enabled) {
            return this;
        }

        runOnBgThread(new Runnable() {
            @Override
            public void run() {
                if (dbHelper.getDiagnosticEventCount() == 0) {
                    return;
                }

                try {
                    List<JSONObject> events = dbHelper.getDiagnosticEvents(
                        lastDiagnosticEventId, diagnosticEventMaxCount
                    );
                } catch (JSONException e) {}
            }
        });

        return this;
    }


    protected void runOnBgThread(Runnable r) {
        if (Thread.currentThread() != diagnosticThread) {
            diagnosticThread.post(r);
        } else {
            r.run();
        }
    }
}
