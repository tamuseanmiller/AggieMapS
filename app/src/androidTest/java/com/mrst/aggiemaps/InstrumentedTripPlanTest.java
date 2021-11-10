package com.mrst.aggiemaps;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Intent;

import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class InstrumentedTripPlanTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule
            = new ActivityTestRule<>(
            MainActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False to customize the intent

    DirectionsFragment directionsFragment;

    @Before
    public void initDirections() {
        Intent intent = new Intent();
        MainActivity mainActivity = activityRule.launchActivity(intent);
        directionsFragment = (DirectionsFragment) mainActivity.getSupportFragmentManager().findFragmentByTag("f1");
    }

    @Test
    public void testZachToEABA() {
        // ZACH to EABA
        LatLng src = new LatLng(30.62133, -96.34030);
        LatLng dest = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, MapsFragment.TripType.WALK);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testEABAToZach() {
        // EABA to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, MapsFragment.TripType.WALK);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testZachToZach() {
        // ZACH to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.62133, -96.34030);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, MapsFragment.TripType.WALK);
        assertNull(tripPlan);
    }
}
