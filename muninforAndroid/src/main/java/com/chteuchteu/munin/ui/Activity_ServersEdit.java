package com.chteuchteu.munin.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.SimpleAdapter;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninServer;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleFloatViewManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Activity_ServersEdit extends MuninActivity {
	private MuninMaster	master;
	private ArrayList<HashMap<String,String>> list;
	private List<MuninServer> 		serversList;
	private List<MuninServer>		deletedServers;
	private DragSortListView       listview;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_servers_edit);
		super.onContentViewSet();
		dh.setDrawerActivity(this);

		actionBar.setTitle(getString(R.string.editServersTitle));
		
		long masterId = getIntent().getExtras().getLong("masterId");
		master = muninFoo.getMasterById((int) masterId);

		if (master == null)
			startActivity(new Intent(this, Activity_Servers.class));
		
		deletedServers = new ArrayList<>();
		serversList = new ArrayList<>();
		
		for (MuninServer s : master.getChildren())
			serversList.add(s);

		listview = (DragSortListView) findViewById(R.id.listview);
		listview.setDropListener(onDrop);
		listview.setRemoveListener(onRemove);
		SimpleFloatViewManager sfvm = new SimpleFloatViewManager(listview);
		sfvm.setBackgroundColor(Color.TRANSPARENT);
		listview.setFloatViewManager(sfvm);
		list = new ArrayList<>();

		updateList(true);
	}
	
	private void updateList(boolean firstTime) {
		list.clear();
		HashMap<String,String> item;
		for (MuninServer s : serversList) {
			item = new HashMap<>();
			item.put("line1", s.getName());
			item.put("line2", s.getServerUrl());
			list.add(item);
		}
		SimpleAdapter sa = new SimpleAdapter(this, list, R.layout.serversedit_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
		
		if (firstTime)
			listview.setAdapter(sa);
		else
			((BaseAdapter)listview.getAdapter()).notifyDataSetChanged();
	}
	
	private void actionSave() {
		for (MuninServer s: deletedServers) {
			muninFoo.deleteServer(s);
			muninFoo.sqlite.dbHlpr.deleteServer(s);
		}
		
		for (int i=0; i<serversList.size(); i++) {
			muninFoo.getServer(serversList.get(i).getServerUrl()).setPosition(i);
			muninFoo.sqlite.dbHlpr.saveMuninServer(serversList.get(i));
		}
		
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

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.serversedit, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		Intent intent;
		switch (item.getItemId()) {
			case R.id.menu_revert:
				intent = new Intent(this, Activity_Servers.class);
				intent.putExtra("fromMaster", master.getId());
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				Util.setTransition(context, TransitionStyle.SHALLOWER);
				return true;
			case R.id.menu_save:
				actionSave();
				intent = new Intent(this, Activity_Servers.class);
				intent.putExtra("fromMaster", master.getId());
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				Util.setTransition(context, TransitionStyle.SHALLOWER);
				return true;
		}

		return true;
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Servers.class);
		intent.putExtra("fromMaster", master.getId());
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
	}
}