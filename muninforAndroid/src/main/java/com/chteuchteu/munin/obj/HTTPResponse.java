package com.chteuchteu.munin.obj;

/**
 * Object returned from a network operation
 */
public class HTTPResponse {
	public String html;
	public int responseCode;
	public String responseReason;
	public String header_wwwauthenticate;
	public boolean timeout;
	public ConnectionType connectionType;

	/**
	 * NORMAL: http
	 * INSECURE: https with certificate error
	 * SECURE: https with valid certificate
	 */
	public enum ConnectionType {
		NORMAL, INSECURE, SECURE;
		public static ConnectionType defaultConnectionType() { return NORMAL; }
	}
	
	public HTTPResponse() {
		this.html = "";
		this.responseCode = -1;
		this.responseReason = "";
		this.header_wwwauthenticate = "";
		this.timeout = false;
		this.connectionType = ConnectionType.defaultConnectionType();
	}
}