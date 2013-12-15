package com.chteuchteu.munin.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_Settings extends Activity {
	private Spinner		spinner_scale;
	private Spinner		spinner_lang;
	private Spinner		spinner_orientation;
	private View		checkable_transitions;
	private View		checkable_drawer;
	private View		checkable_splash;
	
	private MuninFoo 			muninFoo;
	private DrawerHelper		dh;
	private Menu 				menu;
	private String				activityName;
	private Context				context;
	
	private double onlineLastVersion = 0.0;
	// Threading check version
	protected	ProgressDialog	myProgressDialog; 
	final 		Handler 		uiThreadCallback = new Handler();
	
	
	@SuppressLint("NewApi")
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		context = this;
		
		setContentView(R.layout.settings);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			findViewById(R.id.viewTitle).setVisibility(View.GONE);
			findViewById(R.id.viewTitleSep).setVisibility(View.GONE);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getString(R.string.settingsTitle));
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_Settings);
			}
		} else {
			this.getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.grayBackground));
			findViewById(R.id.layout_actions).setVisibility(View.VISIBLE);
			((Button)findViewById(R.id.btn_eraseData)).setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { actionReset(); } });
			((Button)findViewById(R.id.btn_checkUpdates)).setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { actionUpdate(); } });
			((Button)findViewById(R.id.btn_gplay)).setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { actionGPlay(); } });
		}
		
		spinner_scale = 	(Spinner)findViewById(R.id.spinner_scale);
		spinner_lang =		(Spinner)findViewById(R.id.spinner_lang);
		spinner_orientation = (Spinner)findViewById(R.id.spinner_orientation);
		
		checkable_drawer = inflateCheckable((ViewGroup)findViewById(R.id.checkable_drawer), getString(R.string.settings_drawer_checkbox));
		checkable_splash = inflateCheckable((ViewGroup)findViewById(R.id.checkable_splash), getString(R.string.settings_splash_checkbox));
		checkable_transitions = inflateCheckable((ViewGroup)findViewById(R.id.checkable_transitions), getString(R.string.settings_transitions_checkbox));
		
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			findViewById(R.id.switch_drawer_tv).setVisibility(View.GONE);
			checkable_drawer.setVisibility(View.GONE);
		}
		
		// Bouton sauvegarder
		findViewById(R.id.btn_settings_save).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				actionSave();
			}
		});
		
		// Spinner default period
		List<String> list = new ArrayList<String>();
		list.add(getString(R.string.text47_1)); list.add(getString(R.string.text47_2));
		list.add(getString(R.string.text47_3)); list.add(getString(R.string.text47_4));
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_scale.setAdapter(dataAdapter);
		
		
		// Spinner language
		List<String> list2 = new ArrayList<String>();
		list2.add(getString(R.string.lang_english));
		list2.add(getString(R.string.lang_french));
		list2.add(getString(R.string.lang_german));
		ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list2);
		dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_lang.setAdapter(dataAdapter2);
		
		
		// Spinner orientation
		List<String> list3 = new ArrayList<String>();
		list3.add(getString(R.string.text48_1));
		list3.add(getString(R.string.text48_2));
		list3.add(getString(R.string.text48_3));
		ArrayAdapter<String> dataAdapter3 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list3);
		dataAdapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_orientation.setAdapter(dataAdapter3);
	}
	
	public View inflateCheckable(ViewGroup container, String label) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			de.ankri.views.Switch sw = new de.ankri.views.Switch(this);
			sw.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			sw.setText(label);
			container.addView(sw);
			return sw;
		} else {
			CheckBox cb = new CheckBox(this);
			cb.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			cb.setText(label);
			container.addView(cb);
			return cb;
		}
	}
	
	public boolean getCheckableValue(View reference) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			return ((de.ankri.views.Switch)reference).isChecked();
		else
			return ((CheckBox)reference).isChecked();
	}
	
	public void setChecked(View reference, boolean checked) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			((de.ankri.views.Switch)reference).setChecked(checked);
		else
			((CheckBox)reference).setChecked(checked);
	}
	
	public void actionSave() {
		// Disable splash
		if (getCheckableValue(checkable_splash))
			setPref("splash", "false");
		else
			setPref("splash", "true");
		
		// Graph default scale
		if (spinner_scale.getSelectedItemPosition() == 0)
			setPref("defaultScale", "day");
		else if (spinner_scale.getSelectedItemPosition() == 1)
			setPref("defaultScale", "week");
		else if (spinner_scale.getSelectedItemPosition() == 2)
			setPref("defaultScale", "month");
		else if (spinner_scale.getSelectedItemPosition() == 3)
			setPref("defaultScale", "year");
		
		// App language
		if (spinner_lang.getSelectedItemPosition() == 0)
			setPref("lang", "en");
		else if (spinner_lang.getSelectedItemPosition() == 1)
			setPref("lang", "fr");
		else if (spinner_lang.getSelectedItemPosition() == 2)
			setPref("lang", "de");
		else
			setPref("lang", "en");
		
		// Orientation
		if (spinner_orientation.getSelectedItemPosition() == 0)
			setPref("graphview_orientation", "horizontal");
		else if (spinner_orientation.getSelectedItemPosition() == 1)
			setPref("graphview_orientation", "vertical");
		else
			setPref("graphview_orientation", "auto");
		
		if (getCheckableValue(checkable_transitions))
			setPref("transitions", "true");
		else
			setPref("transitions", "false");
		
		if (getCheckableValue(checkable_drawer)) {
			setPref("drawer", "false");
			muninFoo.drawer = false;
		}
		else {
			removePref("drawer");
			muninFoo.drawer = true;
		}
		
		// After saving -> go back to reality
		Intent intent = new Intent(Activity_Settings.this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("action", "settingsSave");
		startActivity(intent);
		setTransition("shallower");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Disable splash
		if (getPref("splash").equals("false"))
			setChecked(checkable_splash, true);
		
		// Graph default scale
		if (getPref("defaultScale").equals("day"))
			spinner_scale.setSelection(0, true);
		else if (getPref("defaultScale").equals("week"))
			spinner_scale.setSelection(1, true);
		else if (getPref("defaultScale").equals("month"))
			spinner_scale.setSelection(2, true);
		else if (getPref("defaultScale").equals("year"))
			spinner_scale.setSelection(3, true);
		
		// App language
		String lang = "";
		if (!getPref("lang").equals(""))
			lang = getPref("lang");
		else
			lang = Locale.getDefault().getLanguage();
		
		if (lang.equals("en"))
			spinner_lang.setSelection(0, true);
		else if (lang.equals("fr"))
			spinner_lang.setSelection(1, true);
		else if (lang.equals("de"))
			spinner_lang.setSelection(2, true);
		
		// Graphview orientation
		if (getPref("graphview_orientation").equals("horizontal"))
			spinner_orientation.setSelection(0);
		else if (getPref("graphview_orientation").equals("vertical"))
			spinner_orientation.setSelection(1);
		else
			spinner_orientation.setSelection(2);
		
		// Transitions
		if (getPref("transitions").equals("false"))
			setChecked(checkable_transitions, false);
		else
			setChecked(checkable_transitions, true);
		
		// Drawer
		if (getPref("drawer").equals("false"))
			setChecked(checkable_drawer, true);
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
					menu.clear();
					getMenuInflater().inflate(R.menu.main, menu);
				}
			});
			dh.getDrawer().setOnCloseListener(new OnCloseListener() {
				@Override
				public void onClose() {
					getActionBar().setTitle(activityName);
					createOptionsMenu();
				}
			});
		}
		createOptionsMenu();
		return true;
	}
	private void createOptionsMenu() {
		menu.clear();
		getMenuInflater().inflate(R.menu.settings, menu);
		findViewById(R.id.btn_settings_save).setVisibility(View.GONE);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				if (muninFoo.drawer)
					dh.getDrawer().toggle(true);
				else {
					Intent intent = new Intent(this, Activity_Main.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					setTransition("shallower");
				}
				return true;
			case R.id.menu_save:	actionSave();	return true;
			case R.id.menu_reset:	actionReset();	return true;
			case R.id.menu_updates: actionUpdate(); return true;
			case R.id.menu_gplay:	actionGPlay();	return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Settings.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Settings.this, Activity_About.class));
				setTransition("deeper");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	public void actionReset() {
		AlertDialog.Builder builder = new AlertDialog.Builder(Activity_Settings.this);
		// Settings will be reset. Are you sure?
		builder.setMessage(getString(R.string.text01))
		.setCancelable(false)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String serverNumber;
				for (int i=0; i<100; i++) {
					if (i<10)	serverNumber = "0" + i;
					else		serverNumber = ""  + i;
					if (!getPref("server" + serverNumber + "Url").equals("")) {
						removePref("server" + serverNumber + "Url");
						removePref("server" + serverNumber + "Name");
						removePref("server" + serverNumber + "Plugins");
						removePref("server" + serverNumber + "AuthLogin");
						removePref("server" + serverNumber + "AuthPassword");
						removePref("server" + serverNumber + "GraphURL");
						removePref("server" + serverNumber + "SSL");
						removePref("server" + serverNumber + "Position");
					}
				}
				setPref("splash", "true");
				setPref("defaultScale", "day");
				setPref("log", "false");
				setPref("addserver_history", "");
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
					setPref("drawer", "true");
				
				muninFoo.sqlite.deleteLabels();
				muninFoo.sqlite.deleteWidgets();
				muninFoo.sqlite.deletePlugins();
				muninFoo.sqlite.deleteServers();
				
				muninFoo.resetInstance(context);
				
				// Reset performed.
				Toast.makeText(getApplicationContext(), getString(R.string.text02), Toast.LENGTH_SHORT).show();
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {	dialog.cancel();	}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	public void actionUpdate() {
		final AlertDialog alert = new AlertDialog.Builder(Activity_Settings.this).create();
		
		// Checking last version of Munin for Android…
		myProgressDialog = ProgressDialog.show(Activity_Settings.this, "", getString(R.string.text03), true);
		
		final Runnable runInUIThread = new Runnable() {
			public void run() {
				if (muninFoo.version < onlineLastVersion) {
					// New version available
					alert.setTitle(R.string.text04);
					// A new version of Munin for Android is available online.\nPlease update it using Google Play.
					alert.setMessage(getString(R.string.text05));
				} else if (muninFoo.version == onlineLastVersion) {
					// No update needed.
					alert.setTitle(getString(R.string.text06));
					// Munin for Android is up to date.
					alert.setMessage(getString(R.string.text07));
				} else {
					// No update needed.
					alert.setTitle(getString(R.string.text06));
					// You're running a beta version of Munin for Android. You're the best!
					alert.setMessage(getString(R.string.text08));
				}
				
				alert.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
				});
				alert.show();
			}
		};
		new Thread() {
			@Override public void run() {
				// Traitement en arrière plan
				grabVersion();
				myProgressDialog.dismiss();
				uiThreadCallback.post(runInUIThread);
			}
		}.start();
	}
	
	public void actionGPlay() {
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("market://details?id=com.chteuchteu.munin"));
			startActivity(intent);
		} catch (Exception ex) {
			final AlertDialog ad = new AlertDialog.Builder(Activity_Settings.this).create();
			// Error!
			ad.setTitle(getString(R.string.text09));
			ad.setMessage(getString(R.string.text11));
			ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					ad.dismiss();
				}
			});
			ad.setIcon(R.drawable.alerts_and_states_error);
			ad.show();
		}
	}
	
	public void grabVersion() {
		onlineLastVersion = 0;
		grabUrl check = new grabUrl();
		check.execute("");
		int timeout = 0;
		while (onlineLastVersion == 0) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			timeout++;
			if (timeout > 40)
				break;
		}
	}
	
	public class grabUrl extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... url) {
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
			return null;
		}
		@Override
		protected void onPostExecute(Void result) { }
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		setTransition("shallower");
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
}