package com.geekhub.choosehelper.screens.fragments;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.geekhub.choosehelper.R;

public class SettingsFragment extends PreferenceFragmentCompat/* implements OnSharedPreferenceChangeListener*/ {

    /*@BindString(R.string.settings_categories)
    String mSettingsCategories;

    @BindString(R.string.settings_numbers_of_compares)
    String mSettingsNumbersOfCompares;

    @BindString(R.string.settings_language)
    String mSettingsLanguage;

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }*/

    /*@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }*/

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.qwerty);
    }

    /*@Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }*/

    /*@Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (key.equals(mSettingsCategories)) {
            MainActivity.sIsNeedToAutoUpdate = true;
            Set<String> categoriesSet = sharedPreferences.getStringSet(key, new HashSet<>());
            String categoriesString = "";
            if (categoriesSet.size() != 0) {
                int i = 0;
                for (String s : categoriesSet) {
                    if (i == (categoriesSet.size() - 1)) {
                        categoriesString = categoriesString + s;
                    } else {
                        categoriesString = categoriesString + s + ", ";
                    }
                    i++;
                }
            } else {
                categoriesString = "No categories selected";
            }
            preference.setSummary(categoriesString);
        } else if (key.equals(mSettingsNumbersOfCompares)) {
            MainActivity.sIsNeedToAutoUpdate = true;
            preference.setSummary(String.format(Locale.getDefault(), "Show %s compares in main",
                    sharedPreferences.getString(key, "")));
        } else if (key.equals(mSettingsLanguage)) {
            preference.setSummary(sharedPreferences.getString(key, ""));
        }
    }*/
}
