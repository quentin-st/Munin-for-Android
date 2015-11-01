package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.GridsListAlertDialog;
import com.chteuchteu.munin.hlpr.ChromecastHelper;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.MuninPlugin.Period;

import java.util.List;

public class Activity_Grid extends MuninActivity implements IGridActivity {
	public static final String ARG_GRIDID = "gridId";
	private MenuItem menu_refresh;
	private MenuItem menu_edit;
	private MenuItem menu_period;
	private MenuItem menu_open;
	private Period currentPeriod;

	private boolean chromecastEnabled;

	/**
	 * Temporary grid object used activity instantiation. Should not be used
	 *  for main interactions with grid (ie. grid.currentlyOpenedGridItem)
	 *  since Activity.tmpGrid is not the same instance as Fragment.grid
	 */
	private Grid tmpGrid;
	private List<Grid> grids;
	private Handler mHandler;
	private Runnable mHandlerTask;

	private GridsListAlertDialog gridsListAlertDialog;
	private Fragment_Grid fragment;

	private Drawable toolbar_originalButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_grid);
		super.onContentViewSet();

		if (settings.getBool(Settings.PrefKeys.ScreenAlwaysOn))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		this.currentPeriod = Util.getDefaultPeriod(this);

		// Init fragment
		this.fragment = new Fragment_Grid();
		// Pass the gridId
		Bundle bundle = new Bundle();
		long gridId = getIntent().getExtras().getLong(ARG_GRIDID);
		bundle.putLong(Fragment_Grid.ARG_GRIDID, gridId);
		this.fragment.setArguments(bundle);
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, this.fragment).commit();

		this.grids = muninFoo.sqlite.dbHlpr.getGrids(muninFoo);
		// tmpGrid: temporary grid object used to instantiate activity.
		// Should not be used afterwards (use Fragment_Grid.grid instead)
		tmpGrid = getGrid(grids, gridId);

		if (!Util.isOnline(context))
			Toast.makeText(context, getString(R.string.text30), Toast.LENGTH_LONG).show();


		// Toolbar spinner
		actionBar.setDisplayShowTitleEnabled(false);
		final View customActionBarView = findViewById(R.id.actionbar_dropdown);
		final TextView customActionBarView_textView = (TextView) customActionBarView.findViewById(R.id.text);
		customActionBarView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (gridsListAlertDialog == null)
					gridsListAlertDialog = new GridsListAlertDialog(context, customActionBarView, grids,
							new GridsListAlertDialog.GridsListAlertDialogClick() {
								@Override
								public void onItemClick(Grid grid) {
									if (tmpGrid != grid) {
										customActionBarView_textView.setText(grid.getName());

										tmpGrid = grid;

										// If editing / previewing: cancel those
										if (fragment.isPreviewing())
											fragment.hidePreview();

										if (fragment.isEditing())
											fragment.edit();

										fragment = new Fragment_Grid();
										Bundle bundle = new Bundle();
										bundle.putLong(Fragment_Grid.ARG_GRIDID, tmpGrid.getId());
										bundle.putString(Fragment_Grid.ARG_PERIOD, currentPeriod.name());
										fragment.setArguments(bundle);
										getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();

										if (ChromecastHelper.isConnected(muninFoo.chromecastHelper))
											muninFoo.chromecastHelper.sendMessage_inflateGrid(tmpGrid, currentPeriod);
									}
								}
							});

				gridsListAlertDialog.show();
			}
		});
		customActionBarView_textView.setText(tmpGrid.getName());


		// Launch periodical check
		if (settings.getBool(Settings.PrefKeys.AutoRefresh)) {
			mHandler = new Handler();
			final int INTERVAL = 1000 * 60 * 5;
			mHandlerTask = new Runnable() {
				@Override
				public void run() {
					if (!fragment.isUpdating())
						fragment.autoRefresh();
					mHandler.postDelayed(mHandlerTask, INTERVAL);
				}
			};
			mHandlerTask.run();
		}

		// Chromecast
		chromecastEnabled = muninFoo.premium && !settings.getBool(Settings.PrefKeys.DisableChromecast);

		if (chromecastEnabled) {
			// If null: not connected yet
			if (muninFoo.chromecastHelper == null) {
				muninFoo.chromecastHelper = ChromecastHelper.create(this);

				muninFoo.chromecastHelper.onCreate(new Runnable() {
					@Override
					public void run() {
						if (fragment == null || fragment.getGrid() == null)
							return;

						muninFoo.chromecastHelper.sendMessage_inflateGrid(fragment.getGrid(), currentPeriod);
					}
				});

			} else if (muninFoo.chromecastHelper.isConnected())
				muninFoo.chromecastHelper.sendMessage_inflateGrid(tmpGrid, currentPeriod);
		}
	}

	private static Grid getGrid(List<Grid> grids, long gridId) {
		for (Grid grid : grids) {
			if (grid.getId() == gridId)
				return grid;
		}
		return null;
	}

	/**
	 * Return the fragment grid. Must be used only when we are sure
	 * that the fragment has been initialized!
	 */
	public Grid getGrid() {
		return fragment.getGrid();
	}

	@Override
	public void onPreview() {
		menu_open.setVisible(true);
		menu_period.setVisible(false);
		menu_refresh.setVisible(false);
		menu_edit.setVisible(false);

		if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
			muninFoo.chromecastHelper.sendMessage_preview(fragment.getGrid().currentlyOpenedGridItem);
	}

	@Override
	public void onPreviewHide() {
		if (menu_refresh != null)	menu_refresh.setVisible(true);
		if (menu_edit != null)		menu_edit.setVisible(true);
		if (menu_period != null)	menu_period.setVisible(true);
		if (menu_open != null)		menu_open.setVisible(false);

		if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
			muninFoo.chromecastHelper.sendMessage(ChromecastHelper.SimpleChromecastAction.CANCEL_PREVIEW);
	}

	@Override
	public void onEditModeChange(boolean editing) {
		if (menu_refresh != null)	menu_refresh.setVisible(editing);
		if (menu_period != null)	menu_period.setVisible(editing);
		if (menu_edit != null)
			menu_edit.setIcon(
					editing ? R.drawable.ic_action_image_edit
							: R.drawable.ic_action_navigation_check);

		// Toolbar: back button
		if (this.toolbar_originalButton == null)
			this.toolbar_originalButton = toolbar.getNavigationIcon();

		if (editing) {
			toolbar.setNavigationIcon(this.toolbar_originalButton);
			toolbar.setNavigationOnClickListener(drawerHelper.getToggleListener());
		} else {
			toolbar.setNavigationIcon(ContextCompat.getDrawable(context, R.drawable.ic_action_navigation_arrow_back));
			toolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onBackPressed();
				}
			});
		}
	}

	@Override
	public void onGridLoaded(Grid grid) { }

	@Override
	public void onManualLoad() { /* autoLoad=false only used in Activity_Main */ }

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.grid, menu);
		menu_refresh = menu.findItem(R.id.menu_refresh);
		menu_edit = menu.findItem(R.id.menu_edit);
		menu_period = menu.findItem(R.id.menu_period);
		menu_open = menu.findItem(R.id.menu_open);
		menu_refresh.setVisible(!fragment.isEditing());
		if (fragment.isEditing())
			menu_edit.setIcon(R.drawable.ic_action_navigation_check);
		else
			menu_edit.setIcon(R.drawable.ic_action_image_edit);
		menu_period.setTitle(currentPeriod.getLabel(this));

		if (chromecastEnabled)
			muninFoo.chromecastHelper.createOptionsMenu(menu);
	}

	private void openGraph() {
		if (fragment.getGrid().currentlyOpenedGridItem == null)
			return;

		fragment.getGrid().f.setCurrentNode(fragment.getGrid().currentlyOpenedGridItem.getPlugin().getInstalledOn());
		Intent i = new Intent(context, Activity_GraphView.class);
		i.putExtra("plugin", fragment.getGrid().currentlyOpenedGridItem.getPlugin().getName());
		i.putExtra("from", "grid");
		Intent gridIntent = ((Activity) context).getIntent();
		if (gridIntent != null && gridIntent.getExtras() != null && gridIntent.getExtras().containsKey("gridName"))
			i.putExtra("fromGrid", gridIntent.getExtras().getString("gridName"));
		context.startActivity(i);
		Util.setTransition(this, TransitionStyle.DEEPER);
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Grids; }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_refresh:
				fragment.refresh();

				if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
					muninFoo.chromecastHelper.sendMessage(ChromecastHelper.SimpleChromecastAction.REFRESH);
				return true;
			case R.id.menu_edit: fragment.edit(); return true;
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
		}

		return true;
	}

	@Override
	public void onGridSaved() {
		if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
			muninFoo.chromecastHelper.sendMessage_inflateGrid(fragment.getGrid(), currentPeriod);
	}

	private void onPeriodMenuItemChange(Period newPeriod) {
		this.currentPeriod = newPeriod;
		fragment.setCurrentPeriod(newPeriod);
		menu_period.setTitle(newPeriod.getLabel(context));
		fragment.refresh();
		if (chromecastEnabled && ChromecastHelper.isConnected(muninFoo.chromecastHelper))
			muninFoo.chromecastHelper.sendMessage_changePeriod(newPeriod);
	}
	
	@Override
	public void onBackPressed() {
		if (fragment.isPreviewing())
			fragment.hidePreview();
		else if (fragment.isEditing())
			fragment.edit(); // quit edit mode
		else {
			Intent intent = new Intent(this, Activity_Grids.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(this, TransitionStyle.SHALLOWER);
		}
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
