package com.chteuchteu.munin;


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.obj.AlertsWidget;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.ArrayList;
import java.util.List;

public class Widget_AlertsWidget_ViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	private static List<MuninServer> servers;
	private Context context;
	private int appWidgetId;

	public Widget_AlertsWidget_ViewsFactory(Context context, Intent intent) {
		this.context = context;
		this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		this.servers = new ArrayList<MuninServer>();

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

		new PluginsStatesFetcher(appWidgetManager, appWidgetId).execute();
	}

	private class PluginsStatesFetcher extends AsyncTask<Void, Void, Void> {
		private AppWidgetManager appWidgetManager;
		private int appWidgetId;

		public PluginsStatesFetcher(AppWidgetManager appWidgetManager, int appWidgetId) {
			this.appWidgetManager = appWidgetManager;
			this.appWidgetId = appWidgetId;
		}

		@Override
		protected Void doInBackground(Void... params) {
			DatabaseHelper dbHelper = new DatabaseHelper(context);
			AlertsWidget alertsWidget = dbHelper.getAlertsWidget(appWidgetId, null);

			Log.i("", "fetching plugins states for " + alertsWidget.getServers().size() + " servers");
			for (MuninServer server : alertsWidget.getServers())
				server.fetchPluginsStates();

			Widget_AlertsWidget_ViewsFactory.servers = alertsWidget.getServers();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.servers);
		}
	}

	@Override
	public void onCreate() { }

	@Override
	public void onDestroy() { }

	@Override
	public int getCount() { return servers.size(); }

	@Override
	public RemoteViews getViewAt(int position) {
		Log.i("", "getViewAt(" + position + ")");
		RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_alertswidget_part);
		row.setTextViewText(R.id.item, servers.get(position).getName());

		Intent intent = new Intent();
		row.setOnClickFillInIntent(R.id.item, intent);

		return row;
	}

	@Override
	public RemoteViews getLoadingView() { return null; }

	@Override
	public int getViewTypeCount() { return 1; }

	@Override
	public long getItemId(int position) { return position; }

	@Override
	public boolean hasStableIds() { return true; }

	@Override
	public void onDataSetChanged() { }
}
