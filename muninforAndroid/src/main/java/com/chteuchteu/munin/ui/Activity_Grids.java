package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_Grids;
import com.chteuchteu.munin.hlpr.ChromecastHelper;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.ImportExportHelper;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.MuninPlugin.Period;

import java.util.List;

public class Activity_Grids extends MuninActivity implements IGridActivity, IImportExportActivity {
	public static final String ARG_GRIDID = "gridId";
	private MenuItem menu_refresh;
	private MenuItem menu_edit;
	private MenuItem menu_period;
	private MenuItem menu_open;
	private MenuItem menu_add;
	private MenuItem menu_rename;
	private MenuItem menu_delete;
	private MenuItem menu_importExport;
	private Period currentPeriod;

	private LockableViewPager viewPager;
	private TabLayout tabLayout;
	private Adapter_Grids adapter;

	private List<Grid> grids;
	private Grid currentGrid;

	private boolean chromecastEnabled;

	private Handler mHandler;
	private Runnable mHandlerTask;

	private Drawable toolbar_originalIcon;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_grid);
		super.onContentViewSet();
		this.actionBar.setTitle(R.string.button_grid);

		if (settings.getBool(Settings.PrefKeys.ScreenAlwaysOn))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		this.chromecastEnabled = muninFoo.premium && !settings.getBool(Settings.PrefKeys.DisableChromecast);
		this.currentPeriod = Util.getDefaultPeriod(this);
		this.grids = muninFoo.sqlite.dbHlpr.getGrids(muninFoo);

		if (grids.size() == 0)
			addDefaultGrid();

		long currentGridId = grids.get(0).getId();
		if (getIntent() != null)
			currentGridId = getIntent().getIntExtra(ARG_GRIDID, (int) currentGridId);

		// Get currentGrid
		for (Grid grid : grids) {
			if (grid.getId() == currentGridId)
				currentGrid = grid;
		}

		// Init ViewPager
		viewPager = (LockableViewPager) findViewById(R.id.viewPager);
		adapter = new Adapter_Grids(getSupportFragmentManager(), grids);
		viewPager.setAdapter(adapter);
		tabLayout = (TabLayout) findViewById(R.id.tabLayout);
		tabLayout.setupWithViewPager(viewPager);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
		tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				viewPager.setCurrentItem(tab.getPosition());
				chromecast_switchTo();
			}

			@Override public void onTabUnselected(TabLayout.Tab tab) { }
			@Override public void onTabReselected(TabLayout.Tab tab) { }
		});
		TabLayout.Tab tab = tabLayout.getTabAt(grids.indexOf(currentGrid));
		if (tab != null) // Just to ignore Android Studio warning
			tab.select();

		if (!Util.isOnline(context))
			Toast.makeText(context, getString(R.string.text30), Toast.LENGTH_LONG).show();

		// Launch periodical check
		if (settings.getBool(Settings.PrefKeys.AutoRefresh)) {
			mHandler = new Handler();
			final int INTERVAL = 1000 * 60 * 5;
			mHandlerTask = new Runnable() {
				@Override 
				public void run() {
					for (Fragment_Grid fragment : adapter.getAll()) {
						if (!fragment.isUpdating())
							fragment.autoRefresh();
					}
					mHandler.postDelayed(mHandlerTask, INTERVAL);
				}
			};
			mHandlerTask.run();
		}

		if (chromecastEnabled) {
			// If null: not connected yet
			if (muninFoo.chromecastHelper == null) {
				muninFoo.chromecastHelper = ChromecastHelper.create(this);

				muninFoo.chromecastHelper.onCreate(new Runnable() {
					@Override
					public void run() {
						if (currentGrid == null)
							return;

						muninFoo.chromecastHelper.sendMessage_inflateGrid(currentGrid, currentPeriod);
					}
				});
			} else if (muninFoo.chromecastHelper.isConnected()) {
				muninFoo.chromecastHelper.sendMessage_inflateGrid(currentGrid, currentPeriod);
			}
		}
	}

	private void addDefaultGrid() {
		String defaultGridName = getString(R.string.default_grid);
		Grid grid = new Grid(defaultGridName);
		long id = muninFoo.sqlite.dbHlpr.insertGrid(getString(R.string.default_grid));
		grid.setId(id);
		grids.add(grid);
	}

	private void chromecast_switchTo() {
		if (!chromecastEnabled)
			return;

		if (!muninFoo.chromecastHelper.isConnected())
			return;

		muninFoo.chromecastHelper.sendMessage_inflateGrid(currentGrid, currentPeriod);
	}

	/**
	 * Return the fragment grid. Must be used only when we are sure
	 * that the fragment has been initialized!
	 */
	public Grid getGrid() {
		return this.currentGrid;
	}

	@Override
	public void onPreview() {
		menu_open.setVisible(true);
		menu_period.setVisible(false);
		menu_refresh.setVisible(false);
		menu_edit.setVisible(false);
		menu_add.setVisible(false);
		menu_importExport.setVisible(false);

		if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
			muninFoo.chromecastHelper.sendMessage_preview(this.currentGrid.currentlyOpenedGridItem);
	}

	@Override
	public void onPreviewHide() {
		menu_open.setVisible(false);
		menu_period.setVisible(true);
		menu_refresh.setVisible(true);
		menu_edit.setVisible(true);
		menu_add.setVisible(false);
		menu_importExport.setVisible(true);

		if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
			muninFoo.chromecastHelper.sendMessage(ChromecastHelper.SimpleChromecastAction.CANCEL_PREVIEW);
	}

	@Override
	public void onEditModeChange(boolean editing) {
		menu_refresh.setVisible(editing);
		menu_period.setVisible(editing);
		menu_add.setVisible(editing);
		menu_importExport.setVisible(editing);
		menu_edit.setVisible(editing);
		menu_rename.setVisible(!editing);
		menu_delete.setVisible(!editing);
		menu_edit.setIcon(
				editing ? R.drawable.ic_action_image_edit
						: R.drawable.ic_action_navigation_check);

		setPagingEnabled(editing);

		// Toolbar: save button
		if (this.toolbar_originalIcon == null)
			this.toolbar_originalIcon = toolbar.getNavigationIcon();

		if (editing) {
			toolbar.setNavigationIcon(this.toolbar_originalIcon);
			toolbar.setNavigationOnClickListener(drawerHelper.getToggleListener());
		} else {
			toolbar.setNavigationIcon(ContextCompat.getDrawable(context, R.drawable.ic_action_navigation_check));
			toolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Fragment_Grid currentFragment = getCurrentFragment();
					if (currentFragment != null)
						currentFragment.edit();
				}
			});
		}
	}

	@Override
	public void onGridLoaded(Grid grid) { }

	@Override
	public void onManualLoad() { /* autoLoad=false only used in Activity_Main */ }

	/**
	 * Lock or unlock paging
	 * @param enabled boolean
	 */
	private void setPagingEnabled(boolean enabled) {
		this.viewPager.setPagingEnabled(enabled);

		if (enabled)
			this.tabLayout.setAlpha(1.0f);
		else
			this.tabLayout.setAlpha(0.5f);

		// Enable/disable each children click
		LinearLayout tabStrip = ((LinearLayout) tabLayout.getChildAt(0));
		for (int i=0; i<tabStrip.getChildCount(); i++) {
			if (enabled)
				tabStrip.getChildAt(i).setOnTouchListener(null);
			else {
				tabStrip.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						return true;
					}
				});
			}
		}
	}

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.grid, menu);
		menu_refresh = menu.findItem(R.id.menu_refresh);
		menu_edit = menu.findItem(R.id.menu_edit);
		menu_period = menu.findItem(R.id.menu_period);
		menu_open = menu.findItem(R.id.menu_open);
		menu_add = menu.findItem(R.id.menu_add);
		menu_rename = menu.findItem(R.id.menu_rename);
		menu_delete = menu.findItem(R.id.menu_delete);
		menu_importExport = menu.findItem(R.id.menu_importexport);

		menu_edit.setIcon(R.drawable.ic_action_image_edit);
		menu_period.setTitle(currentPeriod.getLabel(this));

		if (chromecastEnabled)
			muninFoo.chromecastHelper.createOptionsMenu(menu);

		// If editing:
		Fragment_Grid fragment = getCurrentFragment();
		if (fragment != null && fragment.isEditing()) {
			menu_refresh.setVisible(false);
			menu_period.setVisible(false);
			menu_add.setVisible(false);
			menu_delete.setVisible(true);
			menu_importExport.setVisible(false);
			menu_edit.setIcon(R.drawable.ic_action_navigation_check);
		}
	}

	private void openGraph() {
		if (this.currentGrid == null || this.currentGrid.currentlyOpenedGridItem == null)
			return;

		muninFoo.setCurrentNode(this.currentGrid.currentlyOpenedGridItem.getPlugin().getInstalledOn());
		Intent i = new Intent(context, Activity_GraphView.class);
		i.putExtra("plugin", this.currentGrid.currentlyOpenedGridItem.getPlugin().getName());
		i.putExtra("from", "grid");
		Intent gridIntent = ((Activity) context).getIntent();
		if (gridIntent != null && gridIntent.getExtras() != null && gridIntent.getExtras().containsKey("gridName"))
			i.putExtra("fromGrid", gridIntent.getExtras().getString("gridName"));
		context.startActivity(i);
		Util.setTransition(this, TransitionStyle.DEEPER);
	}

	private Fragment_Grid getCurrentFragment() {
		return (Fragment_Grid) adapter.getItem(viewPager.getCurrentItem());
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Grids; }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_refresh: {
				Fragment_Grid currentFragment = getCurrentFragment();
				if (currentFragment == null)
					return false;

				currentFragment.refresh();

				if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
					muninFoo.chromecastHelper.sendMessage(ChromecastHelper.SimpleChromecastAction.REFRESH);
				return true;
			}
			case R.id.menu_edit: {
				Fragment_Grid currentFragment = getCurrentFragment();
				if (currentFragment == null)
					return false;

				currentFragment.edit();
				return true;
			}
			case R.id.period_day:
				onPeriodMenuItemChange(Period.DAY);
				return true;
			case R.id.period_week:
				onPeriodMenuItemChange(Period.WEEK);
				return true;
			case R.id.period_month:
				onPeriodMenuItemChange(Period.MONTH);
				return true;
			case R.id.period_year:
				onPeriodMenuItemChange(Period.YEAR);
				return true;
			case R.id.menu_open: openGraph(); return true;
			case R.id.menu_add: {
				final LinearLayout linearLayout = new LinearLayout(this);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setPadding(10, 30, 10, 10);
				final EditText input = new EditText(this);
				linearLayout.addView(input);

				new AlertDialog.Builder(Activity_Grids.this)
						.setTitle(getText(R.string.text69))
						.setView(linearLayout)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String value = input.getText().toString();
								if (!value.equals("")) {
									List<String> existingNames = muninFoo.sqlite.dbHlpr.getGridsNames();
									boolean available = true;
									for (String s : existingNames) {
										if (s.equals(value))
											available = false;
									}
									if (available) {
										long id = muninFoo.sqlite.dbHlpr.insertGrid(value);
										dialog.dismiss();

										// Restart activity... We should probably handle this
										Intent intent = new Intent(Activity_Grids.this, Activity_Grids.class);
										intent.putExtra(ARG_GRIDID, id);
										startActivity(intent);
										Util.setTransition(activity, TransitionStyle.DEEPER);
									} else
										Toast.makeText(context, getString(R.string.text74), Toast.LENGTH_LONG).show();
								}
							}
						})
						.setNegativeButton(getText(R.string.text64), null)
						.create()
						.show();
				return true;
			}
			case R.id.menu_rename: {
				if (currentGrid == null)
					return false;

				final EditText input = new EditText(context);
				input.setText(currentGrid.getName());

				new AlertDialog.Builder(context)
						.setTitle(R.string.rename_grid)
						.setView(input)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String value = input.getText().toString();
								if (!value.equals(currentGrid.getName())) {
									// Check if there's a grid with this name
									boolean alreadyExists = muninFoo.sqlite.dbHlpr.gridExists(value);
									if (!alreadyExists) {
										muninFoo.sqlite.dbHlpr.updateGridName(currentGrid.getName(), value);

										// Restart activity... We should probably handle this
										Util.setTransition(activity, TransitionStyle.DEEPER);
										startActivity(new Intent(Activity_Grids.this, Activity_Grids.class));
									} else
										Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
								}
								dialog.dismiss();
							}
						})
						.setNegativeButton(R.string.text64, null)
						.show();
				return true;
			}
			case R.id.menu_delete:
				new AlertDialog.Builder(context)
						.setTitle(R.string.delete)
						.setMessage(R.string.text80)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (currentGrid == null)
									return;

								muninFoo.sqlite.dbHlpr.deleteGrid(currentGrid);

								// Restart activity... We should probably handle this
								Util.setTransition(activity, TransitionStyle.DEEPER);
								startActivity(new Intent(Activity_Grids.this, Activity_Grids.class));
							}
						})
						.setNegativeButton(R.string.no, null)
						.show();
				return true;
			case R.id.menu_import:
				ImportExportHelper.showImportDialog(muninFoo, context, ImportExportHelper.ImportExportType.GRIDS, this);
				return true;
			case R.id.menu_export:
				ImportExportHelper.showExportDialog(muninFoo, context, ImportExportHelper.ImportExportType.GRIDS, this);
				return true;
			default:
				return false;
		}
	}

	@Override
	public void onGridSaved(Grid grid) {
		if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
			muninFoo.chromecastHelper.sendMessage_inflateGrid(grid, currentPeriod);
	}

	private void onPeriodMenuItemChange(Period newPeriod) {
		this.currentPeriod = newPeriod;

		// Notify all fragments
		for (Fragment_Grid fragment : adapter.getAll()) {
			fragment.setCurrentPeriod(newPeriod);
			fragment.refresh();
		}

		menu_period.setTitle(newPeriod.getLabel(context));
		if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
			muninFoo.chromecastHelper.sendMessage_changePeriod(newPeriod);
	}
	
	@Override
	public void onBackPressed() {
        if (drawerHelper.closeDrawerIfOpen())
            return;

		Fragment_Grid currentFragment = getCurrentFragment();

        if (findViewById(R.id.fullscreen).getVisibility() == View.VISIBLE) {
			if (currentFragment == null)
				return;

			currentFragment.hidePreview();
		}
		else {
			if (currentFragment != null && currentFragment.isEditing())
				currentFragment.edit(); // quit edit mode
			else {
				Intent intent = new Intent(this, Activity_Grids.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				Util.setTransition(this, TransitionStyle.SHALLOWER);
			}
		}
	}

	@Override
	public void onExportSuccess(String pswd) {
		final View dialogView = View.inflate(context, R.layout.dialog_export_success, null);
		TextView code = (TextView) dialogView.findViewById(R.id.export_succes_code);
		Util.Fonts.setFont(context, code, Util.Fonts.CustomFont.RobotoCondensed_Bold);
		code.setText(pswd);

		new AlertDialog.Builder(context)
				.setTitle(R.string.export_success_title)
				.setView(dialogView)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, null)
				.show();
	}

	@Override
	public void onExportError() {
		Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onImportSuccess() {
		new AlertDialog.Builder(context)
				.setTitle(R.string.import_success_title)
				.setMessage(R.string.import_success)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Restart activity... We should probably handle this
						Util.setTransition(activity, TransitionStyle.DEEPER);
						startActivity(new Intent(Activity_Grids.this, Activity_Grids.class));
					}
				})
				.show();
	}

	@Override
	public void onImportError() {
		Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (chromecastEnabled)
			muninFoo.chromecastHelper.onResume();
	}

	@Override
	protected void onPause() {
		if (chromecastEnabled)
			muninFoo.chromecastHelper.onPause();
		super.onPause();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (chromecastEnabled)
			muninFoo.chromecastHelper.onStart();
	}

	@Override
	public void onStop() {
		if (chromecastEnabled)
			muninFoo.chromecastHelper.onStop();
		super.onStop();

		if (settings.getBool(Settings.PrefKeys.ScreenAlwaysOn))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
