package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.extras.toolbar.MaterialMenuIconToolbar;
import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.tjeannin.apprate.AppRate;

import java.util.Locale;

/**
 * We are not extending MuninActivity from here since the activity cycle
 * is very different from others (showing UI elements only when the app
 * is loaded)
 */
public class Activity_Main extends ActionBarActivity {
	private MuninFoo		muninFoo;
	private MaterialMenuIconToolbar materialMenu;

	private Menu 			menu;
	private boolean		doubleBackPressed;

	private DrawerHelper dh;
	private boolean isDrawerOpened;
	
	// Preloading
	private boolean preloading;
	private boolean optionsMenuLoaded;
	private Context context;
	private ProgressDialog myProgressDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Crashlytics.start(this);
		preloading = true;
		boolean loaded = MuninFoo.isLoaded();
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		optionsMenuLoaded = false;
		if (loaded)
			preloading = false;
		
		context = this;
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				dh.toggle();
			}
		});

		dh = new DrawerHelper(this, muninFoo);

		Util.UI.applySwag(this);

		this.isDrawerOpened = false;
		this.materialMenu = new MaterialMenuIconToolbar(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN) {
			@Override public int getToolbarViewId() {
				return R.id.toolbar;
			}
		};
		this.materialMenu.setNeverDrawTouch(true);
		dh.getDrawerLayout().setDrawerListener(new DrawerLayout.DrawerListener() {
			@Override
			public void onDrawerSlide(View view, float slideOffset) {
				materialMenu.setTransformationOffset(
						MaterialMenuDrawable.AnimationState.BURGER_ARROW,
						isDrawerOpened ? 2 - slideOffset : slideOffset
				);
			}

			@Override
			public void onDrawerOpened(View view) {
				isDrawerOpened = true;
				materialMenu.animatePressedState(MaterialMenuDrawable.IconState.ARROW);
			}

			@Override
			public void onDrawerClosed(View view) {
				isDrawerOpened = false;
				materialMenu.animatePressedState(MaterialMenuDrawable.IconState.BURGER);
			}

			@Override
			public void onDrawerStateChanged(int i) { }
		});
		
		Fonts.setFont(this, (TextView)findViewById(R.id.main_clear_appname), CustomFont.RobotoCondensed_Regular);
		
		if (loaded)
			onLoadFinished();
		else
			preload();
	}
	
	/**
	 * Executed when the app has loaded :
	 * 	- launching app, after the initialization
	 * 	- going back to Activity_Main
	 */
	private void onLoadFinished() {
		preloading = false;

		// Inflate menu if not already done
		if (!optionsMenuLoaded)
			createOptionsMenu();
		
		// Ask the user to rate the app
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setTitle(getText(R.string.rate))
			.setIcon(R.drawable.launcher_icon)
			.setMessage(getText(R.string.rate_long))
			.setPositiveButton(getText(R.string.text33), null) // Yes
			.setNegativeButton(getText(R.string.text34), null) // No
			.setNeutralButton(getText(R.string.not_now), null); // Not now
		new AppRate(this)
			.setCustomDialog(builder)
			.setMinDaysUntilPrompt(8)
			.setMinLaunchesUntilPrompt(10)
			.init();
		
		// Display the "follow on Twitter" message
		// after X launches
		displayTwitterAlertIfNeeded();

		// Reset drawer
		dh.reset();
		dh.toggle();
		materialMenu.animatePressedState(MaterialMenuDrawable.IconState.ARROW);
	}
	
	@Override
	public void onBackPressed() {
		if (doubleBackPressed) {
			// Close the app when tapping twice on it.
			// Useful when going in GraphView from widgets
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		
		doubleBackPressed = true;
		
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				doubleBackPressed = false;
			}
		}, 2000);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home)
			dh.closeDrawerIfOpened();

		switch (item.getItemId()) {
			case android.R.id.home:
				dh.toggle();
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
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;

		if (!preloading && !optionsMenuLoaded)
			createOptionsMenu();
		
		return true;
	}
	private void createOptionsMenu() {
		if (menu == null)
			return;

		optionsMenuLoaded = true;
		menu.clear();
		getMenuInflater().inflate(R.menu.main, menu);
	}
	
	private void displayTwitterAlertIfNeeded() {
		int NB_LAUNCHES = 8;
		String nbLaunches = Util.getPref(this, Util.PrefKeys.Twitter_NbLaunches);
		if (nbLaunches.equals(""))
			Util.setPref(this, Util.PrefKeys.Twitter_NbLaunches, "1");
		else if (!nbLaunches.equals("ok")) {
			int n = Integer.parseInt(nbLaunches);
			if (n == NB_LAUNCHES) {
				// Display message
				AlertDialog.Builder builder = new AlertDialog.Builder(Activity_Main.this);
				builder.setMessage("Be the first to try beta versions of the app, and learn cool news like upcoming updates and known issues!")
				.setTitle("Follow Munin for Android on Twitter")
				.setCancelable(true)
				// Yes
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						try {
						   startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=muninforandroid")));
						} catch (Exception e) {
						   startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/#!/muninforandroid")));
						}
					}
				})
				// No
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
				Util.setPref(this, Util.PrefKeys.Twitter_NbLaunches, "ok");
			} else
				Util.setPref(this, Util.PrefKeys.Twitter_NbLaunches, String.valueOf(n+1));
		}
	}
	
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
	
	private void preload() {
		boolean updateOperations = !Util.getPref(context, Util.PrefKeys.LastMFAVersion).equals(MuninFoo.VERSION + "");
		
		
		if (updateOperations) {
			if (myProgressDialog == null || !myProgressDialog.isShowing())
				myProgressDialog = ProgressDialog.show(context, "", getString(R.string.text39), true);
			// Please wait while the app does some update operations
			new UpdateOperations().execute();
		} else
			onLoadFinished();
	}
	
	private void updateActions() {
		if (Util.getPref(context, Util.PrefKeys.Lang).equals(""))
			Util.setPref(context, Util.PrefKeys.Lang, Locale.getDefault().getLanguage());
		
		if (Util.getPref(context, Util.PrefKeys.GraphviewOrientation).equals(""))
			Util.setPref(context, Util.PrefKeys.GraphviewOrientation, "auto");
		
		if (Util.getPref(context, Util.PrefKeys.DefaultScale).equals(""))
			Util.setPref(context, Util.PrefKeys.DefaultScale, "day");
		
		if (Util.hasPref(context, Util.PrefKeys.Drawer))
			Util.removePref(context, Util.PrefKeys.Drawer);
		
		if (Util.hasPref(context, Util.PrefKeys.Splash))
			Util.removePref(context, Util.PrefKeys.Splash);
		
		if (Util.hasPref(context, Util.PrefKeys.ListViewMode))
			Util.removePref(context, Util.PrefKeys.ListViewMode);
		
		if (Util.hasPref(context, Util.PrefKeys.Transitions))
			Util.removePref(context, Util.PrefKeys.Transitions);

		if (!Util.hasPref(context, Util.PrefKeys.GraphsZoom))
			Util.setPref(context, Util.PrefKeys.GraphsZoom, "true");
		
		// MfA 3.0 : moved auth attributes from MuninServer to MuninMaster : migrate those if possible
		String strFromVersion = Util.getPref(context, Util.PrefKeys.LastMFAVersion);
		double fromVersion = 0;
		if (!strFromVersion.equals(""))
			fromVersion = Double.parseDouble(Util.getPref(context, Util.PrefKeys.LastMFAVersion));

		if (fromVersion < 4.2) // 4.2 = V3.0
			muninFoo.sqlite.migrateTo3();
		
		Util.setPref(context, Util.PrefKeys.LastMFAVersion, MuninFoo.VERSION + "");
		muninFoo.resetInstance(this);
	}
	
	private class UpdateOperations extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			updateActions();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// When rotating the device while updating : may crash
			if (myProgressDialog != null && myProgressDialog.isShowing()) {
				try {
					myProgressDialog.dismiss();
				} catch (Exception ex) { ex.printStackTrace(); }
			}
			
			onLoadFinished();
		}
	}

	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		materialMenu.syncState(savedInstanceState);
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		materialMenu.onSaveInstanceState(outState);
	}
}