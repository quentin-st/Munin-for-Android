package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.extras.toolbar.MaterialMenuIconToolbar;
import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.I18nHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.tjeannin.apprate.AppRate;

import java.util.Locale;

/**
 * We are not extending MuninActivity from here since the activity cycle
 * is very different from others (showing UI elements only when the app
 * is loaded)
 */
public class Activity_Main extends ActionBarActivity implements IGridActivity, ILabelsActivity {
	private MuninFoo		muninFoo;
	private MaterialMenuIconToolbar materialMenu;

	private Toolbar        toolbar;
	private Menu 			menu;
	private boolean		doubleBackPressed;

	private DrawerHelper dh;
	private boolean isDrawerOpened;
	
	// Preloading
	private boolean preloading;
	private boolean optionsMenuLoaded;
	private Context context;
	private ProgressDialog myProgressDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Crashlytics.start(this);
		preloading = true;
		boolean loaded = MuninFoo.isLoaded();
		muninFoo = MuninFoo.getInstance(this);
		I18nHelper.loadLanguage(this, muninFoo);
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

		dh = new DrawerHelper(this, muninFoo);

		Util.UI.applySwag(this);

		this.isDrawerOpened = false;
		this.materialMenu = new MaterialMenuIconToolbar(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN) {
			@Override public int getToolbarViewId() {
				return R.id.toolbar;
			}
		};
		this.materialMenu.setNeverDrawTouch(true);
		dh.getDrawerLayout().setDrawerListener(new DrawerLayout.DrawerListener() {
			@Override
			public void onDrawerSlide(View view, float slideOffset) {
				materialMenu.setTransformationOffset(
						MaterialMenuDrawable.AnimationState.BURGER_ARROW,
						isDrawerOpened ? 2 - slideOffset : slideOffset
				);
			}

			@Override
			public void onDrawerOpened(View view) {
				isDrawerOpened = true;
				materialMenu.animatePressedState(MaterialMenuDrawable.IconState.ARROW);
			}

			@Override
			public void onDrawerClosed(View view) {
				isDrawerOpened = false;
				materialMenu.animatePressedState(MaterialMenuDrawable.IconState.BURGER);
			}

			@Override
			public void onDrawerStateChanged(int i) { }
		});
		
