package com.mrst.aggiemaps;

import static com.google.gson.internal.$Gson$Types.arrayOf;
import static com.mrst.aggiemaps.R.layout.custom_intro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroCustomLayoutFragment;
import com.github.appintro.AppIntroFragment;
import com.github.appintro.AppIntroPageTransformerType;

public class AppIntroduction extends AppIntro{
    public static final String SHARED_PREFS = "sharedPrefs" ;
    public static final String AppIntro_Seen = "" ;


    @SuppressLint({"ResourceAsColor", "ResourceType"})
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        if (sharedPreferences.getBoolean(AppIntro_Seen,false)){
            addSlide(AppIntroFragment.newInstance());
            startActivity(new Intent(getApplicationContext(),MainActivity.class));
        }
        else{
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
                    false);


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
        //TODO: Cache Buses Button does not work
        ConstraintLayout custom_intro = (ConstraintLayout) getLayoutInflater().inflate(R.layout.custom_intro, null);
        Button cacheBusesBtn =  custom_intro.findViewById(R.id.cacheBusesBtn);

        cacheBusesBtn.setOnClickListener(view -> {
            Toast.makeText(getApplicationContext(), "TESTHERE", Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    protected void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        startActivity(new Intent(getApplicationContext(),MainActivity.class));
        AppIntroSeen();
    }

    @Override
    protected void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        startActivity(new Intent(getApplicationContext(),MainActivity.class));
        AppIntroSeen();
    }

    private void AppIntroSeen(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AppIntro_Seen, true);
        editor.apply();
    }
}

