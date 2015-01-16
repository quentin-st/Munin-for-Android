package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.GridDownloadHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.MuninPlugin.Period;

import java.util.List;

public class Activity_Grid extends MuninActivity {
	public static boolean	editing;
	private Grid			grid;
	private LinearLayout	container;
	private ImageView		fs_iv;
	
	public static MenuItem menu_refresh;
	public static MenuItem menu_edit;
	public static MenuItem menu_period;
	public static MenuItem menu_open;
	
	private Period currentPeriod;
	
	public static boolean	updating;
	private Handler		mHandler;
	private Runnable		mHandlerTask;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_grid);
		super.onContentViewSet();
		dh.setDrawerActivity(this);

		editing = false;
		updating = false;
		
		if (Util.getPref(this, Util.PrefKeys.ScreenAlwaysOn).equals("true"))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		fs_iv = (ImageView) findViewById(R.id.fullscreen_iv);
		
		container = (LinearLayout) findViewById(R.id.grid_root_container);
		currentPeriod = Util.getDefaultPeriod(this);
		if (menu_period != null)
			menu_period.setTitle(currentPeriod.getLabel(this));
		
		setupGrid();
		
		
		// ActionBar spinner if needed
		final List<String> gridsNames = muninFoo.sqlite.dbHlpr.getGridsNames();
		if (gridsNames.size() > 1) {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setDisplayShowTitleEnabled(false);
			
			// Get current index
			int index = -1;
			int i=0;
			for (String gridName : gridsNames) {
				if (gridName.equals(grid.name))
					index = i;
				i++;
			}
			final int currentSelectedIndex = index;
			
			SpinnerAdapter spinnerAdapter = new ArrayAdapter<>(getApplicationContext(),
					android.R.layout.simple_spinner_dropdown_item, gridsNames);
			
			ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					if (itemPosition != currentSelectedIndex) {
						Intent intent = new Intent(Activity_Grid.this, Activity_Grid.class);
						intent.putExtra("gridName", gridsNames.get(itemPosition));
						startActivity(intent);
						// Animation : RTL / LTR
						if (itemPosition > currentSelectedIndex)
							Util.setTransition(context, TransitionStyle.DEEPER);
						else
							Util.setTransition(context, TransitionStyle.SHALLOWER);
					}
					return false;
				}
			};
			actionBar.setListNavigationCallbacks(spinnerAdapter, navigationListener);
			if (currentSelectedIndex != -1)
				actionBar.setSelectedNavigationItem(currentSelectedIndex);
		}
		
		// On rotate : onRetainNonConfigurationInstance has been called.
		// Let's get back some values (grid period for example)
		Activity_Grid beforeOnRotate = (Activity_Grid) getLastNonConfigurationInstance();
		if (beforeOnRotate != null)
			this.currentPeriod = beforeOnRotate.currentPeriod;
		
		
		grid.dHelper = new GridDownloadHelper(grid);
		grid.dHelper.init(3, currentPeriod);
		grid.dHelper.start(false);
		
		
		findViewById(R.id.add_line_bottom).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				grid.addLine(context, true);
			}
		});
		findViewById(R.id.add_column_right).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				grid.addColumn(context, true);
			}
		});
		findViewById(R.id.fullscreen).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hidePreview();
			}
		});
		
		Fonts.setFont(this, (TextView) findViewById(R.id.fullscreen_tv), CustomFont.Roboto_Regular);
		
		if (grid.items.size() == 0)
			edit();
		
		// Launch periodical check
		if (Util.getPref(this, Util.PrefKeys.AutoRefresh).equals("true")) {
			mHandler = new Handler();
			final int INTERVAL = 1000 * 60 * 5;
			mHandlerTask = new Runnable() {
				@Override 
				public void run() {
					if (!updating)
						autoRefresh();
					mHandler.postDelayed(mHandlerTask, INTERVAL);
				}
			};
			mHandlerTask.run();
		}
	}
	
	/*/**
	 * Retain period on rotate
	 * @return
	 *//*
	public  Object onRetainNonConfigurationInstance() {
		return this;
	}*/
	
	private void hidePreview() {
		grid.currentlyOpenedPlugin = null;
		if (menu_refresh != null)	menu_refresh.setVisible(true);
		if (menu_edit != null)		menu_edit.setVisible(true);
		if (menu_period != null)	menu_period.setVisible(true);
		if (menu_open != null)		menu_open.setVisible(false);

		Util.Animations.animate(findViewById(R.id.fullscreen), Util.Animations.CustomAnimation.FADE_OUT,
				Util.Animations.AnimationSpeed.MEDIUM, new Runnable() {
					@Override
					public void run() {
						findViewById(R.id.fullscreen).setVisibility(View.GONE);
						fs_iv.setImageBitmap(null);
						((TextView) ((Activity) context).findViewById(R.id.fullscreen_tv)).setText("");
					}
				});
	}
	
	private void setupGrid() {
		if (!Util.isOnline(this))
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
		
		container.removeAllViews();
		grid = null;
		
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("gridName")) {
			String gridName = thisIntent.getExtras().getString("gridName");
			grid = muninFoo.sqlite.dbHlpr.getGrid(this, muninFoo, gridName);
		}
		
		if (grid == null)
			startActivity(new Intent(this, Activity_Grids.class));
		
		actionBar.setTitle(getText(R.string.text75) + " " + grid.name);
		
		grid.setupLayout();
		container.addView(grid.buildLayout(this));
		grid.updateLayoutSizes(this);
	}
	
	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.grid, menu);
		menu_refresh = menu.findItem(R.id.menu_refresh);
		menu_edit = menu.findItem(R.id.menu_edit);
		menu_period = menu.findItem(R.id.menu_period);
		menu_open = menu.findItem(R.id.menu_open);
		menu_refresh.setVisible(!editing);
		if (editing)	menu_edit.setIcon(R.drawable.ic_action_navigation_check);
		else 			menu_edit.setIcon(R.drawable.ic_action_image_edit);
		menu_period.setTitle(currentPeriod.getLabel(this));
	}
	
	private void edit() {
		if (menu_refresh != null)	menu_refresh.setVisible(editing);
		if (menu_period != null)	menu_period.setVisible(editing);
		
		if (editing) { // Cancel edit
			grid.cancelEdit(this);
			if (menu_edit != null) menu_edit.setIcon(R.drawable.ic_action_image_edit);
			grid.toggleFootersVisibility(true);
			muninFoo.sqlite.dbHlpr.saveGridItemsRelations(grid);
		} else { // Edit
			grid.edit(this);
			if (menu_edit != null) menu_edit.setIcon(R.drawable.ic_action_navigation_check);
			grid.toggleFootersVisibility(false);
		}
		
		editing = !editing;
	}
	
	private void refresh() {
		// Update each GridItem with the currentPeriod
		for (GridItem item : grid.items)
			item.setPeriod(this.currentPeriod);
		
		if (!Util.isOnline(this))
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
		grid.dHelper.period = this.currentPeriod;
		grid.dHelper.start(true);
	}
	
	private void autoRefresh() {
		grid.dHelper.period = this.currentPeriod;
		grid.dHelper.start(true);
	}
	
	private void openGraph() {
		if (grid.currentlyOpenedPlugin == null)
			return;
		grid.f.setCurrentServer(grid.currentlyOpenedPlugin.getInstalledOn());
		Intent i = new Intent(context, Activity_GraphView.class);
		i.putExtra("plugin", grid.currentlyOpenedPlugin.getName());
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
			case R.id.menu_refresh: refresh(); return true;
			case R.id.menu_edit: edit(); return true;
			case R.id.period_day:
				this.currentPeriod = Period.DAY;
				menu_period.setTitle(currentPeriod.getLabel(context));
				refresh();
				return true;
			case R.id.period_week:
				this.currentPeriod = Period.WEEK;
				menu_period.setTitle(currentPeriod.getLabel(context));
				refresh();
				return true;
			case R.id.period_month:
				this.currentPeriod = Period.MONTH;
				menu_period.setTitle(currentPeriod.getLabel(context));
				refresh();
				return true;
			case R.id.period_year:
				this.currentPeriod = Period.YEAR;
				menu_period.setTitle(currentPeriod.getLabel(context));
				refresh();
				return true;
			case R.id.menu_open: openGraph(); return true;
		}

		return true;
	}
	
	@Override
	public void onBackPressed() {
		if (findViewById(R.id.fullscreen).getVisibility() == View.VISIBLE)
			hidePreview();
		else {
			if (editing)
				edit(); // quit edit mode
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