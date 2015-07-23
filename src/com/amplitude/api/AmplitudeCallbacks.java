package com.amplitude.api;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

public class AmplitudeCallbacks implements Application.ActivityLifecycleCallbacks {

    public static final String TAG = "com.amplitude.api.AmplitudeCallbacks";
    private static final String NULLMSG = "Need to initialize AmplitudeCallbacks with AmplitudeClient instance";

    private AmplitudeClient clientInstance = null;

    public AmplitudeCallbacks(AmplitudeClient clientInstance) {
        if (clientInstance == null) {
            Log.e(TAG, NULLMSG);
            return;
        }

        this.clientInstance = clientInstance;
        clientInstance.useAccurateTracking(true);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {
        if (clientInstance == null) {
            Log.e(TAG, NULLMSG);
            return;
        }

        Log.d(TAG, "onActivityPaused");
        clientInstance.refreshSessionTime(getCurrentTimeMillis());
        clientInstance.setInForeground(false);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (clientInstance == null) {
            Log.e(TAG, NULLMSG);
            return;
        }

        Log.d(TAG, "onActivityResumed");
        clientInstance.startNewSessionIfNeeded(getCurrentTimeMillis(), false);
        clientInstance.setInForeground(true);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outstate) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}
