package com.odysee.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class OdyseeApp extends Application {
    public static final String PREFERENCE_KEY_DARK_MODE = "com.odysee.app.preference.userinterface.DarkMode";
    public static final String PREFERENCE_KEY_DARK_MODE_SETTING = "com.odysee.app.preference.userinterface.DarkModeSetting";
    public static final String PREFERENCE_KEY_SHOW_MATURE_CONTENT = "com.odysee.app.preference.userinterface.ShowMatureContent";
    public static final String APP_SETTING_DARK_MODE_NIGHT = "night";
    public static final String APP_SETTING_DARK_MODE_NOTNIGHT = "notnight";
    public static final String APP_SETTING_DARK_MODE_SYSTEM = "system";

    @Override
    public void onCreate() {
        super.onCreate();

        if (getDarkModeAppSetting().equals(APP_SETTING_DARK_MODE_NIGHT)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (getDarkModeAppSetting().equals(APP_SETTING_DARK_MODE_NOTNIGHT)){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Returns the Dark mode app setting, which could be Light/Night -up to Android 10- or Light/Night/System -from Android 11-
     * @return - For API Level < 30, 'night' or 'notnight'. For newer versions, 'system' also.
     */
    private String getDarkModeAppSetting() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            return sp.getString(PREFERENCE_KEY_DARK_MODE_SETTING, APP_SETTING_DARK_MODE_NOTNIGHT);
        } else {
            boolean darkMode = sp.getBoolean(PREFERENCE_KEY_DARK_MODE, false);
            if (darkMode) {
                return APP_SETTING_DARK_MODE_NIGHT;
            } else {
                return APP_SETTING_DARK_MODE_NOTNIGHT;
            }
        }
    }
}
