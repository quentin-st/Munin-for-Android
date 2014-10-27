package com.chteuchteu.munin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.chteuchteu.munin.hlpr.SQLite;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.AlertsWidget;
import com.chteuchteu.munin.ui.Activity_Alerts;
import com.chteuchteu.munin.ui.Activity_GoPremium;

public class Widget_AlertsWidget extends AppWidgetProvider {
	private static RemoteViews 		views;
	private static AppWidgetManager 	awm;
	private static int				widgetId;
	private static final String ACTION_REFRESH = "com.chteuchteu.munin.widget2.REFRESH";
	private static final String ACTION_START_ACTIVITY = "com.chteuchteu.munin.widget2.START_ACTIVITY";
	private static final String ACTION_START_PREMIUM = "com.chteuchteu.munin.widget2.START_PREMIUM";
	
	private static SQLite sqlite;
	private static AlertsWidget alertsWidget;
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		sqlite = new SQLite(context, MuninFoo.getInstance(context));
		
		// Get all ids
		ComponentName thisWidget = new ComponentName(context, Widget_AlertsWidget.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		
		// Perform this loop procedure for each App GraphWidget that belongs to this provider
		for (int i=0; i < allWidgetIds.length; i++)
			updateAppWidget(context, appWidgetManager, allWidgetIds[i], false);
	}
	
	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean forceUpdate) {
		awm = appWidgetManager;
		widgetId = appWidgetId;
		
		boolean premium = MuninFoo.isPremium(context);
		
		// Updating graphWidget
		views = new RemoteViews(context.getPackageName(), R.layout.widget_alertswidget_layout);
		if (!premium) {
			views.setTextViewText(R.id.textview, "Munin for Android Features Pack needed");
			
			// Action open Munin for Android
			Intent intent2 = new Intent(context, Widget_AlertsWidget.class);
			intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			intent2.setAction(ACTION_START_PREMIUM);
			PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, appWidgetId, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_graph, pendingIntent2);
			
			awm.updateAppWidget(widgetId, views);
		} else {
			// premium
			if (sqlite == null)
				sqlite = new SQLite(context, MuninFoo.getInstance(context));

			alertsWidget = sqlite.dbHlpr.getAlertsWidget(widgetId, null);
			
			/*if (alertsWidget != null && !alertsWidget.getServers().isEmpty()) {
				if (!graphWidget.getHideServerName())
					views.setTextViewText(R.id.widget_servername, graphWidget.getPlugin().getInstalledOn().getName());
				else {
					views.setViewVisibility(R.id.widget_legend, View.GONE);
					views.setInt(R.id.widget_graph, "setBackgroundColor", Color.TRANSPARENT);
				}

				// Update action
				Intent intent = new Intent(context, Widget_AlertsWidget.class);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				intent.setAction(ACTION_UPDATE_GRAPH);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				views.setOnClickPendingIntent(R.id.widget_legend, pendingIntent);
				
				// Action open Munin for Android
				Intent intent2 = new Intent(context, Widget_AlertsWidget.class);
				intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				intent2.setAction(ACTION_START_ACTIVITY);
				PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, appWidgetId, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
				views.setOnClickPendingIntent(R.id.widget_graph, pendingIntent2);
				
				if (!graphWidget.isWifiOnly() || forceUpdate) {
					// Launching Asyntask
					ApplyBitmap task = new ApplyBitmap(graphWidget, views, awm, appWidgetId);
					task.execute();
				} else {
					// Automatic update -> let's check if on wifi or data
					ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					
					if (mWifi.isConnected())
						updateAppWidget(context, appWidgetManager, appWidgetId, true);
				}
			}*/
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		if (sqlite == null)
			sqlite = new SQLite(context, MuninFoo.getInstance(context));
		
		if (intent.getAction() != null) {
			if (intent.getAction().equals(ACTION_REFRESH)) {
				// Check if connection is available
				if (Util.isOnline(context)) {
					Bundle extras = intent.getExtras();
					if (extras != null) {
						AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
						int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
						
						updateAppWidget(context, appWidgetManager, widgetId, true);
					}
				}
			} else if (intent.getAction().equals(ACTION_START_ACTIVITY)) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
					alertsWidget = sqlite.dbHlpr.getAlertsWidget(widgetId, null);
					Intent intent2 = new Intent(context, Activity_Alerts.class);
					intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent2);
				}
			} else if (intent.getAction().equals(ACTION_START_PREMIUM)) {
				Intent intent2 = new Intent(context, Activity_GoPremium.class);
				intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent2);
			} else {
				super.onReceive(context, intent);
			}
		} else
			super.onReceive(context, intent);
	}
	
	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
	}
	

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		if (sqlite == null)
			sqlite = new SQLite(context, MuninFoo.getInstance(context));
		for (int i : appWidgetIds)
			sqlite.dbHlpr.deleteAlertsWidget(i);
	}
}