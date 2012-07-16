package com.sonalight.analytics.api;

import android.os.Handler;
import android.os.Looper;

public class LogThread extends Thread {

  public static LogThread instance = new LogThread();

  static {
    instance.start();
  }

  private Handler handler;

  private LogThread() {
  }

  @Override
  public void run() {
    Looper.prepare();
    synchronized (instance) {
      handler = new Handler();
      instance.notifyAll();
    }
    Looper.loop();
  }

  public static void post(Runnable r) {
    while (instance.handler == null) {
      synchronized (instance) {
        try {
          instance.wait();
        } catch (InterruptedException e) {
        }
      }
    }
    instance.handler.post(r);
  }

  public static void postDelayed(Runnable r, long delayMillis) {
    while (instance.handler == null) {
      synchronized (instance) {
        try {
          instance.wait();
        } catch (InterruptedException e) {
        }
      }
    }
    instance.handler.postDelayed(r, delayMillis);
  }

  public static void removeCallbacks(Runnable r) {
    instance.handler.removeCallbacks(r);
  }
}
