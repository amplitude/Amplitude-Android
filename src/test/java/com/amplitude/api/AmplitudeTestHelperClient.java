package com.amplitude.api;

import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;

import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import java.io.IOException;
import java.net.HttpURLConnection;

// override getCurrentTimeMillis to enforce time progression in tests
class AmplitudeTestHelperClient extends AmplitudeClient {
    private final BaseTest baseTest;
    BaseTest.MockClock mockClock;

    HttpService.RequestListener requestListener = null;

    public AmplitudeTestHelperClient(BaseTest baseTest, BaseTest.MockClock mockClock) {
        this.baseTest = baseTest;
        this.mockClock = mockClock;
    }

    @Override
    protected long getCurrentTimeMillis() {
        return mockClock.currentTimeMillis();
    }

    @Override
    public synchronized AmplitudeClient initializeInternal(
            final Context context,
            final String apiKey,
            final String userId,
            final String platform,
            final boolean enableDiagnosticLogging
    ) {
        super.initializeInternal(context, apiKey, userId, platform, enableDiagnosticLogging);
        ShadowLooper looper = Shadows.shadowOf(baseTest.amplitude.logThread.getLooper());
        looper.runToEndOfTasks();
        //re-initialize httpService because we want to pass a custom request listener into this
        baseTest.amplitude.httpService = baseTest.amplitude.initHttpService();
        try {
            HttpClient origClient = baseTest.amplitude.httpService.messageHandler.httpClient;
            if (!(new MockUtil().isMock(origClient))) {
                HttpClient spyClient = Mockito.spy(origClient);

                //mock server responses, needed for tests enqueueing a series of responses
                Mockito.when(spyClient.getNewConnection(baseTest.amplitude.url)).thenAnswer(new Answer<HttpURLConnection>() {
                    @Override
                    public HttpURLConnection answer(InvocationOnMock invocation) throws Throwable {
                        HttpURLConnection conn = Mockito.spy(baseTest.server.getNextResponse());
                        return conn;
                    }
                });

                //send a record of the request call containing the events string
                //to be inspected later in tests
                Mockito.doAnswer(new Answer<HttpResponse>() {
                    @Override
                    public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                        String eventsSent = invocation.getArgumentAt(0, String.class);
                        baseTest.server.sendRequest(new RecordedRequest(eventsSent));
                        return (HttpResponse) invocation.callRealMethod();
                    }
                }).when(spyClient).makeRequest(Mockito.anyString());

                baseTest.amplitude.httpService.messageHandler.httpClient = spyClient;
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
