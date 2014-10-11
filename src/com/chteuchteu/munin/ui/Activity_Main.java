package com.chteuchteu.munin.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.Widget_GraphWidget;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;
import com.chteuchteu.munin.obj.Widget;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;
import com.tjeannin.apprate.AppRate;

public class Activity_Main extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	
	private Menu 			menu;
	private String			activityName;
	private boolean		doubleBackPressed;
	
	// Preloading
	private boolean preloading;
	private boolean optionsMenuLoaded;
	private Context context;
	private ProgressDialog myProgressDialog;
	private boolean updateOperations;
	private boolean migration;
	private boolean migrationSuccess;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		preloading = true;
		boolean loaded = MuninFoo.isLoaded();
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		optionsMenuLoaded = false;
		if (loaded)
			preloading = false;
		
		
		context = this;
		setContentView(R.layout.main_clear);
		getActionBar().setDisplayHomeAsUpEnabled(false);
		getActionBar().setTitle("");
		
		Util.UI.applySwag(this);
		Fonts.setFont(this, (TextView)findViewById(R.id.main_clear_appname), CustomFont.RobotoCondensed_Regular);
		
		if (loaded)
			onLoadFinished();
		
		// If not loaded : load :)
		if (!loaded)
			preload();
	}
	
	/**
	 * Executed when the app has loaded :
	 * 	- launching app, after the initialization
	 * 	- going back to Activity_Main
	 */
	private void onLoadFinished() {
		preloading = false;
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(dh.Activity_Main);
		
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
		
		dh.getDrawer().toggle(true);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Inflate menu if not already done
		if (preloading && !optionsMenuLoaded)
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
				dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Main.this, Activity_Settings.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Main.this, Activity_About.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
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
	
	public void displayTwitterAlertIfNeeded() {
		int NB_LAUNCHES = 8;
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
	
	private void preload() {
		Crashlytics.start(this);
		
		updateOperations = false;
		if (!Util.getPref(context, "lastMFAVersion").equals(MuninFoo.VERSION + "")
				|| !Util.getPref(context, "serverUrl").equals("")
				|| !Util.getPref(context, "server00Url").equals(""))
			updateOperations = true;
		
		
		if (updateOperations) {
			if (myProgressDialog == null || (myProgressDialog != null && !myProgressDialog.isShowing()))
				myProgressDialog = ProgressDialog.show(context, "", getString(R.string.text39), true);
			// Please wait while the app does some update operations
			new UpdateOperations().execute();
		} else
			onLoadFinished();
	}
	
	private void updateActions() {
		if (Util.getPref(context, "lastMFAVersion").equals("1.3") || Util.getPref(context, "lastMFAVersion").equals("1.4") || Util.getPref(context, "lastMFAVersion").equals("1.5") || Util.getPref(context, "lastMFAVersion").equals("1.6")) {
			// Cleaning database
			String serverNumber = "";
			for (int i=0; i<100; i++) {
				if (i<10)	serverNumber = "0" + i;
				else		serverNumber = ""  + i;
				if (Util.getPref(context, "server" + serverNumber + "Url").equals("")) {
					Util.removePref(context, "server" + serverNumber + "Url");
					Util.removePref(context, "server" + serverNumber + "Name");
					Util.removePref(context, "server" + serverNumber + "Favs");
					Util.removePref(context, "server" + serverNumber + "Version");
					Util.removePref(context, "server" + serverNumber + "Plugins");
					Util.removePref(context, "server" + serverNumber + "AuthLogin");
					Util.removePref(context, "server" + serverNumber + "AuthPassword");
				}
			}
		}
		
		// To 2.0: Adding graphUrl
		boolean maj_save = false;
		for (int i=0; i<muninFoo.getHowManyServers(); i++) {
			if (muninFoo.getServer(i).getGraphURL() == null || muninFoo.getServer(i).getGraphURL().equals("")) {
				muninFoo.getServer(i).fetchPluginsList();
				maj_save = true;
			}
		}
		if (maj_save) {
			muninFoo.sqlite.saveServers();
			muninFoo.resetInstance(this);
		}
		
		if (Util.getPref(context, "lastMFAVersion").equals("1.8")) {
			String numberServer = "";
			for (int i=0; i<muninFoo.getHowManyServers(); i++) {
				if (i<10)	numberServer = "0" + i;
				else		numberServer = ""  + i;
				Util.removePref(context, "server" + numberServer + "Version");
			}
		}
		
		if (Util.getPref(context, "lang").equals(""))
			Util.setPref(context, "lang", Locale.getDefault().getLanguage());
		
		if (Util.getPref(context, "graphview_orientation").equals(""))
			Util.setPref(context, "graphview_orientation", "auto");
		
		if (Util.getPref(context, "defaultScale").equals(""))
			Util.setPref(context, "defaultScale", "day");
		
		// 2.6 : migrate database. Operations under those ones will be done on the new DB.
		File old_database = getApplicationContext().getDatabasePath("MuninforAndroid.db");
		
		boolean alreadyMigrated = Util.getPref(this, "db_migrated").equals("true");
		if (!alreadyMigrated && old_database.exists()) {
			migration = true;
			migrationSuccess = muninFoo.sqlite.migrateDatabase(this);
		}
		
		// BDD Migration : SharedPreferences ==> SQLite
		if (!Util.getPref(context, "server00Url").equals("")) {
			MuninServer serv;
			String		serverNumber = "0";
			String[]	pluginsStr;
			
			for (int i=0; i<100; i++) {
				if (i<10)	serverNumber = "0" + i;
				else		serverNumber = ""  + i;
				if (!Util.getPref(context, "server" + serverNumber + "Url").equals("")) {
					serv = new MuninServer(Util.getPref(context, "server" + serverNumber + "Name"), Util.getPref(context, "server" + serverNumber + "Url"));
					if (Util.getPref(context, "server" + serverNumber + "Plugins").contains(";"))
						pluginsStr = Util.getPref(context, "server" + serverNumber + "Plugins").split(";");
					else {
						pluginsStr = new String[1];
						pluginsStr[0] = Util.getPref(context, "server" + serverNumber + "Plugins");
					}
					List<MuninPlugin> mp = new ArrayList<MuninPlugin>();
					MuninPlugin m;
					for (int y=0; y<pluginsStr.length; y++) {
						try {
							m = new MuninPlugin(pluginsStr[y].split(",")[0], serv);
							m.setFancyName(pluginsStr[y].split(",")[1]);
							mp.add(m);
						} catch (Exception ex) { }
					}
					serv.setPluginsList(mp);
					
					if (Util.getPref(context, "server" + serverNumber + "Position").equals(""))
						Util.setPref(context, "server" + serverNumber + "Position", i + "");
					serv.setPosition(Integer.parseInt(Util.getPref(context, "server" + serverNumber + "Position")));
					
					if (!Util.getPref(context, "server" + serverNumber + "AuthLogin").equals("") || !Util.getPref(context, "server" + serverNumber + "AuthPassword").equals(""))
						serv.setAuthIds(Util.getPref(context, "server" + serverNumber + "AuthLogin"), Util.getPref(context, "server" + serverNumber + "AuthPassword"));
					if (Util.getPref(context, "server" + serverNumber + "SSL").equals("true"))
						serv.setSSL(true);
					serv.setGraphURL(Util.getPref(context, "server" + serverNumber + "GraphURL"));
					
					DatabaseHelper dbHlpr = new DatabaseHelper(getApplicationContext());
					serv.setId(dbHlpr.insertMuninServer(serv));
					
					for (MuninPlugin ms : serv.getPlugins())
						ms.setId(dbHlpr.insertMuninPlugin(ms));
					
					Util.removePref(context, "server" + serverNumber + "Url");
					Util.removePref(context, "server" + serverNumber + "Name");
					Util.removePref(context, "server" + serverNumber + "Plugins");
					Util.removePref(context, "server" + serverNumber + "Position");
					Util.removePref(context, "server" + serverNumber + "AuthLogin");
					Util.removePref(context, "server" + serverNumber + "AuthPassword");
					Util.removePref(context, "server" + serverNumber + "SSL");
					Util.removePref(context, "server" + serverNumber + "GraphURL");
				}
			}
			
			// Migration widgets
			ComponentName name = new ComponentName(context, Widget_GraphWidget.class);
			int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
			
			if (ids.length > 0 && !Util.getPref(context, "widget" + ids[0] + "_Url").equals("")) {
				try {
					for (int id : ids) {
						Widget w = new Widget();
						// Recherche du serveur
						String url = Util.getPref(context, "widget" + id + "_Url");
						for (MuninServer s : muninFoo.getServers()) {
							if (s.equalsApprox(url)) {
								w.setPeriod(Util.getPref(context, "widget" + id + "_Period"));
								if (Util.getPref(context, "widget" + id + "_WifiOnly").equals("true"))
									w.setWifiOnly(true);
								else
									w.setWifiOnly(false);
								for (MuninPlugin p : s.getPlugins()) {
									if (p.getPluginUrl().equals(Util.getPref(context, "widget" + id + "_GraphUrl"))) {
										w.setPlugin(p); break;
									}
								}
								if (w.getPlugin() == null)
									w.setPlugin(s.getPlugin(0));
								w.setWidgetId(id);
								
								break;
							}
						}
						
						DatabaseHelper dbHlpr = new DatabaseHelper(getApplicationContext());
						dbHlpr.insertWidget(w);
						
						Util.removePref(context, "widget" + id + "_Url");
						Util.removePref(context, "widget" + id + "_Period");
						Util.removePref(context, "widget" + id + "_WifiOnly");
						Util.removePref(context, "widget" + id + "_GraphUrl");
					}
				} catch (Exception ex) {}
			}
		}
		
		if (Util.getPref(context, "transitions").equals(""))
			Util.setPref(context, "transitions", "true");
		
		List<MuninServer> servers = muninFoo.getServers();
		DatabaseHelper dbHlpr = null;
		if (servers.size() > 0)
			dbHlpr = new DatabaseHelper(getApplicationContext());
		for (MuninServer s : muninFoo.getServers()) {
			if (s.getAuthType() == AuthType.UNKNOWN) {
				MuninServer b = muninFoo.sqlite.getBDDInstance(s);
				if (b.isAuthNeeded())
					b.setAuthType(AuthType.BASIC);
				else
					b.setAuthType(AuthType.NONE);
				b.setAuthString("");
				dbHlpr.updateMuninServer(b);
			}
		}
		
		if (Util.hasPref(context, "drawer"))
			Util.removePref(context, "drawer");
		
		if (Util.hasPref(context, "splash"))
			Util.removePref(context, "splash");
		
		Util.setPref(context, "lastMFAVersion", MuninFoo.VERSION + "");
		
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
			if (myProgressDialog != null && myProgressDialog.isShowing()) {
				try {
					myProgressDialog.dismiss();
				} catch (Exception ignored) { }
			}
			if (migration && !migrationSuccess) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				String message = "We changed the way data in stored in the app. Unfortunately, it seems that we weren't able to migrate the servers information from the old to the new database."
						+ "This means that you will have to re-add the servers manually. Please excuse us for the inconvenience.";
				builder.setMessage(message).setTitle("Migration failed");
				builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						onLoadFinished();
					}
				});
				builder.show();
			} else
				onLoadFinished();
		}
	}
}