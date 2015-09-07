package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.extras.toolbar.MaterialMenuIconToolbar;
import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.I18nHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;

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
	protected DrawerHelper  dh;
	protected Context       context;
	protected Activity      activity;
	protected android.support.v7.app.ActionBar actionBar;
	protected Toolbar       toolbar;
	private MaterialMenuIconToolbar materialMenu;
	private boolean         isDrawerOpen;
	protected Menu          menu;

	private Runnable    onDrawerOpen;
	private Runnable    onDrawerClose;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Fabric.with(this, new Crashlytics());

		this.context = this;
		this.activity = this;
		this.muninFoo = MuninFoo.getInstance(this);
		I18nHelper.loadLanguage(this, muninFoo);

		// setContentView...
	}

	public void onContentViewSet() {
		Util.UI.applySwag(this);
		this.toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		this.actionBar = getSupportActionBar();
		this.actionBar.setDisplayShowHomeEnabled(false);
		this.dh = new DrawerHelper(this, muninFoo);
		this.materialMenu = new MaterialMenuIconToolbar(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN) {
			@Override public int getToolbarViewId() {
				return R.id.toolbar;
			}
		};
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				dh.toggle();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(this, Activity_Settings.class));
				Util.setTransition(this, Util.TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(this, Activity_About.class));
				Util.setTransition(this, Util.TransitionStyle.DEEPER);
				return true;
			default:
				// In any other case, close the drawer before executing action
				dh.closeDrawerIfOpen();
				return true;
		}
	}

	protected void createOptionsMenu() { menu.clear(); }

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		dh.getDrawerLayout().setDrawerListener(new DrawerLayout.DrawerListener() {
			@Override
			public void onDrawerSlide(View view, float slideOffset) {
				materialMenu.setTransformationOffset(
						MaterialMenuDrawable.AnimationState.BURGER_ARROW,
						isDrawerOpen ? 2 - slideOffset : slideOffset
				);
			}

			@Override
			public void onDrawerOpened(View view) {
				isDrawerOpen = true;
				materialMenu.animatePressedState(MaterialMenuDrawable.IconState.ARROW);

				actionBar.setSubtitle(actionBar.getTitle());
				actionBar.setTitle(getString(R.string.app_name));

				// Runnable set in Activity
				if (onDrawerOpen != null)
					onDrawerOpen.run();

				menu.clear();
				getMenuInflater().inflate(R.menu.main, menu);
			}

			@Override
			public void onDrawerClosed(View view) {
				isDrawerOpen = false;
				materialMenu.animatePressedState(MaterialMenuDrawable.IconState.BURGER);

				actionBar.setTitle(actionBar.getSubtitle());
				actionBar.setSubtitle(null);

				// Runnable set in Activity
				if (onDrawerClose != null)
					onDrawerClose.run();

				createOptionsMenu();
			}

			@Override
			public void onDrawerStateChanged(int i) { }
		});

		createOptionsMenu();

		return true;
	}

	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.None; }
	protected void setOnDrawerOpen(Runnable val) { this.onDrawerOpen = val; }
	protected void setOnDrawerClose(Runnable val) { this.onDrawerClose = val; }

	@Override
	protected void onStart() {
		super.onStart();

		if (!BuildConfig.DEBUG)
			EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onStop() {
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

	protected void log(String s) { MuninFoo.log(((Object) this).getClass().getName(), s); }
}
