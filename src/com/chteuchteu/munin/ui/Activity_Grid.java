package com.chteuchteu.munin.ui;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.GridDownloadHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.CustomFont;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_Grid extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private String			activityName;
	private Menu			menu;
	private Context			c;
	
	public static boolean	editing;
	private Grid			grid;
	private LinearLayout	container;
	
	private MenuItem		menu_refresh;
	private MenuItem		menu_edit;
	private MenuItem		menu_delete;
	private MenuItem		menu_period;
	
	private Period			currentPeriod;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		setContentView(R.layout.grid);
		Crashlytics.start(this);
		c = this;
		
		editing = false;
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getString(R.string.button_grid));
			
			((TextView)findViewById(R.id.viewTitle)).setVisibility(View.GONE);
			((LinearLayout)findViewById(R.id.viewTitleSep)).setVisibility(View.GONE);
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_Grid);
			}
		} else
			this.getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.grayBackground));
		
		container = (LinearLayout) findViewById(R.id.grid_root_container);
		currentPeriod = Util.getDefaultPeriod(this);
		setupGrid();
		
		grid.dHelper = new GridDownloadHelper(grid);
		grid.dHelper.init(3, currentPeriod);
		grid.dHelper.start(false);
		
		
		findViewById(R.id.add_line_bottom).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				grid.addLine(c, true);
			}
		});
		findViewById(R.id.add_column_right).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				grid.addColumn(c, true);
			}
		});
		findViewById(R.id.fullscreen).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hidePreview();
			}
		});
		
		Util.setFont(this, (TextView) findViewById(R.id.fullscreen_tv), CustomFont.RobotoCondensed_Regular);
		Util.setFont(this, (TextView) findViewById(R.id.fullscreen_action1), CustomFont.RobotoCondensed_Regular);
		Util.setFont(this, (TextView) findViewById(R.id.fullscreen_action2), CustomFont.RobotoCondensed_Regular);
		
		if (grid.items.size() == 0)
			edit();
	}
	
	private void hidePreview() {
		AlphaAnimation a = new AlphaAnimation(1.0f, 0.0f);
		a.setDuration(300);
		a.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) { }
			@Override
			public void onAnimationRepeat(Animation animation) { }
			@Override
			public void onAnimationEnd(Animation animation) {
				findViewById(R.id.fullscreen).setVisibility(View.GONE);
				((ImageView) ((Activity) c).findViewById(R.id.fullscreen_iv)).setImageBitmap(null);
				((TextView) ((Activity) c).findViewById(R.id.fullscreen_tv)).setText("");
			}
		});
		findViewById(R.id.fullscreen).startAnimation(a);
	}
	
	@SuppressLint("NewApi")
	private void setupGrid() {
		container.removeAllViews();
		
		int position = 0;
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null) {
			if (thisIntent.getExtras().containsKey("position")) {
				position = Integer.parseInt(thisIntent.getExtras().getString("position"));
				List<Grid> grids = muninFoo.sqlite.dbHlpr.getGrids(this, muninFoo);
				if (grids.size() > position)
					grid = grids.get(position);
			} else if (thisIntent.getExtras().containsKey("gridName")) {
				String gridName = thisIntent.getExtras().getString("gridName");
				grid = muninFoo.sqlite.dbHlpr.getGrid(this, muninFoo, true, gridName);
			}
		}
		
		if (grid == null)
			startActivity(new Intent(this, Activity_GridSelection.class));
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			getActionBar().setTitle(getText(R.string.text75) + " " + grid.name);
		
		grid.setupLayout(this);
		container.addView(grid.buildLayout(this));
		grid.updateLayoutSizes(this);
	}
	
	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		if (muninFoo.drawer) {
			dh.getDrawer().setOnOpenListener(new OnOpenListener() {
				@Override
				public void onOpen() {
					activityName = getActionBar().getTitle().toString();
					getActionBar().setTitle("Munin for Android");
					menu.clear();
					getMenuInflater().inflate(R.menu.main, menu);
				}
			});
			dh.getDrawer().setOnCloseListener(new OnCloseListener() {
				@Override
				public void onClose() {
					getActionBar().setTitle(activityName);
					createOptionsMenu();
				}
			});
		}
		createOptionsMenu();
		return true;
	}
	
	private void createOptionsMenu() {
		menu.clear();
		getMenuInflater().inflate(R.menu.grid, menu);
		menu_refresh = menu.findItem(R.id.menu_refresh);
		menu_edit = menu.findItem(R.id.menu_edit);
		menu_delete = menu.findItem(R.id.menu_delete);
		menu_period = menu.findItem(R.id.menu_period);
		menu_delete.setVisible(editing);
		menu_refresh.setVisible(!editing);
		if (editing)	menu_edit.setIcon(R.drawable.navigation_accept_dark);
		else 			menu_edit.setIcon(R.drawable.content_edit);
	}
	
	private void edit() {
		if (menu_refresh != null) {
			menu_refresh.setVisible(editing);
			menu_delete.setVisible(!editing);
			menu_period.setVisible(editing);
		}
		
		if (editing) {
			grid.cancelEdit(this, this);
			if (menu_edit != null) menu_edit.setIcon(R.drawable.content_edit);
			muninFoo.sqlite.dbHlpr.saveGridItems(grid);
		} else {
			grid.edit(this);
			if (menu_edit != null) menu_edit.setIcon(R.drawable.navigation_accept_dark);
		}
		
		editing = !editing;
	}
	
	private void delete() {
		muninFoo.sqlite.dbHlpr.deleteGrid(grid);
		startActivity(new Intent(Activity_Grid.this, Activity_GridSelection.class));
		setTransition("shallower");
	}
	
	private void refresh() {
		grid.dHelper.period = this.currentPeriod;
		grid.dHelper.start(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				if (muninFoo.drawer)
					dh.getDrawer().toggle(true);
				else {
					Intent intent = new Intent(this, Activity_GridSelection.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					setTransition("shallower");
				}
				return true;
			case R.id.menu_refresh:
				refresh();
				return true;
			case R.id.menu_edit:
				edit();
				return true;
			case R.id.menu_delete:
				delete();
				return true;
			case R.id.period_day:
				this.currentPeriod = Period.DAY;
				refresh();
				return true;
			case R.id.period_week:
				this.currentPeriod = Period.WEEK;
				refresh();
				return true;
			case R.id.period_month:
				this.currentPeriod = Period.MONTH;
				refresh();
				return true;
			case R.id.period_year:
				this.currentPeriod = Period.YEAR;
				refresh();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Grid.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Grid.this, Activity_About.class));
				setTransition("deeper");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onBackPressed() {
		if (findViewById(R.id.fullscreen).getVisibility() == View.VISIBLE)
			hidePreview();
		else {
			if (editing)
				edit(); // quit edit mode
			else {
				Intent intent = new Intent(this, Activity_GridSelection.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				setTransition("shallower");
			}
		}
	}
	
	public void setTransition(String level) {
		if (getPref("transitions").equals("true")) {
			if (level.equals("deeper"))
				overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
			else if (level.equals("shallower"))
				overridePendingTransition(R.anim.shallower_in, R.anim.shallower_out);
		}
	}
	
	public String getPref(String key) {
		return this.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!muninFoo.debug)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!muninFoo.debug)
			EasyTracker.getInstance(this).activityStop(this);
	}
}