package com.amplitude.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import android.content.Context;
import android.content.SharedPreferences;

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
                .putLong("com.amplitude.a.previousSessionTime", 100L)
                .putLong("com.amplitude.a.previousEndSessionTime", 200L)
                .putLong("com.amplitude.a.previousEndSessionId", 300L)
                .putLong("com.amplitude.a.previousSessionId", 400L)
                .putString("com.amplitude.a.deviceId", "deviceid")
                .putString("com.amplitude.a.userId", "userid")
                .putBoolean("com.amplitude.a.optOut", true)
                .commit();

        assertTrue(AmplitudeClient.upgradePrefs(context, "com.amplitude.a", null));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_SESSION_TIME, -1), 100L);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_END_SESSION_TIME, -1), 200L);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_END_SESSION_ID, -1), 300L);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, -1), 400L);
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
                .putLong("partial.previousSessionTime", 100L)
                .putString("partial.deviceId", "deviceid")
                .commit();

        assertTrue(AmplitudeClient.upgradePrefs(context, "partial", null));

        String targetName = Constants.PACKAGE_NAME + "." + context.getPackageName();
        SharedPreferences target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_SESSION_TIME, -1), 100L);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_END_SESSION_TIME, -1), -1);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_END_SESSION_ID, -1), -1);
        assertEquals(target.getLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, -1), -1);
        assertEquals(target.getString(Constants.PREFKEY_DEVICE_ID, null), "deviceid");
        assertEquals(target.getString(Constants.PREFKEY_USER_ID, null), null);
        assertEquals(target.getBoolean(Constants.PREFKEY_OPT_OUT, false), false);

        int size = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE).getAll().size();
        assertEquals(size, 0);
    }

}
