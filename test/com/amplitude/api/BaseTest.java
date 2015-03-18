package com.amplitude.api;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.lang.InterruptedException;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.FakeHttp;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.Shadows;

import android.content.Context;

public class BaseTest {


    protected Amplitude.Lib amplitude;
    protected Context context;
    protected MockWebServer server;

    public void setUp() throws Exception {
        ShadowApplication.getInstance().setPackageName("com.amplitude.test");
        context = ShadowApplication.getInstance().getApplicationContext();

        server = new MockWebServer();
        server.play();
        Amplitude.setUrl(server.getUrl("/"));

        amplitude = new Amplitude.Lib();
        // this sometimes deadlocks with lock contention by logThread and httpThread for
        // a ShadowWrangler instance and the ShadowLooper class
        // Might be a sign of a bug, or just Robolectric's bug.
        amplitude.initialize(context, "1cc2c1978ebab0f6451112a8f5df4f4e");
    }

    public void tearDown() throws Exception {
        amplitude.logThread.getLooper().quit();
        amplitude.httpThread.getLooper().quit();

        server.shutdown();
    }

    public RecordedRequest sendEvent(Amplitude.Lib amplitude, String name, JSONObject props) {
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        amplitude.logEvent(name, props);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        server.enqueue(new MockResponse().setBody("success"));
        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httplooper.runToEndOfTasks();

        try {
            return server.takeRequest(1, SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }
}
