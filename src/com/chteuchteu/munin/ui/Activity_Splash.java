package com.chteuchteu.munin.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.Widget_GraphWidget;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.Widget;
import com.crashlytics.android.Crashlytics;

public class Activity_Splash extends Activity {
	private MuninFoo muninFoo;
	
	//protected boolean _active = true;
	protected int _splashTime = 2100;
	protected String activity;
	protected boolean splash;
	protected boolean updating = true;
	protected boolean splashing = true;
	
	// update thread
	protected ProgressDialog myProgressDialog;
	
	private boolean updateOperations;
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v("", "Splashing");
		muninFoo = new MuninFoo(this);
		Crashlytics.start(this);
		
		splash = (getPref("splash").equals("true") || getPref("splash").equals(""));
		
		if (splash) {
			setContentView(R.layout.splash);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				int id = getResources().getIdentifier("config_enableTranslucentDecor", "bool", "android");
				if (id != 0 && getResources().getBoolean(id)) { // Translucent available
					Window w = getWindow();
					w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
					w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				}
			}
			TextView text = (TextView)findViewById(R.id.splash_overlay_appname);
			Typeface mFont = Typeface.createFromAsset(getAssets(), "RobotoCondensed-Regular.ttf");
			text.setTypeface(mFont);
			
			AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
			animation.setDuration(1300);
			findViewById(R.id.ll_splash).startAnimation(animation);
		}
		else
			_splashTime = 0;
		
		updateOperations = false;
		if (!getPref("lastMFAVersion").equals(muninFoo.version + "") || !getPref("serverUrl").equals("") || !getPref("server00Url").equals("")) {
			updateOperations = true;
			updating = true;
		}
		
		if (updateOperations) {
			if (myProgressDialog == null || (myProgressDialog != null && !myProgressDialog.isShowing()))
				myProgressDialog = ProgressDialog.show(Activity_Splash.this, "", getString(R.string.text39), true);
			// Please wait while the app does some update operations…
			new UpdateOperations().execute();
		} else
			updating = false;
		
		// thread for displaying the SplashScreen
		Thread splashTread = new Thread() {
			@Override
			public void run() {
				try {
					int waited = 0;
					while(waited < _splashTime) {
						sleep(100);
						waited += 100;
					}
				} catch(Exception e) {
				} finally {
					if (!updating)
						startActivity(new Intent(Activity_Splash.this, Activity_Main.class));
					splashing = false;
				}
			}
		};
		splashTread.start();
	}
	
	public void updateActions() {
		if (getPref("lastMFAVersion").equals("1.3") || getPref("lastMFAVersion").equals("1.4") || getPref("lastMFAVersion").equals("1.5") || getPref("lastMFAVersion").equals("1.6")) {
			// Nettoyage de la base de données
			String serverNumber = "";
			for (int i=0; i<100; i++) {
				if (i<10)	serverNumber = "0" + i;
				else		serverNumber = ""  + i;
				if (getPref("server" + serverNumber + "Url").equals("")) {
					removePref("server" + serverNumber + "Url");
					removePref("server" + serverNumber + "Name");
					removePref("server" + serverNumber + "Favs");
					removePref("server" + serverNumber + "Version");
					removePref("server" + serverNumber + "Plugins");
					removePref("server" + serverNumber + "AuthLogin");
					removePref("server" + serverNumber + "AuthPassword");
				}
			}
		}
		// Mise à jour de 1.3 a 1.4: modification du serveur
		if (!getPref("serverUrl").equals("")) {	// Qqch dans les anciens settings
			MuninServer migrationServ = new MuninServer(getPref("serverName"), getPref("serverUrl"));
			
			// Nouvelle recherche de plugins (vrais noms des plugins)
			try {	migrationServ.fetchPluginsList();	}
			catch (Exception ex) { }
			
			if (!getPref("authLogin").equals(""))
				migrationServ.setAuthIds(getPref("authLogin"), getPref("authPassword"));
			muninFoo.addServer(migrationServ);
			
			setPref("serverUrl", "");
		}
		// fin mise à jour
		
		// Mise à jour vers 2.0: ajout du graphURL
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
		
		if (getPref("lastMFAVersion").equals("1.8")) {
			String numberServer = "";
			for (int i=0; i<muninFoo.getHowManyServers(); i++) {
				if (i<10)	numberServer = "0" + i;
				else		numberServer = ""  + i;
				removePref("server" + numberServer + "Version");
			}
		}
		
		if (getPref("lang").equals(""))
			setPref("lang", Locale.getDefault().getLanguage());
		
		if (getPref("graphview_orientation").equals(""))
			setPref("graphview_orientation", "auto");
		
		if (getPref("defaultScale").equals(""))
			setPref("defaultScale", "day");
		
		// 2.6 : migrate database. Operations under those ones will be done on the new DB.
		File old_database = getApplicationContext().getDatabasePath("MuninforAndroid.db");
		
		boolean alreadyMigrated = Util.getPref(this, "db_migrated").equals("true");
		if (!alreadyMigrated && old_database.exists())
			muninFoo.sqlite.migrateDatabase(this);
		
		// BDD Migration : SharedPreferences ==> SQLite
		if (!getPref("server00Url").equals("")) {
			MuninServer serv;
			String		serverNumber = "0";
			String[]	pluginsStr;
			
			for (int i=0; i<100; i++) {
				if (i<10)	serverNumber = "0" + i;
				else		serverNumber = ""  + i;
				if (!getPref("server" + serverNumber + "Url").equals("")) {
					serv = new MuninServer(getPref("server" + serverNumber + "Name"), getPref("server" + serverNumber + "Url"));
					if (getPref("server" + serverNumber + "Plugins").contains(";"))
						pluginsStr = getPref("server" + serverNumber + "Plugins").split(";");
					else {
						pluginsStr = new String[1];
						pluginsStr[0] = getPref("server" + serverNumber + "Plugins");
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
					
					if (getPref("server" + serverNumber + "Position").equals(""))
						setPref("server" + serverNumber + "Position", i + "");
					serv.setPosition(Integer.parseInt(getPref("server" + serverNumber + "Position")));
					
					if (!getPref("server" + serverNumber + "AuthLogin").equals("") || !getPref("server" + serverNumber + "AuthPassword").equals(""))
						serv.setAuthIds(getPref("server" + serverNumber + "AuthLogin"), getPref("server" + serverNumber + "AuthPassword"));
					if (getPref("server" + serverNumber + "SSL").equals("true"))
						serv.setSSL(true);
					serv.setGraphURL(getPref("server" + serverNumber + "GraphURL"));
					
					DatabaseHelper dbHlpr = new DatabaseHelper(getApplicationContext());
					serv.setId(dbHlpr.insertMuninServer(serv));
					
					for (MuninPlugin ms : serv.getPlugins())
						ms.setId(dbHlpr.insertMuninPlugin(ms));
					
					removePref("server" + serverNumber + "Url");
					removePref("server" + serverNumber + "Name");
					removePref("server" + serverNumber + "Plugins");
					removePref("server" + serverNumber + "Position");
					removePref("server" + serverNumber + "AuthLogin");
					removePref("server" + serverNumber + "AuthPassword");
					removePref("server" + serverNumber + "SSL");
					removePref("server" + serverNumber + "GraphURL");
				}
			}
			
			// Migration widgets
			Context context = getApplicationContext();
			ComponentName name = new ComponentName(context, Widget_GraphWidget.class);
			int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
			
			if (ids.length > 0 && !getPref("widget" + ids[0] + "_Url").equals("")) {
				try {
					for (int id : ids) {
						Widget w = new Widget();
						// Recherche du serveur
						String url = getPref("widget" + id + "_Url");
						for (MuninServer s : muninFoo.getServers()) {
							if (s.equalsApprox(url)) {
								w.setPeriod(getPref("widget" + id + "_Period"));
								if (getPref("widget" + id + "_WifiOnly").equals("true"))
									w.setWifiOnly(true);
								else
									w.setWifiOnly(false);
								for (MuninPlugin p : s.getPlugins()) {
									if (p.getPluginUrl().equals(getPref("widget" + id + "_GraphUrl"))) {
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
						
						removePref("widget" + id + "_Url");
						removePref("widget" + id + "_Period");
						removePref("widget" + id + "_WifiOnly");
						removePref("widget" + id + "_GraphUrl");
					}
				} catch (Exception ex) {}
			}
		}
		
		if (getPref("transitions").equals(""))
			setPref("transitions", "true");
		
		List<MuninServer> servers = muninFoo.getServers();
		DatabaseHelper dbHlpr = null;
		if (servers.size() > 0)
			dbHlpr = new DatabaseHelper(getApplicationContext());
		for (MuninServer s : muninFoo.getServers()) {
			if (s.getAuthType() == MuninServer.AUTH_UNKNOWN) {
				MuninServer b = muninFoo.sqlite.getBDDInstance(s);
				if (b.isAuthNeeded())
					b.setAuthType(MuninServer.AUTH_BASIC);
				else
					b.setAuthType(MuninServer.AUTH_NONE);
				b.setAuthString("");
				dbHlpr.updateMuninServer(b);
			}
		}
		
		setPref("lastMFAVersion", muninFoo.version + "");
		
		muninFoo.resetInstance(this);
	}
	
	private class UpdateOperations extends AsyncTask<Void, Integer, Void>
	{
		@Override
		protected Void doInBackground(Void... arg0) {
			updateActions();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			myProgressDialog.dismiss();
			if (!splashing)
				startActivity(new Intent(Activity_Splash.this, Activity_Main.class));
			updating = false;
		}
	}
	
	// SHARED PREFERENCES
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
}