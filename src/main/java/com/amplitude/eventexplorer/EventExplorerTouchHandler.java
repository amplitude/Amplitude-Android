package com.amplitude.eventexplorer;

import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class EventExplorerTouchHandler implements View.OnTouchListener {
    private int initialX;
    private float initialTouchX;

    private int initialY;
    private float initialTouchY;

    private WindowManager.LayoutParams layoutParams;
    private WindowManager windowManager;
    private String instanceName;

    EventExplorerTouchHandler(WindowManager windowManager,
                              WindowManager.LayoutParams layoutParams,
                              String instanceName) {
        this.layoutParams = layoutParams;
        this.windowManager = windowManager;
        this.instanceName = instanceName;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialY = layoutParams.y;
                initialX = layoutParams.x;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                windowManager.updateViewLayout(v, layoutParams);
                return true;
            case MotionEvent.ACTION_UP:
                float endX = event.getRawX();
                float endY = event.getRawY();
                if (isAClick(initialTouchX, endX, initialTouchY, endY)) {
                    v.performClick();

                    Intent intent = new Intent(v.getContext(), EventExplorerInfoActivity.class);
                    intent.putExtra("instanceName", this.instanceName);
                    v.getContext().startActivity(intent);
                }
                return true;
        }
        return false;
    }

    private boolean isAClick(float startX, float endX, float startY, float endY) {
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);
        return !(differenceX > 5 || differenceY > 5);
    }
}
