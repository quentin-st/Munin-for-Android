package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.async.AppUpdater;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.I18nHelper;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.tjeannin.apprate.AppRate;

import java.util.Locale;

import io.fabric.sdk.android.Fabric;

/**
 * We are not extending MuninActivity from here since the activity cycle
 * is very different from others (showing UI elements only when the app
 * is loaded)
 */
public class Activity_Main extends AppCompatActivity implements IGridActivity, ILabelsActivity, IAlertsActivity {
	private MuninFoo		muninFoo;
	private Settings		settings;

	private Toolbar        toolbar;
	private Menu 			menu;
	private MenuItem       menu_grid_refresh;
	private MenuItem       menu_grid_changePeriod;
	private boolean		doubleBackPressed;
	private ProgressBar     progressBar;

	private DrawerHelper dh;
	
	// Preloading
	private boolean preloading;
	private boolean optionsMenuLoaded;
	private Context context;
	public ProgressDialog progressDialog;

	// Fragments
	private enum MainFragment { NONE, GRID, LABEL, ALERTS }
	private MainFragment mainFragment;
	private Fragment fragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Fabric.with(this, new Crashlytics());
		preloading = true;
		boolean loaded = MuninFoo.isLoaded();
		muninFoo = MuninFoo.getInstance(this);
		settings = muninFoo.getSettings();

		if (!BuildConfig.DEBUG) {
			Tracker tracker = this.muninFoo.getDefaultTracker(this);
			tracker.setScreenName(this.getClass().getSimpleName());
			tracker.send(new HitBuilders.ScreenViewBuilder().build());
		}

		optionsMenuLoaded = false;
		if (loaded)
			preloading = false;
		
