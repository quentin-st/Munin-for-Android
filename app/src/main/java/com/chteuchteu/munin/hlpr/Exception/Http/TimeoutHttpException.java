package com.chteuchteu.munin.hlpr.Exception.Http;

import com.chteuchteu.munin.hlpr.Http.ResponseCode;

/**
 * @deprecated Specific HTTP exceptions are not used for now.
 */
public class TimeoutHttpException extends HttpException {
    public TimeoutHttpException(String url) {
        super(url, ResponseCode.HTTP_REQUEST_TIMEOUT, "Timeout");
    }
}
