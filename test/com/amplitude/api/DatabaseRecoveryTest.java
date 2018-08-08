package com.amplitude.api;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;

@RunWith(RobolectricTestRunner.class)
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

        amplitude.initialize(context, apiKey);
        looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runOneTask();
        dbInstance = DatabaseHelper.getDatabaseHelper(context);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        if (dbInstance != null) {
            if (dbInstance.getWritableDatabase().isOpen()) {
                dbInstance.close();
            }
        }
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
    }
}
