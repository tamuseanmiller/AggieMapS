package com.mrst.aggiemaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.rubensousa.decorator.ColumnProvider;
import com.rubensousa.decorator.GridMarginDecoration;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomSheetBehavior<View> standardBottomSheetBehavior;
    private RecyclerView recyclerRoutes;
    private RecyclerViewAdapterBusRoutes adapterRoutes;
    private RecyclerViewAdapterBusRoutes adapterOnCampus;
    private RecyclerView recyclerOnCampus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the status bar to be transparent
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Set up recycler
        recyclerRoutes = findViewById(R.id.recycler_favorites);
        adapterRoutes = null;

        // Set decoration
        recyclerRoutes.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        ColumnProvider col = () -> 2;
        recyclerRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                // Add buses
                Request request = new Request.Builder()
                        .url("https://transport.tamu.edu/BusRoutesFeed/api/Routes")
                        .build();
                Response response = client.newCall(request).execute();
                ResponseBody body = response.body();
                String str = body.string();
                JSONArray routes = new JSONArray(str);

                List<BusRoute> routeList = new ArrayList<>();
                routeList.add(new BusRoute("All", "Favorites", ContextCompat.getColor(this, R.color.all_color)));
                for (int i = 0; i < routes.length(); i++) {

                    // Get Color int
                    String strRGB = routes.getJSONObject(i).getString("Color");
                    int color = Color.rgb(255, 0, 255);
                    if (strRGB.startsWith("rgb")) {
                        String[] colors = strRGB.substring(4, strRGB.length() - 1).split(",");
                        color = Color.rgb(
                                Integer.parseInt(colors[0].trim()),
                                Integer.parseInt(colors[1].trim()),
                                Integer.parseInt(colors[2].trim()));
                    }

                    routeList.add(new BusRoute(routes.getJSONObject(i).getString("ShortName"), routes.getJSONObject(i).getString("Name"), color));
                }
                adapterRoutes = new RecyclerViewAdapterBusRoutes(this, routeList);
                runOnUiThread(() -> recyclerRoutes.setAdapter(adapterRoutes));
                //adapterRoutes.setClickListener(this);

            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

        }).start();

        // Set up recycler
        recyclerOnCampus = findViewById(R.id.recycler_oncampus);
        adapterOnCampus = null;

        // Set decoration
        recyclerOnCampus.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        recyclerOnCampus.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                // Add buses
                Request request = new Request.Builder()
                        .url("https://transport.tamu.edu/BusRoutesFeed/api/Routes")
                        .build();
                Response response = client.newCall(request).execute();
                ResponseBody body = response.body();
                String str = body.string();
                JSONArray routes = new JSONArray(str);

                List<BusRoute> routeList = new ArrayList<>();
                routeList.add(new BusRoute("All", "On Campus", ContextCompat.getColor(this, R.color.all_color)));
                for (int i = 0; i < routes.length(); i++) {

                    if (routes.getJSONObject(i).getString("Description").contains("On Campus")) {

                        // Get Color int
                        String strRGB = routes.getJSONObject(i).getString("Color");
                        int color = Color.rgb(255, 0, 255);
                        if (strRGB.startsWith("rgb")) {
                            String[] colors = strRGB.substring(4, strRGB.length() - 1).split(",");
                            color = Color.rgb(
                                    Integer.parseInt(colors[0].trim()),
                                    Integer.parseInt(colors[1].trim()),
                                    Integer.parseInt(colors[2].trim()));
                        }

                        routeList.add(new BusRoute(routes.getJSONObject(i).getString("ShortName"), routes.getJSONObject(i).getString("Name"), color));
                    }
                }
                adapterOnCampus = new RecyclerViewAdapterBusRoutes(this, routeList);
                runOnUiThread(() -> recyclerOnCampus.setAdapter(adapterOnCampus));
                //adapterRoutes.setClickListener(this);

            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

        }).start();



        // Set up the bottom sheet
        View standardBottomSheet = findViewById(R.id.standard_bottom_sheet);
        standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet);
        standardBottomSheetBehavior.setSaveFlags(BottomSheetBehavior.SAVE_ALL);
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        standardBottomSheetBehavior.setPeekHeight(200);
        standardBottomSheetBehavior.setHideable(false);

        BottomSheetBehavior.BottomSheetCallback bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }

        };

        // To add the callback:
        standardBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback);

        // To remove the callback:
        standardBottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback);
    }
}