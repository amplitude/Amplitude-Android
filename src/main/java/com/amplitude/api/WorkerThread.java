package com.amplitude.api;

import android.os.Handler;
import android.os.HandlerThread;

public class WorkerThread extends HandlerThread {
	
	public WorkerThread(String name) {
		super(name);
	}

	private Handler handler;

	Handler getHandler() {
		return handler;
	}
	
	void post(Runnable r) {
		waitForInitialization();
		handler.post(r);
	}

	void postDelayed(Runnable r, long delayMillis) {
		waitForInitialization();
		handler.postDelayed(r, delayMillis);
	}

	void removeCallbacks(Runnable r) {
		waitForInitialization();
		handler.removeCallbacks(r);
	}

	private synchronized void waitForInitialization() {
		if (handler == null) {
			handler = new Handler(getLooper());
		}
	}
}
