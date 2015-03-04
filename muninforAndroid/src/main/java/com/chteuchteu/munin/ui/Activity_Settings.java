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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.I18nHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Activity_Settings extends MuninActivity {
	private Spinner	spinner_scale;
	private Spinner	spinner_defaultServer;
	private Spinner	spinner_lang;
	private Spinner	spinner_orientation;
	private Spinner    spinner_gridsLegend;
	private Spinner    spinner_defaultActivity;
	private Spinner    spinner_defaultActivity_grid;
	private Spinner    spinner_defaultActivity_label;
	private CheckBox checkbox_alwaysOn;
	private CheckBox checkbox_autoRefresh;
	private CheckBox checkbox_graphsZoom;
	private CheckBox checkbox_hdGraphs;
	private EditText editText_userAgent;

	private List<Grid> grids;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_settings);
		super.onContentViewSet();
		dh.setDrawerActivity(this);

		actionBar.setTitle(getString(R.string.settingsTitle));
		
		spinner_scale = (Spinner)findViewById(R.id.spinner_scale);
		spinner_defaultServer = (Spinner)findViewById(R.id.spinner_defaultserver);
		spinner_lang = (Spinner)findViewById(R.id.spinner_lang);
		spinner_orientation = (Spinner)findViewById(R.id.spinner_orientation);
		spinner_gridsLegend = (Spinner)findViewById(R.id.spinner_gridsLegend);
		spinner_defaultActivity = (Spinner)findViewById(R.id.spinner_defaultActivity);
		spinner_defaultActivity_grid = (Spinner)findViewById(R.id.spinner_defaultActivity_grid);
		spinner_defaultActivity_label = (Spinner)findViewById(R.id.spinner_defaultActivity_label);

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
		
		
		// Default server spinner
		List<String> serversList = new ArrayList<>();
		serversList.add(getString(R.string.text48_3));
		for (MuninServer server : muninFoo.getServers())
			serversList.add(server.getName());
		ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, serversList);
		dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_defaultServer.setAdapter(dataAdapter1);
		
		// Language spinner
		List<String> list2 = new ArrayList<>();
		for (I18nHelper.AppLanguage lang : I18nHelper.AppLanguage.values())
			list2.add(getString(lang.localeNameRes));
		ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list2);
		dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_lang.setAdapter(dataAdapter2);

		// Orientation spinner
		List<String> list3 = new ArrayList<>();
		list3.add(getString(R.string.text48_1));
		list3.add(getString(R.string.text48_2));
		list3.add(getString(R.string.text48_3));
		ArrayAdapter<String> dataAdapter3 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list3);
		dataAdapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_orientation.setAdapter(dataAdapter3);

		// Grids legend spinner
		List<String> list4 = new ArrayList<>();
		list4.add(getString(R.string.grids_legend_none));
		list4.add(getString(R.string.grids_legend_pluginName));
		list4.add(getString(R.string.grids_legend_serverName));
		list4.add(getString(R.string.grids_legend_both));
		ArrayAdapter<String> dataAdapter4 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list4);
		dataAdapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_gridsLegend.setAdapter(dataAdapter4);

		// Default activity spinner
		grids = muninFoo.sqlite.dbHlpr.getGrids(muninFoo);
		List<String> list5 = new ArrayList<>();
		list5.add(getString(R.string.grids_legend_none));
		list5.add(getString(R.string.button_grid));
		list5.add(getString(R.string.button_labels));
		list5.add(getString(R.string.alertsTitle));
		ArrayAdapter<String> dataAdapter5 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list5);
		dataAdapter5.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_defaultActivity.setAdapter(dataAdapter5);
		spinner_defaultActivity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
				findViewById(R.id.defaultActivity_grid_container).setVisibility((i == 1 && muninFoo.premium) ? View.VISIBLE : View.GONE);
				findViewById(R.id.defaultActivity_label_container).setVisibility((i == 2 && muninFoo.premium) ? View.VISIBLE : View.GONE);
			}
			@Override public void onNothingSelected(AdapterView<?> adapterView) { }
		});

		// Default activity - Grid spinner
		List<String> gridsList = new ArrayList<>();
		for (Grid grid : grids)
			gridsList.add(grid.getName());
		ArrayAdapter<String> dataAdapter6 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gridsList);
		dataAdapter6.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_defaultActivity_grid.setAdapter(dataAdapter6);
		if (grids.isEmpty() || !muninFoo.premium) {
			findViewById(R.id.title16).setAlpha(0.5f);
			findViewById(R.id.spinner_defaultActivity_grid).setEnabled(false);
		}

		// Default activity - Labels spinner
		List<String> labelsList = new ArrayList<>();
		for (Label label : muninFoo.labels)
			labelsList.add(label.getName());
		ArrayAdapter<String> dataAdapter7 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labelsList);
		dataAdapter7.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_defaultActivity_label.setAdapter(dataAdapter7);
		if (labelsList.isEmpty()) {
			findViewById(R.id.title15).setAlpha(0.5f);
			findViewById(R.id.spinner_defaultActivity_label).setEnabled(false);
		}

		// Set fonts
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title1), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title2), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title3), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title4), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title5), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title6), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title7), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title8), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title9), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title10), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title11), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title12), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title13), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title14), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title15), CustomFont.Roboto_Medium);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.title16), CustomFont.Roboto_Medium);

		// Apply current settings
		// Graph default scale
		switch (Util.getPref(context, Util.PrefKeys.DefaultScale)) {
			case "day": spinner_scale.setSelection(0, true); break;
			case "week": spinner_scale.setSelection(1, true); break;
			case "month": spinner_scale.setSelection(2, true); break;
			case "year": spinner_scale.setSelection(3, true); break;
		}
		
		// App language
		I18nHelper.AppLanguage appLanguage;
		if (!Util.getPref(context, Util.PrefKeys.Lang).equals(""))
			appLanguage = I18nHelper.AppLanguage.get(Util.getPref(context, Util.PrefKeys.Lang));
		else {
			if (I18nHelper.isLanguageSupported(Locale.getDefault().getLanguage()))
				appLanguage = I18nHelper.AppLanguage.get(Locale.getDefault().getLanguage());
			else
				appLanguage = I18nHelper.AppLanguage.defaultLang();
		}

		spinner_lang.setSelection(appLanguage.getIndex());
		
		// Graphview orientation
		switch (Util.getPref(context, Util.PrefKeys.GraphviewOrientation)) {
			case "horizontal": spinner_orientation.setSelection(0); break;
			case "vertical": spinner_orientation.setSelection(1); break;
			default: spinner_orientation.setSelection(2); break;
		}
		
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
			for (MuninServer server : muninFoo.getServers()) {
				if (server.getServerUrl().equals(defaultServerUrl)) {
					pos = i;
					break;
				}
				i++;
			}
			if (pos != -1)
				spinner_defaultServer.setSelection(pos+1);
		}

		// User Agent
		editText_userAgent.setText(muninFoo.getUserAgent());

		// Grids legend
		switch (Util.getPref(this, Util.PrefKeys.GridsLegend)) {
			case "none": spinner_gridsLegend.setSelection(0); break;
			case "pluginName":
			case "": spinner_gridsLegend.setSelection(1); break;
			case "serverName": spinner_gridsLegend.setSelection(2); break;
			case "both": spinner_gridsLegend.setSelection(3); break;
		}

		// Default activity
		switch (Util.getPref(this, Util.PrefKeys.DefaultActivity)) {
			case "": spinner_defaultActivity.setSelection(0); break;
			case "grid": spinner_defaultActivity.setSelection(1); break;
			case "label": spinner_defaultActivity.setSelection(2); break;
			case "alerts": spinner_defaultActivity.setSelection(3); break;
		}

		// Default activity_grid
		if (spinner_defaultActivity.getSelectedItemPosition() == 1) {
			int gridId = Integer.parseInt(Util.getPref(context, Util.PrefKeys.DefaultActivity_GridId));
			for (Grid grid : grids) {
				if (grid.getId() == gridId)
					spinner_defaultActivity_grid.setSelection(grids.indexOf(grid));
			}
		}

		// Default activity label
		if (spinner_defaultActivity.getSelectedItemPosition() == 2) {
			int labelId = Integer.parseInt(Util.getPref(context, Util.PrefKeys.DefaultActivity_LabelId));
			spinner_defaultActivity_label.setSelection(muninFoo.labels.indexOf(muninFoo.getLabel(labelId)));
		}


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

		// Highlight "Default Activity" when coming from main activity
		if (getIntent() != null && getIntent().getExtras() != null
				&& getIntent().getExtras().containsKey("highlightDefaultActivity")
				&& getIntent().getExtras().getBoolean("highlightDefaultActivity")) {
			final Animation animation = new AlphaAnimation(1, 0);
			animation.setDuration(500);
			animation.setInterpolator(new LinearInterpolator());
			animation.setRepeatCount(4);
			animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
			findViewById(R.id.defaultActivityContainer).startAnimation(animation);
		}
	}
	
	private void actionSave() {
		// Graph default scale
		switch (spinner_scale.getSelectedItemPosition()) {
			case 0: Util.setPref(context, Util.PrefKeys.DefaultScale, "day"); break;
			case 1: Util.setPref(context, Util.PrefKeys.DefaultScale, "week"); break;
			case 2: Util.setPref(context, Util.PrefKeys.DefaultScale, "month"); break;
			case 3: Util.setPref(context, Util.PrefKeys.DefaultScale, "year"); break;
		}
		
		// App language
		I18nHelper.AppLanguage currentLang = I18nHelper.AppLanguage.get(Util.getPref(context, Util.PrefKeys.Lang));
		I18nHelper.AppLanguage newLang = I18nHelper.AppLanguage.values()[spinner_lang.getSelectedItemPosition()];
		Util.setPref(context, Util.PrefKeys.Lang, newLang.langCode);

		if (currentLang != newLang)
			I18nHelper.loadLanguage(context, muninFoo, true);
		
		// Orientation
		switch (spinner_orientation.getSelectedItemPosition()) {
			case 0: Util.setPref(context, Util.PrefKeys.GraphviewOrientation, "horizontal"); break;
			case 1: Util.setPref(context, Util.PrefKeys.GraphviewOrientation, "vertical"); break;
			case 2: Util.setPref(context, Util.PrefKeys.GraphviewOrientation, "auto"); break;
		}
		
		Util.setPref(context, Util.PrefKeys.ScreenAlwaysOn, String.valueOf(checkbox_alwaysOn.isChecked()));
		Util.setPref(context, Util.PrefKeys.AutoRefresh, String.valueOf(checkbox_autoRefresh.isChecked()));
		Util.setPref(context, Util.PrefKeys.GraphsZoom, String.valueOf(checkbox_graphsZoom.isChecked()));
		Util.setPref(context, Util.PrefKeys.HDGraphs, String.valueOf(checkbox_hdGraphs.isChecked()));
		
		// Default server
		int defaultServerPosition = spinner_defaultServer.getSelectedItemPosition()-1;
		if (defaultServerPosition == -1)
			Util.removePref(this, Util.PrefKeys.DefaultServer);
		else {
			MuninServer defaultServer = muninFoo.getServers().get(defaultServerPosition);
			Util.setPref(this, Util.PrefKeys.DefaultServer, defaultServer.getServerUrl());
		}

		// User Agent
		boolean userAgentChanged = !Util.getPref(context, Util.PrefKeys.UserAgent).equals(editText_userAgent.getText().toString());
		if (userAgentChanged) {
			Util.setPref(context, Util.PrefKeys.UserAgentChanged, "true");
			Util.setPref(context, Util.PrefKeys.UserAgent, editText_userAgent.getText().toString());
			muninFoo.setUserAgent(editText_userAgent.getText().toString());
		}

		// Grids legend
		switch (spinner_gridsLegend.getSelectedItemPosition()) {
			case 0: Util.setPref(this, Util.PrefKeys.GridsLegend, "none"); break;
			case 1: Util.setPref(this, Util.PrefKeys.GridsLegend, "pluginName"); break;
			case 2: Util.setPref(this, Util.PrefKeys.GridsLegend, "serverName"); break;
			case 3: Util.setPref(this, Util.PrefKeys.GridsLegend, "both"); break;
		}

		// Default activity
		switch (spinner_defaultActivity.getSelectedItemPosition()) {
			case 0:
				Util.removePref(this, Util.PrefKeys.DefaultActivity);
				Util.removePref(this, Util.PrefKeys.DefaultActivity_GridId);
				Util.removePref(this, Util.PrefKeys.DefaultActivity_LabelId);
				break;
			case 1:
				Util.setPref(this, Util.PrefKeys.DefaultActivity, "grid");
				Util.setPref(this, Util.PrefKeys.DefaultActivity_GridId,
						String.valueOf(grids.get(spinner_defaultActivity_grid.getSelectedItemPosition()).getId()));
				break;
			case 2:
				Util.setPref(this, Util.PrefKeys.DefaultActivity, "label");
				Util.setPref(this, Util.PrefKeys.DefaultActivity_LabelId,
						String.valueOf(muninFoo.labels.get(spinner_defaultActivity_label.getSelectedItemPosition()).getId()));
				break;
			case 3:
				Util.setPref(this, Util.PrefKeys.DefaultActivity, "alerts");
				break;
		}

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
				// Delete every preference
				for (Util.PrefKeys prefKey : Util.PrefKeys.values())
					Util.removePref(context, prefKey);


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
			ad.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
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
			e.printStackTrace();
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
