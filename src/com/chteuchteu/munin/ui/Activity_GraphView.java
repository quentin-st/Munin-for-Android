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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.GraphView_Adapter;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.obj.MuninServer;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class Activity_GraphView extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh = null;
	private	int				previousPos = -1;
	private Context			c;
	
	public static Period	load_period;
	public static ViewFlow	viewFlow;
	public static int		position;
	public static Bitmap[]	bitmaps;
	
	private Spinner 		spinner;
	private MenuItem		item_previous;
	private MenuItem		item_next;
	private Menu 			menu;
	private String			activityName;
	
	private Handler			mHandler;
	private Runnable		mHandlerTask;
	
	
	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		c = this;
		// Entry point: widgets
		Crashlytics.start(this);
		
		if (Util.getPref(this, "graphview_orientation").equals("vertical"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else if (Util.getPref(this, "graphview_orientation").equals("horizontal"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		if (Util.getPref(this, "screenAlwaysOn").equals("true"))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.graphview);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		if (muninFoo.currentServer != null)
			actionBar.setTitle(muninFoo.currentServer.getName());
		
		if (muninFoo.drawer) {
			dh = new DrawerHelper(this, muninFoo);
			dh.setDrawerActivity(dh.Activity_GraphView);
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
		
		// Coming from widget
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null
				&& thisIntent.getExtras().containsKey("server")
				&& thisIntent.getExtras().containsKey("plugin")
				&& thisIntent.getExtras().containsKey("period")) {
			String server = thisIntent.getExtras().getString("server");
			String plugin = thisIntent.getExtras().getString("plugin");
			String period = thisIntent.getExtras().getString("period");
			// Setting currentServer
			muninFoo.currentServer = muninFoo.getServer(server);
			if (muninFoo.currentServer == null)
				muninFoo.currentServer = muninFoo.getFirstServer();
			
			// Giving position of plugin in list to GraphView
			for (int i=0; i<muninFoo.currentServer.getPlugins().size(); i++) {
				if (muninFoo.currentServer.getPlugins().get(i) != null && muninFoo.currentServer.getPlugins().get(i).getName().equals(plugin))
					thisIntent.putExtra("position", "" + i);
			}
			
			if (period.equals("day"))		spinner.setSelection(0);
			else if (period.equals("week"))	spinner.setSelection(1);
			else if (period.equals("month"))	spinner.setSelection(2);
			else if (period.equals("year"))	spinner.setSelection(3);
			else							spinner.setSelection(0);
		}
		
		if (muninFoo.currentServer == null)
			muninFoo.currentServer = muninFoo.getFirstServer();
		
		int pos = 0;
		
		// Coming from Grid
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("plugin")) {
			int i = 0;
			for (MuninPlugin p : muninFoo.currentServer.getPlugins()) {
				if (p.getName().equals(thisIntent.getExtras().getString("plugin"))) {
					pos = i; break;
				}
				i++;
			}
		}
		
		// Coming from PluginSelection or if orientation changed
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("position")) {
			String position = thisIntent.getExtras().getString("position");
			pos = Integer.parseInt(position);
		}/* else { // Vérification de l'intent ratée: redirection
			Intent intent2 = new Intent(Activity_GraphView.this, Activity_PluginSelection.class);
			startActivity(intent2);
		}*/
		
		if (savedInstanceState != null)
			pos = savedInstanceState.getInt("position");
		
		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
				bitmaps = new Bitmap[muninFoo.currentServer.getPlugins().size()];
				if (position == 0)	Activity_GraphView.load_period = Period.DAY;
				else if (position == 1)	Activity_GraphView.load_period = Period.WEEK;
				else if (position == 2)	Activity_GraphView.load_period = Period.MONTH;
				else if (position == 3)	Activity_GraphView.load_period = Period.YEAR;
				else 					Activity_GraphView.load_period = Period.DAY;
				
				if (viewFlow != null) // Update Viewflow
					viewFlow.setSelection(viewFlow.getSelectedItemPosition());
			}
			public void onNothingSelected(AdapterView<?> parentView) { }
		});
		
		// Viewflow
		position = pos;
		bitmaps = new Bitmap[muninFoo.currentServer.getPlugins().size()];
		viewFlow = (ViewFlow) findViewById(R.id.viewflow);
		GraphView_Adapter adapter = new GraphView_Adapter(this);
		viewFlow.setAdapter(adapter, pos);
		viewFlow.setAnimationEnabled(Util.getPref(this, "transitions").equals("true"));
		TitleFlowIndicator indicator = (TitleFlowIndicator) findViewById(R.id.viewflowindic);
		indicator.setTitleProvider(adapter);
		viewFlow.setFlowIndicator(indicator);
		
		if (dh != null) {
			dh.setViewFlow(viewFlow);
			dh.initPluginsList();
		}
		
		viewFlow.setOnViewSwitchListener(new ViewSwitchListener() {
			public void onSwitched(View v, int position) {
				Activity_GraphView.position = position;
				
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
		
		if (!Util.isOnline(this))
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
		
		// Launch periodical check
		if (Util.getPref(this, "autoRefresh").equals("true")) {
			mHandler = new Handler();
			final int INTERVAL = 1000 * 60 * 5;
			mHandlerTask = new Runnable() {
				@Override 
				public void run() {
					actionRefresh();
					mHandler.postDelayed(mHandlerTask, INTERVAL);
				}
			};
			mHandlerTask.run();
		}
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
		}
		if (viewFlow.getSelectedItemPosition() == muninFoo.currentServer.getPlugins().size()-1) {
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
								Util.setTransition(c, TransitionStyle.SHALLOWER);
							}
						} else if (from.equals("alerts")) {
							if (thisIntent.getExtras().containsKey("server")) {
								if (muninFoo.getServer(thisIntent.getExtras().getString("server")) != null)
									muninFoo.currentServer = muninFoo.getServer(thisIntent.getExtras().getString("server"));
								Intent intent = new Intent(Activity_GraphView.this, Activity_AlertsPluginSelection.class);
								startActivity(intent);
								Util.setTransition(c, TransitionStyle.SHALLOWER);
							}
						}
					} else {
						Intent intent = new Intent(this, Activity_PluginSelection.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
						Util.setTransition(c, TransitionStyle.SHALLOWER);
					}
				}
				return true;
			case R.id.menu_previous:	actionPrevious();		return true;
			case R.id.menu_next:		actionNext();			return true;
			case R.id.menu_refresh:		actionRefresh(); 		return true;
			case R.id.menu_save:		actionSave();			return true;
			case R.id.menu_switchServer:actionServerSwitch();	return true;
			case R.id.menu_labels:
				actionLabels();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_GraphView.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_GraphView.this, Activity_About.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			default:	return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onBackPressed() {
		//recycleBitmaps();
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("from")) {
			String from = thisIntent.getExtras().getString("from");
			if (from.equals("labels")) {
				if (thisIntent.getExtras().containsKey("label")) {
					Intent intent = new Intent(Activity_GraphView.this, Activity_LabelsPluginSelection.class);
					intent.putExtra("label", thisIntent.getExtras().getString("label"));
					startActivity(intent);
					Util.setTransition(c, TransitionStyle.SHALLOWER);
				}
			} else if (from.equals("alerts")) {
				if (thisIntent.getExtras().containsKey("server")) {
					if (muninFoo.getServer(thisIntent.getExtras().getString("server")) != null)
						muninFoo.currentServer = muninFoo.getServer(thisIntent.getExtras().getString("server"));
					Intent intent = new Intent(Activity_GraphView.this, Activity_AlertsPluginSelection.class);
					startActivity(intent);
					Util.setTransition(c, TransitionStyle.SHALLOWER);
				}
			} else if (from.equals("grid")) {
				if (thisIntent.getExtras().containsKey("fromGrid")) {
					Intent intent = new Intent(Activity_GraphView.this, Activity_Grid.class);
					intent.putExtra("gridName", thisIntent.getExtras().getString("fromGrid"));
					startActivity(intent);
					Util.setTransition(c, TransitionStyle.SHALLOWER);
				} else {
					startActivity(new Intent(Activity_GraphView.this, Activity_GridSelection.class));
					Util.setTransition(c, TransitionStyle.SHALLOWER);
				}
			}
		} else {
			Intent intent = new Intent(this, Activity_PluginSelection.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(c, TransitionStyle.SHALLOWER);
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
					Util.setTransition(c, TransitionStyle.DEEPER);
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
		
		if (viewFlow.getSelectedItemPosition() != 0)
			viewFlow.setSelection(viewFlow.getSelectedItemPosition() - 1);
	}
	public void actionNext() {
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
	
	public void actionAddLabel() {
		final LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(10, 30, 10, 10);
		final EditText input = new EditText(this);
		ll.addView(input);
		
		new AlertDialog.Builder(Activity_GraphView.this)
		.setTitle(getText(R.string.text70_2))
		.setView(ll)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (!value.equals(""))
					muninFoo.addLabel(new Label(value));
				dialog.dismiss();
				actionLabels();
			}
		}).setNegativeButton(getText(R.string.text64), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) { }
		}).show();
	}
	
	public void actionLabels() {
		final CharSequence[] items = new CharSequence[muninFoo.labels.size()];
		for (int i=0; i<muninFoo.labels.size(); i++)
			items[i] = muninFoo.labels.get(i).getName();
		
		LinearLayout checkboxesContainer = new LinearLayout(this);
		checkboxesContainer.setPadding(10, 10, 10, 10);
		checkboxesContainer.setOrientation(LinearLayout.VERTICAL);
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
			
			if (l.contains(muninFoo.currentServer.getPlugin(viewFlow.getSelectedItemPosition())))	checkboxes.get(i).setChecked(true);
			
			((CheckBox) v.findViewById(R.id.line_0)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// Save
					String labelName = ((TextView)v.findViewById(R.id.line_a)).getText().toString();
					MuninPlugin p = muninFoo.currentServer.getPlugin(viewFlow.getSelectedItemPosition());
					if (isChecked)
						muninFoo.getLabel(labelName).addPlugin(p);
					else
						muninFoo.getLabel(labelName).removePlugin(p);
					
					muninFoo.sqlite.saveLabels();
				}
			});
			
			((TextView)v.findViewById(R.id.line_a)).setText(l.getName());
			
			int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
			((CheckBox) v.findViewById(R.id.line_0)).setButtonDrawable(id);
			
			checkboxesContainer.addView(v);
			i++;
		}
		if (muninFoo.labels.size() == 0) {
			TextView tv = new TextView(this);
			tv.setText(getText(R.string.text62));
			tv.setTextSize(18f);
			tv.setPadding(20, 20, 0, 0);
			checkboxesContainer.addView(tv);
		}
		
		AlertDialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getText(R.string.button_labels));
		builder.setView(checkboxesContainer)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// OK
				dialog.dismiss();
			}
		})
		.setNeutralButton(getText(R.string.text70_2), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Add a label
				dialog.dismiss();
				actionAddLabel();
			}
		})
		.setNegativeButton(getText(R.string.text64), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// Cancel
				dialog.dismiss();
			}
		});
		dialog = builder.create();
		dialog.show();
	}
	
	@SuppressLint("NewApi")
	public void onResume() {
		super.onResume();
		
		Activity_GraphView.load_period = Period.get(Util.getPref(this, "defaultScale"));
		
		// Venant de widget
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("period"))
			Activity_GraphView.load_period = Period.get(thisIntent.getExtras().getString("period"));
		
		if (Activity_GraphView.load_period == null)
			Activity_GraphView.load_period = Period.DAY;
		
		if (Activity_GraphView.load_period == Period.DAY)
			spinner.setSelection(0, true);
		else if (Activity_GraphView.load_period == Period.WEEK)
			spinner.setSelection(1, true);
		else if (Activity_GraphView.load_period == Period.MONTH)
			spinner.setSelection(2, true);
		else if (Activity_GraphView.load_period == Period.YEAR)
			spinner.setSelection(3, true);
		
		if (muninFoo.currentServer != null)
			getActionBar().setTitle(muninFoo.currentServer.getName());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!MuninFoo.debug)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!MuninFoo.debug)
			EasyTracker.getInstance(this).activityStop(this);
		
		if (Util.getPref(this, "screenAlwaysOn").equals("true"))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}