package com.giraffegraph.api;

import android.os.Handler;
import android.os.Looper;

class GGLogThread extends Thread {

  static GGLogThread instance = new GGLogThread();

  static {
    instance.start();
  }

  private Handler handler;

  private GGLogThread() {
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

  static void post(Runnable r) {
    waitForHandlerInitialization();
    instance.handler.post(r);
  }

  static void postDelayed(Runnable r, long delayMillis) {
    waitForHandlerInitialization();
    instance.handler.postDelayed(r, delayMillis);
  }

  static void removeCallbacks(Runnable r) {
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
