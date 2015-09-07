package com.chteuchteu.munin.wdget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.SQLite;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.AlertsWidget;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.ui.Activity_Alerts;
import com.chteuchteu.munin.ui.Activity_GoPremium;

import java.util.ArrayList;
import java.util.List;

public class Widget_SimpleAlertsWidget_WidgetProvider extends AppWidgetProvider {
    private static final String ACTION_UPDATE = "com.chteuchteu.munin.widget.ALERTS_REFRESH";
    private static final String ACTION_START_ACTIVITY = "com.chteuchteu.munin.widget.START_ACTIVITY";
    private static final String ACTION_START_PREMIUM = "com.chteuchteu.munin.widget.START_PREMIUM";

    private static SQLite sqlite;

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

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int widgetId, boolean forceUpdate) {
        boolean premium = MuninFoo.isPremium(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simplealertswidget_layout);
        if (!premium) {
            Toast.makeText(context, R.string.featuresPackNeeded, Toast.LENGTH_SHORT).show();

            appWidgetManager.updateAppWidget(widgetId, views);
        } else {
            if (sqlite == null)
                sqlite = new SQLite(context, MuninFoo.getInstance(context));
            AlertsWidget widget = sqlite.dbHlpr.getAlertsWidget(widgetId, null);

            if (widget != null && widget.getNodes().size() > 0) {
                // Update action
                Intent intent = new Intent(context, Widget_GraphWidget_WidgetProvider.class);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                intent.setAction(ACTION_UPDATE);
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
                    PluginsStatesFetcher task = new PluginsStatesFetcher(appWidgetManager, views, context, widgetId);
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
                case ACTION_UPDATE:
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
                            Intent intent2 = new Intent(context, Activity_Alerts.class);
                            intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

    private static class PluginsStatesFetcher extends AsyncTask<Void, Void, Void> {
        private AppWidgetManager manager;
        private RemoteViews views;
        private int appWidgetId;
        private Context context;

        private int nodes_ok;
        private int nodes_total;
        private int plugins_warning;
        private int plugins_error;

        public PluginsStatesFetcher(AppWidgetManager manager, RemoteViews views, Context context, int appWidgetId) {
            this.manager = manager;
            this.views = views;
            this.context = context;
            this.appWidgetId = appWidgetId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            nodes_total = nodes_ok = plugins_error = plugins_warning = 0;
            this.views.setTextViewText(R.id.alerts_ok, "?/" + this.nodes_total);
            this.views.setTextViewText(R.id.alerts_ok_label, context.getString(R.string.nodes));
            this.views.setTextViewText(R.id.alerts_warning, "?");
            this.views.setTextViewText(R.id.alerts_warning_label, context.getString(R.string.plugins));
            this.views.setTextViewText(R.id.alerts_error, "?");
            this.views.setTextViewText(R.id.alerts_error_label, context.getString(R.string.plugins));

            this.manager.updateAppWidget(new ComponentName(context, Widget_SimpleAlertsWidget_WidgetProvider.class), this.views);
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

            String userAgent = MuninFoo.getUserAgent(context);

            for (MuninNode node : newNodesList)
                node.fetchPluginsStates(userAgent);

            // Update nodes list according to those results
            for (MuninNode node : newNodesList) {
                nodes_total++;
                if (node.getErroredPlugins().isEmpty() && node.getWarnedPlugins().isEmpty())
                    nodes_ok++;
                else {
                    plugins_error += node.getErroredPlugins().size();
                    plugins_warning += node.getWarnedPlugins().size();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //this.views.setTextViewText(R.id.textview, "New text");
            this.views.setTextViewText(R.id.alerts_ok, this.nodes_ok + "/" + this.nodes_total);
            this.views.setTextViewText(R.id.alerts_ok_label, context.getString(this.nodes_ok == 1 ? R.string.node : R.string.nodes));
            this.views.setTextViewText(R.id.alerts_warning, String.valueOf(this.plugins_warning));
            this.views.setTextViewText(R.id.alerts_warning_label, context.getString(this.plugins_warning == 1 ? R.string.plugin : R.string.plugins));
            this.views.setTextViewText(R.id.alerts_error, String.valueOf(this.plugins_error));
            this.views.setTextViewText(R.id.alerts_error_label, context.getString(this.plugins_error == 1 ? R.string.plugin : R.string.plugins));

            this.manager.updateAppWidget(new ComponentName(context, Widget_SimpleAlertsWidget_WidgetProvider.class), this.views);
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
