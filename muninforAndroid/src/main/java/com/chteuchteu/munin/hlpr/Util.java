package com.chteuchteu.munin.hlpr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.obj.MuninServer;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class Util {
	public static final class UI {
		/**
		 * Applies the following UI tweaks :
		 * 		- Colors the status bar background (KitKat+)
		 * @param activity
		 */
		@SuppressLint("InlinedApi")
		public static void applySwag(Activity activity) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				int id = activity.getResources().getIdentifier("config_enableTranslucentDecor", "bool", "android");
				if (id != 0 && activity.getResources().getBoolean(id)) { // Translucent available
					Window w = activity.getWindow();
					//w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
					w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
					SystemBarTintManager tintManager = new SystemBarTintManager(activity);
					tintManager.setStatusBarTintEnabled(true);
					tintManager.setStatusBarTintResource(R.color.statusBarColor);
				}
			}
		}
		
		/**
		 * Show loading spinner on actionbar
			activity.requestWindowFeature(Window.FEATURE_PROGRESS);
		 * @param val
		 * @param activity
		 */
		public static void setLoading(boolean val, Activity activity) {
			activity.setProgressBarIndeterminateVisibility(val);
		}
		
		/**
		 * Prepares a Gmail-style progressbar on the actionBar
		 * Should be call in onCreate
		 * @param activity
		 */
		public static ProgressBar prepareGmailStyleProgressBar(final Activity activity) {
			// create new ProgressBar and style it
			final ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
			progressBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 24));
			progressBar.setProgress(0);
			progressBar.setVisibility(View.GONE);
			
			// retrieve the top view of our application
			final FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
			decorView.addView(progressBar);
			
			// Here we try to position the ProgressBar to the correct position by looking
			// at the position where content area starts. But during creating time, sizes 
			// of the components are not set yet, so we have to wait until the components
			// has been laid out
			// Also note that doing progressBar.setY(136) will not work, because of different
			// screen densities and different sizes of actionBar
			ViewTreeObserver observer = progressBar.getViewTreeObserver();
			observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {
					View contentView = decorView.findViewById(android.R.id.content);
					int y = Util.getStatusBarHeight(activity) + Util.getActionBarHeight(activity);
					progressBar.setY(y + contentView.getY() - 10);
					progressBar.setProgressDrawable(activity.getResources().getDrawable(
							R.drawable.progress_horizontal_holo_no_background_light));
					
					ViewTreeObserver observer = progressBar.getViewTreeObserver();
					observer.removeGlobalOnLayoutListener(this);
				}
			});
			
			return progressBar;
		}
	}
	
	public static final class Fonts {
		/* ENUM Custom Fonts */
		public enum CustomFont {
			RobotoCondensed_Regular("RobotoCondensed-Regular.ttf"),
			RobotoCondensed_Light("RobotoCondensed-Light.ttf"),
			RobotoCondensed_Bold("RobotoCondensed-Bold.ttf"),
			Roboto_Medium("Roboto-Medium.ttf");
			
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
	}
	
	public static int[] getDeviceSize(Context c) {
		int[] r = new int[2];
		DisplayMetrics dm = c.getResources().getDisplayMetrics();
		r[0] = dm.widthPixels;
		r[1] = dm.heightPixels;
		return r;
	}
	
	public enum TransitionStyle { DEEPER, SHALLOWER }
	public static void setTransition(Context context, TransitionStyle transitionStyle) {
		switch (transitionStyle) {
			case DEEPER:
				((Activity) context).overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
				break;
			case SHALLOWER:
				((Activity) context).overridePendingTransition(R.anim.shallower_in, R.anim.shallower_out);
				break;
		}
	}
	
	public static int getStatusBarHeight(Context c) {
		int result = 0;
		int resourceId = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0)
			result = c.getResources().getDimensionPixelSize(resourceId);
		return result;
	}
	
	public static int getActionBarHeight(Context c) {
		final TypedArray styledAttributes = c.getTheme().obtainStyledAttributes(
				new int[] { android.R.attr.actionBarSize });
		int height = (int) styledAttributes.getDimension(0, 0);
		styledAttributes.recycle();
		return height;
	}
	
	public static boolean isOnline(Context c) {
		ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return (netInfo != null && netInfo.isConnectedOrConnecting());
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
		// if null or does not needs to be cropped
		return original;
	}
	
	public static Bitmap dropShadow(Bitmap src) {
		if (src == null)
			return null;
		
		try {
			// Parameters
			int verticalPadding = 10;
			int horizontalPadding = 10;
			int radius = 3;
			int color = 0x44000000;
			
			// Create result bitmap
			Bitmap bmOut = Bitmap.createBitmap(src.getWidth() + horizontalPadding, src.getHeight() + verticalPadding, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bmOut);
			canvas.drawColor(0, PorterDuff.Mode.CLEAR);
			Paint ptBlur = new Paint();
			ptBlur.setMaskFilter(new BlurMaskFilter(radius, Blur.OUTER));
			int[] offsetXY = new int[2];
			// Capture alpha into a bitmap
			Bitmap bmAlpha = src.extractAlpha(ptBlur, offsetXY);
			Paint ptAlphaColor = new Paint();
			ptAlphaColor.setColor(color);
			canvas.drawBitmap(bmAlpha, 0, 0, ptAlphaColor);
			bmAlpha.recycle();
			// Paint image source
			canvas.drawBitmap(src, radius, radius, null);
			return bmOut;
		} catch (Exception ex) {
			return src;
		}
	}
	
	public static boolean hasPref(Context context, String key) {
		return context.getSharedPreferences("user_pref", Context.MODE_PRIVATE).contains(key);
	}
	
	public static String getPref(Context context, String key) {
		return context.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	public static void setPref(Context context, String key, String value) {
		if (value.equals(""))
			removePref(context, key);
		else {
			SharedPreferences prefs = context.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(key, value);
			editor.commit();
		}
	}
	
	public static void removePref(Context context, String key) {
		SharedPreferences prefs = context.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		editor.commit();
	}
	
	public static class URLManipulation {
		public static String setHttps(String url) {
			if (url.contains("http://"))
				url = url.replaceAll("http://", "https://");
			url = Util.URLManipulation.setPort(url, 443);
			return url;
		}
		
		private static String setPort(String url, int port) {
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
		
		public static String getHostFromUrl(String url) { return getHostFromUrl(url, url); }
		public static String getHostFromUrl(String url, String defaultUri) {
			try {
				URI uri = new URI(url);
				String domain = uri.getHost();
				return domain.startsWith("www.") ? domain.substring(4) : domain;
			} catch (Exception ex) {
				ex.printStackTrace();
				return defaultUri;
			}
		}
		/**
		 * Get http://host.dd/ from ascendDirectory(2, http://host.dd/x/y/)
		 * @param nbLevels
		 * @param url
		 * @return
		 */
		public static String ascendDirectory(int nbLevels, String url) {
			// We assume the last trailing slash has been added :
			// http://host.dd/sub/
			
			String newUrl = url;
			// Remove everything after the trailing slash
			newUrl = newUrl.substring(0, newUrl.lastIndexOf('/'));
			
			// Ascend
			for (int i=0; i<nbLevels; i++)
				newUrl = newUrl.substring(0, newUrl.lastIndexOf('/'));
			
			// Add trailing slash
			if (!newUrl.endsWith("/"))
				newUrl += "/";
			
			// Check if we still have a consistent URL
			String hostName = Util.URLManipulation.getHostFromUrl(url);
			if (!newUrl.contains(hostName))
				return url;
			
			return newUrl;
		}
	}
	
	public static boolean serversListContainsPos(List<MuninServer> l, int pos) {
		for (MuninServer s : l) {
			if (s.getPosition() == pos)
				return true;
		}
		return false;
	}
	
	public static final class HDGraphs {
		private static float getScreenDensity(Context context) {
			return context.getResources().getDisplayMetrics().density;
		}
		
		public static int[] getBestImageDimensions(View imageView, Context context) {
			int[] res = new int[2];
			
			float screenDensity = getScreenDensity(context);
			if (screenDensity < 1)
				screenDensity = 1;
			
			int dimens_x = imageView.getMeasuredWidth();
			int dimens_y = imageView.getMeasuredHeight();
			
			// Apply density
			dimens_x = (int) (dimens_x/screenDensity);
			dimens_y = (int) (dimens_y/screenDensity);
			
			// Limit ratio
			if (dimens_y != 0) {
				double minRatio = ((double)360) / 210;
				double currentRatio = ((double)dimens_x) / dimens_y;
				
				if (currentRatio < minRatio) {
					// Adjust height
					dimens_y = (int) (dimens_x/minRatio);
				}
			}
			
			res[0] = dimens_x;
			res[1] = dimens_y;
			return res;
		}
	}
	
	public static void hideKeyboard(Activity activity, EditText editText) {
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
	}
	
	public static final class Encryption {
		public static String encrypt(String seed, String cleartext) throws Exception {
			byte[] rawKey = getRawKey(seed.getBytes());
			byte[] result = encrypt(rawKey, cleartext.getBytes());
			return toHex(result);
		}
		
		public static String decrypt(String seed, String encrypted) throws Exception {
			byte[] rawKey = getRawKey(seed.getBytes());
			byte[] enc = toByte(encrypted);
			byte[] result = decrypt(rawKey, enc);
			return new String(result);
		}
		
		@SuppressLint("TrulyRandom")
		private static byte[] getRawKey(byte[] seed) throws Exception {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
			sr.setSeed(seed);
			kgen.init(128, sr); // 192 and 256 bits may not be available
			SecretKey skey = kgen.generateKey();
			byte[] raw = skey.getEncoded();
			return raw;
		}
		
		
		private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			byte[] encrypted = cipher.doFinal(clear);
			return encrypted;
		}
		
		private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec);
			byte[] decrypted = cipher.doFinal(encrypted);
			return decrypted;
		}
		
		private static byte[] toByte(String hexString) {
			int len = hexString.length()/2;
			byte[] result = new byte[len];
			for (int i = 0; i < len; i++)
				result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
			return result;
		}
		
		private static String toHex(byte[] buf) {
			if (buf == null)
				return "";
			StringBuffer result = new StringBuffer(2*buf.length);
			for (int i = 0; i < buf.length; i++) {
				appendHex(result, buf[i]);
			}
			return result.toString();
		}
		private final static String HEX = "0123456789ABCDEF";
		private static void appendHex(StringBuffer sb, byte b) {
			sb.append(HEX.charAt((b>>4)&0x0f)).append(HEX.charAt(b&0x0f));
		}
	}
	
	public static View getActionBarView(Activity activity) {
		Window window = activity.getWindow();
		View v = window.getDecorView();
		int resId = activity.getResources().getIdentifier("action_bar_container", "id", "android");
		return v.findViewById(resId);
	}
	
	/**
	 * "apache" => "Apache"
	 * "some words" => "Some words"
	 * @param original
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	public static String capitalize(String original) {
		if (original.length() < 2)
			return original;
		
		return original.substring(0, 1).toUpperCase() + original.substring(1);
	}
	
	public static enum SpecialBool { UNKNOWN, TRUE, FALSE }
}