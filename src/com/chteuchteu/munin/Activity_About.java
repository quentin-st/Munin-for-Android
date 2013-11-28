package com.chteuchteu.munin;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


public class Activity_About extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper 	dh;
	private Menu 			menu;
	private String			activityName;
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		setContentView(R.layout.about);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getString(R.string.aboutTitle));
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_About);
			}
		}
		
		
		WebView wv = (WebView)findViewById(R.id.webView1);
		wv.setVerticalScrollBarEnabled(true);
		wv.getSettings().setDefaultTextEncodingName("utf-8");
		wv.setBackgroundColor(0x00000000);
		String content = getString(R.string.aboutText);
		String versionName;
		try {
			versionName = getPackageManager()
				    .getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			versionName = "";
		}
		content = content.replaceAll("#version#", versionName);
		wv.loadDataWithBaseURL(null, content, "text/html", "utf-8", null);
		// Eviter le clignotement pendant le scroll
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
			wv.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		}
	}
	
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				if (muninFoo.drawer)
					dh.getDrawer().toggle(true);
				else {
					Intent intent = new Intent(this, Activity_Main.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					setTransition("shallower");
				}
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_About.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_About.this, Activity_About.class));
				setTransition("deeper");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent(this, Activity_Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			setTransition("shallower");
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public String getPref(String key) {
		return this.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	public void setPref(String key, String value) {
		if (value.equals(""))
			removePref(key);
		else {
			SharedPreferences prefs = this.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(key, value);
			editor.commit();
		}
	}
	
	public void removePref(String key) {
		SharedPreferences prefs = this.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		editor.commit();
	}
	
	public void setTransition(String level) {
		if (getPref("transitions").equals("true")) {
			if (level.equals("deeper"))
				overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
			else if (level.equals("shallower"))
				overridePendingTransition(R.anim.shallower_in, R.anim.shallower_out);
		}
	}
	
	private void createOptionsMenu() {
		menu.clear();
	}
	
	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		if (muninFoo.drawer) {
			dh.getDrawer().setOnOpenListener(new OnOpenListener() {
				@Override
				public void onOpen() {
					activityName = getActionBar().getTitle().toString();
					getActionBar().setTitle("Munin for Android");
					menu.clear();
					getMenuInflater().inflate(R.menu.main, menu);
				}
			});
			dh.getDrawer().setOnCloseListener(new OnCloseListener() {
				@Override
				public void onClose() {
					getActionBar().setTitle(activityName);
					createOptionsMenu();
				}
			});
		}
		createOptionsMenu();
		
		return true;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!muninFoo.debug)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!muninFoo.debug)
			EasyTracker.getInstance(this).activityStop(this);
	}
}