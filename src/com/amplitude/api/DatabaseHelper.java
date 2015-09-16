package com.amplitude.api;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.util.Pair;

class DatabaseHelper extends SQLiteOpenHelper {

    static DatabaseHelper instance;
    private static final String TAG = "com.amplitude.api.DatabaseHelper";

    protected static final String STORE_TABLE_NAME = "store";
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    protected static final String EVENT_TABLE_NAME = "events";
    protected static final String IDENTIFY_TABLE_NAME = "identifys";
    private static final String ID_FIELD = "id";
    private static final String EVENT_FIELD = "event";

    private static final String CREATE_STORE_TABLE = "CREATE TABLE IF NOT EXISTS "
            + STORE_TABLE_NAME + " (" + KEY_FIELD + " TEXT PRIMARY KEY NOT NULL, "
            + VALUE_FIELD + " TEXT);";
    private static final String CREATE_EVENTS_TABLE = "CREATE TABLE IF NOT EXISTS "
            + EVENT_TABLE_NAME + " (" + ID_FIELD + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + EVENT_FIELD + " TEXT);";
    private static final String CREATE_IDENTIFYS_TABLE = "CREATE TABLE IF NOT EXISTS "
            + IDENTIFY_TABLE_NAME + " (" + ID_FIELD + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + EVENT_FIELD + " TEXT);";

    private File file;

    static synchronized DatabaseHelper getDatabaseHelper(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
        file = context.getDatabasePath(Constants.DATABASE_NAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_STORE_TABLE);
        // INTEGER PRIMARY KEY AUTOINCREMENT guarantees that all generated values
        // for the field will be monotonically increasing and unique over the
        // lifetime of the table, even if rows get removed
        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(CREATE_IDENTIFYS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion > newVersion) {
            Log.e(TAG, "onUpgrade() with invalid oldVersion and newVersion");
            resetDatabase(db);
            return;
        }

        switch (oldVersion) {

            case 1:
                db.execSQL(CREATE_STORE_TABLE);
                if (newVersion <= 2) {
                    break;
                }

            case 2:
                db.execSQL(CREATE_IDENTIFYS_TABLE);
                if (newVersion <= 3) {
                    break;
                }

            case 3:
                break;

            default:
                Log.e(TAG, "onUpgrade() with unknown oldVersion " + oldVersion);
                resetDatabase(db);
        }
    }

