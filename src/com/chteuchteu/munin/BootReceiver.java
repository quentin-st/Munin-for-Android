package com.chteuchteu.munin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.chteuchteu.munin.hlpr.Util;

public class BootReceiver extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		// In our case intent will always be BOOT_COMPLETED, so we can just set the alarm
		if (Util.getPref(context, "notifications").equals("true")) {
			int min = 0;
			if (!Util.getPref(context, "notifs_refreshRate").equals(""))
				min = Integer.parseInt(Util.getPref(context, "notifs_refreshRate"));
			
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