package com.chteuchteu.munin;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;

import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninPlugin.AlertState;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_Alerts;

import java.util.ArrayList;
import java.util.List;

public class Service_Notifications extends Service {
	private WakeLock mWakeLock;
	
	/**
	 * Simply return null, since our Service will not be communicating with
	 * any other components. It just does its work silently.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@SuppressWarnings("deprecation")
	private void handleIntent(Intent intent) {
		// obtain the wake lock
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.chteuchteu.munin");
		mWakeLock.acquire();
		
		// check the global background data setting
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (!cm.getBackgroundDataSetting()) {
			stopSelf();
			return;
		}
		
		NetworkInfo mWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		boolean wifiOnly = Util.getPref(Service_Notifications.this, "notifs_wifiOnly").equals("true");
		
		if (!wifiOnly || mWifi.isConnected())
			new PollTask().execute();
		else {
			if (mWakeLock.isHeld())
				mWakeLock.release();
			stopSelf();
		}
	}
	
	private class PollTask extends AsyncTask<Void, Void, Void> {
		private int nbCriticals;
		private int nbWarnings;
		private int nbServers;
		private String criticalPlugins;
		private String warningPlugins;
		
		@Override
		protected Void doInBackground(Void... params) {
			List<MuninServer> servers = new ArrayList<MuninServer>();
			String serversList = Util.getPref(Service_Notifications.this, "notifs_serversList");
			String[] serversToWatch = serversList.split(";");

			DatabaseHelper dbHelper = new DatabaseHelper(Service_Notifications.this);
			List<MuninMaster> masters = new ArrayList<MuninMaster>();
			List<MuninServer> dbServers = dbHelper.getServers(masters);
			
			nbCriticals = 0;
			nbWarnings = 0;
			nbServers = 0;
			criticalPlugins = "";
			warningPlugins = "";
			
			for (MuninServer s: dbServers) {
				for (String url : serversToWatch) {
					if (s.equalsApprox(url))
						servers.add(s);
				}
			}
			
			for (MuninServer s: servers)
				s.fetchPluginsStates(MuninFoo.getUserAgent(Service_Notifications.this));
			
			
			for (MuninServer s: servers) {
				boolean throatingServer = false;
				for (MuninPlugin p: s.getPlugins()) {
					if (p != null) {
						if (p.getState() == AlertState.CRITICAL || p.getState() == AlertState.WARNING)
							throatingServer = true;
						if (p.getState() == AlertState.CRITICAL) {
							criticalPlugins = criticalPlugins + p.getFancyName() + ", ";
							nbCriticals++;
						}
						else if (p.getState() == AlertState.WARNING) {
							warningPlugins = warningPlugins + p.getFancyName() + ", ";
							nbWarnings++;
						}
					}
				}
				if (throatingServer)
					nbServers++;
			}
			
			return null;
		}
		
		@SuppressWarnings("deprecation")
		@Override
		protected void onPostExecute(Void result) {
			//<string name="text58"> critical / criticals /&amp;amp; / warning / warnings /on / server/ servers</string>
			String[] strings = getString(R.string.text58).split("/");
			
			String notifTitle = "";
			if (nbCriticals > 0 && nbWarnings > 0) {
				notifTitle = nbCriticals + "";
				if (nbCriticals == 1)
					notifTitle += " " + strings[0];
				else
					notifTitle += strings[1];
				notifTitle += strings[2];
				notifTitle += nbWarnings;
				if (nbWarnings == 1)
					notifTitle += strings[3];
				else
					notifTitle += strings[4];
				notifTitle += strings[5];
				notifTitle += nbServers;
				if (nbServers == 1)
					notifTitle += strings[6];
				else
					notifTitle += strings[7];
				//String titreNotification = nbCriticals + " criticals & " + nbWarnings + " warnings on " + nbServers + " servers";
			} else if (nbCriticals == 0 && nbWarnings > 0) {
				notifTitle = nbWarnings + "";
				if (nbWarnings == 1)
					notifTitle += strings[3];
				else
					notifTitle += strings[4];
				notifTitle += strings[5];
				notifTitle += nbServers;
				if (nbServers == 1)
					notifTitle += strings[6];
				else
					notifTitle += strings[7];
			} else if (nbCriticals > 0 && nbWarnings == 0) {
				notifTitle = nbCriticals + "";
				if (nbCriticals == 1)
					notifTitle += " " + strings[0];
				else
					notifTitle += strings[1];
				notifTitle += strings[5];
				notifTitle += nbServers;
				if (nbServers == 1)
					notifTitle += strings[6];
				else
					notifTitle += strings[7];
			}
			
			String notifText;
			
			if (criticalPlugins.length() > 2 && criticalPlugins.substring(criticalPlugins.length()-2).equals(", "))
				criticalPlugins = criticalPlugins.substring(0, criticalPlugins.length()-2);
			if (warningPlugins.length() > 2 && warningPlugins.substring(warningPlugins.length()-2).equals(", "))
				warningPlugins = warningPlugins.substring(0, warningPlugins.length()-2);
			
			if (nbCriticals > 0 && nbWarnings > 0)
				notifText = criticalPlugins + ", " + warningPlugins;
			else if (nbCriticals > 0)
				notifText = criticalPlugins;
			else
				notifText = warningPlugins;
			
			if (nbCriticals > 0 || nbWarnings > 0) {
				if (!Util.getPref(Service_Notifications.this, "lastNotificationText").equals(notifText)) {
					NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
					Notification notification = new Notification(R.drawable.launcher_icon_mono, getString(R.string.app_name), System.currentTimeMillis());
					
					PendingIntent pendingIntent = PendingIntent.getActivity(Service_Notifications.this, 0, new Intent(Service_Notifications.this, Activity_Alerts.class), 0);
					notification.setLatestEventInfo(Service_Notifications.this, notifTitle, notifText, pendingIntent);

					// Dismiss notification on click
					notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
					
					Util.setPref(Service_Notifications.this, "lastNotificationText", notifText);

					if (Util.getPref(Service_Notifications.this, "notifs_vibrate").equals("true"))
						vibrate();
					
					notificationManager.notify(1234, notification);
				}
			} else {
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.cancel(1234);
			}
			
			// Important : release wake lock in the end
			stopSelf();
			if (mWakeLock.isHeld())
				mWakeLock.release();
		}
	}

	private void vibrate() {
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (v.hasVibrator())
			v.vibrate(500);
	}
	
	/**
	 * Returning START_NOT_STICKY tells the system to not restart the
	 * service if it is killed because of poor resource (memory/cpu) conditions.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleIntent(intent);
		return START_NOT_STICKY;
	}
	
	/**
	 * In onDestroy() we release our wake lock. This ensures that whenever the
	 * Service stops (killed for resources, stopSelf() called, etc.), the wake
	 * lock will be released.
	 */
	public void onDestroy() {
		super.onDestroy();
		if (mWakeLock.isHeld())
			mWakeLock.release();
	}
}