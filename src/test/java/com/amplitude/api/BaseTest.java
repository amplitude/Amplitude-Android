package com.amplitude.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownServiceException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

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
    protected class TestingAmplitudeClient extends AmplitudeClient {
        MockClock mockClock;

        HttpService.RequestListener requestListener = null;

        public TestingAmplitudeClient(MockClock mockClock) { this.mockClock = mockClock; }

        @Override
        protected long getCurrentTimeMillis() { return mockClock.currentTimeMillis(); }

        @Override
        public synchronized AmplitudeClient initializeInternal(
                final Context context,
                final String apiKey,
                final String userId,
                final String platform,
                final boolean enableDiagnosticLogging
        ) {
            super.initializeInternal(context, apiKey, userId, platform, enableDiagnosticLogging);
            ShadowLooper looper = shadowOf(amplitude.logThread.getLooper());
            looper.runToEndOfTasks();
            //re-initialize httpService because we want to pass a custom request listener into this
            amplitude.httpService = amplitude.initHttpService();
            try {
                HttpClient origClient = amplitude.httpService.messageHandler.httpClient;
                if (!(new MockUtil().isMock(origClient))) {
                    HttpClient spyClient = Mockito.spy(origClient);

                    //mock server responses, needed for tests enqueueing a series of responses
                    Mockito.when(spyClient.getNewConnection(amplitude.url)).thenAnswer(new Answer<HttpURLConnection>() {
                        @Override
                        public HttpURLConnection answer(InvocationOnMock invocation) throws Throwable {
                            HttpURLConnection conn = Mockito.spy(server.getNextResponse());
                            return conn;
                        }
                    });

                    //send a record of the request call containing the events string
                    //to be inspected later in tests
                    Mockito.doAnswer(new Answer<HttpResponse>() {
                        @Override
                        public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                            String eventsSent = invocation.getArgumentAt(0, String.class);
                            server.sendRequest(new RecordedRequest(eventsSent));
                            return (HttpResponse) invocation.callRealMethod();
                        }
                    }).when(spyClient).getSyncHttpResponse(Mockito.anyString());

                    amplitude.httpService.messageHandler.httpClient = spyClient;
                }
            } catch (IOException e) {
                fail(e.toString());
            }
            return this;
        }

        @Override
        protected HttpService.RequestListener getRequestListener() {
            if (requestListener != null) return requestListener;
            return super.getRequestListener();
        }
    }

    // override AmplitudeDatabaseHelper to throw Cursor Allocation Exception
    protected class MockDatabaseHelper extends DatabaseHelper {

        protected MockDatabaseHelper(Context context) {
            super(context);
        }

        @Override
        Cursor queryDb(
                SQLiteDatabase db, String table, String[] columns, String selection,
                String[] selectionArgs, String groupBy, String having, String orderBy, String limit
        ) {
            // cannot import CursorWindowAllocationException, so we throw the base class instead
            throw new RuntimeException("Cursor window allocation of 2048 kb failed.");
        }
    }

    private static final String TEST_PACKAGE_NAME = "com.amplitude.test";
    private static final String TEST_VERSION_NAME = "test_version";

    protected AmplitudeClient amplitude;
    protected Context context;
    protected MockClock clock;
    protected MockWebServer server;
    protected String apiKey = "1cc2c1978ebab0f6451112a8f5df4f4e";
    protected String[] instanceNames = {Constants.DEFAULT_INSTANCE, "app1", "app2", "newApp1", "newApp2", "new_app"};

    protected PackageManager packageManager;
    protected ShadowPackageManager shadowPackageManager;

    public void setUp() throws Exception {
        setUp(true);
    }

    /**
     * Handle common test setup for default cases. Specific cases can
     * override the defaults by providing an amplitude object before
     * calling this method or passing false for withServer.
     */
    public void setUp(boolean withServer) throws Exception {
        context = ApplicationProvider.getApplicationContext();
        packageManager = RuntimeEnvironment.application.getPackageManager();
        shadowPackageManager = shadowOf(packageManager);

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = TEST_PACKAGE_NAME;

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = TEST_PACKAGE_NAME;
        packageInfo.versionName = TEST_VERSION_NAME;
        packageInfo.applicationInfo = applicationInfo;
        shadowPackageManager.addPackage(packageInfo);

        // Clear the database helper for each test. Better to have isolation.
        // See https://github.com/robolectric/robolectric/issues/569
        // and https://github.com/robolectric/robolectric/issues/1622
        Amplitude.instances.clear();
        DatabaseHelper.instances.clear();

        if (clock == null) {
            clock = new MockClock();
        }

        if (withServer) {
            server = new MockWebServer();
        }

        if (amplitude == null) {
            // this sometimes deadlocks with lock contention by logThread and httpThread for
            // a ShadowWrangler instance and the ShadowLooper class
            // Might be a sign of a bug, or just Robolectric's bug.
            amplitude = new TestingAmplitudeClient(clock);
        }
    }

    public void tearDown() throws Exception {
        if (amplitude != null) {
            amplitude.logThread.getLooper().quit();
            if (amplitude.httpService != null) {
                amplitude.httpService.shutdown();
            }
            amplitude = null;
        }

        Amplitude.instances.clear();
        DatabaseHelper.instances.clear();
    }

    public RecordedRequest runRequest(AmplitudeClient amplitude) {
        try {
            MockHttpUrlConnection response = new MockHttpUrlConnection(amplitude.url).setBody("success");
            server.enqueueResponse(response);

            ShadowLooper httpLooper = shadowOf(amplitude.httpService.getHttpThreadLooper());
            httpLooper.runToEndOfTasks();

            shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

            return server.takeRequest();
        } catch (MalformedURLException e) {
            fail(e.toString());
        }
        return null;
    }

    public RecordedRequest sendEvent(AmplitudeClient amplitude, String name, JSONObject props) {
        shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        amplitude.logEvent(name, props);
        shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        return runRequest(amplitude);
    }

    public RecordedRequest sendIdentify(AmplitudeClient amplitude, Identify identify) {
        shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        amplitude.identify(identify);
        shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();
        shadowOf(amplitude.logThread.getLooper()).runToEndOfTasks();

        return runRequest(amplitude);
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

    public JSONArray getEventsFromRequest(RecordedRequest request) throws JSONException {
        return new JSONArray(request.getBody());
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

    public class RecordedRequest {
        public String body;
        public String getBody() { return body; }
        public RecordedRequest(String body) {
            this.body = body;
        }
    }

    public class MockWebServer {
        private Queue<RecordedRequest> requests;
        private Queue<MockHttpUrlConnection> queue;
        private int numRequestsMade = 0;

        public MockWebServer() {
            this.requests = new LinkedList<>();
            this.queue = new LinkedList<>();
        }

        public void sendRequest(RecordedRequest requesto) {
            requests.add(requesto);
            numRequestsMade++;
        }

        public void enqueueResponse(MockHttpUrlConnection res) { queue.add(res); }
        public MockHttpUrlConnection getNextResponse() { return queue.poll(); }
        public RecordedRequest takeRequest() {
            return requests.poll();
        }

        public int getRequestCount() {
            return numRequestsMade;
        }
    }

    public static class MockHttpUrlConnection extends HttpURLConnection {

        public MockHttpUrlConnection(String str) throws MalformedURLException {
            this(new URL(str));
        }

        protected MockHttpUrlConnection(URL u) {
            super(u);
            this.responseCode = 200;
            this.responseMessage = "";
        }

        public static MockHttpUrlConnection defaultRes() {
            try {
                MockHttpUrlConnection request = new MockHttpUrlConnection(Constants.EVENT_LOG_URL);
                request.setBody("success");
                return request;
            } catch (MalformedURLException e) {
                fail(e.toString());
            }
            return null;
        }

        public MockHttpUrlConnection setBody(String body) {
            responseMessage = body;
            return this;
        }
        public String getBody() {
            return responseMessage;
        }

        public MockHttpUrlConnection setResponseCode(int code) {
            this.responseCode = code;
            return this;
        }

        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }
        public InputStream getInputStream() {
            return new ByteArrayInputStream(this.responseMessage.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void disconnect() {

        }
        @Override
        public boolean usingProxy() {
            return false;
        }
        @Override
        public void connect() throws IOException {

        }
    }

}
