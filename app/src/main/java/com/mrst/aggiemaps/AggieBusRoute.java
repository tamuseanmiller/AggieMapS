package com.mrst.aggiemaps;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.navigation.NavType;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
 * Class used to cache polyline data, taken from
 * https://stackoverflow.com/questions/15502398/serialize-or-save-polylineoptions-in-android
 */
public class AggieBusRoute {

    public PolylineOptions polylineOptions;
    public ArrayList<Pair<String, LatLng>> stops;
    public LatLng northEastBound;
    public LatLng southWestBound;

    public AggieBusRoute(PolylineOptions polylineOptions, ArrayList<Pair<String, LatLng>> stops, LatLng northEastBound, LatLng southWestBound) {
        this.polylineOptions = polylineOptions;
        this.stops = stops;
        this.northEastBound = northEastBound;
        this.southWestBound = southWestBound;
    }

    public static void writeData(Context c, AggieBusRoute pd, String name) {
        Gson gson = new Gson();
        SharedPreferences.Editor spEditor = c.getSharedPreferences("RecordedPoints", MODE_PRIVATE).edit();
        spEditor.putString(name, gson.toJson(pd)).apply();
    }

    public static Map<String, AggieBusRoute> getData(Context c) {
        Gson gson = new Gson();
        SharedPreferences sp = c.getSharedPreferences("RecordedPoints", MODE_PRIVATE);
        Map<String, ?> mp = sp.getAll();
        Map<String, AggieBusRoute> routes = new HashMap<>();

        for (Map.Entry<String, ?> entry : mp.entrySet()) {
            String json = entry.getValue().toString();
            AggieBusRoute pd = gson.fromJson(json, AggieBusRoute.class);
            routes.put(entry.getKey(), pd);
        }

        return routes;

    }

    public boolean equals(@NonNull AggieBusRoute newRoute) {
        return newRoute.northEastBound == northEastBound &&
                newRoute.southWestBound == southWestBound &&
                newRoute.polylineOptions == polylineOptions &&
                newRoute.stops == stops;
    }
}

