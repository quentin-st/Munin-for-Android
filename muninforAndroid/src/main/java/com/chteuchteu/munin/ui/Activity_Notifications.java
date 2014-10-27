package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
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

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.Service_Notifications;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressLint("InflateParams")
public class Activity_Notifications extends MuninActivity {
	private CheckBox		cb_notifications;
	private Spinner		sp_refreshRate;
	private CheckBox		cb_wifiOnly;
	private CheckBox       cb_vibrate;

	private LinearLayout	checkboxesView;
	private static CheckBox[]	checkboxes;
	
	private String			currentRefreshRate;
	private static final String[] REFRESH_RATES = {"10", "30", "60", "120", "300", "600", "1440"};
	private static final float PAGE_WEIGHT = 12.25f;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.notifications);
		super.onContentViewSet();
		dh.setDrawerActivity(DrawerHelper.Activity_Notifications);

		actionBar.setTitle(getString(R.string.notificationsTitle));
		
		sp_refreshRate = (Spinner) findViewById(R.id.spinner_refresh);
		cb_notifications = (CheckBox) findViewById(R.id.checkbox_notifications);
		cb_wifiOnly = (CheckBox) findViewById(R.id.checkbox_wifiOnly);
		cb_vibrate = (CheckBox) findViewById(R.id.checkbox_vibrate);
		
		// Refresh rate spinner
		String[] values = getString(R.string.text57).split("/");
		List<String> list = new ArrayList<String>();
		Collections.addAll(list, values);
		
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_refreshRate.setAdapter(dataAdapter);
		
		boolean notificationsEnabled = Util.getPref(context, "notifications").equals("true");
		cb_notifications.setChecked(notificationsEnabled);
		if (!notificationsEnabled)
			findViewById(R.id.notificationsEnabled).setVisibility(View.GONE);
		cb_wifiOnly.setChecked(Util.getPref(context, "notifs_wifiOnly").equals("true"));

		// Check if the device can vibrate
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		cb_vibrate.setEnabled(v.hasVibrator());
		cb_vibrate.setChecked(Util.getPref(context, "notifs_vibrate").equals("true"));
		
		currentRefreshRate = Util.getPref(context, "notifs_refreshRate");
		if (currentRefreshRate.equals(""))
			currentRefreshRate = "60";
		if (currentRefreshRate.equals("10"))		sp_refreshRate.setSelection(0);
		else if (currentRefreshRate.equals("30"))	sp_refreshRate.setSelection(1);
		else if (currentRefreshRate.equals("60"))	sp_refreshRate.setSelection(2);
		else if (currentRefreshRate.equals("120"))	sp_refreshRate.setSelection(3);
		else if (currentRefreshRate.equals("300"))	sp_refreshRate.setSelection(4);
		else if (currentRefreshRate.equals("600"))	sp_refreshRate.setSelection(5);
		else if (currentRefreshRate.equals("1440"))	sp_refreshRate.setSelection(6);
		
		sp_refreshRate.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
				currentRefreshRate = REFRESH_RATES[pos];
				computeEstimatedConsumption();
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) { }
		});
		
		checkboxes = new CheckBox[muninFoo.getOrderedServers().size()];
		
		findViewById(R.id.btn_selectServersToWatch).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				String watchedServers = Util.getPref(context, "notifs_serversList");
				
				ScrollView scrollView = new ScrollView(activity);
				checkboxesView = new LinearLayout(activity);
				checkboxesView.setOrientation(LinearLayout.VERTICAL);
				for (int i=0; i<muninFoo.getOrderedServers().size(); i++) {
					LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View v = vi.inflate(R.layout.servers_list_checkbox, null);
					
					checkboxes[i] = (CheckBox) v.findViewById(R.id.line_0);
					int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
					checkboxes[i].setButtonDrawable(id);
					
					if (watchedServers.contains(muninFoo.getOrderedServers().get(i).getServerUrl()))
						checkboxes[i].setChecked(true);
					
					v.findViewById(R.id.ll_container).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							CheckBox checkbox = (CheckBox) v.findViewById(R.id.line_0);
							checkbox.setChecked(!checkbox.isChecked());
						}
					});
					
					((TextView)v.findViewById(R.id.line_a)).setText(muninFoo.getOrderedServers().get(i).getName());
					((TextView)v.findViewById(R.id.line_b)).setText(muninFoo.getOrderedServers().get(i).getServerUrl());
					
					checkboxesView.addView(v);
				}
				scrollView.addView(checkboxesView);
				
				new AlertDialog.Builder(context)
				.setTitle(R.string.text56)
				.setView(scrollView)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						saveServersListSettings();
						computeEstimatedConsumption();
						dialog.dismiss();
					}
				})
				.show();
			}
		});
		
		cb_notifications.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				View notificationsSettings = activity.findViewById(R.id.notificationsEnabled);
				if (isChecked)
					notificationsSettings.setVisibility(View.VISIBLE);
				else
					notificationsSettings.setVisibility(View.GONE);
			}
		});
	}
	
	private void enableNotifications() {
		if (muninFoo.premium) {
			Util.removePref(context, "lastNotificationText");
			int min = 0;
			if (!Util.getPref(context, "notifs_refreshRate").equals(""))
				min = Integer.parseInt(Util.getPref(context, "notifs_refreshRate"));
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			Intent i = new Intent(this, Service_Notifications.class);
			PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
			am.cancel(pi);
			
			if (min > 0) {
				am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime() + min*60*1000,
						min*60*1000, pi);
			}
		}
	}
	
	private void disableNotifications() {
		Util.removePref(context, "lastNotificationText");
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent i = new Intent(this, Service_Notifications.class);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		am.cancel(pi);
	}
	
	private void saveServersListSettings() {
		String servers = "";
		int i=0;
		for (CheckBox checkbox: checkboxes) {
			if (checkbox.isChecked()) {
				if (i != checkboxes.length - 1)
					servers = servers + muninFoo.getOrderedServers().get(i).getServerUrl() + ";";
				else
					servers = servers + muninFoo.getOrderedServers().get(i).getServerUrl();
			}
			i++;
		}
		Util.setPref(context, "notifs_serversList", servers);
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
	}
	
	private void computeEstimatedConsumption() {
		int refreshRate = Integer.parseInt(currentRefreshRate);
		
		String watchedServers = Util.getPref(context, "notifs_serversList");
		int nbServers = watchedServers.equals("") ? 0 : watchedServers.split(";").length;
		
		double result = (1440/refreshRate) * nbServers * PAGE_WEIGHT;
		String unit = "ko";
		if (result > 1024) {
			result = result / 1024;
			unit = "Mo";
		}
		DecimalFormat df;
		if (unit.equals("ko"))
			df = new DecimalFormat("###");
		else
			df = new DecimalFormat("###");
		((TextView)findViewById(R.id.estimated_data_consumption)).setText(getString(R.string.text54).replace("??", df.format(result) + " " + unit));
	}
	
	private void actionSave() {
		if (muninFoo.premium) {
			// At least one server selected
			boolean ok = false;

			if (checkboxes.length > 0 && checkboxes[0] != null) {
				// Opened at least once servers list
				for (CheckBox checkBox : checkboxes) {
					if (checkBox.isChecked()) {
						ok = true;
						break;
					}
				}
			} else {
				// Check from pref string
				int length = Util.getPref(context, "notifs_serversList").length();
				if (length > 2) // != "" && != ";"
					ok = true;
			}

			if (ok) {
				if (cb_notifications.isChecked()) {
					Util.setPref(context, "notifications", "true");
					Util.setPref(context, "notifs_wifiOnly", String.valueOf(cb_wifiOnly.isChecked()));
					Util.setPref(context, "notifs_vibrate", String.valueOf(cb_vibrate.isChecked()));
					Util.setPref(context, "notifs_refreshRate", REFRESH_RATES[sp_refreshRate.getSelectedItemPosition()]);
					enableNotifications();
				} else {
					Util.setPref(context, "notifications", "false");
					Util.removePref(context, "notifs_wifiOnly");
					Util.removePref(context, "notifs_refreshRate");
					Util.removePref(context, "notifs_vibrate");
					disableNotifications();
				}
				Toast.makeText(context, R.string.text36, Toast.LENGTH_SHORT).show();
			} else
				Toast.makeText(context, R.string.text56, Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_save:
				actionSave();
				return true;
		}

		return true;
	}
	
	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.notifications, menu);
	}
}