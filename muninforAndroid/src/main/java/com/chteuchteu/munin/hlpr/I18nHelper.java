package com.chteuchteu.munin.hlpr;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import com.chteuchteu.munin.R;

import java.util.Arrays;
import java.util.Locale;

public class I18nHelper {
	/**
	 * AppLanguage enum
	 * Once most strings are supplied for a language,
	 * 	we make it available in the settings using this enum.
	 */
	public enum AppLanguage {
		DE("de", R.string.lang_german),
		EN("en", R.string.lang_english),
		ES("es", R.string.lang_spanish),
		FI("fi", R.string.lang_finnish),
		FR("fr", R.string.lang_french),
		HU("hu", R.string.lang_hungarian),
		IT("it", R.string.lang_italian),
		JA("ja", R.string.lang_japanese),
		NL("nl", R.string.lang_dutch),
		PT_RBR("pt-rBR", R.string.lang_portuguese_brazil),
		RU("ru", R.string.lang_russian),
		UK("uk", R.string.lang_ukrainian),
		ZH_RCN("zh_rCN", R.string.lang_chinese),
		ZH_TW("zh_TW", R.string.lang_chinese_taiwan),
		PL("pl", R.string.lang_polish);

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

    /**
     * Returns true if the specified language is explicitly supported by Munin for Android
     * (if it has translation keys)
     */
	public static boolean isLanguageSupported(String languageCode) {
		for (AppLanguage lang : AppLanguage.values()) {
			if (lang.langCode.toLowerCase().equals(languageCode.toLowerCase()))
				return true;
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	public static void updateLocale(Application app) {
		Locale locale = getSettingsLocaleOrDefault(Settings.getInstance(app));

		if (locale == null)
			return;

		Resources resources = app.getResources();
		Configuration configuration = new Configuration(resources.getConfiguration());

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
			configuration.locale = locale;
		else
			configuration.setLocale(locale);

		app.getResources().updateConfiguration(configuration, resources.getDisplayMetrics());
	}

	public static Locale getSettingsLocaleOrDefault(Settings settings) {
		if (settings.has(Settings.PrefKeys.Lang)) {
			String lang = settings.getString(Settings.PrefKeys.Lang);

			if (!isLanguageSupported(lang))
				lang = AppLanguage.defaultLang().langCode;

			return new Locale(lang);
		}

		// Locale not set
		return null;
	}

    /**
     * Returns true if the specified language is supported on the current device
     */
	public static boolean isLanguageSupportedByDevice(AppLanguage language) {
        Locale[] availableLocales = Locale.getAvailableLocales();

        for (Locale locale : availableLocales) {
            if (locale.getLanguage().equals(language.langCode)) {
                return true;
            }
        }

        return false;
    }
}
