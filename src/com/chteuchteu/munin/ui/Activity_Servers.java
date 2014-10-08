package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.Adapter_ExpandableListView;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.ImportExportHelper.ExportRequestMaker;
import com.chteuchteu.munin.hlpr.JSONHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninServer;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_Servers extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private static Context		c;
	
	Map<String, List<String>> serversCollection;
	ExpandableListView		expListView;
	private Menu 			menu;
	private MenuItem		importExportMenuItem;
	private String			activityName;
	
	public static boolean	menu_firstLoad = true;
	
	@SuppressLint("NewApi")
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		c = this;
		
		setContentView(R.layout.servers);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(getString(R.string.serversTitle));
		
		if (muninFoo.drawer) {
			dh = new DrawerHelper(this, muninFoo);
			dh.setDrawerActivity(dh.Activity_Servers);
		}
		
		Util.UI.applySwag(this);
		
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
		final Adapter_ExpandableListView expListAdapter = new Adapter_ExpandableListView(this, masters, serversCollection, muninFoo);
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
		
		if (muninFoo.getHowManyServers() == 0)
			((LinearLayout)findViewById(R.id.servers_noserver)).setVisibility(View.VISIBLE);
	}
	
	private void displayImportDialog() {
		final View dialogView = View.inflate(this, R.layout.dialog_import, null);
		new AlertDialog.Builder(this)
			.setTitle(R.string.import_title)
			.setView(dialogView)
			.setCancelable(true)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String jsonTxt = ((EditText) dialogView.findViewById(R.id.json_input)).getText().toString();
					ArrayList<MuninMaster> newMasters = JSONHelper.getMastersFromJSONString(jsonTxt);
					for (MuninMaster newMaster : newMasters) {
						MuninFoo.getInstance().getMasters().add(newMaster);
						for (MuninServer server : newMaster.getChildren())
							MuninFoo.getInstance().addServer(server);
					} // TODO
					MuninFoo.getInstance().sqlite.saveServers();
				}
			})
			.setNegativeButton(R.string.text64, null)
			.show();
	}
	
	public static void onExportSuccess(String pswd) {
		final View dialogView = View.inflate(c, R.layout.dialog_export_success, null);
		TextView code = (TextView) dialogView.findViewById(R.id.export_succes_code);
		Util.Fonts.setFont(c, code, CustomFont.RobotoCondensed_Bold);
		code.setText(pswd);
		
		new AlertDialog.Builder(c)
			.setTitle(R.string.export_success_title)
			.setView(dialogView)
			.setCancelable(true)
			.setPositiveButton("OK", null)
			.show();
	}
	
	public static void onExportError() {
		// TODO
	}
	
	private void displayExportDialog() {
		String json = JSONHelper.getMastersJSONString(MuninFoo.getInstance().getMasters(), true);
		if (json.equals(""))
			Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
		else
			new ExportRequestMaker(json).execute();
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
		this.importExportMenuItem = menu.findItem(R.id.menu_importexport);
		if (!muninFoo.premium || muninFoo.getHowManyServers() == 0)
			importExportMenuItem.setVisible(false);
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
			case R.id.menu_import:
				displayImportDialog();
				return true;
			case R.id.menu_export:
				displayExportDialog();
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
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStop(this);
	}
}