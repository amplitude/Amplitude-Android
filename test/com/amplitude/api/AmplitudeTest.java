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
        Amplitude.instances.clear();
        DatabaseHelper.instances.clear();

        String newInstance1 = "newApp1";
        String newApiKey1 = "1234567890";
        String newInstance2 = "newApp2";
        String newApiKey2 = "0987654321";
        File oldDbFile = context.getDatabasePath(Constants.DATABASE_NAME);
        File newDbFile1 = context.getDatabasePath(Constants.DATABASE_NAME + "_" + newInstance1);
        File newDbFile2 = context.getDatabasePath(Constants.DATABASE_NAME + "_" + newInstance2);

        // Setup existing Databasefile
        DatabaseHelper oldDbHelper = DatabaseHelper.getDatabaseHelper(context);
        oldDbHelper.insertOrReplaceKeyValue("device_id", "oldDeviceId");
        oldDbHelper.insertOrReplaceKeyLongValue("sequence_number", 1000L);
        oldDbHelper.addEvent("oldEvent1");
        oldDbHelper.addIdentify("oldIdentify1");
        oldDbHelper.addIdentify("oldIdentify2");

        // Verify persistence of old database file in default instance
        Amplitude.getInstance().initialize(context, apiKey);
        Shadows.shadowOf(Amplitude.getInstance().logThread.getLooper()).runToEndOfTasks();
        assertEquals(Amplitude.getInstance().getDeviceId(), "oldDeviceId");
        assertEquals(Amplitude.getInstance().getNextSequenceNumber(), 1001L);
        assertTrue(oldDbFile.exists());
        assertFalse(newDbFile1.exists());
        assertFalse(newDbFile2.exists());

        // init first new app and verify separate database file
        Amplitude.getInstance(newInstance1).initialize(context, newApiKey1);
        Shadows.shadowOf(
            Amplitude.getInstance(newInstance1).logThread.getLooper()
        ).runToEndOfTasks();
        assertTrue(newDbFile1.exists()); // db file is created after deviceId initialization

        DatabaseHelper newDbHelper1 = DatabaseHelper.getDatabaseHelper(context, newInstance1);
        assertFalse(newDbHelper1.getValue("device_id").equals("oldDeviceId"));
        assertEquals(
            newDbHelper1.getValue("device_id"), Amplitude.getInstance(newInstance1).getDeviceId()
        );
        assertEquals(Amplitude.getInstance(newInstance1).getNextSequenceNumber(), 1L);
        assertEquals(newDbHelper1.getEventCount(), 0);
        assertEquals(newDbHelper1.getIdentifyCount(), 0);

        // init second new app and verify separate database file
        Amplitude.getInstance(newInstance2).initialize(context, newApiKey2);
        Shadows.shadowOf(
            Amplitude.getInstance(newInstance2).logThread.getLooper()
        ).runToEndOfTasks();
        assertTrue(newDbFile2.exists()); // db file is created after deviceId initialization

        DatabaseHelper newDbHelper2 = DatabaseHelper.getDatabaseHelper(context, newInstance2);
        assertFalse(newDbHelper2.getValue("device_id").equals("oldDeviceId"));
        assertEquals(
            newDbHelper2.getValue("device_id"), Amplitude.getInstance(newInstance2).getDeviceId()
        );
        assertEquals(Amplitude.getInstance(newInstance2).getNextSequenceNumber(), 1L);
        assertEquals(newDbHelper2.getEventCount(), 0);
        assertEquals(newDbHelper2.getIdentifyCount(), 0);

        // verify existing database still intact
        assertTrue(oldDbFile.exists());
        assertEquals(oldDbHelper.getValue("device_id"), "oldDeviceId");
        assertEquals(oldDbHelper.getLongValue("sequence_number").longValue(), 1001L);
        assertEquals(oldDbHelper.getEventCount(), 1);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        // verify both apps can modify their database independently and not affect old database
        newDbHelper1.insertOrReplaceKeyValue("device_id", "fakeDeviceId");
        assertEquals(newDbHelper1.getValue("device_id"), "fakeDeviceId");
        assertFalse(newDbHelper2.getValue("device_id").equals("fakeDeviceId"));
        assertEquals(oldDbHelper.getValue("device_id"), "oldDeviceId");
        newDbHelper1.addIdentify("testIdentify3");
        assertEquals(newDbHelper1.getIdentifyCount(), 1);
        assertEquals(newDbHelper2.getIdentifyCount(), 0);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        newDbHelper2.insertOrReplaceKeyValue("device_id", "brandNewDeviceId");
        assertEquals(newDbHelper1.getValue("device_id"), "fakeDeviceId");
        assertEquals(newDbHelper2.getValue("device_id"), "brandNewDeviceId");
        assertEquals(oldDbHelper.getValue("device_id"), "oldDeviceId");
        newDbHelper2.addEvent("testEvent2");
        newDbHelper2.addEvent("testEvent3");
        assertEquals(newDbHelper1.getEventCount(), 0);
        assertEquals(newDbHelper2.getEventCount(), 2);
        assertEquals(oldDbHelper.getEventCount(), 1);
    }
}
