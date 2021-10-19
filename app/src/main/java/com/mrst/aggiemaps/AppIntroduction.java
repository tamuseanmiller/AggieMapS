package com.mrst.aggiemaps;

import static com.google.gson.internal.$Gson$Types.arrayOf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroCustomLayoutFragment;
import com.github.appintro.AppIntroFragment;
import com.github.appintro.AppIntroPageTransformerType;

public class AppIntroduction extends AppIntro{
    @SuppressLint({"ResourceAsColor", "ResourceType"})
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addSlide(AppIntroFragment.newInstance("HOWDY!",
                "Welcome to AggieMapS, an application to help navigate the Texas A&M " +
                        "Campus through the Aggie Spirit Bus system!",
                R.drawable.map_intro,
                ContextCompat.getColor(this,R.color.red_60)
        ));

        // Default add slides for bus routes test and matching of custom layout file
        // addSlide(AppIntroFragment.newInstance("Bus Routes",
        //        "Please click the button below to download the bus routes for smoother app navigation!",
        //        R.drawable.current_location_intro,
        //        ContextCompat.getColor(this,R.color.accent)
        // ));

        // custom slide for the cache button
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.custom_intro));

        addSlide(AppIntroFragment.newInstance("Locations Permission",
                "Please allow AggieMapS to access your location. We will use it to optimize " +
                        "navigation for you!",
                R.drawable.current_location_intro,
                ContextCompat.getColor(this,R.color.accent)
        ));

        askForPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                2,
               true);


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
        setSkipButtonEnabled(true);

        //Enable immersive mode (no status and nav bar)
        setImmersiveMode();

        //Enable/disable page indicators
        setIndicatorEnabled(true);

        //Dhow/hide ALL buttons
        setButtonsEnabled(true);
    }

    @Override
    protected void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        Intent i = new Intent(getApplicationContext(),MainActivity.class);
        startActivity(i);
    }

    @Override
    protected void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        Intent i = new Intent(getApplicationContext(),MainActivity.class);
        startActivity(i);
    }
}

