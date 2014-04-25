package com.amplitude.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.util.Pair;

class DatabaseHelper extends SQLiteOpenHelper {

	private static DatabaseHelper instance;
	static final String TAG = "com.amplitude.api.AmplitudeDatabaseHelper";

	static DatabaseHelper getDatabaseHelper(Context context) {
		if (instance == null) {
			instance = new DatabaseHelper(context);
		}
		return instance;
	}

	private DatabaseHelper(Context context) {
		super(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// INTEGER PRIMARY KEY AUTOINCREMENT guarantees that all generated
		// values
		// for the field will be monotonically increasing and unique over the
		// lifetime of the table, even if rows get removed
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Constants.EVENT_TABLE_NAME
				+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, " + Constants.EVENT_FIELD + " TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

	long addEvent(String event) {
		synchronized (this) {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put(Constants.EVENT_FIELD, event);
			long result = db.insert(Constants.EVENT_TABLE_NAME, null, contentValues);
			if (result == -1) {
				Log.w(TAG, "Insert failed");
			}
			db.close();
			return result;
		}
	}

	long getNumberRows() {
		SQLiteDatabase db = getWritableDatabase();
		String query = "SELECT COUNT(*) FROM " + Constants.EVENT_TABLE_NAME;
		SQLiteStatement statement = db.compileStatement(query);
		long numberRows = statement.simpleQueryForLong();
		db.close();
		return numberRows;
	}

	Pair<Long, JSONArray> getEvents(long lessThanId, int limit) throws JSONException {

		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = db.query(Constants.EVENT_TABLE_NAME, Constants.TABLE_FIELD_NAMES,
				lessThanId >= 0 ? Constants.ID_FIELD + " < " + lessThanId : null,
				null, null, null, Constants.ID_FIELD + " ASC", limit >= 0 ? "" + limit : null);

		long maxId = -1;
		JSONArray events = new JSONArray();

		while (cursor.moveToNext()) {
			long eventId = cursor.getLong(0);
			String event = cursor.getString(1);

			JSONObject obj = new JSONObject(event);
			obj.put("event_id", eventId);
			events.put(obj);

			maxId = eventId;
		}

		cursor.close();
		db.close();
		return new Pair<Long, JSONArray>(maxId, events);
	}

	long getNthEventId(long n) {
		SQLiteDatabase db = getWritableDatabase();
		String query = "SELECT " + Constants.ID_FIELD + " FROM " + Constants.EVENT_TABLE_NAME
				+ " LIMIT 1 OFFSET " + (n - 1);
		SQLiteStatement statement = db.compileStatement(query);
		long nthEventId = -1;
		try {
			nthEventId = statement.simpleQueryForLong();
		} catch (SQLiteDoneException e) {
		}
		db.close();
		return nthEventId;
	}

	void removeEvents(long maxId) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(Constants.EVENT_TABLE_NAME, Constants.ID_FIELD + " <= " + maxId, null);
		db.close();
	}

	void removeEvent(long id) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(Constants.EVENT_TABLE_NAME, Constants.ID_FIELD + " = " + id, null);
		db.close();
	}

}
