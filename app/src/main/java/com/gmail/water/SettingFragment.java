package com.gmail.water;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Set;

public class SettingFragment extends PreferenceFragmentCompat {
    SharedPreferences pref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.setting);
    }

    @Override
    public void onResume() {
        super.onResume();

        pref.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();

        pref.unregisterOnSharedPreferenceChangeListener(listener);

    }

    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("necessity")) {
                Set<String> datas;
                datas = pref.getStringSet(key, null);
            } else {
                EditTextPreference ep = (EditTextPreference) findPreference(key);
                ep.setSummary(pref.getString(key, ""));
            }
        }
    };
}