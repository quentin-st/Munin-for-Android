package com.chteuchteu.munin.obj.HTTPResponse;

public class HTMLResponse extends BaseResponse {
    private String html;

    public HTMLResponse(String url) {
        super(url);
        this.html = "";
    }

    public void setHtml(String val) { this.html = val; }
    public String getHtml() { return this.html; }
}
