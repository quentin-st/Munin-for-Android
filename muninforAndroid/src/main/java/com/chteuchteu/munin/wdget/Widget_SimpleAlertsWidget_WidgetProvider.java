package com.chteuchteu.munin.wdget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.SQLite;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.AlertsWidget;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.SimpleAlertsWidget;
import com.chteuchteu.munin.ui.Activity_GoPremium;
import com.chteuchteu.munin.ui.Activity_GraphView;

import java.util.ArrayList;
import java.util.List;

public class Widget_SimpleAlertsWidget_WidgetProvider extends AppWidgetProvider {
    private static final String ACTION_UPDATE_GRAPH = "com.chteuchteu.munin.widget.ALERTS_REFRESH";
    private static final String ACTION_START_ACTIVITY = "com.chteuchteu.munin.widget.START_ACTIVITY";
    private static final String ACTION_START_PREMIUM = "com.chteuchteu.munin.widget.START_PREMIUM";

    private SQLite sqlite;
    private Context context;
    private SimpleAlertsWidget widget;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        sqlite = new SQLite(context, MuninFoo.getInstance(context));

        // Get all ids
        ComponentName thisWidget = new ComponentName(context, Widget_GraphWidget_WidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // Perform this loop procedure for each widget that belongs to this provider
        for (Integer i : allWidgetIds)
            updateAppWidget(context, appWidgetManager, i, false);
    }

    public void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int widgetId, boolean forceUpdate) {
        boolean premium = MuninFoo.isPremium(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simplealertswidget_layout);
        if (!premium) {
            Toast.makeText(context, R.string.featuresPackNeeded, Toast.LENGTH_SHORT).show();

            appWidgetManager.updateAppWidget(widgetId, views);
        } else {
            if (sqlite == null)
                sqlite = new SQLite(context, MuninFoo.getInstance(context));
            widget = (SimpleAlertsWidget) sqlite.dbHlpr.getAlertsWidget(widgetId, null);

            if (widget != null && widget.getNodes().size() > 0) {
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

                if (!widget.isWifiOnly() || forceUpdate) {
                    // Launching Asyntask
                    PluginsStatesFetcher task = new PluginsStatesFetcher(appWidgetManager, widgetId);
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
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
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
                            widget = (SimpleAlertsWidget) sqlite.dbHlpr.getAlertsWidget(widgetId, null);
                            Intent intent2 = new Intent(context, Activity_GraphView.class);
                            intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent2.putExtra("node", graphWidget.getPlugin().getInstalledOn().getUrl());
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

            // Remove nodes duplicates (duplicated in db for no reason)
            List<MuninNode> newNodesList = new ArrayList<>();
            for (MuninNode node : alertsWidget.getNodes()) {
                if (!newNodesList.contains(node))
                    newNodesList.add(node);
            }

            for (MuninNode node : newNodesList)
                node.fetchPluginsStates(MuninFoo.getUserAgent(context));

            // Update nodes list according to those results
            nodes.clear();
            for (MuninNode node : newNodesList) {
                if (node.reachable != Util.SpecialBool.TRUE || node.getErroredPlugins().size() > 0 || node.getWarnedPlugins().size() > 0)
                    nodes.add(node);
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
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (sqlite == null)
            sqlite = new SQLite(context, MuninFoo.getInstance(context));
        for (int i : appWidgetIds)
            sqlite.dbHlpr.deleteAlertsWidget(i);
    }
}
