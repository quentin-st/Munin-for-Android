package com.chteuchteu.munin.ntfs;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.obj.IgnoredNotification;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.ui.Activity_IgnoreNotification;
import com.chteuchteu.munin.ui.Activity_Main;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService {

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        if (BuildConfig.DEBUG)
            debugDataBundle(data);

        // Check if this is a test notifications
        if (data.getString("test", "false").equals("true"))
            sendTestNotification();
        else
            handleNotification(data);
    }

    private void handleNotification(Bundle data) {
        // [{"plugin":"plugin","host":"localhost.localdomain","category":"plugin_category","fields":[{"c":":2","level":"c","w":":1","extra":"","label":"submitted","value":"6.00"}],"group":"localdomain"}]

        // Check if notifications are enabled
        if (!Settings.getInstance(this).getBool(Settings.PrefKeys.Notifications))
            return;

        // Parse alerts
        String alerts = data.getString("alerts");
        try {
            JSONArray alertsArray = new JSONArray(alerts);

            for (int i=0; i<alertsArray.length(); i++) {
                JSONObject alert = alertsArray.getJSONObject(i);

                String group = alert.getString("group");
                String host = alert.getString("host");
                //String category = alert.getString("category");
                String plugin = alert.getString("plugin");

                // Get alert level
                MuninPlugin.AlertState alertLevel = MuninPlugin.AlertState.UNDEFINED;
                String value = null;
                String fieldName = null;

                JSONArray fields = alert.getJSONArray("fields");

                if (fields.length() != 0) {
                    JSONObject field = fields.getJSONObject(0);

                    value = field.getString("value");
                    fieldName = field.getString("label");

                    String level = fields.getJSONObject(0).getString("level");
                    switch (level) {
                        case "c":
                            alertLevel = MuninPlugin.AlertState.CRITICAL;
                            break;
                        case "w":
                            alertLevel = MuninPlugin.AlertState.WARNING;
                            break;
                        case "u":
                            alertLevel = MuninPlugin.AlertState.UNKNOWN;
                            break;
                    }
                }

                if (!isNotificationIgnored(group, host, plugin))
                    sendNotification(group, host, plugin, value, fieldName, alertLevel);
            }

        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private boolean isNotificationIgnored(String group, String host, String plugin) {
        DatabaseHelper databaseHelper = MuninFoo.getInstance(this).sqlite.dbHlpr;
        List<IgnoredNotification> ignoredNotifications = databaseHelper.getIgnoredNotifications(group, host, plugin);

        if (BuildConfig.DEBUG) {
            MuninFoo.log("Found " + ignoredNotifications.size() + " rules:");
            for (IgnoredNotification rule : ignoredNotifications)
                MuninFoo.log(rule.toString());
        }

        // Notification is ignored if ignoredNotifications list is not empty
        return ignoredNotifications.size() > 0;
    }

    private void debugDataBundle(Bundle data) {
        for (String key : data.keySet()) {
            Object object = data.get(key);

            if (object != null)
                MuninFoo.log("-> " + key + " = " + object.toString() + " (is a " + object.getClass().getSimpleName() + ")");
        }
    }

    /**
     * Create and show a notification for an alert
     */
    private void sendNotification(String group, String host, String plugin, String value, String field, MuninPlugin.AlertState alertLevel) {
        int notificationId = getUniqueNotificationId();

        // Open app intent
        Intent intent = new Intent(this, Activity_Main.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        // Ignore intent
        Intent ignoreIntent = new Intent(this, Activity_IgnoreNotification.class);
        ignoreIntent.putExtra(Activity_IgnoreNotification.EXTRA_NOTIFICATION_ID, notificationId);
        ignoreIntent.putExtra(Activity_IgnoreNotification.EXTRA_GROUP, group);
        ignoreIntent.putExtra(Activity_IgnoreNotification.EXTRA_HOST, host);
        ignoreIntent.putExtra(Activity_IgnoreNotification.EXTRA_PLUGIN, plugin);
        ignoreIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent ignorePendingIntent = PendingIntent.getActivity(this, 1, ignoreIntent, PendingIntent.FLAG_ONE_SHOT);

        String title = plugin + "." + field + " = " + value;
        String text = group + " - " + host;

        int iLargeIcon = alertLevel == MuninPlugin.AlertState.CRITICAL
                ? R.drawable.ic_action_alert_critical
                : R.drawable.ic_action_alert_warning;
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), iLargeIcon);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_alert_box_white)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(alertLevel == MuninPlugin.AlertState.CRITICAL
                        ? NotificationCompat.PRIORITY_HIGH
                        : NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_bookmark_remove_grey600, getString(R.string.ignore), ignorePendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void sendTestNotification() {
        Intent intent = new Intent(this, Activity_Main.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_alert_box_white)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(getString(R.string.notifications_test_title))
                .setContentText(getString(R.string.notifications_test_text))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1 /* ID of notification */, notificationBuilder.build());
    }

    private static int getUniqueNotificationId() {
        long time = new Date().getTime();
        String sTime = String.valueOf(time);
        return Integer.valueOf(sTime.substring(sTime.length() - 5));
    }
}
