package com.chteuchteu.munin.wdget;

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

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.SQLite;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.GraphWidget;
import com.chteuchteu.munin.ui.Activity_GoPremium;
import com.chteuchteu.munin.ui.Activity_GraphView;

public class Widget_GraphWidget_WidgetProvider extends AppWidgetProvider {
	private static final String ACTION_UPDATE_GRAPH = "com.chteuchteu.munin.widget.UPDATE_GRAPH";
	private static final String ACTION_START_ACTIVITY = "com.chteuchteu.munin.widget.START_ACTIVITY";
	private static final String ACTION_START_PREMIUM = "com.chteuchteu.munin.widget.START_PREMIUM";
	
	private static SQLite sqlite;
	private static GraphWidget graphWidget;
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		sqlite = new SQLite(context, MuninFoo.getInstance(context));
		
		// Get all ids
		ComponentName thisWidget = new ComponentName(context, Widget_GraphWidget_WidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		
		// Perform this loop procedure for each App GraphWidget that belongs to this provider
		for (Integer i : allWidgetIds)
			updateAppWidget(context, appWidgetManager, i, false);
	}
	
	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int widgetId, boolean forceUpdate) {
		boolean premium = MuninFoo.isPremium(context);
		
		// Updating graphWidget
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_graphwidget_layout);
		if (!premium){
			views.setTextViewText(R.id.widget_servername, "Munin for Android Features Pack needed");
			//views.setBitmap(R.id.widget_graph, "setImageBitmap", BitmapFactory.decodeResource(context.getResources(), R.drawable.widget_featurespack));
			
			// Action open Munin for Android
			Intent intent2 = new Intent(context, Widget_GraphWidget_WidgetProvider.class);
			intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
			intent2.setAction(ACTION_START_PREMIUM);
			PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, widgetId, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_graph, pendingIntent2);

			appWidgetManager.updateAppWidget(widgetId, views);
		} else {
			// premium
			if (sqlite == null)
				sqlite = new SQLite(context, MuninFoo.getInstance(context));
			graphWidget = sqlite.dbHlpr.getGraphWidget(widgetId);
			
			if (graphWidget != null && graphWidget.getPlugin() != null
					&& graphWidget.getPlugin().getInstalledOn() != null
					&& graphWidget.getPlugin().getInstalledOn().getParent() != null) {
				if (!graphWidget.getHideServerName())
					views.setTextViewText(R.id.widget_servername, graphWidget.getPlugin().getInstalledOn().getName());
				else {
					views.setViewVisibility(R.id.widget_legend, View.GONE);
					views.setInt(R.id.widget_graph, "setBackgroundColor", Color.TRANSPARENT);
				}
				
				// Update action
				Intent intent = new Intent(context, Widget_GraphWidget_WidgetProvider.class);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
				intent.setAction(ACTION_UPDATE_GRAPH);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				views.setOnClickPendingIntent(R.id.widget_legend, pendingIntent);
				
				// Action open Munin for Android
				Intent intent2 = new Intent(context, Widget_GraphWidget_WidgetProvider.class);
				intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
				intent2.setAction(ACTION_START_ACTIVITY);
				PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, widgetId, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
				views.setOnClickPendingIntent(R.id.widget_graph, pendingIntent2);
				
				if (!graphWidget.isWifiOnly() || forceUpdate) {
					// Launching Asyntask
					ApplyBitmap task = new ApplyBitmap(graphWidget, views, appWidgetManager, widgetId, context);
					task.execute();
				} else {
					// Automatic update -> let's check if on wifi or data
					ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					
					if (mWifi.isConnected())
						updateAppWidget(context, appWidgetManager, widgetId, true);
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
			switch (intent.getAction()) {
				case ACTION_UPDATE_GRAPH:
					// Check if connection is available
					if (Util.isOnline(context)) {
						Bundle extras = intent.getExtras();
						if (extras != null) {
							AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
							int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

							updateAppWidget(context, appWidgetManager, widgetId, true);
						}
					}
					break;
				case ACTION_START_ACTIVITY:
					Bundle extras = intent.getExtras();
					if (extras != null) {
						try {
							int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
							graphWidget = sqlite.dbHlpr.getGraphWidget(widgetId);
							Intent intent2 = new Intent(context, Activity_GraphView.class);
							intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent2.putExtra("server", graphWidget.getPlugin().getInstalledOn().getUrl());
							intent2.putExtra("plugin", graphWidget.getPlugin().getName());
							intent2.putExtra("period", graphWidget.getPeriod());
							context.startActivity(intent2);
						}
						catch (NullPointerException ex) {
							ex.printStackTrace();
						}
					}
					break;

				case ACTION_START_PREMIUM:
					Intent intent2 = new Intent(context, Activity_GoPremium.class);
					intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent2);
					break;
				default:
					super.onReceive(context, intent);
					break;
			}
		} else
			super.onReceive(context, intent);
	}
	
	private static class ApplyBitmap extends AsyncTask<Void, Integer, Void> {
		private Bitmap bm;

		private String url;
		private GraphWidget graphWidget;
		
		private RemoteViews views;
		private AppWidgetManager awm;
		private int widgetId;
		private Context context;
		
		public ApplyBitmap(GraphWidget graphWidget, RemoteViews v, AppWidgetManager a, int w, Context context) {
			super();
			this.url = graphWidget.getPlugin().getImgUrl(graphWidget.getPeriod());
			this.graphWidget = graphWidget;
			this.views = v;
			this.awm = a;
			this.widgetId = w;
			this.context = context;
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
			bm = graphWidget.getPlugin().getGraph(url, MuninFoo.getUserAgent(context));
			bm = Util.removeBitmapBorder(bm);
			if (graphWidget.getHideServerName())
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
			sqlite.dbHlpr.deleteGraphWidget(i);
	}
}