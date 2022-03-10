package com.odysee.app.ui.other;

import android.content.Context;
import android.content.SharedPreferences;
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
        if (context instanceof MainActivity) {
            ((MainActivity) context).resetCurrentDisplayFragment();
        }
        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equalsIgnoreCase(MainActivity.PREFERENCE_KEY_DARK_MODE)) {
            boolean darkMode = sp.getBoolean(MainActivity.PREFERENCE_KEY_DARK_MODE, false);
            AppCompatDelegate.setDefaultNightMode(darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        }
        if (key.equalsIgnoreCase("com.odysee.app.preference.userinterface.DarkModeSetting")) {
            String darkModeValue = sp.getString("com.odysee.app.preference.userinterface.DarkModeSetting", "notnight");
            if (darkModeValue.equals("night")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else if (darkModeValue.equals("notnight")){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                int nightMode = AppCompatDelegate.getDefaultNightMode();
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM);
            }
        }
    }
}