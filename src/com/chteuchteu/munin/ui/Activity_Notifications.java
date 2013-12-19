package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.Service_Notifications;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Activity_Notifications extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	
	private Menu 			menu;
	private String			activityName;
	public static CheckBox[]	checkboxes;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		setContentView(R.layout.notifications);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getString(R.string.notificationsTitle));
			
			((TextView)findViewById(R.id.viewTitle)).setVisibility(View.GONE);
			((LinearLayout)findViewById(R.id.viewTitleSep)).setVisibility(View.GONE);
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_Notifications);
			}
		} else {
			this.getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.grayBackground));
		}
		
		final Spinner sp_refreshRate = 		(Spinner) findViewById(R.id.spinner_refresh);
		final CheckBox cb_notifications = 	(CheckBox) findViewById(R.id.checkbox_notifications);
		final CheckBox cb_wifiOnly = 		(CheckBox) findViewById(R.id.checkbox_wifiOnly);
		TextView btn_servers2watch = 		(TextView) findViewById(R.id.btn_selectServersToWatch);
		
		
		// Remplissage spinner
		String[] values = getString(R.string.text57).split("/");
		List<String> list = new ArrayList<String>();
		for (String v: values) {
			list.add(v);
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_refreshRate.setAdapter(dataAdapter);
		
		
		// A partir des paramètres
		if (getPref("notifications").equals("true"))			cb_notifications.setChecked(true);
		if (getPref("notifs_wifiOnly").equals("true"))			cb_wifiOnly.setChecked(true);
		
		if (getPref("notifs_refreshRate").equals("10"))			sp_refreshRate.setSelection(0);
		else if (getPref("notifs_refreshRate").equals("30"))	sp_refreshRate.setSelection(1);
		else if (getPref("notifs_refreshRate").equals("60"))	sp_refreshRate.setSelection(2);
		else if (getPref("notifs_refreshRate").equals("120"))	sp_refreshRate.setSelection(3);
		else if (getPref("notifs_refreshRate").equals("300"))	sp_refreshRate.setSelection(4);
		else if (getPref("notifs_refreshRate").equals("600"))	sp_refreshRate.setSelection(5);
		else if (getPref("notifs_refreshRate").equals("1440"))	sp_refreshRate.setSelection(6);
		else													sp_refreshRate.setSelection(2);
		
		
		cb_notifications.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
				if (isChecked) {
					setPref("notifications", "true");
					enableNotifications();
				} else {
					setPref("notifications", "false");
					disableNotifications();
				}
				
				if (cb_wifiOnly.isChecked())	setPref("notifs_wifiOnly", "true");
				else							setPref("notifs_wifiOnly", "false");
				
				String[] values = {"10", "30", "60", "120", "300", "600", "1440"};
				if (sp_refreshRate.getSelectedItemPosition() >= 0 && sp_refreshRate.getSelectedItemPosition() < values.length)
					setPref("notifs_refreshRate", values[sp_refreshRate.getSelectedItemPosition()]);
				computeEstimatedConsumption();
			}
		});
		cb_wifiOnly.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
				if (isChecked)
					setPref("notifs_wifiOnly", "true");
				else
					setPref("notifs_wifiOnly", "false");
			}
		});
		sp_refreshRate.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
				String[] values = {"10", "30", "60", "120", "300", "600", "1440"};
				if (pos >= 0 && pos < values.length)
					setPref("notifs_refreshRate", values[pos]);
				computeEstimatedConsumption();
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		
		checkboxes = new CheckBox[muninFoo.getOrderedServers().size()];
		
		String watchedServers = "";
		if (!getPref("notifs_serversList").equals(""))
			watchedServers = getPref("notifs_serversList");
		
		for (int i=0; i<muninFoo.getOrderedServers().size(); i++) {
			LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = vi.inflate(R.layout.servers_list_checkbox, null);
			
			checkboxes[i] = (CheckBox) v.findViewById(R.id.line_0);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
				checkboxes[i].setButtonDrawable(id);
			}
			
			if (watchedServers.contains(muninFoo.getOrderedServers().get(i).getServerUrl()))
				checkboxes[i].setChecked(true);
			
			v.findViewById(R.id.ll_container).setOnClickListener(new OnClickListener() { @Override public void onClick(View v) { ((CheckBox) v.findViewById(R.id.line_0)).setChecked(!((CheckBox) v.findViewById(R.id.line_0)).isChecked()); } });
			
			((TextView)v.findViewById(R.id.line_a)).setText(muninFoo.getOrderedServers().get(i).getName());
			((TextView)v.findViewById(R.id.line_b)).setText(muninFoo.getOrderedServers().get(i).getServerUrl());
			
			View insertPoint = findViewById(R.id.list_container);
			((ViewGroup) insertPoint).addView(v);
		}
		
		btn_servers2watch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				changeListViewVisibility(true);
			}
		});
	}
	
	@SuppressLint("NewApi")
	public void changeListViewVisibility(boolean toBeShown) {
		if (toBeShown) {
			((LinearLayout)findViewById(R.id.list_container)).setVisibility(View.VISIBLE);
			((ScrollView)findViewById(R.id.list_scrollview)).setVisibility(View.VISIBLE);
			((LinearLayout)findViewById(R.id.view1)).setVisibility(View.GONE);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				getActionBar().setTitle(getString(R.string.text56));
		} else {
			((LinearLayout)findViewById(R.id.list_container)).setVisibility(View.GONE);
			((ScrollView)findViewById(R.id.list_scrollview)).setVisibility(View.GONE);
			((LinearLayout)findViewById(R.id.view1)).setVisibility(View.VISIBLE);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				getActionBar().setTitle(getString(R.string.notificationsTitle));
			computeEstimatedConsumption();
		}
	}
	
	public void enableNotifications() {
		if (muninFoo.premium) {
			setPref("lastNotificationText", "");
			int min = 0;
			if (!getPref("notifs_refreshRate").equals(""))
				min = Integer.parseInt(getPref("notifs_refreshRate"));
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			Intent i = new Intent(this, Service_Notifications.class);
			PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
			am.cancel(pi);
			// by my own convention, minutes <= 0 means notifications are disabled
			if (min > 0) {
				am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime() + min*60*1000,
						min*60*1000, pi);
			}
		}
	}
	
	public void disableNotifications() {
		setPref("lastNotificationText", "");
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent i = new Intent(this, Service_Notifications.class);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		am.cancel(pi);
	}
	
	public void saveServersListSettings() {
		String servers = "";
		int i=0;
		for (CheckBox c: checkboxes) {
			if (c.isChecked()) {
				if (i != checkboxes.length - 1)
					servers = servers + muninFoo.getOrderedServers().get(i).getServerUrl() + ";";
				else
					servers = servers + muninFoo.getOrderedServers().get(i).getServerUrl();
			}
			i++;
		}
		setPref("notifs_serversList", servers);
		Toast.makeText(getApplicationContext(), getString(R.string.text36), Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onBackPressed() {
		if (findViewById(R.id.list_container).getVisibility() == View.VISIBLE) {
			changeListViewVisibility(false);
			saveServersListSettings();
		} else {
			Intent intent = new Intent(this, Activity_Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			setTransition("shallower");
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Calcul de la consommation estimée:
		computeEstimatedConsumption();
	}
	
	public void computeEstimatedConsumption() {
		//double pageWeight = 29.5;
		double pageWeight = 12.25;
		int refreshRate;
		if (!getPref("notifs_refreshRate").equals(""))
			refreshRate = Integer.parseInt(getPref("notifs_refreshRate"));
		else
			refreshRate = 60;
		int nbServers = 0;
		for (CheckBox c: checkboxes) {
			if (c.isChecked())
				nbServers++;
		}
		double result = (1440/refreshRate) * nbServers * pageWeight;
		String unit = "ko";
		if (result > 1024) {
			result = result / 1024;
			unit = "Mo";
		}
		DecimalFormat df;
		if (unit.equals("ko"))
			df = new DecimalFormat("###.#");
		else
			df = new DecimalFormat("###.##");
		((TextView)findViewById(R.id.estimated_data_consumption)).setText(getString(R.string.text54).replace("??", df.format(result) + " " + unit));
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				if (findViewById(R.id.list_container).getVisibility() == View.VISIBLE) {
					changeListViewVisibility(false);
					saveServersListSettings();
				} else {
					if (muninFoo.drawer)
						dh.getDrawer().toggle(true);
					else {
						Intent intent = new Intent(this, Activity_Main.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
						setTransition("shallower");
					}
					return true;
				}
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Notifications.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Notifications.this, Activity_About.class));
				setTransition("deeper");
				return true;
			default:
				return super.onOptionsItemSelected(item);
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