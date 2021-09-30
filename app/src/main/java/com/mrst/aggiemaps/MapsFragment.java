package com.mrst.aggiemaps;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esri.core.geometry.Point;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rubensousa.decorator.ColumnProvider;
import com.rubensousa.decorator.GridMarginDecoration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapsFragment extends Fragment implements OnCampusAdapter.ItemClickListener, FavAdapter.ItemClickListener, OffCampusAdapter.ItemClickListener, GameDayAdapter.ItemClickListener, SwipeAdapter.ItemClickListener {

    private OkHttpClient client;  // Client to make API requests
    public static GoogleMap mMap;       // The Map itself
    private BottomSheetBehavior<View> standardBottomSheetBehavior;
    private RecyclerView onCampusRoutes;
    private OnCampusAdapter onCampusAdapter;
    private OffCampusAdapter offCampusAdapter;
    private RecyclerView offCampusRoutes;
    private FavAdapter favAdapter;
    private RecyclerView favRoutes;
    private GameDayAdapter gameDayAdapter;
    private RecyclerView gameDayRoutes;
    private TextView favoritesText;
    private Set<String> favoritesSet;
    private List<BusRoute> favList;
    private List<BusRoute> onList;
    private List<BusRoute> offList;
    private List<BusRoute> gameDayList;

    @Override
    public void onItemClick(View view, int position) {
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
    }

    enum TripType {
        WALK,
        BUS,
        BIKE,
        DRIVE
    }

    /*
     * Method to convert transportation coords to LatLng
     * returns Point
     */
    private Point convertWebMercatorToLatLng(final double x, final double y) {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem targetCRS = crsFactory.createFromName("EPSG:4236");
        CoordinateReferenceSystem sourceCRS = crsFactory.createFromName("EPSG:3857");
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CoordinateTransform wgsToUtm = ctFactory.createTransform(sourceCRS, targetCRS);
        ProjCoordinate result = new ProjCoordinate();
        wgsToUtm.transform(new ProjCoordinate(x, y), result);

        return new Point(result.x, result.y);
    }

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

            return Objects.requireNonNull(body).string(); // Return the response as a string

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Helper method to convert a drawable to a BitmapDescriptor for use with a maps marker
     * Taken from somewhere similar to here
     * https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon
     */
    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId, int color, int modifySize) {
        // below line is use to generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        // below line is use to set bounds to our vector drawable.
        assert vectorDrawable != null;
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth() + modifySize, vectorDrawable.getIntrinsicHeight() + modifySize);
        vectorDrawable.setTintList(ColorStateList.valueOf(color));

        // below line is use to create a bitmap for our
        // drawable which we have added.
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        // below line is use to add bitmap in our canvas.
        Canvas canvas = new Canvas(bitmap);

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas);

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /*
     * Method to draw a bus route on the map
     */
    private void drawBusRoute(String routeNo, int color) {

        // Check for cached data
        Map<String, AggiePolyline> route = AggiePolyline.getData(requireActivity());
        if (route.containsKey(routeNo)) {

            // Draw polyline of route
            PolylineOptions newPolyline = Objects.requireNonNull(route.get(routeNo)).polylineOptions;
            newPolyline.color(color);
            requireActivity().runOnUiThread(() -> mMap.addPolyline(newPolyline));

            // Draw stops
            for (LatLng i : Objects.requireNonNull(route.get(routeNo)).stops) {
                MarkerOptions marker = new MarkerOptions();
                marker.flat(true);
                marker.icon(BitmapFromVector(getActivity(), R.drawable.checkbox_blank_circle, color, -20));
                marker.anchor(0.5F, 0.5F);
                marker.position(i);
                requireActivity().runOnUiThread(() -> mMap.addMarker(marker));
            }
            return;
        }
        try {
            Request request = new Request.Builder()
                    .url("https://transport.tamu.edu/BusRoutesFeed/api/route/" + routeNo + "/pattern")
                    .build();

            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();

            String str = Objects.requireNonNull(body).string();
            JSONArray stops = new JSONArray(str);
            PolylineOptions polylineOptions = new PolylineOptions();
            LatLng first = null;

            ArrayList<LatLng> busStops = new ArrayList<>();
            for (int i = 0; i < stops.length(); i++) {

                Point p = convertWebMercatorToLatLng(stops.getJSONObject(i).getDouble("Longtitude"),
                        stops.getJSONObject(i).getDouble("Latitude"));
                double y = p.getX();
                double x = p.getY();
                if (i == 0) {
                    first = new LatLng(x, y);
                }
                System.out.println(x + ", " + y);
                polylineOptions.add(new LatLng(x, y));

                // Add bus stop circles
                if (stops.getJSONObject(i).getString("PointTypeCode").equals("1")) {
                    MarkerOptions marker = new MarkerOptions();
                    marker.flat(true);
                    marker.icon(BitmapFromVector(getActivity(), R.drawable.checkbox_blank_circle, color, -20));

                    marker.anchor(0.5F, 0.5F);
                    marker.position(new LatLng(x, y));
                    requireActivity().runOnUiThread(() -> mMap.addMarker(marker));
                    busStops.add(new LatLng(x, y));
                }
            }

            assert first != null;
            polylineOptions.add(first);
            polylineOptions.color(color);
            polylineOptions.width(10);
            polylineOptions.geodesic(true);
            polylineOptions.pattern(null);
            polylineOptions.clickable(true);
            requireActivity().runOnUiThread(() -> mMap.addPolyline(polylineOptions));
            AggiePolyline.writeData(requireActivity(), new AggiePolyline(polylineOptions, busStops), routeNo);
        } catch (JSONException | IOException jsonException) {
            jsonException.printStackTrace();
        }
    }

    /*
     * Method to draw all buses on a given route
     */
    private void drawBusesOnRoute(String routeNo) {
        new Thread(() -> {
            boolean first = true;
            ArrayList<Marker> busMarkers = new ArrayList<>();
            try {
                while (true) {
                    // Add buses
                    Request request = new Request.Builder()
                            .url("https://transport.tamu.edu/BusRoutesFeed/api/route/" + routeNo + "/buses")
                            .build();
                    Response response = client.newCall(request).execute();
                    ResponseBody body = response.body();
                    String str = body.string();
                    JSONArray buses = new JSONArray(str);

                    for (int i = 0; i < buses.length(); i++) {
                        busMarkers.add(null);
                        JSONObject currentBus = buses.getJSONObject(i);
                        Point p = convertWebMercatorToLatLng(currentBus.getDouble("lng"), currentBus.getDouble("lat"));
                        double x = p.getY();
                        double y = p.getX();
                        int finalI = i;
                        if (first) {
                            MarkerOptions marker = new MarkerOptions();
                            marker.icon(BitmapFromVector(requireActivity(), R.drawable.bus_side, ContextCompat.getColor(requireActivity(), R.color.foreground), 0));
                            marker.zIndex(100);
                            marker.anchor(0.5F, 0.8F);
                            marker.rotation((float) currentBus.getDouble("direction") - 90);
                            marker.position(new LatLng(x, y));
                            requireActivity().runOnUiThread(() -> busMarkers.set(finalI, mMap.addMarker(marker)));
                        } else {
                            while (busMarkers.get(finalI) == null) {

                                requireActivity().runOnUiThread(() -> {
                                    busMarkers.get(finalI).setPosition(new LatLng(x, y));
                                    try {
                                        busMarkers.get(finalI).setRotation((float) currentBus.getDouble("direction") - 90);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        }
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            try {
                                busMarkers.get(finalI).setTitle(currentBus.getString("occupancy"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });

                    }
                    first = false;
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (JSONException | IOException jsonException) {
                jsonException.printStackTrace();
            }


        }).start();
    }

    /*
     * Method to create array of a route from two latlng coordinates
     * returns a TripPlan obj
     */
    private TripPlan getTripPlan(LatLng src, LatLng dest, TripType type) {

        return null;
    }

    /*
     * When the map is ready to be interacted with
     * Ex. Draw lines, add circles, set style
     */
    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            LatLng collegeStation = new LatLng(30.611812, -96.329767);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(collegeStation, 14.0f));
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.light));
        }
    };

    /*
     * LinearLayoutManager that stops a recycler from being scrolled
     * Used for swiping up on the buses button
     */
    private static class UnscrollableLinearLayoutManager extends LinearLayoutManager {
        public UnscrollableLinearLayoutManager(Context context) {
            super(context);
        }

        @Override
        public boolean canScrollVertically() {
            return false;
        }
    }

    /*
     * When the view is created, what happens
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!isAdded()) return null;

        // Inflate View
        View mView = inflater.inflate(R.layout.fragment_maps, container, false);
        client = new OkHttpClient.Builder() // Create OkHttpClient to be used in API requests
                .cache(new Cache(new File(requireActivity().getCacheDir(), "http_cache"),
                        50L * 1024L * 1024L))
                .build();
        favoritesText = mView.findViewById(R.id.favorites_text); // Initialize favorites text

        // Set up recyclers
        RecyclerView swipeRecycler = mView.findViewById(R.id.swipe_recycler);
        favRoutes = mView.findViewById(R.id.recycler_favorites);
        favAdapter = null;
        onCampusRoutes = mView.findViewById(R.id.recycler_oncampus);
        onCampusAdapter = null;
        offCampusRoutes = mView.findViewById(R.id.recycler_offcampus);
        offCampusAdapter = null;
        gameDayRoutes = mView.findViewById(R.id.recycler_gameday);
        gameDayAdapter = null;
        swipeRecycler.setLayoutManager(new UnscrollableLinearLayoutManager(getActivity()));
        List<String> l = new ArrayList<>();
        l.add(" ");
        SwipeAdapter swipeAdapter = new SwipeAdapter(getActivity(), l);
        swipeRecycler.setAdapter(swipeAdapter);
        swipeAdapter.setClickListener(this);

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                swipeAdapter.notifyItemChanged(0);
            }
        });
        helper.attachToRecyclerView(null);
        helper.attachToRecyclerView(swipeRecycler);

        // Set decorations for the recyclers
        ColumnProvider col = () -> 1;
        favRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
        favRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        DisplayMetrics metrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        col = () -> 2;
        if (metrics.heightPixels < convertDpToPx(15 + (241 * 3))) {
            col = () -> 1;
            onCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            offCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            gameDayRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            gameDayRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        } else if (metrics.heightPixels < convertDpToPx(15 + (241 * 2))) {
            onCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            col = () -> 1;
            offCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            gameDayRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            gameDayRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        } else if (metrics.heightPixels < convertDpToPx(15 + (241 * 1))) {
            onCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            offCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            col = () -> 1;
            gameDayRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            gameDayRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        } else {
            onCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            offCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            gameDayRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            gameDayRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        }
        // Set up the bottom sheet
        View standardBottomSheet = mView.findViewById(R.id.standard_bottom_sheet);
        standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet);
        standardBottomSheetBehavior.setSaveFlags(BottomSheetBehavior.SAVE_ALL);
        standardBottomSheetBehavior.setHideable(false);
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        standardBottomSheetBehavior.setPeekHeight(0);
        standardBottomSheetBehavior.setHalfExpandedRatio(0.49f);

        // Then set up the bus routes on the bottom sheet
        new Thread(this::setUpBusRoutes).start();

        return mView;
    }

    private int convertDpToPx(int dp) {
        DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /*
     * Method to create and display all bus routes on the bottom sheet
     */
    private void setUpBusRoutes() {
        try {
            String str = getApiCall("https://transport.tamu.edu/BusRoutesFeed/api/Routes");
            JSONArray routes = new JSONArray(str);
            favoritesSet = new HashSet<>();  // Initialize the favorites set
            loadFavorites();  // Load the favorites set from sharedpreferences

            // Initialize lists and add the `ALL` Route
            favList = new ArrayList<>();
            onList = new ArrayList<>();
            offList = new ArrayList<>();
            gameDayList = new ArrayList<>();
            favList.add(new BusRoute("All", "Favorites", ContextCompat.getColor(requireActivity(), R.color.all_color), BusRouteTag.FAVORITES));
            onList.add(new BusRoute("All", "On Campus", ContextCompat.getColor(requireActivity(), R.color.all_color), BusRouteTag.ON_CAMPUS));
            offList.add(new BusRoute("All", "Off Campus", ContextCompat.getColor(requireActivity(), R.color.all_color), BusRouteTag.OFF_CAMPUS));
            gameDayList.add(new BusRoute("All", "Game Day", ContextCompat.getColor(requireActivity(), R.color.all_color), BusRouteTag.GAME_DAY));

            // Traverse through all routes
            for (int i = 0; i < routes.length(); i++) {

                // Get Color int
                String strRGB = routes.getJSONObject(i).getString("Color");
                int color = Integer.MAX_VALUE;
                if (strRGB.startsWith("rgb")) {
                    String[] colors = strRGB.substring(4, strRGB.length() - 1).split(",");
                    color = Color.rgb(
                            Integer.parseInt(colors[0].trim()),
                            Integer.parseInt(colors[1].trim()),
                            Integer.parseInt(colors[2].trim()));
                }

                // See what group the route lies in, then add it
                String shortName = routes.getJSONObject(i).getString("ShortName");
                String name = routes.getJSONObject(i).getString("Name");
                String group = routes.getJSONObject(i).getJSONObject("Group").getString("Name");

                // If in the favorites set, add it to favorites, otherwise continue
                if (favoritesSet.contains(shortName)) {
                    switch (group) {
                        case "On Campus":
                            favList.add(new BusRoute(shortName, name, color, BusRouteTag.ON_CAMPUS));
                            break;
                        case "Off Campus":
                            favList.add(new BusRoute(shortName, name, color, BusRouteTag.OFF_CAMPUS));
                            break;
                        case "Game Day":
                            favList.add(new BusRoute(shortName, name, color, BusRouteTag.GAME_DAY));
                            break;
                    }
                } else {
                    switch (group) {
                        case "On Campus":
                            onList.add(new BusRoute(shortName, name, color, BusRouteTag.ON_CAMPUS));
                            break;
                        case "Off Campus":
                            offList.add(new BusRoute(shortName, name, color, BusRouteTag.OFF_CAMPUS));
                            break;
                        case "Game Day":
                            gameDayList.add(new BusRoute(shortName, name, color, BusRouteTag.GAME_DAY));
                            break;
                    }
                }
            }

            // Create adapters and add them to the recyclers
            favAdapter = new FavAdapter(getActivity(), favList, BusRouteTag.FAVORITES);
            onCampusAdapter = new OnCampusAdapter(getActivity(), onList, BusRouteTag.ON_CAMPUS);
            offCampusAdapter = new OffCampusAdapter(getActivity(), offList, BusRouteTag.OFF_CAMPUS);
            gameDayAdapter = new GameDayAdapter(getActivity(), gameDayList, BusRouteTag.GAME_DAY);
            requireActivity().runOnUiThread(() -> {
                favRoutes.setAdapter(favAdapter);
                onCampusRoutes.setAdapter(onCampusAdapter);
                offCampusRoutes.setAdapter(offCampusAdapter);
                gameDayRoutes.setAdapter(gameDayAdapter);
            });

            // Set click listeners
            favAdapter.setClickListener(this);
            onCampusAdapter.setClickListener(this);
            offCampusAdapter.setClickListener(this);
            gameDayAdapter.setClickListener(this);

            // If no favorites don't show the list
            if (favList.size() == 1) {
                requireActivity().runOnUiThread(() -> {
                    favRoutes.setVisibility(View.GONE);
                    favoritesText.setVisibility(View.GONE);
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
     * Method to save the current favorites that the user has to sharedpreferences
     */
    private void saveFavorites() {
        Gson gson = new Gson();
        SharedPreferences.Editor spEditor = requireActivity().getSharedPreferences("RecordedFavorites", MODE_PRIVATE).edit();
        spEditor.putString("favorites", gson.toJson(favoritesSet)).apply();
    }

    /*
     * Method to load the favorites that the user has from sharedpreferences
     */
    private void loadFavorites() {
        Gson gson = new Gson();
        SharedPreferences sp = requireActivity().getSharedPreferences("RecordedFavorites", MODE_PRIVATE);
        String defValue = gson.toJson(new HashSet<String>());
        String json = sp.getString("favorites", defValue);
        TypeToken<HashSet<String>> token = new TypeToken<HashSet<String>>() {
        };
        favoritesSet = gson.fromJson(json, token.getType());
    }

    /*
    * When is route or a favorite is tapped, calls this method
     */
    @Override
    public void onItemClick(View view, BusRoute busRoute, int position, BusRouteTag tag) {
        // When a favorite is clicked, add to favorites, remove from prev list
        if (view instanceof MaterialButton) {

            // Remove a route from it's list
            switch (tag) {
                case ON_CAMPUS:
                    onList.remove(position);
                    onCampusAdapter.notifyItemRemoved(position - 1);
                    break;
                case OFF_CAMPUS:
                    offList.remove(position);
                    offCampusAdapter.notifyItemRemoved(position);
                    break;
                case GAME_DAY:
                    gameDayList.remove(position);
                    gameDayAdapter.notifyItemRemoved(position);
                    break;
                case FAVORITES:

                    // If it's a favorite then add it to the correct list
                    switch (favList.get(position).tag) {
                        case ON_CAMPUS:
                            onList.add(busRoute);
                            onCampusAdapter.notifyItemInserted(onList.size() - 1);
                            break;
                        case OFF_CAMPUS:
                            offList.add(busRoute);
                            offCampusAdapter.notifyItemInserted(onList.size() - 1);
                            break;
                        case GAME_DAY:
                            gameDayList.add(busRoute);
                            gameDayAdapter.notifyItemInserted(onList.size() - 1);
                            break;
                    }

                    // Remove from the favorite list/set and save
                    favList.remove(position);
                    favAdapter.notifyItemRemoved(position);
                    if (favList.size() == 1) {
                        favRoutes.setVisibility(View.GONE);
                        favoritesText.setVisibility(View.GONE);
                    }
                    favoritesSet.remove(busRoute.routeNumber);
                    new Thread(this::saveFavorites).start();
                    return;
            }

            // If not an unfavorite add it to the favorites list and save
            favoritesSet.add(busRoute.routeNumber);
            favList.add(busRoute);
            favAdapter.notifyItemInserted(favList.size() - 1);
            favRoutes.setVisibility(View.VISIBLE);
            favoritesText.setVisibility(View.VISIBLE);
            new Thread(this::saveFavorites).start();
            return;  // skip showing the route if
        }

        // When a route is clicked, close sheet, set route number and draw route
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mMap.clear(); // Clear map first
        if (busRoute.routeNumber.equals("All")) {
            new Thread(() -> {
                switch (busRoute.routeName) {
                    case "Favorites":
                        for (int i = 1; i < favAdapter.getItemCount(); i++) {
                            int finalI = i;
                            new Thread(() -> drawBusRoute(favAdapter.getItem(finalI).routeNumber, favAdapter.getItem(finalI).color)).start();
                        }
                        break;
                    case "On Campus":
                        for (int i = 1; i < onCampusAdapter.getItemCount(); i++) {
                            int finalI = i;
                            new Thread(() -> drawBusRoute(onCampusAdapter.getItem(finalI).routeNumber, onCampusAdapter.getItem(finalI).color)).start();
                        }
                        break;
                    case "Off Campus":
                        for (int i = 1; i < offCampusAdapter.getItemCount(); i++) {
                            int finalI = i;
                            new Thread(() -> drawBusRoute(offCampusAdapter.getItem(finalI).routeNumber, offCampusAdapter.getItem(finalI).color)).start();
                        }
                        break;
                    case "Game Day":
                        for (int i = 1; i < gameDayAdapter.getItemCount(); i++) {
                            int finalI = i;
                            new Thread(() -> drawBusRoute(gameDayAdapter.getItem(finalI).routeNumber, gameDayAdapter.getItem(finalI).color)).start();
                        }
                        break;
                }
            }).start();
        } else {
            new Thread(() -> drawBusRoute(busRoute.routeNumber, busRoute.color)).start();
            drawBusesOnRoute(busRoute.routeNumber);
        }
    }

    /*
     * Don't worry about this
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }
}