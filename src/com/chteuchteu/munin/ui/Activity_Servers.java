package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ListActivity;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.obj.MuninServer;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_Servers extends ListActivity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	
	private SimpleAdapter 	sa;
	ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	public static Button 	addServer;
	private Menu 			menu;
	private String			activityName;
	
	public static boolean	menu_firstLoad = true;
	
	@SuppressLint("NewApi")
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		
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
				setTransition("deeper");
			}
		});
		
		list.clear();
		HashMap<String,String> item;
		for (int i=0; i<muninFoo.getOrderedServers().size(); i++) {
			item = new HashMap<String,String>();
			item.put("line1", muninFoo.getOrderedServers().get(i).getName());
			item.put("line2", muninFoo.getOrderedServers().get(i).getServerUrl());
			list.add(item);
		}
		sa = new SimpleAdapter(this, list, R.layout.servers_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
		setListAdapter(sa);
		
		
		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				//TextView name = (TextView) view.findViewById(R.id.line_a);
				TextView url = (TextView) view.findViewById(R.id.line_b);
				MuninServer s = muninFoo.getServer(url.getText().toString());
				if (s != null)
					muninFoo.currentServer = s;
				Intent intent = new Intent(Activity_Servers.this, Activity_AddServer.class);
				intent.putExtra("contextServerUrl", url.getText().toString());
				intent.putExtra("action", "edit");
				startActivity(intent);
				setTransition("deeper");
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
		
		if (muninFoo.getServers().size() == 0)
			menu.findItem(R.id.menu_edit).setVisible(false);
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
					setTransition("shallower");
				}
				return true;
			case R.id.menu_add:
				intent = new Intent(this, Activity_AddServer.class);
				intent.putExtra("contextServerUrl", "");
				startActivity(intent);
				setTransition("deeper");
				return true;
			case R.id.menu_edit:
				startActivity(new Intent(this, Activity_ServersEdit.class));
				setTransition("deeper");
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Servers.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Servers.this, Activity_About.class));
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