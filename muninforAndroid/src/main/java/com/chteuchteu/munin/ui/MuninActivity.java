package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Settings;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import io.fabric.sdk.android.Fabric;

/**
 * One class to rule them all
 * Every Activity_* extends this one to avoid code redundancy
 * Note: Activity_Main doesn't extend it because of it special
 *  way of working (loading app before displaying anything)
 */
@SuppressLint("Registered")
public class MuninActivity extends AppCompatActivity {
	protected MuninFoo      muninFoo;
	protected Settings		settings;
	protected DrawerHelper  drawerHelper;
	protected Context       context;
	protected Activity      activity;
	protected android.support.v7.app.ActionBar actionBar;
	protected Toolbar       toolbar;
	protected Menu          menu;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Fabric.with(this, new Crashlytics());

		this.context = this;
		this.activity = this;
		this.muninFoo = MuninFoo.getInstance(this);
		this.settings = muninFoo.getSettings();

		if (!BuildConfig.DEBUG) {
			Tracker tracker = this.muninFoo.getDefaultTracker(this);
			tracker.setScreenName(this.getClass().getSimpleName());
			tracker.send(new HitBuilders.ScreenViewBuilder().build());
		}

		// setContentView...
	}

	public void onContentViewSet() {
		this.toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		this.actionBar = getSupportActionBar();
		this.drawerHelper = new DrawerHelper(this, muninFoo, this.toolbar);
	}

	protected void createOptionsMenu() { menu.clear(); }

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;

		createOptionsMenu();

		return true;
	}

	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.None; }

	protected void log(String s) { MuninFoo.log(((Object) this).getClass().getName(), s); }
}
