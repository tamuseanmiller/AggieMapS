package com.mrst.aggiemaps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import android.content.Intent;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.rule.ActivityTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MapsFragmentTest {
    @Rule
    public ActivityTestRule<MainActivity> activityRule
            = new ActivityTestRule<>(
            MainActivity.class,
            true,
            false);
    private MainActivity mainActivity;

    @Before
    public void initMaps() throws InterruptedException {
        Intent intent = new Intent();
        mainActivity = activityRule.launchActivity(intent);
        Thread.sleep(1000);
    }

    @Test
    public void busRoutesTest() throws InterruptedException {
        onView(withId(R.id.buses))
                .perform(click());
        onView(withId(R.id.standard_bottom_sheet))
                .check(matches(isDisplayed()));
        Thread.sleep(500);
        onView(withId(R.id.recycler_favorites))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        Thread.sleep(3000);
    }

    @Test
    public void searchBarTest() throws InterruptedException {
        onView(withId(R.id.material_search_bar))
                .perform(click());
        onView(withId(R.id.material_search_view))
                .check(matches(isDisplayed()));
        onView(withId(R.id.search_view_edit_text))
                .perform(typeText("Zachry"));
        Thread.sleep(1000);
        onView(withId(mainActivity.gisId))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
    }
}
