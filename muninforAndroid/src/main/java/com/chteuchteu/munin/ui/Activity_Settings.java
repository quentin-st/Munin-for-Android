package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
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
	private EditText editText_userAgent;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_settings);
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

		editText_userAgent = (EditText)findViewById(R.id.edittext_useragent);
		
		
		// Spinner default period
		List<String> list = new ArrayList<>();
		list.add(getString(R.string.text47_1)); list.add(getString(R.string.text47_2));
		list.add(getString(R.string.text47_3)); list.add(getString(R.string.text47_4));
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_scale.setAdapter(dataAdapter);
		
		
		// Spinner default server
		List<String> serversList = new ArrayList<>();
		serversList.add(getString(R.string.text48_3));
		for (MuninServer server : muninFoo.getOrderedServers())
			serversList.add(server.getName());
		ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, serversList);
		dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_defaultServer.setAdapter(dataAdapter1);
		
		// Spinner language
		List<String> list2 = new ArrayList<>();
		list2.add(getString(R.string.lang_english));
		list2.add(getString(R.string.lang_french));
		list2.add(getString(R.string.lang_german));
		list2.add(getString(R.string.lang_russian));
		ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list2);
		dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_lang.setAdapter(dataAdapter2);
		
		
		// Spinner orientation
		List<String> list3 = new ArrayList<>();
		list3.add(getString(R.string.text48_1));
		list3.add(getString(R.string.text48_2));
		list3.add(getString(R.string.text48_3));
		ArrayAdapter<String> dataAdapter3 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list3);
		dataAdapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_orientation.setAdapter(dataAdapter3);
		
		// Set fonts
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title1), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title2), CustomFont.Roboto_Medium);
		
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title3), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title4), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title7), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title8), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title9), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title10), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title11), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title12), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title13), CustomFont.Roboto_Medium);
		
		// Apply current settings
		// Graph default scale
		if (Util.getPref(context, Util.PrefKeys.DefaultScale).equals("day"))
			spinner_scale.setSelection(0, true);
		else if (Util.getPref(context, Util.PrefKeys.DefaultScale).equals("week"))
			spinner_scale.setSelection(1, true);
		else if (Util.getPref(context, Util.PrefKeys.DefaultScale).equals("month"))
			spinner_scale.setSelection(2, true);
		else if (Util.getPref(context, Util.PrefKeys.DefaultScale).equals("year"))
			spinner_scale.setSelection(3, true);
		
		// App language
		String lang;
		if (!Util.getPref(context, Util.PrefKeys.Lang).equals(""))
			lang = Util.getPref(context, Util.PrefKeys.Lang);
		else
			lang = Locale.getDefault().getLanguage();

        switch (lang) {
            case "en":
                spinner_lang.setSelection(0, true);
                break;
            case "fr":
                spinner_lang.setSelection(1, true);
                break;
            case "de":
                spinner_lang.setSelection(2, true);
                break;
            case "ru":
                spinner_lang.setSelection(3, true);
                break;
        }
		
		// Graphview orientation
		if (Util.getPref(context, Util.PrefKeys.GraphviewOrientation).equals("horizontal"))
			spinner_orientation.setSelection(0);
		else if (Util.getPref(context, Util.PrefKeys.GraphviewOrientation).equals("vertical"))
			spinner_orientation.setSelection(1);
		else
			spinner_orientation.setSelection(2);
		
		// Always on
		checkbox_alwaysOn.setChecked(
				Util.getPref(context, Util.PrefKeys.ScreenAlwaysOn).equals("true"));
		
		// Auto refresh
		checkbox_autoRefresh.setChecked(
				Util.getPref(context, Util.PrefKeys.AutoRefresh).equals("true"));
		
		// Graph zoom
		checkbox_graphsZoom.setChecked(
					Util.getPref(context, Util.PrefKeys.GraphsZoom).equals("true"));
		
		// HD Graphs
		if (Util.getPref(context, Util.PrefKeys.HDGraphs).equals("false"))
			checkbox_hdGraphs.setChecked(false);
		else
			checkbox_hdGraphs.setChecked(true);
		
		// Default server
		String defaultServerUrl = Util.getPref(this, Util.PrefKeys.DefaultServer);
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

		editText_userAgent.setText(muninFoo.getUserAgent());


		// Since we manually defined the checkbox and text
		// (so the checkbox can be at the right and still have the view tinting introduced
		// on Android 5.0), we have to manually define the onclick listener on the label
		for (View view : Util.getViewsByTag((ViewGroup)findViewById(R.id.settingsContainer), "checkable")) {
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					ViewGroup row = (ViewGroup) view;
					CheckBox checkBox = (CheckBox) Util.getChild(row, android.support.v7.internal.widget.TintCheckBox.class);
					if (checkBox != null)
						checkBox.setChecked(!checkBox.isChecked());
				}
			});
		}

		// Avoid keyboard showing up because of user agent edittext
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}
	
	private void actionSave() {
		// Graph default scale
		if (spinner_scale.getSelectedItemPosition() == 0)
			Util.setPref(context, Util.PrefKeys.DefaultScale, "day");
		else if (spinner_scale.getSelectedItemPosition() == 1)
			Util.setPref(context, Util.PrefKeys.DefaultScale, "week");
		else if (spinner_scale.getSelectedItemPosition() == 2)
			Util.setPref(context, Util.PrefKeys.DefaultScale, "month");
		else if (spinner_scale.getSelectedItemPosition() == 3)
			Util.setPref(context, Util.PrefKeys.DefaultScale, "year");
		
		// App language
		String currentLang = Util.getPref(context, Util.PrefKeys.Lang);
		if (spinner_lang.getSelectedItemPosition() == 0)
			Util.setPref(context, Util.PrefKeys.Lang, "en");
		else if (spinner_lang.getSelectedItemPosition() == 1)
			Util.setPref(context, Util.PrefKeys.Lang, "fr");
		else if (spinner_lang.getSelectedItemPosition() == 2)
			Util.setPref(context, Util.PrefKeys.Lang, "de");
		else if (spinner_lang.getSelectedItemPosition() == 3)
			Util.setPref(context, Util.PrefKeys.Lang, "ru");
		else
			Util.setPref(context, Util.PrefKeys.Lang, "en");
		String newLang = Util.getPref(context, Util.PrefKeys.Lang);
		if (!currentLang.equals(newLang))
			MuninFoo.loadLanguage(context, true);
		
		// Orientation
		if (spinner_orientation.getSelectedItemPosition() == 0)
			Util.setPref(context, Util.PrefKeys.GraphviewOrientation, "horizontal");
		else if (spinner_orientation.getSelectedItemPosition() == 1)
			Util.setPref(context, Util.PrefKeys.GraphviewOrientation, "vertical");
		else
			Util.setPref(context, Util.PrefKeys.GraphviewOrientation, "auto");
		
		if (checkbox_alwaysOn.isChecked())
			Util.setPref(context, Util.PrefKeys.ScreenAlwaysOn, "true");
		else
			Util.setPref(context, Util.PrefKeys.ScreenAlwaysOn, "false");
		
		if (checkbox_autoRefresh.isChecked())
			Util.setPref(context, Util.PrefKeys.AutoRefresh, "true");
		else
			Util.setPref(context, Util.PrefKeys.AutoRefresh, "false");
		
		if (checkbox_graphsZoom.isChecked())
			Util.setPref(context, Util.PrefKeys.GraphsZoom, "true");
		else
			Util.setPref(context, Util.PrefKeys.GraphsZoom, "false");
		
		if (checkbox_hdGraphs.isChecked())
			Util.setPref(context, Util.PrefKeys.HDGraphs, "true");
		else
			Util.setPref(context, Util.PrefKeys.HDGraphs, "false");
		
		// Default server
		int defaultServerPosition = spinner_defaultServer.getSelectedItemPosition()-1;
		if (defaultServerPosition == -1)
			Util.removePref(this, Util.PrefKeys.DefaultServer);
		else {
			MuninServer defaultServer = muninFoo.getOrderedServers().get(defaultServerPosition);
			Util.setPref(this, Util.PrefKeys.DefaultServer, defaultServer.getServerUrl());
		}

		Util.setPref(context, Util.PrefKeys.UserAgent, editText_userAgent.getText().toString());
		muninFoo.setUserAgent(editText_userAgent.getText().toString());
		
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
			case R.id.menu_twitter: actionTwitter(); return true;
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
				Util.setPref(context, Util.PrefKeys.DefaultScale, "day");
				Util.setPref(context, Util.PrefKeys.AddServer_History, "");
				Util.setPref(context, Util.PrefKeys.ScreenAlwaysOn, "");
				Util.setPref(context, Util.PrefKeys.GraphsZoom, "false");
				Util.setPref(context, Util.PrefKeys.HDGraphs, "true");
				
				
				muninFoo.sqlite.dbHlpr.deleteGraphWidgets();
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

	private void actionTwitter() {
		try {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=muninforandroid")));
		} catch (Exception e) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/#!/muninforandroid")));
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