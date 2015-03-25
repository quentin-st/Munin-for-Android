package com.chteuchteu.munin.ntfs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.chteuchteu.munin.hlpr.Util;

/**
 * BootReceiver called by Android system on device launch
 *  (we use it to start the notifications service)
 */
public class BootReceiver extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		// In our case intent will always be BOOT_COMPLETED, so we can just set the alarm
		if (Util.getPref(context, Util.PrefKeys.Notifications).equals("true")) {
			int min = 0;

			String refreshRate = Util.getPref(context, Util.PrefKeys.Notifs_RefreshRate);
			if (!refreshRate.equals(""))
				min = Integer.parseInt(refreshRate);
			
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(context, Service_Notifications.class);
			PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
			am.cancel(pi);
			
			if (min > 0) {
				am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime() + min*60*1000,
						min*60*1000, pi);
			}
		}
	}
}