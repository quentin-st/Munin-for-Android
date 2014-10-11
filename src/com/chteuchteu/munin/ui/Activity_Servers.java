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
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.Adapter_ExpandableListView;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.ImportExportHelper;
import com.chteuchteu.munin.hlpr.ImportExportHelper.Export.ExportRequestMaker;
import com.chteuchteu.munin.hlpr.ImportExportHelper.Import.ImportRequestMaker;
import com.chteuchteu.munin.hlpr.JSONHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninServer;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

@SuppressLint("DefaultLocale")
public class Activity_Servers extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private static Context	context;
	
	private Map<String, List<String>> serversCollection;
	private ExpandableListView expListView;
	private Menu 			menu;
	private MenuItem		importExportMenuItem;
	private MenuItem		exportMenuItem;
	private String			activityName;
	
	public static boolean	menu_firstLoad = true;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		context = this;
		
		setContentView(R.layout.servers);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(getString(R.string.serversTitle));
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(dh.Activity_Servers);
		
		Util.UI.applySwag(this);
		
		Intent i = getIntent();
		MuninMaster fromServersEdit = null;
		if (i.getExtras() != null && i.getExtras().containsKey("fromMaster"))
			fromServersEdit = muninFoo.getMasterById((int) i.getExtras().getLong("fromMaster"));
		
		expListView = (ExpandableListView) findViewById(R.id.servers_list);
		
		List<String> masters = muninFoo.getMastersNames();
		this.serversCollection = getServersCollection();
		final Adapter_ExpandableListView expListAdapter = new Adapter_ExpandableListView(this, this, masters, serversCollection);
		expListView.setAdapter(expListAdapter);
		
		if (fromServersEdit != null)
			expListView.expandGroup(muninFoo.getMasterPosition(fromServersEdit));
		
		if (muninFoo.getHowManyServers() == 0)
			((LinearLayout)findViewById(R.id.servers_noserver)).setVisibility(View.VISIBLE);
	}
	
	private Map<String, List<String>> getServersCollection() {
		// Create collection
		LinkedHashMap<String, List<String>> serversCollection = new LinkedHashMap<String, List<String>>();
		
		for (MuninMaster m : muninFoo.masters) {
			List<String> childList = new ArrayList<String>();
			for (MuninServer s : m.getOrderedChildren())
				childList.add(s.getName());
			serversCollection.put(m.getName(), childList);
		}
		
		return serversCollection;
	}
	
	/**
	 * Called when a click event is triggered on a child-level element of the listview
	 * Called from @see com.chteuchteu.munin.Adapter_ExpandableListView#getChildView(int, int, boolean, View, android.view.ViewGroup)
	 * @param groupPosition
	 * @param childPosition
	 */
	public void onChildClick(int groupPosition, int childPosition) {
		//final String selected = (String) expListAdapter.getChild(groupPosition, childPosition);
		MuninServer s = muninFoo.masters.get(groupPosition).getServerFromFlatPosition(childPosition);
		Intent intent = new Intent(Activity_Servers.this, Activity_Server.class);
		intent.putExtra("contextServerUrl", s.getServerUrl());
		intent.putExtra("action", "edit");
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.DEEPER);
	}
	
	/**
	 * Called when a long click event is triggered on a child-level element of the listview
	 * Called from @see com.chteuchteu.munin.Adapter_ExpandableListView#getChildView(int, int, boolean, View, android.view.ViewGroup)
	 * @param groupPosition
	 * @param childPosition
	 * @return
	 */
	public boolean onChildLongClick(int groupPosition, int childPosition) {
		final MuninServer server = muninFoo.masters.get(groupPosition).getServerFromFlatPosition(childPosition);
		
		// Display actions list
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				context, android.R.layout.simple_list_item_1);
		arrayAdapter.add(context.getString(R.string.menu_addserver_delete));
		
		builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0:
						new AlertDialog.Builder(context)
						.setTitle(R.string.delete)
						.setMessage(R.string.text83)
						.setPositiveButton(R.string.text33, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// When going back : expand the list to the current master if possible
								MuninMaster m = null;
								if (server.getParent() != null && server.getParent().getChildren().size() > 1)
									m = server.getParent();
								
								muninFoo.sqlite.dbHlpr.deleteServer(server);
								muninFoo.deleteServer(server, true);
								
								if (muninFoo.currentServer.equalsApprox(server)) {
									if (muninFoo.getHowManyServers() == 0)
										muninFoo.currentServer = null;
									else
										muninFoo.currentServer = muninFoo.getServer(0);
								}
								
								Intent intent = new Intent(Activity_Servers.this, Activity_Servers.class);
								if (m != null)
									intent.putExtra("fromMaster", m.getId());
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								startActivity(intent);
							}
						})
						.setNegativeButton(R.string.text34, null)
						.show();
						break;
				}
			}
		});
		builderSingle.show();
		
		return true;
	}
	
	/**
	 * Called when a click event is triggered on the overflow icon on each
	 * parent-level list item
	 * Called from @see com.chteuchteu.munin.Adapter_ExpandableListView#getGroupView(int, boolean, View, android.view.ViewGroup)
	 * @param position
	 */
	public void onGroupItemOptionsClick(final int position) {
		// Display actions list
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				context, android.R.layout.simple_list_item_1);
		arrayAdapter.add(context.getString(R.string.delete_master));
		arrayAdapter.add(context.getString(R.string.editServersTitle));
		arrayAdapter.add(context.getString(R.string.renameMaster));
		
		builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final MuninMaster master = muninFoo.masters.get(position);
				
				switch (which) {
					case 0:
						muninFoo.deleteMuninMaster(master);
						context.startActivity(new Intent(context, Activity_Servers.class));
						
						break;
					case 1:
						Intent i = new Intent(context, Activity_ServersEdit.class);
						i.putExtra("masterId", master.getId());
						context.startActivity(i);
						Util.setTransition(context, TransitionStyle.DEEPER);
						break;
					case 2:
						final EditText input = new EditText(context);
						input.setText(master.getName());
						
						new AlertDialog.Builder(context)
						.setTitle(R.string.renameMaster)
						.setView(input)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String value = input.getText().toString();
								if (!value.equals(master.getName())) {
									master.setName(value);
									MuninFoo.getInstance(context).sqlite.dbHlpr.updateMuninMaster(master);
									context.startActivity(new Intent(context, Activity_Servers.class));
								}
								dialog.dismiss();
							}
						}).setNegativeButton(R.string.text64, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) { }
						}).show();
						break;
				}
			}
		});
		builderSingle.show();
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
				String code = ((EditText) dialogView.findViewById(R.id.import_code)).getText().toString();
				code = code.toLowerCase();
				new ImportRequestMaker(code, context).execute();
				dialog.dismiss();
			}
		})
		.setNegativeButton(R.string.text64, null)
		.show();
	}
	
	public static void onExportSuccess(String pswd) {
		final View dialogView = View.inflate(context, R.layout.dialog_export_success, null);
		TextView code = (TextView) dialogView.findViewById(R.id.export_succes_code);
		Util.Fonts.setFont(context, code, CustomFont.RobotoCondensed_Bold);
		code.setText(pswd);
		
		new AlertDialog.Builder(context)
		.setTitle(R.string.export_success_title)
		.setView(dialogView)
		.setCancelable(true)
		.setPositiveButton("OK", null)
		.show();
	}
	
	public static void onExportError() {
		Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
	}
	
	public static void onImportSuccess() {
		new AlertDialog.Builder(context)
		.setTitle(R.string.import_success_title)
		.setMessage(R.string.import_success_txt1)
		.setCancelable(true)
		.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				context.startActivity(new Intent(context, Activity_Servers.class));
			}
		})
		.show();
	}
	
	public static void onImportError() {
		Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
	}
	
	private void displayExportDialog() {
		new AlertDialog.Builder(context)
		.setTitle(R.string.export_servers)
		.setMessage(R.string.export_explanation)
		.setCancelable(true)
		.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String json = JSONHelper.getMastersJSONString(MuninFoo.getInstance().getMasters(), ImportExportHelper.ENCRYPTION_SEED);
				if (json.equals(""))
					Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show();
				else
					new ExportRequestMaker(json, context).execute();
			}
		})
		.setNegativeButton(R.string.text64, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		
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
		
		createOptionsMenu();
		return true;
	}
	private void createOptionsMenu() {
		menu.clear();
		getMenuInflater().inflate(R.menu.servers, menu);
		this.importExportMenuItem = menu.findItem(R.id.menu_importexport);
		this.exportMenuItem = menu.findItem(R.id.menu_export);
		if (!MuninFoo.isPremium(context))
			importExportMenuItem.setVisible(false);
		if (muninFoo.getHowManyServers() == 0)
			exportMenuItem.setVisible(false);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		Intent intent;
		switch (item.getItemId()) {
			case android.R.id.home:
				dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_add:
				intent = new Intent(this, Activity_Server.class);
				intent.putExtra("contextServerUrl", "");
				startActivity(intent);
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_import:
				displayImportDialog();
				return true;
			case R.id.menu_export:
				displayExportDialog();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Servers.this, Activity_Settings.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Servers.this, Activity_About.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
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
		Util.setTransition(context, TransitionStyle.SHALLOWER);
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