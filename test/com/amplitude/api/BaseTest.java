package com.amplitude.api;

import android.content.Context;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.fail;

public class BaseTest {

    protected class MockClock {
        int index = 0;
        long timestamps [];

        public void setTimestamps(long [] timestamps) {
            this.timestamps = timestamps;
        }

        public long currentTimeMillis() {
            if (timestamps == null || index >= timestamps.length) {
                return System.currentTimeMillis();
            }
            return timestamps[index++];
        }
    }

    // override getCurrentTimeMillis to enforce time progression in tests
    protected class AmplitudeClientWithTime extends AmplitudeClient {
        MockClock mockClock;

        public AmplitudeClientWithTime(MockClock mockClock) { this.mockClock = mockClock; }

        @Override
        protected long getCurrentTimeMillis() { return mockClock.currentTimeMillis(); }
    }

    protected AmplitudeClient amplitude;
    protected Context context;
    protected MockWebServer server;
    protected MockClock clock;

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

        if (clock == null) {
            clock = new MockClock();
        }

        if (amplitude == null) {
            amplitude = new AmplitudeClientWithTime(clock);
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

    public RecordedRequest sendIdentify(AmplitudeClient amplitude, Identify identify) {
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        amplitude.identify(identify);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        return runRequest();
    }

    public long getUnsentEventCount() {
        return DatabaseHelper.getDatabaseHelper(context).getEventCount();
    }

    public long getUnsentIdentifyCount() {
        return DatabaseHelper.getDatabaseHelper(context).getIdentifyCount();
    }


    public JSONObject getLastUnsentEvent() {
        JSONArray events = getUnsentEventsFromTable(DatabaseHelper.EVENT_TABLE_NAME, 1);
        return (JSONObject)events.opt(events.length() - 1);
    }

    public JSONObject getLastUnsentIdentify() {
        JSONArray events = getUnsentEventsFromTable(DatabaseHelper.IDENTIFY_TABLE_NAME, 1);
        return (JSONObject)events.opt(events.length() - 1);
    }

    public JSONArray getUnsentEvents(int limit) {
        return getUnsentEventsFromTable(DatabaseHelper.EVENT_TABLE_NAME, limit);
    }

    public JSONArray getUnsentIdentifys(int limit) {
        return getUnsentEventsFromTable(DatabaseHelper.IDENTIFY_TABLE_NAME, limit);
    }

    public JSONArray getUnsentEventsFromTable(String table, int limit) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
            List<JSONObject> events = table.equals(DatabaseHelper.IDENTIFY_TABLE_NAME) ?
                    dbHelper.getIdentifys(-1, -1) : dbHelper.getEvents(-1, -1);

            JSONArray out = new JSONArray();
            int start = Math.max(limit - events.size(), 0);
            for (int i = start; i < limit; i++) {
                out.put(i, events.get(events.size() - limit + i));
            }
            return out;
        } catch (JSONException e) {
            fail(e.toString());
        }

        return null;
    }

    public JSONObject getLastEvent() {
        return getLastEventFromTable(DatabaseHelper.EVENT_TABLE_NAME);
    }

    public JSONObject getLastIdentify() {
        return getLastEventFromTable(DatabaseHelper.IDENTIFY_TABLE_NAME);
    }

    public JSONObject getLastEventFromTable(String table) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
            List<JSONObject> events = table.equals(DatabaseHelper.IDENTIFY_TABLE_NAME) ?
                    dbHelper.getIdentifys(-1, -1) : dbHelper.getEvents(-1, -1);
            return events.get(events.size() - 1);
        } catch (JSONException e) {
            fail(e.toString());
        }
        return null;
    }

    public boolean compareJSONObjects(JSONObject o1, JSONObject o2) throws JSONException {
        if (o1 == o2) {
            return true;
        }

        if ((o1 != null && o2 == null) || (o1 == null && o2 != null)) {
            return false;
        }

        if (o1.length() != o2.length()) {
            return false;
        }

        Iterator<?> keys = o1.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (!o2.has(key)) {
                return false;
            }

            Object value1 = o1.get(key);
            Object value2 = o2.get(key);

            if (!value1.getClass().equals(value2.getClass())) {
                return false;
            }

            if (value1.getClass() == JSONObject.class) {
                if (!compareJSONObjects((JSONObject) value1, (JSONObject) value2)) {
                    return false;
                }
            } else if (!value1.equals(value2)) {
                return false;
            }
        }

        return true;
    }

    public JSONArray getEventsFromRequest(RecordedRequest request) throws JSONException {
        Map<String, String> parsedBody = parseRequest(request.getUtf8Body());
        if (parsedBody == null && !parsedBody.containsKey("e")) {
            return null;
        }
        return new JSONArray(parsedBody.get("e"));
    }

    // parse request string into a key:value map
    public static Map<String, String> parseRequest(String request) {
        try {
            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            String[] pairs = request.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
            return query_pairs;
        } catch (UnsupportedEncodingException e) {
            fail(e.toString());
        }
        return null;
    }
}
