package com.chteuchteu.munin;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * On a layout using PhotoView, zooming on the view makes the app crash
 * We have to catch the exception using this custom HackyDrawerLayout.
 * Used on Adapter_GraphView layout
 */
public class HackyDrawerLayout extends DrawerLayout {
	public HackyDrawerLayout(Context context) {
		super(context);
	}

	public HackyDrawerLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public HackyDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		try {
			return super.onInterceptTouchEvent(ev);
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}
}