package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.chteuchteu.munin.MuninFoo;
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

	public static boolean isLanguageSupported(String languageCode) {
		for (AppLanguage lang : AppLanguage.values()) {
			if (lang.langCode.toLowerCase().equals(languageCode.toLowerCase()))
				return true;
		}

		return false;
	}

	/**
	 * Updates the global locale if different than the current/default one
	 * @param context Activity context
	 */
	public static void updateLocale(Context context, MuninFoo muninFoo) {
		Locale locale = muninFoo.getLocale();

		Resources resources = context.getApplicationContext().getResources();
		Configuration configuration = resources.getConfiguration();

		if (!configuration.locale.equals(locale) || !Locale.getDefault().equals(locale))
			applyLocale(context, locale);
	}

	private static void applyLocale(Context context, Locale locale) {
		Locale.setDefault(locale);
		Resources resources = context.getApplicationContext().getResources();
		Configuration configuration = new Configuration(resources.getConfiguration());
		configuration.locale = locale;
		resources.updateConfiguration(configuration, resources.getDisplayMetrics());
	}

	public static Locale getSettingsLocaleOrDefault(Context context, Settings settings) {
		if (settings.has(Settings.PrefKeys.Lang)) {
			String lang = settings.getString(Settings.PrefKeys.Lang);

			if (!isLanguageSupported(lang))
				lang = AppLanguage.defaultLang().langCode;

			return new Locale(lang);
		}

		// Locale not set: return default one
		Resources resources = context.getApplicationContext().getResources();
		Configuration configuration = resources.getConfiguration();
		return configuration.locale;
	}
}
