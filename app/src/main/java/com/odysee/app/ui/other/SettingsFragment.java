package com.odysee.app.ui.other;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.utils.LbryAnalytics;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;


public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        /*EditTextPreference miniPlayerBottomMarginPreference = manager.findPreference(MainActivity.PREFERENCE_KEY_MINI_PLAYER_BOTTOM_MARGIN);
        miniPlayerBottomMarginPreference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        });*/
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity activity = (MainActivity) getContext();
        if (activity != null) {
            activity.hideSearchBar();
            /*activity.showNavigationBackIcon();
            activity.lockDrawer();
            activity.hideFloatingWalletBalance();*/

            activity.setActionBarTitle(R.string.settings);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context instanceof MainActivity) {
            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
            MainActivity activity = (MainActivity) context;
            MainActivity.suspendGlobalPlayer(context);
            LbryAnalytics.setCurrentScreen(activity, "Settings", "Settings");
        }
    }
    @Override
    public void onPause() {
        Context context = getContext();
        if (context != null) {
            MainActivity.resumeGlobalPlayer(context);
            PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        Context context = getContext();

        if (context != null) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).resetCurrentDisplayFragment();
            }

            // Applying dark mode default setting after closing the Settings fragment avoids a problem where
            // some UI components being hidden when they shouldn't. If current dark mode is the same as the
            // applied one, no activity recreation is performed, so there is no penalty.
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            if (sp != null && Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                String darkModeValue = sp.getString(MainActivity.PREFERENCE_KEY_DARK_MODE_SETTING, MainActivity.APP_SETTING_DARK_MODE_NOTNIGHT);
                switch (darkModeValue) {
                    case MainActivity.APP_SETTING_DARK_MODE_NIGHT:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    case MainActivity.APP_SETTING_DARK_MODE_NOTNIGHT:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    default:
                        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM);
                        break;
                }
            }
            super.onStop();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equalsIgnoreCase(MainActivity.PREFERENCE_KEY_DARK_MODE)) {
            boolean darkMode = sp.getBoolean(MainActivity.PREFERENCE_KEY_DARK_MODE, false);
            AppCompatDelegate.setDefaultNightMode(darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}