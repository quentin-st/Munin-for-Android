package com.chteuchteu.munin;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.ui.Activity_Alerts;

public class Widget_AlertsWidget_WidgetProvider extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (int i=0; i<appWidgetIds.length; i++)
			updateAppWidget(context, appWidgetManager, appWidgetIds[i]);

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		Intent intent = new Intent(context, Widget_AlertsWidget_WidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

		RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.widget_alertswidget_layout);
		widget.setRemoteAdapter(appWidgetId, R.id.servers, intent);

		Intent clickIntent = new Intent(context, Activity_Alerts.class);
		PendingIntent clickPI = PendingIntent.getActivity(context, 0, clickIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		widget.setPendingIntentTemplate(R.id.servers, clickPI);

		appWidgetManager.updateAppWidget(appWidgetId, widget);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		DatabaseHelper dHelper = new DatabaseHelper(context);
		for (int i : appWidgetIds)
			dHelper.deleteAlertsWidget(i);
	}
}
