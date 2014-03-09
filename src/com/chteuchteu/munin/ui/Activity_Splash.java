package com.chteuchteu.munin.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.Widget_GraphWidget;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;
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
	
	private Context c;
	
	// update thread
	protected ProgressDialog myProgressDialog;
	
	private boolean updateOperations;
	
	private boolean migration = false;
	private boolean migrationSuccess = false;
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = new MuninFoo(this);
		c = this;
		Crashlytics.start(this);
		
		splash = (Util.getPref(c, "splash").equals("true") || Util.getPref(c, "splash").equals(""));
		
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
			Fonts.setFont(this, text, CustomFont.RobotoCondensed_Regular);
			
			AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
			animation.setDuration(1300);
			findViewById(R.id.ll_splash).startAnimation(animation);
		}
		else
			_splashTime = 0;
		
		updateOperations = false;
		if (!Util.getPref(c, "lastMFAVersion").equals(muninFoo.version + "") || !Util.getPref(c, "serverUrl").equals("") || !Util.getPref(c, "server00Url").equals("")) {
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
		if (Util.getPref(c, "lastMFAVersion").equals("1.3") || Util.getPref(c, "lastMFAVersion").equals("1.4") || Util.getPref(c, "lastMFAVersion").equals("1.5") || Util.getPref(c, "lastMFAVersion").equals("1.6")) {
			// Nettoyage de la base de données
			String serverNumber = "";
			for (int i=0; i<100; i++) {
				if (i<10)	serverNumber = "0" + i;
				else		serverNumber = ""  + i;
				if (Util.getPref(c, "server" + serverNumber + "Url").equals("")) {
					Util.removePref(c, "server" + serverNumber + "Url");
					Util.removePref(c, "server" + serverNumber + "Name");
					Util.removePref(c, "server" + serverNumber + "Favs");
					Util.removePref(c, "server" + serverNumber + "Version");
					Util.removePref(c, "server" + serverNumber + "Plugins");
					Util.removePref(c, "server" + serverNumber + "AuthLogin");
					Util.removePref(c, "server" + serverNumber + "AuthPassword");
				}
			}
		}
		// Mise à jour de 1.3 a 1.4: modification du serveur
		if (!Util.getPref(c, "serverUrl").equals("")) {	// Qqch dans les anciens settings
			MuninServer migrationServ = new MuninServer(Util.getPref(c, "serverName"), Util.getPref(c, "serverUrl"));
			
			// Nouvelle recherche de plugins (vrais noms des plugins)
			try {	migrationServ.fetchPluginsList();	}
			catch (Exception ex) { }
			
			if (!Util.getPref(c, "authLogin").equals(""))
				migrationServ.setAuthIds(Util.getPref(c, "authLogin"), Util.getPref(c, "authPassword"));
			muninFoo.addServer(migrationServ);
			
			Util.setPref(c, "serverUrl", "");
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
		
		if (Util.getPref(c, "lastMFAVersion").equals("1.8")) {
			String numberServer = "";
			for (int i=0; i<muninFoo.getHowManyServers(); i++) {
				if (i<10)	numberServer = "0" + i;
				else		numberServer = ""  + i;
				Util.removePref(c, "server" + numberServer + "Version");
			}
		}
		
		if (Util.getPref(c, "lang").equals(""))
			Util.setPref(c, "lang", Locale.getDefault().getLanguage());
		
		if (Util.getPref(c, "graphview_orientation").equals(""))
			Util.setPref(c, "graphview_orientation", "auto");
		
		if (Util.getPref(c, "defaultScale").equals(""))
			Util.setPref(c, "defaultScale", "day");
		
		// 2.6 : migrate database. Operations under those ones will be done on the new DB.
		File old_database = getApplicationContext().getDatabasePath("MuninforAndroid.db");
		
		boolean alreadyMigrated = Util.getPref(this, "db_migrated").equals("true");
		if (!alreadyMigrated && old_database.exists()) {
			migration = true;
			migrationSuccess = muninFoo.sqlite.migrateDatabase(this);
		}
		
		// BDD Migration : SharedPreferences ==> SQLite
		if (!Util.getPref(c, "server00Url").equals("")) {
			MuninServer serv;
			String		serverNumber = "0";
			String[]	pluginsStr;
			
			for (int i=0; i<100; i++) {
				if (i<10)	serverNumber = "0" + i;
				else		serverNumber = ""  + i;
				if (!Util.getPref(c, "server" + serverNumber + "Url").equals("")) {
					serv = new MuninServer(Util.getPref(c, "server" + serverNumber + "Name"), Util.getPref(c, "server" + serverNumber + "Url"));
					if (Util.getPref(c, "server" + serverNumber + "Plugins").contains(";"))
						pluginsStr = Util.getPref(c, "server" + serverNumber + "Plugins").split(";");
					else {
						pluginsStr = new String[1];
						pluginsStr[0] = Util.getPref(c, "server" + serverNumber + "Plugins");
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
					
					if (Util.getPref(c, "server" + serverNumber + "Position").equals(""))
						Util.setPref(c, "server" + serverNumber + "Position", i + "");
					serv.setPosition(Integer.parseInt(Util.getPref(c, "server" + serverNumber + "Position")));
					
					if (!Util.getPref(c, "server" + serverNumber + "AuthLogin").equals("") || !Util.getPref(c, "server" + serverNumber + "AuthPassword").equals(""))
						serv.setAuthIds(Util.getPref(c, "server" + serverNumber + "AuthLogin"), Util.getPref(c, "server" + serverNumber + "AuthPassword"));
					if (Util.getPref(c, "server" + serverNumber + "SSL").equals("true"))
						serv.setSSL(true);
					serv.setGraphURL(Util.getPref(c, "server" + serverNumber + "GraphURL"));
					
					DatabaseHelper dbHlpr = new DatabaseHelper(getApplicationContext());
					serv.setId(dbHlpr.insertMuninServer(serv));
					
					for (MuninPlugin ms : serv.getPlugins())
						ms.setId(dbHlpr.insertMuninPlugin(ms));
					
					Util.removePref(c, "server" + serverNumber + "Url");
					Util.removePref(c, "server" + serverNumber + "Name");
					Util.removePref(c, "server" + serverNumber + "Plugins");
					Util.removePref(c, "server" + serverNumber + "Position");
					Util.removePref(c, "server" + serverNumber + "AuthLogin");
					Util.removePref(c, "server" + serverNumber + "AuthPassword");
					Util.removePref(c, "server" + serverNumber + "SSL");
					Util.removePref(c, "server" + serverNumber + "GraphURL");
				}
			}
			
			// Migration widgets
			Context context = getApplicationContext();
			ComponentName name = new ComponentName(context, Widget_GraphWidget.class);
			int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
			
			if (ids.length > 0 && !Util.getPref(c, "widget" + ids[0] + "_Url").equals("")) {
				try {
					for (int id : ids) {
						Widget w = new Widget();
						// Recherche du serveur
						String url = Util.getPref(c, "widget" + id + "_Url");
						for (MuninServer s : muninFoo.getServers()) {
							if (s.equalsApprox(url)) {
								w.setPeriod(Util.getPref(c, "widget" + id + "_Period"));
								if (Util.getPref(c, "widget" + id + "_WifiOnly").equals("true"))
									w.setWifiOnly(true);
								else
									w.setWifiOnly(false);
								for (MuninPlugin p : s.getPlugins()) {
									if (p.getPluginUrl().equals(Util.getPref(c, "widget" + id + "_GraphUrl"))) {
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
						
						Util.removePref(c, "widget" + id + "_Url");
						Util.removePref(c, "widget" + id + "_Period");
						Util.removePref(c, "widget" + id + "_WifiOnly");
						Util.removePref(c, "widget" + id + "_GraphUrl");
					}
				} catch (Exception ex) {}
			}
		}
		
		if (Util.getPref(c, "transitions").equals(""))
			Util.setPref(c, "transitions", "true");
		
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
		
		Util.setPref(c, "lastMFAVersion", muninFoo.version + "");
		
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
			if (myProgressDialog != null && myProgressDialog.isShowing()) {
				try {
					myProgressDialog.dismiss();
				} catch (Exception ignored) { }
			}
			if (migration && !migrationSuccess) {
				AlertDialog.Builder builder = new AlertDialog.Builder(c);
				String message = "We changed the way data in stored in the app. Unfortunately, it seems that we weren't able to migrate the servers information from the old to the new database."
						+ "This means that you will have to re-add the servers manually. Please excuse us for the inconvenience.";
				builder.setMessage(message).setTitle("Migration failed");
				builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						if (!splashing)
							startActivity(new Intent(Activity_Splash.this, Activity_Main.class));
						updating = false;
					}
				});
				AlertDialog dial = builder.create();
				dial.show();
				
			} else {
				if (!splashing)
					startActivity(new Intent(Activity_Splash.this, Activity_Main.class));
				updating = false;
			}
		}
	}
}