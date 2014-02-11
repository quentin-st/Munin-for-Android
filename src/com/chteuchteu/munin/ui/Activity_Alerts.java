package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.obj.MuninPlugin.AlertState;
import com.chteuchteu.munin.obj.MuninServer;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_Alerts extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	
	private boolean		hideNormalStateServers;
	private int 			nb_loadings;
	private ProgressBar 	loading;
	private Menu 			menu;
	private String			activityName;
	
	private List<MuninServer> servers;
	
	private LinearLayout[]	part_part;
	private TextView[] 		part_serverName;
	private LinearLayout[] 	part_criticals;
	private TextView[] 		part_criticalsNumber;
	private TextView[] 		part_criticalsLabel;
	private TextView[]		part_criticalsPluginsList;
	private LinearLayout[] 	part_warnings;
	private TextView[] 		part_warningsNumber;
	private TextView[] 		part_warningsLabel;
	private TextView[]		part_warningsPluginsList;
	
	public static String	BG_COLOR_UNDEFINED = "#B2B2B2";
	public static String	BG_COLOR_OK = "#8EC842";
	public static String	BG_COLOR_WARNING = "#FFAE5B";
	public static String	BG_COLOR_CRITICAL = "#FF7B68";
	
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		setContentView(R.layout.alerts);
		Crashlytics.start(this);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getString(R.string.alertsTitle));
			
			((TextView)findViewById(R.id.viewTitle)).setVisibility(View.GONE);
			((LinearLayout)findViewById(R.id.viewTitleSep)).setVisibility(View.GONE);
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_Alerts);
			}
		} else
			this.getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.grayBackground));
		
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
		loading = (ProgressBar) findViewById(R.id.loading_spin);
		loading.setIndeterminate(true);
		
		servers = new ArrayList<MuninServer>();
		// Populating servers list
		for (int i=0; i<muninFoo.getOrderedServers().size(); i++) {
			if (muninFoo.getOrderedServers().get(i) != null)
				servers.add(muninFoo.getOrderedServers().get(i));
		}
		
		int i = 0;
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
					overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
				}
			});
			
			View insertPoint = findViewById(R.id.alerts_root_container);
			((ViewGroup) insertPoint).addView(v);
			
			i++;
		}
		
		updateStates();
		
		((TextView) findViewById(R.id.hideNoAlerts)).setOnClickListener(new OnClickListener() {
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
	
	public void updateStates() {
		nb_loadings = 0;
		loading.setVisibility(View.VISIBLE);
		for (int i = 0; i < muninFoo.getHowManyServers(); i++) {
			part_criticals[i].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
			part_warnings[i].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
			updateState(i);
		}
	}
	
	public void updateState(int i) {
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
						loading.setVisibility(View.INVISIBLE);
					
					updateView(hideNormalStateServers);
				}
			};
			new Thread() {
				@Override public void run() {
					// Traitement en arrière plan
					muninFoo.getServer(z).fetchPluginsStates();
					
					uiThreadCallback.post(runInUIThread);
				}
			}.start();
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
					setTransition("shallower");
				}
				return true;
			case R.id.menu_refresh:
				updateStates();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Alerts.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Alerts.this, Activity_About.class));
				setTransition("deeper");
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
		setTransition("shallower");
	}
	
	public void enableArrow(boolean b, int p) {
		if (p >= 0 && p < part_serverName.length) {
			if (b) {
				part_serverName[p].setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.arrow, 0);
			} else {
				part_serverName[p].setCompoundDrawables(null, null, null, null);
			}
		}
	}
	
	public String getPref(String key) {
		return this.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	public void setTransition(String level) {
		if (getPref("transitions").equals("true")) {
			if (level.equals("deeper"))
				overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
			else if (level.equals("shallower"))
				overridePendingTransition(R.anim.shallower_in, R.anim.shallower_out);
		}
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