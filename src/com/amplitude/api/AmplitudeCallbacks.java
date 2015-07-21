package com.amplitude.api;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class AmplitudeCallbacks implements Application.ActivityLifecycleCallbacks {

    private AmplitudeClient client;

    public AmplitudeCallbacks(AmplitudeClient client) {
        this.client = client;
        client.useAccurateTracking(true);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {
        client.refreshSessionTime(System.currentTimeMillis());
        client.setInForeground(false);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        client.startNewSessionIfNeeded(System.currentTimeMillis());
        client.setInForeground(true);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outstate) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

}
