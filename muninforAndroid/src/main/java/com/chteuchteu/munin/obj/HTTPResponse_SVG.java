package com.chteuchteu.munin.obj;

import com.larvalabs.svgandroid.SVG;

/**
 * Object returned from an SVG download operation
 */
public class HTTPResponse_SVG extends HTTPResponse_Image {
	public HTTPResponse_SVG() {
		super();
	}

	public void setSVG(SVG val) { this.image = val; }
	public SVG getSVG() { return (SVG) this.image; }
}
