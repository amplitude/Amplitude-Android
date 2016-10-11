package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DiagnosticsTest extends BaseTest {

    private Diagnostics logger;
    private ShadowLooper looper;
    private DatabaseHelper dbHelper;

    @Before
    public void setUp() throws Exception {
        super.setUp(false);
        logger = Diagnostics.getLogger(context).setEnabled(true);
        looper = Shadows.shadowOf(logger.diagnosticThread.getLooper());
        dbHelper = DatabaseHelper.getDatabaseHelper(context);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Diagnostics.instance = null;
    }

    @Test
    public void testInitNew() {
        looper.runToEndOfTasks();
        assertEquals(logger.lastDiagnosticEventId, 0);
        assertNull(dbHelper.getLongValue(Diagnostics.LAST_DIAGNOSTIC_EVENT_ID));
    }

    @Test
    public void testReinit() {
        dbHelper.insertOrReplaceKeyLongValue(Diagnostics.LAST_DIAGNOSTIC_EVENT_ID, 51L);
        looper.runToEndOfTasks();
        assertEquals(logger.lastDiagnosticEventId, 51);
    }

    @Test
    public void testLogError() throws JSONException {
        long timestamp = System.currentTimeMillis();
        looper.runToEndOfTasks();
        logger.logError("test_error");
        looper.runToEndOfTasks();
        logger.logError("test_error1");
        looper.runToEndOfTasks();
        logger.logError("test_error2");
        looper.runToEndOfTasks();

        assertEquals(dbHelper.getDiagnosticEventCount(), 3);
        List<JSONObject> events = dbHelper.getDiagnosticEvents(-1, -1);
        assertEquals(events.size(), 3);
        assertEquals(events.get(0).optString("error"), "test_error");
        assertTrue(events.get(0).optLong("timestamp") >= timestamp);
        assertEquals(events.get(1).optString("error"), "test_error1");
        assertTrue(events.get(1).optLong("timestamp") >= timestamp);
        assertEquals(events.get(2).optString("error"), "test_error2");
        assertTrue(events.get(2).optLong("timestamp") >= timestamp);

        // test truncation
        logger.setDiagnosticEventMaxCount(7);
        logger.logError("test_error3");
        looper.runToEndOfTasks();
        logger.logError("test_error4");
        looper.runToEndOfTasks();
        logger.logError("test_error5");
        looper.runToEndOfTasks();
        logger.logError("test_error6");
        looper.runToEndOfTasks();
        logger.logError("test_error7");
        looper.runToEndOfTasks();

        // logged 8 events, but removed 5, so 3 left
        assertEquals(dbHelper.getDiagnosticEventCount(), 3);
        events = dbHelper.getDiagnosticEvents(-1, -1);
        assertEquals(events.get(0).optString("error"), "test_error5");
        assertEquals(events.get(1).optString("error"), "test_error6");
        assertEquals(events.get(2).optString("error"), "test_error7");

        assertEquals(logger.lastDiagnosticEventId, 8);
        assertEquals((long) dbHelper.getLongValue(Diagnostics.LAST_DIAGNOSTIC_EVENT_ID), 8L);
    }

    @Test
    public void testDisabled() throws JSONException {
        logger.setEnabled(false);
        looper.runToEndOfTasks();
        logger.logError("test_error");
        looper.runToEndOfTasks();
        logger.logError("test_error1");
        looper.runToEndOfTasks();
        logger.logError("test_error2");
        looper.runToEndOfTasks();

        assertEquals(dbHelper.getDiagnosticEventCount(), 0);
        List<JSONObject> events = dbHelper.getDiagnosticEvents(-1, -1);
        assertEquals(events.size(), 0);
    }
}

