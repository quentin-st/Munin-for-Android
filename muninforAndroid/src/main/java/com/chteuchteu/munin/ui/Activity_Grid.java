package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.MuninPlugin.Period;

import java.util.ArrayList;
import java.util.List;

public class Activity_Grid extends MuninActivity implements IActivity_Grid {
	public MenuItem menu_refresh;
	public MenuItem menu_edit;
	public MenuItem menu_period;
	public MenuItem menu_open;

	private List<Grid> grids;
	private Grid grid;
	private Handler mHandler;
	private Runnable mHandlerTask;

	private Fragment_Grid fragment;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_grid);
		super.onContentViewSet();
		dh.setDrawerActivity(this);

		if (Util.getPref(this, Util.PrefKeys.ScreenAlwaysOn).equals("true"))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Init fragment
		this.fragment = new Fragment_Grid();
		// Pass the gridId
		Bundle bundle = new Bundle();
		long gridId = getIntent().getExtras().getLong("gridId");
		bundle.putLong(Fragment_Grid.ARG_GRIDID, gridId);
		this.fragment.setArguments(bundle);
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, this.fragment).commit();

		this.grids = muninFoo.sqlite.dbHlpr.getGrids(muninFoo);
		this.grid = getGrid(grids, gridId);
		actionBar.setTitle(getText(R.string.text75) + " " + grid.getName());

		if (!Util.isOnline(context))
			Toast.makeText(context, getString(R.string.text30), Toast.LENGTH_LONG).show();

		
		// ActionBar spinner if needed
		if (grids.size() > 1) {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setDisplayShowTitleEnabled(false);

			SpinnerAdapter spinnerAdapter = new ArrayAdapter<>(getApplicationContext(),
					android.R.layout.simple_spinner_dropdown_item, getGridsNames(grids));
			
			ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					if (itemPosition != grids.indexOf(grid)) {
						long gridId = grids.get(itemPosition).getId();
						grid = getGrid(grids, gridId);

						// If editing / previewing: cancel those
						if (fragment.isPreviewing())
							fragment.hidePreview();

						if (fragment.isEditing())
							fragment.edit();

						fragment = new Fragment_Grid();
						Bundle bundle = new Bundle();
						bundle.putLong(Fragment_Grid.ARG_GRIDID, grids.get(itemPosition).getId());
						fragment.setArguments(bundle);
						getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
					}
					return false;
				}
			};
			actionBar.setListNavigationCallbacks(spinnerAdapter, navigationListener);
			if (grids.indexOf(grid) != -1)
				actionBar.setSelectedNavigationItem(grids.indexOf(grid));
		}

		// Launch periodical check
		if (Util.getPref(this, Util.PrefKeys.AutoRefresh).equals("true")) {
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
	}

	private static Grid getGrid(List<Grid> grids, long gridId) {
		for (Grid grid : grids) {
			if (grid.getId() == gridId)
				return grid;
		}
		return null;
	}

	private static List<String> getGridsNames(List<Grid> grids) {
		List<String> list = new ArrayList<>();
		for (Grid grid : grids)
			list.add(grid.getName());
		return list;
	}

	@Override
	public void updatePeriodMenuItem(Period period) {
		if (menu_period != null)
			menu_period.setTitle(period.getLabel(this));
	}

	@Override
	public void onPreview() {
		menu_open.setVisible(true);
		menu_period.setVisible(false);
		menu_refresh.setVisible(false);
		menu_edit.setVisible(false);
	}

	@Override
	public void onPreviewHide() {
		if (menu_refresh != null)	menu_refresh.setVisible(true);
		if (menu_edit != null)		menu_edit.setVisible(true);
		if (menu_period != null)	menu_period.setVisible(true);
		if (menu_open != null)		menu_open.setVisible(false);
	}

	@Override
	public void onEditModeChange(boolean editing) {
		if (menu_refresh != null)	menu_refresh.setVisible(editing);
		if (menu_period != null)	menu_period.setVisible(editing);
		if (menu_edit != null)
			menu_edit.setIcon(
					editing ? R.drawable.ic_action_image_edit
							: R.drawable.ic_action_navigation_check);
	}

	@Override
	public void onGridLoaded(Grid grid) {}

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
		menu_period.setTitle(fragment.getCurrentPeriod().getLabel(this));
	}

	private void openGraph() {
		if (grid.currentlyOpenedGridItem == null)
			return;

		grid.f.setCurrentServer(grid.currentlyOpenedGridItem.getPlugin().getInstalledOn());
		Intent i = new Intent(context, Activity_GraphView.class);
		i.putExtra("plugin", grid.currentlyOpenedGridItem.getPlugin().getName());
		i.putExtra("from", "grid");
		Intent gridIntent = ((Activity) context).getIntent();
		if (gridIntent != null && gridIntent.getExtras() != null && gridIntent.getExtras().containsKey("gridName"))
			i.putExtra("fromGrid", gridIntent.getExtras().getString("gridName"));
		context.startActivity(i);
		Util.setTransition(context, TransitionStyle.DEEPER);
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Grid; }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_refresh: fragment.refresh(); return true;
			case R.id.menu_edit: fragment.edit(); return true;
			case R.id.period_day:
				fragment.setCurrentPeriod(Period.DAY);
				menu_period.setTitle(Period.DAY.getLabel(context));
				fragment.refresh();
				return true;
			case R.id.period_week:
				fragment.setCurrentPeriod(Period.WEEK);
				menu_period.setTitle(Period.WEEK.getLabel(context));
				fragment.refresh();
				return true;
			case R.id.period_month:
				fragment.setCurrentPeriod(Period.MONTH);
				menu_period.setTitle(Period.MONTH.getLabel(context));
				fragment.refresh();
				return true;
			case R.id.period_year:
				fragment.setCurrentPeriod(Period.YEAR);
				menu_period.setTitle(Period.YEAR.getLabel(context));
				fragment.refresh();
				return true;
			case R.id.menu_open: openGraph(); return true;
		}

		return true;
	}
	
	@Override
	public void onBackPressed() {
		if (findViewById(R.id.fullscreen).getVisibility() == View.VISIBLE)
			fragment.hidePreview();
		else {
			if (fragment.isEditing())
				fragment.edit(); // quit edit mode
			else {
				Intent intent = new Intent(this, Activity_Grids.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				Util.setTransition(context, TransitionStyle.SHALLOWER);
			}
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		if (Util.getPref(this, Util.PrefKeys.ScreenAlwaysOn).equals("true"))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
