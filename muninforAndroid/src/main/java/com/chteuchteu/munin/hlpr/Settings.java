package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class Settings {
    private static Settings instance;

    private SharedPreferences sharedPreferences;

    private Settings(Context context) {
        this.sharedPreferences = context.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
    }

    public static synchronized void init(Context context) { instance = new Settings(context);}
    public static synchronized Settings getInstance() { return instance; }


    // SET
    public void set(PrefKeys key, boolean value) {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putBoolean(key.getKey(), value);
        editor.apply();
    }

    public void set(PrefKeys key, String value) {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putString(key.getKey(), value);
        editor.apply();
    }


    // GET
    public boolean getBool(PrefKeys key) { return getBool(key, false); }
    public boolean getBool(PrefKeys key, boolean defaultValue) {
        return this.sharedPreferences.getBoolean(key.getKey(), defaultValue);
    }

    public String getString(PrefKeys key) { return getString(key, null); }
    public String getString(PrefKeys key, String defaultValue) {
        return this.sharedPreferences.getString(key.getKey(), defaultValue);
    }


    // REMOVE
    public void remove(PrefKeys key) {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.remove(key.getKey());
        editor.apply();
    }


    // HAS
    public boolean has(PrefKeys key) {
        return this.sharedPreferences.contains(key.getKey());
    }


    public enum PrefKeys {
        GraphviewOrientation("graphview_orientation"),  Notifications("notifications"),
        ScreenAlwaysOn("screenAlwaysOn"),                 Notifs_RefreshRate("notifs_refreshRate"),
        DefaultScale("defaultScale"),                      Notifs_NodesList("notifs_serversList"),
        LastMFAVersion("lastMFAVersion"),				  Notifs_WifiOnly("notifs_wifiOnly"),
        Notifs_Vibrate("notifs_vibrate"),
        Notifs_LastNotificationText("lastNotificationText"),

        AutoRefresh("autoRefresh"),                        UserAgent("userAgent"), UserAgentChanged("userAgentChanged"),
        HDGraphs("hdGraphs"),                               Lang("lang"),
        GraphsZoom("graphsZoom"),                          DefaultNode("defaultServer"),
        GridsLegend("gridsLegend"),						 DisableChromecast("disableChromecast"),

        Twitter_NbLaunches("twitter_nbLaunches"),        AddServer_History("addserver_history"),
        Widget2_ForceUpdate("widget2_forceUpdate"),      OpenSourceDialogShown("openSourceDialogShown"),
        I18NDialogShown("i18nDialogShown"),
        DefaultActivity("defaultActivity"),
        DefaultActivity_GridId("defaultActivity_gridId"),
        DefaultActivity_LabelId("defaultActivity_labelId"),

        ChromecastApplicationId("chromecastAppId"),
        ImportExportServer("importExportServer"),

        // Old prefs
        Drawer("drawer"), Splash("splash"), ListViewMode("listViewMode"), Transitions("transitions");

        private String key;
        PrefKeys(String k) { this.key = k; }

        public String getKey() { return this.key; }
    }
}
