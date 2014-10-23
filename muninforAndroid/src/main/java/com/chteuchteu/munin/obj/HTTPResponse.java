package com.chteuchteu.munin.obj;

public class HTTPResponse {
	public String html;
	public int responseCode;
	public String responseReason;
	public String header_wwwauthenticate;
	public boolean timeout;
	
	public HTTPResponse() {
		this.html = "";
		this.responseCode = -1;
		this.responseReason = "";
		this.header_wwwauthenticate = "";
		this.timeout = false;
	}
}