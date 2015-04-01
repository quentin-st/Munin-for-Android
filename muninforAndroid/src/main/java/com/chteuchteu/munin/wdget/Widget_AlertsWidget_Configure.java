package com.chteuchteu.munin.wdget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.AlertsWidget;

public class Widget_AlertsWidget_Configure extends Activity {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private MuninFoo muninFoo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		muninFoo = MuninFoo.getInstance(this);
		
		// If the user closes window, don't create the widget
		setResult(RESULT_CANCELED);
		
		// Find widget id from launching intent
		Bundle extras = getIntent().getExtras();
		if (extras != null)
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		
		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
			finish();

		if (muninFoo == null) {
			Toast.makeText(this, getString(R.string.text09), Toast.LENGTH_SHORT).show();
			finish();
		}
		if (!muninFoo.premium) {
			Toast.makeText(this, R.string.featuresPackNeeded, Toast.LENGTH_SHORT).show();
			finish();
		}
		if (muninFoo.getNodes().size() == 0) {
			Toast.makeText(this, R.string.text37, Toast.LENGTH_SHORT).show();
			finish();
		}
		
		if (!muninFoo.sqlite.dbHlpr.hasAlertsWidget(mAppWidgetId)) {
			final AlertsWidget alertsWidget = new AlertsWidget();
			alertsWidget.setWidgetId(mAppWidgetId);

			final Context context = new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light);
			final ScrollView scrollView = new ScrollView(context);

			LinearLayout checkboxesView = new LinearLayout(context);
			checkboxesView.setOrientation(LinearLayout.VERTICAL);

			final CheckBox[] checkboxes = new CheckBox[muninFoo.getNodes().size()];

			for (int i=0; i<muninFoo.getNodes().size(); i++) {
				LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View v = vi.inflate(R.layout.servers_list_checkbox, checkboxesView, false);

				checkboxes[i] = (CheckBox) v.findViewById(R.id.line_0);
				int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
				checkboxes[i].setButtonDrawable(id);

				v.findViewById(R.id.ll_container).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						CheckBox checkbox = (CheckBox) v.findViewById(R.id.line_0);
						checkbox.setChecked(!checkbox.isChecked());
					}
				});

				((TextView)v.findViewById(R.id.line_a)).setText(muninFoo.getNodes().get(i).getName());
				((TextView)v.findViewById(R.id.line_b)).setText(muninFoo.getNodes().get(i).getUrl());

				checkboxesView.addView(v);
			}
			scrollView.addView(checkboxesView);


			new AlertDialog.Builder(context)
					.setView(scrollView)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int ii) {
							// Save and close
							int i = 0;
							for (CheckBox checkbox : checkboxes) {
								if (checkbox.isChecked())
									alertsWidget.getNodes().add(muninFoo.getNodes().get(i));

								i++;
							}

							muninFoo.sqlite.dbHlpr.insertAlertsWidget(alertsWidget);

							configureWidget(getApplicationContext());

							// Make sure we pass back the original appWidgetId before closing the activity
							Intent resultValue = new Intent();
							resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
							setResult(RESULT_OK, resultValue);
							finish();
						}
					})
					.show();
		}
	}
	
	/**
	 * Configures the created widget
	 * @param context Context
	 */
	private void configureWidget(Context context) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		Widget_AlertsWidget_WidgetProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId);
	}
}
