package com.amplitude.eventexplorer;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.amplitude.R;

public class EventExplorer {
    private String instanceName;
    private View bubbleView;

    public EventExplorer(String instanceName) {
        this.instanceName = instanceName;
    }

    public void show(final Activity rootActivity) {
        if (this.bubbleView == null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                final WindowManager windowManager = rootActivity.getWindowManager();
                final DisplayMetrics displayMetrics = new DisplayMetrics();

                if (windowManager.getDefaultDisplay() != null) {
                    windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                }

                final WindowManager.LayoutParams layoutParams
                        = prepareWindowManagerLayoutParams(rootActivity, displayMetrics);

                this.bubbleView = rootActivity.getLayoutInflater().inflate(R.layout.amp_bubble_view, null);

                windowManager.addView(this.bubbleView, layoutParams);

                this.bubbleView.setOnTouchListener(new EventExplorerTouchHandler(windowManager, layoutParams, this.instanceName));
            });
        }
    }

    private WindowManager.LayoutParams prepareWindowManagerLayoutParams(Context context,
                                                                        DisplayMetrics displayMetrics) {
        int navbarHeight = 0;
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            navbarHeight = resources.getDimensionPixelSize(resourceId);
        }

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        layoutParams.y = (displayMetrics.heightPixels - navbarHeight) / 2;
        layoutParams.x = (displayMetrics.widthPixels) / 2;

        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;

        return layoutParams;
    }
}
