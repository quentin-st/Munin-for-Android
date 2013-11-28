package com.chteuchteu.munin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

public class Widget_GraphWidget extends AppWidgetProvider {
	public static RemoteViews 		views;
	public static AppWidgetManager 	awm;
	public static int				widgetId;
	public static final String ACTION_UPDATE_GRAPH = "com.chteuchteu.munin.widget.UPDATE_GRAPH";
	public static final String ACTION_START_ACTIVITY = "com.chteuchteu.munin.widget.START_ACTIVITY";
	public static final String ACTION_START_PREMIUM = "com.chteuchteu.munin.widget.START_PREMIUM";
	
	public static SQLite sqlite = new SQLite(MuninFoo.getInstance());
	public static MuninWidget widget;
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// Get all ids
		ComponentName thisWidget = new ComponentName(context, Widget_GraphWidget.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		
		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int i = 0; i < allWidgetIds.length; i++) {
			updateAppWidget(context, appWidgetManager, allWidgetIds[i], false);
		}
	}
	
	/**
	 * Update the widget
	 *
	 * @param context
	 * @param appWidgetManager
	 * @param appWidgetId
	 */
	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean forceUpdate) {
		awm = appWidgetManager;
		widgetId = appWidgetId;
		
		boolean premium = checkPremium(context);
		
		// Mise à jour du widget
		views = new RemoteViews(context.getPackageName(), R.layout.graphwidget_layout);
		if (!premium){
			//views.setTextViewText(R.id.widget_pluginname, "Munin for Android Features Pack needed");
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
			
			//MuninServer serv = null;
			//String pluginName = "";
			//String serverName = "";
			
			/*for (MuninWidget w : sqlite.getWidgets()) {
				if (w.getWidgetId() == appWidgetId)
					widget = w;
			}*/
			
			widget = sqlite.getWidget(appWidgetId);

			/*if (widget != null) {
				// Chargement à partir des paramètres
				serv = new MuninServer(getPref("widget" + appWidgetId+ "_Name", context), getPref("widget" + appWidgetId+ "_Url", context));
				List<MuninPlugin> a = new ArrayList<MuninPlugin>();
				a.add(new MuninPlugin(getPref("widget" + appWidgetId+ "_GraphName", context), serv));
				serv.setPluginsList(a);
				serv.getPlugin(0).setFancyName(getPref("widget" + appWidgetId+ "_GraphFancyName", context));
				
				if (!getPref("widget" + appWidgetId+ "_AuthLogin", context).equals("") || !getPref("widget" + appWidgetId+ "_AuthPassword", context).equals(""))
					serv.setAuthIds(getPref("widget" + appWidgetId+ "_AuthLogin", context), getPref("widget" + appWidgetId+ "_AuthPassword", context));
				if (getPref("widget" + appWidgetId + "_SSL", context).equals("true"))
					serv.setSSL(true);
				
				
				pluginName = serv.getPlugin(0).getFancyName();
				serverName = serv.getName();
			}*/
			
			//if (serv != null) {
			if (widget != null) {
				//views.setTextViewText(R.id.widget_pluginname, pluginName);
				//views.setTextViewText(R.id.widget_servername, serverName);
				
				//views.setTextViewText(R.id.widget_pluginname, widget.getPlugin().getFancyName());
				views.setTextViewText(R.id.widget_servername, widget.getServer().getName());
				
				// Action update
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
				
				
				//boolean wifiOnly = false;
				//if (getPref("widget" + appWidgetId + "_WifiOnly", context).equals("true"))
				//	wifiOnly = true;
				
				//if (!wifiOnly || forceUpdate) {
				if (!widget.isWifiOnly() || forceUpdate) {
					// Lancement de l'Asyntask
					//applyBitmap task = new applyBitmap((MuninServer) serv, getPref("widget" + appWidgetId + "_GraphURL", context), views, awm, appWidgetId);
					applyBitmap task = new applyBitmap((MuninServer) widget.getServer(), widget.getPlugin().getImgUrl(widget.getPeriod()), views, awm, appWidgetId);
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
		
		if (intent.getAction() != null) {
			if (intent.getAction().equals(ACTION_UPDATE_GRAPH)) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
					int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
					
					updateAppWidget(context, appWidgetManager, widgetId, true);
				}
			} else if (intent.getAction().equals(ACTION_START_ACTIVITY)) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					if (widget == null || (widget != null && (widget.getServer() == null || widget.getPlugin() == null)))
						widget = sqlite.getWidget(extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
					Intent intent2 = new Intent(context, Activity_GraphView.class);
					intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					//int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
					//intent2.putExtra("server", getPref("widget" + widgetId + "_Name", context));
					//intent2.putExtra("plugin", getPref("widget" + widgetId + "_GraphName", context));
					//intent2.putExtra("period", getPref("widget" + widgetId + "_Period", context));
					intent2.putExtra("server", widget.getServer().getServerUrl());
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
		//updateAppWidget(context, appWidgetManager, appWidgetId, true);
		/*PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName("com.android.settings",
                                        ".widget.SettingsAppWidgetProvider"),
                                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                        PackageManager.DONT_KILL_APP);*/
	}
	
	static class applyBitmap extends AsyncTask<Void, Integer, Void> {
		private Bitmap		bm;
		
		private MuninServer	serv;
		private String		url;
		
		private RemoteViews	views;
		private AppWidgetManager awm;
		private	int			widgetId;
		
		public applyBitmap(MuninServer s, String ad, RemoteViews v, AppWidgetManager a, int w) {
			super();
			this.serv = s;
			this.url = ad;
			this.views = v;
			this.awm = a;
			this.widgetId = w;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			//this.views.setViewVisibility(R.id.widget_pluginname, View.GONE);
			this.views.setViewVisibility(R.id.widget_servername, View.GONE);
			this.views.setViewVisibility(R.id.widget_loading, View.VISIBLE);
			this.awm.updateAppWidget(this.widgetId, this.views);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			bm = serv.getPlugin(0).getGraph(url);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (this.bm != null)
				this.views.setBitmap(R.id.widget_graph, "setImageBitmap", this.bm);
			this.views.setViewVisibility(R.id.widget_loading, View.GONE);
			//this.views.setViewVisibility(R.id.widget_pluginname, View.VISIBLE);
			this.views.setViewVisibility(R.id.widget_servername, View.VISIBLE);
			this.awm.updateAppWidget(this.widgetId, this.views);
		}
	}
	
	public static boolean checkPremium(Context context) {
		if (isPackageInstalled("com.chteuchteu.muninforandroidfeaturespack", context)) {
			PackageManager manager = context.getPackageManager();
			if (manager.checkSignatures("com.chteuchteu.munin", "com.chteuchteu.muninforandroidfeaturespack")
					== PackageManager.SIGNATURE_MATCH) {
				return true;
			}
			return false;
		}
		return false;
	}
	public static boolean isPackageInstalled (String packageName, Context context) {
		PackageManager pm = context.getPackageManager();
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return false;
		}
		return true;
	}
	
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		// called when widgets are deleted
		// array of widgetIds -> iteration
		/*String[] prefs = {"_Name", "_Url", "_AuthLogin", "_AuthPassword", "_SSL", "_GraphURL", "_GraphName", "_GraphFrancyName"};
		for (int i=0; i<appWidgetIds.length; i++) {
			for (int y=0; y<prefs.length; y++)
				removePref("widget" + appWidgetIds[i] + prefs[y], context);
		}*/
		
		for (int i : appWidgetIds) {
			sqlite.deleteWidget(i);
		}
	}
	
	
	// SHARED PREFERENCES
	/*public static String getPref(String key, Context c) {
		return c.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	public static void removePref(String key, Context c) {
		SharedPreferences prefs = c.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		editor.commit();
	}*/
}