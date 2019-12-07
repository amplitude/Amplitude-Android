package com.amplitude.api;

import android.util.Log;

public class AmplitudeLog {
    private volatile boolean enableLogging = true;
    private volatile int logLevel = Log.INFO; // default log level

    protected static AmplitudeLog instance = new AmplitudeLog();

    public static AmplitudeLog getLogger() {
        return instance;
    }

    private AmplitudeLog() {} // prevent instantiation

    AmplitudeLog setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
        return instance;
    }

    AmplitudeLog setLogLevel(int logLevel) {
        this.logLevel = logLevel;
        return instance;
    }

    int d(String tag, String msg) {
        if (enableLogging && logLevel <= Log.DEBUG) return Log.d(tag, msg);
        return 0;
    }

    int d(String tag, String msg, Throwable tr) {
        if (enableLogging && logLevel <= Log.DEBUG) return Log.d(tag, msg, tr);
        return 0;
    }

    int e(String tag, String msg) {
        if (enableLogging && logLevel <= Log.ERROR) return Log.e(tag, msg);
        return 0;
    }

    int e(String tag, String msg, Throwable tr) {
        if (enableLogging && logLevel <= Log.ERROR) return Log.e(tag, msg, tr);
        return 0;
    }

    String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    int i(String tag, String msg) {
        if (enableLogging && logLevel <= Log.INFO) return Log.i(tag, msg);
        return 0;
    }

    int i(String tag, String msg, Throwable tr) {
        if (enableLogging && logLevel <= Log.INFO) return Log.i(tag, msg, tr);
        return 0;
    }

    boolean isLoggable(String tag, int level) {
        return Log.isLoggable(tag, level);
    }

    int println(int priority, String tag, String msg) {
        return Log.println(priority, tag, msg);
    }

    int v(String tag, String msg) {
        if (enableLogging && logLevel <= Log.VERBOSE) return Log.v(tag, msg);
        return 0;
    }

    int v(String tag, String msg, Throwable tr) {
        if (enableLogging && logLevel <= Log.VERBOSE) return Log.v(tag, msg, tr);
        return 0;
    }

    int w(String tag, String msg) {
        if (enableLogging && logLevel <= Log.WARN) return Log.w(tag, msg);
        return 0;
    }

    int w(String tag, Throwable tr) {
        if (enableLogging && logLevel <= Log.WARN) return Log.w(tag, tr);
        return 0;
    }

    int w(String tag, String msg, Throwable tr) {
        if (enableLogging && logLevel <= Log.WARN) return Log.w(tag, msg, tr);
        return 0;
    }

    // wtf = What a Terrible Failure, logged at level ASSERT
    int wtf(String tag, String msg) {
        if (enableLogging && logLevel <= Log.ASSERT) return Log.wtf(tag, msg);
        return 0;
    }

    int wtf(String tag, Throwable tr) {
        if (enableLogging && logLevel <= Log.ASSERT) return Log.wtf(tag, tr);
        return 0;
    }

    int wtf(String tag, String msg, Throwable tr) {
        if (enableLogging && logLevel <= Log.ASSERT) return Log.wtf(tag, msg, tr);
        return 0;
    }
}
