package com.chteuchteu.munin.ntfs.poll;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.chteuchteu.munin.hlpr.Settings;

/**
 * BootReceiver called by Android system on device launch
 *  (we use it to start the notifications service)
 */
public class BootReceiver extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		Settings settings = Settings.getInstance(context);

		if (!settings.getBool(Settings.PrefKeys.Notifs_Poll))
			return;

		int min = settings.getInt(Settings.PrefKeys.Notifs_Poll_RefreshRate);

		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, Service_PollNotifications.class);
		PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
		am.cancel(pi);

		if (min > 0) {
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + min * 60 * 1000,
					min * 60 * 1000, pi);
		}
	}
}
