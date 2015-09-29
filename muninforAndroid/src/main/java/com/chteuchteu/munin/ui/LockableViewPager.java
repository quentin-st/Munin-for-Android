package com.chteuchteu.munin.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class LockableViewPager extends HackyViewPager {
    private boolean isPagingEnabled;

    public LockableViewPager(Context context) {
        super(context);
        this.isPagingEnabled = true;
    }

    public LockableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.isPagingEnabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.isPagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.isPagingEnabled && super.onInterceptTouchEvent(event);
    }

    public void setPagingEnabled(boolean enabled) {
        this.isPagingEnabled = enabled;
    }
}
