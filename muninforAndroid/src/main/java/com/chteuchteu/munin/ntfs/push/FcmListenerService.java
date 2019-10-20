package com.chteuchteu.munin.ntfs.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DatabaseHelper;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.obj.NotifIgnoreRule;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.ui.Activity_IgnoreNotification;
import com.chteuchteu.munin.ui.Activity_Main;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class FcmListenerService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_pref", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Settings.PrefKeys.Notifs_Push_regId.getKey(), token).apply();
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        //String from = message.getFrom();
        Map<String, String> data = message.getData();

        if (BuildConfig.DEBUG) {
            debugDataBundle(data);
        }

        // Check if this is a test notifications
        if (data.containsKey("test")) {
            sendTestNotification();
        }
        else {
            handleNotification(data);
        }
    }

    private void handleNotification(Map<String, String> data) {
        // [{"plugin":"plugin","host":"localhost.localdomain","category":"plugin_category","fields":[{"c":":2","level":"c","w":":1","extra":"","label":"submitted","value":"6.00"}],"group":"localdomain"}]

        // Check if notifications are enabled
        if (!Settings.getInstance(this).getBool(Settings.PrefKeys.Notifs_Push)) {
            return;
        }

        // Parse alerts
        String alerts = data.get("alerts");
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
        List<NotifIgnoreRule> notifIgnoreRules = databaseHelper.getNotifIgnoreRules(group, host, plugin);

        if (BuildConfig.DEBUG) {
            MuninFoo.log("Found " + notifIgnoreRules.size() + " rules:");
            for (NotifIgnoreRule rule : notifIgnoreRules)
                MuninFoo.log(rule.toString());
        }

        // Notification is ignored if ignoredNotifications list is not empty
        return notifIgnoreRules.size() > 0;
    }

    private void debugDataBundle(Map<String, String> data) {
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

        // Field & value can both be null. In that case, just display the plugin name
        String title = field != null ? plugin + "." + field + " = " + value : plugin;
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

        this.withNotificationChannel(notificationBuilder);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void sendTestNotification() {
        Intent intent = new Intent(this, Activity_Main.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_alert_box_white)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(getString(R.string.notifications_test_title))
                .setContentText(getString(R.string.notifications_test_text))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        this.withNotificationChannel(notificationBuilder);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1 /* ID of notification */, notificationBuilder.build());
    }

    private void withNotificationChannel(NotificationCompat.Builder builder)
    {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "mfa_channel";
        CharSequence channelName = getString(R.string.notificationsTitle);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

        builder.setChannelId(channelId);
    }

    private static int getUniqueNotificationId() {
        long time = new Date().getTime();
        String sTime = String.valueOf(time);
        return Integer.valueOf(sTime.substring(sTime.length() - 5));
    }
}
