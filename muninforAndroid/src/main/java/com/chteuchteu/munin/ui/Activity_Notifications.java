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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_NotifIgnoreRules;
import com.chteuchteu.munin.async.Notifications_SendInstructionsByMail;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.ntfs.RegistrationIntentService;
import com.chteuchteu.munin.obj.NotifIgnoreRule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.List;

public class Activity_Notifications extends MuninActivity {
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	public static final String REGISTRATION_COMPLETE = "REGISTRATION_COMPLETE";
	public static final String INSTRUCTIONS_EMAIL_TARGET = "http://gcm-proxy.munin-for-android.com/android/sendConfig";

	private ProgressDialog progressDialog;
	private BroadcastReceiver registrationBroadcastReceiver;

	private CheckBox	cb_notifications;
	private CheckBox    cb_vibrate;
	private Button      bt_sendByMail;
	private TextView    ignoreRulesText;
	private Button      manageIgnoreRules;

	private List<NotifIgnoreRule> ignoreRules;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_notifications);
		super.onContentViewSet();

		actionBar.setTitle(getString(R.string.notificationsTitle));

		registrationBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (progressDialog != null)
					progressDialog.dismiss();

				updateDeviceCode();
				bt_sendByMail.setEnabled(true);
			}
		};
		
		cb_notifications = (CheckBox) findViewById(R.id.checkbox_notifications);
		cb_vibrate = (CheckBox) findViewById(R.id.checkbox_vibrate);
		bt_sendByMail = (Button) findViewById(R.id.sendByMail);
		bt_sendByMail.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final EditText input = new EditText(context);
				input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

				new AlertDialog.Builder(context)
						.setView(input)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String value = input.getText().toString();
								if (!value.isEmpty()) {
									new Notifications_SendInstructionsByMail(
											context, value,
											muninFoo.getSettings().getString(Settings.PrefKeys.Notifs_GCM_regId),
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
		bt_sendByMail.setEnabled(settings.getString(Settings.PrefKeys.Notifs_GCM_regId) != null);

		updateDeviceCode();

		boolean notificationsEnabled = settings.getBool(Settings.PrefKeys.Notifications);
		cb_notifications.setChecked(notificationsEnabled);
		if (!notificationsEnabled)
			findViewById(R.id.notificationsEnabled).setVisibility(View.GONE);

		// Check if the device can vibrate
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		cb_vibrate.setEnabled(v.hasVibrator());
		cb_vibrate.setChecked(settings.getBool(Settings.PrefKeys.Notifs_Vibrate));
		
		cb_notifications.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				View notificationsSettings = activity.findViewById(R.id.notificationsEnabled);
				if (isChecked)
					notificationsSettings.setVisibility(View.VISIBLE);
				else
					notificationsSettings.setVisibility(View.GONE);

				// Get reg id
				if (isChecked && muninFoo.getSettings().getString(Settings.PrefKeys.Notifs_GCM_regId) == null) {
					if (checkPlayServices()) {
						progressDialog = ProgressDialog.show(context, "", getString(R.string.loading), true);
						Intent intent = new Intent(context, RegistrationIntentService.class);
						startService(intent);
					}
				}
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

		// Set fonts
		for (View view : Util.getViewsByTag((ViewGroup)findViewById(R.id.container), "set_font"))
			Util.Fonts.setFont(this, (TextView) view, Util.Fonts.CustomFont.Roboto_Medium);

		// Get ignore rules
		ignoreRules = muninFoo.sqlite.dbHlpr.getAllNotifIgnoreRules(true);
		ignoreRulesText = (TextView) findViewById(R.id.ignoreRulesText);
		manageIgnoreRules = (Button) findViewById(R.id.manageIgnoreRules);
		manageIgnoreRules.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				manageIgnoreRules();
			}
		});

		updateIgnoreRulesCount();
	}

	private void updateIgnoreRulesCount() {
		ignoreRulesText.setText(String.format(getString(R.string.ignoreRulesText), ignoreRules.size()));
		manageIgnoreRules.setEnabled(ignoreRules.size() > 0);
	}

	private void manageIgnoreRules() {
		// Create view
		ListView listView = new ListView(this);
		Adapter_NotifIgnoreRules adapter = new Adapter_NotifIgnoreRules(this, this.ignoreRules);
		listView.setAdapter(adapter);

		new AlertDialog.Builder(this)
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
		String deviceCode = settings.getString(Settings.PrefKeys.Notifs_GCM_regId);
		if (deviceCode != null) {
			TextView tv_deviceCode = (TextView) findViewById(R.id.device_code);
			tv_deviceCode.setText(deviceCode.substring(0, Math.min(deviceCode.length()-1, 15)) + "...");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(registrationBroadcastReceiver,
				new IntentFilter(REGISTRATION_COMPLETE));
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(registrationBroadcastReceiver);
		super.onPause();
	}

	@Override
	public void onBackPressed() {
        if (drawerHelper.closeDrawerIfOpen())
            return;

        Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
	}

	private void actionSave() {
		if (!muninFoo.premium)
			return;

		settings.set(Settings.PrefKeys.Notifications, cb_notifications.isChecked());
		settings.set(Settings.PrefKeys.Notifs_Vibrate, cb_vibrate.isChecked());
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

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (apiAvailability.isUserResolvableError(resultCode))
				apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			else
				finish();

			return false;
		}
		return true;
	}
}
