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
import android.widget.Spinner;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.ntfs.GcmListenerService;

/**
 * Activity launched on click to the "Ignore" button on each notification
 */
public class Activity_IgnoreAlert extends Activity {
	private Activity activity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = this;

		// Dismiss notification
		Intent intent = getIntent();
		if (intent.hasExtra(GcmListenerService.EXTRA_NOTIFICATION_ID)) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(intent.getIntExtra(GcmListenerService.EXTRA_NOTIFICATION_ID, -1));
		}


		LinearLayout container = new LinearLayout(this);
		container.setPadding(30, 45, 30, 0);
		container.setOrientation(LinearLayout.VERTICAL);

		TextView textView = new TextView(this);
		textView.setText(R.string.ignore_for);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(15, 10, 15, 10);
		textView.setLayoutParams(layoutParams);
		container.addView(textView);

		Spinner ignoreDuration = new Spinner(this);
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

		container.addView(ignoreDuration);

		// Create dialog
		new AlertDialog.Builder(this)
				.setTitle(R.string.ignore_alert)
				.setView(container)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// TODO

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
