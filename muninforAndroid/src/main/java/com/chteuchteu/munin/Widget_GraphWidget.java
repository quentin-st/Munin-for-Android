package com.chteuchteu.munin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.chteuchteu.munin.hlpr.SQLite;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.Widget;
import com.chteuchteu.munin.ui.Activity_GoPremium;
import com.chteuchteu.munin.ui.Activity_GraphView;

public class Widget_GraphWidget extends AppWidgetProvider {
	private static RemoteViews 		views;
	private static AppWidgetManager 	awm;
	private static int				widgetId;
	private static final String ACTION_UPDATE_GRAPH = "com.chteuchteu.munin.widget.UPDATE_GRAPH";
	private static final String ACTION_START_ACTIVITY = "com.chteuchteu.munin.widget.START_ACTIVITY";
	private static final String ACTION_START_PREMIUM = "com.chteuchteu.munin.widget.START_PREMIUM";
	
	private static SQLite sqlite;
	private static Widget widget;
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		sqlite = new SQLite(context, MuninFoo.getInstance(context));
		
		// Get all ids
		ComponentName thisWidget = new ComponentName(context, Widget_GraphWidget.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		
		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int i = 0; i < allWidgetIds.length; i++) {
			updateAppWidget(context, appWidgetManager, allWidgetIds[i], false);
		}
	}
	
	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean forceUpdate) {
		awm = appWidgetManager;
		widgetId = appWidgetId;
		
		boolean premium = MuninFoo.isPremium(context);
		
		// Updating widget
		views = new RemoteViews(context.getPackageName(), R.layout.graphwidget_layout);
		if (!premium){
			views.setTextViewText(R.id.widget_servername, "Munin for Android Features Pack needed");
			//views.setBitmap(R.id.widget_graph, "setImageBitmap", BitmapFactory.decodeResource(context.getResources(), R.drawable.widget_featurespack));
			
			// Action open Munin for Android
			Intent intent2 = new Intent(context, Widget_GraphWidget.class);
			intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			intent2.setAction(ACTION_START_PREMIUM);
			PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, appWidgetId, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_graph, pendingIntent2);
			
			awm.updateAppWidget(widgetId, views);
		} else {
			// premium
			if (sqlite == null)
				sqlite = new SQLite(context, MuninFoo.getInstance(context));
			widget = sqlite.dbHlpr.getWidget(appWidgetId);
			
			if (widget != null && widget.getPlugin() != null && widget.getPlugin().getInstalledOn() != null) {
				if (!widget.getHideServerName())
					views.setTextViewText(R.id.widget_servername, widget.getPlugin().getInstalledOn().getName());
				else {
					views.setViewVisibility(R.id.widget_legend, View.GONE);
					views.setInt(R.id.widget_graph, "setBackgroundColor", Color.TRANSPARENT);
				}
				
				// Update action
				Intent intent = new Intent(context, Widget_GraphWidget.class);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				intent.setAction(ACTION_UPDATE_GRAPH);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				views.setOnClickPendingIntent(R.id.widget_legend, pendingIntent);
				
				// Action open Munin for Android
				Intent intent2 = new Intent(context, Widget_GraphWidget.class);
				intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				intent2.setAction(ACTION_START_ACTIVITY);
				PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, appWidgetId, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
				views.setOnClickPendingIntent(R.id.widget_graph, pendingIntent2);
				
				if (!widget.isWifiOnly() || forceUpdate) {
					// Launching Asyntask
					ApplyBitmap task = new ApplyBitmap(widget, views, awm, appWidgetId);
					task.execute();
				} else {
					// Automatic update -> let's check if on wifi or data
					ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					
					if (mWifi.isConnected())
						updateAppWidget(context, appWidgetManager, appWidgetId, true);
				}
			}
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		if (sqlite == null)
			sqlite = new SQLite(context, MuninFoo.getInstance(context));
		
		if (intent.getAction() != null) {
			if (intent.getAction().equals(ACTION_UPDATE_GRAPH)) {
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
					widget = sqlite.dbHlpr.getWidget(widgetId);
					Intent intent2 = new Intent(context, Activity_GraphView.class);
					intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent2.putExtra("server", widget.getPlugin().getInstalledOn().getServerUrl());
					intent2.putExtra("plugin", widget.getPlugin().getName());
					intent2.putExtra("period", widget.getPeriod());
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
	
	private static class ApplyBitmap extends AsyncTask<Void, Integer, Void> {
		private Bitmap bm;
		
		private MuninServer serv;
		private String url;
		private Widget widget;
		
		private RemoteViews views;
		private AppWidgetManager awm;
		private int widgetId;
		
		public ApplyBitmap(Widget widget, RemoteViews v, AppWidgetManager a, int w) {
			super();
			this.serv = widget.getPlugin().getInstalledOn();
			this.url = widget.getPlugin().getImgUrl(widget.getPeriod());
			this.widget = widget;
			this.views = v;
			this.awm = a;
			this.widgetId = w;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			this.views.setViewVisibility(R.id.widget_servername, View.GONE);
			this.views.setViewVisibility(R.id.widget_loading, View.VISIBLE);
			this.awm.updateAppWidget(this.widgetId, this.views);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			bm = serv.getPlugin(0).getGraph(url);
			bm = Util.removeBitmapBorder(bm);
			if (widget.getHideServerName())
				bm = Util.dropShadow(bm);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (this.bm != null)
				this.views.setBitmap(R.id.widget_graph, "setImageBitmap", this.bm);
			this.views.setViewVisibility(R.id.widget_loading, View.GONE);
			this.views.setViewVisibility(R.id.widget_servername, View.VISIBLE);
			this.awm.updateAppWidget(this.widgetId, this.views);
		}
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		if (sqlite == null)
			sqlite = new SQLite(context, MuninFoo.getInstance(context));
		for (int i : appWidgetIds)
			sqlite.dbHlpr.deleteWidget(i);
	}
}