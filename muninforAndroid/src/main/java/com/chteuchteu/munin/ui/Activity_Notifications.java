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
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
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

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.ntfs.Service_Notifications;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninNode;

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

		setContentView(R.layout.activity_notifications);
		super.onContentViewSet();
		dh.setDrawerActivity(this);

		actionBar.setTitle(getString(R.string.notificationsTitle));
		
		sp_refreshRate = (Spinner) findViewById(R.id.spinner_refresh);
		cb_notifications = (CheckBox) findViewById(R.id.checkbox_notifications);
		cb_wifiOnly = (CheckBox) findViewById(R.id.checkbox_wifiOnly);
		cb_vibrate = (CheckBox) findViewById(R.id.checkbox_vibrate);
		
		// Refresh rate spinner
		String[] values = getString(R.string.text57).split("/");
		List<String> list = new ArrayList<>();
		Collections.addAll(list, values);
		
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_refreshRate.setAdapter(dataAdapter);
		
		boolean notificationsEnabled = Util.getPref(context, Util.PrefKeys.Notifications).equals("true");
		cb_notifications.setChecked(notificationsEnabled);
		if (!notificationsEnabled)
			findViewById(R.id.notificationsEnabled).setVisibility(View.GONE);
		cb_wifiOnly.setChecked(Util.getPref(context, Util.PrefKeys.Notifs_WifiOnly).equals("true"));

		// Check if the device can vibrate
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		cb_vibrate.setEnabled(v.hasVibrator());
		cb_vibrate.setChecked(Util.getPref(context, Util.PrefKeys.Notifs_Vibrate).equals("true"));
		
		currentRefreshRate = Util.getPref(context, Util.PrefKeys.Notifs_RefreshRate);
		if (currentRefreshRate.equals(""))
			currentRefreshRate = "60";
        switch (currentRefreshRate) {
            case "10":
                sp_refreshRate.setSelection(0);
                break;
            case "30":
                sp_refreshRate.setSelection(1);
                break;
            case "60":
                sp_refreshRate.setSelection(2);
                break;
            case "120":
                sp_refreshRate.setSelection(3);
                break;
            case "300":
                sp_refreshRate.setSelection(4);
                break;
            case "600":
                sp_refreshRate.setSelection(5);
                break;
            case "1440":
                sp_refreshRate.setSelection(6);
                break;
        }
		
		sp_refreshRate.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
				currentRefreshRate = REFRESH_RATES[pos];
				computeEstimatedConsumption();
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) { }
		});
		
		checkboxes = new CheckBox[muninFoo.getNodes().size()];
		
		findViewById(R.id.btn_selectServersToWatch).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				String watchedNodes = Util.getPref(context, Util.PrefKeys.Notifs_NodesList);
				
				ScrollView scrollView = new ScrollView(activity);
				checkboxesView = new LinearLayout(activity);
				checkboxesView.setOrientation(LinearLayout.VERTICAL);
				for (int i=0; i<muninFoo.getNodes().size(); i++) {
                    MuninNode node = muninFoo.getNodes().get(i);

					LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View v = vi.inflate(R.layout.servers_list_checkbox, null);
					
					checkboxes[i] = (CheckBox) v.findViewById(R.id.line_0);
					int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
					checkboxes[i].setButtonDrawable(id);
					
					if (watchedNodes.contains(node.getUrl()))
						checkboxes[i].setChecked(true);
					
					v.findViewById(R.id.ll_container).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							CheckBox checkbox = (CheckBox) v.findViewById(R.id.line_0);
							checkbox.setChecked(!checkbox.isChecked());
						}
					});
					
					((TextView)v.findViewById(R.id.line_a)).setText(node.getName());
					((TextView)v.findViewById(R.id.line_b)).setText(node.getParent().getName());
					
					checkboxesView.addView(v);
				}
				scrollView.addView(checkboxesView);
				
				new AlertDialog.Builder(context)
				.setTitle(R.string.text56)
				.setView(scrollView)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						saveNodesListSettings();
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

		// Since we manually defined the checkbox and text
		// (so the checkbox can be at the right and still have the view tinting introduced
		// on Android 5.0), we have to manually define the onclick listener on the label
		for (View view : Util.getViewsByTag((ViewGroup)findViewById(R.id.container), "checkable")) {
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					ViewGroup row = (ViewGroup) view;
					CheckBox checkBox = (CheckBox) Util.getChild(row, AppCompatCheckBox.class);
					if (checkBox != null)
						checkBox.setChecked(!checkBox.isChecked());
				}
			});
		}
	}
	
	private void enableNotifications() {
		if (muninFoo.premium) {
			Util.removePref(context, Util.PrefKeys.Notifs_LastNotificationText);
			int min = 0;
			if (!Util.getPref(context, Util.PrefKeys.Notifs_RefreshRate).equals(""))
				min = Integer.parseInt(Util.getPref(context, Util.PrefKeys.Notifs_RefreshRate));
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
		Util.removePref(context, Util.PrefKeys.Notifs_LastNotificationText);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent i = new Intent(this, Service_Notifications.class);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		am.cancel(pi);
	}
	
	private void saveNodesListSettings() {
		String nodes = "";
		int i=0;
		for (CheckBox checkbox: checkboxes) {
			if (checkbox.isChecked()) {
				if (i != checkboxes.length - 1)
					nodes = nodes + muninFoo.getNodes().get(i).getUrl() + ";";
				else
					nodes = nodes + muninFoo.getNodes().get(i).getUrl();
			}
			i++;
		}
		Util.setPref(context, Util.PrefKeys.Notifs_NodesList, nodes);
	}
	
	@Override
	public void onBackPressed() {
        if (dh.closeDrawerIfOpen())
            return;

        Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
	}
	
	private void computeEstimatedConsumption() {
		int refreshRate = Integer.parseInt(currentRefreshRate);
		
		String watchedNodes = Util.getPref(context, Util.PrefKeys.Notifs_NodesList);
		int nbNodes = watchedNodes.equals("") ? 0 : watchedNodes.split(";").length;
		
		double result = (1440/refreshRate) * nbNodes * PAGE_WEIGHT;
		String unit = "kb";
		if (result > 1024) {
			result = result / 1024;
			unit = "Mb";
		}
		DecimalFormat df = new DecimalFormat("###");
		((TextView)findViewById(R.id.estimated_data_consumption)).setText(getString(R.string.text54).replace("??", df.format(result) + " " + unit));
	}
	
	private void actionSave() {
		if (muninFoo.premium) {
			// At least one node selected
			boolean ok = false;

			// If notifications disabled : ok = true
			if (!cb_notifications.isChecked())
				ok = true;
			else {
				if (checkboxes.length > 0 && checkboxes[0] != null) {
					// Opened at least once nodes list
					for (CheckBox checkBox : checkboxes) {
						if (checkBox.isChecked()) {
							ok = true;
							break;
						}
					}
				} else {
					// Check from pref string
					int length = Util.getPref(context, Util.PrefKeys.Notifs_NodesList).length();
					if (length > 2) // != "" && != ";"
						ok = true;
				}
			}

			if (ok) {
				if (cb_notifications.isChecked()) {
					Util.setPref(context, Util.PrefKeys.Notifications, "true");
					Util.setPref(context, Util.PrefKeys.Notifs_WifiOnly, String.valueOf(cb_wifiOnly.isChecked()));
					Util.setPref(context, Util.PrefKeys.Notifs_Vibrate, String.valueOf(cb_vibrate.isChecked()));
					Util.setPref(context, Util.PrefKeys.Notifs_RefreshRate, REFRESH_RATES[sp_refreshRate.getSelectedItemPosition()]);
					enableNotifications();
				} else {
					Util.setPref(context, Util.PrefKeys.Notifications, "false");
					Util.removePref(context, Util.PrefKeys.Notifs_WifiOnly);
					Util.removePref(context, Util.PrefKeys.Notifs_RefreshRate);
					Util.removePref(context, Util.PrefKeys.Notifs_Vibrate);
					disableNotifications();
				}
				Toast.makeText(context, R.string.text36, Toast.LENGTH_SHORT).show();
			} else
				Toast.makeText(context, R.string.text56, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Notifications; }
	
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