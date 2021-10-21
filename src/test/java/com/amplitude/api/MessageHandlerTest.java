package com.amplitude.api;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Message;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class MessageHandlerTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testHandleMessage() {
        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();

        MessageHandler msgHandler = amplitude.httpService.messageHandler;

        //nonexistent message type
        int badMessageCode = 128312479;
        Message message = msgHandler.obtainMessage(badMessageCode, null);
        msgHandler.handleMessage(message);
    }

}
