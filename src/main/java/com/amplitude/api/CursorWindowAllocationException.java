package com.amplitude.api;

/**
 * This is Amplitude's substitute for android.database.CursorWindowAllocationException.
 * Android's CursorWindow will throw that exception, but Android does not allow you to import
 * the exception class directly to catch it. This is Amplitude's stand-in for that class.
 *
 * @hide
 */
public class CursorWindowAllocationException extends RuntimeException {
    public CursorWindowAllocationException(String description) {
        super(description);
    }
}
