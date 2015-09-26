package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

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

    public static synchronized Settings getInstance(Context context) {
        if (instance == null)
            instance = new Settings(context);
        return instance;
    }


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
        Object defaultValue = key.getDefaultValue();
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

        return (String) key.getDefaultValue();
    }

    public int getInt(PrefKeys key) {
        Object defaultValue = key.getDefaultValue();
        return getInt(key, defaultValue != null ? (int) defaultValue : -1);
    }
    public int getInt(PrefKeys key, int defaultValue) {
        // Cache hit
        if (this.intPrefs.keySet().contains(key))
            return this.intPrefs.get(key);

        return this.sharedPreferences.getInt(key.getKey(), defaultValue);
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


    /**
     * For debug purpose, get raw shared preferences values
     * @return Map
     */
    public Map<String, ?> getAll() {
        return this.sharedPreferences.getAll();
    }


    // HAS
    public boolean has(PrefKeys key) {
        return this.sharedPreferences.contains(key.getKey());
    }

    public enum PrefKeys {
        ScreenAlwaysOn("screenAlwaysOn", false),
        DefaultScale("defaultScale", "day"),
        LastMFAVersion("lastMFAVersion", ""),
        Notifications("notifications", false),
        Notifs_RefreshRate("notifs_refreshRate", -1),
        Notifs_NodesList("notifs_serversList", ""),
        Notifs_WifiOnly("notifs_wifiOnly",  false),
        Notifs_Vibrate("notifs_vibrate", true),
        Notifs_LastNotificationText("lastNotificationText", ""),


        AutoRefresh("autoRefresh", false),
        HDGraphs("hdGraphs", false),
        GraphsZoom("graphsZoom", true),
        GridsLegend("gridsLegend", "pluginName"),
        UserAgent("userAgent", null),
        UserAgentChanged("userAgentChanged", false),
        Lang("lang", null),
        DefaultNode("defaultServer", null),
        DisableChromecast("disableChromecast", false),

        AddServer_History("addserver_history", null),
        Widget2_ForceUpdate("widget2_forceUpdate", false),
        I18NDialogShown("i18nDialogShown", false),
        DefaultActivity("defaultActivity", null),
        DefaultActivity_GridId("defaultActivity_gridId", -1),
        DefaultActivity_LabelId("defaultActivity_labelId", -1),

        ChromecastApplicationId("chromecastAppId", null),
        ImportExportServer("importExportServer", null);

        private String key;
        private Object defaultValue;
        PrefKeys(String key, Object defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String getKey() { return this.key; }
        public Object getDefaultValue() { return this.defaultValue; }
    }

    /**
     * Migrate from old settings (all-string) to this new shiny one
     */
    public void migrate() {
        // Screen always on
        String screenAlwaysOn = getString(PrefKeys.ScreenAlwaysOn);
        remove(PrefKeys.ScreenAlwaysOn);
        set(PrefKeys.ScreenAlwaysOn, screenAlwaysOn != null && screenAlwaysOn.equals("true"));

        // Auto-refresh
        String autoRefresh = getString(PrefKeys.AutoRefresh);
        remove(PrefKeys.AutoRefresh);
        set(PrefKeys.AutoRefresh, autoRefresh != null && autoRefresh.equals("true"));

        // Graph zoom
        String graphsZoom = getString(PrefKeys.GraphsZoom);
        remove(PrefKeys.GraphsZoom);
        set(PrefKeys.GraphsZoom, graphsZoom == null || graphsZoom.equals("true"));

        // HD graphs
        String hdGraphs = getString(PrefKeys.HDGraphs);
        remove(PrefKeys.HDGraphs);
        set(PrefKeys.HDGraphs, hdGraphs != null && hdGraphs.equals("true"));

        // Disable chromecast
        String disableChromecast = getString(PrefKeys.DisableChromecast);
        remove(PrefKeys.DisableChromecast);
        set(PrefKeys.DisableChromecast, disableChromecast != null && disableChromecast.equals("true"));

        // Default activity_grid
        String activity_grid = getString(PrefKeys.DefaultActivity_GridId);
        remove(PrefKeys.DefaultActivity_GridId);
        if (activity_grid != null && !activity_grid.equals(""))
            set(PrefKeys.DefaultActivity_GridId, Integer.parseInt(activity_grid));

        // Default activity_label
        String activity_label = getString(PrefKeys.DefaultActivity_LabelId);
        remove(PrefKeys.DefaultActivity_LabelId);
        if (activity_label != null && !activity_label.equals(""))
            set(PrefKeys.DefaultActivity_LabelId, Integer.parseInt(activity_label));

        // I18NDialogShown
        String i18NDialogShown = getString(PrefKeys.I18NDialogShown);
        remove(PrefKeys.I18NDialogShown);
        if (i18NDialogShown != null && !i18NDialogShown.equals(""))
            set(PrefKeys.I18NDialogShown, i18NDialogShown.equals("true"));

        // Vibrate
        String vibrate = getString(PrefKeys.Notifs_Vibrate);
        remove(PrefKeys.Notifs_Vibrate);
        set(PrefKeys.Notifs_Vibrate, vibrate == null || vibrate.equals("true"));

        // Notifications
        String notifications = getString(PrefKeys.Notifications);
        remove(PrefKeys.Notifications);
        set(PrefKeys.Notifications, notifications != null && notifications.equals("true"));

        // Refresh rate
        String refreshRate = getString(PrefKeys.Notifs_RefreshRate);
        remove(PrefKeys.Notifs_RefreshRate);
        if (refreshRate != null && !refreshRate.equals(""))
            set(PrefKeys.Notifs_RefreshRate, Integer.valueOf(refreshRate));

        // Notifs_wifiOnly
        String wifiOnly = getString(PrefKeys.Notifs_WifiOnly);
        remove(PrefKeys.Notifs_WifiOnly);
        set(PrefKeys.Notifs_WifiOnly, wifiOnly != null && wifiOnly.equals("true"));
    }
}