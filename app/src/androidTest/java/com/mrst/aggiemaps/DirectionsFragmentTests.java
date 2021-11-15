package com.mrst.aggiemaps;

import android.content.Intent;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DirectionsFragmentTests {

    private static final String TIME_MIN_OUTPUT = "17 min";
    private static final String TIME_SEC_OUTPUT = "74 sec";
    private static final String DISTANCE_MILES_OUTPUT = "0.50 miles";
    private static final String DISTANCE_FEET_OUTPUT = "500 feet";

    @Rule
    public ActivityTestRule<MainActivity> activityRule
            = new ActivityTestRule<>(
            MainActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False to customize the intent
    private MainActivity mainActivity;

    @Before
    public void initDirections() throws InterruptedException {
        Intent intent = new Intent();
        mainActivity = activityRule.launchActivity(intent);
        Thread.sleep(100);
        onView(withId(R.id.directions))            // withId(R.id.my_view) is a ViewMatcher
                .perform(click());              // click() is a ViewAction
    }


    @Test
    public void A_searchBarCheck() throws InterruptedException {
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
        Thread.sleep(1000);
        onView(withId(mainActivity.gisId))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        onView(withId(R.id.src_search_bar))
                .check(matches(isDisplayed()));
    }

    @Test
    public void B_zachryToEABATest() throws InterruptedException {
        // Populate source search bar
        onView(withId(R.id.src_search_bar))
                .perform(click());
        onView(withId(R.id.material_search_view))
                .check(matches(isDisplayed()));
        Thread.sleep(1000);
        onView(withId(mainActivity.recentId))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));

        // Populate destination search bar
        onView(withId(R.id.dest_search_bar))
                .perform(click());
        onView(withId(R.id.material_search_view))
                .check(matches(isDisplayed()));
        onView(withId(R.id.search_view_edit_text))
                .perform(typeText("EABA"));
        Thread.sleep(1000);
        onView(withId(mainActivity.gisId))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));

        // Check directions sheet
        onView(withId(R.id.trip_progress))
                .check(matches(isDisplayed()));
        Thread.sleep(1000);
        onView(withId(R.id.directions_bottom_sheet))
                .check(matches(isDisplayed()));

        // Switch travel modes and check
        onView(withId(R.id.chip_bus))
                .perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.chip_bike))
                .perform(click());

        // Swap
        Thread.sleep(1000);
        onView(withId(R.id.fab_swap))
                .perform(click());
        Thread.sleep(1500);
        onView(withId(R.id.directions_bottom_sheet))
                .check(matches(isDisplayed()));
    }

    @Test
    public void C_currLocationToCurrLocationTest() throws InterruptedException {
        // Populate source search bar
        onView(withId(R.id.src_search_bar))
                .perform(click());
        onView(withId(R.id.material_search_view))
                .check(matches(isDisplayed()));
        onView(withId(mainActivity.recentId))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Populate destination search bar
        onView(withId(R.id.dest_search_bar))
                .perform(click());
        onView(withId(R.id.material_search_view))
                .check(matches(isDisplayed()));
        onView(withId(mainActivity.recentId))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Check directions sheet did not open
        Thread.sleep(1000);
        onView(withId(R.id.directions_bottom_sheet))
                .check(matches(not(isDisplayed())));
    }

    @Test
    public void D_getTimeTextReturnsMinutes() throws InterruptedException {
        DirectionsFragment testFragment = new DirectionsFragment();
        String result = testFragment.getTimeText(17.41241234);
        assertThat(result, is(TIME_MIN_OUTPUT));
    }

    @Test
    public void E_getTimeTextReturnsSeconds() throws InterruptedException {
        DirectionsFragment testFragment = new DirectionsFragment();
        String result = testFragment.getTimeText(1.2345);
        assertThat(result, is(TIME_SEC_OUTPUT));
    }

    @Test
    public void F_getDistanceTextReturnsMiles() throws InterruptedException {
        DirectionsFragment testFragment = new DirectionsFragment();
        String result = testFragment.getDistanceText(0.49511111);
        assertThat(result, is(DISTANCE_MILES_OUTPUT));
    }

    @Test
    public void G_getDistanceTextReturnsFeet() throws InterruptedException {
        DirectionsFragment testFragment = new DirectionsFragment();
        String result = testFragment.getDistanceText(0.0947123);
        assertThat(result, is(DISTANCE_FEET_OUTPUT));
    }
}

