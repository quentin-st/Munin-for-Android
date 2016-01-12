package com.chteuchteu.munin.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.ntfs.poll.Service_PollNotifications;
import com.chteuchteu.munin.obj.MuninNode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Fragment_Notifications_Poll extends Fragment implements INotificationsFragment {
	private View        view;
	private Context     context;
	private MuninFoo    muninFoo;
	private Settings    settings;

	private CheckBox	cb_notifications;
	private Spinner     sp_refreshRate;
	private CheckBox	cb_wifiOnly;
	private CheckBox    cb_vibrate;

	private LinearLayout checkboxesView;
	private static CheckBox[] checkboxes;

	private int		    currentRefreshRate;
	private static final int[] REFRESH_RATES = {10, 30, 60, 120, 300, 600, 1440};
	private static final float PAGE_WEIGHT = 12.25f;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		this.context = context;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_notifications_poll, container, false);
		muninFoo = MuninFoo.getInstance(context);
		settings = muninFoo.getSettings();

		sp_refreshRate = (Spinner) view.findViewById(R.id.spinner_refresh);
		cb_notifications = (CheckBox) view.findViewById(R.id.checkbox_notifications);
		cb_wifiOnly = (CheckBox) view.findViewById(R.id.checkbox_wifiOnly);
		cb_vibrate = (CheckBox) view.findViewById(R.id.checkbox_vibrate);

		// Refresh rate spinner
		String[] values = getString(R.string.text57).split("/");
		List<String> list = new ArrayList<>();
		Collections.addAll(list, values);

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_refreshRate.setAdapter(dataAdapter);

		boolean notificationsEnabled = settings.getBool(Settings.PrefKeys.Notifs_Poll, false);
		cb_notifications.setChecked(notificationsEnabled);
		if (!notificationsEnabled)
			view.findViewById(R.id.notificationsEnabled).setVisibility(View.GONE);
		cb_wifiOnly.setChecked(settings.getBool(Settings.PrefKeys.Notifs_Poll_WifiOnly));

		// Check if the device can vibrate
		Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		cb_vibrate.setEnabled(vibrator.hasVibrator());
		cb_vibrate.setChecked(settings.getBool(Settings.PrefKeys.Notifs_Poll_Vibrate));

		currentRefreshRate = settings.getInt(Settings.PrefKeys.Notifs_Poll_RefreshRate);
		if (currentRefreshRate == -1)
			currentRefreshRate = 60;

		for (int i=0; i<REFRESH_RATES.length; i++) {
			if (REFRESH_RATES[i] == currentRefreshRate) {
				sp_refreshRate.setSelection(i);
				break;
			}
		}

		sp_refreshRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
				currentRefreshRate = REFRESH_RATES[pos];
				computeEstimatedConsumption();
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) { }
		});

		checkboxes = new CheckBox[muninFoo.getNodes().size()];

		view.findViewById(R.id.btn_selectServersToWatch).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String watchedNodes = settings.getString(Settings.PrefKeys.Notifs_Poll_NodesList);

				ScrollView scrollView = new ScrollView(context);
				checkboxesView = new LinearLayout(context);
				checkboxesView.setOrientation(LinearLayout.VERTICAL);
				for (int i=0; i<muninFoo.getNodes().size(); i++) {
					MuninNode node = muninFoo.getNodes().get(i);

					LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View v = vi.inflate(R.layout.adapter_twolines_checkbox, null);

					checkboxes[i] = (CheckBox) v.findViewById(R.id.line_0);
					int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
					checkboxes[i].setButtonDrawable(id);

					if (watchedNodes.contains(node.getUrl()))
						checkboxes[i].setChecked(true);

					v.findViewById(R.id.ll_container).setOnClickListener(new View.OnClickListener() {
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

		cb_notifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				View notificationsSettings = view.findViewById(R.id.notificationsEnabled);
				if (isChecked)
					notificationsSettings.setVisibility(View.VISIBLE);
				else
					notificationsSettings.setVisibility(View.GONE);
			}
		});

		// Since we manually defined the checkbox and text
		// (so the checkbox can be at the right and still have the view tinting introduced
		// on Android 5.0), we have to manually define the onclick listener on the label
		for (View v : Util.getViewsByTag((ViewGroup)view.findViewById(R.id.container), "checkable")) {
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					ViewGroup row = (ViewGroup) view;
					CheckBox checkBox = (CheckBox) Util.getChild(row, AppCompatCheckBox.class);
					if (checkBox != null)
						checkBox.setChecked(!checkBox.isChecked());
				}
			});
		}

		// Set fonts
		for (View v : Util.getViewsByTag((ViewGroup)view.findViewById(R.id.container), "set_font"))
			Util.Fonts.setFont(context, (TextView) v, Util.Fonts.CustomFont.Roboto_Medium);

		return view;
	}

	private void enableNotifications() {
		if (muninFoo.premium) {
			settings.remove(Settings.PrefKeys.Notifs_Poll_LastNotificationText);
			int min = settings.getInt(Settings.PrefKeys.Notifs_Poll_RefreshRate, 0);
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(context, Service_PollNotifications.class);
			PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
			am.cancel(pi);

			if (min > 0) {
				am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime() + min*60*1000,
						min*60*1000, pi);
			}
		}
	}

	private void disableNotifications() {
		settings.remove(Settings.PrefKeys.Notifs_Poll_LastNotificationText);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, Service_PollNotifications.class);
		PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
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
		settings.set(Settings.PrefKeys.Notifs_Poll_NodesList, nodes);
	}

	private void computeEstimatedConsumption() {
		String watchedNodes = settings.getString(Settings.PrefKeys.Notifs_Poll_NodesList);
		int nbNodes = watchedNodes.equals("") ? 0 : watchedNodes.split(";").length;

		double result = (1440/currentRefreshRate) * nbNodes * PAGE_WEIGHT;
		String unit = "kb";
		if (result > 1024) {
			result = result / 1024;
			unit = "Mb";
		}
		DecimalFormat df = new DecimalFormat("###");
		((TextView) view.findViewById(R.id.estimated_data_consumption)).setText(getString(R.string.text54).replace("??", df.format(result) + " " + unit));
	}

	@Override
	public void save() {
		if (!muninFoo.premium)
			return;

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
				int length = settings.getString(Settings.PrefKeys.Notifs_Poll_NodesList).length();
				if (length > 2) // != "" && != ";"
					ok = true;
			}
		}

		if (ok) {
			if (cb_notifications.isChecked()) {
				settings.set(Settings.PrefKeys.Notifs_Poll, true);
				settings.set(Settings.PrefKeys.Notifs_Poll_WifiOnly, cb_wifiOnly.isChecked());
				settings.set(Settings.PrefKeys.Notifs_Poll_Vibrate, cb_vibrate.isChecked());
				settings.set(Settings.PrefKeys.Notifs_Poll_RefreshRate, REFRESH_RATES[sp_refreshRate.getSelectedItemPosition()]);
				enableNotifications();
			} else {
				settings.set(Settings.PrefKeys.Notifs_Poll, true);
				settings.remove(Settings.PrefKeys.Notifs_Poll_WifiOnly);
				settings.remove(Settings.PrefKeys.Notifs_Poll_RefreshRate);
				settings.remove(Settings.PrefKeys.Notifs_Poll_Vibrate);
				disableNotifications();
			}
			Toast.makeText(context, R.string.text36, Toast.LENGTH_SHORT).show();
		} else
			Toast.makeText(context, R.string.text56, Toast.LENGTH_SHORT).show();
	}
}
