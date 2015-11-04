package com.chteuchteu.munin.ui;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class HackyDrawerLayout extends DrawerLayout {
	public HackyDrawerLayout(Context context) {
		super(context);
	}

	public HackyDrawerLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		try {
			return super.onTouchEvent(ev);
		} catch (IllegalArgumentException ex) {
			//ex.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		try {
			return super.onInterceptTouchEvent(ev);
		} catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}
}