		Fonts.setFont(this, (TextView)findViewById(R.id.main_clear_appname), CustomFont.RobotoCondensed_Regular);
		
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
	private void onLoadFinished() {
		preloading = false;

		// Inflate menu if not already done
		if (!optionsMenuLoaded)
			createOptionsMenu();
		
		// Ask the user to rate the app
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setTitle(getText(R.string.rate))
			.setIcon(R.drawable.launcher_icon)
			.setMessage(getText(R.string.rate_long))
			.setPositiveButton(getText(R.string.yes), null) // Yes
			.setNegativeButton(getText(R.string.no), null) // No
			.setNeutralButton(getText(R.string.not_now), null); // Not now
		new AppRate(this)
			.setCustomDialog(builder)
			.setMinDaysUntilPrompt(8)
			.setMinLaunchesUntilPrompt(10)
			.init();
		
		// Display the "follow on Twitter" message
		// after X launches
		displayTwitterAlertIfNeeded();

		displayOpenSourceAlertIfNeeded();

		displayI18nAlertIfNeeded();

		// Load fragment if needed
		boolean fragmentLoaded = false;

		switch (Util.getPref(context, Util.PrefKeys.DefaultActivity)) {
			case "":
				fragmentLoaded = false;
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
				}
				else
					findViewById(R.id.setDefaultActivity).setVisibility(View.GONE);
				break;
			case "grid": {
				fragmentLoaded = true;
				findViewById(R.id.empty_layout).setVisibility(View.GONE);
				findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

				Fragment_Grid fragmentGrid = new Fragment_Grid();
				Bundle bundle = new Bundle();
				long gridId = Integer.parseInt(Util.getPref(context, Util.PrefKeys.DefaultActivity_GridId));
				bundle.putLong(Fragment_Grid.ARG_GRIDID, gridId);
				bundle.putBoolean(Fragment_Grid.ARG_AUTOLOAD, false);
				bundle.putBoolean(Fragment_Grid.ARG_OVERFLOW_ACTIONS, true);
				fragmentGrid.setArguments(bundle);
				getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragmentGrid).commit();
				break;
			}
			case "label": {
				fragmentLoaded = true;
				findViewById(R.id.empty_layout).setVisibility(View.GONE);
				findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

				Fragment_LabelsItemsList fragmentLabels = new Fragment_LabelsItemsList();
				Bundle bundle = new Bundle();
				long labelId = Integer.parseInt(Util.getPref(context, Util.PrefKeys.DefaultActivity_LabelId));
				bundle.putLong("labelId", labelId);
				fragmentLabels.setArguments(bundle);
				getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragmentLabels).commit();
				toolbar.setSubtitle(muninFoo.getLabel(labelId).getName());
				break;
			}
		}

		// Reset drawer
		dh.reset();
		if (!fragmentLoaded) {
			dh.toggle();
			materialMenu.animatePressedState(MaterialMenuDrawable.IconState.ARROW);
		}
		else
			materialMenu.animatePressedState(MaterialMenuDrawable.IconState.BURGER);
	}
	
	@Override
	public void onBackPressed() {
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
		if (item.getItemId() != android.R.id.home)
			dh.closeDrawerIfOpened();

		switch (item.getItemId()) {
			case android.R.id.home:
				dh.toggle();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(context, Activity_Settings.class));
				Util.setTransition(context, Util.TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(context, Activity_About.class));
				Util.setTransition(context, Util.TransitionStyle.DEEPER);
				return true;
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
	}
	
	private void displayTwitterAlertIfNeeded() {
		int NB_LAUNCHES = 8;
		String nbLaunches = Util.getPref(this, Util.PrefKeys.Twitter_NbLaunches);
		if (nbLaunches.equals(""))
			Util.setPref(this, Util.PrefKeys.Twitter_NbLaunches, "1");
		else if (!nbLaunches.equals("ok")) {
			int n = Integer.parseInt(nbLaunches);
			if (n == NB_LAUNCHES) {
				// Display message
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Be the first to try beta versions of the app, and learn cool news like upcoming updates and known issues!")
				.setTitle("Follow Munin for Android on Twitter")
				.setCancelable(true)
				// Yes
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						try {
						   startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=muninforandroid")));
						} catch (Exception e) {
						   startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/#!/muninforandroid")));
						}
					}
				})
				// No
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
				Util.setPref(this, Util.PrefKeys.Twitter_NbLaunches, "ok");
			} else
				Util.setPref(this, Util.PrefKeys.Twitter_NbLaunches, String.valueOf(n+1));
		}
	}

	private void displayOpenSourceAlertIfNeeded() {
		if (!Util.getPref(this, Util.PrefKeys.OpenSourceDialogShown).equals("true")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.alert_opensource)
					.setCancelable(true)
					.setPositiveButton(R.string.alert_opensource_action, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/chteuchteu/Munin-for-Android")));
						}
					})
					.setNegativeButton(R.string.close, null);
			builder.create().show();

			Util.setPref(this, Util.PrefKeys.OpenSourceDialogShown, "true");
		}
	}

	private void displayI18nAlertIfNeeded() {
		// Only display the alertDialog if the device language is not fr/en/de/ru
		String deviceLanguage = Locale.getDefault().getLanguage();

		if (!I18nHelper.isLanguageSupported(deviceLanguage)) {
			// Don't display OpenSource & I18n dialogs at the same time
			if (Util.getPref(this, Util.PrefKeys.OpenSourceDialogShown).equals("true")
					&& !Util.getPref(this, Util.PrefKeys.I18NDialogShown).equals("true")) {
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
												startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.munin-for-android.com/i18n.php")));
											}
										})
										.setNegativeButton(R.string.close, null);
								builder.create().show();
							}
						})
						.setNegativeButton(R.string.no, null);
				builder.create().show();

				Util.setPref(this, Util.PrefKeys.I18NDialogShown, "true");
			}
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!BuildConfig.DEBUG)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!BuildConfig.DEBUG)
			EasyTracker.getInstance(this).activityStop(this);
	}
	
	private void preload() {
		boolean updateOperations = !Util.getPref(context, Util.PrefKeys.LastMFAVersion).equals(MuninFoo.VERSION + "");
		
		
		if (updateOperations) {
			if (myProgressDialog == null || !myProgressDialog.isShowing())
				myProgressDialog = ProgressDialog.show(context, "", getString(R.string.text39), true);
			// Please wait while the app does some update operations
			new UpdateOperations().execute();
		} else
			onLoadFinished();
	}
	
	private void updateActions() {
		if (Util.getPref(context, Util.PrefKeys.Lang).equals(""))
			Util.setPref(context, Util.PrefKeys.Lang, Locale.getDefault().getLanguage());
		
		if (Util.getPref(context, Util.PrefKeys.GraphviewOrientation).equals(""))
			Util.setPref(context, Util.PrefKeys.GraphviewOrientation, "auto");
		
		if (Util.getPref(context, Util.PrefKeys.DefaultScale).equals(""))
			Util.setPref(context, Util.PrefKeys.DefaultScale, "day");
		
		if (Util.hasPref(context, Util.PrefKeys.Drawer))
			Util.removePref(context, Util.PrefKeys.Drawer);
		
		if (Util.hasPref(context, Util.PrefKeys.Splash))
			Util.removePref(context, Util.PrefKeys.Splash);
		
		if (Util.hasPref(context, Util.PrefKeys.ListViewMode))
			Util.removePref(context, Util.PrefKeys.ListViewMode);
		
		if (Util.hasPref(context, Util.PrefKeys.Transitions))
			Util.removePref(context, Util.PrefKeys.Transitions);

		if (!Util.hasPref(context, Util.PrefKeys.GraphsZoom))
			Util.setPref(context, Util.PrefKeys.GraphsZoom, "true");
		
		// MfA 3.0 : moved auth attributes from MuninServer to MuninMaster : migrate those if possible
		String strFromVersion = Util.getPref(context, Util.PrefKeys.LastMFAVersion);
		double fromVersion = 0;
		if (!strFromVersion.equals(""))
			fromVersion = Double.parseDouble(Util.getPref(context, Util.PrefKeys.LastMFAVersion));

		if (fromVersion < 4.2) // 4.2 = V3.0
			muninFoo.sqlite.migrateTo3();

		// UserAgentChanged catchup
		if (Util.getPref(context, Util.PrefKeys.UserAgentChanged).equals("")) {
			String currentUserAgent = muninFoo.getUserAgent();
			// MuninForAndroid/3.0 (Android 4.4.4 KITKAT)
			boolean userAgentChanged = (currentUserAgent.contains("MuninForAndroid/")
					&& currentUserAgent.contains(" (Android "));

			Util.setPref(context, Util.PrefKeys.UserAgentChanged, String.valueOf(userAgentChanged));
		}

		// Update UserAgent
		if (Util.getPref(context, Util.PrefKeys.UserAgentChanged).equals("false")) {
			String newUserAgent = MuninFoo.generateUserAgent(this);
			muninFoo.setUserAgent(newUserAgent);
			Util.setPref(this, Util.PrefKeys.UserAgent, newUserAgent);
		}

		// Munin for Android 3.5 : added MuninServer.hdGraphUrl
		// Migrate information if there are
		for (MuninServer server : muninFoo.getServers()) {
			if (server.getParent().isDynazoomAvailable() == MuninMaster.DynazoomAvailability.TRUE) {
				server.setHdGraphURL(server.getGraphURL());
				muninFoo.sqlite.dbHlpr.updateMuninServer(server);
			}
		}

		Util.setPref(context, Util.PrefKeys.LastMFAVersion, MuninFoo.VERSION + "");
		muninFoo.resetInstance(this);
	}

	/* Grid fragment */
	@Override public void updatePeriodMenuItem(MuninPlugin.Period newPeriod) { }
	@Override public void onPreviewHide() { }
	@Override public void onEditModeChange(boolean editing) { }
	@Override public void onPreview() { }
	@Override
	public void onGridLoaded(Grid grid) {
		toolbar.setSubtitle(grid.getName());
	}

	/* Label fragment */
	@Override public void onLabelClick(Label label) { } // Not used here
	@Override public void onLabelItemClick(int pos, String labelName, long labelId) {
		Intent intent = new Intent(context, Activity_GraphView.class);
		intent.putExtra("position", pos);
		intent.putExtra("from", "main_labels");
		intent.putExtra("label", labelName);
		intent.putExtra("labelId", labelId);
		startActivity(intent);
		Util.setTransition(context, Util.TransitionStyle.DEEPER);
	}
	@Override public void onLabelsItemsListFragmentLoaded() { }

	private class UpdateOperations extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			updateActions();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// When rotating the device while updating : may crash
			if (myProgressDialog != null && myProgressDialog.isShowing()) {
				try {
					myProgressDialog.dismiss();
				} catch (Exception ex) { ex.printStackTrace(); }
			}
			
			onLoadFinished();
		}
	}

	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		materialMenu.syncState(savedInstanceState);
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		materialMenu.onSaveInstanceState(outState);
	}
}