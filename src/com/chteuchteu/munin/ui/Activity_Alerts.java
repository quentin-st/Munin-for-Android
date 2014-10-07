package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin.AlertState;
import com.chteuchteu.munin.obj.MuninServer;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_Alerts extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private Context		c;
	
	private boolean		hideNormalStateServers;
	private int 			nb_loadings;
	private Menu 			menu;
	private MenuItem		menu_flatList;
	private String			activityName;
	/* If the menu items are flat / expanded */
	private boolean		listMode_flat;
	
	private List<MuninServer> servers;
	
	private LinearLayout[]		part_part;
	private TextView[] 		part_serverName;
	private LinearLayout[] 	part_criticals;
	private TextView[] 		part_criticalsNumber;
	private TextView[] 		part_criticalsLabel;
	private TextView[]			part_criticalsPluginsList;
	private LinearLayout[] 	part_warnings;
	private TextView[] 		part_warningsNumber;
	private TextView[] 		part_warningsLabel;
	private TextView[]			part_warningsPluginsList;
	
	private static final String	BG_COLOR_UNDEFINED = "#B2B2B2";
	private static final String	BG_COLOR_OK = "#8EC842";
	public static final String	BG_COLOR_WARNING = "#FFAE5B";
	public static final String	BG_COLOR_CRITICAL = "#FF7B68";
	private static final String	TEXT_COLOR = "#333333";
	
	private Handler			mHandler;
	private Runnable		mHandlerTask;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		setContentView(R.layout.alerts);
		c = this;
		Crashlytics.start(this);
		listMode_flat = false;
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(getString(R.string.alertsTitle));
		
		if (muninFoo.drawer) {
			dh = new DrawerHelper(this, muninFoo);
			dh.setDrawerActivity(dh.Activity_Alerts);
		}
		
		Util.UI.applySwag(this);
		
		if (Util.getPref(this, "screenAlwaysOn").equals("true"))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		int nbS = muninFoo.getHowManyServers();
		part_part 					= new LinearLayout[nbS];
		part_serverName 			= new TextView[nbS];
		part_criticals 				= new LinearLayout[nbS];
		part_criticalsNumber 		= new TextView[nbS];
		part_criticalsLabel 		= new TextView[nbS];
		part_criticalsPluginsList 	= new TextView[nbS];
		part_warnings 				= new LinearLayout[nbS];
		part_warningsNumber 		= new TextView[nbS];
		part_warningsLabel 			= new TextView[nbS];
		part_warningsPluginsList 	= new TextView[nbS];
		
		hideNormalStateServers = true;
		
		servers = new ArrayList<MuninServer>();
		// Populating servers list
		for (MuninMaster master : muninFoo.masters) {
			for (int i=0; i<master.getOrderedChildren().size(); i++) {
				if (master.getOrderedChildren().get(i) != null)
					servers.add(master.getOrderedChildren().get(i));
			}
		}
		
		int i = 0;
		LinearLayout wholeContainer = new LinearLayout(this);
		wholeContainer.setOrientation(LinearLayout.VERTICAL);
		
		for (final MuninServer server : servers) {
			LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = vi.inflate(R.layout.alerts_part, null);
			
			part_part[i] 					= (LinearLayout) v.findViewById(R.id.alerts_part);
			part_serverName[i] 				= (TextView) v.findViewById(R.id.alerts_part_serverName);
			part_criticals[i] 				= (LinearLayout) v.findViewById(R.id.alerts_part_criticals);
			part_criticalsNumber[i] 		= (TextView) v.findViewById(R.id.alerts_part_criticalsNumber);
			part_criticalsLabel[i] 			= (TextView) v.findViewById(R.id.alerts_part_criticalsLabel);
			part_criticalsPluginsList[i] 	= (TextView) v.findViewById(R.id.alerts_part_criticalsPluginsList);
			part_warnings[i] 				= (LinearLayout) v.findViewById(R.id.alerts_part_warnings);
			part_warningsNumber[i]			= (TextView) v.findViewById(R.id.alerts_part_warningsNumber);
			part_warningsLabel[i] 			= (TextView) v.findViewById(R.id.alerts_part_warningsLabel);
			part_warningsPluginsList[i] 	= (TextView) v.findViewById(R.id.alerts_part_warningsPluginsList);
			
			part_part[i].setVisibility(View.GONE);
			part_serverName[i].setText(server.getName());
			
			part_serverName[i].setOnClickListener(new OnClickListener() {
				public void onClick (View v) {
					muninFoo.currentServer = server;
					startActivity(new Intent(Activity_Alerts.this, Activity_AlertsPluginSelection.class));
					Util.setTransition(c, TransitionStyle.DEEPER);
				}
			});
			
			Util.Fonts.setFont(this, (ViewGroup) v, CustomFont.RobotoCondensed_Regular);
			
			wholeContainer.addView(v);
			
			i++;
		}
		
		View insertPoint = findViewById(R.id.alerts_root_container);
		((ViewGroup) insertPoint).addView(wholeContainer);
		
		// If coming from PluginSelection : don't check again
		Intent thisIntent = getIntent();
		if (thisIntent.hasExtra("dontCheckAgain") && thisIntent.getExtras().getBoolean("dontCheckAgain"))
			updateStates(false);
		else
			updateStates(true);
		
		
		findViewById(R.id.hideNoAlerts).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (hideNormalStateServers) {
					hideNormalStateServers = false;
					((TextView) findViewById(R.id.hideNoAlerts)).setText(getString(R.string.text49_1));
					updateView(false);
				} else {
					hideNormalStateServers = true;
					((TextView) findViewById(R.id.hideNoAlerts)).setText(getString(R.string.text49_2));
					updateView(true);
				}
			}
		});
		
		// Launch periodical check
		if (Util.getPref(this, "autoRefresh").equals("true")) {
			mHandler = new Handler();
			final int INTERVAL = 1000 * 60 * 5;
			mHandlerTask = new Runnable() {
				@Override 
				public void run() {
					updateStates(true);
					mHandler.postDelayed(mHandlerTask, INTERVAL);
				}
			};
			mHandlerTask.run();
		}
	}
	
	public void updateView(boolean hideNormal) {
		for (int i=0; i<part_part.length; i++) {
			boolean hide = false;
			if (hideNormal) {
				if (i<muninFoo.getHowManyServers() && muninFoo.getServer(i) != null) {
					int nbErrors = 0;
					int nbWarnings = 0;
					for (int y=0; y < muninFoo.getServer(i).getPlugins().size(); y++) {
						if (muninFoo.getServer(i).getPlugin(y) != null) {
							if (muninFoo.getServer(i).getPlugin(y).getState() == AlertState.CRITICAL)
								nbErrors++;
							else if (muninFoo.getServer(i).getPlugin(y).getState() == AlertState.WARNING)
								nbWarnings++;
						}
					}
					if (nbErrors == 0 && nbWarnings == 0) {
						hide = true;
						part_serverName[i].setClickable(false);
						enableArrow(false, i);
					} else {
						hide = false;
						part_serverName[i].setClickable(true);
						enableArrow(true, i);
					}
				}
			} else {
				int nbErrors = 0;
				int nbWarnings = 0;
				for (int y=0; y < muninFoo.getServer(i).getPlugins().size(); y++) {
					if (muninFoo.getServer(i).getPlugin(y) != null) {
						if (muninFoo.getServer(i).getPlugin(y).getState() == AlertState.CRITICAL)
							nbErrors++;
						else if (muninFoo.getServer(i).getPlugin(y).getState() == AlertState.WARNING)
							nbWarnings++;
					}
				}
				if (nbErrors == 0 && nbWarnings == 0) {
					part_serverName[i].setClickable(false);
					enableArrow(false, i);
				}
				else {
					part_serverName[i].setClickable(true);
					enableArrow(true, i);
				}
			}
			
			if (hide)
				part_part[i].setVisibility(View.GONE);
			else
				part_part[i].setVisibility(View.VISIBLE);
		}
	}
	
	/**
	 * Update UI
	 * @param fetch Use cached data or not
	 */
	public void updateStates(boolean fetch) {
		if (fetch && !Util.isOnline(this)) {
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
			return;
		}
		
		nb_loadings = 0;
		Util.UI.setLoading(true, this);
		for (int i = 0; i < muninFoo.getHowManyServers(); i++) {
			part_criticals[i].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
			part_warnings[i].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
			updateState(i, fetch);
		}
	}
	
	private void updateState(int i, final boolean fetch) {
		nb_loadings++;
		final int z = i;
		
		if (i >= 0 && i < muninFoo.getHowManyServers()) {
			final Handler uiThreadCallback = new Handler();
			final Runnable runInUIThread = new Runnable() {
				public void run() {
					int nbErrors = 0;
					int nbWarnings = 0;
					for (int i=0; i<muninFoo.getServer(z).getPlugins().size(); i++) {
						if (muninFoo.getServer(z).getPlugin(i) != null) {
							if (muninFoo.getServer(z).getPlugin(i).getState() == AlertState.CRITICAL)
								nbErrors++;
							else if (muninFoo.getServer(z).getPlugin(i).getState() == AlertState.WARNING)
								nbWarnings++;
						}
					}
					if (nbErrors > 0) {
						part_criticals[z].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_CRITICAL));
						// Liste plugins
						String toBeShown1 = "";
						for (int y=0; y<muninFoo.getServer(z).getErroredPlugins().size(); y++) {
							if (muninFoo.getServer(z).getErroredPlugins().get(y) != null) {
								if (y != muninFoo.getServer(z).getErroredPlugins().size() - 1)
									toBeShown1 = toBeShown1 + muninFoo.getServer(z).getErroredPlugins().get(y).getFancyName() + ", ";
								else
									toBeShown1 += muninFoo.getServer(z).getErroredPlugins().get(y).getFancyName();
							}
						}
						part_criticalsPluginsList[z].setText(toBeShown1);
					} else
						part_criticals[z].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_OK));
					
					if (nbWarnings > 0) {
						part_warnings[z].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_WARNING));
						// Liste plugins
						String toBeShown2 = "";
						for (int y=0; y<muninFoo.getServer(z).getWarnedPlugins().size(); y++) {
							if (muninFoo.getServer(z).getWarnedPlugins().get(y) != null) {
								if (y != muninFoo.getServer(z).getWarnedPlugins().size() - 1)
									toBeShown2 = toBeShown2 + muninFoo.getServer(z).getWarnedPlugins().get(y).getFancyName() + ", ";
								else
									toBeShown2 += muninFoo.getServer(z).getWarnedPlugins().get(y).getFancyName();
							}
						}
						part_warningsPluginsList[z].setText(toBeShown2);
					} else
						part_warnings[z].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_OK));
					
					part_criticalsNumber[z].setText(nbErrors + "");
					if (nbErrors == 1) // critical
						part_criticalsLabel[z].setText(getString(R.string.text50_1));
					else // criticals
						part_criticalsLabel[z].setText(getString(R.string.text50_2));
					
					part_warningsNumber[z].setText(nbWarnings + "");
					if (nbWarnings == 1) // warning
						part_warningsLabel[z].setText(getString(R.string.text51_1));
					else // warnings
						part_warningsLabel[z].setText(getString(R.string.text51_2));
					
					
					nb_loadings--;
					if (nb_loadings == 0)
						Util.UI.setLoading(false, Activity_Alerts.this);
					
					updateView(hideNormalStateServers);
					
					// Can't flat the list before the first loading is finished
					if (z == muninFoo.getHowManyServers()-1)
						menu_flatList.setVisible(true);
				}
			};
			new Thread() {
				@Override public void run() {
					if (fetch)
						muninFoo.getServer(z).fetchPluginsStates();
					
					uiThreadCallback.post(runInUIThread);
				}
			}.start();
		}
	}
	
	/**
	 * Switchs from flat to expanded list mode
	 */
	private void switchListMode() {
		if (this.listMode_flat) {
			// Expand
			for (LinearLayout ll : this.part_criticals)
				ll.setVisibility(View.VISIBLE);
			for (LinearLayout ll : this.part_warnings)
				ll.setVisibility(View.VISIBLE);
			
			for (int i=0; i<this.part_part.length; i++) {
				this.part_serverName[i].setTextColor(Color.parseColor(TEXT_COLOR));
				this.part_serverName[i].setBackgroundColor(Color.WHITE);
			}
			
			this.listMode_flat = false;
		} else {
			// Reduce
			for (LinearLayout ll : this.part_criticals)
				ll.setVisibility(View.GONE);
			for (LinearLayout ll : this.part_warnings)
				ll.setVisibility(View.GONE);
			
			// Set the background color so we can see the server state
			for (int i=0; i<this.part_part.length; i++) {
				int criticalsNumber = Integer.parseInt(this.part_criticalsNumber[i].getText().toString());
				int warningsNumber = Integer.parseInt(this.part_warningsNumber[i].getText().toString());
				if (criticalsNumber > 0 || warningsNumber > 0) {
					if (criticalsNumber > 0)
						this.part_serverName[i].setBackgroundColor(Color.parseColor(BG_COLOR_CRITICAL));
					else if (warningsNumber > 0)
						this.part_serverName[i].setBackgroundColor(Color.parseColor(BG_COLOR_WARNING));
					
					this.part_serverName[i].setTextColor(Color.WHITE);
				}
			}
			
			this.listMode_flat = true;
		}
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
		getMenuInflater().inflate(R.menu.alerts, menu);
		this.menu_flatList = menu.findItem(R.id.menu_flatlist);
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
					Intent intent = new Intent(this, Activity_Main.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					Util.setTransition(c, TransitionStyle.SHALLOWER);
				}
				return true;
			case R.id.menu_flatlist:
				switchListMode();
				return true;
			case R.id.menu_refresh:
				updateStates(true);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Alerts.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Alerts.this, Activity_About.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(c, TransitionStyle.SHALLOWER);
	}
	
	public void enableArrow(boolean b, int p) {
		if (p >= 0 && p < part_serverName.length) {
			if (b)
				part_serverName[p].setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.arrow, 0);
			else
				part_serverName[p].setCompoundDrawables(null, null, null, null);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!MuninFoo.debug)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!MuninFoo.debug)
			EasyTracker.getInstance(this).activityStop(this);
		
		if (Util.getPref(this, "screenAlwaysOn").equals("true"))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}