package com.chteuchteu.munin.obj;

public class HTTPResponse {
	public String html;
	public int responseCode;
	public String responseReason;
	public String header_wwwauthenticate;
	public boolean timeout;
	
	public HTTPResponse() {
		html = "";
		responseCode = -1;
		responseReason = "";
		header_wwwauthenticate = "";
		timeout = false;
	}
	public HTTPResponse(String html, int responseCode) {
		html = "";
		responseCode = -1;
		responseReason = "";
		header_wwwauthenticate = "";
		this.html = html;
		this.responseCode = responseCode;
		timeout = false;
	}
}