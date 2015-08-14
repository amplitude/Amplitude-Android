package com.amplitude.api;

import android.database.sqlite.SQLiteDatabase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DatabaseHelperTest extends BaseTest {

    protected DatabaseHelper dbInstance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        dbInstance = DatabaseHelper.getDatabaseHelper(context);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        dbInstance = null;
    }

    protected long addEvent(String type) {
        return addEvent(type, new JSONObject());
    }

    protected long addEvent(String type, JSONObject props) {
        try {
            props.put("event_type", type);
            return dbInstance.addEvent(props.toString());
        } catch (JSONException e) {
            fail(e.toString());
        }
        return -1;
    }

    protected long insertOrReplaceKeyValue(String key, String value) {
        return dbInstance.insertOrReplaceKeyValue(key, value);
    }

    protected String getValue(String key) {
        return dbInstance.getValue(key);
    }

    @Test
    public void testCreate() {
        dbInstance.onCreate(dbInstance.getWritableDatabase());
        assertEquals(1, insertOrReplaceKeyValue("test_key", "test_value"));
        assertEquals(1, addEvent("test_create"));
    }

    @Test
    public void testUpgrade() {
        SQLiteDatabase db = dbInstance.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseHelper.STORE_TABLE_NAME);

        // store table doesn't exist in v1, insert will fail
        String key = "test_key";
        String value = "test_value";
        assertEquals(-1, insertOrReplaceKeyValue(key, value));
        assertEquals(1, addEvent("test_upgrade"));

        // after upgrade, can insert
        dbInstance.onUpgrade(dbInstance.getWritableDatabase(), 1, 2);
        assertEquals(2, addEvent("test_upgrade"));
        assertEquals(1, insertOrReplaceKeyValue(key, value));
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
    public void testAddEvent() {
        assertEquals(1, addEvent("test_add_event"));
        assertEquals(1, getLastUnsentEvent().optLong("event_id"));
        assertEquals(2, addEvent("test_add_event"));
        assertEquals(2, getLastUnsentEvent().optLong("event_id"));
        assertEquals(3, addEvent("test_add_event"));
        assertEquals(3, getLastUnsentEvent().optLong("event_id"));
    }

    @Test
    public void testGetEvents() {
        assertEquals(1, addEvent("test_get_events_1"));
        assertEquals(2, addEvent("test_get_events_2"));
        assertEquals(3, addEvent("test_get_events_3"));
        assertEquals(4, addEvent("test_get_events_4"));
        assertEquals(5, addEvent("test_get_events_5"));

        try {
            JSONArray events;
            assertEquals(5, (long)dbInstance.getEvents(-1, -1).first);

            events = dbInstance.getEvents(-1, -1).second;
            assertEquals(5, events.length());
            assertEquals(1, ((JSONObject)events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_1", ((JSONObject)events.get(0)).getString("event_type"));

            events = dbInstance.getEvents(1, -1).second;
            assertEquals(1, events.length());

            events = dbInstance.getEvents(5, -1).second;
            assertEquals(5, events.length());
            assertEquals(5, ((JSONObject)events.get(4)).getLong("event_id"));
            assertEquals("test_get_events_5", ((JSONObject)events.get(4)).getString("event_type"));

            events = dbInstance.getEvents(-1, 0).second;
            assertEquals(0, events.length());

            events = dbInstance.getEvents(-1, 1).second;
            assertEquals(1, events.length());
            assertEquals(1, ((JSONObject)events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_1", ((JSONObject)events.get(0)).getString("event_type"));

            events = dbInstance.getEvents(5, 1).second;
            assertEquals(1, events.length());
            assertEquals(1, ((JSONObject)events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_1", ((JSONObject)events.get(0)).getString("event_type"));

            dbInstance.removeEvent(1);
            events = dbInstance.getEvents(5, 1).second;
            assertEquals(1, events.length());
            assertEquals(2, ((JSONObject)events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_2", ((JSONObject)events.get(0)).getString("event_type"));

            dbInstance.removeEvents(3);
            events = dbInstance.getEvents(5, 1).second;
            assertEquals(1, events.length());
            assertEquals(4, ((JSONObject)events.get(0)).getLong("event_id"));
            assertEquals("test_get_events_4", ((JSONObject)events.get(0)).getString("event_type"));

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
}

