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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninPlugin.AlertState;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


@SuppressLint("InflateParams")
public class Activity_AlertsPluginSelection extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private String			activityName;
	private Menu			menu;
	private Context			c;
	
	private static List<MuninPlugin> plugins;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		c = this;
		
		setContentView(R.layout.alerts_pluginselection);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(muninFoo.currentServer.getName());
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(dh.Activity_AlertsPluginSelection);
		
		Util.UI.applySwag(this);
		
		plugins = new ArrayList<MuninPlugin>();
		if (muninFoo.currentServer != null && muninFoo.currentServer.getPlugins() != null && muninFoo.currentServer.getPlugins().size() > 0) {
			for (int i=0; i<muninFoo.currentServer.getPlugins().size(); i++) {
				if (muninFoo.currentServer.getPlugin(i) != null && 
						(muninFoo.currentServer.getPlugin(i).getState() == AlertState.WARNING || muninFoo.currentServer.getPlugin(i).getState() == AlertState.CRITICAL)) {
					plugins.add(muninFoo.currentServer.getPlugin(i));
					
					LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View v = vi.inflate(R.layout.pluginselection_list_dark, null);
					
					((TextView)v.findViewById(R.id.line_a)).setText(muninFoo.currentServer.getPlugin(i).getFancyName());
					((TextView)v.findViewById(R.id.line_b)).setText(muninFoo.currentServer.getPlugin(i).getName());
					if (muninFoo.currentServer.getPlugin(i).getState() == AlertState.WARNING)
						((LinearLayout)v.findViewById(R.id.pluginselection_part_ll)).setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_WARNING));
					else if (muninFoo.currentServer.getPlugin(i).getState() == AlertState.CRITICAL)
						((LinearLayout)v.findViewById(R.id.pluginselection_part_ll)).setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_CRITICAL));
					
					v.findViewById(R.id.pluginselection_part_ll).setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							Intent i = new Intent(Activity_AlertsPluginSelection.this, Activity_GraphView.class);
							i.putExtra("plugin", ((TextView)v.findViewById(R.id.line_b)).getText().toString());
							// Get plugin index in list
							for (int y=0; y<muninFoo.currentServer.getPlugins().size(); y++) {
								if (muninFoo.currentServer.getPlugin(y) != null && muninFoo.currentServer.getPlugin(y).getName().equals(((TextView)v.findViewById(R.id.line_b)).getText().toString())) {
									i.putExtra("position", y + ""); break;
								}
							}
							i.putExtra("server", muninFoo.currentServer.getServerUrl());
							i.putExtra("from", "alerts");
							startActivity(i);
							Util.setTransition(c, TransitionStyle.DEEPER);
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
				dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_AlertsPluginSelection.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_AlertsPluginSelection.this, Activity_About.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			default:	return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		
		dh.getDrawer().setOnOpenListener(new OnOpenListener() {
			@Override
			public void onOpen() {
				activityName = getActionBar().getTitle().toString();
				getActionBar().setTitle(R.string.app_name);
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
		getMenuInflater().inflate(R.menu.alertspluginselection, menu);
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Alerts.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("dontCheckAgain", true);
		startActivity(intent);
		Util.setTransition(c, TransitionStyle.SHALLOWER);
	}
}