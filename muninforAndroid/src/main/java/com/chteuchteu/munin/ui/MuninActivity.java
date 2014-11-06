package com.chteuchteu.munin.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuIcon;
import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

/**
 * One class to rule them all
 */
public class MuninActivity extends Activity {
	protected MuninFoo      muninFoo;
	protected DrawerHelper  dh;
	protected Context       context;
	protected Activity      activity;
	protected ActionBar     actionBar;
	private MaterialMenuIcon materialMenu;
	protected Menu          menu;
	protected String        activityName;

	private Runnable    onDrawerOpen;
	private Runnable    onDrawerClose;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Crashlytics.start(this);

		this.context = this;
		this.activity = this;
		this.muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);

		// setContentView...
	}

	public void onContentViewSet() {
		Util.UI.applySwag(this);
		this.actionBar = getActionBar();
		this.actionBar.setDisplayShowHomeEnabled(false);
		this.dh = new DrawerHelper(this, muninFoo);
		this.materialMenu = new MaterialMenuIcon(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);
		this.materialMenu.setNeverDrawTouch(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home)
			dh.closeDrawerIfOpened();

		switch (item.getItemId()) {
			case android.R.id.home:
				dh.toggle(true);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(context, Activity_Settings.class));
				Util.setTransition(context, Util.TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(context, Activity_About.class));
				Util.setTransition(context, Util.TransitionStyle.DEEPER);
				return true;
		}

		return true;
	}

	protected void createOptionsMenu() { menu.clear(); }

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		dh.getDrawer().setOnOpenListener(new SlidingMenu.OnOpenListener() {
			@Override
			public void onOpen() {
				materialMenu.animatePressedState(MaterialMenuDrawable.IconState.ARROW);

				activityName = actionBar.getTitle().toString();
				actionBar.setTitle(R.string.app_name);

				// Runnable set in Activity
				if (onDrawerOpen != null)
					onDrawerOpen.run();

				menu.clear();
				getMenuInflater().inflate(R.menu.main, menu);
			}
		});
		dh.getDrawer().setOnCloseListener(new SlidingMenu.OnCloseListener() {
			@Override
			public void onClose() {
				materialMenu.animatePressedState(MaterialMenuDrawable.IconState.BURGER);
				actionBar.setTitle(activityName);

				// Runnable set in Activity
				if (onDrawerClose != null)
					onDrawerClose.run();

				createOptionsMenu();
			}
		});

		createOptionsMenu();

		return true;
	}

	protected void setOnDrawerOpen(Runnable val) { this.onDrawerOpen = val; }
	protected void setOnDrawerClose(Runnable val) { this.onDrawerClose = val; }

	@Override
	public void onStart() {
		super.onStart();

		if (!BuildConfig.DEBUG)
			EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		if (!BuildConfig.DEBUG)
			EasyTracker.getInstance(this).activityStop(this);
	}

	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		materialMenu.syncState(savedInstanceState);
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		materialMenu.onSaveInstanceState(outState);
	}

	protected void log(String s) { MuninFoo.log(getClass().getName(), s); }
}
