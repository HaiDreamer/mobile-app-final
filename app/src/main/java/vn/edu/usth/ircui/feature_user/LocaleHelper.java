package vn.edu.usth.ircui.feature_user;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_LANGUAGE = "app_language";

    public static Context onAttach(Context context) {
        String language = getLanguage(context);
        return setLocale(context, language);
    }

    public static Context setLocale(Context context, String languageCode) {
        persistLanguage(context, languageCode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, languageCode);
        } else {
            updateResourcesLegacy(context, languageCode);
            return context;
        }
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    private static void persistLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    @SuppressWarnings("deprecation")
    private static void updateResourcesLegacy(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    @SuppressWarnings("deprecation")
    private static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(config);
        } else {
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }
}