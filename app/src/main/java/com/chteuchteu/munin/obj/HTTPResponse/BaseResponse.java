package com.chteuchteu.munin.obj.HTTPResponse;

import com.chteuchteu.munin.hlpr.Exception.Http.HttpException;
import com.chteuchteu.munin.hlpr.Http.ResponseCode;

import java.net.HttpURLConnection;

public abstract class BaseResponse {
    protected String requestUrl;
    protected int responseCode;
    protected String responseMessage;

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

    public BaseResponse(String url) {
        this.requestUrl = url;
        this.responseCode = -1;
        this.responseMessage = "";
        this.header_wwwauthenticate = "";
        this.timeout = false;
        this.connectionType = ConnectionType.defaultConnectionType();
        this.lastUrl = "";
    }

    public void setRequestUrl(String val) { this.requestUrl = val; }
    public void setResponseCode(int val) { this.responseCode = val; }
    public void setResponseMessage(String val) { this.responseMessage = val; }
    public void setTimeout(boolean val) { this.timeout = val; }
    public void setAuthenticateHeader(String val) { this.header_wwwauthenticate = val; }
    public void setConnectionType(ConnectionType val) { this.connectionType = val; }
    public void setLastUrl(String val) { this.lastUrl = val; }

    public String getRequestUrl() { return this.requestUrl; }
    public int getResponseCode() { return this.responseCode; }
    public String getResponseMessage() { return this.responseMessage; }
    public boolean getTimeout() { return this.timeout; }
    public ConnectionType getConnectionType() { return this.connectionType; }
    public String getLastUrl() { return this.lastUrl; }
    public String getAuthenticateHeader() { return this.header_wwwauthenticate; }

    public boolean hasSucceeded() {
        return !this.timeout && this.responseCode == ResponseCode.HTTP_OK;
    }
    public boolean wasRedirected() { return !this.requestUrl.equals(this.lastUrl); }

    // Logging
    private long startTime;
    private long endTime;
    public void begin() { this.startTime = System.currentTimeMillis(); }
    public void end() { this.endTime = System.currentTimeMillis(); }
    public long getExecutionTime() { return this.endTime - this.startTime; }

    public void throwOnFailure() throws HttpException {
        if (!this.hasSucceeded()) {
            throw this.toException();
        }
    }

    public HttpException toException() {
        return new HttpException(this.lastUrl, this.responseCode, this.responseMessage);
    }
}
