package com.chteuchteu.munin.hlpr.Exception.Http;

import com.chteuchteu.munin.obj.HTTPResponse.BaseResponse;

public class HttpException extends Exception {
    private String url;
    private int code;
    private String message;

    public HttpException(String url, int code, String message) {
        this.url = url;
        this.code = code;
        this.message = message;
    }

    public HttpException(BaseResponse response) {
        this.url = response.getLastUrl();
        this.code = response.getResponseCode();
        this.message = response.getResponseMessage();
    }

    public String getUrl() {
        return url;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
