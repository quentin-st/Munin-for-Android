package com.chteuchteu.munin.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.taptwo.android.widget.TitleFlowIndicator;
import org.taptwo.android.widget.ViewFlow;
import org.taptwo.android.widget.ViewFlow.ViewSwitchListener;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.GraphView_Adapter;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_GraphView extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh = null;
	private	int				previousPos = -1;
	
	public static String	load_period;
	public static ViewFlow	viewFlow;
	public static int		position;
	public static Bitmap[]	bitmaps;
	
	private Spinner 		spinner;
	private MenuItem		item_previous;
	private MenuItem		item_next;
	private ImageButton 	btn_previous;
	private ImageButton 	btn_next;
	private ImageButton		btn_list;
	private Menu 			menu;
	private String			activityName;
	
	
	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		// Point d'entrée: widgets
		Crashlytics.start(this);
		
		
		if (getPref("graphview_orientation").equals("vertical"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else if (getPref("graphview_orientation").equals("horizontal"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.getWindow().getDecorView().setBackgroundColor(Color.WHITE);
		}
		setContentView(R.layout.graphview);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			if (muninFoo.currentServer != null)
				actionBar.setTitle(muninFoo.currentServer.getName());
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_GraphView);
			}
		} else {
			findViewById(R.id.comp_relativelayout).setVisibility(View.VISIBLE);
			btn_previous = (ImageButton) findViewById(R.id.comp_previous);
			btn_next = (ImageButton) findViewById(R.id.comp_next);
			btn_list = (ImageButton) findViewById(R.id.comp_list);
			
			btn_previous.setOnClickListener(new OnClickListener() {	@Override
				public void onClick(View actualView) {	actionPrevious();	}
			});
			btn_next.setOnClickListener(new OnClickListener() {		@Override
				public void onClick(View actualView) {	actionNext();	}
			});
			findViewById(R.id.comp_refresh).setOnClickListener(new OnClickListener() {	@Override
				public void onClick(View actualView) {	actionRefresh();	}
			});
			btn_list.setOnClickListener(new OnClickListener() {	@Override
				public void onClick(View actualView) {	actionCompList();	}
			});
		}
		
		spinner = (Spinner)findViewById(R.id.spinner);
		// Remplissage spinner period
		List<String> list = new ArrayList<String>();
		list.add(getString(R.string.text47_1)); list.add(getString(R.string.text47_2));
		list.add(getString(R.string.text47_3)); list.add(getString(R.string.text47_4));
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(dataAdapter);
		// Fin remplissage spinner
		
		// Origine = widget
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null
				&& thisIntent.getExtras().containsKey("server")
				&& thisIntent.getExtras().containsKey("plugin")
				&& thisIntent.getExtras().containsKey("period")) {
			
			String server = thisIntent.getExtras().getString("server");
			String plugin = thisIntent.getExtras().getString("plugin");
			String period = thisIntent.getExtras().getString("period");
			// Setting currentServer
			for (MuninServer s : muninFoo.getServers()) {
				if (s.getServerUrl().equals(server)) {
					muninFoo.currentServer = s; break;
				}
			}
			
			// Giving position of plugin in list to GraphView
			for (int i=0; i<muninFoo.currentServer.getPlugins().size(); i++) {
				if (muninFoo.currentServer.getPlugins().get(i) != null && muninFoo.currentServer.getPlugins().get(i).getName().equals(plugin)) {
					thisIntent.putExtra("position", "" + i);
				}
			}
			
			if (period.equals("day"))		spinner.setSelection(0);
			else if (period.equals("week"))	spinner.setSelection(1);
			else if (period.equals("month"))	spinner.setSelection(2);
			else if (period.equals("year"))	spinner.setSelection(3);
			else							spinner.setSelection(0);
		}
		
		if (muninFoo.currentServer == null)
			muninFoo.currentServer = muninFoo.getServer(0);
		
		
		// Recuperation du nom du plugin
		int pos = 0;
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("position")) {
			String position = thisIntent.getExtras().getString("position");
			pos = Integer.parseInt(position);
			
			if (savedInstanceState != null)
				pos = savedInstanceState.getInt("position");
			
			
			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
					bitmaps = new Bitmap[muninFoo.currentServer.getPlugins().size()];
					if (position == 0)	Activity_GraphView.load_period = "day";
					else if (position == 1)	Activity_GraphView.load_period = "week";
					else if (position == 2)	Activity_GraphView.load_period = "month";
					else if (position == 3)	Activity_GraphView.load_period = "year";
					else 					Activity_GraphView.load_period = "day";
					
					if (viewFlow != null) // Update Viewflow
						viewFlow.setSelection(viewFlow.getSelectedItemPosition());
				}
				public void onNothingSelected(AdapterView<?> parentView) { }
			});
		} else { // Vérification de l'intent ratée: redirection
			// Redirection vers la liste des plugins
			Intent intent2 = new Intent(Activity_GraphView.this, Activity_PluginSelection.class);
			startActivity(intent2);
		}
		
		// Viewflow
		position = pos;
		bitmaps = new Bitmap[muninFoo.currentServer.getPlugins().size()];
		viewFlow = (ViewFlow) findViewById(R.id.viewflow);
		GraphView_Adapter adapter = new GraphView_Adapter(this);
		viewFlow.setAdapter(adapter, pos);
		viewFlow.setAnimationEnabled(getPref("transitions").equals("true"));
		Log.v("", "animations : " + getPref("transitions").equals("true"));
		TitleFlowIndicator indicator = (TitleFlowIndicator) findViewById(R.id.viewflowindic);
		indicator.setTitleProvider(adapter);
		viewFlow.setFlowIndicator(indicator);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (dh != null) {
				dh.setViewFlow(viewFlow);
				dh.initPluginsList();
			}
		}
		
		viewFlow.setOnViewSwitchListener(new ViewSwitchListener() {
			public void onSwitched(View v, int position) {
				Activity_GraphView.position = position;
				if (item_previous != null && item_next != null) {
					if (viewFlow.getSelectedItemPosition() == 0) {
						item_previous.setIcon(R.drawable.blank);
						item_previous.setEnabled(false);
					} else if (viewFlow.getSelectedItemPosition() == muninFoo.currentServer.getPlugins().size()-1) {
						item_next.setIcon(R.drawable.blank);
						item_next.setEnabled(false);
					} else {
						item_previous.setIcon(R.drawable.navigation_previous_item_dark);
						item_next.setIcon(R.drawable.navigation_next_item_dark);
						item_previous.setEnabled(true);
						item_next.setEnabled(true);
					}
				} else if (btn_previous != null && btn_next != null) {
					if (viewFlow.getSelectedItemPosition() == 0)
						btn_previous.setVisibility(View.GONE);
					else if (viewFlow.getSelectedItemPosition() == muninFoo.currentServer.getPlugins().size()-1)
						btn_next.setVisibility(View.GONE);
					else {
						btn_previous.setVisibility(View.VISIBLE);
						btn_next.setVisibility(View.VISIBLE);
					}
				}
				if (muninFoo.drawer && dh != null) {
					int scroll = dh.getDrawerScrollY();
					if (previousPos != -1) {
						if (previousPos < viewFlow.getSelectedItemPosition())
							scroll += 97;
						else
							scroll -= 97;
					}
					
					dh.initPluginsList(scroll);
					previousPos = viewFlow.getSelectedItemPosition();
				}
			}
		});
		
		if (!isOnline())
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		
		savedInstanceState.putInt("position", position);
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
		getMenuInflater().inflate(R.menu.graphview, menu);
		
		item_previous = menu.findItem(R.id.menu_previous);
		item_next = menu.findItem(R.id.menu_next);
		
		// Grisage eventuel des boutons next et previous
		if (viewFlow.getSelectedItemPosition() == 0) {
			item_previous.setIcon(R.drawable.blank);
			item_previous.setEnabled(false);
		} else if (viewFlow.getSelectedItemPosition() == muninFoo.currentServer.getPlugins().size()-1) {
			item_next.setIcon(R.drawable.blank);
			item_next.setEnabled(false);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				if (findViewById(R.id.serverSwitch_mask).getVisibility() == View.VISIBLE) {
					if (findViewById(R.id.labels_container).getVisibility() == View.VISIBLE)
						actionCloseLabels();
					else
						actionServerSwitchQuit();
				} else {
					if (muninFoo.drawer)
						dh.getDrawer().toggle(true);
					else {
						Intent thisIntent = getIntent();
						if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("from")) {
							String from = thisIntent.getExtras().getString("from");
							if (from.equals("labels")) {
								if (thisIntent.getExtras().containsKey("label")) {
									Intent intent = new Intent(Activity_GraphView.this, Activity_LabelsPluginSelection.class);
									intent.putExtra("label", thisIntent.getExtras().getString("label"));
									startActivity(intent);
									setTransition("shallower");
								}
							} else if (from.equals("alerts")) {
								if (thisIntent.getExtras().containsKey("server")) {
									if (muninFoo.getServer(thisIntent.getExtras().getString("server")) != null)
										muninFoo.currentServer = muninFoo.getServer(thisIntent.getExtras().getString("server"));
									Intent intent = new Intent(Activity_GraphView.this, Activity_AlertsPluginSelection.class);
									startActivity(intent);
									setTransition("shallower");
								}
							}
						} else {
							Intent intent = new Intent(this, Activity_PluginSelection.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							setTransition("shallower");
						}
					}
				}
				return true;
			case R.id.menu_previous:	actionPrevious();		return true;
			case R.id.menu_next:		actionNext();			return true;
			case R.id.menu_refresh:		actionRefresh(); 		return true;
			case R.id.menu_save:		actionSave();			return true;
			case R.id.menu_switchServer:actionServerSwitch();	return true;
			case R.id.menu_labels:
				if (findViewById(R.id.labels_container).getVisibility() == View.GONE)
					actionLabels();
				else
					actionCloseLabels();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_GraphView.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_GraphView.this, Activity_About.class));
				setTransition("deeper");
				return true;
			default:	return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onBackPressed() {
		if (findViewById(R.id.serverSwitch_mask).getVisibility() == View.VISIBLE) {
			if (findViewById(R.id.labels_container).getVisibility() == View.VISIBLE)
				actionCloseLabels();
			else
				actionServerSwitchQuit();
		} else {
			//recycleBitmaps();
			Intent thisIntent = getIntent();
			if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("from")) {
				String from = thisIntent.getExtras().getString("from");
				if (from.equals("labels")) {
					if (thisIntent.getExtras().containsKey("label")) {
						Intent intent = new Intent(Activity_GraphView.this, Activity_LabelsPluginSelection.class);
						intent.putExtra("label", thisIntent.getExtras().getString("label"));
						startActivity(intent);
						setTransition("shallower");
					}
				} else if (from.equals("alerts")) {
					if (thisIntent.getExtras().containsKey("server")) {
						if (muninFoo.getServer(thisIntent.getExtras().getString("server")) != null)
							muninFoo.currentServer = muninFoo.getServer(thisIntent.getExtras().getString("server"));
						Intent intent = new Intent(Activity_GraphView.this, Activity_AlertsPluginSelection.class);
						startActivity(intent);
						setTransition("shallower");
					}
				}
			} else {
				Intent intent = new Intent(this, Activity_PluginSelection.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				setTransition("shallower");
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void actionServerSwitch() {
		ListView switch_server = (ListView) findViewById(R.id.serverSwitch_listview);
		switch_server.setVisibility(View.VISIBLE);
		findViewById(R.id.serverSwitch_mask).setVisibility(View.VISIBLE);
		
		findViewById(R.id.serverSwitch_mask).setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { actionServerSwitchQuit(); } });
		
		int screenH = 0;
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			screenH = size.y;
		} else {
			Display display = getWindowManager().getDefaultDisplay();
			screenH = display.getHeight();
		}
		// Animation translation listview
		TranslateAnimation a1 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
				Animation.RELATIVE_TO_SELF, 0,
				Animation.ABSOLUTE, screenH,
				Animation.RELATIVE_TO_SELF, 0);
		a1.setDuration(300);
		a1.setFillAfter(true);
		a1.setInterpolator(new AccelerateDecelerateInterpolator());
		
		// Animation alpha fond
		AlphaAnimation a2 = new AlphaAnimation(0.0f, 1.0f);
		a2.setDuration(300);
		
		switch_server.startAnimation(a1);
		findViewById(R.id.serverSwitch_mask).startAnimation(a2);
		
		MuninPlugin currentPlugin = muninFoo.currentServer.getPlugin(viewFlow.getSelectedItemPosition());
		
		ArrayList<HashMap<String,String>> servers_list = new ArrayList<HashMap<String,String>>();
		servers_list.clear();
		HashMap<String,String> item;
		List<MuninServer> liste = muninFoo.getServersFromPlugin(currentPlugin);
		for (int i=0; i<liste.size(); i++) {
			item = new HashMap<String,String>();
			item.put("line1", liste.get(i).getName());
			item.put("line2", liste.get(i).getServerUrl());
			servers_list.add(item);
		}
		SimpleAdapter sa = new SimpleAdapter(this, servers_list, R.layout.servers_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
		switch_server.setAdapter(sa);
		
		switch_server.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				TextView url = (TextView) view.findViewById(R.id.line_b);
				MuninServer s = muninFoo.getServer(url.getText().toString());
				
				if (!s.equalsApprox(muninFoo.currentServer)) {
					MuninPlugin plugin = muninFoo.currentServer.getPlugin(viewFlow.getSelectedItemPosition());
					
					if (s != null)
						muninFoo.currentServer = s;
					Intent intent = new Intent(Activity_GraphView.this, Activity_GraphView.class);
					intent.putExtra("contextServerUrl", url.getText().toString());
					intent.putExtra("position", muninFoo.currentServer.getPosition(plugin) + "");
					startActivity(intent);
					setTransition("deeper");
				} else
					actionServerSwitchQuit();
			}
		});
	}
	public void actionServerSwitchQuit() {
		ListView switch_server = (ListView) findViewById(R.id.serverSwitch_listview);
		switch_server.setVisibility(View.GONE);
		findViewById(R.id.serverSwitch_mask).setVisibility(View.GONE);
		
		// Animation alpha
		AlphaAnimation a = new AlphaAnimation(1.0f, 0.0f);
		a.setDuration(300);
		
		switch_server.startAnimation(a);
		findViewById(R.id.serverSwitch_mask).startAnimation(a);
	}
	
	public void actionPrevious() {
		if (item_previous != null && item_next != null) {
			if (viewFlow.getSelectedItemPosition() == 0) {
				item_previous.setIcon(R.drawable.blank);
				item_previous.setEnabled(false);
			} else if (viewFlow.getSelectedItemPosition() == muninFoo.currentServer.getPlugins().size()-1) {
				item_next.setIcon(R.drawable.blank);
				item_next.setEnabled(false);
			} else {
				item_previous.setIcon(R.drawable.navigation_previous_item_dark);
				item_next.setIcon(R.drawable.navigation_next_item_dark);
				item_previous.setEnabled(true);
				item_next.setEnabled(true);
			}
		} else if (btn_previous != null && btn_next != null) {
			if (viewFlow.getSelectedItemPosition() == 0) {
				btn_previous.setVisibility(View.GONE);
			} else if (viewFlow.getSelectedItemPosition() == muninFoo.currentServer.getPlugins().size()-1) {
				btn_next.setVisibility(View.GONE);
			} else {
				btn_previous.setVisibility(View.VISIBLE);
				btn_next.setVisibility(View.VISIBLE);
			}
		}
		if (viewFlow.getSelectedItemPosition() != 0)
			viewFlow.setSelection(viewFlow.getSelectedItemPosition() - 1);
	}
	public void actionNext() {
		if (item_previous != null && item_next != null) {
			if (viewFlow.getSelectedItemPosition() == 0) {
				item_previous.setIcon(R.drawable.blank);
				item_previous.setEnabled(false);
			} else if (viewFlow.getSelectedItemPosition() == muninFoo.currentServer.getPlugins().size()-1) {
				item_next.setIcon(R.drawable.blank);
				item_next.setEnabled(false);
			} else {
				item_previous.setIcon(R.drawable.navigation_previous_item_dark);
				item_next.setIcon(R.drawable.navigation_next_item_dark);
				item_previous.setEnabled(true);
				item_next.setEnabled(true);
			}
		} else if (btn_previous != null && btn_next != null) {
			if (viewFlow.getSelectedItemPosition() == 0) {
				btn_previous.setVisibility(View.GONE);
			} else if (viewFlow.getSelectedItemPosition() == muninFoo.currentServer.getPlugins().size()-1) {
				btn_next.setVisibility(View.GONE);
			} else {
				btn_previous.setVisibility(View.VISIBLE);
				btn_next.setVisibility(View.VISIBLE);
			}
		}
		if (viewFlow.getSelectedItemPosition() != muninFoo.currentServer.getPlugins().size()-1)
			viewFlow.setSelection(viewFlow.getSelectedItemPosition() + 1);
	}
	public void actionRefresh() {
		bitmaps = new Bitmap[muninFoo.currentServer.getPlugins().size()];
		if (viewFlow != null)
			viewFlow.setSelection(viewFlow.getSelectedItemPosition());
	}
	public void actionSave() {
		Bitmap image = null;
		if (viewFlow.getSelectedItemPosition() >= 0 && viewFlow.getSelectedItemPosition() < bitmaps.length)
			image = bitmaps[viewFlow.getSelectedItemPosition()];
		if (image != null) {
			String root = Environment.getExternalStorageDirectory().toString();
			File dir = new File(root + "/muninForAndroid/");
			if(!dir.exists() || !dir.isDirectory())
				dir.mkdir();
			
			String pluginName = muninFoo.currentServer.getPlugin(viewFlow.getSelectedItemPosition()).getFancyName();
			
			String fileName1 = muninFoo.currentServer.getName() + " - " + pluginName + " by " + spinner.getSelectedItem().toString();
			String fileName2 = "01.png";
			File file = new File(dir, fileName1 + fileName2);
			int i = 1; 	String i_s = "";
			while (file.exists()) {
				if (i<99) {
					if (i<10)	i_s = "0" + i;
					else		i_s = "" + i;
					fileName2 = i_s + ".png";
					file = new File(dir, fileName1 + fileName2);
					i++;
				}
				else
					break;
			}
			if (file.exists())
				file.delete();
			
			try {
				FileOutputStream out = new FileOutputStream(file);
				image.compress(Bitmap.CompressFormat.PNG, 100, out);
				out.flush();
				out.close();
				String filePath = dir + fileName1 + fileName2;
				MediaScannerConnection.scanFile(Activity_GraphView.this, new String[] { filePath }, null, null);
				// Graph saved as /muninForAndroid/[...]
				Toast.makeText(this, getString(R.string.text28) + fileName1 + fileName2, Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				// Error while saving the graph
				Toast.makeText(this, getString(R.string.text29), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
	}
	
	public void actionCompList() {
		List<String> actions = new ArrayList<String>();
		actions.add(getString(R.string.menu_graph_save) + "::0");
		actions.add(getString(R.string.menu_graph_switch) + "::1");
		actions.add(getString(R.string.button_labels) + "::2");
		
		String[] popupActionsItems = new String[actions.size()];
		actions.toArray(popupActionsItems);
		//popupActions = popupActions();
		final PopupWindow popupActions = new PopupWindow(this);
		ListView lv = new ListView(this);
		lv.setAdapter(actionsAdapter(popupActionsItems));
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Animation fadeInAnimation = AnimationUtils.loadAnimation(arg1.getContext(), android.R.anim.fade_in);
				fadeInAnimation.setDuration(10);
				arg1.startAnimation(fadeInAnimation);
				popupActions.dismiss();
				//String selectedItemText = ((TextView) arg1).getText().toString();
				int id = Integer.parseInt(((TextView) arg1).getTag().toString());
				switch (id) {
					case 0:
						actionSave();
						break;
					case 1:
						actionServerSwitch();
						break;
					case 2:
						actionLabels();
						break;
				}
			}
		});
		popupActions.setFocusable(true);
		popupActions.setWidth(500);
		popupActions.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		popupActions.setContentView(lv);
		
		popupActions.showAsDropDown(btn_list, -5, 0);
	}
	
	private ArrayAdapter<String> actionsAdapter(String[] actionsArray) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, actionsArray) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// setting the ID and text for every items in the list
				String item = getItem(position);
				String[] itemArr = item.split("::");
				String text = itemArr[0];
				String id = itemArr[1];
				// visual settings for the list item
				TextView listItem = new TextView(Activity_GraphView.this);
				listItem.setText(text);
				listItem.setTag(id);
				listItem.setTextSize(22);
				listItem.setPadding(10, 10, 10, 10);
				listItem.setTextColor(Color.WHITE);
				return listItem;
			}
		};
		return adapter;
	}
	
	public void actionLabels() {
		findViewById(R.id.serverSwitch_mask).setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { actionCloseLabels(); } });
		
		findViewById(R.id.serverSwitch_mask).setVisibility(View.VISIBLE);
		findViewById(R.id.labels_container).setVisibility(View.VISIBLE);
		
		refreshLabelsList();
		
		Button b = (Button) findViewById(R.id.addLabelButton);
		final EditText e = (EditText) findViewById(R.id.addLabelEditText);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!e.getText().toString().equals("")) {
					if (muninFoo.addLabel(new Label(e.getText().toString()))) // Si label ajouté : ajout de relation
						muninFoo.getLabel(e.getText().toString()).addPlugin(muninFoo.currentServer.getPlugin(viewFlow.getSelectedItemPosition()));
				}
				e.setText("");
				muninFoo.sqlite.saveLabels();
				refreshLabelsList();
			}
		});
	}
	public void refreshLabelsList() {
		View insertPoint = findViewById(R.id.listviewLabels);
		((LinearLayout)insertPoint).removeAllViews();
		final List<CheckBox> checkboxes = new ArrayList<CheckBox>();
		
		int i = 0;
		for (Label l : muninFoo.labels) {
			LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View v = vi.inflate(R.layout.labels_list_checkbox, null);
			
			checkboxes.add((CheckBox) v.findViewById(R.id.line_0));
			
			v.findViewById(R.id.line).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					CheckBox cb = (CheckBox) v.findViewById(R.id.line_0);
					cb.setChecked(!cb.isChecked());
				}
			});
			
			if (l.contains(muninFoo.currentServer.getPlugin(viewFlow.getSelectedItemPosition())))
				checkboxes.get(i).setChecked(true);
			
			((CheckBox) v.findViewById(R.id.line_0)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// Save
					String labelName = ((TextView)v.findViewById(R.id.line_a)).getText().toString();
					MuninPlugin p = muninFoo.currentServer.getPlugin(viewFlow.getSelectedItemPosition());
					if (isChecked)
						muninFoo.getLabel(labelName).addPlugin(p);
					else {
						muninFoo.getLabel(labelName).removePlugin(p);
						if (muninFoo.getLabel(labelName).plugins.size() == 0) {
							muninFoo.removeLabel(muninFoo.getLabel(labelName));
							refreshLabelsList();
						}
					}
					muninFoo.sqlite.saveLabels();
				}
			});
			
			((TextView)v.findViewById(R.id.line_a)).setText(l.getName());
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
				((CheckBox) v.findViewById(R.id.line_0)).setButtonDrawable(id);
			}
			
			((ViewGroup) insertPoint).addView(v);
			i++;
		}
		if (muninFoo.labels.size() == 0) {
			findViewById(R.id.labels_nolabel).setVisibility(View.VISIBLE);
			findViewById(R.id.listviewLabels).setVisibility(View.GONE);
		}
		else {
			findViewById(R.id.labels_nolabel).setVisibility(View.GONE);
			findViewById(R.id.listviewLabels).setVisibility(View.VISIBLE);
		}
	}
	public void actionCloseLabels() {
		findViewById(R.id.serverSwitch_mask).setVisibility(View.GONE);
		findViewById(R.id.labels_container).setVisibility(View.GONE);
		AlphaAnimation a = new AlphaAnimation(1.0f, 0.0f);
		a.setDuration(300);
		findViewById(R.id.serverSwitch_mask).startAnimation(a);
		findViewById(R.id.labels_container).startAnimation(a);
	}
	
	@SuppressLint("NewApi")
	public void onResume() {
		super.onResume();
		
		Activity_GraphView.load_period = getPref("defaultScale");
		
		// Venant de widget
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("period"))
			Activity_GraphView.load_period = thisIntent.getExtras().getString("period");
		
		
		if (Activity_GraphView.load_period == null || Activity_GraphView.load_period.equals(""))
			Activity_GraphView.load_period = "day";
		
		if (Activity_GraphView.load_period.equals("day"))
			spinner.setSelection(0, true);
		else if (Activity_GraphView.load_period.equals("week"))
			spinner.setSelection(1, true);
		else if (Activity_GraphView.load_period.equals("month"))
			spinner.setSelection(2, true);
		else if (Activity_GraphView.load_period.equals("year"))
			spinner.setSelection(3, true);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (muninFoo.currentServer != null)
				getActionBar().setTitle(muninFoo.currentServer.getName());
		}
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
	
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting())
			return true;
		return false;
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