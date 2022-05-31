package com.odysee.app.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class MediaRelativeLayout extends RelativeLayout {
    public GestureDetector gestureDetector;

    public MediaRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            return gestureDetector.onTouchEvent(ev);
        }
        return false;
    }
}
