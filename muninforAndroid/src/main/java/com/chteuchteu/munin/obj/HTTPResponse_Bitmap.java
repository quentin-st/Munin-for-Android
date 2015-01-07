package com.chteuchteu.munin.obj;

import android.graphics.Bitmap;

/**
 * Object returned from a bitmap download operation
 */
public class HTTPResponse_Bitmap extends HTTPResponse {
	public static final int UnknownHostExceptionError = -5;
	public static final int UnknownError = -1;

	public Bitmap bitmap;

	public HTTPResponse_Bitmap() {
		super();
	}

}
