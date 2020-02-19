package com.amplitude.api;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class DatabaseRecoveryTest extends BaseTest {

    protected DatabaseHelper dbInstance;
    protected ShadowLooper looper;
    protected long startTime;

    @Before
    public void setUp() throws Exception {
        super.setUp(false);

        Robolectric.getForegroundThreadScheduler().advanceTo(1);
        startTime = System.currentTimeMillis();

        amplitude.setEventUploadPeriodMillis(10*60*1000);
        amplitude.initialize(context, apiKey, null, null, true);

        looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runOneTask();
        dbInstance = DatabaseHelper.getDatabaseHelper(context);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        DatabaseHelper.instances.put(Constants.DEFAULT_INSTANCE, null);
        dbInstance = null;
    }

    @Test
    public void testRecoverFromDatabaseReset() {

        // log an event normally, verify metadata updated in table
        amplitude.logEvent("test");
        looper.runToEndOfTasks();

        // metadata: deviceId, sessionId, sequence number, last_event_id, last_event_time, previous_session_id
        assertEquals(dbInstance.getEventCount(), 1);
        assertEquals(dbInstance.getTotalEventCount(), 1);
        assertEquals(dbInstance.getNthEventId(1), 1);

        String deviceId = dbInstance.getValue(AmplitudeClient.DEVICE_ID_KEY);
        long previousSessionId = dbInstance.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY);
        long sequenceNumber = dbInstance.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY);
        long lastEventId = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY);
        long lastEventTime = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY);
        long lastIdentifyId = dbInstance.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY);

        assertNotNull(deviceId);
        assertTrue(deviceId.endsWith("R"));
        assertTrue(previousSessionId > 0);
        assertEquals(sequenceNumber, 1);
        assertEquals(lastEventId, 1);
        assertTrue(lastEventTime >= startTime);
        assertEquals(lastIdentifyId, -1);

        // difficult to mock out the SQLiteDatabase object inside DatabaseHelper since it's private
        // add helper method specifically for mocking / testing
        DatabaseHelper mockDbHelper = PowerMockito.spy(dbInstance);
        PowerMockito.doThrow(new SQLiteException("test")).when(mockDbHelper).insertEventContentValuesIntoTable(Matchers.any(SQLiteDatabase.class), anyString(), Matchers.any(ContentValues.class));
        amplitude.dbHelper = mockDbHelper;

        // log an event to trigger SQLException that we set up with mocks
        amplitude.logEvent("test");
        looper.runToEndOfTasks();

        // verify that the metadata has been persisted back into database
        String newDeviceId = dbInstance.getValue(AmplitudeClient.DEVICE_ID_KEY);
        long newPreviousSessionId = dbInstance.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY);
        long newLastEventTime = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY);

        // these do not get persisted and should be reset
        Long newSequenceNumber = dbInstance.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY);
        Long newLastEventId = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY);
        Long newLastIdentifyId = dbInstance.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY);

        assertEquals(newDeviceId, deviceId);
        assertEquals(newPreviousSessionId, previousSessionId);
        assertTrue(newLastEventTime >= lastEventTime);

        assertNull(newSequenceNumber);
        assertEquals(newLastEventId, Long.valueOf(-1));  // insert event fails, and returns -1
        assertNull(newLastIdentifyId);

        reset(mockDbHelper);
    }

    @Test
    public void testDatabaseResetAvoidStackOverflow() {

        // log an event normally, verify metadata updated in table
        amplitude.logEvent("test");
        looper.runToEndOfTasks();

        // metadata: deviceId, sessionId, sequence number, last_event_id, last_event_time, previous_session_id
        assertEquals(dbInstance.getEventCount(), 1);
        assertEquals(dbInstance.getTotalEventCount(), 1);
        assertEquals(dbInstance.getNthEventId(1), 1);

        String deviceId = dbInstance.getValue(AmplitudeClient.DEVICE_ID_KEY);
        long previousSessionId = dbInstance.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY);
        long sequenceNumber = dbInstance.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY);
        long lastEventId = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY);
        long lastEventTime = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY);
        long lastIdentifyId = dbInstance.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY);

        assertNotNull(deviceId);
        assertTrue(deviceId.endsWith("R"));
        assertTrue(previousSessionId > 0);
        assertEquals(sequenceNumber, 1);
        assertEquals(lastEventId, 1);
        assertTrue(lastEventTime >= startTime);
        assertEquals(lastIdentifyId, -1);

        // difficult to mock out the SQLiteDatabase object inside DatabaseHelper since it's private
        // add helper method specifically for mocking / testing
        DatabaseHelper mockDbHelper = PowerMockito.spy(dbInstance);
        PowerMockito.doThrow(new SQLiteException("test")).when(mockDbHelper).insertKeyValueContentValuesIntoTable(Matchers.any(SQLiteDatabase.class), anyString(), Matchers.any(ContentValues.class));
        amplitude.dbHelper = mockDbHelper;

        // log an event to trigger SQLException that we set up with mocks
        amplitude.logEvent("test");
        looper.runToEndOfTasks();

        // the reset callback handler will keep retriggering the exception, so make sure we guard against stack overflows
        // verify that the metadata has not been persisted back to database
        String newDeviceId = dbInstance.getValue(AmplitudeClient.DEVICE_ID_KEY);
        Long newPreviousSessionId = dbInstance.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY);
        Long newLastEventTime = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY);
        Long newSequenceNumber = dbInstance.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY);
        Long newLastEventId = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY);
        Long newLastIdentifyId = dbInstance.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY);

        assertNull(newDeviceId);
        assertNull(newPreviousSessionId);
        assertNull(newLastEventTime);
        assertNull(newSequenceNumber);
        assertNull(newLastEventId);  // insert event fails, and returns -1
        assertNull(newLastIdentifyId);

        reset(mockDbHelper);
    }

    @Test
    public void testCorruptingDatabaseFile() throws IOException, JSONException {

        // log an event normally, verify metadata updated in table
        amplitude.logEvent("test");
        looper.runToEndOfTasks();

        // metadata: deviceId, sessionId, sequence number, last_event_id, last_event_time, previous_session_id
        assertEquals(dbInstance.getEventCount(), 1);
        assertEquals(dbInstance.getTotalEventCount(), 1);
        assertEquals(dbInstance.getNthEventId(1), 1);

        String deviceId = dbInstance.getValue(AmplitudeClient.DEVICE_ID_KEY);
        long previousSessionId = dbInstance.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY);
        long sequenceNumber = dbInstance.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY);
        long lastEventId = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY);
        long lastEventTime = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY);
        long lastIdentifyId = dbInstance.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY);

        assertNotNull(deviceId);
        assertTrue(deviceId.endsWith("R"));
        assertTrue(previousSessionId > 0);
        assertEquals(sequenceNumber, 1);
        assertEquals(lastEventId, 1);
        assertTrue(lastEventTime >= startTime);
        assertEquals(lastIdentifyId, -1);

        // try to corrupt database file and then log another event
        File file = dbInstance.file;
        RandomAccessFile writer = new RandomAccessFile(file.getAbsolutePath(), "rw");
        writer.seek(2);
        writer.writeChars("corrupt database file with random string");
        writer.close();

        amplitude.logEvent("test");
        looper.runToEndOfTasks();

        // since events table recreated, the event id should have been reset back to 1
        List<JSONObject> events = dbInstance.getEvents(5, 5);
        assertEquals(events.size(), 1);
        assertEquals(events.get(0).optInt("event_id"), 1);

        // verify metadata is re-inserted into database
        String newDeviceId = dbInstance.getValue(AmplitudeClient.DEVICE_ID_KEY);
        long newPreviousSessionId = dbInstance.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY);
        long newLastEventTime = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY);
        long newSequenceNumber = dbInstance.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY);
        long newLastEventId = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY);
        Long newLastIdentifyId = dbInstance.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY);

        assertEquals(newDeviceId, deviceId);
        assertEquals(newPreviousSessionId, previousSessionId);
        assertTrue(newLastEventTime >= lastEventTime);
        assertEquals(newSequenceNumber, 2);
        assertEquals(newLastEventId, 1);
        assertNull(newLastIdentifyId);
    }

    @Test
    public void testDeletedDatabaseFile() throws IOException, JSONException {

        // log an event normally, verify metadata updated in table
        amplitude.logEvent("test");
        looper.runToEndOfTasks();

        // metadata: deviceId, sessionId, sequence number, last_event_id, last_event_time, previous_session_id
        assertEquals(dbInstance.getEventCount(), 1);
        assertEquals(dbInstance.getTotalEventCount(), 1);
        assertEquals(dbInstance.getNthEventId(1), 1);

        String deviceId = dbInstance.getValue(AmplitudeClient.DEVICE_ID_KEY);
        long previousSessionId = dbInstance.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY);
        long sequenceNumber = dbInstance.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY);
        long lastEventId = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY);
        long lastEventTime = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY);
        long lastIdentifyId = dbInstance.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY);

        assertNotNull(deviceId);
        assertTrue(deviceId.endsWith("R"));
        assertTrue(previousSessionId > 0);
        assertEquals(sequenceNumber, 1);
        assertEquals(lastEventId, 1);
        assertTrue(lastEventTime >= startTime);
        assertEquals(lastIdentifyId, -1);

        // try to delete database file and test logging event
        File file = dbInstance.file;
        context.deleteDatabase(file.getName());
        amplitude.logEvent("test");
        looper.runToEndOfTasks();

        // since events table recreated, the event id should have been reset back to 1
        List<JSONObject> events = dbInstance.getEvents(5, 5);
        assertEquals(events.size(), 1);
        assertEquals(events.get(0).optInt("event_id"), 1);

        // verify metadata is re-inserted into database
        String newDeviceId = dbInstance.getValue(AmplitudeClient.DEVICE_ID_KEY);
        long newPreviousSessionId = dbInstance.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY);
        long newLastEventTime = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY);
        long newSequenceNumber = dbInstance.getLongValue(AmplitudeClient.SEQUENCE_NUMBER_KEY);
        long newLastEventId = dbInstance.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY);
        Long newLastIdentifyId = dbInstance.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY);

        assertEquals(newDeviceId, deviceId);
        assertEquals(newPreviousSessionId, previousSessionId);
        assertTrue(newLastEventTime >= lastEventTime);
        assertEquals(newSequenceNumber, 2);
        assertEquals(newLastEventId, 1);
        assertNull(newLastIdentifyId);
    }
}
