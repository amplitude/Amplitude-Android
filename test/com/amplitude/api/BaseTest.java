package com.amplitude.api;

import static org.junit.Assert.fail;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.lang.InterruptedException;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.Shadows;

import android.content.Context;
import android.util.Pair;

public class BaseTest {

    protected AmplitudeClient amplitude;
    protected Context context;
    protected MockWebServer server;

    public void setUp() throws Exception {
        setUp(true);
    }

    /**
     * Handle common test setup for default cases. Specific cases can
     * override the defaults by providing an amplitude object before
     * calling this method or passing false for withServer.
     */
    public void setUp(boolean withServer) throws Exception {
        ShadowApplication.getInstance().setPackageName("com.amplitude.test");
        context = ShadowApplication.getInstance().getApplicationContext();

        // Clear the database helper for each test. Better to have isolation.
        // See https://github.com/robolectric/robolectric/issues/569
        // and https://github.com/robolectric/robolectric/issues/1622
        DatabaseHelper.instance = null;

        if (withServer) {
            server = new MockWebServer();
            server.play();
        }

        if (amplitude == null) {
            amplitude = new AmplitudeClient();
            // this sometimes deadlocks with lock contention by logThread and httpThread for
            // a ShadowWrangler instance and the ShadowLooper class
            // Might be a sign of a bug, or just Robolectric's bug.
            amplitude.initialize(context, "1cc2c1978ebab0f6451112a8f5df4f4e");
        }

        if (server != null) {
            amplitude.url = server.getUrl("/").toString();            
        }
    }

    public void tearDown() throws Exception {
        if (amplitude != null) {
            amplitude.logThread.getLooper().quit();
            amplitude.httpThread.getLooper().quit();
            amplitude = null;
        }

        if (server != null) {
            server.shutdown();
        }

        DatabaseHelper.instance = null;
    }

    public RecordedRequest runRequest() {
        server.enqueue(new MockResponse().setBody("success"));
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httplooper.runToEndOfTasks();

        try {
            return server.takeRequest(1, SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public RecordedRequest sendEvent(AmplitudeClient amplitude, String name, JSONObject props) {
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        amplitude.logEvent(name, props);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        return runRequest();
    }

    public long getUnsentEventCount() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        return dbHelper.getEventCount();
    }


    public JSONObject getLastUnsentEvent() {
        JSONArray events = getUnsentEvents(1);
        return (JSONObject)events.opt(events.length() - 1);
    }

    public JSONArray getUnsentEvents(int limit) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
            Pair<Long, JSONArray> pair = dbHelper.getEvents(-1, -1);

            JSONArray out = new JSONArray();
            int start = Math.max(limit - pair.second.length(), 0);
            for (int i = start; i < limit; i++) {
                out.put(i, pair.second.get(pair.second.length() - limit + i));
            }
            return out;
        } catch (JSONException e) {
            fail(e.toString());
        }

        return null;
    }

    public JSONObject getLastEvent() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
            Pair<Long, JSONArray> pair = dbHelper.getEvents(-1, -1);
            return (JSONObject)pair.second.get(pair.second.length() - 1);
        } catch (JSONException e) {
            fail(e.toString());
        }
        return null;
    }
}
