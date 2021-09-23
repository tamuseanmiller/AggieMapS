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
    private RecyclerView onCampusRoutes;
    private OnCampusAdapter onCampusAdapter;
    private OffCampusAdapter offCampusAdapter;
    private RecyclerView offCampusRoutes;
    private FavAdapter favAdapter;
    private RecyclerView favRoutes;
    private GameDayAdapter gameDayAdapter;
    private RecyclerView gameDayRoutes;
    private OkHttpClient client;  // Client to make API requests

    /*
     * Method to make a GET request to a given URL
     * returns response body as String
     */
    private String getApiCall(String url) {
        try {
            // Create request
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            // Execute request and get response
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();

            return body.string(); // Return the response as a string

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the status bar to be transparent
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Initialize OkHttp Client
        client = new OkHttpClient(); // Create OkHttpClient to be used in API requests

        // Set up recyclers
        favRoutes = findViewById(R.id.recycler_favorites);
        favAdapter = null;
        onCampusRoutes = findViewById(R.id.recycler_oncampus);
        onCampusAdapter = null;
        offCampusRoutes = findViewById(R.id.recycler_offcampus);
        offCampusAdapter = null;
        gameDayRoutes = findViewById(R.id.recycler_gameday);
        gameDayAdapter = null;

        // Set decorations
        ColumnProvider col = () -> 1;
        favRoutes.setLayoutManager(new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false));
        favRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        col = () -> 2;
        onCampusRoutes.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        offCampusRoutes.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        gameDayRoutes.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        gameDayRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));

        // Thread for fetching the routes
        new Thread(() -> {
            try {
                String str = getApiCall("https://transport.tamu.edu/BusRoutesFeed/api/Routes");
                JSONArray routes = new JSONArray(str);

                // Initialize lists and add the `ALL` Route
                List<BusRoute> favList = new ArrayList<>();
                List<BusRoute> onList = new ArrayList<>();
                List<BusRoute> offList = new ArrayList<>();
                List<BusRoute> gameDayList = new ArrayList<>();
                favList.add(new BusRoute("All", "Favorites", ContextCompat.getColor(this, R.color.all_color)));
                onList.add(new BusRoute("All", "On Campus", ContextCompat.getColor(this, R.color.all_color)));
                offList.add(new BusRoute("All", "Off Campus", ContextCompat.getColor(this, R.color.all_color)));
                gameDayList.add(new BusRoute("All", "Game Day", ContextCompat.getColor(this, R.color.all_color)));

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

                    // See what group the route lies in, then add it
                    switch (routes.getJSONObject(i).getJSONObject("Group").getString("Name")) {
                        case "On Campus":
                            onList.add(new BusRoute(routes.getJSONObject(i).getString("ShortName"), routes.getJSONObject(i).getString("Name"), color));
                            break;
                        case "Off Campus":
                            offList.add(new BusRoute(routes.getJSONObject(i).getString("ShortName"), routes.getJSONObject(i).getString("Name"), color));
                            break;
                        case "Game Day":
                            gameDayList.add(new BusRoute(routes.getJSONObject(i).getString("ShortName"), routes.getJSONObject(i).getString("Name"), color));
                            break;
                        default:
                            favList.add(new BusRoute(routes.getJSONObject(i).getString("ShortName"), routes.getJSONObject(i).getString("Name"), color));
                    }
                }

                // Create adapters and add them to the recyclers
                favAdapter = new FavAdapter(this, favList, BusRouteTag.FAVORITES);
                onCampusAdapter = new OnCampusAdapter(this, onList, BusRouteTag.ON_CAMPUS);
                offCampusAdapter = new OffCampusAdapter(this, offList, BusRouteTag.OFF_CAMPUS);
                gameDayAdapter = new GameDayAdapter(this, gameDayList, BusRouteTag.GAME_DAY);
                runOnUiThread(() -> {
                    favRoutes.setAdapter(favAdapter);
                    onCampusRoutes.setAdapter(onCampusAdapter);
                    offCampusRoutes.setAdapter(offCampusAdapter);
                    gameDayRoutes.setAdapter(gameDayAdapter);
                });

                // If no favorites don't show the list
                if (favList.size() == 1) {
                    favRoutes.setVisibility(View.GONE);
                    findViewById(R.id.favorites_text).setVisibility(View.GONE);
                }

            } catch (JSONException e) {
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
        standardBottomSheetBehavior.setHalfExpandedRatio(0.9f);

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