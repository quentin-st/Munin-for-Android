package com.chteuchteu.munin.hlpr;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.obj.MuninServer;

public final class Util {
	private Util() { }
	
	public static final class Fonts {
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
	}
	
	public static int[] getDeviceSize(Context c) {
		int[] r = new int[2];
		DisplayMetrics dm = c.getResources().getDisplayMetrics();
		r[0] = dm.widthPixels;
		r[1] = dm.heightPixels;
		return r;
	}
	
	public enum TransitionStyle { DEEPER, SHALLOWER }
	
	public static void setTransition(Context c, TransitionStyle ts) {
		if (getPref(c, "transitions").equals("true")) {
			if (ts == TransitionStyle.DEEPER)
				((Activity) c).overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
			else if (ts == TransitionStyle.SHALLOWER)
				((Activity) c).overridePendingTransition(R.anim.shallower_in, R.anim.shallower_out);
		}
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
	
	public static Period getDefaultPeriod(Context c) {
		return Period.get(Util.getPref(c, "defaultScale"));
	}
	
	public static Bitmap removeBitmapBorder(Bitmap original) {
		if (original != null && original.getPixel(0, 0) == 0xFFCFCFCF) {
			try {
				return Bitmap.createBitmap(original, 2, 2, original.getWidth()-4, original.getHeight()-4);
			} catch (Exception ignored) {
				return original;
			}
		}
		// if null of does not needs to be cropped
		return original;
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
	
	public static final class Dates {
		@SuppressLint("SimpleDateFormat")
		public static String getNow() {
			DateFormat df = new SimpleDateFormat("yyyMMdd HH:mm:ss");
			Date today = Calendar.getInstance().getTime();        
			return df.format(today);
		}
	}
	
	public static boolean serversListContainsPos(List<MuninServer> l, int pos) {
		for (MuninServer s : l) {
			if (s.getPosition() == pos)
				return true;
		}
		return false;
	}
}