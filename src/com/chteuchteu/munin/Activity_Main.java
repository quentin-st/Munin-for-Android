package com.chteuchteu.munin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.devspark.appmsg.AppMsg;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_Main extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	
	public static Button 	buttonGraphs;
	public static Button	buttonAlerts;
	public static Button	buttonNotifications;
	public static Button	buttonLabels;
	private Menu 			menu;
	private String			activityName;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (muninFoo.drawer)
				setContentView(R.layout.main_clear);
			else
				setContentView(R.layout.main);
			getActionBar().setDisplayHomeAsUpEnabled(false);
			getActionBar().setTitle("");
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_Main);
				setFont((ViewGroup)findViewById(R.id.ll_splash), "RobotoCondensed-Regular.ttf");
			} else
				setFont((ViewGroup)findViewById(R.id.buttonsContainer), "RobotoCondensed-Regular.ttf");
		}
		else {
			setContentView(R.layout.main);
			this.getWindow().getDecorView().setBackgroundColor(Color.WHITE);
		}
		
		if (Locale.getDefault().getLanguage().equals("de") && getPref("suggestLanguage").equals("") && (getPref("lang").equals("fr") || getPref("lang").equals("en"))) {
			AlertDialog.Builder builder2 = new AlertDialog.Builder(Activity_Main.this);
			builder2.setMessage("Die App ist nun auch auf Deutsch verfügbar. Möchten Sie die Sprache wechseln?")
			.setCancelable(true)
			// Yes
			.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					setPref("lang", "de");
					setPref("suggestLanguage", "true");
					startActivity(new Intent(Activity_Main.this, Activity_Main.class));
				}
			})
			// No
			.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					setPref("suggestLanguage", "true");
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
			final Button buttonSettings 	= (Button) findViewById(R.id.settingsContainer);
			final Button buttonAbout 		= (Button) findViewById(R.id.aboutContainer);
			final Button buttonServer 		= (Button) findViewById(R.id.serverContainer);
			final Button buttonPremium		= (Button) findViewById(R.id.premiumContainer);
			
			buttonGraphs.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					startActivity(new Intent(Activity_Main.this, Activity_PluginSelection.class));
					setTransition("deeper");
				}
			});
			buttonAlerts.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					startActivity(new Intent(Activity_Main.this, Activity_Alerts.class));
					setTransition("deeper");
				}
			});
			buttonLabels.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					if (muninFoo.premium) {
						startActivity(new Intent(Activity_Main.this, Activity_Labels.class));
						setTransition("deeper");
					}
				}
			});
			buttonServer.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					startActivity(new Intent(Activity_Main.this, Activity_Servers.class));
					setTransition("deeper");
				}
			});
			buttonNotifications.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					if (muninFoo.premium) {
						startActivity(new Intent(Activity_Main.this, Activity_Notifications.class));
						setTransition("deeper");
					}
				}
			});
			buttonSettings.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					startActivity(new Intent(Activity_Main.this, Activity_Settings.class));
					setTransition("deeper");
				}
			});
			buttonAbout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					startActivity(new Intent(Activity_Main.this, Activity_About.class));
					setTransition("deeper");
				}
			});
			buttonPremium.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					startActivity(new Intent(Activity_Main.this, Activity_GoPremium.class));
					setTransition("deeper");
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
				AppMsg.makeText(this, getString(R.string.text36), AppMsg.STYLE_INFO).show();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (!muninFoo.drawer) {
			boolean enable = muninFoo.currentServer != null;
			buttonGraphs.setEnabled(enable);
			buttonAlerts.setEnabled(enable);
			buttonNotifications.setEnabled(enable);
			buttonLabels.setEnabled(enable);
			
			if (!muninFoo.premium) {
				buttonNotifications.setEnabled(false);
				buttonLabels.setEnabled(false);
			}
		}
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
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Main.this, Activity_About.class));
				setTransition("deeper");
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
					//menu.clear();
					//getMenuInflater().inflate(R.menu.main, menu);
				}
			});
			dh.getDrawer().setOnCloseListener(new OnCloseListener() {
				@Override
				public void onClose() {
					getActionBar().setTitle(activityName);
					//createOptionsMenu();
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
			((Button)findViewById(R.id.settingsContainer)).setVisibility(View.GONE);
			((Button)findViewById(R.id.aboutContainer)).setVisibility(View.GONE);
		}
		getMenuInflater().inflate(R.menu.main, menu);
	}
	
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
	
	public class verifUpdate extends AsyncTask<String, Void, Void> {
		double onlineLastVersion = -1;
		@SuppressLint("SimpleDateFormat")
		@Override
		protected Void doInBackground(String... url) {
			String timeStamp = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
			if (!getPref("lastAppUpdateCheck").equals(timeStamp)) {
				String source = "";
				try {
					URL adresse = new URL("http://chteuchteu.free.fr/MuninforAndroid/version.txt");
					BufferedReader in = new BufferedReader(
							new InputStreamReader(adresse.openStream()));
					String inputLine;
					while ((inputLine = in.readLine()) != null) {
						source = source + inputLine + "\n";
					}
					in.close();  
					onlineLastVersion = Double.parseDouble(source);
				} catch (Exception e) { }
			}
			setPref("lastAppUpdateCheck", timeStamp);
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			if (onlineLastVersion != -1 && muninFoo.version < onlineLastVersion)
				((LinearLayout)findViewById(R.id.updateNotification)).setVisibility(View.VISIBLE);
			
			((LinearLayout)findViewById(R.id.updateNotification)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View actualView) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("market://details?id=com.chteuchteu.munin"));
					startActivity(intent);
				}
			});
		}
	}
	public boolean isPackageInstalled (String packageName) {
		PackageManager pm = getPackageManager();
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return false;
		}
		return true;
	}
	@Override
	public void onStart() {
		super.onStart();
		if (!muninFoo.debug)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!muninFoo.debug)
			EasyTracker.getInstance(this).activityStop(this);
	}
	
	public void setFont(ViewGroup g, String font) {
		Typeface mFont = Typeface.createFromAsset(getAssets(), font);
		setFont(g, mFont);
	}
	
	public void setFont(ViewGroup group, Typeface font) {
		int count = group.getChildCount();
		View v;
		for (int i = 0; i < count; i++) {
			v = group.getChildAt(i);
			if (v instanceof TextView || v instanceof EditText || v instanceof Button) {
				((TextView) v).setTypeface(font);
			} else if (v instanceof ViewGroup)
				setFont((ViewGroup) v, font);
		}
	}
}