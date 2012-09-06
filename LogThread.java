package com.giraffegraph.api;

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
    waitForHandlerInitialization();
    instance.handler.post(r);
  }

  public static void postDelayed(Runnable r, long delayMillis) {
    waitForHandlerInitialization();
    instance.handler.postDelayed(r, delayMillis);
  }

  public static void removeCallbacks(Runnable r) {
    waitForHandlerInitialization();
    instance.handler.removeCallbacks(r);
  }
  
  private static void waitForHandlerInitialization() {
    while (instance.handler == null) {
      synchronized (instance) {
        try {
          instance.wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }
}
