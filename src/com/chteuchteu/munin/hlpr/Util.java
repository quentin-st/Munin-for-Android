package com.chteuchteu.munin.hlpr;

import java.net.MalformedURLException;
import java.net.URL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public final class Util {
	private Util() { }
	
	/* ENUM Custom Fonts */
	public enum CustomFont {
		RobotoCondensed_Regular("RobotoCondensed-Regular.ttf"), Roboto_Thin("Roboto-Thin.ttf");
		final String file;
		private CustomFont(String fileName) { this.file = fileName; }
		public String getValue() { return this.file; }
	}
	
	/* Fonts */
	public static void setFont(Context c, ViewGroup g, CustomFont font) {
		Typeface mFont = Typeface.createFromAsset(c.getAssets(), font.getValue());
		setFont(g, mFont);
	}
	
	public static void setFont(Context c, TextView t, CustomFont font) {
		Typeface mFont = Typeface.createFromAsset(c.getAssets(), font.getValue());
		t.setTypeface(mFont);
	}
	
	public static void setFont(Context c, Button t, CustomFont font) {
		Typeface mFont = Typeface.createFromAsset(c.getAssets(), font.getValue());
		t.setTypeface(mFont);
	}
	
	private static void setFont(ViewGroup group, Typeface font) {
		int count = group.getChildCount();
		View v;
		for (int i = 0; i < count; i++) {
			v = group.getChildAt(i);
			if (v instanceof TextView || v instanceof EditText || v instanceof Button) {
				((TextView) v).setTypeface(font);
			} else if (v instanceof ViewGroup)
				setFont((ViewGroup) v, font);
		}
	}
	
	public static Typeface getTypeFace(Context c, CustomFont name) {
		return Typeface.createFromAsset(c.getAssets(), name.getValue());
	}
	
	@SuppressLint("NewApi")
	public static boolean deviceHasBackKey(Context c) {
		if (Build.VERSION.SDK_INT >= 14)
			return ViewConfiguration.get(c).hasPermanentMenuKey();
		else
			return true;
	}
	
	public static int getStatusBarHeight(Context c) {
		int result = 0;
		int resourceId = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0)
			result = c.getResources().getDimensionPixelSize(resourceId);
		return result;
	}
	
	public static boolean isOnline(Context c) {
		ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting())
			return true;
		return false;
	}
	
	public static Bitmap removeBitmapBorder(Bitmap original) {
		if (original != null) {
			if (original.getPixel(0, 0) == 0xFFCFCFCF)
				return Bitmap.createBitmap(original, 2, 2, original.getWidth()-4, original.getHeight()-4);
			else
				return original;
		}
		return null;
	}
	
	public static String getPref(Context c, String key) {
		return c.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	public static void setPref(Context c, String key, String value) {
		if (value.equals(""))
			removePref(c, key);
		else {
			SharedPreferences prefs = c.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(key, value);
			editor.commit();
		}
	}
	
	public static void removePref(Context c, String key) {
		SharedPreferences prefs = c.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		editor.commit();
	}
	
	public static Bitmap fastblur(Bitmap sentBitmap, int radius) {
		
		// Stack Blur v1.0 from
		// http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
		//
		// Java Author: Mario Klingemann <mario at quasimondo.com>
		// http://incubator.quasimondo.com
		// created Feburary 29, 2004
		// Android port : Yahel Bouaziz <yahel at kayenko.com>
		// http://www.kayenko.com
		// ported april 5th, 2012
		
		// This is a compromise between Gaussian Blur and Box blur
		// It creates much better looking blurs than Box Blur, but is
		// 7x faster than my Gaussian Blur implementation.
		//
		// I called it Stack Blur because this describes best how this
		// filter works internally: it creates a kind of moving stack
		// of colors whilst scanning through the image. Thereby it
		// just has to add one new block of color to the right side
		// of the stack and remove the leftmost color. The remaining
		// colors on the topmost layer of the stack are either added on
		// or reduced by one, depending on if they are on the right or
		// on the left side of the stack.
		//
		// If you are using this algorithm in your code please add
		// the following line:
		//
		// Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
		
		Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
		
		if (radius < 1) {
			return (null);
		}
		
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		
		int[] pix = new int[w * h];
		//Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.getPixels(pix, 0, w, 0, 0, w, h);
		
		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;
		
		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];
		
		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}
		
		yw = yi = 0;
		
		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;
		
		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = pix[yi + Math.min(wm, Math.max(i, 0))];
				sir = stack[i + radius];
				sir[0] = (p & 0xff0000) >> 16;
			sir[1] = (p & 0x00ff00) >> 8;
		sir[2] = (p & 0x0000ff);
		rbs = r1 - Math.abs(i);
		rsum += sir[0] * rbs;
		gsum += sir[1] * rbs;
		bsum += sir[2] * rbs;
		if (i > 0) {
			rinsum += sir[0];
			ginsum += sir[1];
			binsum += sir[2];
		} else {
			routsum += sir[0];
			goutsum += sir[1];
			boutsum += sir[2];
		}
			}
			stackpointer = radius;
			
			for (x = 0; x < w; x++) {
				
				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];
				
				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;
				
				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];
				
				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];
				
				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];
				
				sir[0] = (p & 0xff0000) >> 16;
			sir[1] = (p & 0x00ff00) >> 8;
			sir[2] = (p & 0x0000ff);
			
			rinsum += sir[0];
			ginsum += sir[1];
			binsum += sir[2];
			
			rsum += rinsum;
			gsum += ginsum;
			bsum += binsum;
			
			stackpointer = (stackpointer + 1) % div;
			sir = stack[(stackpointer) % div];
			
			routsum += sir[0];
			goutsum += sir[1];
			boutsum += sir[2];
			
			rinsum -= sir[0];
			ginsum -= sir[1];
			binsum -= sir[2];
			
			yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;
				
				sir = stack[i + radius];
				
				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];
				
				rbs = r1 - Math.abs(i);
				
				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;
				
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
				
				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				// Preserve alpha channel: ( 0xff000000 & pix[yi] )
				pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];
				
				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;
				
				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];
				
				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];
				
				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];
				
				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];
				
				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];
				
				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;
				
				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];
				
				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];
				
				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];
				
				yi += w;
			}
		}
		
		//Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.setPixels(pix, 0, w, 0, 0, w, h);
		
		return (bitmap);
	}
	
	public static String setHttps(String url) {
		if (url.contains("http://"))
			url = url.replaceAll("http://", "https://");
		url = Util.setPort(url, 443);
		return url;
	}
	
	public static String setPort(String url, int port) {
		URL _url = null;
		try {
			_url = new URL(url);
		} catch (MalformedURLException e) {
	        return url;
	    }
		if (url == null)
			return url;
		if (_url.getPort() == port)
			return url;
		return _url.getProtocol() + "://" + _url.getHost() + ":" + port + _url.getFile();
	}
}