package com.chteuchteu.munin;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


@SuppressLint("NewApi")
public class Activity_AlertsPluginSelection extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private String			activityName;
	private Menu			menu;
	
	private static List<MuninPlugin> plugins;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setContentView(R.layout.alerts_pluginselection);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			findViewById(R.id.viewTitle).setVisibility(View.GONE);
			findViewById(R.id.viewTitleSep).setVisibility(View.GONE);
			actionBar.setTitle(muninFoo.currentServer.getName());
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_AlertsPluginSelection);
			}
		} else {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.alerts_pluginselection);
			super.setTheme(R.style.ListFont);
			((TextView) this.findViewById(R.id.viewTitle)).setText(muninFoo.currentServer.getName());
		}
		
		
		plugins = new ArrayList<MuninPlugin>();
		if (muninFoo.currentServer != null && muninFoo.currentServer.getPlugins() != null && muninFoo.currentServer.getPlugins().size() > 0) {
			// Construction de plugins - liste des plugins à afficher
			for (int i=0; i<muninFoo.currentServer.getNbPlugins(); i++) {
				if (muninFoo.currentServer.getPlugin(i) != null && 
						(muninFoo.currentServer.getPlugin(i).getState().equals(MuninPlugin.ALERTS_STATE_WARNING) || muninFoo.currentServer.getPlugin(i).getState().equals(MuninPlugin.ALERTS_STATE_CRITICAL))) {
					plugins.add(muninFoo.currentServer.getPlugin(i));
					// Construction de la vue
					LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View v = vi.inflate(R.layout.pluginselection_list_dark, null);
					
					((TextView)v.findViewById(R.id.line_a)).setText(muninFoo.currentServer.getPlugin(i).getFancyName());
					((TextView)v.findViewById(R.id.line_b)).setText(muninFoo.currentServer.getPlugin(i).getName());
					if (muninFoo.currentServer.getPlugin(i).getState().equals(MuninPlugin.ALERTS_STATE_WARNING))
						((LinearLayout)v.findViewById(R.id.pluginselection_part_ll)).setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_WARNING));
					else if (muninFoo.currentServer.getPlugin(i).getState().equals(MuninPlugin.ALERTS_STATE_CRITICAL))
						((LinearLayout)v.findViewById(R.id.pluginselection_part_ll)).setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_CRITICAL));
					
					((LinearLayout)v.findViewById(R.id.pluginselection_part_ll)).setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							Intent i = new Intent(Activity_AlertsPluginSelection.this, Activity_GraphView.class);
							i.putExtra("plugin", ((TextView)v.findViewById(R.id.line_b)).getText().toString());
							// Récupération de la position du plugin dans la liste
							for (int y=0; y<muninFoo.currentServer.getNbPlugins(); y++) {
								if (muninFoo.currentServer.getPlugin(y) != null && muninFoo.currentServer.getPlugin(y).getName().equals(((TextView)v.findViewById(R.id.line_b)).getText().toString())) {
									i.putExtra("position", y + ""); break;
								}
							}
							i.putExtra("server", muninFoo.currentServer.getServerUrl());
							i.putExtra("from", "alerts");
							startActivity(i);
							setTransition("deeper");
						}
					});
					
					View insertPoint = findViewById(R.id.alerts_pluginselection_inserthere);
					((ViewGroup) insertPoint).addView(v);
				}
			}
		} else
			startActivity(new Intent(Activity_AlertsPluginSelection.this, Activity_Alerts.class));
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
					startActivity(new Intent(this, Activity_Alerts.class));
					setTransition("shallower");
				}
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_AlertsPluginSelection.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_AlertsPluginSelection.this, Activity_About.class));
				setTransition("deeper");
				return true;
			default:	return super.onOptionsItemSelected(item);
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent(this, Activity_Alerts.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			setTransition("shallower");
			return false;
		}
		return super.onKeyDown(keyCode, event);
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
}