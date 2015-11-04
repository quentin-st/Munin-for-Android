package com.chteuchteu.munin.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * When using ViewPager, a random crash may randomly happen.
 * Let's try catch that exception.
 */
public class HackyViewPager extends android.support.v4.view.ViewPager {
	public HackyViewPager(Context context) {
		super(context);
	}

	public HackyViewPager(Context context, AttributeSet attrs) {
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
