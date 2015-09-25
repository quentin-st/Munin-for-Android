package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Locale;

public class Settings {
    private static Settings instance;

    private SharedPreferences sharedPreferences;

    private HashMap<PrefKeys, String> stringPrefs;
    private HashMap<PrefKeys, Boolean> boolPrefs;
    private HashMap<PrefKeys, Integer> intPrefs;

    private Settings(Context context) {
        this.sharedPreferences = context.getSharedPreferences("user_pref", Context.MODE_PRIVATE);

        this.stringPrefs = new HashMap<>();
        this.boolPrefs = new HashMap<>();
        this.intPrefs = new HashMap<>();
    }

    public static synchronized Settings init(Context context) {
        instance = new Settings(context);
        return instance;
    }
    public static synchronized Settings getInstance() { return instance; }


    // SET
    public void set(PrefKeys key, boolean value) {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putBoolean(key.getKey(), value);
        editor.apply();

        // Update cached val
        this.boolPrefs.put(key, value);
    }

    public void set(PrefKeys key, String value) {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putString(key.getKey(), value);
        editor.apply();

        // Update cached val
        this.stringPrefs.put(key, value);
    }

    public void set(PrefKeys key, int value) {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putInt(key.getKey(), value);
        editor.apply();

        // Update cached val
        this.intPrefs.put(key, value);
    }


    // GET
    public boolean getBool(PrefKeys key) {
        Object defaultValue = getDefaultValue(key);
        return getBool(key, defaultValue != null && (boolean) defaultValue);
    }
    public boolean getBool(PrefKeys key, boolean defaultValue) {
        // Cache hit
        if (this.boolPrefs.keySet().contains(key))
            return this.boolPrefs.get(key);

        return this.sharedPreferences.getBoolean(key.getKey(), defaultValue);
    }

    public String getString(PrefKeys key) { return getString(key, null); }
    public String getString(PrefKeys key, String defaultValue) {
        // Cache hit
        if (this.stringPrefs.keySet().contains(key))
            return this.stringPrefs.get(key);

        if (this.sharedPreferences.contains(key.getKey()))
            return this.sharedPreferences.getString(key.getKey(), defaultValue);

        if (defaultValue != null)
            return defaultValue;

        return (String) getDefaultValue(key);
    }

    public int getInt(PrefKeys key) {
        Object defaultValue = getDefaultValue(key);
        return getInt(key, defaultValue != null ? (int) defaultValue : -1);
    }
    public int getInt(PrefKeys key, int defaultValue) {
        // Cache hit
        if (this.intPrefs.keySet().contains(key))
            return this.intPrefs.get(key);

        return this.sharedPreferences.getInt(key.getKey(), defaultValue);
    }

    private Object getDefaultValue(PrefKeys key) {
        switch (key) {
            case Lang:
                Locale.getDefault().getLanguage();
            case GraphviewOrientation:
                return "auto";
            case DefaultScale:
                return "day";
            case GraphsZoom:
                return true;
            default:
                return null;
        }
    }


    // REMOVE
    public void remove(PrefKeys key) {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.remove(key.getKey());
        editor.apply();

        // Update cache
        if (this.boolPrefs.keySet().contains(key))
            this.boolPrefs.remove(key);
        else if (this.stringPrefs.keySet().contains(key))
            this.stringPrefs.remove(key);
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
