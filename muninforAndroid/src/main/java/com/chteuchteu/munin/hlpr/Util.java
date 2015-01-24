package com.chteuchteu.munin.hlpr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.obj.MuninServer;
import com.larvalabs.svgandroid.SVG;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class Util {
	public static final class UI {
		/**
		 * Applies the following UI tweaks :
		 * 		- Colors the status bar background (KitKat+)
		 * @param activity Activity
		 */
		@SuppressLint("InlinedApi")
		public static void applySwag(Activity activity) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				int id = activity.getResources().getIdentifier("config_enableTranslucentDecor", "bool", "android");
				if (id != 0 && activity.getResources().getBoolean(id)) { // Translucent available
					Window w = activity.getWindow();
					//w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
					w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

					// On Android KitKat => statusBarColor. Above => actionBarColor
					int statusBarColor = Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT ? R.color.statusBarColor : R.color.actionBarColor;
					SystemBarTintManager tintManager = new SystemBarTintManager(activity);
					tintManager.setStatusBarTintEnabled(true);
					tintManager.setStatusBarTintResource(statusBarColor);
				}
			}
		}

		/**
		 * Prepares a Gmail-style progressbar on the actionBar
		 * Should be call in onCreate
		 * @param activity Activity
		 */
		public static ProgressBar prepareGmailStyleProgressBar(final Activity activity, final ActionBar actionBar) {
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
					int y = Util.getStatusBarHeight(activity) + actionBar.getHeight();

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
			RobotoCondensed_Bold("RobotoCondensed-Bold.ttf"),
			Roboto_Medium("Roboto-Medium.ttf"),
			Roboto_Regular("Roboto-Regular.ttf");
			
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
				if (v instanceof TextView)
					((TextView) v).setTypeface(font);
				else if (v instanceof ViewGroup)
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
	
	public static boolean isOnline(Context c) {
		ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return (netInfo != null && netInfo.isConnectedOrConnecting());
	}
	
	public static Period getDefaultPeriod(Context c) {
		return Period.get(Util.getPref(c, PrefKeys.DefaultScale));
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
	
	public static boolean hasPref(Context context, PrefKeys key) {
		return context.getSharedPreferences("user_pref", Context.MODE_PRIVATE).contains(key.getKey());
	}

	public enum PrefKeys {
		GraphviewOrientation("graphview_orientation"),  Notifications("notifications"),
		ScreenAlwaysOn("screenAlwaysOn"),                 Notifs_RefreshRate("notifs_refreshRate"),
		DefaultScale("defaultScale"),                      Notifs_ServersList("notifs_serversList"),
		LastMFAVersion("lastMFAVersion"),				  Notifs_WifiOnly("notifs_wifiOnly"),
															  Notifs_Vibrate("notifs_vibrate"),
															  Notifs_LastNotificationText("lastNotificationText"),

		AutoRefresh("autoRefresh"),                        UserAgent("userAgent"), UserAgentChanged("userAgentChanged"),
		HDGraphs("hdGraphs"),                               Lang("lang"),
		GraphsZoom("graphsZoom"),                          DefaultServer("defaultServer"),
		GridsLegend("gridsLegend"),

		Twitter_NbLaunches("twitter_nbLaunches"),        AddServer_History("addserver_history"),
		Widget2_ForceUpdate("widget2_forceUpdate"),      OpenSourceDialogShown("openSourceDialogShown"),
														 I18NDialogShown("i18nDialogShown"),

		// Old prefs
		Drawer("drawer"), Splash("splash"), ListViewMode("listViewMode"), Transitions("transitions");

		private String key;
		PrefKeys(String k) { this.key = k; }

		public String getKey() { return this.key; }
	}

	public static String getPref(Context context, PrefKeys key) {
		return context.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key.getKey(), "");
	}
	
	public static void setPref(Context context, PrefKeys key, String value) {
		if (value.equals(""))
			removePref(context, key);
		else {
			SharedPreferences prefs = context.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(key.getKey(), value);
			editor.apply();
		}
	}
	
	public static void removePref(Context context, PrefKeys key) {
		SharedPreferences prefs = context.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key.getKey());
		editor.apply();
	}
	
	public static class URLManipulation {
		public static String setHttps(String url) {
			if (url.contains("http://"))
				url = url.replaceAll("http://", "https://");
			url = Util.URLManipulation.setPort(url, 443);
			return url;
		}
		
		private static String setPort(String url, int port) {
			URL _url;
			try {
				_url = new URL(url);
			} catch (MalformedURLException e) {
				return url;
			}
			if (url == null)
				return null;
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
		 *  If we ascent too much times, the original URL will be returned.
		 * @param nbLevels Ascend X times
		 * @param url Source URL
		 * @return Result URL
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
	
	/**
	 * "apache" => "Apache"
	 * "some words" => "Some words"
	 * @param original Original string
	 * @return Capitalized string
	 */
	@SuppressLint("DefaultLocale")
	public static String capitalize(String original) {
		if (original.length() < 2)
			return original;
		
		return original.substring(0, 1).toUpperCase() + original.substring(1);
	}

	/**
	 * Extended boolean, especially useful when we don't know something's state at first
	 * For example: documentation availability: UNKNOWN => (TRUE/FALSE)
	 */
	public static enum SpecialBool { UNKNOWN, TRUE, FALSE }

	public static String readFromAssets(Context context, String file) {
		try {
			InputStream is = context.getAssets().open(file);
			int size = is.available();

			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();

			return new String(buffer);
		} catch (IOException ex) {
			ex.printStackTrace();
			return "";
		}
	}

	public static String getAppVersion(Context context) {
		String versionName;
		try {
			versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			versionName = "";
		}
		return versionName;
	}

	public static String getAndroidVersion() {
		String str = "Android " + Build.VERSION.RELEASE;

		// Get "KitKat"
		Field[] fields = Build.VERSION_CODES.class.getFields();
		for (Field field : fields) {
			String fieldName = field.getName();
			int fieldValue = -1;

			try {
				fieldValue = field.getInt(new Object());
			} catch (IllegalArgumentException | IllegalAccessException | NullPointerException e) {
				e.printStackTrace();
			}

            if (fieldValue == Build.VERSION.SDK_INT)
				str += " " + fieldName;
		}

		return str;
	}

	public static boolean isPackageInstalled (String packageName, Context c) {
		PackageManager pm = c.getPackageManager();
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
		return true;
	}

	public static List<View> getViewsByTag(ViewGroup root, String tag) {
		List<View> views = new ArrayList<>();
		final int childCount = root.getChildCount();
		for (int i=0; i<childCount; i++) {
			final View child = root.getChildAt(i);
			if (child instanceof ViewGroup)
				views.addAll(Util.getViewsByTag((ViewGroup) child, tag));

			final Object tagObj = child.getTag();
			if (tagObj != null && tagObj.equals(tag))
				views.add(child);
		}
		return views;
	}

	/**
	 * Returns the first-level child of the parent, with the type 'type'
	 * @param parent root view
	 * @param type view type (EditText, ImageView, ...)
	 * @return may be null
	 */
	public static View getChild(ViewGroup parent, Class<?> type) {
		for (int i=0; i<parent.getChildCount(); i++) {
			View child = parent.getChildAt(i);

			if (child.getClass() == type)
				return child;
			else if (child instanceof ViewGroup) {
				View child2 = getChild((ViewGroup) child, type);
				if (child2 != null)
					return child2;
			}
		}

		return null;
	}

	/**
	 * Returns the bitmap position inside an imageView.
	 * @param imageView source ImageView
	 * @return 0: left, 1: top, 2: width, 3: height
	 */
	public static int[] getBitmapPositionInsideImageView(ImageView imageView) {
		int[] ret = new int[4];

		if (imageView == null || imageView.getDrawable() == null)
			return ret;

		// Get image dimensions
		// Get image matrix values and place them in an array
		float[] f = new float[9];
		imageView.getImageMatrix().getValues(f);

		// Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
		final float scaleX = f[Matrix.MSCALE_X];
		final float scaleY = f[Matrix.MSCALE_Y];

		// Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
		final Drawable d = imageView.getDrawable();
		final int origW = d.getIntrinsicWidth();
		final int origH = d.getIntrinsicHeight();

		// Calculate the actual dimensions
		final int actW = Math.round(origW * scaleX);
		final int actH = Math.round(origH * scaleY);

		ret[2] = actW;
		ret[3] = actH;

		// Get image position
		// We assume that the image is centered into ImageView
		int imgViewW = imageView.getWidth();
		int imgViewH = imageView.getHeight();

		int top = (imgViewH - actH)/2;
		int left = (imgViewW - actW)/2;

		ret[0] = left;
		ret[1] = top;

		return ret;
	}

	/**
	 * Returns a string containing the date (from timestamp) using the device locale
	 * @param timestamp long
	 * @return String
	 */
	public static String prettyDate(long timestamp) {
		return DateFormat.getDateTimeInstance().format(new Date(timestamp*1000));
	}

	public static final class Animations {
		public enum CustomAnimation { FADE_IN, FADE_OUT }
		public enum AnimationSpeed {
			SLOW(1000), MEDIUM(300), FAST(100);

			private int duration;
			AnimationSpeed(int duration) { this.duration = duration; }
			public int getDuration() { return this.duration; }
		}

		public static void animate(View view, CustomAnimation animation) { animate(view, animation, AnimationSpeed.MEDIUM, null); }
		public static void animate(View view, CustomAnimation animation, AnimationSpeed animationSpeed, final Runnable onAnimationEnd) {
			if (view == null)
				return;

			switch (animation) {
				case FADE_IN:
					AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
					fadeIn.setDuration(animationSpeed.getDuration());
					fadeIn.setAnimationListener(new Animation.AnimationListener() {
						@Override public void onAnimationStart(Animation animation) { }
						@Override
						public void onAnimationEnd(Animation animation) {
							if (onAnimationEnd != null)
								onAnimationEnd.run();
						}
						@Override public void onAnimationRepeat(Animation animation) { }
					});
					view.startAnimation(fadeIn);

					break;
				case FADE_OUT:
					AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
					fadeOut.setDuration(animationSpeed.getDuration());
					fadeOut.setAnimationListener(new Animation.AnimationListener() {
						@Override public void onAnimationStart(Animation animation) { }
						@Override
						public void onAnimationEnd(Animation animation) {
							if (onAnimationEnd != null)
								onAnimationEnd.run();
						}
						@Override public void onAnimationRepeat(Animation animation) { }
					});
					view.startAnimation(fadeOut);

					break;
			}
		}
	}

	public interface ProgressNotifier { public void notify(int progress, int total); }

	@SuppressWarnings("deprecation")
	public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener){
		if (Build.VERSION.SDK_INT < 16)
			v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
		else
			v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
	}

	public static Bitmap svgToBitmap(SVG svg) {
		PictureDrawable pictureDrawable = svg.createPictureDrawable();
		Bitmap bitmap = Bitmap.createBitmap(pictureDrawable.getIntrinsicWidth(),
				pictureDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawPicture(pictureDrawable.getPicture());
		return bitmap;
	}
}
