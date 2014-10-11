package com.chteuchteu.munin.ui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.Service_Notifications;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;

@SuppressLint("InflateParams")
public class Activity_Notifications extends Activity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private Context			c;
	
	private Menu 			menu;
	private String			activityName;
	public static CheckBox[]	checkboxes;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		c = this;
		setContentView(R.layout.notifications);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(getString(R.string.notificationsTitle));
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(dh.Activity_Notifications);
		
		Util.UI.applySwag(this);
		
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
		if (Util.getPref(c, "notifications").equals("true"))			cb_notifications.setChecked(true);
		if (Util.getPref(c, "notifs_wifiOnly").equals("true"))			cb_wifiOnly.setChecked(true);
		
		if (Util.getPref(c, "notifs_refreshRate").equals("10"))			sp_refreshRate.setSelection(0);
		else if (Util.getPref(c, "notifs_refreshRate").equals("30"))	sp_refreshRate.setSelection(1);
		else if (Util.getPref(c, "notifs_refreshRate").equals("60"))	sp_refreshRate.setSelection(2);
		else if (Util.getPref(c, "notifs_refreshRate").equals("120"))	sp_refreshRate.setSelection(3);
		else if (Util.getPref(c, "notifs_refreshRate").equals("300"))	sp_refreshRate.setSelection(4);
		else if (Util.getPref(c, "notifs_refreshRate").equals("600"))	sp_refreshRate.setSelection(5);
		else if (Util.getPref(c, "notifs_refreshRate").equals("1440"))	sp_refreshRate.setSelection(6);
		else													sp_refreshRate.setSelection(2);
		
		
		cb_notifications.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
				if (isChecked) {
					Util.setPref(c, "notifications", "true");
					enableNotifications();
				} else {
					Util.setPref(c, "notifications", "false");
					disableNotifications();
				}
				
				if (cb_wifiOnly.isChecked())	Util.setPref(c, "notifs_wifiOnly", "true");
				else							Util.setPref(c, "notifs_wifiOnly", "false");
				
				String[] values = {"10", "30", "60", "120", "300", "600", "1440"};
				if (sp_refreshRate.getSelectedItemPosition() >= 0 && sp_refreshRate.getSelectedItemPosition() < values.length)
					Util.setPref(c, "notifs_refreshRate", values[sp_refreshRate.getSelectedItemPosition()]);
				computeEstimatedConsumption();
			}
		});
		cb_wifiOnly.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
				if (isChecked)
					Util.setPref(c, "notifs_wifiOnly", "true");
				else
					Util.setPref(c, "notifs_wifiOnly", "false");
			}
		});
		sp_refreshRate.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
				String[] values = {"10", "30", "60", "120", "300", "600", "1440"};
				if (pos >= 0 && pos < values.length)
					Util.setPref(c, "notifs_refreshRate", values[pos]);
				computeEstimatedConsumption();
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		
		checkboxes = new CheckBox[muninFoo.getOrderedServers().size()];
		
		String watchedServers = "";
		if (!Util.getPref(c, "notifs_serversList").equals(""))
			watchedServers = Util.getPref(c, "notifs_serversList");
		
		for (int i=0; i<muninFoo.getOrderedServers().size(); i++) {
			LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = vi.inflate(R.layout.servers_list_checkbox, null);
			
			checkboxes[i] = (CheckBox) v.findViewById(R.id.line_0);
			int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
			checkboxes[i].setButtonDrawable(id);
			
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
	
	public void changeListViewVisibility(boolean toBeShown) {
		if (toBeShown) {
			findViewById(R.id.list_container).setVisibility(View.VISIBLE);
			findViewById(R.id.list_scrollview).setVisibility(View.VISIBLE);
			findViewById(R.id.view1).setVisibility(View.GONE);
			getActionBar().setTitle(getString(R.string.text56));
		} else {
			findViewById(R.id.list_container).setVisibility(View.GONE);
			findViewById(R.id.list_scrollview).setVisibility(View.GONE);
			findViewById(R.id.view1).setVisibility(View.VISIBLE);
			getActionBar().setTitle(getString(R.string.notificationsTitle));
			computeEstimatedConsumption();
		}
	}

	public void enableNotifications() {
		if (muninFoo.premium) {
			Util.setPref(c, "lastNotificationText", "");
			int min = 0;
			if (!Util.getPref(c, "notifs_refreshRate").equals(""))
				min = Integer.parseInt(Util.getPref(c, "notifs_refreshRate"));
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
		Util.setPref(c, "lastNotificationText", "");
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
		Util.setPref(c, "notifs_serversList", servers);
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
			Util.setTransition(c, TransitionStyle.SHALLOWER);
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
		if (!Util.getPref(c, "notifs_refreshRate").equals(""))
			refreshRate = Integer.parseInt(Util.getPref(c, "notifs_refreshRate"));
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
				} else
					dh.getDrawer().toggle(true);
				
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Notifications.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Notifications.this, Activity_About.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
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