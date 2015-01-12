package com.chteuchteu.munin.obj;

import android.graphics.Bitmap;

/**
 * Object returned from a bitmap download operation
 */
public class HTTPResponse_Bitmap extends HTTPResponse {
	private Bitmap bitmap;

	public HTTPResponse_Bitmap() {
		super();
	}

	public void setBitmap(Bitmap val) { this.bitmap = val; }
	public Bitmap getBitmap() { return this.bitmap; }

	/**
	 * Same as hasSuceeded(), but doesn't test if bitmap == null
	 */
	public boolean requestSucceeded() {
		return super.hasSucceeded();
	}

	@Override
	public boolean hasSucceeded() {
		return super.hasSucceeded() && this.bitmap != null;
	}
}
