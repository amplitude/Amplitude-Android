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

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DatabaseHelperTest extends BaseTest {

    protected DatabaseHelper dbInstance;

    @Before
    public void setUp() throws Exception {
        super.setUp(false);
        amplitude.initialize(context, apiKey);
        Shadows.shadowOf(amplitude.logThread.getLooper()).runOneTask();
        dbInstance = DatabaseHelper.getDatabaseHelper(context);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        dbInstance = null;
    }

    protected long addEvent(String type) {
        return addEventToTable(DatabaseHelper.EVENT_TABLE_NAME, type, new JSONObject());
    }

    protected long addEventToTable(String table, String type, JSONObject props) {
        try {
            props.put("event_type", type);
            return table.equals(DatabaseHelper.IDENTIFY_TABLE_NAME) ?
                    dbInstance.addIdentify(props.toString()) :
                    dbInstance.addEvent(props.toString());
        } catch (JSONException e) {
            fail(e.toString());
        }
        return -1;
    }

    protected long addIdentify(String identifyEvent) {
        return addEventToTable(DatabaseHelper.IDENTIFY_TABLE_NAME, identifyEvent, new JSONObject());
    }

    protected long insertOrReplaceKeyValue(String key, String value) {
        return dbInstance.insertOrReplaceKeyValue(key, value);
    }

    protected long insertOrReplaceKeyLongValue(String key, Long value) {
        return dbInstance.insertOrReplaceKeyLongValue(key, value);
    }

    protected String getValue(String key) {
        return dbInstance.getValue(key);
    }

    protected Long getLongValue(String key) { return dbInstance.getLongValue(key); }

    @Test
    public void testCreate() {
        dbInstance.onCreate(dbInstance.getWritableDatabase());
        assertEquals(1, insertOrReplaceKeyValue("test_key", "test_value"));
        // due to upgradeSharedPrefsToDb, there are already 5 entries in long table
        // so this next insertion will be row 6
        assertEquals(6, insertOrReplaceKeyLongValue("test_key", 1L));
        assertEquals(1, addEvent("test_create"));
        assertEquals(1, addIdentify("test_create"));
    }

    // need separate tests for different version to version upgrades since insertion failure
    // triggers database deletion and recreation - need to refetch writable database as well
    @Test
    public void testUpgradeVersion1ToVersion2() {
        // store table doesn't exist in v1, insert will fail
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.STORE_TABLE_NAME);
        String key = "test_key";
        String value = "test_value";
        assertEquals(-1, insertOrReplaceKeyValue(key, value));

        // long store table doesn't exist in v1, insert will fail
        Long longValue = 1L;
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.LONG_STORE_TABLE_NAME);
        assertEquals(-1, insertOrReplaceKeyLongValue(key, longValue));

        // identify table doesn't exist in v1, insert will fail
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.IDENTIFY_TABLE_NAME);
        assertEquals(-1, addIdentify("test_upgrade"));

        // only event inserts will work
        assertEquals(1, addEvent("test_upgrade"));

        // after v2 upgrade, can insert into store table
        // still can't insert into identify table or long store table
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.STORE_TABLE_NAME);
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.IDENTIFY_TABLE_NAME);
        dbInstance.onUpgrade(dbInstance.getWritableDatabase(), 1, 2);
        assertEquals(2, addEvent("test_upgrade"));
        assertEquals(1, insertOrReplaceKeyValue(key, value));
        assertEquals(-1, addIdentify("test_upgrade"));
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.LONG_STORE_TABLE_NAME);
        assertEquals(-1, insertOrReplaceKeyLongValue(key, longValue));
    }

    @Test
    public void testUpgradeVersion2ToVersion3() {
        // identify table doesn't exist in v2, insert will fail
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.IDENTIFY_TABLE_NAME);
        assertEquals(-1, addIdentify("test_upgrade"));

        // long store table doesn't exist in v2, insert will fail
        String key = "test_key";
        Long longValue = 1L;
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.LONG_STORE_TABLE_NAME);
        assertEquals(-1, insertOrReplaceKeyLongValue(key, longValue));

        // events and store inserts will work
        String value = "test_value";
        assertEquals(1, insertOrReplaceKeyValue(key, value));
        assertEquals(1, addEvent("test_upgrade"));

        // after v3 upgrade, can insert into identify table and long store
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.IDENTIFY_TABLE_NAME);
        dbInstance.onUpgrade(dbInstance.getWritableDatabase(), 2, 3);
        assertEquals(2, addEvent("test_upgrade"));
        assertEquals(2, insertOrReplaceKeyValue(key, value));
        assertEquals(1, addIdentify("test_upgrade"));
        assertEquals(1, insertOrReplaceKeyLongValue(key, longValue));
    }

    @Test
    public void testUpgradeVersion1ToVersion3() {
        // store table doesn't exist in v1, insert will fail
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.STORE_TABLE_NAME);
        String key = "test_key";
        String value = "test_value";
        assertEquals(-1, insertOrReplaceKeyValue(key, value));

        // identify table doesn't exist in v1, insert will fail
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.IDENTIFY_TABLE_NAME);
        assertEquals(-1, addIdentify("test_upgrade"));

        // long store table doesn't exist in v1, insert will fail
        Long longValue = 1L;
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.LONG_STORE_TABLE_NAME);
        assertEquals(-1, insertOrReplaceKeyLongValue(key, longValue));

        // only event inserts will work
        assertEquals(1, addEvent("test_upgrade"));

        // after v3 upgrade, can insert into store and identify tables
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.STORE_TABLE_NAME);
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.IDENTIFY_TABLE_NAME);
        dbInstance.getWritableDatabase().execSQL(
                "DROP TABLE IF EXISTS " + DatabaseHelper.LONG_STORE_TABLE_NAME);
        dbInstance.onUpgrade(dbInstance.getWritableDatabase(), 1, 3);
        assertEquals(2, addEvent("test_upgrade"));
        assertEquals(1, insertOrReplaceKeyValue(key, value));
        assertEquals(1, addIdentify("test_upgrade"));
        assertEquals(1, insertOrReplaceKeyLongValue(key, longValue));
    }

    @Test
    public void testInsertOrReplaceKeyValue() {
        String key = "test_key";
        String value1 = "test_value1";
        String value2 = "test_value2";
        assertEquals(null, getValue(key));

        insertOrReplaceKeyValue(key, value1);
        assertEquals(value1, getValue(key));

        insertOrReplaceKeyValue(key, value2);
        assertEquals(value2, getValue(key));
    }

    @Test
    public void testInsertOrReplaceKeyLongValue() {
        String key = "test_key";
        Long value1 = 1L;
        Long value2 = 2L;
        assertEquals(null, getLongValue(key));

        insertOrReplaceKeyLongValue(key, value1);
        assertEquals(value1, getLongValue(key));

        insertOrReplaceKeyLongValue(key, value2);
        assertEquals(value2, getLongValue(key));
    }

    @Test
    public void testInsertNullValues() {
        String key = "test_key";

        assertNull(getValue(key));
        insertOrReplaceKeyValue(key, "test");
        assertEquals(getValue(key), "test");
        insertOrReplaceKeyValue(key, null);
        assertNull(getValue(key));

        assertNull(getLongValue(key));
        insertOrReplaceKeyLongValue(key, 15L);
        assertEquals((long)getLongValue(key), 15L);
        insertOrReplaceKeyLongValue(key, null);
        assertNull(getValue(key));
    }

    @Test
    public void testAddEvent() {
        assertEquals(1, addEvent("test_add_event"));
        assertEquals(1, getLastUnsentEvent().optLong("event_id"));
        assertEquals(2, addEvent("test_add_event"));
        assertEquals(2, getLastUnsentEvent().optLong("event_id"));
        assertEquals(3, addEvent("test_add_event"));
        assertEquals(3, getLastUnsentEvent().optLong("event_id"));
    }

    @Test
    public void testAddIdentify() {
        assertEquals(1, addIdentify("test_add_identify"));
        assertEquals(1, getLastUnsentIdentify().optLong("event_id"));
        assertEquals(2, addIdentify("test_add_identify"));
        assertEquals(2, getLastUnsentIdentify().optLong("event_id"));
        assertEquals(3, addIdentify("test_add_identify"));
        assertEquals(3, getLastUnsentIdentify().optLong("event_id"));
    }

    @Test
     public void testGetEvents() {
        assertEquals(1, addEvent("test_get_events_1"));
        assertEquals(2, addEvent("test_get_events_2"));
        assertEquals(3, addEvent("test_get_events_3"));
        assertEquals(4, addEvent("test_get_events_4"));
        assertEquals(5, addEvent("test_get_events_5"));

        try {
            List<JSONObject> events;
            assertEquals(5, dbInstance.getEventCount());

            events = dbInstance.getEvents(-1, -1);
            assertEquals(5, events.size());
            assertEquals(1, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_1", (events.get(0)).getString("event_type"));

            events = dbInstance.getEvents(1, -1);
            assertEquals(1, events.size());

            events = dbInstance.getEvents(5, -1);
            assertEquals(5, events.size());
            assertEquals(5, (events.get(4)).getLong("event_id"));
            assertEquals("test_get_events_5", (events.get(4)).getString("event_type"));

            events = dbInstance.getEvents(-1, 0);
            assertEquals(0, events.size());

            events = dbInstance.getEvents(-1, 1);
            assertEquals(1, events.size());
            assertEquals(1, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_1", (events.get(0)).getString("event_type"));

            events = dbInstance.getEvents(5, 1);
            assertEquals(1, events.size());
            assertEquals(1, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_1", (events.get(0)).getString("event_type"));

            dbInstance.removeEvent(1);
            events = dbInstance.getEvents(5, 1);
            assertEquals(1, events.size());
            assertEquals(2, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_2", (events.get(0)).getString("event_type"));

            dbInstance.removeEvents(3);
            events = dbInstance.getEvents(5, 1);
            assertEquals(1, events.size());
            assertEquals(4, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_4", (events.get(0)).getString("event_type"));

        } catch (JSONException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testGetIdentifys() {
        assertEquals(1, addIdentify("test_get_identifys_1"));
        assertEquals(2, addIdentify("test_get_identifys_2"));
        assertEquals(3, addIdentify("test_get_identifys_3"));
        assertEquals(4, addIdentify("test_get_identifys_4"));
        assertEquals(5, addIdentify("test_get_identifys_5"));

        try {
            List<JSONObject> events;
            assertEquals(5, dbInstance.getIdentifyCount());

            events = dbInstance.getIdentifys(-1, -1);
            assertEquals(5, events.size());
            assertEquals(1, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_identifys_1", (events.get(0)).getString("event_type"));

            events = dbInstance.getIdentifys(1, -1);
            assertEquals(1, events.size());

            events = dbInstance.getIdentifys(5, -1);
            assertEquals(5, events.size());
            assertEquals(5, (events.get(4)).getLong("event_id"));
            assertEquals("test_get_identifys_5", (events.get(4)).getString("event_type")
            );

            events = dbInstance.getIdentifys(-1, 0);
            assertEquals(0, events.size());

            events = dbInstance.getIdentifys(-1, 1);
            assertEquals(1, events.size());
            assertEquals(1, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_identifys_1", (events.get(0)).getString("event_type"));

            events = dbInstance.getIdentifys(5, 1);
            assertEquals(1, events.size());
            assertEquals(1, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_identifys_1", (events.get(0)).getString("event_type"));

            dbInstance.removeIdentify(1);
            events = dbInstance.getIdentifys(5, 1);
            assertEquals(1, events.size());
            assertEquals(2, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_identifys_2", (events.get(0)).getString("event_type")
            );

            dbInstance.removeIdentifys(3);
            events = dbInstance.getIdentifys(5, 1);
            assertEquals(1, events.size());
            assertEquals(4, (events.get(0)).getLong("event_id"));
            assertEquals("test_get_identifys_4", (events.get(0)).getString("event_type")
            );
        } catch (JSONException e) {
            fail(e.toString());
        }
    }

    @Test
     public void testGetEventCount() {
        assertEquals(1, addEvent("test_get_event_count_1"));
        assertEquals(2, addEvent("test_get_event_count_2"));
        assertEquals(3, addEvent("test_get_event_count_3"));
        assertEquals(4, addEvent("test_get_event_count_4"));
        assertEquals(5, addEvent("test_get_event_count_5"));

        assertEquals(5, dbInstance.getEventCount());

        dbInstance.removeEvent(1);
        assertEquals(4, dbInstance.getEventCount());

        dbInstance.removeEvents(3);
        assertEquals(2, dbInstance.getEventCount());

        dbInstance.removeEvents(10);
        assertEquals(0, dbInstance.getEventCount());
    }

    @Test
    public void testGetIdentifyCount() {
        assertEquals(1, addIdentify("test_get_identify_count_1"));
        assertEquals(2, addIdentify("test_get_identify_count_2"));
        assertEquals(3, addIdentify("test_get_identify_count_3"));
        assertEquals(4, addIdentify("test_get_identify_count_4"));
        assertEquals(5, addIdentify("test_get_identify_count_5"));

        assertEquals(5, dbInstance.getIdentifyCount());

        dbInstance.removeIdentify(1);
        assertEquals(4, dbInstance.getIdentifyCount());

        dbInstance.removeIdentifys(3);
        assertEquals(2, dbInstance.getIdentifyCount());

        dbInstance.removeIdentifys(10);
        assertEquals(0, dbInstance.getIdentifyCount());
    }

    @Test
    public void testGetNthEventId() {
        assertEquals(1, addEvent("test_get_nth_event_id_1"));
        assertEquals(2, addEvent("test_get_nth_event_id_2"));
        assertEquals(3, addEvent("test_get_nth_event_id_3"));
        assertEquals(4, addEvent("test_get_nth_event_id_4"));
        assertEquals(5, addEvent("test_get_nth_event_id_5"));

        assertEquals(1, dbInstance.getNthEventId(0));
        assertEquals(1, dbInstance.getNthEventId(1));
        assertEquals(2, dbInstance.getNthEventId(2));
        assertEquals(3, dbInstance.getNthEventId(3));
        assertEquals(4, dbInstance.getNthEventId(4));
        assertEquals(5, dbInstance.getNthEventId(5));

        dbInstance.removeEvent(1);
        assertEquals(2, dbInstance.getNthEventId(1));

        dbInstance.removeEvents(3);
        assertEquals(4, dbInstance.getNthEventId(1));

        dbInstance.removeEvents(10);
        assertEquals(-1, dbInstance.getNthEventId(1));
    }

    @Test
    public void testGetNthIdentifyId() {
        assertEquals(1, addIdentify("test_get_nth_identify_id_1"));
        assertEquals(2, addIdentify("test_get_nth_identify_id_2"));
        assertEquals(3, addIdentify("test_get_nth_identify_id_3"));
        assertEquals(4, addIdentify("test_get_nth_identify_id_4"));
        assertEquals(5, addIdentify("test_get_nth_identify_id_5"));

        assertEquals(1, dbInstance.getNthIdentifyId(0));
        assertEquals(1, dbInstance.getNthIdentifyId(1));
        assertEquals(2, dbInstance.getNthIdentifyId(2));
        assertEquals(3, dbInstance.getNthIdentifyId(3));
        assertEquals(4, dbInstance.getNthIdentifyId(4));
        assertEquals(5, dbInstance.getNthIdentifyId(5));

        dbInstance.removeIdentify(1);
        assertEquals(2, dbInstance.getNthIdentifyId(1));

        dbInstance.removeIdentifys(3);
        assertEquals(4, dbInstance.getNthIdentifyId(1));

        dbInstance.removeIdentifys(10);
        assertEquals(-1, dbInstance.getNthIdentifyId(1));
    }

    @Test
    public void testNoConflictBetweenEventsAndIdentifys() {
        assertEquals(1, addEvent("test_add_event_id_1"));
        assertEquals(2, addEvent("test_add_event_id_2"));
        assertEquals(3, addEvent("test_add_event_id_3"));
        assertEquals(4, addEvent("test_add_event_id_4"));
        assertEquals(4, dbInstance.getEventCount());
        assertEquals(0, dbInstance.getIdentifyCount());

        assertEquals(1, addIdentify("test_add_identify_id_1"));
        assertEquals(2, addIdentify("test_add_identify_id_2"));
        assertEquals(4, dbInstance.getEventCount());
        assertEquals(2, dbInstance.getIdentifyCount());

        dbInstance.removeEvent(1);
        assertEquals(3, dbInstance.getEventCount());
        assertEquals(2, dbInstance.getIdentifyCount());

        dbInstance.removeIdentify(1);
        assertEquals(3, dbInstance.getEventCount());
        assertEquals(1, dbInstance.getIdentifyCount());

        dbInstance.removeEvents(4);
        assertEquals(0, dbInstance.getEventCount());
        assertEquals(1, dbInstance.getIdentifyCount());
    }
}

