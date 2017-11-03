package com.chteuchteu.munin.hlpr;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Arrays;
import java.util.Locale;

public class I18nHelper {
	/**
	 * AppLanguage enum
	 * Once most strings are supplied for a language,
	 * 	we make it available in the settings using this enum.
	 */
	public enum AppLanguage {
		DE("de"),
		EN("en"),
		ES("es"),
		FI("fi"),
		FR("fr"),
		HU("hu"),
		IT("it"),
		JA("ja"),
		NL("nl"),
		PT_RBR("pt_BR"),
		RU("ru"),
		UK("uk"),
		ZH_RCN("zh_CN"),
		ZH_TW("zh_TW"),
		PL("pl"),
        CA("ca"),
        CS("cs"),
        NB_RNO("nb_NO");

		public String langCode;

		AppLanguage(String langCode) {
			this.langCode = langCode;
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
		public Locale getLocale() {
		    int separatorIndex = this.langCode.indexOf('_');
		    String language = "";
		    String country = "";

		    if (separatorIndex == -1) {
		        language = this.langCode;
            }
            else {
		        language = this.langCode.substring(0, separatorIndex);
		        country = this.langCode.substring(separatorIndex+1);
            }

		    return new Locale(language, country);
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
            if (locale.toString().equals(language.langCode)) {
                return true;
            }
        }

        return false;
    }
}
