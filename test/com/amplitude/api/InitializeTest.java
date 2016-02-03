package com.amplitude.api;

import android.content.Context;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class InitializeTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testInitializeUserId() {
        String userId = "user_id";

        Amplitude.getInstance().initialize(context, "1cc2c1978ebab0f6451112a8f5df4f4e", userId);

        // Test that the user id is set.
        String sharedPreferences = Constants.SHARED_PREFERENCES_NAME_PREFIX + "."
                + context.getPackageName();
        assertEquals(sharedPreferences, "com.amplitude.api.com.amplitude.test");
        assertEquals(
                userId,
                context.getSharedPreferences(sharedPreferences, Context.MODE_PRIVATE).getString(
                        Constants.PREFKEY_USER_ID, null));

        // Test that events are logged.
        RecordedRequest request = sendEvent(amplitude, "init_test_event", null);
        assertNotNull(request);
    }

    @Test
    public void testMigrateExistingDatabaseFile() throws JSONException {
        String deviceId = "testDeviceId";
        String event1 = "testEvent1";
        String identify1 = "testIdentify1";
        String identify2 = "testIdentify2";

        String newApiKey = "1234567890";
        String newApiKeySuffix = newApiKey.substring(0, 6);

        // Setup existing Databasefile
        DatabaseHelper oldDbHelper = DatabaseHelper.getDatabaseHelper(context);
        oldDbHelper.insertOrReplaceKeyValue("device_id", deviceId);
        oldDbHelper.insertOrReplaceKeyLongValue("sequence_number", 1000L);
        oldDbHelper.addEvent(event1);
        oldDbHelper.addIdentify(identify1);
        oldDbHelper.addIdentify(identify2);

        File oldDbFile = context.getDatabasePath(Constants.DATABASE_NAME);
        assertTrue(oldDbFile.exists());
        File newDbFile = context.getDatabasePath(Constants.DATABASE_NAME + "_" + newApiKeySuffix);
        assertFalse(newDbFile.exists());

        // Migrate with init
        Amplitude.getInstance("new app").initialize(context, newApiKey);
        assertTrue(newDbFile.exists());
        DatabaseHelper newDbHelper = DatabaseHelper.getDatabaseHelper(context, newApiKeySuffix);
        assertEquals(newDbHelper.getValue("device_id"), deviceId);
        assertEquals(newDbHelper.getLongValue("sequence_number").longValue(), 1000L);
        assertEquals(newDbHelper.getEventCount(), 1);
        assertEquals(newDbHelper.getIdentifyCount(), 2);

        // verify existing database still intact
        assertTrue(oldDbFile.exists());
        assertEquals(oldDbHelper.getValue("device_id"), deviceId);
        assertEquals(oldDbHelper.getLongValue("sequence_number").longValue(), 1000L);
        assertEquals(oldDbHelper.getEventCount(), 1);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        // verify modifying new database does not affect old database
        newDbHelper.insertOrReplaceKeyValue("device_id", "fakeDeviceId");
        assertEquals(newDbHelper.getValue("device_id"), "fakeDeviceId");
        assertEquals(oldDbHelper.getValue("device_id"), deviceId);
        newDbHelper.addIdentify("testIdentify3");
        assertEquals(newDbHelper.getIdentifyCount(), 3);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);
    }

    @Test
    public void testDoNotMigrateExistingDatabaseFile() {
        String deviceId = "testDeviceId";
        String event1 = "testEvent1";
        String identify1 = "testIdentify1";
        String identify2 = "testIdentify2";

        String newApiKey = "1234567890";
        String newApiKeySuffix = newApiKey.substring(0, 6);

        // Setup existing Databasefile
        DatabaseHelper oldDbHelper = DatabaseHelper.getDatabaseHelper(context);
        oldDbHelper.insertOrReplaceKeyValue("device_id", deviceId);
        oldDbHelper.insertOrReplaceKeyLongValue("sequence_number", 1000L);
        oldDbHelper.addEvent(event1);
        oldDbHelper.addIdentify(identify1);
        oldDbHelper.addIdentify(identify2);

        File oldDbFile = context.getDatabasePath(Constants.DATABASE_NAME);
        assertTrue(oldDbFile.exists());
        File newDbFile = context.getDatabasePath(Constants.DATABASE_NAME + "_" + newApiKeySuffix);
        assertFalse(newDbFile.exists());

        // Migrate with init - set newBlankInstance to true to skip db file migration
        Amplitude.getInstance("new app").initialize(context, newApiKey, null, true);

        // database file will be created once app goes through deviceId init process
        assertFalse(newDbFile.exists());
        Shadows.shadowOf(Amplitude.getInstance("new app").logThread.getLooper()).runToEndOfTasks();
        assertTrue(newDbFile.exists());
        DatabaseHelper newDbHelper = DatabaseHelper.getDatabaseHelper(context, newApiKeySuffix);
        assertFalse(newDbHelper.getValue("device_id").equals(deviceId));
        assertEquals(newDbHelper.getEventCount(), 0);
        assertEquals(newDbHelper.getIdentifyCount(), 0);

        // verify existing database still intact
        assertTrue(oldDbFile.exists());
        assertEquals(oldDbHelper.getValue("device_id"), deviceId);
        assertEquals(oldDbHelper.getLongValue("sequence_number").longValue(), 1000L);
        assertEquals(oldDbHelper.getEventCount(), 1);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        // verify modifying new database does not affect old database
        newDbHelper.insertOrReplaceKeyValue("device_id", "fakeDeviceId");
        assertEquals(newDbHelper.getValue("device_id"), "fakeDeviceId");
        assertEquals(oldDbHelper.getValue("device_id"), deviceId);
        newDbHelper.addIdentify("testIdentify3");
        assertEquals(newDbHelper.getIdentifyCount(), 1);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);
    }
}
