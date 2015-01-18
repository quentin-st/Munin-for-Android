package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;

import java.util.Arrays;
import java.util.Locale;

public final class I18nHelper {
	public enum AppLanguage {
		EN("en", R.string.lang_english), FR("fr", R.string.lang_french), DE("de", R.string.lang_german), RU("ru", R.string.lang_russian) /*, ES("es", R.string.lang_spanish) */;

		public String langCode;
		public int localeNameRes;

		AppLanguage(String langCode, int localeNameRes) {
			this.langCode = langCode; this.localeNameRes = localeNameRes;
		}
		public static AppLanguage defaultLang() { return AppLanguage.EN; }

		/**
		 * get AppLanguage enum value from langCode.
		 *  If not found, returns the default language (EN)
		 */
		public static AppLanguage get(String langCode) {
			for (AppLanguage lang : AppLanguage.values()) {
				if (lang.langCode.equals(langCode))
					return lang;
			}

			return defaultLang();
		}
		public int getIndex() {
			return Arrays.asList(AppLanguage.values()).indexOf(this);
		}
	}

	public static boolean isLanguageSupported(String languageCode) {
		for (AppLanguage lang : AppLanguage.values()) {
			if (lang.langCode.equals(languageCode))
				return true;
		}

		return false;
	}

	public static void loadLanguage(Context context, MuninFoo muninFoo) { loadLanguage(context, muninFoo, false); }
	/**
	 * Load language according to preferences, if set. If not : lang is
	 *  already set according to device locale.
	 * @param context Activity context
	 * @param forceLoad Force language load (after language change)
	 */
	public static void loadLanguage(Context context, MuninFoo muninFoo, boolean forceLoad) {
		String lang = Util.getPref(context, Util.PrefKeys.Lang);

		if (!lang.equals("")) {
			if (!muninFoo.languageLoaded || forceLoad) {
				if (!isLanguageSupported(lang))
					lang = "en";

				Resources res = context.getApplicationContext().getResources();
				DisplayMetrics dm = res.getDisplayMetrics();
				Configuration conf = res.getConfiguration();
				conf.locale = new Locale(lang);
				res.updateConfiguration(conf, dm);

				muninFoo.languageLoaded = true;
			}
		}
		// else: lang set according to device locale
	}
}
