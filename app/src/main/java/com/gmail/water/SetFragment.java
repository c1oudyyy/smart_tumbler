package com.gmail.water;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;

import java.util.Set;

public class SetFragment extends Fragment {
    View v;
    SharedPreferences pref;


    public SetFragment(){

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_log, container, false);
        super.onCreate(savedInstanceState);

        //만약 여기서 호출되면 꾸미기도 가능하지 않을까?
        getFragmentManager().beginTransaction().replace(R.id.setting_preferenceFragment, new SettingFragment()).commit();

        return v;
    }

   /* @Override
    public void onResume() {
        super.onResume();
        pref.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        pref.registerOnSharedPreferenceChangeListener(listener);
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
    };*/
}


