package com.mrst.aggiemaps;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
* Class used to cache polyline data, taken from
* https://stackoverflow.com/questions/15502398/serialize-or-save-polylineoptions-in-android
 */
public class AggiePolyline {

    public PolylineOptions polylineOptions;
    public ArrayList<LatLng> stops;

    public AggiePolyline(PolylineOptions polylineOptions, ArrayList<LatLng> stops) {
        this.polylineOptions = polylineOptions;
        this.stops = stops;
    }

    public static void writeData(Context c, AggiePolyline pd, String name) {
        Gson gson = new Gson();
        SharedPreferences.Editor spEditor = c.getSharedPreferences("RecordedPoints", MODE_PRIVATE).edit();
        spEditor.putString(name, gson.toJson(pd)).apply();
    }

    public static Map<String, AggiePolyline> getData(Context c) {
        Gson gson = new Gson();
        SharedPreferences sp = c.getSharedPreferences("RecordedPoints", MODE_PRIVATE);
        Map<String, ?> mp = sp.getAll();
        Map<String, AggiePolyline> routes = new HashMap<>();

        for (Map.Entry<String, ?> entry : mp.entrySet()) {
            String json = entry.getValue().toString();
            AggiePolyline pd = gson.fromJson(json, AggiePolyline.class);
            routes.put(entry.getKey(), pd);
        }

        return routes;

    }
}
