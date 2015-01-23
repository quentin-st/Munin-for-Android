package com.chteuchteu.munin.obj;

/**
 * Object returned from an image (bitmap/svg) download operation
 */
public class HTTPResponse_Image extends HTTPResponse {
	protected Object image;

	public HTTPResponse_Image() {
		super();
	}

	public void setImage(Object val) { this.image = val; }

	/**
	 * Same as hasSuceeded(), but doesn't test if bitmap == null
	 */
	public boolean requestSucceeded() {
		return super.hasSucceeded();
	}

	@Override
	public boolean hasSucceeded() {
		return super.hasSucceeded() && this.image != null;
	}
}
