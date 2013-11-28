package com.chteuchteu.munin;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

public class Activity_Splash extends Activity {
	private MuninFoo muninFoo;
	
	//protected boolean _active = true;
	protected int _splashTime = 3000; // time to display the splash screen in ms
	protected boolean splashEnabled;
	protected String activity;
	protected boolean splash;
	protected boolean updating = true;
	protected boolean splashing = true;
	
	// update thread
	protected ProgressDialog myProgressDialog; 
	final Handler uiThreadCallback = new Handler();
	
	private boolean updateOperations;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Crashlytics.start(this);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			getActionBar().hide();
		SharedPreferences prefs = this.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
		if (prefs.getString("splash", "").equals("true") || prefs.getString("splash", "").equals(""))
			splash = true;
		else
			splash = false;
		
		if (splash) {
			_splashTime = 3000;
			setContentView(R.layout.splash);
			
			/*findViewById(R.id.ll_splash).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					if (!updating)
						startActivity(new Intent(Activity_Splash.this, Activity_Main.class));
				}
			});*/
			
			/*int screenH = 0;
			if (android.os.Build.VERSION.SDK_INT >= 13) {
				Display display = getWindowManager().getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				screenH = size.y;
			} else {
				Display display = getWindowManager().getDefaultDisplay();
				screenH = display.getHeight();
			}*/
			
			//ImageView logo = (ImageView)findViewById(R.id.overlay_icon);
			TextView text = (TextView)findViewById(R.id.splash_overlay_appname);
			Typeface mFont = Typeface.createFromAsset(getAssets(), "RobotoCondensed-Regular.ttf");
			text.setTypeface(mFont);
			
			/*TranslateAnimation a1 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
					Animation.RELATIVE_TO_SELF, 0,
					Animation.ABSOLUTE, screenH,
					Animation.RELATIVE_TO_SELF, 0);
			a1.setDuration(1000);
			a1.setFillAfter(true);
			a1.setInterpolator(new AccelerateDecelerateInterpolator());
			
			TranslateAnimation a2 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
					Animation.RELATIVE_TO_SELF, 0,
					Animation.ABSOLUTE, screenH,
					Animation.RELATIVE_TO_SELF, 0);
			a2.setDuration(1050);
			a2.setFillAfter(true);
			a2.setStartOffset(100);
			a2.setInterpolator(new AccelerateDecelerateInterpolator());
			
			TranslateAnimation a3 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
					Animation.RELATIVE_TO_SELF, 0,
					Animation.ABSOLUTE, screenH,
					Animation.RELATIVE_TO_SELF, 0);
			a3.setDuration(1000);
			a3.setFillAfter(true);
			a3.setStartOffset(300);
			a3.setInterpolator(new AccelerateDecelerateInterpolator());
			
			try {
				logo.startAnimation(a1);
				text.startAnimation(a2);
				text2.startAnimation(a3);
			} catch (Exception ex) { ex.printStackTrace(); }*/
		}
		else
			_splashTime = 0;
		
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
					// do nothing
				} finally {
					if (!updating) {
						startActivity(new Intent(Activity_Splash.this, Activity_Main.class));
						finish();
					}
					splashing = false;
				}
			}
		};
		splashTread.start();
		
		// Démarrage
		muninFoo = MuninFoo.getInstance(this);
		
		onUpdateActionsThreading();
	}
	
	public void onUpdateActionsThreading() {
		updateOperations = false;
		// Conditions de déclenchement des opérations de mise à jour
		if (!getPref("lastMFAVersion").equals(muninFoo.version + "") || !getPref("serverUrl").equals("") || !getPref("server00Url").equals(""))
			updateOperations = true;
		
		if (updateOperations)
			myProgressDialog = ProgressDialog.show(Activity_Splash.this, "", getString(R.string.text39), true);
		// Please wait while the app does some update operations…
		
		new Thread() {
			@Override public void run() {
				// Traitement en arrière plan
				if (updateOperations) {
					onUpdateActions();
					myProgressDialog.dismiss();
				}
				if (!splashing) {
					startActivity(new Intent(Activity_Splash.this, Activity_Main.class));
					finish();
				}
				updating = false;
			}
		}.start();
	}
	
	public void onUpdateActions() {
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
			if (muninFoo.getServer(i).getGraphURL() == null || (muninFoo.getServer(i).getGraphURL() != null && muninFoo.getServer(i).getGraphURL().equals(""))) {
				muninFoo.getServer(i).fetchPluginsList();
				maj_save = true;
			}
		}
		if (maj_save)
			muninFoo.sqlite.saveServers();
		
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
		
		// Migration de la base de données: SharedPreferences ==> SQLite
		if (!getPref("server00Url").equals("") || !getPref("server01Url").equals("")) {
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
							m = null;
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
					
					serv.save();
					for (MuninPlugin ms : serv.getPlugins())
						ms.save();
					
					Log.v("", "Updated server " + serverNumber);
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
						MuninWidget w = new MuninWidget();
						// Recherche du serveur
						String url = getPref("widget" + id + "_Url");
						for (MuninServer s : muninFoo.getServers()) {
							if (s.equalsApprox(url)) {
								MuninServer bddInstance = muninFoo.sqlite.getBDDInstance(s);
								w.setServer(bddInstance);
								w.setPeriod(getPref("widget" + id + "_Period"));
								if (getPref("widget" + id + "_WifiOnly").equals("true"))
									w.setWifiOnly(true);
								else
									w.setWifiOnly(false);
								for (MuninPlugin p : s.getPlugins()) {
									if (p.getPluginUrl().equals(getPref("widget" + id + "_GraphUrl"))) {
										w.setPlugin(muninFoo.sqlite.getBDDInstance(p, bddInstance)); break;
									}
								}
								if (w.getPlugin() == null)
									w.setPlugin(muninFoo.sqlite.getBDDInstance(s.getPlugin(0), bddInstance));
								w.setWidgetId(id);
								
								break;
							}
						}
						w.save();
						// On les laisse au cas où il faut ajouter un fix dans la prochaine version
						/*removePref("widget" + id + "_Url");
						removePref("widget" + id + "_Period");
						removePref("widget" + id + "_WifiOnly");
						removePref("widget" + id + "_GraphUrl");*/
					}
				} catch (Exception ex) {}
			}
		}
		
		if (getPref("transitions").equals(""))
			setPref("transitions", "true");
		
		for (MuninServer s : muninFoo.getServers()) {
			if (s.getAuthType() == MuninServer.AUTH_UNKNOWN) {
				MuninServer b = muninFoo.sqlite.getBDDInstance(s);
				if (b.getAuthNeeded())
					b.setAuthType(MuninServer.AUTH_BASIC);
				else
					b.setAuthType(MuninServer.AUTH_NONE);
				b.setAuthString("");
				b.save();
			}
		}
		
		setPref("lastMFAVersion", muninFoo.version + "");
		
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.resetInstance();
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