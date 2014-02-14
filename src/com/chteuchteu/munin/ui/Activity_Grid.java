package com.chteuchteu.munin.ui;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.GridDownloadHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
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
	private ImageView		fs_iv;
	
	public static MenuItem	menu_refresh;
	public static MenuItem	menu_edit;
	private MenuItem		menu_delete;
	public static MenuItem	menu_period;
	public static MenuItem	menu_open;
	public static ImageButton comp_delete;
	public static ImageButton comp_refresh;
	public static ImageButton comp_edit;
	
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
		} else {
			this.getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.grayBackground));
			findViewById(R.id.comp_actions).setVisibility(View.VISIBLE);
			comp_delete = (ImageButton) findViewById(R.id.btn_comp_delete);
			comp_delete.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { delete(); } });
			comp_refresh = (ImageButton) findViewById(R.id.btn_comp_refresh);
			comp_refresh.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { refresh(); } });
			comp_edit = (ImageButton) findViewById(R.id.btn_comp_edit);
			comp_edit.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { edit(); } });
		}
		
		fs_iv = (ImageView) findViewById(R.id.fullscreen_iv);
		
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
		
		Fonts.setFont(this, (TextView) findViewById(R.id.fullscreen_tv), CustomFont.RobotoCondensed_Regular);
		
		if (grid.items.size() == 0)
			edit();
	}
	
	private void hidePreview() {
		grid.currentlyOpenedPlugin = null;
		if (menu_refresh != null)	menu_refresh.setVisible(true);
		if (menu_edit != null)		menu_edit.setVisible(true);
		if (menu_period != null)	menu_period.setVisible(true);
		if (menu_open != null)		menu_open.setVisible(false);
		
		AlphaAnimation a = new AlphaAnimation(1.0f, 0.0f);
		a.setDuration(300);
		a.setAnimationListener(new AnimationListener() {
			@Override public void onAnimationStart(Animation animation) { }
			@Override public void onAnimationRepeat(Animation animation) { }
			@Override
			public void onAnimationEnd(Animation animation) {
				findViewById(R.id.fullscreen).setVisibility(View.GONE);
				fs_iv.setImageBitmap(null);
				((TextView) ((Activity) c).findViewById(R.id.fullscreen_tv)).setText("");
			}
		});
		findViewById(R.id.fullscreen).startAnimation(a);
	}
	
	@SuppressLint("NewApi")
	private void setupGrid() {
		if (!Util.isOnline(this))
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
		
		container.removeAllViews();
		
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("gridName")) {
			String gridName = thisIntent.getExtras().getString("gridName");
			grid = muninFoo.sqlite.dbHlpr.getGrid(this, muninFoo, true, gridName);
		}
		
		if (grid == null)
			startActivity(new Intent(this, Activity_GridSelection.class));
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			getActionBar().setTitle(getText(R.string.text75) + " " + grid.name);
		else
			((TextView) findViewById(R.id.viewTitle)).setText(getText(R.string.text75) + " " + grid.name);
		
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
		menu_open = menu.findItem(R.id.menu_open);
		menu_delete.setVisible(editing);
		menu_refresh.setVisible(!editing);
		if (editing)	menu_edit.setIcon(R.drawable.navigation_accept_dark);
		else 			menu_edit.setIcon(R.drawable.content_edit_dark);
	}
	
	private void edit() {
		if (menu_refresh != null)	menu_refresh.setVisible(editing);
		if (menu_delete != null)	menu_delete.setVisible(!editing);
		if (menu_period != null)	menu_period.setVisible(editing);
		
		if (editing) { // Cancel edit
			grid.cancelEdit(this);
			if (menu_edit != null) menu_edit.setIcon(R.drawable.content_edit_dark);
			if (comp_edit != null) comp_edit.setImageResource(R.drawable.ic_action_edit);
			muninFoo.sqlite.dbHlpr.saveGridItems(grid);
		} else { // Edit
			grid.edit(this);
			if (menu_edit != null) menu_edit.setIcon(R.drawable.navigation_accept_dark);
			if (comp_edit != null) comp_edit.setImageResource(R.drawable.navigation_accept);
		}
		
		editing = !editing;
	}
	
	private void delete() {
		muninFoo.sqlite.dbHlpr.deleteGrid(grid);
		startActivity(new Intent(Activity_Grid.this, Activity_GridSelection.class));
		setTransition("shallower");
	}
	
	private void refresh() {
		if (!Util.isOnline(this))
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
		grid.dHelper.period = this.currentPeriod;
		grid.dHelper.start(true);
	}
	
	private void openGraph() {
		grid.f.currentServer = grid.currentlyOpenedPlugin.getInstalledOn();
		Intent i = new Intent(c, Activity_GraphView.class);
		i.putExtra("plugin", grid.currentlyOpenedPlugin.getName());
		i.putExtra("from", "grid");
		Intent gridIntent = ((Activity) c).getIntent();
		if (gridIntent != null && gridIntent.getExtras() != null && gridIntent.getExtras().containsKey("gridName"))
			i.putExtra("fromGrid", gridIntent.getExtras().getString("gridName"));
		c.startActivity(i);
		Util.setTransition(c, "deeper");
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
			case R.id.menu_open:
				openGraph();
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