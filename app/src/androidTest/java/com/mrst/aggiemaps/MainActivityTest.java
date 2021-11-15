package com.mrst.aggiemaps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule
            = new ActivityTestRule<>(
            MainActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False to customize the intent

    @Before
    public void initDirections() {
        Intent intent = new Intent();
        MainActivity mainActivity = activityRule.launchActivity(intent);
    }


    @Test
    public void searchBarCheck() {
        onView(withId(R.id.material_search_bar))            // withId(R.id.my_view) is a ViewMatcher
                .perform(click());              // click() is a ViewAction
        onView(withId(R.id.material_search_view))
                .check(matches(isDisplayed())); // matches(isDisplayed()) is a ViewAssertion
    }

    @Test
    public void bottomBar() {
        onView(withId(R.id.directions))            // withId(R.id.my_view) is a ViewMatcher
                .perform(click());              // click() is a ViewAction
        onView(withId(R.id.directions))
                .check(matches(isDisplayed())); // matches(isDisplayed()) is a ViewAssertion
        onView(withId(R.id.buses))            // withId(R.id.my_view) is a ViewMatcher
                .perform(click())
                .perform(click());      // click() is a ViewAction
        onView(withId(R.id.standard_bottom_sheet))
                .check(matches(isDisplayed())); // matches(isDisplayed()) is a ViewAssertion
    }

    @Test
    public void garageView() {
        onView(withId(R.id.bottom_bar))            // withId(R.id.my_view) is a ViewMatcher
                .perform(click());              // click() is a ViewAction
        onView(withId(R.id.bottom_bar))
                .check(matches(isDisplayed())); // matches(isDisplayed()) is a ViewAssertion
        onView(withId(R.id.directions))            // withId(R.id.my_view) is a ViewMatcher
                .perform(click());              // click() is a ViewAction
        onView(withId(R.id.directions))
                .check(matches(isDisplayed())); // matches(isDisplayed()) is a ViewAssertion
        onView(withId(R.id.garages))            // withId(R.id.my_view) is a ViewMatcher
                .perform(click());              // click() is a ViewAction
        onView(withId(R.id.garages))
                .check(matches(isDisplayed())); // matches(isDisplayed()) is a ViewAssertion
        onView(withId(R.id.swl_garages))
                .check(matches(isDisplayed())); // matches(isDisplayed()) is a ViewAssertion
        onView(withId(R.id.garages_recycler))
                .check(matches(isDisplayed())); // matches(isDisplayed()) is a ViewAssertion
    }

}
