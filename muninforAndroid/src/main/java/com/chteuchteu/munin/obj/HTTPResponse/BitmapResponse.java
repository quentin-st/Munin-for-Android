package com.chteuchteu.munin.obj.HTTPResponse;

import android.graphics.Bitmap;

public class BitmapResponse extends BaseResponse {
    private Bitmap bitmap;

    public BitmapResponse(String url) {
        super(url);
    }

    public void setBitmap(Bitmap val) { this.bitmap = val; }
    public Bitmap getBitmap() { return this.bitmap; }

    /**
     * Same as hasSuceeded(), but doesn't test if bitmap == null
     */
    public boolean requestSucceeded() {
        return super.hasSucceeded();
    }

    @Override
    public boolean hasSucceeded() {
        return super.hasSucceeded() && this.bitmap != null;
    }
}
