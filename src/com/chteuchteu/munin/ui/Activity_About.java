package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


public class Activity_About extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper 	dh;
	private Menu 			menu;
	private String			activityName;
	private Context		c;
	
	@SuppressWarnings("deprecation")
	@SuppressLint("DefaultLocale")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		setContentView(R.layout.about);
		c = this;
		
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(getString(R.string.aboutTitle));
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(DrawerHelper.Activity_About);
		
		Util.UI.applySwag(this);
		
		
		WebView wv = (WebView)findViewById(R.id.webView1);
		wv.setVerticalScrollBarEnabled(true);
		wv.getSettings().setDefaultTextEncodingName("utf-8");
		wv.setBackgroundColor(0x00000000);
		String content = getString(R.string.aboutText);
		String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			versionName = "";
		}
		content = content.replaceAll("#version#", versionName);
		wv.loadDataWithBaseURL(null, content, "text/html", "utf-8", null);
		wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		wv.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		
		TextView tv1 = (TextView) findViewById(R.id.about_txt1);
		TextView tv2 = (TextView) findViewById(R.id.about_txt2);
		Util.Fonts.setFont(this, tv1, CustomFont.RobotoCondensed_Regular);
		Util.Fonts.setFont(this, tv2, CustomFont.RobotoCondensed_Regular);
		tv1.setText(tv1.getText().toString().toUpperCase());
		tv2.setText(getString(R.string.app_name) + " " + versionName);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_About.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_About.this, Activity_About.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(c, TransitionStyle.SHALLOWER);
	}
	
	private void createOptionsMenu() {
		menu.clear();
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		dh.getDrawer().setOnOpenListener(new OnOpenListener() {
			@Override
			public void onOpen() {
				activityName = getActionBar().getTitle().toString();
				getActionBar().setTitle(R.string.app_name);
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
		createOptionsMenu();
		
		return true;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStop(this);
	}
}