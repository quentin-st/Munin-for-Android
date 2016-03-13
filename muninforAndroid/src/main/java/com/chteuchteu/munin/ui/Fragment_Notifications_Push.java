package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_NotifIgnoreRules;
import com.chteuchteu.munin.async.Notifications_SendInstructionsByMail;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.ntfs.push.RegistrationIntentService;
import com.chteuchteu.munin.obj.NotifIgnoreRule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.List;

public class Fragment_Notifications_Push extends Fragment implements INotificationsFragment {
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	public static final String REGISTRATION_COMPLETE = "REGISTRATION_COMPLETE";
	public static final String INSTRUCTIONS_EMAIL_TARGET = "http://gcm-proxy.munin-for-android.com/android/sendConfig";

	private MuninFoo    muninFoo;
	private Settings    settings;
	private Context     context;
	private View        view;

	private ProgressDialog progressDialog;
	private BroadcastReceiver registrationBroadcastReceiver;

	private CheckBox    cb_notifications;
	private CheckBox    cb_vibrate;
	private Button      bt_sendByMail;
	private TextView    ignoreRulesText;
	private Button      manageIgnoreRules;

	private List<NotifIgnoreRule> ignoreRules;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		this.context = context;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_notifications_push, container, false);
		muninFoo = MuninFoo.getInstance(context);
		settings = muninFoo.getSettings();
		
		registrationBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (progressDialog != null)
					progressDialog.dismiss();

				updateDeviceCode();
				bt_sendByMail.setEnabled(true);
			}
		};

		cb_notifications = (CheckBox) view.findViewById(R.id.checkbox_notifications);
		cb_vibrate = (CheckBox) view.findViewById(R.id.checkbox_vibrate);
		bt_sendByMail = (Button) view.findViewById(R.id.sendByMail);
		bt_sendByMail.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LayoutInflater inflater = LayoutInflater.from(context);
				ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_edittext, null, false);
				final EditText input = (EditText) view.findViewById(R.id.input);
				input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

				new AlertDialog.Builder(context)
						.setTitle(R.string.notifications_sendByMail)
						.setView(view)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String value = input.getText().toString();
								if (!value.isEmpty()) {
									new Notifications_SendInstructionsByMail(
											context, value,
											muninFoo.getSettings().getString(Settings.PrefKeys.Notifs_Push_regId),
											muninFoo.getUserAgent()
									).execute();
								}
								dialog.dismiss();
							}
						})
						.setNegativeButton(R.string.text64, null)
						.show();
			}
		});
		bt_sendByMail.setEnabled(settings.getString(Settings.PrefKeys.Notifs_Push_regId) != null);

		updateDeviceCode();

		boolean notificationsEnabled = settings.getBool(Settings.PrefKeys.Notifs_Push);
		cb_notifications.setChecked(notificationsEnabled);
		if (!notificationsEnabled)
			view.findViewById(R.id.notificationsEnabled).setVisibility(View.GONE);

		// Check if the device can vibrate
		Vibrator vivrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		cb_vibrate.setEnabled(vivrator.hasVibrator());
		cb_vibrate.setChecked(settings.getBool(Settings.PrefKeys.Notifs_Push_Vibrate));

		cb_notifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				View notificationsSettings = view.findViewById(R.id.notificationsEnabled);
				if (isChecked)
					notificationsSettings.setVisibility(View.VISIBLE);
				else
					notificationsSettings.setVisibility(View.GONE);

				// Get reg id
				if (isChecked && muninFoo.getSettings().getString(Settings.PrefKeys.Notifs_Push_regId) == null) {
					if (checkPlayServices()) {
						progressDialog = ProgressDialog.show(context, "", getString(R.string.loading), true);
						Intent intent = new Intent(context, RegistrationIntentService.class);
						context.startService(intent);
					}
				}
			}
		});

		// Since we manually defined the checkbox and text
		// (so the checkbox can be at the right and still have the view tinting introduced
		// on Android 5.0), we have to manually define the onclick listener on the label
		for (View v : Util.getViewsByTag((ViewGroup) view.findViewById(R.id.container), "checkable")) {
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

		// Get ignore rules
		ignoreRules = muninFoo.sqlite.dbHlpr.getAllNotifIgnoreRules(true);
		ignoreRulesText = (TextView) view.findViewById(R.id.ignoreRulesText);
		manageIgnoreRules = (Button) view.findViewById(R.id.manageIgnoreRules);
		manageIgnoreRules.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				manageIgnoreRules();
			}
		});

		updateIgnoreRulesCount();

		return view;
	}

	public void updateIgnoreRulesCount() {
		ignoreRulesText.setText(String.format(getString(R.string.ignoreRulesText), ignoreRules.size()));
		manageIgnoreRules.setEnabled(ignoreRules.size() > 0);
	}

	private void manageIgnoreRules() {
		// Create view
		ListView listView = new ListView(context);
		Adapter_NotifIgnoreRules adapter = new Adapter_NotifIgnoreRules(this, context, this.ignoreRules);
		listView.setAdapter(adapter);

		new AlertDialog.Builder(context)
				.setTitle(R.string.ignoreRules)
				.setView(listView)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Update rules count
						ignoreRulesText.setText(String.format(getString(R.string.ignoreRulesText), ignoreRules.size()));
					}
				})
				.show();
	}

	private void updateDeviceCode() {
		String deviceCode = settings.getString(Settings.PrefKeys.Notifs_Push_regId);
		if (deviceCode != null) {
			TextView tv_deviceCode = (TextView) view.findViewById(R.id.device_code);
			tv_deviceCode.setText(deviceCode.substring(0, Math.min(deviceCode.length()-1, 15)) + "...");
		}
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (apiAvailability.isUserResolvableError(resultCode))
				apiAvailability.getErrorDialog(getActivity(), resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();

			return false;
		}
		return true;
	}

	@Override
	public void save() {
		settings.set(Settings.PrefKeys.Notifs_Push, cb_notifications.isChecked());
		settings.set(Settings.PrefKeys.Notifs_Push_Vibrate, cb_vibrate.isChecked());
	}

	@Override
	public void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(context).registerReceiver(registrationBroadcastReceiver,
				new IntentFilter(REGISTRATION_COMPLETE));
	}

	@Override
	public void onPause() {
		LocalBroadcastManager.getInstance(context).unregisterReceiver(registrationBroadcastReceiver);
		super.onPause();
	}
}
