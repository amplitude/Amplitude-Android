package com.amplitude.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class UpgradePrefsTest extends BaseTest {
    @Before
    public void setUp() throws Exception {
        super.setUp(false);
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

        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));
        assertEquals(
            DatabaseHelper.getDatabaseHelper(context).getValue(AmplitudeClient.DEVICE_ID_KEY),
            deviceId
        );

        // deviceId should be removed from sharedPrefs after upgrade
        assertNull(prefs.getString(Constants.PREFKEY_DEVICE_ID, null));
    }

    @Test
    public void testUpgradeDeviceIdToDBEmpty() {
        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));
        assertNull(
            DatabaseHelper.getDatabaseHelper(context).getValue(AmplitudeClient.DEVICE_ID_KEY)
        );
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
        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        assertEquals(
            DatabaseHelper.getDatabaseHelper(context).getValue(AmplitudeClient.DEVICE_ID_KEY),
            deviceId
        );

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
        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        assertNull(target.getString(Constants.PREFKEY_DEVICE_ID, null));
        assertNull(
            DatabaseHelper.getDatabaseHelper(context).getValue(AmplitudeClient.DEVICE_ID_KEY)
        );
    }

    @Test
    public void testUpgradeOptOutFromSharedPrefsToDB() {
        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Constants.PREFKEY_OPT_OUT, true).commit();

        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));
        assertEquals(
            (long) DatabaseHelper.getDatabaseHelper(context).getLongValue(
                AmplitudeClient.OPT_OUT_KEY
            ), 1L
        );

        // deviceId should be removed from sharedPrefs after upgrade
        assertFalse(prefs.getBoolean(Constants.PREFKEY_OPT_OUT, false));
    }

    @Test
    public void testSkipUpgradeOptOutFromSharedPrefsToDB() {
        // we skip the upgrade of individual fields if they already exist in the database
        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Constants.PREFKEY_OPT_OUT, true).commit();

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.OPT_OUT_KEY, 0L);

        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));
        assertEquals(
            (long) DatabaseHelper.getDatabaseHelper(context).getLongValue(
                AmplitudeClient.OPT_OUT_KEY
            ), 0L
        );

        // shared prefs not deleted since migration skipped
        assertTrue(prefs.getBoolean(Constants.PREFKEY_OPT_OUT, false));
    }

    @Test
    public void testUpgradeOptOutFromLegacyToDB() {
        String legacyPkgName = "com.amplitude.a";
        String sourceName = legacyPkgName + "." + context.getPackageName();
        context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).edit()
                .putBoolean(legacyPkgName + ".optOut", true)
                .commit();

        assertTrue(AmplitudeClient.upgradePrefs(context, legacyPkgName, null));
        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        assertFalse(target.getBoolean(Constants.PREFKEY_DEVICE_ID, false));
        assertEquals(
            (long) DatabaseHelper.getDatabaseHelper(context).getLongValue(
                AmplitudeClient.OPT_OUT_KEY
            ), 1L
        );
    }

    @Test
    public void testUpgradeUserIdFromSharedPrefsToDB() {
        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_USER_ID, "testUserId").commit();

        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));
        assertEquals(
            DatabaseHelper.getDatabaseHelper(context).getValue(AmplitudeClient.USER_ID_KEY),
            "testUserId"
        );

        // deviceId should be removed from sharedPrefs after upgrade
        assertNull(prefs.getString(Constants.PREFKEY_USER_ID, null));
    }

    @Test
    public void testUpgradeUserIdFromLegacyToDB() {
        String legacyPkgName = "com.amplitude.a";
        String sourceName = legacyPkgName + "." + context.getPackageName();
        context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).edit()
                .putString(legacyPkgName + ".userId", "testUserId2").commit();

        assertTrue(AmplitudeClient.upgradePrefs(context, legacyPkgName, null));
        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        assertNull(target.getString(Constants.PREFKEY_USER_ID, null));
        assertEquals(DatabaseHelper.getDatabaseHelper(context).getValue(
            AmplitudeClient.USER_ID_KEY
        ), "testUserId2");
    }

    @Test
    public void testSkipUpgradeSharedPrefsToDb() {
        // skips if DB already has deviceId, previous session id, and last event time
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.insertOrReplaceKeyValue(AmplitudeClient.DEVICE_ID_KEY, "testDeviceId");
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY, 1000L);
        dbHelper.insertOrReplaceKeyLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY, 2000L);

        assertNull(dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY));

        String sourceName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences prefs = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFKEY_DEVICE_ID, "otherDeviceId").commit();
        prefs.edit().putString(Constants.PREFKEY_USER_ID, "testUserId").commit();
        prefs.edit().putBoolean(Constants.PREFKEY_OPT_OUT, true).commit();
        prefs.edit().putLong(Constants.PREFKEY_LAST_IDENTIFY_ID, 3000L).commit();

        assertTrue(AmplitudeClient.upgradeSharedPrefsToDB(context));

        // after upgrade, pref values still there since they weren't deleted
        assertEquals(dbHelper.getValue(AmplitudeClient.DEVICE_ID_KEY), "testDeviceId");
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.PREVIOUS_SESSION_ID_KEY), 1000L);
        assertEquals((long) dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_TIME_KEY), 2000L);
        assertNull(dbHelper.getValue(AmplitudeClient.USER_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.LAST_EVENT_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.LAST_IDENTIFY_ID_KEY));
        assertNull(dbHelper.getLongValue(AmplitudeClient.OPT_OUT_KEY));

        assertEquals(prefs.getString(Constants.PREFKEY_DEVICE_ID, null), "otherDeviceId");
        assertEquals(prefs.getString(Constants.PREFKEY_USER_ID, null), "testUserId");
        assertTrue(prefs.getBoolean(Constants.PREFKEY_OPT_OUT, false));
        assertEquals(prefs.getLong(Constants.PREFKEY_LAST_IDENTIFY_ID, -1), 3000L);
    }
}
