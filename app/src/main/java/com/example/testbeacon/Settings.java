package com.example.testbeacon;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

// Settings Fragment (PreferenceFragment) which is embedded in MainActivity
public class Settings extends PreferenceFragmentCompat  {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public Settings() {
        // Required empty public constructor
    }

    public static Settings newInstance(String param1, String param2) {
        Settings fragment = new Settings();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    // this method is called when the fragment is created (user navigates to fragment)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // retrieve state of background scanning switch from shared preferences and set stored state, store when state changes
        SwitchPreference switchScanning = (SwitchPreference) getPreferenceManager().findPreference("switch_scanning");
        switchScanning.setChecked(((MainActivity)getActivity()).getSwitchState());
        switchScanning.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((MainActivity)getActivity()).saveSwitchState((boolean) newValue);
                return true;
            }
        });

        // retrieve state of dark theme switch from shared preferences and set stored state, store when state changes
        SwitchPreference switchDark = (SwitchPreference) getPreferenceManager().findPreference("switch_dark");
        switchDark.setChecked((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        switchDark.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((MainActivity)getActivity()).setDarkMode();
                return true;
            }
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Indicate here the XML resource you created above that holds the preferences
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}