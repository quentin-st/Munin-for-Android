package com.chteuchteu.munin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.SimpleAdapter;

import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleFloatViewManager;
//import com.mobeta.android.dslv.DragSortListView;
//import com.mobeta.android.dslv.SimpleFloatViewManager;

public class Activity_ServersEdit extends ListActivity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	
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
		
		// ==== ACTION BAR ====
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
		// ==== ACTION BAR ====
		
		deletedServers = new ArrayList<MuninServer>();
		serversList = new ArrayList<MuninServer>();
		for(int i=0; i<muninFoo.getOrderedServers().size(); i++) {
			serversList.add(muninFoo.getOrderedServers().get(i));
		}
		
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
		for(int i=0; i<serversList.size(); i++){
			item = new HashMap<String,String>();
			item.put("line1", serversList.get(i).getName());
			item.put("line2", serversList.get(i).getServerUrl());
			list.add(item);
		}
		sa = new SimpleAdapter(this, list, R.layout.serversedit_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
		
		if (firstTime)
			getListView().setAdapter(sa);
		else
			((BaseAdapter) getListView().getAdapter()).notifyDataSetChanged();
	}
	
	public void actionSave() {
		for (MuninServer s: deletedServers) {
			muninFoo.deleteServer(s);
		}
		
		for (int i=0; i<serversList.size(); i++) {
			muninFoo.getServer(serversList.get(i).getServerUrl()).setPosition(i);
			Log.v("", muninFoo.getServer(serversList.get(i).getServerUrl()).getName() + " -> pos. " + i);
		}
		
		muninFoo.sqlite.saveServers();
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
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					setTransition("shallower");
				}
				return true;
			case R.id.menu_revert:
				intent = new Intent(this, Activity_Servers.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				setTransition("shallower");
				return true;
			case R.id.menu_save:
				actionSave();
				intent = new Intent(this, Activity_Servers.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				setTransition("shallower");
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_ServersEdit.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_ServersEdit.this, Activity_About.class));
				setTransition("deeper");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Servers.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		setTransition("shallower");
	}
	
	public String getPref(String key) {
		return this.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	public void setPref(String key, String value) {
		if (value.equals(""))
			removePref(key);
		else {
			SharedPreferences prefs = this.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(key, value);
			editor.commit();
		}
	}
	
	public void removePref(String key) {
		SharedPreferences prefs = this.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		editor.commit();
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