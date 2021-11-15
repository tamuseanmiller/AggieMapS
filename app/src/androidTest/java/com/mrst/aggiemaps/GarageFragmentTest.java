package com.mrst.aggiemaps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import com.levibostian.recyclerviewmatcher.RecyclerViewMatcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SmallTest
public class GarageFragmentTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule
            = new ActivityTestRule<>(
            MainActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False to customize the intent

    GarageFragment garageFragment;
    private MainActivity mainActivity;

    @Before
    public void initDirections() {
        Intent intent = new Intent();
        mainActivity = activityRule.launchActivity(intent);
        garageFragment = (GarageFragment) mainActivity.getSupportFragmentManager().findFragmentByTag("f0");
    }

    /*
    Test to see if the API call values are correctly displayed in the recyclerview
     */
    @Test
    public void APICallValuesEqualDisplayedValues() {
        new Thread(() -> {
            // Get the live counts from the function
            HashMap garageHashMap = garageFragment.getLiveCount();
            List<Integer> values = new ArrayList<Integer>(garageHashMap.values());
            List<String> keys = new ArrayList<String>(garageHashMap.keySet());

            garageFragment.updateGarageUI();

            for(int i=0; i<values.size();i++){
                onView(new RecyclerViewMatcher(R.id.garages_recycler).viewHolderViewAtPosition(i,R.id.spacesRow)).check(matches(withText(values.get(i).toString())));
            }

        }).start();
    }
}


