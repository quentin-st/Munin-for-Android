package com.chteuchteu.munin.wdget;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.ui.Activity_Alerts;

public class Widget_AlertsWidget_WidgetProvider extends AppWidgetProvider {
	public static final String ACTION_REFRESH = "com.chteuchteu.munin.widget2.REFRESH";
	public static final String ACTION_START_ACTIVITY = "com.chteuchteu.munin.widget2.START_ACTIVITY";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (Integer i : appWidgetIds)
			updateAppWidget(context, appWidgetManager, i);

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		Intent intent = new Intent(context, Widget_AlertsWidget_WidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

		RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.widget_alertswidget_layout);
		widget.setRemoteAdapter(appWidgetId, R.id.servers, intent);

		// Open Alerts
		Intent clickIntent = new Intent(context, Activity_Alerts.class);
		PendingIntent clickPI = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		widget.setPendingIntentTemplate(R.id.servers, clickPI);

		// Refresh
		widget.setOnClickPendingIntent(R.id.refresh, getPendingSelfIntent(context, ACTION_REFRESH, appWidgetId));

		appWidgetManager.updateAppWidget(appWidgetId, widget);
	}

	private static PendingIntent getPendingSelfIntent(Context context, String action, int appWidgetId) {
		Intent intent = new Intent(context, Widget_AlertsWidget_WidgetProvider.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setAction(action);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}

	@Override
	public void onReceive(@NonNull Context context, @NonNull Intent intent) {
		super.onReceive(context, intent);

		if (intent.getAction() != null) {
			if (intent.getAction().equals(ACTION_REFRESH)) {
				// Only display "No connection" toast if manual refresh
				if (Util.isOnline(context)) {
					Settings.getInstance(context).set(Settings.PrefKeys.Widget2_ForceUpdate, true);
					AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
					int appWidgetIds[] = appWidgetManager.getAppWidgetIds(
							new ComponentName(context, Widget_AlertsWidget_WidgetProvider.class));
					appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.servers);
				}
				else
					Toast.makeText(context, R.string.text30, Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		DatabaseHelper dHelper = new DatabaseHelper(context);
		for (int i : appWidgetIds)
			dHelper.deleteAlertsWidget(i);
	}
}
