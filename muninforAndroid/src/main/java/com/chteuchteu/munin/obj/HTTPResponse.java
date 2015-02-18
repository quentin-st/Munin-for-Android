package com.chteuchteu.munin.obj;

import java.net.HttpURLConnection;

/**
 * Object returned from a network operation
 */
public class HTTPResponse {
	protected String requestUrl;
	protected String html;
	protected int responseCode;
	protected String responsePhrase;
	protected String header_wwwauthenticate;
	protected boolean timeout;
	protected ConnectionType connectionType;
	/**
	 * Contains the lastUrl (different from request url if redirected)
	 */
	protected String lastUrl;

	public static final int UnknownHostExceptionError = -5;
	public static final int UnknownError = -1;

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
		this.requestUrl = "";
		this.html = "";
		this.responseCode = -1;
		this.responsePhrase = "";
		this.header_wwwauthenticate = "";
		this.timeout = false;
		this.connectionType = ConnectionType.defaultConnectionType();
		this.lastUrl = "";
	}

	public void setRequestUrl(String val) { this.requestUrl = val; }
	public void setHtml(String val) { this.html = val; }
	public void setResponseCode(int val) { this.responseCode = val; }
	public void setResponsePhrase(String val) { this.responsePhrase = val; }
	public void setTimeout(boolean val) { this.timeout = val; }
	public void setAuthenticateHeader(String val) { this.header_wwwauthenticate = val; }
	public void setConnectionType(ConnectionType val) { this.connectionType = val; }
	public void setLastUrl(String val) { this.lastUrl = val; }

	public String getRequestUrl() { return this.requestUrl; }
	public String getHtml() { return this.html; }
	public int getResponseCode() { return this.responseCode; }
	public String getResponsePhrase() { return this.responsePhrase; }
	public boolean getTimeout() { return this.timeout; }
	public ConnectionType getConnectionType() { return this.connectionType; }
	public String getLastUrl() { return this.lastUrl; }

	public boolean hasSucceeded() {
		return !this.timeout && this.responseCode == HttpURLConnection.HTTP_OK;
	}
	public boolean wasRedirected() { return !this.requestUrl.equals(this.lastUrl); }


	// Logging
	private long startTime;
	private long endTime;
	public void begin() { this.startTime = System.currentTimeMillis(); }
	public void end() { this.endTime = System.currentTimeMillis(); }
	public long getExecutionTime() { return this.endTime - this.startTime; }
}