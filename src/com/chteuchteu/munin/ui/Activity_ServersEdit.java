package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.SimpleAdapter;

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
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleFloatViewManager;

public class Activity_ServersEdit extends ListActivity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private Context			c;
	
	private MuninMaster		m;
	private SimpleAdapter 	sa;
	ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	List<MuninServer> 		serversList;
	List<MuninServer>		deletedServers;
	private Menu 			menu;
	private String			activityName;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		c = this;
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setContentView(R.layout.servers_edit);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getString(R.string.editServersTitle));
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_ServersEdit);
			}
		} else {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.getWindow().getDecorView().setBackgroundColor(Color.WHITE);
			super.setTheme(R.style.ListFont);
			setContentView(R.layout.servers_edit);
		}
		
		long masterId = getIntent().getExtras().getLong("masterId");
		Log.v("", "trying to retrieve from id " + masterId);
		m = muninFoo.getMasterById((int) masterId);
		
		deletedServers = new ArrayList<MuninServer>();
		serversList = new ArrayList<MuninServer>();
		
		for (MuninServer s : m.getChildren())
			serversList.add(s);
		
		updateList(true);
		
		DragSortListView dslv = (DragSortListView) getListView();
		dslv.setDropListener(onDrop);
		dslv.setRemoveListener(onRemove);
		SimpleFloatViewManager sfvm = new SimpleFloatViewManager(dslv);
		sfvm.setBackgroundColor(Color.TRANSPARENT);
		dslv.setFloatViewManager(sfvm);
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	public void updateList(boolean firstTime) {
		list.clear();
		HashMap<String,String> item;
		for (MuninServer s : serversList) {
			item = new HashMap<String,String>();
			item.put("line1", s.getName());
			item.put("line2", s.getServerUrl());
			list.add(item);
		}
		sa = new SimpleAdapter(this, list, R.layout.serversedit_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
		
		if (firstTime)
			getListView().setAdapter(sa);
		else
			((BaseAdapter) getListView().getAdapter()).notifyDataSetChanged();
	}
	
	public void actionSave() {
		for (MuninServer s: deletedServers)
			muninFoo.deleteServer(s);
		
		for (int i=0; i<serversList.size(); i++)
			muninFoo.getServer(serversList.get(i).getServerUrl()).setPosition(i);
		
		muninFoo.sqlite.saveServers();
		muninFoo.resetInstance(this);
	}
	
	private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			MuninServer item = serversList.get(from);
			serversList.remove(from);
			serversList.add(to, item);
			updateList(false);
		}
	};
	
	private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
		@Override
		public void remove(int which) {
			MuninServer item = serversList.get(which);
			deletedServers.add(item);
			serversList.remove(which);
			updateList(false);
		}
	};
	
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
		getMenuInflater().inflate(R.menu.serversedit, menu);
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
					intent = new Intent(this, Activity_Servers.class);
					intent.putExtra("fromMaster", m.getId());
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					Util.setTransition(c, TransitionStyle.SHALLOWER);
				}
				return true;
			case R.id.menu_revert:
				intent = new Intent(this, Activity_Servers.class);
				intent.putExtra("fromMaster", m.getId());
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				Util.setTransition(c, TransitionStyle.SHALLOWER);
				return true;
			case R.id.menu_save:
				actionSave();
				intent = new Intent(this, Activity_Servers.class);
				intent.putExtra("fromMaster", m.getId());
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				Util.setTransition(c, TransitionStyle.SHALLOWER);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_ServersEdit.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_ServersEdit.this, Activity_About.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Servers.class);
		intent.putExtra("fromMaster", m.getId());
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