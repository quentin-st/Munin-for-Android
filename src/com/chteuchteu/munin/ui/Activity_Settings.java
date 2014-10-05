package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninServer;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_Settings extends Activity {
	private Spinner	spinner_scale;
	private Spinner	spinner_defaultServer;
	private Spinner	spinner_lang;
	private Spinner	spinner_orientation;
	private View		checkable_transitions;
	private View		checkable_drawer;
	private View		checkable_splash;
	private View		checkable_alwaysOn;
	private View		checkable_autoRefresh;
	private View		checkable_graphsZoom;
	private View		checkable_hdGraphs;
	
	private MuninFoo 		muninFoo;
	private DrawerHelper	dh;
	private Menu 			menu;
	private String			activityName;
	private Context		context;
	
	
	@SuppressLint("NewApi")
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		context = this;
		
		setContentView(R.layout.settings);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(getString(R.string.settingsTitle));
		
		if (muninFoo.drawer) {
			dh = new DrawerHelper(this, muninFoo);
			dh.setDrawerActivity(dh.Activity_Settings);
		}
		
		Util.UI.applySwag(this);
		
		spinner_scale = (Spinner)findViewById(R.id.spinner_scale);
		spinner_defaultServer = (Spinner)findViewById(R.id.spinner_defaultserver);
		spinner_lang = (Spinner)findViewById(R.id.spinner_lang);
		spinner_orientation = (Spinner)findViewById(R.id.spinner_orientation);
		
		checkable_drawer = inflateCheckable((ViewGroup)findViewById(R.id.checkable_drawer), getString(R.string.settings_drawer_checkbox));
		checkable_splash = inflateCheckable((ViewGroup)findViewById(R.id.checkable_splash), getString(R.string.settings_splash_checkbox));
		checkable_transitions = inflateCheckable((ViewGroup)findViewById(R.id.checkable_transitions), getString(R.string.settings_transitions_checkbox));
		checkable_alwaysOn = inflateCheckable((ViewGroup)findViewById(R.id.checkable_screenalwayson), getString(R.string.settings_screenalwayson_checkbox));
		checkable_autoRefresh = inflateCheckable((ViewGroup)findViewById(R.id.checkable_autorefresh), getString(R.string.settings_autorefresh_checkbox));
		checkable_graphsZoom = inflateCheckable((ViewGroup)findViewById(R.id.checkable_enablegraphszoom), getString(R.string.settings_enablegraphszoom));
		checkable_hdGraphs = inflateCheckable((ViewGroup)findViewById(R.id.checkable_hdgraphs), getString(R.string.settings_hdgraphs_text));
		
		
		// Spinner default period
		List<String> list = new ArrayList<String>();
		list.add(getString(R.string.text47_1)); list.add(getString(R.string.text47_2));
		list.add(getString(R.string.text47_3)); list.add(getString(R.string.text47_4));
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_scale.setAdapter(dataAdapter);
		
		
		// Spinner default server
		List<String> serversList = new ArrayList<String>();
		serversList.add(getString(R.string.text48_3));
		for (MuninServer server : muninFoo.getOrderedServers())
			serversList.add(server.getName());
		ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, serversList);
		dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_defaultServer.setAdapter(dataAdapter1);
		
		// Spinner language
		List<String> list2 = new ArrayList<String>();
		list2.add(getString(R.string.lang_english));
		list2.add(getString(R.string.lang_french));
		list2.add(getString(R.string.lang_german));
		list2.add(getString(R.string.lang_russian));
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
		
		// Set fonts
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title1), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title2), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title3), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title4), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title5), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title6), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title7), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title8), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title9), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title10), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title11), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title12), CustomFont.RobotoCondensed_Bold);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title13), CustomFont.RobotoCondensed_Bold);
	}
	
	public View inflateCheckable(ViewGroup container, String label) {
		de.ankri.views.Switch sw = new de.ankri.views.Switch(this);
		sw.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		sw.setText(label);
		container.addView(sw);
		return sw;
	}
	
	public boolean getCheckableValue(View reference) {
		return ((de.ankri.views.Switch)reference).isChecked();
	}
	
	public void setChecked(View reference, boolean checked) {
		((de.ankri.views.Switch)reference).setChecked(checked);
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
		else if (spinner_lang.getSelectedItemPosition() == 3)
			setPref("lang", "ru");
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
		
		if (getCheckableValue(checkable_alwaysOn))
			setPref("screenAlwaysOn", "true");
		else
			setPref("screenAlwaysOn", "false");
		
		if (getCheckableValue(checkable_autoRefresh))
			setPref("autoRefresh", "true");
		else
			setPref("autoRefresh", "false");
		
		if (getCheckableValue(checkable_graphsZoom))
			setPref("graphsZoom", "true");
		else
			setPref("graphsZoom", "false");
		
		// Drawer
		if (getCheckableValue(checkable_drawer)) {
			setPref("drawer", "false");
			muninFoo.drawer = false;
		}
		else {
			removePref("drawer");
			muninFoo.drawer = true;
		}
		
		if (getCheckableValue(checkable_hdGraphs))
			setPref("hdGraphs", "true");
		else
			setPref("hdGraphs", "false");
		
		// Default server
		int defaultServerPosition = spinner_defaultServer.getSelectedItemPosition()-1;
		if (defaultServerPosition == -1)
			Util.removePref(this, "defaultServer");
		else {
			MuninServer defaultServer = muninFoo.getOrderedServers().get(defaultServerPosition);
			Util.setPref(this, "defaultServer", defaultServer.getServerUrl());
		}
		
		// After saving -> go back to reality
		Intent intent = new Intent(Activity_Settings.this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("action", "settingsSave");
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
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
		String lang;
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
		else if (lang.equals("ru"))
			spinner_lang.setSelection(3, true);
		
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
		
		// Always on
		if (getPref("screenAlwaysOn").equals("true"))
			setChecked(checkable_alwaysOn, true);
		
		// Auto refresh
		if (getPref("autoRefresh").equals("true"))
			setChecked(checkable_autoRefresh, true);
		
		// Graph zoom
		if (getPref("graphsZoom").equals("true"))
			setChecked(checkable_graphsZoom, true);
		
		// HD Graphs
		if (getPref("hdGraphs").equals("false"))
			setChecked(checkable_hdGraphs, false);
		else
			setChecked(checkable_hdGraphs, true);
		
		// Defaut server
		String defaultServerUrl = Util.getPref(this, "defaultServer");
		if (!defaultServerUrl.equals("")) {
			int pos = -1;
			int i = 0;
			for (MuninServer server : muninFoo.getOrderedServers()) {
				if (server.getServerUrl().equals(defaultServerUrl)) {
					pos = i;
					break;
				}
				i++;
			}
			if (pos != -1)
				spinner_defaultServer.setSelection(pos+1);
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
					Util.setTransition(this, TransitionStyle.SHALLOWER);
				}
				return true;
			case R.id.menu_save:	actionSave();	return true;
			case R.id.menu_reset:	actionReset();	return true;
			case R.id.menu_gplay:	actionGPlay();	return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Settings.this, Activity_Settings.class));
				Util.setTransition(this, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Settings.this, Activity_About.class));
				Util.setTransition(this, TransitionStyle.DEEPER);
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
				setPref("screenAlwaysOn", "");
				setPref("drawer", "true");
				setPref("graphsZoom", "false");
				setPref("hdGraphs", "true");
				
				
				muninFoo.sqlite.dbHlpr.deleteWidgets();
				muninFoo.sqlite.dbHlpr.deleteLabels();
				muninFoo.sqlite.dbHlpr.deleteLabelsRelations();
				muninFoo.sqlite.dbHlpr.deleteMuninPlugins();
				muninFoo.sqlite.dbHlpr.deleteMuninServers();
				muninFoo.sqlite.dbHlpr.deleteGrids();
				muninFoo.sqlite.dbHlpr.deleteGridItemRelations();
				muninFoo.sqlite.dbHlpr.deleteMuninMasters();
				
				muninFoo.resetInstance(context);
				
				if (muninFoo.drawer)
					dh.reInitDrawer();
				
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
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
	}
	
	public String getPref(String key) {
		return this.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	public void setPref(String key, String value) {
		Util.setPref(this, key, value);
	}
	
	public void removePref(String key) {
		Util.removePref(this, key);
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		if (!MuninFoo.debug)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!MuninFoo.debug)
			EasyTracker.getInstance(this).activityStop(this);
	}
}