package com.mrst.aggiemaps;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggieBusRoutes {

    List<BusRoute> favList;
    List<BusRoute> onList;
    List<BusRoute> offList;
    List<BusRoute> gameDayList;

    public AggieBusRoutes(List<BusRoute> favList, List<BusRoute> onList, List<BusRoute> offList, List<BusRoute> gameDayList) {
        this.favList = favList;
        this.onList = onList;
        this.offList = offList;
        this.gameDayList = gameDayList;
    }

    public static void writeData(Context c, AggieBusRoutes pd, String name) {
        Gson gson = new Gson();
        SharedPreferences.Editor spEditor = c.getSharedPreferences("RecordedRoutes", MODE_PRIVATE).edit();
        spEditor.putString(name, gson.toJson(pd)).apply();
    }

    public static Map<String, AggieBusRoutes> getData(Context c) {
        Gson gson = new Gson();
        SharedPreferences sp = c.getSharedPreferences("RecordedRoutes", MODE_PRIVATE);
        Map<String, ?> mp = sp.getAll();
        Map<String, AggieBusRoutes> routes = new HashMap<>();

        for (Map.Entry<String, ?> entry : mp.entrySet()) {
            String json = entry.getValue().toString();
            AggieBusRoutes pd = gson.fromJson(json, AggieBusRoutes.class);
            routes.put(entry.getKey(), pd);
        }

        return routes;

    }
}
