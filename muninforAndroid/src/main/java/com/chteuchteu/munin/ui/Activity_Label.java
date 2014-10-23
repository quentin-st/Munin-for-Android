package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.chteuchteu.munin.Adapter_SeparatedList;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


public class Activity_Label extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private Context			c;
	private Label			label;
	private String			activityName;
	private List<List<MuninPlugin>> labelsListCat;
	private List<MuninPlugin> correspondance;
	private List<String> 	correspondanceServers;
	private Menu			menu;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		c = this;
		
		// Getting current label
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null
				&& thisIntent.getExtras().containsKey("label")) {
			String labelName = thisIntent.getExtras().getString("label");
			label = muninFoo.getLabel(labelName);
			if (label == null) {
				Toast.makeText(this, "Error while trying to display this list...", Toast.LENGTH_LONG).show();
				startActivity(new Intent(Activity_Label.this, Activity_Labels.class));
			}
		} else
			startActivity(new Intent(Activity_Label.this, Activity_Labels.class));
		
		
		setContentView(R.layout.labels_pluginselection);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(label.getName());
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(DrawerHelper.Activity_Label);
		
		Util.UI.applySwag(this);
		
		labelsListCat = label.getPluginsSortedByServer(muninFoo);
		correspondance = new ArrayList<MuninPlugin>();
		correspondanceServers = new ArrayList<String>();
		Adapter_SeparatedList adapter = new Adapter_SeparatedList(this, false);
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
			
			adapter.addSection(serverName, new SimpleAdapter(this, elements, R.layout.plugins_list,
					new String[] { "title", "caption" }, new int[] { R.id.line_a, R.id.line_b }));
		}
		
		ListView labels_listView = (ListView) findViewById(R.id.labels_listview);
		
		labels_listView.setAdapter(adapter);
		labels_listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				//TextView line_a = (TextView) view.findViewById(R.id.line_a);
				//TextView line_b = (TextView) view.findViewById(R.id.line_b);
				
				MuninPlugin plugin = correspondance.get(position);
				String serverUrl = correspondanceServers.get(position);
				Intent intent = new Intent(Activity_Label.this, Activity_GraphView.class);
				muninFoo.currentServer = muninFoo.getServer(serverUrl);
				int pos = muninFoo.currentServer.getPluginPosition(plugin);
				intent.putExtra("position", pos + "");
				intent.putExtra("from", "labels");
				intent.putExtra("label", label.getName());
				startActivity(intent);
				Util.setTransition(c, TransitionStyle.DEEPER);
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
		if (item.getItemId() != android.R.id.home)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Label.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Label.this, Activity_About.class));
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
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Labels.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(c, TransitionStyle.SHALLOWER);
	}
}