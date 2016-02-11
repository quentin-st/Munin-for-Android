package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
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

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.ChromecastHelper;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.I18nHelper;
import com.chteuchteu.munin.hlpr.ImportExportHelper;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Activity_Settings extends MuninActivity {
	private Spinner	spinner_scale;
	private Spinner	spinner_defaultNode;
	private Spinner	spinner_lang;
	private Spinner spinner_gridsLegend;
	private Spinner spinner_defaultActivity;
	private Spinner spinner_defaultActivity_grid;
	private Spinner spinner_defaultActivity_label;
	private CheckBox checkbox_alwaysOn;
	private CheckBox checkbox_autoRefresh;
	private CheckBox checkbox_graphsZoom;
	private CheckBox checkbox_hdGraphs;
	private CheckBox checkbox_disableChromecast;
	private CheckBox checkbox_autoloadGraphs;
	private EditText editText_userAgent;
	private EditText editText_chromecastAppId;
	private EditText editText_importExportServer;

	private List<Grid> grids;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_settings);
		super.onContentViewSet();

		actionBar.setTitle(getString(R.string.settingsTitle));
		
		spinner_scale = (Spinner)findViewById(R.id.spinner_scale);
		spinner_defaultNode = (Spinner)findViewById(R.id.spinner_defaultnode);
		spinner_lang = (Spinner)findViewById(R.id.spinner_lang);
		spinner_gridsLegend = (Spinner)findViewById(R.id.spinner_gridsLegend);
		spinner_defaultActivity = (Spinner)findViewById(R.id.spinner_defaultActivity);
		spinner_defaultActivity_grid = (Spinner)findViewById(R.id.spinner_defaultActivity_grid);
		spinner_defaultActivity_label = (Spinner)findViewById(R.id.spinner_defaultActivity_label);

		checkbox_alwaysOn = (CheckBox)findViewById(R.id.checkbox_screenalwayson);
		checkbox_autoRefresh = (CheckBox)findViewById(R.id.checkbox_autorefresh);
		checkbox_graphsZoom = (CheckBox)findViewById(R.id.checkbox_enablegraphszoom);
		checkbox_hdGraphs = (CheckBox)findViewById(R.id.checkbox_hdgraphs);
		checkbox_disableChromecast = (CheckBox)findViewById(R.id.checkbox_disable_chromecast);
		checkbox_autoloadGraphs = (CheckBox)findViewById(R.id.checkbox_autoload_graphs);

		editText_userAgent = (EditText)findViewById(R.id.edittext_useragent);

		editText_chromecastAppId = (EditText)findViewById(R.id.edittext_chromecastAppid);
		editText_importExportServer = (EditText)findViewById(R.id.edittext_importExportServer);
		
		
		// Spinner default period
		String[] scales = {
				getString(R.string.text47_1), getString(R.string.text47_2), getString(R.string.text47_3), getString(R.string.text47_4)
		};
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, scales);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_scale.setAdapter(dataAdapter);
		
		
		// Default node spinner
		List<String> nodesList = new ArrayList<>();
		nodesList.add(getString(R.string.auto));
		for (MuninNode node : muninFoo.getNodes())
			nodesList.add(node.getName());
		ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nodesList);
		dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_defaultNode.setAdapter(dataAdapter1);
		
		// Language spinner
		List<String> list2 = new ArrayList<>();
		for (I18nHelper.AppLanguage lang : I18nHelper.AppLanguage.values()) {
			list2.add(getString(lang.localeNameRes) + " (" + lang.langCode + ")");
		}
		ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list2);
		dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_lang.setAdapter(dataAdapter2);

		// Grids legend spinner
		String[] legendModes = {
				getString(R.string.grids_legend_none), getString(R.string.grids_legend_pluginName), getString(R.string.grids_legend_nodeName), getString(R.string.grids_legend_both)
		};
		ArrayAdapter<String> dataAdapter4 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, legendModes);
		dataAdapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_gridsLegend.setAdapter(dataAdapter4);

		// Default activity spinner
		grids = muninFoo.sqlite.dbHlpr.getGrids(muninFoo);
		String[] activities = {
				getString(R.string.grids_legend_none), getString(R.string.button_grid), getString(R.string.button_labels), getString(R.string.alertsTitle)
		};
		ArrayAdapter<String> dataAdapter5 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activities);
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

		// Default activity - Grid - Autoload graphs
		checkbox_autoloadGraphs.setChecked(settings.getBool(Settings.PrefKeys.DefaultActivity_Grid_AutoloadGraphs));

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
		for (View view : Util.getViewsByTag((ViewGroup)findViewById(R.id.settingsContainer), "set_font"))
			Util.Fonts.setFont(this, (TextView) view, CustomFont.Roboto_Medium);


		// Apply current settings
		Settings settings = muninFoo.getSettings();

		// Graph default scale
		switch (settings.getString(Settings.PrefKeys.DefaultScale)) {
			case "day": spinner_scale.setSelection(0, true); break;
			case "week": spinner_scale.setSelection(1, true); break;
			case "month": spinner_scale.setSelection(2, true); break;
			case "year": spinner_scale.setSelection(3, true); break;
		}
		
		// App language
		I18nHelper.AppLanguage appLanguage;
		if (settings.has(Settings.PrefKeys.Lang))
			appLanguage = I18nHelper.AppLanguage.get(settings.getString(Settings.PrefKeys.Lang));
		else {
			if (I18nHelper.isLanguageSupported(Locale.getDefault().getLanguage()))
				appLanguage = I18nHelper.AppLanguage.get(Locale.getDefault().getLanguage());
			else
				appLanguage = I18nHelper.AppLanguage.defaultLang();
		}

		spinner_lang.setSelection(appLanguage.getIndex());

		
		// Always on
		checkbox_alwaysOn.setChecked(settings.getBool(Settings.PrefKeys.ScreenAlwaysOn));
		
		// Auto refresh
		checkbox_autoRefresh.setChecked(settings.getBool(Settings.PrefKeys.AutoRefresh));
		
		// Graph zoom
		checkbox_graphsZoom.setChecked(settings.getBool(Settings.PrefKeys.GraphsZoom));
		
		// HD Graphs
		checkbox_hdGraphs.setChecked(settings.getBool(Settings.PrefKeys.HDGraphs));

		// Disable Chromecast
		checkbox_disableChromecast.setChecked(settings.getBool(Settings.PrefKeys.DisableChromecast));
		
		// Default node
		if (settings.has(Settings.PrefKeys.DefaultNode)) {
			String defaultNodeUrl = settings.getString(Settings.PrefKeys.DefaultNode);
			MuninNode node = muninFoo.getNode(defaultNodeUrl);
			if (node != null)
				spinner_defaultNode.setSelection(muninFoo.getNodes().indexOf(node) + 1);
		}

		// User Agent
		editText_userAgent.setText(muninFoo.getUserAgent());
		findViewById(R.id.userAgent_reset).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				editText_userAgent.setText(MuninFoo.generateUserAgent(context));
			}
		});

		// Grids legend
		switch (settings.getString(Settings.PrefKeys.GridsLegend)) {
			case "none": spinner_gridsLegend.setSelection(0); break;
			case "serverName": spinner_gridsLegend.setSelection(2); break;
			case "both": spinner_gridsLegend.setSelection(3); break;
			case "pluginName":
			default: spinner_gridsLegend.setSelection(1); break;
		}

		// Default activity
		if (settings.has(Settings.PrefKeys.DefaultActivity)) {
			switch (settings.getString(Settings.PrefKeys.DefaultActivity)) {
				case "grid":
					spinner_defaultActivity.setSelection(1);
					break;
				case "label":
					spinner_defaultActivity.setSelection(2);
					break;
				case "alerts":
					spinner_defaultActivity.setSelection(3);
					break;
			}
		}
		else
			spinner_defaultActivity.setSelection(0);

		// Default activity_grid
		if (spinner_defaultActivity.getSelectedItemPosition() == 1) {
			int gridId = settings.getInt(Settings.PrefKeys.DefaultActivity_GridId);
			for (Grid grid : grids) {
				if (grid.getId() == gridId)
					spinner_defaultActivity_grid.setSelection(grids.indexOf(grid));
			}
		}

		// Default activity label
		if (spinner_defaultActivity.getSelectedItemPosition() == 2) {
			int labelId = settings.getInt(Settings.PrefKeys.DefaultActivity_LabelId);
			spinner_defaultActivity_label.setSelection(muninFoo.labels.indexOf(muninFoo.getLabel(labelId)));
		}

		// Chromecast app id
		editText_chromecastAppId.setText(ChromecastHelper.getChromecastApplicationId(this));
		findViewById(R.id.chromecastAppId_reset).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				editText_chromecastAppId.setText(ChromecastHelper.CHROMECAST_APPLICATION_ID);
			}
		});

		// Import/export server
		editText_importExportServer.setText(ImportExportHelper.getImportExportServerUrl(this));
		findViewById(R.id.importExportServer_reset).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				editText_importExportServer.setText(ImportExportHelper.IMPORT_EXPORT_URI);
			}
		});


		// Since we manually defined the checkbox and text
		// (so the checkbox can be at the right and still have the view tinting introduced
		// on Android 5.0), we have to manually define the onclick listener on the label
		for (View view : Util.getViewsByTag((ViewGroup)findViewById(R.id.settingsContainer), "checkable")) {
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					ViewGroup row = (ViewGroup) view;
					CheckBox checkBox = (CheckBox) Util.getChild(row, AppCompatCheckBox.class);
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
		Settings settings = muninFoo.getSettings();

		// Graph default scale
		switch (spinner_scale.getSelectedItemPosition()) {
			case 0: settings.set(Settings.PrefKeys.DefaultScale, "day"); break;
			case 1: settings.set(Settings.PrefKeys.DefaultScale, "week"); break;
			case 2: settings.set(Settings.PrefKeys.DefaultScale, "month"); break;
			case 3: settings.set(Settings.PrefKeys.DefaultScale, "year"); break;
		}
		
		// App language
		I18nHelper.AppLanguage currentLang = I18nHelper.AppLanguage.get(settings.getString(Settings.PrefKeys.Lang));
		I18nHelper.AppLanguage newLang = I18nHelper.AppLanguage.values()[spinner_lang.getSelectedItemPosition()];
		settings.set(Settings.PrefKeys.Lang, newLang.langCode);

		if (currentLang != newLang)
			I18nHelper.loadLanguage(context, muninFoo, true);

		settings.set(Settings.PrefKeys.ScreenAlwaysOn, checkbox_alwaysOn.isChecked());
		settings.set(Settings.PrefKeys.AutoRefresh, checkbox_autoRefresh.isChecked());
		settings.set(Settings.PrefKeys.GraphsZoom, checkbox_graphsZoom.isChecked());
		settings.set(Settings.PrefKeys.HDGraphs, checkbox_hdGraphs.isChecked());
		
		// Default node
		int defaultNodePosition = spinner_defaultNode.getSelectedItemPosition()-1;
		if (defaultNodePosition == -1)
			settings.remove(Settings.PrefKeys.DefaultNode);
		else {
			MuninNode defaultNode = muninFoo.getNodes().get(defaultNodePosition);
			settings.set(Settings.PrefKeys.DefaultNode, defaultNode.getUrl());
		}

		// User Agent
		String oldUserAgent = settings.getString(Settings.PrefKeys.UserAgent);
		boolean userAgentChanged = oldUserAgent == null || !oldUserAgent.equals(editText_userAgent.getText().toString());
		if (userAgentChanged) {
			settings.set(Settings.PrefKeys.UserAgentChanged, true);
			settings.set(Settings.PrefKeys.UserAgent, editText_userAgent.getText().toString());
			muninFoo.setUserAgent(editText_userAgent.getText().toString());
		}

		// Grids legend
		switch (spinner_gridsLegend.getSelectedItemPosition()) {
			case 0: settings.set(Settings.PrefKeys.GridsLegend, "none"); break;
			case 1: settings.set(Settings.PrefKeys.GridsLegend, "pluginName"); break;
			case 2: settings.set(Settings.PrefKeys.GridsLegend, "serverName"); break;
			case 3: settings.set(Settings.PrefKeys.GridsLegend, "both"); break;
		}

		// Default activity
		switch (spinner_defaultActivity.getSelectedItemPosition()) {
			case 0:
				settings.remove(Settings.PrefKeys.DefaultActivity);
				settings.remove(Settings.PrefKeys.DefaultActivity_GridId);
				settings.remove(Settings.PrefKeys.DefaultActivity_LabelId);
				break;
			case 1: { // Grid
				int selectedItemPos = spinner_defaultActivity_grid.getSelectedItemPosition();

				// When there's no grid, the grids spinner is empty
				if (selectedItemPos != Spinner.INVALID_POSITION) {
					settings.set(Settings.PrefKeys.DefaultActivity, "grid");
					settings.set(Settings.PrefKeys.DefaultActivity_GridId, (int) grids.get(selectedItemPos).getId());
				}

				settings.set(Settings.PrefKeys.DefaultActivity_Grid_AutoloadGraphs, checkbox_autoloadGraphs.isChecked());
				break;
			}
			case 2: { // Label
				int selectedItemPos = spinner_defaultActivity_label.getSelectedItemPosition();

				// When there's no label, the labels spinner is empty
				if (selectedItemPos != Spinner.INVALID_POSITION) {
					settings.set(Settings.PrefKeys.DefaultActivity, "label");
					settings.set(Settings.PrefKeys.DefaultActivity_LabelId, (int) muninFoo.labels.get(selectedItemPos).getId());
				}
				break;
			}
			case 3:
				settings.set(Settings.PrefKeys.DefaultActivity, "alerts");
				break;
		}

		// Disable Chromecast
		settings.set(Settings.PrefKeys.DisableChromecast, checkbox_disableChromecast.isChecked());

		// Chromecast App Id
		settings.set(Settings.PrefKeys.ChromecastApplicationId, editText_chromecastAppId.getText().toString());

		// Import/export server
		settings.set(Settings.PrefKeys.ImportExportServer, editText_importExportServer.getText().toString());


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
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// Delete every preference
				for (Settings.PrefKeys prefKey : Settings.PrefKeys.values())
					muninFoo.getSettings().remove(prefKey);


				muninFoo.sqlite.dbHlpr.deleteGraphWidgets();
				muninFoo.sqlite.dbHlpr.deleteLabels();
				muninFoo.sqlite.dbHlpr.deleteLabelsRelations();
				muninFoo.sqlite.dbHlpr.deleteMuninPlugins();
				muninFoo.sqlite.dbHlpr.deleteMuninNodes();
				muninFoo.sqlite.dbHlpr.deleteGrids();
				muninFoo.sqlite.dbHlpr.deleteGridItemRelations();
				muninFoo.sqlite.dbHlpr.deleteMuninMasters();
				
				muninFoo.resetInstance(context);
				
				// Reset performed.
				Toast.makeText(getApplicationContext(), getString(R.string.text02), Toast.LENGTH_SHORT).show();
				
				drawerHelper.reset();
			}
		})
		.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
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
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Settings; }
	
	@Override
	public void onBackPressed() {
        if (drawerHelper.closeDrawerIfOpen())
            return;

        Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
	}
}