		context = this;
		setContentView(R.layout.activity_main);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				dh.toggle();
			}
		});

		dh = new DrawerHelper(this, muninFoo, this.toolbar);

		progressBar = Util.UI.prepareGmailStyleProgressBar(this, getSupportActionBar());
		
		if (loaded)
			onLoadFinished();
		else
			preload();
	}
	
	/**
	 * Executed when the app has loaded :
	 * 	- launching app, after the initialization
	 * 	- going back to Activity_Main
	 */
    public void onLoadFinished() {
		preloading = false;
		
		// Ask the user to rate the app
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setTitle(getText(R.string.rate))
			.setIcon(R.drawable.launcher_icon)
			.setMessage(getText(R.string.rate_long))
			.setPositiveButton(getText(R.string.yes), null)
			.setNegativeButton(getText(R.string.no), null)
			.setNeutralButton(getText(R.string.not_now), null);
		new AppRate(this)
			.setCustomDialog(builder)
			.setMinDaysUntilPrompt(8)
			.setMinLaunchesUntilPrompt(10)
			.init();

		displayI18nAlertIfNeeded();

		// Load fragment if needed
		mainFragment = MainFragment.NONE;

		if (!settings.has(Settings.PrefKeys.DefaultActivity)) {
			if (muninFoo.sqlite.dbHlpr.hasGrids() && muninFoo.premium || !muninFoo.labels.isEmpty()) {
				findViewById(R.id.setDefaultActivity).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent intent = new Intent(Activity_Main.this, Activity_Settings.class);
						intent.putExtra("highlightDefaultActivity", true);
						startActivity(intent);
						Util.setTransition(Activity_Main.this, Util.TransitionStyle.DEEPER);
					}
				});
			} else
				findViewById(R.id.setDefaultActivity).setVisibility(View.GONE);
		} else {
			switch (settings.getString(Settings.PrefKeys.DefaultActivity)) {
				case "grid": {
					mainFragment = MainFragment.GRID;
					findViewById(R.id.empty_layout).setVisibility(View.GONE);
					findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

					// Prepare fragment
					boolean autoLoad = settings.getBool(Settings.PrefKeys.DefaultActivity_Grid_AutoloadGraphs);
					fragment = new Fragment_Grid();
					Bundle bundle = new Bundle();
					int gridId = muninFoo.getSettings().getInt(Settings.PrefKeys.DefaultActivity_GridId);
					bundle.putLong(Fragment_Grid.ARG_GRIDID, gridId);
					bundle.putBoolean(Fragment_Grid.ARG_AUTOLOAD, autoLoad);
					fragment.setArguments(bundle);
					getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
					break;
				}
				case "label": {
					mainFragment = MainFragment.LABEL;
					findViewById(R.id.empty_layout).setVisibility(View.GONE);
					findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

					fragment = new Fragment_LabelsItemsList();
					Bundle bundle = new Bundle();
					long labelId = muninFoo.getSettings().getInt(Settings.PrefKeys.DefaultActivity_LabelId);
					bundle.putLong("labelId", labelId);
					fragment.setArguments(bundle);
					getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
					Label label = muninFoo.getLabel(labelId);
					if (label != null)
						toolbar.setSubtitle(label.getName());
					break;
				}
				case "alerts":
					mainFragment = MainFragment.ALERTS;
					findViewById(R.id.empty_layout).setVisibility(View.GONE);
					findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

					fragment = new Fragment_Alerts();
					getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
					break;
			}
		}

		// Reset drawer
		dh.reset();
		if (mainFragment == MainFragment.NONE)
			dh.toggle();

	    // Inflate menu if not already done
	    if (!optionsMenuLoaded)
		    createOptionsMenu();
	}
	
	@Override
	public void onBackPressed() {
        if (dh.closeDrawerIfOpen())
            return;

        if (doubleBackPressed) {
			// Close the app when tapping twice on it.
			// Useful when going in GraphView from widgets
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		
		doubleBackPressed = true;
		
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				doubleBackPressed = false;
			}
		}, 2000);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			/* Fragments */
			case R.id.menu_open:
				switch (mainFragment) {
					case GRID: {
						Intent intent = new Intent(context, Activity_Grid.class);
						Grid grid = ((Fragment_Grid) fragment).getGrid();
						intent.putExtra(Activity_Grid.ARG_GRIDID, grid.getId());
						startActivity(intent);
						Util.setTransition(this, Util.TransitionStyle.DEEPER);
						break;
					}
					case LABEL: {
						Intent intent = new Intent(context, Activity_Labels.class);
						int labelId = muninFoo.getSettings().getInt(Settings.PrefKeys.DefaultActivity_LabelId);
						intent.putExtra("labelId", labelId);
						startActivity(intent);
						Util.setTransition(this, Util.TransitionStyle.SHALLOWER);
						break;
					}
					case ALERTS:
						startActivity(new Intent(this, Activity_Alerts.class));
						break;
				}
				break;
			/* Fragment - Grid */
			case R.id.menu_grid_refresh:
				((Fragment_Grid) fragment).refresh();
				break;
			case R.id.period_day:
			case R.id.period_week:
			case R.id.period_month:
			case R.id.period_year:
				// Get Period from menu item
				MuninPlugin.Period period;
				switch (item.getItemId())  {
					case R.id.period_day: period = MuninPlugin.Period.DAY; break;
					case R.id.period_week: period = MuninPlugin.Period.WEEK; break;
					case R.id.period_month: period = MuninPlugin.Period.MONTH; break;
					case R.id.period_year: period = MuninPlugin.Period.YEAR; break;
					default: period = MuninPlugin.Period.DAY; break; // Arbitrary default
				}

				Fragment_Grid fragment_grid = (Fragment_Grid) this.fragment;
				fragment_grid.setCurrentPeriod(period);
				menu_grid_changePeriod.setTitle(period.getLabel(this));
				fragment_grid.refresh();
				return true;
			/* Fragment - Alerts */
			case R.id.menu_alerts_refresh:
				((Fragment_Alerts) fragment).refresh(true);
				break;
		}

		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;

		if (!preloading && !optionsMenuLoaded)
			createOptionsMenu();
		
		return true;
	}
	private void createOptionsMenu() {
		if (menu == null)
			return;

		optionsMenuLoaded = true;
		menu.clear();
		getMenuInflater().inflate(R.menu.main, menu);

		// Fragments menu items
		menu.findItem(R.id.menu_open).setVisible(mainFragment != MainFragment.NONE);

		// Grid
		// _refresh and _changePeriod are set visible when the user hits the "Load" button
		menu_grid_refresh = menu.findItem(R.id.menu_grid_refresh);
		menu_grid_changePeriod = menu.findItem(R.id.menu_grid_period);
		// If autoLoad, toggle their visibility
		if (settings.getString(Settings.PrefKeys.DefaultActivity, "none").equals("grid")
				&& settings.getBool(Settings.PrefKeys.DefaultActivity_Grid_AutoloadGraphs)) {
			onManualLoad();
		}

		// Alerts
		menu.findItem(R.id.menu_alerts_refresh).setVisible(mainFragment == MainFragment.ALERTS);
	}

	private void displayI18nAlertIfNeeded() {
		// Only display the alertDialog if the device language is not fr/en/de/ru/...
		String deviceLanguage = Locale.getDefault().getLanguage();

		if (!I18nHelper.isLanguageSupported(deviceLanguage)) {
			if (!muninFoo.getSettings().getBool(Settings.PrefKeys.I18NDialogShown, false)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.alert_i18n)
						.setCancelable(true)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								AlertDialog.Builder builder = new AlertDialog.Builder(context);
								builder.setMessage(R.string.alert_i18n_yes)
										.setCancelable(true)
										.setPositiveButton(R.string.openInBrowser, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialogInterface, int i) {
												startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.munin-for-android.com/i18n.php")));
											}
										})
										.setNegativeButton(R.string.close, null);
								builder.create().show();
							}
						})
						.setNegativeButton(R.string.no, null);
				builder.create().show();

				muninFoo.getSettings().set(Settings.PrefKeys.I18NDialogShown, true);
			}
		}
	}
	
	private void preload() {
		boolean updateOperations = !muninFoo.getSettings().getString(Settings.PrefKeys.LastDbVersion).equals(MuninFoo.DB_VERSION + "");
		
		
		if (updateOperations) {
			if (progressDialog == null || !progressDialog.isShowing())
				progressDialog = ProgressDialog.show(context, "", getString(R.string.text39), true);
			// Please wait while the app does some update operations
			new AppUpdater(this).execute();
		} else
			onLoadFinished();
	}
	
	public void updateActions() {
		String strFromVersion = settings.getString(Settings.PrefKeys.LastDbVersion);
		double fromVersion = 0;
		if (!strFromVersion.equals(""))
			fromVersion = Double.parseDouble(settings.getString(Settings.PrefKeys.LastDbVersion));

		// Update preferences types
		if (fromVersion != 0 && fromVersion < 6.8)
			settings.migrate();

		// Update UserAgent
		if (!settings.getBool(Settings.PrefKeys.UserAgentChanged)) {
			String newUserAgent = MuninFoo.generateUserAgent(this);
			muninFoo.setUserAgent(newUserAgent);
			settings.set(Settings.PrefKeys.UserAgent, newUserAgent);
		}

		// Munin for Android 3.5 : added MuninNode.hdGraphUrl
		// Migrate information if there are
		for (MuninNode node : muninFoo.getNodes()) {
			if (node.getParent().isDynazoomAvailable() == MuninMaster.DynazoomAvailability.TRUE) {
				node.setHdGraphURL(node.getGraphURL());
				muninFoo.sqlite.dbHlpr.updateMuninNode(node);
			}
		}

		settings.set(Settings.PrefKeys.LastDbVersion, MuninFoo.DB_VERSION + "");
		muninFoo.resetInstance(this);
	}

	/* Grid fragment */
	@Override public void onPreviewHide() { }
	@Override public void onEditModeChange(boolean editing) { }
	@Override public void onPreview() { }
	@Override
	public void onGridLoaded(Grid grid) {
		toolbar.setSubtitle(grid.getName());
	}
	@Override
	public void onManualLoad() {
		menu_grid_refresh.setVisible(true);
		menu_grid_changePeriod.setVisible(true);
		// Set period MenuItem text
		Fragment_Grid fragment = (Fragment_Grid) this.fragment;
		menu_grid_changePeriod.setTitle(fragment.getCurrentPeriod().getLabel(this));
	}
	@Override public void onGridSaved() { }

	/* Label fragment */
	@Override public void onLabelClick(Label label) { } // Not used here
	@Override public void onLabelItemClick(int pos, String labelName, long labelId) {
		Intent intent = new Intent(context, Activity_GraphView.class);
		intent.putExtra("position", pos);
		intent.putExtra("from", "main_labels");
		intent.putExtra("label", labelName);
		intent.putExtra("labelId", labelId);
		startActivity(intent);
		Util.setTransition(this, Util.TransitionStyle.DEEPER);
	}
	@Override public void onLabelsItemsListFragmentLoaded() { }
	@Override
	public void onLabelsFragmentLoaded() { }
	@Override
	public void unselectLabel() { }

	/* Alerts fragment */
	@Override public void setLoading(boolean val) { this.progressBar.setVisibility(val ? View.VISIBLE : View.GONE); }
	@Override public void setLoadingProgress(int val) { this.progressBar.setProgress(val); }
}
