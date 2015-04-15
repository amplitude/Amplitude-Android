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

    private static final String EVENT_TABLE_NAME = "events";
    private static final String ID_FIELD = "id";
    private static final String EVENT_FIELD = "event";

    private static final String CREATE_EVENTS_TABLE = "CREATE TABLE IF NOT EXISTS "
            + EVENT_TABLE_NAME + " (" + ID_FIELD + " INTEGER PRIMARY KEY AUTOINCREMENT, "
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
        // INTEGER PRIMARY KEY AUTOINCREMENT guarantees that all generated values
        // for the field will be monotonically increasing and unique over the
        // lifetime of the table, even if rows get removed
        db.execSQL(CREATE_EVENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + EVENT_TABLE_NAME);
        db.execSQL(CREATE_EVENTS_TABLE);
    }

    synchronized long addEvent(String event) {
        long result = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(EVENT_FIELD, event);
            result = db.insert(EVENT_TABLE_NAME, null, contentValues);
            if (result == -1) {
                Log.w(TAG, "Insert failed");
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "addEvent failed", e);
            // Not much we can do, just start fresh
            delete();
        } finally {
            close();
        }
        return result;
    }

    synchronized Pair<Long, JSONArray> getEvents(long lessThanId, int limit) throws JSONException {
        long maxId = -1;
        JSONArray events = new JSONArray();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(EVENT_TABLE_NAME, new String[] { ID_FIELD, EVENT_FIELD },
                    lessThanId >= 0 ? ID_FIELD + " < " + lessThanId : null, null, null, null,
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
            Log.e(TAG, "getEvents failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return new Pair<Long, JSONArray>(maxId, events);
    }

    synchronized long getEventCount() {
        long numberRows = 0;
        SQLiteStatement statement = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String query = "SELECT COUNT(*) FROM " + EVENT_TABLE_NAME;
            statement = db.compileStatement(query);
            numberRows = statement.simpleQueryForLong();
        } catch (SQLiteException e) {
            Log.e(TAG, "getNumberRows failed", e);
        } finally {
            if (statement != null) {
                statement.close();
            }
            close();
        }
        return numberRows;
    }

    synchronized long getNthEventId(long n) {
        long nthEventId = -1;
        SQLiteStatement statement = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String query = "SELECT " + ID_FIELD + " FROM " + EVENT_TABLE_NAME + " LIMIT 1 OFFSET "
                    + (n - 1);
            statement = db.compileStatement(query);
            nthEventId = -1;
            try {
                nthEventId = statement.simpleQueryForLong();
            } catch (SQLiteDoneException e) {
                Log.w(TAG, e);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "getNthEventId failed", e);
        } finally {
            if (statement != null) {
                statement.close();
            }
            close();
        }
        return nthEventId;
    }

    synchronized void removeEvents(long maxId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(EVENT_TABLE_NAME, ID_FIELD + " <= " + maxId, null);
        } catch (SQLiteException e) {
            Log.e(TAG, "removeEvents failed", e);
        } finally {
            close();
        }
    }

    synchronized void removeEvent(long id) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(EVENT_TABLE_NAME, ID_FIELD + " = " + id, null);
        } catch (SQLiteException e) {
            Log.e(TAG, "removeEvent failed", e);
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
