package com.chteuchteu.munin.ntfs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.chteuchteu.munin.async.PollTask;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;

/**
 * Notifications Service
 * Launched when enabling notifications from the app or
 * on device boot using BootReceiver class
 */
public class Service_Notifications extends Service {
	public WakeLock wakeLock;

	/**
	 * Simply return null, since our Service will not be communicating with
	 * any other components. It just does its work silently.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void handleIntent() {
		boolean isWifiConnected = Util.isWifiConnected(this);
		boolean wifiOnly = Settings.getInstance(this).getBool(Settings.PrefKeys.Notifs_WifiOnly);

		if (wifiOnly && !isWifiConnected)
			stopSelf();

		// Obtain the wake lock
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName());
		wakeLock.acquire();


		new PollTask(this).execute();
	}

	/**
	 * Returning START_NOT_STICKY tells the system to not restart the
	 * service if it is killed because of poor resource (memory/cpu) conditions.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleIntent();
		return START_NOT_STICKY;
	}

	/**
	 * In onDestroy() we release our wake lock. This ensures that whenever the
	 * Service stops (killed for resources, stopSelf() called, etc.), the wake
	 * lock will be released.
	 */
	public void onDestroy() {
		super.onDestroy();
		if (wakeLock.isHeld())
			wakeLock.release();
	}
}
