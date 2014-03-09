package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;

import com.chteuchteu.munin.ExpandableListViewAdapter;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninServer;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_Servers extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private Context			c;
	
	Map<String, List<String>> serversCollection;
	ExpandableListView		expListView;
	public static Button 	addServer;
	private Menu 			menu;
	private String			activityName;
	
	public static boolean	menu_firstLoad = true;
	
	@SuppressLint("NewApi")
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		c = this;
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setContentView(R.layout.servers);
			findViewById(R.id.viewTitle).setVisibility(View.GONE);
			findViewById(R.id.viewTitleSep).setVisibility(View.GONE);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getString(R.string.serversTitle));
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_Servers);
			}
		} else {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.getWindow().getDecorView().setBackgroundColor(Color.WHITE);
			super.setTheme(R.style.ListFont);
			setContentView(R.layout.servers);
		}
		
		addServer = (Button)findViewById(R.id.servers_btn_add_a_server);
		addServer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View actualView) {
				Intent intent = new Intent(Activity_Servers.this, Activity_AddServer.class);
				intent.putExtra("contextServerUrl", "");
				startActivity(intent);
				Util.setTransition(c, TransitionStyle.DEEPER);
			}
		});
		
		Intent i = getIntent();
		MuninMaster fromServersEdit = null;
		if (i.getExtras() != null && i.getExtras().containsKey("fromMaster"))
			fromServersEdit = muninFoo.getMasterById((int) i.getExtras().getLong("fromMaster"));
		
		expListView = (ExpandableListView) findViewById(R.id.servers_list);
		
		List<String> masters = muninFoo.getMastersNames();
		// Create collection
		serversCollection = new LinkedHashMap<String, List<String>>();
		
		for (MuninMaster m : muninFoo.masters) {
			List<String> childList = new ArrayList<String>();
			for (MuninServer s : m.getOrderedChildren())
				childList.add(s.getName());
			serversCollection.put(m.getName(), childList);
		}
		final ExpandableListViewAdapter expListAdapter = new ExpandableListViewAdapter(this, masters, serversCollection, muninFoo);
		expListView.setAdapter(expListAdapter);
		
		if (fromServersEdit != null)
			expListView.expandGroup(muninFoo.getMasterPosition(fromServersEdit));
		
		expListView.setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				//final String selected = (String) expListAdapter.getChild(groupPosition, childPosition);
				MuninServer s = muninFoo.masters.get(groupPosition).getServerFromFlatPosition(childPosition);
				Intent intent = new Intent(Activity_Servers.this, Activity_AddServer.class);
				intent.putExtra("contextServerUrl", s.getServerUrl());
				intent.putExtra("action", "edit");
				startActivity(intent);
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			}
		});
		
		if (muninFoo.getHowManyServers() == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			((LinearLayout)findViewById(R.id.servers_noserver)).setVisibility(View.VISIBLE);
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
		getMenuInflater().inflate(R.menu.servers, menu);
		addServer.setVisibility(View.GONE);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		Intent intent;
		switch (item.getItemId()) {
			case android.R.id.home:
				if (muninFoo.drawer)
					dh.getDrawer().toggle(true);
				else {
					intent = new Intent(this, Activity_Main.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					Util.setTransition(c, TransitionStyle.SHALLOWER);
				}
				return true;
			case R.id.menu_add:
				intent = new Intent(this, Activity_AddServer.class);
				intent.putExtra("contextServerUrl", "");
				startActivity(intent);
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Servers.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Servers.this, Activity_About.class));
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