package com.mrst.aggiemaps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DirectionsFragmentTests {
    @Rule
    public ActivityTestRule<MainActivity> activityRule
            = new ActivityTestRule<>(
            MainActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False to customize the intent
    private MainActivity mainActivity;

    @Before
    public void initDirections() {
        Intent intent = new Intent();
        mainActivity = activityRule.launchActivity(intent);
        onView(withId(R.id.directions))            // withId(R.id.my_view) is a ViewMatcher
                .perform(click());              // click() is a ViewAction
    }


    @Test
    public void searchBarCheck() {
        onView(withId(R.id.src_search_bar))
                .perform(click());
        onView(withId(R.id.material_search_view))
                .check(matches(isDisplayed()));
        Espresso.closeSoftKeyboard();
        Espresso.pressBack();
        onView(withId(R.id.dest_search_bar))
                .perform(click());
        onView(withId(R.id.material_search_view))
                .check(matches(isDisplayed()));
        onView(withId(R.id.search_view_edit_text))
                .perform(typeText("Zachry"));
        onView(withId(mainActivity.gisId))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        onView(withId(R.id.src_search_bar))
                .check(matches(isDisplayed()));


    }
}
