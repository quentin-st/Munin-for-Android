package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.obj.NotifIgnoreRule;

import java.util.Calendar;

/**
 * Activity launched on click to the "Ignore" button on each notification
 */
public class Activity_IgnoreNotification extends Activity {
	public static String EXTRA_NOTIFICATION_ID = "notification_id";
	public static String EXTRA_GROUP = "group";
	public static String EXTRA_HOST = "host";
	public static String EXTRA_PLUGIN = "plugin";

	private Activity activity;

	private String group, host, plugin;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = this;

		// Dismiss notification
		Intent intent = getIntent();
		if (intent.hasExtra(EXTRA_NOTIFICATION_ID)) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1));
		}

		// Get extras
		group = intent.getStringExtra(EXTRA_GROUP);
		host = intent.getStringExtra(EXTRA_HOST);
		plugin = intent.getStringExtra(EXTRA_PLUGIN);

		LinearLayout container = new LinearLayout(this);
		container.setPadding(30, 45, 30, 0);
		container.setOrientation(LinearLayout.VERTICAL);

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(15, 10, 15, 10);

		TextView textView = new TextView(this);
		textView.setText(R.string.ignore_for);
		textView.setLayoutParams(layoutParams);
		container.addView(textView);

		// Ignore mode RadioGroup
		RadioGroup radioGroup = new RadioGroup(this);
		radioGroup.setOrientation(RadioGroup.VERTICAL);
		radioGroup.setLayoutParams(layoutParams);

		final RadioButton rb_wholeGroup = new RadioButton(this);
		rb_wholeGroup.setText(String.format(getString(R.string.ignore_whole_group), group));
		radioGroup.addView(rb_wholeGroup);

		final RadioButton rb_wholeHost = new RadioButton(this);
		rb_wholeHost.setText(String.format(getString(R.string.ignore_whole_host), host, group));
		radioGroup.addView(rb_wholeHost);

		final RadioButton rb_pluginIn = new RadioButton(this);
		rb_pluginIn.setText(String.format(getString(R.string.ignore_plugin_in), plugin, host, group));
		radioGroup.addView(rb_pluginIn);

		final RadioButton rb_plugin = new RadioButton(this);
		rb_plugin.setText(String.format(getString(R.string.ignore_plugin), plugin));
		radioGroup.addView(rb_plugin);

		radioGroup.check(rb_pluginIn.getId());

		container.addView(radioGroup);


		// Ignore duration spinner
		final Spinner ignoreDuration = new Spinner(this);
		String[] spinnerArray = {
				getString(R.string.one_hour),
				getString(R.string.six_hours),
				getString(R.string.one_day),
				getString(R.string.one_week),
				getString(R.string.forever)
		};
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerArray);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ignoreDuration.setAdapter(spinnerAdapter);
		ignoreDuration.setLayoutParams(layoutParams);

		container.addView(ignoreDuration);

		// Create dialog
		new AlertDialog.Builder(this)
				.setTitle(R.string.ignore_alert)
				.setView(container)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String _group = group, _host = host, _plugin = plugin;

						// Depending on ignore mode, set any of _group, _host & _plugin to null
						if (rb_wholeGroup.isChecked())
							_host = _plugin = null;
						else if (rb_wholeHost.isChecked())
							_plugin = null;
						else if (rb_plugin.isChecked())
							_group = _host = null;

						// Until
						Calendar until = Calendar.getInstance();
						MuninFoo.log("Now: " + until.getTimeInMillis());
						switch (ignoreDuration.getSelectedItemPosition()) {
							case 0: until.add(Calendar.HOUR, 1);    break;
							case 1: until.add(Calendar.HOUR, 6);    break;
							case 2: until.add(Calendar.DATE, 1);    break;
							case 3: until.add(Calendar.DATE, 7);    break;
							case 4: until = null;                   break;
						}
						MuninFoo.log("Now modified: " + (until != null ? until.getTimeInMillis() : 0));

						DatabaseHelper dbHelper = MuninFoo.getInstance(activity).sqlite.dbHlpr;
						dbHelper.addNotifIgnoreRule(new NotifIgnoreRule(
								_group, _host, _plugin, until
						));

						activity.finish();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						activity.finish();
					}
				})
				.setIcon(R.drawable.ic_bookmark_remove_grey600)
				.show();
	}
}
