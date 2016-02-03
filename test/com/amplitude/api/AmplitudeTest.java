package com.amplitude.api;

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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AmplitudeTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetInstance() {
        AmplitudeClient a = Amplitude.getInstance();
        AmplitudeClient b = Amplitude.getInstance("");
        AmplitudeClient c = Amplitude.getInstance(null);
        AmplitudeClient d = Amplitude.getInstance(Constants.DEFAULT_INSTANCE);
        AmplitudeClient e = Amplitude.getInstance("app1");
        AmplitudeClient f = Amplitude.getInstance("app2");

        assertSame(a, b);
        assertSame(b, c);
        assertSame(c, d);
        assertSame(d, Amplitude.getInstance());
        assertNotSame(d, e);
        assertSame(e, Amplitude.getInstance("app1"));
        assertNotSame(e, f);
        assertSame(f, Amplitude.getInstance("app2"));

        assertTrue(Amplitude.instances.size() == 3);
        assertTrue(Amplitude.instances.containsKey(Constants.DEFAULT_INSTANCE));
        assertTrue(Amplitude.instances.containsKey("app1"));
        assertTrue(Amplitude.instances.containsKey("app2"));
    }

    @Test
    public void testSeparateInstancesLogEventsSeparately() {
        String deviceId = "testDeviceId";
        String event1 = "testEvent1";
        String identify1 = "testIdentify1";
        String identify2 = "testIdentify2";

        String newApiKey1 = "1234567890";
        String newApiKeySuffix1 = newApiKey1.substring(0, 6);
        String newApiKey2 = "0987654321";
        String newApiKeySuffix2 = newApiKey2.substring(0, 6);

        // Setup existing Databasefile
        DatabaseHelper oldDbHelper = DatabaseHelper.getDatabaseHelper(context);
        oldDbHelper.insertOrReplaceKeyValue("device_id", deviceId);
        oldDbHelper.insertOrReplaceKeyLongValue("sequence_number", 1000L);
        oldDbHelper.addEvent(event1);
        oldDbHelper.addIdentify(identify1);
        oldDbHelper.addIdentify(identify2);

        File oldDbFile = context.getDatabasePath(Constants.DATABASE_NAME);
        assertTrue(oldDbFile.exists());
        File newDbFile1 = context.getDatabasePath(Constants.DATABASE_NAME + "_" + newApiKeySuffix1);
        assertFalse(newDbFile1.exists());
        File newDbFile2 = context.getDatabasePath(Constants.DATABASE_NAME + "_" + newApiKeySuffix2);
        assertFalse(newDbFile2.exists());

        // init first new app and do database file migration
        Amplitude.getInstance("app1").initialize(context, newApiKey1);
        assertTrue(newDbFile1.exists());
        assertFalse(newDbFile2.exists());

        DatabaseHelper newDbHelper1 = DatabaseHelper.getDatabaseHelper(context, newApiKeySuffix1);
        assertEquals(newDbHelper1.getValue("device_id"), deviceId);
        assertEquals(newDbHelper1.getLongValue("sequence_number").longValue(), 1000L);
        assertEquals(newDbHelper1.getEventCount(), 1);
        assertEquals(newDbHelper1.getIdentifyCount(), 2);

        // init second new app without database file migration
        Amplitude.getInstance("app2").initialize(context, newApiKey2, null, true);
        assertTrue(newDbFile1.exists());
        assertFalse(newDbFile2.exists());

        // database file will be created once app2 goes through deviceId init process
        Shadows.shadowOf(Amplitude.getInstance("app2").logThread.getLooper()).runToEndOfTasks();
        assertTrue(newDbFile2.exists());
        DatabaseHelper newDbHelper2 = DatabaseHelper.getDatabaseHelper(context, newApiKeySuffix2);
        assertFalse(newDbHelper2.getValue("device_id").equals(deviceId));
        assertEquals(newDbHelper2.getEventCount(), 0);
        assertEquals(newDbHelper2.getIdentifyCount(), 0);

        // verify existing database still intact
        assertTrue(oldDbFile.exists());
        assertEquals(oldDbHelper.getValue("device_id"), deviceId);
        assertEquals(oldDbHelper.getLongValue("sequence_number").longValue(), 1000L);
        assertEquals(oldDbHelper.getEventCount(), 1);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        // verify both apps can modify their database indepdently and not affect old database
        newDbHelper1.insertOrReplaceKeyValue("device_id", "fakeDeviceId");
        assertEquals(newDbHelper1.getValue("device_id"), "fakeDeviceId");
        assertFalse(newDbHelper2.getValue("device_id").equals("fakeDeviceId"));
        assertEquals(oldDbHelper.getValue("device_id"), deviceId);
        newDbHelper1.addIdentify("testIdentify3");
        assertEquals(newDbHelper1.getIdentifyCount(), 3);
        assertEquals(newDbHelper2.getIdentifyCount(), 0);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        // verify modifying new database does not affect old database
        newDbHelper2.insertOrReplaceKeyValue("device_id", "brandNewDeviceId");
        assertEquals(newDbHelper1.getValue("device_id"), "fakeDeviceId");
        assertEquals(newDbHelper2.getValue("device_id"), "brandNewDeviceId");
        assertEquals(oldDbHelper.getValue("device_id"), deviceId);
        newDbHelper2.addEvent("testEvent2");
        newDbHelper2.addEvent("testEvent3");
        assertEquals(newDbHelper1.getEventCount(), 1);
        assertEquals(newDbHelper2.getEventCount(), 2);
        assertEquals(oldDbHelper.getEventCount(), 1);
    }
}
