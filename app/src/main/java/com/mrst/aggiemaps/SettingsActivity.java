package com.mrst.aggiemaps;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.model.MapStyleOptions;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.accent)));
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            PreferenceManager preferenceManager = getPreferenceManager();

            // Set the preferences for a theme change
            SharedPreferences sharedPref = getActivity().getSharedPreferences("com.mrst.aggiemaps.preferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            preferenceManager.findPreference("theme").setDefaultValue(sharedPref.getString("theme", "system_theme"));

            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            preferenceManager.findPreference("theme").setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) (preference, newValue) -> {
                    switch (newValue.toString()) {
                        case "light":
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            break;
                        case "dark":
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            break;
                        case "system_theme":
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
                }
                editor.putString("theme", newValue.toString());
                editor.apply();
                return true;
            });

            // Listen for light map style change
            preferenceManager.findPreference("light_maps").setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) (preference, newValue) -> {
                // If light mode is on
                if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                    switch (newValue.toString()) {
                        case "light":
                            MapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.light));
                            break;
                        case "retro":
                            MapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.retro));
                            break;
                        case "classic":
                            MapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.classic));
                    }
                }
                editor.putString("light_maps", newValue.toString());
                editor.apply();
                return true;
            });

            // Listen for dark map style change
            preferenceManager.findPreference("dark_maps").setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) (preference, newValue) -> {
                // If dark mode is on
                if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                    switch (newValue.toString()) {
                        case "dark":
                            MapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.dark));
                            break;
                        case "sin_city":
                            MapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.sin_city));
                            break;
                        case "night":
                            MapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.night));
                            break;
                    }
                }
                editor.putString("dark_maps", newValue.toString());
                editor.apply();
                return true;
            });
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}