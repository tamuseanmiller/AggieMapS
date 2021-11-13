package com.mrst.aggiemaps;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@SmallTest
public class DrawBusesTest {
    @Rule
    public ActivityTestRule<MainActivity> activityRule
            = new ActivityTestRule<>(
            MainActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False to customize the intent

    MapsFragment mapsFragment;
    private MainActivity mainActivity;

    @Before
    public void initDirections() {
        Intent intent = new Intent();
        mainActivity = activityRule.launchActivity(intent);
        mapsFragment = (MapsFragment) mainActivity.getSupportFragmentManager().findFragmentByTag("f2");
    }

    /*
    Test to see if the location of the buses on the route are updating
     */
    @Test
    public void checkIfBusesLocationUpdated() {
        new Thread(() -> {
            mapsFragment.drawBusesOnRoute("04");
            if (!mapsFragment.busMarkers.isEmpty()){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LatLng testMarkerPosition1 = mapsFragment.busMarkers.get(0).getPosition();
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mapsFragment.drawBusesOnRoute("04");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LatLng testMarkerPosition2 = mapsFragment.busMarkers.get(0).getPosition();

                assertNotEquals(testMarkerPosition1, testMarkerPosition2);
            }
        }).start();


    }

    /*
       Test to see if the API call returns the same amount of buses as whats in the busMarkers
       Array after drawBusesOnRoute() call.
    */
    @Test
    public void checkIfAllBusesInBusMarkersArray(){
        new Thread(() -> {
            List<BusRoute> allRoutes = new ArrayList<>();
            allRoutes.addAll(mapsFragment.onList);
            allRoutes.addAll(mapsFragment.offList);
            allRoutes.addAll(mapsFragment.gameDayList);

            for(int i=0; i< allRoutes.size(); i++){
                String API_url = "https://transport.tamu.edu/BusRoutesFeed/api/route/" + allRoutes.get(i).routeNumber +
                        "/buses";
                try {
                    JSONArray busData_jsonArray = new JSONArray(mapsFragment.getApiCall(API_url));
                    mapsFragment.drawBusesOnRoute(allRoutes.get(i).routeNumber);
                    Thread.sleep(1000);
                    ArrayList busMarkersArray = mapsFragment.busMarkers;
                    assertEquals(busData_jsonArray.length(), busMarkersArray.size());
                } catch (JSONException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

}
