package com.chteuchteu.munin.ui;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;
import com.tjeannin.apprate.AppRate;

public class Activity_Main extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private Context		c;
	
	public static Button 	buttonGraphs;
	public static Button	buttonAlerts;
	public static Button	buttonNotifications;
	public static Button	buttonLabels;
	public static Button	buttonGrids;
	private Menu 			menu;
	private String			activityName;
	private boolean		doubleBackPressed;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		c = this;
		
		if (muninFoo.drawer)
			setContentView(R.layout.main_clear);
		else
			setContentView(R.layout.main);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("");
		
		Util.UI.applySwag(this);
		
		if (muninFoo.drawer) {
			dh = new DrawerHelper(this, muninFoo);
			dh.setDrawerActivity(dh.Activity_Main);
			Fonts.setFont(this, (ViewGroup)findViewById(R.id.ll_splash), CustomFont.RobotoCondensed_Regular);
		} else
			Fonts.setFont(this, (ViewGroup)findViewById(R.id.buttonsContainer), CustomFont.RobotoCondensed_Regular);
		
		if (Locale.getDefault().getLanguage().equals("de") && Util.getPref(c, "suggestLanguage").equals("") && (Util.getPref(c, "lang").equals("fr") || Util.getPref(c, "lang").equals("en"))) {
			AlertDialog.Builder builder2 = new AlertDialog.Builder(Activity_Main.this);
			builder2.setMessage("Die App ist nun auch auf Deutsch verfügbar. Möchten Sie die Sprache wechseln?")
			.setCancelable(true)
			// Yes
			.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Util.setPref(c, "lang", "de");
					Util.setPref(c, "suggestLanguage", "true");
					startActivity(new Intent(Activity_Main.this, Activity_Main.class));
				}
			})
			// No
			.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Util.setPref(c, "suggestLanguage", "true");
					dialog.cancel();
				}
			});
			AlertDialog alert2 = builder2.create();
			alert2.show();
		}
		
		if (!muninFoo.drawer) {
			buttonGraphs					= (Button) findViewById(R.id.graphsContainer);
			buttonAlerts					= (Button) findViewById(R.id.alertsContainer);
			buttonNotifications				= (Button) findViewById(R.id.notificationsContainer);
			buttonLabels					= (Button) findViewById(R.id.labelsContainer);
			buttonGrids						= (Button) findViewById(R.id.gridsContainer);
			final Button buttonSettings 	= (Button) findViewById(R.id.settingsContainer);
			final Button buttonAbout 		= (Button) findViewById(R.id.aboutContainer);
			final Button buttonServer 		= (Button) findViewById(R.id.serverContainer);
			final Button buttonPremium		= (Button) findViewById(R.id.premiumContainer);
			
			buttonGraphs.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Activity_Main.this, Activity_PluginSelection.class));
					Util.setTransition(c, TransitionStyle.DEEPER);
				}
			});
			buttonGrids.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Activity_Main.this, Activity_GridSelection.class));
					Util.setTransition(c, TransitionStyle.DEEPER);
				}
			});
			buttonAlerts.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Activity_Main.this, Activity_Alerts.class));
					Util.setTransition(c, TransitionStyle.DEEPER);
				}
			});
			buttonLabels.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Activity_Main.this, Activity_Labels.class));
					Util.setTransition(c, TransitionStyle.DEEPER);
				}
			});
			buttonServer.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Activity_Main.this, Activity_Servers.class));
					Util.setTransition(c, TransitionStyle.DEEPER);
				}
			});
			buttonNotifications.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (muninFoo.premium) {
						startActivity(new Intent(Activity_Main.this, Activity_Notifications.class));
						Util.setTransition(c, TransitionStyle.DEEPER);
					}
				}
			});
			buttonSettings.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Activity_Main.this, Activity_Settings.class));
					Util.setTransition(c, TransitionStyle.DEEPER);
				}
			});
			buttonAbout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Activity_Main.this, Activity_About.class));
					Util.setTransition(c, TransitionStyle.DEEPER);
				}
			});
			buttonPremium.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Activity_Main.this, Activity_GoPremium.class));
					Util.setTransition(c, TransitionStyle.DEEPER);
				}
			});
			
			if (muninFoo.premium)
				buttonPremium.setVisibility(View.GONE);
		}
		
		// Display a message after settings save
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("action")) {
			String action = thisIntent.getExtras().getString("action");
			if (action != null && action.equals("settingsSave"))
				// Settings saved successfully!
				Toast.makeText(this, getString(R.string.text36), Toast.LENGTH_SHORT).show();
		}
		
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
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (!muninFoo.drawer) {
			boolean enable = muninFoo.currentServer != null;
			buttonGraphs.setEnabled(enable);
			buttonGrids.setEnabled(enable);
			buttonAlerts.setEnabled(enable);
			buttonNotifications.setEnabled(enable);
			buttonLabels.setEnabled(enable);
			
			if (!muninFoo.premium) {
				buttonNotifications.setEnabled(false);
				buttonGrids.setEnabled(false);
			}
		}
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
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				if (muninFoo.drawer)
					dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Main.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Main.this, Activity_About.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
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
				}
			});
			dh.getDrawer().setOnCloseListener(new OnCloseListener() {
				@Override
				public void onClose() {
					getActionBar().setTitle(activityName);
				}
			});
		}
		createOptionsMenu();
		if (muninFoo.drawer)
			dh.getDrawer().toggle(false);
		return true;
	}
	private void createOptionsMenu() {
		menu.clear();
		if (!muninFoo.drawer) {
			findViewById(R.id.settingsContainer).setVisibility(View.GONE);
			findViewById(R.id.aboutContainer).setVisibility(View.GONE);
		}
		getMenuInflater().inflate(R.menu.main, menu);
	}
	
	public void displayTwitterAlertIfNeeded() {
		int NB_LAUNCHES = 3;
		String nbLaunches = Util.getPref(this, "twitter_nbLaunches");
		if (nbLaunches.equals(""))
			Util.setPref(this, "twitter_nbLaunches", "1");
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
				Util.setPref(this, "twitter_nbLaunches", "ok");
			} else
				Util.setPref(this, "twitter_nbLaunches", String.valueOf(n+1));
		}
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