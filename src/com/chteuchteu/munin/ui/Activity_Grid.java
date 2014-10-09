package com.chteuchteu.munin.ui;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.GridDownloadHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
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
	private Context			context;
	
	public static boolean	editing;
	private Grid			grid;
	private LinearLayout	container;
	private ImageView		fs_iv;
	
	public static MenuItem	menu_refresh;
	public static MenuItem	menu_edit;
	private MenuItem		menu_delete;
	public static MenuItem	menu_period;
	public static MenuItem	menu_open;
	
	private Period			currentPeriod;
	
	public static boolean	updating;
	private Handler			mHandler;
	private Runnable		mHandlerTask;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.grid);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Crashlytics.start(this);
		context = this;
		
		editing = false;
		updating = false;
		
		Util.UI.applySwag(this);
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(dh.Activity_Grid);
		
		if (Util.getPref(this, "screenAlwaysOn").equals("true"))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		fs_iv = (ImageView) findViewById(R.id.fullscreen_iv);
		
		container = (LinearLayout) findViewById(R.id.grid_root_container);
		currentPeriod = Util.getDefaultPeriod(this);
		if (menu_period != null)
			menu_period.setTitle(currentPeriod.getLabel(this));
		
		setupGrid();
		
		
		// ActionBar spinner if needed
		ActionBar actionBar = getActionBar();
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
			
			SpinnerAdapter spinnerAdapter = new ArrayAdapter<String>(getActionBar().getThemedContext(),
					android.R.layout.simple_spinner_dropdown_item, gridsNames);
			
			ActionBar.OnNavigationListener navigationListener = new OnNavigationListener() {
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
		
		Fonts.setFont(this, (TextView) findViewById(R.id.fullscreen_tv), CustomFont.RobotoCondensed_Regular);
		
		if (grid.items.size() == 0)
			edit();
		
		// Launch periodical check
		if (Util.getPref(this, "autoRefresh").equals("true")) {
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
				((TextView) ((Activity) context).findViewById(R.id.fullscreen_tv)).setText("");
			}
		});
		findViewById(R.id.fullscreen).startAnimation(a);
	}
	
	@SuppressLint("NewApi")
	private void setupGrid() {
		if (!Util.isOnline(this))
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
		
		container.removeAllViews();
		grid = null;
		
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("gridName")) {
			String gridName = thisIntent.getExtras().getString("gridName");
			grid = muninFoo.sqlite.dbHlpr.getGrid(this, muninFoo, true, gridName);
		}
		
		if (grid == null)
			startActivity(new Intent(this, Activity_GridSelection.class));
		
		getActionBar().setTitle(getText(R.string.text75) + " " + grid.name);
		
		grid.setupLayout(this);
		container.addView(grid.buildLayout(this));
		grid.updateLayoutSizes(this);
	}
	
	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		
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
		menu_period.setTitle(currentPeriod.getLabel(this));
	}
	
	private void edit() {
		if (menu_refresh != null)	menu_refresh.setVisible(editing);
		if (menu_delete != null)	menu_delete.setVisible(!editing);
		if (menu_period != null)	menu_period.setVisible(editing);
		
		if (editing) { // Cancel edit
			grid.cancelEdit(this);
			if (menu_edit != null) menu_edit.setIcon(R.drawable.content_edit_dark);
			muninFoo.sqlite.dbHlpr.saveGridItemsRelations(grid);
		} else { // Edit
			grid.edit(this);
			if (menu_edit != null) menu_edit.setIcon(R.drawable.navigation_accept_dark);
		}
		
		editing = !editing;
	}
	
	private void delete() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.delete)
		.setMessage(R.string.text80)
		.setPositiveButton(R.string.text33, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				muninFoo.sqlite.dbHlpr.deleteGrid(grid);
				startActivity(new Intent(Activity_Grid.this, Activity_GridSelection.class));
				Util.setTransition(context, TransitionStyle.SHALLOWER);
			}
		})
		.setNegativeButton(R.string.text34, null)
		.show();
	}
	
	private void refresh() {
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
		grid.f.currentServer = grid.currentlyOpenedPlugin.getInstalledOn();
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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				dh.getDrawer().toggle(true);
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
			case R.id.menu_open:
				openGraph();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Grid.this, Activity_Settings.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Grid.this, Activity_About.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
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
				Util.setTransition(context, TransitionStyle.SHALLOWER);
			}
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStop(this);
		
		if (Util.getPref(this, "screenAlwaysOn").equals("true"))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}