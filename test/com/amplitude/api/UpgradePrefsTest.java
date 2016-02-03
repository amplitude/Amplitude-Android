package com.amplitude.api;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class UpgradePrefsTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        ShadowApplication.getInstance().setPackageName("com.amplitude.test");
        context = ShadowApplication.getInstance().getApplicationContext();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testUpgradeOnInit() {
        Constants.class.getPackage().getName();

        amplitude = new AmplitudeClient();
        amplitude.initialize(context, "KEY");
    }

    @Test
    public void testUpgrade() {
        String sourceName = "com.amplitude.a" + "." + context.getPackageName();
        context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).edit()
                .putLong("com.amplitude.a.previousSessionId", 100L)
                .putString("com.amplitude.a.deviceId", "deviceid")
                .putString("com.amplitude.a.userId", "userid")
                .putBoolean("com.amplitude.a.optOut", true)
                .commit();

        assertTrue(AmplitudeClient.upgradePrefs(context, "com.amplitude.a", null));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, -1), 100L);
        assertEquals(target.getString(Constants.PREFKEY_DEVICE_ID, null), "deviceid");
        assertEquals(target.getString(Constants.PREFKEY_USER_ID, null), "userid");
        assertEquals(target.getBoolean(Constants.PREFKEY_OPT_OUT, false), true);

        int size = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).getAll().size();
        assertEquals(size, 0);
    }

    @Test
    public void testUpgradeSelf() {
        assertFalse(AmplitudeClient.upgradePrefs(context));
    }

    @Test
    public void testUpgradeEmpty() {
        assertFalse(AmplitudeClient.upgradePrefs(context, "empty", null));

        String sourceName = "empty" + "." + context.getPackageName();
        context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).edit()
                .commit();

        assertFalse(AmplitudeClient.upgradePrefs(context, "empty", null));
    }

    @Test
    public void testUpgradePartial() {
        String sourceName = "partial" + "." + context.getPackageName();
        context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).edit()
                .putLong("partial.lastEventTime", 100L)
                .putString("partial.deviceId", "deviceid")
                .commit();

        assertTrue(AmplitudeClient.upgradePrefs(context, "partial", null));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, -1), -1);
        assertEquals(target.getString(Constants.PREFKEY_DEVICE_ID, null), "deviceid");
        assertEquals(target.getString(Constants.PREFKEY_USER_ID, null), null);
        assertEquals(target.getBoolean(Constants.PREFKEY_OPT_OUT, false), false);

        int size = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).getAll().size();
        assertEquals(size, 0);
    }

    @Test
    public void testUpgradeDeviceIdToDB() {
        String deviceId = "device_id";
        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_DEVICE_ID, deviceId).commit();

        assertTrue(AmplitudeClient.upgradeDeviceIdToDB(context, null, apiKeySuffix));
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context, apiKeySuffix);
        assertEquals(dbHelper.getValue(AmplitudeClient.DEVICE_ID_KEY), deviceId);

        // deviceId should be removed from sharedPrefs after upgrade
        assertNull(prefs.getString(Constants.PREFKEY_DEVICE_ID, null));
    }

    @Test
    public void testUpgradeDeviceIdToDBEmpty() {
        assertTrue(AmplitudeClient.upgradeDeviceIdToDB(context, null, apiKeySuffix));
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context, apiKeySuffix);
        assertNull(dbHelper.getValue(AmplitudeClient.DEVICE_ID_KEY));
    }

    @Test
    public void testUpgradeDeviceIdFromLegacyToDB() {
        String deviceId = "device_id";
        String legacyPkgName = "com.amplitude.a";
        String sourceName = legacyPkgName + "." + context.getPackageName();
        context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).edit()
                .putString(legacyPkgName + ".deviceId", deviceId)
                .commit();

        assertTrue(AmplitudeClient.upgradePrefs(context, legacyPkgName, null));
        assertTrue(AmplitudeClient.upgradeDeviceIdToDB(context, null, apiKeySuffix));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context, apiKeySuffix);
        assertEquals(dbHelper.getValue(AmplitudeClient.DEVICE_ID_KEY), deviceId);

        // deviceId should be removed from sharedPrefs after upgrade
        assertNull(target.getString(Constants.PREFKEY_DEVICE_ID, null));
    }

    @Test
    public void testUpgradeDeviceIdFromLegacyToDBEmpty() {
        String legacyPkgName = "com.amplitude.a";
        String sourceName = legacyPkgName + "." + context.getPackageName();
        context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).edit()
                .putLong("partial.lastEventTime", 100L)
                .commit();

        assertTrue(AmplitudeClient.upgradePrefs(context, legacyPkgName, null));
        assertTrue(AmplitudeClient.upgradeDeviceIdToDB(context, null, apiKeySuffix));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        assertNull(target.getString(Constants.PREFKEY_DEVICE_ID, null));
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context, apiKeySuffix);
        assertNull(dbHelper.getValue(AmplitudeClient.DEVICE_ID_KEY));
    }

    @Test
    public void testMigrateDatabaseFile() {
        String apiKeySuffix = "123456";
        File oldDbFile = context.getDatabasePath(Constants.DATABASE_NAME);
        assertFalse(oldDbFile.exists());
        DatabaseHelper oldDbHelper = DatabaseHelper.getDatabaseHelper(context);
        oldDbHelper.insertOrReplaceKeyValue("device_id", "testDeviceId");
        assertTrue(oldDbFile.exists());

        File newDbFile = context.getDatabasePath(Constants.DATABASE_NAME + "_" + apiKeySuffix);
        assertFalse(newDbFile.exists());

        assertTrue(AmplitudeClient.migrateDatabaseFile(context, apiKeySuffix));
        assertTrue(newDbFile.exists());
        DatabaseHelper newDbHelper = DatabaseHelper.getDatabaseHelper(context, apiKeySuffix);
        assertEquals(newDbHelper.getValue("device_id"), "testDeviceId");

        // verify modifying old file does not affect new file
        oldDbHelper.insertOrReplaceKeyValue("device_id", "fakeDeviceId");
        assertEquals(oldDbHelper.getValue("device_id"), "fakeDeviceId");
        assertEquals(newDbHelper.getValue("device_id"), "testDeviceId");
    }

    @Test
    public void testMigrateDatabaseFileFail() {
        String apiKeySuffix = "123456";
        File oldDbFile = context.getDatabasePath(Constants.DATABASE_NAME);
        assertFalse(oldDbFile.exists());
        File newDbFile = context.getDatabasePath(Constants.DATABASE_NAME + "_" + apiKeySuffix);
        assertFalse(newDbFile.exists());
        assertFalse(AmplitudeClient.migrateDatabaseFile(context, apiKeySuffix));
        assertFalse(newDbFile.exists());
    }
}
