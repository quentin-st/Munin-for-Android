package com.chteuchteu.munin.wdget;


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.AlertsWidget;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.ArrayList;
import java.util.List;

public class Widget_AlertsWidget_ViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	private List<MuninServer> servers;
	private Context context;
	private boolean pluginsStatesFetched;
	private int widgetId;

	public Widget_AlertsWidget_ViewsFactory(Context context, Intent intent) {
		this.context = context;
		this.widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		this.servers = new ArrayList<MuninServer>();
		this.pluginsStatesFetched = false;

		refresh();
	}

	private void refresh() {
		this.servers.clear();
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		Util.setPref(context, "widget2_forceUpdate", "false");
		new PluginsStatesFetcher(appWidgetManager, widgetId).execute();
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

			for (MuninServer server : alertsWidget.getServers())
				server.fetchPluginsStates();

			// Update servers list according to those results
			servers.clear();
			for (MuninServer server : alertsWidget.getServers()) {
				if (server.reachable != Util.SpecialBool.TRUE || server.getErroredPlugins().size() > 0 || server.getWarnedPlugins().size() > 0)
					servers.add(server);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			pluginsStatesFetched = true;
			appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.servers);
		}
	}

	@Override
	public void onCreate() { }

	@Override
	public void onDestroy() { }

	@Override
	public int getCount() {
		if (!pluginsStatesFetched)
			return 1; // Fake view
		else
			return servers.size();
	}

	@Override
	public RemoteViews getViewAt(int position) {
		RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_alertswidget_part);

		if (!pluginsStatesFetched && position == 0) // Loading
			row.setTextViewText(R.id.item, context.getString(R.string.loading));
		else { // Loading finished : display data
			MuninServer server = servers.get(position);
			int nbWarnings = server.getWarnedPlugins().size();
			int nbErrors = server.getErroredPlugins().size();

			// Check reason
			if (server.reachable != Util.SpecialBool.TRUE) {
				// Set line in gray, hide both line1 and line2
			} else if (nbErrors > 0 && nbWarnings > 0) {
				// Set line in red ; line1 = X criticals && X warnings
				// line2 = errors = , ... (cf Activity_Alerts)
			} else if (nbErrors > 0 && nbWarnings == 0) {
				// Set line in ref, line1 = X criticals
				// line2 = errors = ...
			} else if (nbErrors == 0 && nbWarnings > 0) {
				// Set line in orange, line1 = X warnings
				// line2 = warnings = ...
			}

			row.setTextViewText(R.id.item, server.getName());
			row.setTextViewText(R.id.item2, server.getWarnedPlugins().size() + " warning(s) & " + server.getErroredPlugins().size() + " critical(s)");
		}

		// Hide separator if needed
		if (!pluginsStatesFetched && position == 0
				|| position == servers.size()-1)
			row.setViewVisibility(R.id.separator, View.GONE);

		// Set onclick : open Activity_Alerts
		Intent openIntent = new Intent();
		openIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		openIntent.setAction(Widget_AlertsWidget_WidgetProvider.ACTION_START_ACTIVITY);
		row.setOnClickFillInIntent(R.id.line, openIntent);

		return row;
	}

	@Override
	public void onDataSetChanged() {
		// Called from Widget_AlertsWidget_WidgetProvider on refresh button click
		if (!pluginsStatesFetched || Util.getPref(context, "widget2_forceUpdate").equals("true"))
			refresh();
	}

	@Override
	public RemoteViews getLoadingView() { return null; }

	@Override
	public int getViewTypeCount() { return 1; }

	@Override
	public long getItemId(int position) { return position; }

	@Override
	public boolean hasStableIds() { return true; }
}
