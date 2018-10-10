package com.chteuchteu.munin;

import android.app.Application;
import android.content.res.Configuration;

import com.chteuchteu.munin.hlpr.I18nHelper;

public class App extends Application {
    public void onCreate() {
        super.onCreate();

        I18nHelper.updateLocale(this);
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);

        I18nHelper.updateLocale(this);
    }
}
