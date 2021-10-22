package com.mrst.aggiemaps;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecentSearches {

    List<ListItem> recentSearchesList;

    public RecentSearches(List<ListItem> searchesList) {
        this.recentSearchesList = searchesList;
    }

    public static void writeData(Context c, RecentSearches searches, String name) {
        Gson gson = new Gson();
        SharedPreferences.Editor spEditor = c.getSharedPreferences("recentSearches", MODE_PRIVATE).edit();
        spEditor.putString(name, gson.toJson(searches)).apply();
    }

    public static Map<String, RecentSearches> getData(Context c) {
        Gson gson = new Gson();
        SharedPreferences sp = c.getSharedPreferences("recentSearches", MODE_PRIVATE);
        Map<String, ?> mp = sp.getAll();
        Map<String, RecentSearches> searchesMap = new HashMap<>();

        for (Map.Entry<String, ?> entry : mp.entrySet()) {
            String json = entry.getValue().toString();
            RecentSearches searches = gson.fromJson(json, RecentSearches.class);
            searchesMap.put(entry.getKey(), searches);
        }
        return searchesMap;

    }

}
