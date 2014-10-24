package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninActivity;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Activity_Settings extends MuninActivity {
	private Spinner	spinner_scale;
	private Spinner	spinner_defaultServer;
	private Spinner	spinner_lang;
	private Spinner	spinner_orientation;
	private CheckBox checkbox_alwaysOn;
	private CheckBox checkbox_autoRefresh;
	private CheckBox checkbox_graphsZoom;
	private CheckBox checkbox_hdGraphs;
	private CheckBox checkbox_hideGraphviewArrows;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);
		super.onContentViewSet();
		dh.setDrawerActivity(DrawerHelper.Activity_Settings);

		actionBar.setTitle(getString(R.string.settingsTitle));
		
		spinner_scale = (Spinner)findViewById(R.id.spinner_scale);
		spinner_defaultServer = (Spinner)findViewById(R.id.spinner_defaultserver);
		spinner_lang = (Spinner)findViewById(R.id.spinner_lang);
		spinner_orientation = (Spinner)findViewById(R.id.spinner_orientation);
		
		checkbox_alwaysOn = (CheckBox)findViewById(R.id.checkbox_screenalwayson);
		checkbox_autoRefresh = (CheckBox)findViewById(R.id.checkbox_autorefresh);
		checkbox_graphsZoom = (CheckBox)findViewById(R.id.checkbox_enablegraphszoom);
		checkbox_hdGraphs = (CheckBox)findViewById(R.id.checkbox_hdgraphs);
		checkbox_hideGraphviewArrows = (CheckBox)findViewById(R.id.checkbox_hidearrows);
		
		
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
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title1), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title2), CustomFont.Roboto_Medium);
		
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title3), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title7), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title8), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title9), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title10), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title11), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title12), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title13), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title14), CustomFont.Roboto_Medium);
		
		// Apply current settings
		// Graph default scale
		if (Util.getPref(context, "defaultScale").equals("day"))
			spinner_scale.setSelection(0, true);
		else if (Util.getPref(context, "defaultScale").equals("week"))
			spinner_scale.setSelection(1, true);
		else if (Util.getPref(context, "defaultScale").equals("month"))
			spinner_scale.setSelection(2, true);
		else if (Util.getPref(context, "defaultScale").equals("year"))
			spinner_scale.setSelection(3, true);
		
		// App language
		String lang;
		if (!Util.getPref(context, "lang").equals(""))
			lang = Util.getPref(context, "lang");
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
		if (Util.getPref(context, "graphview_orientation").equals("horizontal"))
			spinner_orientation.setSelection(0);
		else if (Util.getPref(context, "graphview_orientation").equals("vertical"))
			spinner_orientation.setSelection(1);
		else
			spinner_orientation.setSelection(2);
		
		// Always on
		checkbox_alwaysOn.setChecked(
				Util.getPref(context, "screenAlwaysOn").equals("true"));
		
		// Auto refresh
		checkbox_autoRefresh.setChecked(
				Util.getPref(context, "autoRefresh").equals("true"));
		
		// Graph zoom
		checkbox_graphsZoom.setChecked(
					Util.getPref(context, "graphsZoom").equals("true"));
		
		// HD Graphs
		if (Util.getPref(context, "hdGraphs").equals("false"))
			checkbox_hdGraphs.setChecked(false);
		else
			checkbox_hdGraphs.setChecked(true);
		
		// Hide graphview arrows
		checkbox_hideGraphviewArrows.setChecked(
				Util.getPref(context, "hideGraphviewArrows").equals("true"));
		
		// Default server
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
	
	private void actionSave() {
		// Graph default scale
		if (spinner_scale.getSelectedItemPosition() == 0)
			Util.setPref(context, "defaultScale", "day");
		else if (spinner_scale.getSelectedItemPosition() == 1)
			Util.setPref(context, "defaultScale", "week");
		else if (spinner_scale.getSelectedItemPosition() == 2)
			Util.setPref(context, "defaultScale", "month");
		else if (spinner_scale.getSelectedItemPosition() == 3)
			Util.setPref(context, "defaultScale", "year");
		
		// App language
		String currentLang = Util.getPref(context, "lang");
		if (spinner_lang.getSelectedItemPosition() == 0)
			Util.setPref(context, "lang", "en");
		else if (spinner_lang.getSelectedItemPosition() == 1)
			Util.setPref(context, "lang", "fr");
		else if (spinner_lang.getSelectedItemPosition() == 2)
			Util.setPref(context, "lang", "de");
		else if (spinner_lang.getSelectedItemPosition() == 3)
			Util.setPref(context, "lang", "ru");
		else
			Util.setPref(context, "lang", "en");
		String newLang = Util.getPref(context, "lang");
		if (!currentLang.equals(newLang))
			MuninFoo.loadLanguage(context, true);
		
		// Orientation
		if (spinner_orientation.getSelectedItemPosition() == 0)
			Util.setPref(context, "graphview_orientation", "horizontal");
		else if (spinner_orientation.getSelectedItemPosition() == 1)
			Util.setPref(context, "graphview_orientation", "vertical");
		else
			Util.setPref(context, "graphview_orientation", "auto");
		
		if (checkbox_alwaysOn.isChecked())
			Util.setPref(context, "screenAlwaysOn", "true");
		else
			Util.setPref(context, "screenAlwaysOn", "false");
		
		if (checkbox_autoRefresh.isChecked())
			Util.setPref(context, "autoRefresh", "true");
		else
			Util.setPref(context, "autoRefresh", "false");
		
		if (checkbox_graphsZoom.isChecked())
			Util.setPref(context, "graphsZoom", "true");
		else
			Util.setPref(context, "graphsZoom", "false");
		
		if (checkbox_hdGraphs.isChecked())
			Util.setPref(context, "hdGraphs", "true");
		else
			Util.setPref(context, "hdGraphs", "false");
		
		if (checkbox_hideGraphviewArrows.isChecked())
			Util.setPref(context, "hideGraphviewArrows", "true");
		else
			Util.setPref(context, "hideGraphviewArrows", "false");
		
		// Default server
		int defaultServerPosition = spinner_defaultServer.getSelectedItemPosition()-1;
		if (defaultServerPosition == -1)
			Util.removePref(this, "defaultServer");
		else {
			MuninServer defaultServer = muninFoo.getOrderedServers().get(defaultServerPosition);
			Util.setPref(this, "defaultServer", defaultServer.getServerUrl());
		}
		
		// After saving -> go back to reality
		Toast.makeText(this, getString(R.string.text36), Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(Activity_Settings.this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
	}

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.settings, menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_save:	actionSave();	return true;
			case R.id.menu_reset:	actionReset();	return true;
			case R.id.menu_gplay:	actionGPlay();	return true;
		}

		return true;
	}
	
	private void actionReset() {
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
					if (!Util.getPref(context, "server" + serverNumber + "Url").equals("")) {
						Util.removePref(context, "server" + serverNumber + "Url");
						Util.removePref(context, "server" + serverNumber + "Name");
						Util.removePref(context, "server" + serverNumber + "Plugins");
						Util.removePref(context, "server" + serverNumber + "AuthLogin");
						Util.removePref(context, "server" + serverNumber + "AuthPassword");
						Util.removePref(context, "server" + serverNumber + "GraphURL");
						Util.removePref(context, "server" + serverNumber + "SSL");
						Util.removePref(context, "server" + serverNumber + "Position");
					}
				}
				
				Util.setPref(context, "defaultScale", "day");
				Util.setPref(context, "addserver_history", "");
				Util.setPref(context, "screenAlwaysOn", "");
				Util.setPref(context, "graphsZoom", "false");
				Util.setPref(context, "hdGraphs", "true");
				
				
				muninFoo.sqlite.dbHlpr.deleteWidgets();
				muninFoo.sqlite.dbHlpr.deleteLabels();
				muninFoo.sqlite.dbHlpr.deleteLabelsRelations();
				muninFoo.sqlite.dbHlpr.deleteMuninPlugins();
				muninFoo.sqlite.dbHlpr.deleteMuninServers();
				muninFoo.sqlite.dbHlpr.deleteGrids();
				muninFoo.sqlite.dbHlpr.deleteGridItemRelations();
				muninFoo.sqlite.dbHlpr.deleteMuninMasters();
				
				muninFoo.resetInstance(context);
				
				// Reset performed.
				Toast.makeText(getApplicationContext(), getString(R.string.text02), Toast.LENGTH_SHORT).show();
				
				dh.reset();
				//startActivity(new Intent(Activity_Settings.this, Activity_Settings.class));
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void actionGPlay() {
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
}