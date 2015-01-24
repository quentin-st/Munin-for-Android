package com.chteuchteu.munin.obj;

import android.graphics.Bitmap;

/**
 * Object returned from a bitmap download operation
 */
public class HTTPResponse_Bitmap extends HTTPResponse_Image {
	public HTTPResponse_Bitmap() {
		super();
	}

	public Bitmap getBitmap() { return (Bitmap) this.image; }
	public void setBitmap(Bitmap val) { this.image = val; }
}
