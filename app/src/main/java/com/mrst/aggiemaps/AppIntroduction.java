package com.mrst.aggiemaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;
import com.github.appintro.AppIntroPageTransformerType;
import com.permissionx.guolindev.PermissionX;

public class AppIntroduction extends AppIntro {
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String AppIntro_Seen = "";


    @SuppressLint({"ResourceAsColor", "ResourceType"})
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        if (sharedPreferences.getBoolean(AppIntro_Seen, false)) {
            addSlide(AppIntroFragment.newInstance());
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        } else {
            addSlide(AppIntroFragment.newInstance("Howdy!",
                    "Welcome to AggieMapS, an application to help navigate the Texas A&M " +
                            "Campus through the Aggie Spirit Bus system!",
                    R.drawable.map_intro,
                    ContextCompat.getColor(this, R.color.red_300)
            ));

            // custom slide for the cache button
            addSlide(new IntroCacheRoutesFragment());

            addSlide(AppIntroFragment.newInstance("Locations Permission",
                    "Please allow AggieMapS to access your location. We will use it to optimize " +
                            "navigation for you!",
                    R.drawable.current_location_intro,
                    ContextCompat.getColor(this, R.color.blue_300)
            ));


            // Fade Transition
            // You can customize your parallax parameters in the constructors.
            setTransformer(new AppIntroPageTransformerType.Parallax(1.0, -1.0, 2.0));

            // Show/hide status bar
            showStatusBar(true);

            //Speed up or down scrolling
            setScrollDurationFactor(2);

            //Enable the color "fade" animation between two slides (make sure the slide implements SlideBackgroundColorHolder)
            setColorTransitionsEnabled(false);

            //Prevent the back button from exiting the slides
            setSystemBackButtonLocked(true);

            //Activate wizard mode (Some aesthetic changes)
            setWizardMode(false);

            //Show/hide skip button
            setSkipButtonEnabled(false);

            //Enable immersive mode (no status and nav bar)
            setImmersiveMode();

            //Enable/disable page indicators
            setIndicatorEnabled(true);

            //Dhow/hide ALL buttons
            setButtonsEnabled(true);

        }

    }

    @Override
    protected void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        AppIntroSeen();
    }

    @Override
    protected void onDonePressed(Fragment currentFragment) {

        // Request Location Permission
        PermissionX.init(currentFragment)
                .permissions(Manifest.permission.ACCESS_FINE_LOCATION)
                .request((allGranted, grantedList, deniedList) -> {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                });

        super.onDonePressed(currentFragment);
        AppIntroSeen();
    }

    private void AppIntroSeen() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AppIntro_Seen, true);
        editor.apply();
    }
}

