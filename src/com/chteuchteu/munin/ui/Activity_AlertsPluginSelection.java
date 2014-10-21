package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
	private Context		c;
	
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
		dh.setDrawerActivity(DrawerHelper.Activity_AlertsPluginSelection);
		
		Util.UI.applySwag(this);
		
		for (MuninPlugin plugin : muninFoo.currentServer.getPlugins()) {
			Log.v("", "Plugin state : " + plugin.getState().name());
			if (plugin.getState() == AlertState.WARNING || plugin.getState() == AlertState.CRITICAL) {
				LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View v = vi.inflate(R.layout.plugins_list_dark, null);
				
				LinearLayout part = (LinearLayout)v.findViewById(R.id.pluginselection_part_ll);
				TextView line_a = (TextView)v.findViewById(R.id.line_a);
				TextView line_b = (TextView)v.findViewById(R.id.line_b);
				
				line_a.setText(plugin.getFancyName());
				line_b.setText(plugin.getName());
				
				if (plugin.getState() == AlertState.WARNING)
					part.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_WARNING));
				else if (plugin.getState() == AlertState.CRITICAL)
					part.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_CRITICAL));
				
				final int indexOfPlugin = muninFoo.currentServer.getPlugins().indexOf(plugin);
				
				part.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						String pluginName = ((TextView)v.findViewById(R.id.line_b)).getText().toString();
						
						Intent i = new Intent(Activity_AlertsPluginSelection.this, Activity_GraphView.class);
						i.putExtra("plugin", pluginName);
						Log.v("", "Setting position " + indexOfPlugin);
						i.putExtra("position", indexOfPlugin);
						i.putExtra("server", muninFoo.currentServer.getServerUrl());
						i.putExtra("from", "alerts");
						startActivity(i);
						Util.setTransition(c, TransitionStyle.DEEPER);
					}
				});
				
				ViewGroup insertPoint = (ViewGroup) findViewById(R.id.alerts_pluginselection_inserthere);
				insertPoint.addView(v);
			}
		}
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