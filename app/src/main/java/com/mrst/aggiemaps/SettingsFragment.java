package com.mrst.aggiemaps;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.MapStyleOptions;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        PreferenceManager preferenceManager = getPreferenceManager();

        // Set the preferences for a theme change
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("com.mrst.aggiemaps.preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        preferenceManager.findPreference("theme").setDefaultValue(sharedPref.getString("theme", "system_theme"));

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        preferenceManager.findPreference("theme").setOnPreferenceChangeListener((preference, newValue) -> {
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

        MapsFragment mapsFragment = ((MainActivity) requireActivity()).mapsFragment;
        DirectionsFragment directionsFragment = ((MainActivity) requireActivity()).directionsFragment;
        //assert mapsFragment != null;

        // Listen for light map style change
        preferenceManager.findPreference("light_maps").setOnPreferenceChangeListener((preference, newValue) -> {
            // If light mode is on
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                switch (newValue.toString()) {
                    case "light":
                        mapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.light));
                        directionsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.light));
                        break;
                    case "retro":
                        mapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.retro));
                        directionsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.retro));
                        break;
                    case "classic":
                        mapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.classic));
                        directionsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.classic));
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
                        mapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.dark));
                        directionsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.dark));
                        break;
                    case "sin_city":
                        mapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.sin_city));
                        directionsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.sin_city));
                        break;
                    case "night":
                        mapsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.night));
                        directionsFragment.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.night));
                        break;
                }
            }
            editor.putString("dark_maps", newValue.toString());
            editor.apply();
            return true;
        });
    }

    private int convertDpToPx(int dp) {
        DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView rv = getListView(); // This holds the PreferenceScreen's items
        rv.setPadding(convertDpToPx(16),  convertDpToPx(80), 0, 0);
        super.onViewCreated(view, savedInstanceState);
    }
}