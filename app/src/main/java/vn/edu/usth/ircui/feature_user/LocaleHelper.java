/**
 * Utility for switching this app's displayed language at runtime.
 *
 * <p>Call this before inflating views on any screen that should reflect
 * the new locale.</p>
 */

package vn.edu.usth.ircui.feature_user;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class LocaleHelper {

    public static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();

        config.setLocale(locale);

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}
