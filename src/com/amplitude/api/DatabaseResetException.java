package com.amplitude.api;

/**
 * This is thrown by Amplitude's DatabaseHelper when it encounters SQLiteExceptions and
 * StackOverflowErrors. The helper recovers by deleting the database file and restarting
 * from scratch. This clears all events and metadata tables, need to communicate back to
 * AmplitudeClient when this happens so it can re-insert the metadata (like device id, session id).
 */

public class DatabaseResetException extends Exception {
    public DatabaseResetException(String description) { super(description); }
}
