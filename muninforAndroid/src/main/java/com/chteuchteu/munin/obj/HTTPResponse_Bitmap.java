package com.chteuchteu.munin.obj;

import android.graphics.Bitmap;

/**
 * Object returned from an image (bitmap/svg) download operation
 */
public class HTTPResponse_Bitmap extends HTTPResponse {
	private Bitmap bitmap;

	public HTTPResponse_Bitmap() {
		super();
	}

	public void setBitmap(Bitmap val) { this.bitmap = val; }

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

	public Bitmap getBitmap() { return this.bitmap; }
}