    private void resetDatabase(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + STORE_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + EVENT_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + IDENTIFY_TABLE_NAME);
        onCreate(db);
    }

    synchronized long insertOrReplaceKeyValue(String key, String value) {
        long result = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(KEY_FIELD, key);
            contentValues.put(VALUE_FIELD, value);
            result = db.insertWithOnConflict(
                    STORE_TABLE_NAME,
                    null,
                    contentValues,
                    SQLiteDatabase.CONFLICT_REPLACE
            );
            if (result == -1) {
                Log.w(TAG, "Insert failed");
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "insertOrReplaceKeyValue failed", e);
            // Not much we can do, just start fresh
            delete();
        } finally {
            close();
        }
        return result;
    }

    synchronized long addEvent(String event) {
        return addEventToTable(EVENT_TABLE_NAME, event);
    }

    synchronized long addIdentify(String identifyEvent) {
        return addEventToTable(IDENTIFY_TABLE_NAME, identifyEvent);
    }

    synchronized long addEventToTable(String table, String event) {
        long result = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(EVENT_FIELD, event);
            result = db.insert(table, null, contentValues);
            if (result == -1) {
                Log.w(TAG, String.format("Insert into %s failed", table));
            }
        } catch (SQLiteException e) {
            Log.e(TAG, String.format("addEvent to %s failed", table), e);
            // Not much we can do, just start fresh
            delete();
        } finally {
            close();
        }
        return result;
    }

    synchronized String getValue(String key) {
        String value = null;
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(
                    STORE_TABLE_NAME,
                    new String[]{KEY_FIELD, VALUE_FIELD},
                    KEY_FIELD + " = ?",
                    new String[]{key},
                    null, null, null, null
            );
            if (cursor.moveToFirst()) {
                value = cursor.getString(1);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "getValue failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return value;
    }

    synchronized Pair<Long, JSONArray> getEvents(long upToId, int limit) throws JSONException {
        return getEventsFromTable(EVENT_TABLE_NAME, upToId, limit);
    }

    synchronized Pair<Long, JSONArray> getIdentifys(long upToId, int limit) throws JSONException {
        return getEventsFromTable(IDENTIFY_TABLE_NAME, upToId, limit);
    }

    synchronized Pair<Long, JSONArray> getEventsFromTable(String table, long upToId, int limit) throws JSONException {
        long maxId = -1;
        JSONArray events = new JSONArray();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(table, new String[] { ID_FIELD, EVENT_FIELD },
                    upToId >= 0 ? ID_FIELD + " <= " + upToId : null, null, null, null,
                    ID_FIELD + " ASC", limit >= 0 ? "" + limit : null);

            while (cursor.moveToNext()) {
                long eventId = cursor.getLong(0);
                String event = cursor.getString(1);

                JSONObject obj = new JSONObject(event);
                obj.put("event_id", eventId);
                events.put(obj);

                maxId = eventId;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, String.format("getEvents from %s failed", table), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return new Pair<Long, JSONArray>(maxId, events);
    }

    synchronized long getEventCount() {
        return getEventCountFromTable(EVENT_TABLE_NAME);
    }

    synchronized long getIdentifyCount() {
        return getEventCountFromTable(IDENTIFY_TABLE_NAME);
    }

    synchronized long getEventCountFromTable(String table) {
        long numberRows = 0;
        SQLiteStatement statement = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String query = "SELECT COUNT(*) FROM " + table;
            statement = db.compileStatement(query);
            numberRows = statement.simpleQueryForLong();
        } catch (SQLiteException e) {
            Log.e(TAG, String.format("getNumberRows for %s failed", table), e);
        } finally {
            if (statement != null) {
                statement.close();
            }
            close();
        }
        return numberRows;
    }

    synchronized long getNthEventId(long n) {
        return getNthEventIdFromTable(EVENT_TABLE_NAME, n);
    }

    synchronized long getNthIdentifyId(long n) {
        return getNthEventIdFromTable(IDENTIFY_TABLE_NAME, n);
    }

    synchronized long getNthEventIdFromTable(String table, long n) {
        long nthEventId = -1;
        SQLiteStatement statement = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String query = "SELECT " + ID_FIELD + " FROM " + table + " LIMIT 1 OFFSET "
                    + (n - 1);
            statement = db.compileStatement(query);
            nthEventId = -1;
            try {
                nthEventId = statement.simpleQueryForLong();
            } catch (SQLiteDoneException e) {
                Log.w(TAG, e);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, String.format("getNthEventId from %s failed", table), e);
        } finally {
            if (statement != null) {
                statement.close();
            }
            close();
        }
        return nthEventId;
    }

    synchronized void removeEvents(long maxId) {
        removeEventsFromTable(EVENT_TABLE_NAME, maxId);
    }

    synchronized void removeIdentifys(long maxId) {
        removeEventsFromTable(IDENTIFY_TABLE_NAME, maxId);
    }

    synchronized void removeEventsFromTable(String table, long maxId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(table, ID_FIELD + " <= " + maxId, null);
        } catch (SQLiteException e) {
            Log.e(TAG, String.format("removeEvents from %s failed", table), e);
        } finally {
            close();
        }
    }

    synchronized void removeEvent(long id) {
        removeEventFromTable(EVENT_TABLE_NAME, id);
    }

    synchronized void removeIdentify(long id) {
        removeEventFromTable(IDENTIFY_TABLE_NAME, id);
    }

    synchronized void removeEventFromTable(String table, long id) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(table, ID_FIELD + " = " + id, null);
        } catch (SQLiteException e) {
            Log.e(TAG, String.format("removeEvent from %s failed", table), e);
        } finally {
            close();
        }
    }

    private void delete() {
        try {
            close();
            file.delete();
        } catch (SecurityException e) {
            Log.e(TAG, "delete failed", e);
        }
    }
}
