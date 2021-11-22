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
    public void testZachToEABAWalk() {
        // ZACH to EABA
        LatLng src = new LatLng(30.62133, -96.34030);
        LatLng dest = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.WALK);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testEABAToZachWalk() {
        // EABA to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.WALK);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testZachToZachWalk() {
        // ZACH to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.62133, -96.34030);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.WALK);
        assertNull(tripPlan);
    }

    @Test
    public void testZachToEABADrive() {
        // ZACH to EABA
        LatLng src = new LatLng(30.62133, -96.34030);
        LatLng dest = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.DRIVE);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testEABAToZachDrive() {
        // EABA to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.DRIVE);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testZachToZachDrive() {
        // ZACH to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.62133, -96.34030);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.DRIVE);
        assertNull(tripPlan);
    }

    @Test
    public void testZachToEABABus() {
        // ZACH to EABA
        LatLng src = new LatLng(30.62133, -96.34030);
        LatLng dest = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.BUS);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testEABAToZachBus() {
        // EABA to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.BUS);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testZachToZachBus() {
        // ZACH to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.62133, -96.34030);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.BUS);
        assertNull(tripPlan);
    }

    @Test
    public void testZachToEABABike() {
        // ZACH to EABA
        LatLng src = new LatLng(30.62133, -96.34030);
        LatLng dest = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.BIKE);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testEABAToZachBike() {
        // EABA to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.BIKE);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testZachToZachBike() {
        // ZACH to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.62133, -96.34030);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.BIKE);
        assertNull(tripPlan);
    }

    @Test
    public void testZachToEABABusADA() {
        // ZACH to EABA
        LatLng src = new LatLng(30.62133, -96.34030);
        LatLng dest = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.BUS_ADA);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testEABAToZachBusADA() {
        // EABA to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.61589, -96.33695);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.BUS_ADA);
        assertNotNull(tripPlan);
        assertNotNull(tripPlan.getFeatures());
        assertNotNull(tripPlan.getGeometry());
        assertFalse(tripPlan.getFeatures().isEmpty());
        assertFalse(tripPlan.getGeometry().isEmpty());
    }

    @Test
    public void testZachToZachBusADA() {
        // ZACH to ZACH
        LatLng dest = new LatLng(30.62133, -96.34030);
        LatLng src = new LatLng(30.62133, -96.34030);
        TripPlan tripPlan = directionsFragment.getTripPlan(src, dest, DirectionsFragment.TripType.BUS_ADA);
        assertNull(tripPlan);
    }
}
