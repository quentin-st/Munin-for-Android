package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.Adapter_SeparatedList;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


public class Activity_Plugins extends ListActivity {
	private MuninFoo			muninFoo;
	private DrawerHelper		dh;
	private Context				context;
	
	private SimpleAdapter 		sa;
	private ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	private List<MuninPlugin>	pluginsList;
	private List<List<MuninPlugin>> pluginsListCat;
	private MuninPlugin[] 		pluginsFilter;
	private int					actionBarSpinnerIndex;
	
	private LinearLayout	ll_filter;
	private EditText		filter;
	private	ActionBar		actionBar;
	private boolean			actionBarListEnabled;
	private Menu 			menu;
	private String			activityName;
	
	private int mode;
	private int MODE_GROUPED = 1;
	private int MODE_FLAT = 2;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		context = this;
		setContentView(R.layout.pluginselection);
		this.actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		Util.UI.applySwag(this);
		
		// Create ActionBar spinner (dropdown) if needed
		actionBarSpinnerIndex = -1;
		actionBarListEnabled = false;
		
		if (muninFoo.getHowManyServers() > 1) {
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBarListEnabled = true;
			
			if (muninFoo.currentServer == null) // hotfix
				muninFoo.currentServer = muninFoo.getServer(0);
			
			actionBarSpinnerIndex = muninFoo.currentServer.getFlatPosition(muninFoo);
			List<String> list2 = new ArrayList<String>();
			List<MuninServer> l1 = muninFoo.getOrderedServers();
			for (MuninServer s : l1)
				list2.add(s.getName());
			
			SpinnerAdapter spinnerAdapter = new ArrayAdapter<String>(getActionBar().getThemedContext(),
					android.R.layout.simple_spinner_dropdown_item, list2);
			
			ActionBar.OnNavigationListener navigationListener = new OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					if (itemPosition != actionBarSpinnerIndex) {
						if (muninFoo.getServerFromFlatPosition(itemPosition) != null) {
							muninFoo.currentServer = muninFoo.getServerFromFlatPosition(itemPosition);
							
							actionBarSpinnerIndex = itemPosition;
							
							updateListView();
						}
					}
					return false;
				}
			};
			actionBar.setListNavigationCallbacks(spinnerAdapter, navigationListener);
			actionBar.setSelectedNavigationItem(actionBarSpinnerIndex);
		} else if (muninFoo.getHowManyServers() == 1) {
			actionBar.setTitle(getString(R.string.button_graphs));
			actionBar.setSubtitle(muninFoo.currentServer.getName());
		}
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(dh.Activity_Plugins);
		
		mode = getListViewMode();
		
		if (muninFoo.currentServer.getPlugins().size() > 0) {
			if (actionBarListEnabled)
				actionBar.setSelectedNavigationItem(muninFoo.currentServer.getFlatPosition(muninFoo));
			
			updateListView();
		}
	}
	
	public int getListViewMode() {
		if (muninFoo.currentServer == null)
			muninFoo.currentServer = muninFoo.getFirstServer();
		
		if (muninFoo.currentServer.getPluginsListWithCategory().size() < 2)
			mode = MODE_FLAT;
		else {
			if (Util.getPref(context, "listViewMode").equals("flat"))
				mode = MODE_FLAT;
			else
				mode = MODE_GROUPED;
		}
		return mode;
	}
	
	public void switchListViewMode(int mode) {
		if (mode == MODE_FLAT)
			Util.setPref(context, "listViewMode", "flat");
		else
			Util.setPref(context, "listViewMode", "grouped");
	}
	
	public void updateListView() {
		updateListView(getListViewMode());
	}
	
	public void updateListView(int mode) {
		if (mode == MODE_FLAT) {
			pluginsList = new ArrayList<MuninPlugin>();
			for (int i=0; i<muninFoo.currentServer.getPlugins().size(); i++) {
				if (muninFoo.currentServer.getPlugins().get(i) != null)
					pluginsList.add(muninFoo.currentServer.getPlugins().get(i));
			}
			
			list.clear();
			HashMap<String,String> item;
			for (MuninPlugin pl : pluginsList) {
				item = new HashMap<String,String>();
				item.put("line1", pl.getFancyName());
				item.put("line2", pl.getName());
				list.add(item);
			}
			sa = new SimpleAdapter(this, list, R.layout.pluginselection_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
			setListAdapter(sa);
		} else {
			// Create plugins list
			pluginsListCat = muninFoo.currentServer.getPluginsListWithCategory();
			
			pluginsList = new ArrayList<MuninPlugin>();
			for (int i=0; i<muninFoo.currentServer.getPlugins().size(); i++) {
				if (muninFoo.currentServer.getPlugins().get(i) != null)
					pluginsList.add(muninFoo.currentServer.getPlugins().get(i));
			}
			
			Adapter_SeparatedList adapter = new Adapter_SeparatedList(this);
			for (List<MuninPlugin> l : pluginsListCat) {
				List<Map<String,?>> elements = new LinkedList<Map<String,?>>();
				String categoryName = "";
				for (MuninPlugin p : l) {
					elements.add(createItem(p.getFancyName(), p.getName()));
					categoryName = p.getCategory();
				}
				
				adapter.addSection(categoryName, new SimpleAdapter(this, elements, R.layout.pluginselection_list,
						new String[] { "title", "caption" }, new int[] { R.id.line_a, R.id.line_b }));
			}
			this.getListView().setAdapter(adapter);
		}
		
		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				TextView plu = (TextView) view.findViewById(R.id.line_b);
				Intent intent = new Intent(Activity_Plugins.this, Activity_GraphView.class);
				int p = 0;
				for (int i=0; i<muninFoo.currentServer.getPlugins().size(); i++) {
					if (muninFoo.currentServer.getPlugin(i) != null && muninFoo.currentServer.getPlugin(i).getName().equals(plu.getText().toString())) {
						p = i;
						break;
					}
				}
				intent.putExtra("position", p + "");
				startActivity(intent);
				Util.setTransition(context, TransitionStyle.DEEPER);
			}
		});
		
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapter, final View view, final int position, long arg) {
				// Display actions list
				AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
						context, android.R.layout.simple_list_item_1);
				arrayAdapter.add(context.getString(R.string.delete_plugin));
				
				builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							TextView plu = (TextView) view.findViewById(R.id.line_b);
							for (int i=0; i<muninFoo.currentServer.getPlugins().size(); i++) {
								MuninPlugin plugin = muninFoo.currentServer.getPlugin(i);
								if (plugin != null && plugin.getName().equals(plu.getText().toString())) {
									muninFoo.currentServer.getPlugins().remove(plugin);
									muninFoo.sqlite.dbHlpr.deleteMuninPlugin(plugin);
									
									// Save scroll state
									int index = getListView().getFirstVisiblePosition();
									View v = getListView().getChildAt(0);
									int top = (v == null) ? 0 : v.getTop();
									
									updateListView();
									
									getListView().setSelectionFromTop(index, top);
									break;
								}
							}
							
							break;
						}
					}
				});
				builderSingle.show();
				
				return true;
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
		getMenuInflater().inflate(R.menu.pluginselection, menu);
		
		ll_filter = (LinearLayout) this.findViewById(R.id.ll_filter);
		filter = (EditText) this.findViewById(R.id.filter);
		
		filter.addTextChangedListener(new TextWatcher() {
			@SuppressLint("DefaultLocale")
			@Override
			public void afterTextChanged(Editable s) {
				if (pluginsList != null && pluginsList.size() > 0 && s != null) {
					list.clear();
					String search = s.toString();
					
					pluginsFilter = new MuninPlugin[pluginsList.size()];
					for (int i=0; i<pluginsList.size(); i++) {
						if (pluginsList.get(i).getFancyName().toLowerCase(Locale.ENGLISH).contains(search.toLowerCase(Locale.ENGLISH))
								|| pluginsList.get(i).getName().toLowerCase(Locale.ENGLISH).contains(search.toLowerCase(Locale.ENGLISH)))
							pluginsFilter[i] = pluginsList.get(i);
					}
					
					HashMap<String,String> item;
					for (MuninPlugin p : pluginsFilter) {
						if (p != null) {
							item = new HashMap<String,String>();
							item.put("line1", p.getFancyName());
							item.put("line2", p.getName());
							list.add(item);
						}
					}
					sa = new SimpleAdapter(Activity_Plugins.this, list, R.layout.pluginselection_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
					setListAdapter(sa);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_filter:
				if (ll_filter.getVisibility() == View.GONE) {
					filter.setFocusable(true);
					filter.setFocusableInTouchMode(true);
					ll_filter.setVisibility(View.VISIBLE);
				} else {
					ll_filter.setVisibility(View.GONE);
					filter.setFocusable(false);
					filter.setFocusableInTouchMode(false);
					filter.clearFocus();
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(filter.getWindowToken(), 0);
				}
				return true;
			case R.id.menu_modelist:
				if (mode == MODE_FLAT)
					switchListViewMode(MODE_GROUPED);
				else
					switchListViewMode(MODE_FLAT);
				updateListView();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Plugins.this, Activity_Settings.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Plugins.this, Activity_About.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
	}
	
	@Override
	public void onBackPressed() {
		if (ll_filter != null && ll_filter.getVisibility() == View.VISIBLE) {
			ll_filter.setVisibility(View.GONE);
			filter.setFocusable(false);
			filter.setFocusableInTouchMode(false);
			filter.clearFocus();
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(filter.getWindowToken(), 0);
		} else {
			Intent intent = new Intent(this, Activity_Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(context, TransitionStyle.SHALLOWER);
		}
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