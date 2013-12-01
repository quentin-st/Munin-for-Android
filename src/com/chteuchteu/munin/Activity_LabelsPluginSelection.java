package com.chteuchteu.munin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


@SuppressLint("NewApi")
public class Activity_LabelsPluginSelection extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private MuninLabel		label;
	private String			activityName;
	private List<List<MuninPlugin>> labelsListCat;
	private List<MuninPlugin> correspondance;
	private List<String> 	correspondanceServers;
	private Menu			menu;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		
		// Récupération label courant
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null
				&& thisIntent.getExtras().containsKey("label")) {
			String labelName = thisIntent.getExtras().getString("label");
			label = muninFoo.getLabel(labelName);
		} else
			startActivity(new Intent(Activity_LabelsPluginSelection.this, Activity_Labels.class));
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setContentView(R.layout.labels_pluginselection);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			findViewById(R.id.viewTitle).setVisibility(View.GONE);
			findViewById(R.id.viewTitleSep).setVisibility(View.GONE);
			actionBar.setTitle(label.getName());
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_LabelsPluginSelection);
			}
		} else {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.alerts_pluginselection);
			super.setTheme(R.style.ListFont);
			((TextView) this.findViewById(R.id.viewTitle)).setText(label.getName());
		}
		
		labelsListCat = label.getPluginsSortedByServer();
		correspondance = new ArrayList<MuninPlugin>();
		correspondanceServers = new ArrayList<String>();
		SeparatedListAdapter adapter = new SeparatedListAdapter(this);
		for (List<MuninPlugin> l : labelsListCat) {
			correspondanceServers.add("");
			correspondance.add(new MuninPlugin());
			List<Map<String,?>> elements = new LinkedList<Map<String,?>>();
			String serverName = "";
			for (MuninPlugin p : l) {
				elements.add(createItem(p.getFancyName(), p.getName()));
				if (serverName.equals(""))
					serverName = p.getInstalledOn().getName();
				correspondance.add(p);
				correspondanceServers.add(p.getInstalledOn().getServerUrl());
			}
			
			adapter.addSection(serverName, new SimpleAdapter(this, elements, R.layout.pluginselection_list,
					new String[] { "title", "caption" }, new int[] { R.id.line_a, R.id.line_b }));
		}
		for (String s : correspondanceServers)
			Log.v("corresp.", "." + s);
		((ListView)findViewById(R.id.labels_listview)).setAdapter(adapter);
		
		((ListView)findViewById(R.id.labels_listview)).setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				//TextView line_a = (TextView) view.findViewById(R.id.line_a);
				//TextView line_b = (TextView) view.findViewById(R.id.line_b);
				
				Log.v("", "position : " + position);
				Log.v("", "correspondance : \t\t" + correspondance.get(position));
				Log.v("", "correspondanceServer : \t" + correspondanceServers.get(position));
				
				MuninPlugin plugin = correspondance.get(position);
				String serverUrl = correspondanceServers.get(position);
				Intent intent = new Intent(Activity_LabelsPluginSelection.this, Activity_GraphView.class);
				muninFoo.currentServer = muninFoo.getServer(serverUrl);
				int pos = muninFoo.currentServer.getPluginPosition(plugin);
				intent.putExtra("position", pos + "");
				intent.putExtra("from", "labels");
				intent.putExtra("label", label.getName());
				startActivity(intent);
				setTransition("deeper");
			}
		});
	}
	
	public Map<String,?> createItem(String title, String caption) {  
		Map<String,String> item = new HashMap<String,String>();  
		item.put("title", title);  
		item.put("caption", caption);  
		return item;  
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
					startActivity(new Intent(this, Activity_Labels.class));
					setTransition("shallower");
				}
				return true;
			case R.id.menu_delete:
				if (muninFoo.removeLabel(label))
					muninFoo.sqlite.saveMuninLabels();
				startActivity(new Intent(this, Activity_Labels.class));
				setTransition("shallower");
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_LabelsPluginSelection.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_LabelsPluginSelection.this, Activity_About.class));
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
		getMenuInflater().inflate(R.menu.labelspluginselection, menu);
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Labels.class);
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
}