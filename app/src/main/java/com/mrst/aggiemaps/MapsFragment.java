package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pair;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esri.core.geometry.Point;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import eu.okatrych.rightsheet.RightSheetBehavior;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapsFragment extends Fragment implements OnCampusAdapter.ItemClickListener, FavAdapter.ItemClickListener, OffCampusAdapter.ItemClickListener, GameDayAdapter.ItemClickListener, SwipeAdapter.ItemClickListener {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 44;
    private OkHttpClient client;  // Client to make API requests
    public BottomSheetBehavior<View> standardBottomSheetBehavior;
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
    private RightSheetBehavior<View> rightSheetBehavior;
    private TableLayout tlTimetable;
    private TableLayout tl_times;
    private TextView viewMoreBtn;
    public static String currentRouteNo;
    public FloatingActionButton fabTimetable;
    public List<BusRoute> busRoutes;
    public GoogleMap mMap;       // The Map itself
    private Handler handler;
    private Runnable runnable;
    private ArrayList<Marker> busMarkers;
    public FloatingActionButton fabMyLocation;
    private LinearProgressIndicator dateProgress;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private final String KEY_CAMERA_POSITION = "camera_position";
    private final String KEY_LOCATION = "location";
    public FrameLayout standardBottomSheet;
    public LatLng deviceLatLng;
    private NestedScrollView vScroll;
    private FrameLayout rightSheet;

    @Override
    public void onItemClick(View view, int position) {
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        if (rightSheetBehavior.getState() != RightSheetBehavior.STATE_COLLAPSED)
            rightSheetBehavior.setState(RightSheetBehavior.STATE_COLLAPSED);
    }

    class TripType {
        public static final int WALK = 1;
        public static final int WALK_ADA = 2;
        public static final int DRIVE = 3;
        public static final int DRIVE_ADA = 4;
        public static final int BUS = 5;
        public static final int BUS_ADA = 6;
        public static final int BIKE = 7;
        public static final int VISITOR_DRIVE = 8;
        public static final int VISITOR_DRIVE_ADA = 9;

    }

    /*
     * Method to convert transportation coords to LatLng
     * returns Point
     */
    private static Point convertWebMercatorToLatLng(final double x, final double y) {
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
    private static BitmapDescriptor BitmapFromVector(Context context, int vectorResId, int color, int modifySize) {
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
     * Method to fetch and a bus route on the map
     */
    public void updateBusRoute(String routeNo, int color, boolean zoom, boolean routeIsDrawn) {
        try {
            // Make the API call
            String str = getApiCall("https://transport.tamu.edu/BusRoutesFeed/api/route/" + routeNo + "/pattern");

            // Create a builder for bounds to zoom to
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            // Parse the waypoints and stops
            JSONArray stops = new JSONArray(str);
            PolylineOptions polylineOptions = new PolylineOptions();
            LatLng first = null;

            ArrayList<Pair<String, LatLng>> busStops = new ArrayList<>();
            for (int i = 0; i < stops.length(); i++) {

                // Convert point and add to the polyline and builder
                Point p = convertWebMercatorToLatLng(stops.getJSONObject(i).getDouble("Longtitude"),
                        stops.getJSONObject(i).getDouble("Latitude"));
                double y = p.getX();
                double x = p.getY();
                if (i == 0) {
                    first = new LatLng(x, y);
                }
                Log.v("WAYPOINT/STOP", x + ", " + y);
                polylineOptions.add(new LatLng(x, y));
                builder.include(new LatLng(x, y));

                // Add bus stop circles
                if (stops.getJSONObject(i).getString("PointTypeCode").equals("1")) {
                    MarkerOptions marker = new MarkerOptions();
                    String title = stops.getJSONObject(i).getJSONObject("Stop").getString("Name");
                    marker.flat(true);
                    marker.icon(BitmapFromVector(getActivity(), R.drawable.checkbox_blank_circle, color, -20));
                    marker.title(title);
                    marker.anchor(0.5F, 0.5F);
                    marker.position(new LatLng(x, y));
                    if (!routeIsDrawn)
                        requireActivity().runOnUiThread(() -> mMap.addMarker(marker));
                    busStops.add(new Pair<>(title, new LatLng(x, y)));
                }
            }

            // Animate the camera to the new bounds
            int padding = 70;
            LatLngBounds bounds = builder.build();
            final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            if (zoom && !routeIsDrawn)
                requireActivity().runOnUiThread(() -> mMap.animateCamera(cu));

            // Draw polyline
            assert first != null;
            polylineOptions.add(first);
            polylineOptions.color(color);
            polylineOptions.width(10);
            polylineOptions.geodesic(true);
            polylineOptions.pattern(null);
            polylineOptions.clickable(true);
            AggieBusRoute newRoute = new AggieBusRoute(polylineOptions, busStops, bounds.northeast, bounds.southwest);
            if (!routeIsDrawn) {
                requireActivity().runOnUiThread(() -> mMap.addPolyline(polylineOptions));
                AggieBusRoute.writeData(requireActivity(), newRoute, routeNo);
            }

            Map<String, AggieBusRoute> route = AggieBusRoute.getData(requireActivity());
            if (routeIsDrawn && currentRouteNo.equals(routeNo)) {

                // Get AggiePolyline and check if the route doesn't already exist
                AggieBusRoute aggieBusRoute = route.get(routeNo);
                if (aggieBusRoute != null && !aggieBusRoute.equals(newRoute)) {
                    Log.v("DRAW_ROUTE", "New Route Added");

                    // Save the new route
                    AggieBusRoute.writeData(requireActivity(), newRoute, routeNo);

                    // Draw Polyline
                    requireActivity().runOnUiThread(() -> mMap.addPolyline(polylineOptions));

                    // Draw Bus Stops
                    for (Pair<String, LatLng> i : busStops) {
                        MarkerOptions marker = new MarkerOptions();
                        marker.flat(true);
                        marker.icon(BitmapFromVector(getActivity(), R.drawable.checkbox_blank_circle, color, -20));
                        marker.anchor(0.5F, 0.5F);
                        marker.position(i.second);
                        marker.title(i.first);
                        requireActivity().runOnUiThread(() -> mMap.addMarker(marker));
                    }

                    // Zoom in
                    if (zoom) requireActivity().runOnUiThread(() -> mMap.animateCamera(cu));
                }

            }

        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
    }

    /*
     * Method to draw a bus route on the map
     */
    public void drawBusRoute(String routeNo, int color, boolean zoom, boolean update) {

        // Check for cached data
        Map<String, AggieBusRoute> route = AggieBusRoute.getData(requireActivity());
        if (route.containsKey(routeNo)) {

            // Get AggiePolyline
            AggieBusRoute aggieBusRoute = route.get(routeNo);
            assert aggieBusRoute != null;

            // Draw polyline of route
            PolylineOptions newPolyline = aggieBusRoute.polylineOptions;
            newPolyline.color(color);
            requireActivity().runOnUiThread(() -> mMap.addPolyline(newPolyline));

            // Draw stops
            for (Pair<String, LatLng> i : aggieBusRoute.stops) {
                MarkerOptions marker = new MarkerOptions();
                marker.flat(true);
                marker.icon(BitmapFromVector(getActivity(), R.drawable.checkbox_blank_circle, color, -20));
                marker.anchor(0.5F, 0.5F);
                marker.title(i.first);
                marker.position(i.second);
                requireActivity().runOnUiThread(() -> mMap.addMarker(marker));
            }

            // Zoom to bounds
            int padding = 70;
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(aggieBusRoute.northEastBound);
            builder.include(aggieBusRoute.southWestBound);
            final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(), padding);
            if (zoom) requireActivity().runOnUiThread(() -> mMap.animateCamera(cu));

            if (update) updateBusRoute(routeNo, color, false, true);

        } else
            updateBusRoute(routeNo, color, zoom, false);  // Always update route in the background
    }


    /*
     * Method to draw all buses on a given route
     */
    public void drawBusesOnRoute(String routeNo) {
        try {
            // Get JSON Array of data from transportation API
            String API_url = "https://transport.tamu.edu/BusRoutesFeed/api/route/" + routeNo +
                    "/buses";
            JSONArray busData_jsonArray = new JSONArray(getApiCall(API_url));
            // Go through the JSON array to get each busses latitude, longitude, direction, and
            // occupancy.
            // Convert Lat and Lng using the helper convertWebMercatorToLatLng function and
            // add it to the marker.
            // Get the direction and use it to rotate the bus icon and add this to the marker.
            // Get the occupancy to show bus occupancy.
            if (!busMarkers.isEmpty() && busData_jsonArray.length() != busMarkers.size()) {
                for (Marker marker : busMarkers) {
                    requireActivity().runOnUiThread(marker::remove);
                }
                busMarkers.clear();
            }
            for (int i = 0; i < busData_jsonArray.length(); i++) {
                // Retrieving Data
                JSONObject currentBus = busData_jsonArray.getJSONObject(i);
                Point p = convertWebMercatorToLatLng(currentBus.getDouble("lng"),
                        currentBus.getDouble("lat"));
                float busDirection = (float) currentBus.getDouble("direction") - 90;
                String occupancy = currentBus.getString("occupancy");
                int finalI = i;
                if (!isAdded()) return;

                // Initialize Markers
                requireActivity().runOnUiThread(() -> {
                    if (busMarkers.size() < busData_jsonArray.length()) {
                        MarkerOptions marker = new MarkerOptions();
                        marker.flat(true);
                        marker.icon(BitmapFromVector(getActivity(), R.drawable.bus_articulated_front,
                                ContextCompat.getColor(requireActivity(), R.color.foreground), 5));
                        marker.zIndex(100);
                        marker.anchor(0.5F, 0.8F);
                        marker.position(new LatLng(p.getY(), p.getX()));
                        marker.rotation(busDirection);
                        marker.title("Occupancy: " + occupancy);
                        busMarkers.add(mMap.addMarker(marker));
                    }
                    // Update the existing Markers
                    else {
                        busMarkers.get(finalI).setPosition(new LatLng(p.getY(), p.getX()));
                        busMarkers.get(finalI).setRotation(busDirection);
                        busMarkers.get(finalI).setTitle("Occupancy: " + occupancy);
                    }
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            locationPermissionGranted = false;
        }
    }

    public void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            getLocationPermission();
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mMap.getUiSettings().setMapToolbarEnabled(true);
                fabMyLocation.setVisibility(View.VISIBLE);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mMap.getUiSettings().setMapToolbarEnabled(false);
                fabMyLocation.setVisibility(View.GONE);
                lastKnownLocation = null;
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            deviceLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(deviceLatLng, 14.0f));
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.");
                        Log.e(TAG, "Exception: %s", task.getException());
                        LatLng collegeStation = new LatLng(30.611812, -96.329767);
                        deviceLatLng = collegeStation;
                        mMap.animateCamera(CameraUpdateFactory
                                .newLatLngZoom(collegeStation, 14.0f));
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
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
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(collegeStation, 13.0f));

            // Set current map style
            SharedPreferences sharedPref = requireActivity().getSharedPreferences("com.mrst.aggiemaps.preferences", Context.MODE_PRIVATE);
            int currentNightMode = requireActivity().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            // If light mode is on
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                String maps = sharedPref.getString("light_maps", "light");
                switch (maps) {
                    case "light":
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.light));
                        break;
                    case "retro":
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.retro));
                        break;
                    case "classic":
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.classic));
                }
            }
            // If dark mode is on
            else if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {

                String maps = sharedPref.getString("dark_maps", "night");
                switch (maps) {
                    case "dark":
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.dark));
                        break;
                    case "sin_city":
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.sin_city));
                        break;
                    case "night":
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.night));
                        break;
                }
            } else {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.light));
            }

            // Turn on the My Location layer and the related control on the map.
            updateLocationUI();

            // Get the current location of the device and set the position of the map.
            getDeviceLocation();

            // Set Click Listener for polyline
            mMap.setOnPolylineClickListener(polyline -> {

                // Show title
                MarkerOptions title = new MarkerOptions();
                title.position(polyline.getPoints().get(0));
                title.title(polyline.getId());
                title.icon(BitmapFromVector(requireActivity(), R.drawable.arrow_left, android.R.color.transparent, 0));
                Marker m = mMap.addMarker(title);
                m.showInfoWindow();

                // Zoom out
                List<LatLng> points = polyline.getPoints();
                new Thread(() -> {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng i : points) {
                        builder.include(i);
                    }
                    int padding = 70;
                    LatLngBounds bounds = builder.build();
                    final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    requireActivity().runOnUiThread(() -> mMap.animateCamera(cu));
                }).start();
            });
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

        // Set default latlng value
        deviceLatLng = new LatLng(30.611812, -96.329767);

        // Inflate View
        View mView = inflater.inflate(R.layout.fragment_maps, container, false);
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        client = new OkHttpClient.Builder()  // Create OkHttpClient to be used in API requests
                .cache(new Cache(new File(requireActivity().getCacheDir(), "http_cache"),
                        50L * 1024L * 1024L))
                .build();
        favoritesText = mView.findViewById(R.id.favorites_text); // Initialize favorites text
        busMarkers = new ArrayList<>();
        handler = new Handler();

        new Thread(() -> {

            favRoutes = mView.findViewById(R.id.recycler_favorites);
            favAdapter = null;
            onCampusRoutes = mView.findViewById(R.id.recycler_oncampus);
            onCampusAdapter = null;
            offCampusRoutes = mView.findViewById(R.id.recycler_offcampus);
            offCampusAdapter = null;
            gameDayRoutes = mView.findViewById(R.id.recycler_gameday);
            gameDayAdapter = null;

            // Set decorations for the recyclers
            ColumnProvider col = () -> 1;
            favRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            favRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            DisplayMetrics metrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            col = () -> 2;
            onCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            offCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            gameDayRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            gameDayRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));

            // Set up the bottom sheet
            standardBottomSheet = mView.findViewById(R.id.standard_bottom_sheet);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;
            standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet);
            requireActivity().runOnUiThread(() -> {
                standardBottomSheetBehavior.setSaveFlags(BottomSheetBehavior.SAVE_ALL);
                standardBottomSheetBehavior.setHideable(false);
                standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                standardBottomSheetBehavior.setPeekHeight(0);
                standardBottomSheetBehavior.setHalfExpandedRatio(0.49f);

                // Set the max height of the bottom sheet by putting it below the searchbar
                View view = requireActivity().findViewById(R.id.main_app_bar);
                if (view instanceof AppBarLayout) {
                    ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
                    if (viewTreeObserver.isAlive()) {
                        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                standardBottomSheetBehavior.setMaxHeight(height - view.getHeight() - convertDpToPx(16));
                            }
                        });
                    }

                } else {
                    standardBottomSheetBehavior.setMaxHeight(height - convertDpToPx(80));
                }

                standardBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            requireActivity().findViewById(R.id.bottom_bar).setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        if (slideOffset < 0.08) {
                            requireActivity().findViewById(R.id.bottom_bar).setVisibility(View.VISIBLE);
                        } else {
                            requireActivity().findViewById(R.id.bottom_bar).setVisibility(View.GONE);
                        }
                    }
                });
            });

            // Set up right sheet for timetable
            rightSheet = mView.findViewById(R.id.timetable_sheet);
            rightSheetBehavior = RightSheetBehavior.from(rightSheet);
            requireActivity().runOnUiThread(() -> {
                rightSheetBehavior.setSaveFlags(RightSheetBehavior.SAVE_ALL);
                rightSheetBehavior.setHideable(false);
                rightSheetBehavior.setPeekWidth(0);
                rightSheetBehavior.setState(RightSheetBehavior.STATE_COLLAPSED);
            });
            tlTimetable = mView.findViewById(R.id.tl_timetable);
            tl_times = mView.findViewById(R.id.tl_times);
            viewMoreBtn = mView.findViewById(R.id.viewMoreBtn);
            vScroll = mView.findViewById(R.id.verticalScroll);

            // Initialize the fab to open the timetable
            requireActivity().runOnUiThread(() -> {
                fabTimetable = mView.findViewById(R.id.fab_timetable);
                fabTimetable.setVisibility(View.GONE);
                fabTimetable.setOnClickListener(v -> {
                    if (rightSheetBehavior.getState() == RightSheetBehavior.STATE_COLLAPSED) {
                        rightSheetBehavior.setState(RightSheetBehavior.STATE_EXPANDED);
                    } else {
                        rightSheetBehavior.setState(RightSheetBehavior.STATE_COLLAPSED);
                    }
                });
            });

            // Initialize the my current location FAB
            fabMyLocation = mView.findViewById(R.id.fab_mylocation);
            requireActivity().runOnUiThread(() -> {
                fabMyLocation.setOnClickListener(v -> getDeviceLocation());
            });

            // Initialize the Date Picker for the TimeTable
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select date")
                    .build();
            datePicker.addOnPositiveButtonClickListener(selection -> {
                tlTimetable.removeAllViews();
                tl_times.removeAllViews();
                String header = datePicker.getHeaderText();
                LocalDate ld;
                if (header.length() == 11) {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
                    ld = LocalDate.parse(datePicker.getHeaderText(), dateFormatter);
                } else {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                    ld = LocalDate.parse(datePicker.getHeaderText(), dateFormatter);
                }
                new Thread(() -> {
                    setUpTimeTable(ld.toString(), true);
                }).start();
            });

            // Initialize Try Another Date Button
            MaterialButton tryAnotherDate = mView.findViewById(R.id.try_another_date_button);
            requireActivity().runOnUiThread(() -> tryAnotherDate.setOnClickListener(v -> datePicker.show(requireActivity().getSupportFragmentManager(), "tag")));

            // Initialize the Progress Bar for after a user picks a date
            dateProgress = mView.findViewById(R.id.date_progress);
            requireActivity().runOnUiThread(() -> dateProgress.setVisibility(View.INVISIBLE));

            // Then set up the bus routes on the bottom sheet
            new Thread(this::setUpBusRoutes).start();

        }).start();

        return mView;
    }

    /*
     * Method to set the values within the table layout for the timetable
     */
    private void setUpTimeTable(String viewMoreTime, boolean viewAll) {
        try {
            int viewTimesAmt = 4;
            requireActivity().runOnUiThread(() -> viewMoreBtn.setVisibility(View.VISIBLE));
            if (viewAll) {
                viewTimesAmt = 900;
                requireActivity().runOnUiThread(() -> viewMoreBtn.setVisibility(View.GONE));
            }

            boolean isToday = viewMoreTime.equals(LocalDate.now().toString());

            requireActivity().runOnUiThread(() -> {
                dateProgress.setVisibility(View.VISIBLE);
            });
            String str = getApiCall("https://transport.tamu.edu/BusRoutesFeed/api/Route/" + currentRouteNo + "/timetable/" + viewMoreTime);

            // If nothing is returned
            if (str == null) return;
            if (str.equals("null")) {
                requireActivity().runOnUiThread(() -> fabTimetable.setVisibility(View.GONE));
                return;
            }
            JSONArray timetableArray = new JSONArray(str);
            int numRows = 0;

            // If no service is scheduled for this date
            if (timetableArray.getJSONObject(0).getString(timetableArray.getJSONObject(0).names().getString(0)).equals("No Service Is Scheduled For This Date")) {
                TableRow tr = new TableRow(getActivity());
                tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                TextView time = new TextView(getActivity());
                time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
                time.setPadding(0, 10, 10, 10);
                time.setText(timetableArray.getJSONObject(0).getString(" "));

                requireActivity().runOnUiThread(() -> {
                    tr.addView(time);
                    tlTimetable.addView(tr);
                    fabTimetable.setVisibility(View.VISIBLE);
                    dateProgress.setVisibility(View.INVISIBLE);
                    viewMoreBtn.setVisibility(View.GONE);
                });
                return;
            }

            // Add the stops as a header row to the table layout
            TableRow headerRow = new TableRow(getActivity());
            TableRow invisHeaderRow = new TableRow(getActivity());
            for (int i = 0; i < timetableArray.getJSONObject(0).names().length(); i++) {
                String header = timetableArray.getJSONObject(0).names().getString(i).substring(36);
                headerRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));

                TextView headerTV = new TextView(getActivity());
                headerTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
                headerTV.setPadding(0, 10, 40, 10);
                Typeface face = ResourcesCompat.getFont(requireActivity(), R.font.roboto_bold);
                headerTV.setTypeface(face);
                headerTV.setText(header);

                invisHeaderRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
//                invisHeaderRow.setVisibility(View.INVISIBLE);
                TextView invisHeaderTV = new TextView(getActivity());
                invisHeaderTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
                invisHeaderTV.setPadding(0, 10, 40, 10);
                Typeface face2 = ResourcesCompat.getFont(requireActivity(), R.font.roboto_bold);
                invisHeaderTV.setTypeface(face2);
                invisHeaderTV.setText(header);

                requireActivity().runOnUiThread(() -> {
//                    headerRow.addView(headerTV);
                    invisHeaderRow.addView(invisHeaderTV);
                });
            }
            requireActivity().runOnUiThread(() -> {
//                tlTimetable.addView(headerRow);
                tl_times.addView(invisHeaderRow);
            });

            // Loop through every row
            int cnt = 0;
            for (int i = 0; i < timetableArray.length(); i++) {
                JSONObject row = timetableArray.getJSONObject(i);

                // Find the current closest row for this date and start there
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
                String lastTime = row.getString(row.names().getString(row.names().length() - 1));
                LocalTime input;

                // Check to see if the last value in the list is null, if so skip it
                if (!lastTime.equals("null")) {
                    input = LocalTime.parse(lastTime, formatter);
                } else continue;

                // Only show 5 rows and only show rows that are after the current time
                if (isToday && input.isBefore(LocalTime.now())) {
                    cnt++;
                } else if (numRows++ <= viewTimesAmt) {
                    TableRow tr = new TableRow(getActivity());
                    tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    // Iterate through all JSONObject keys
                    Iterator<String> keys = row.keys();
                    while (keys.hasNext()) {
                        String value = row.getString(keys.next());
                        TextView time = new TextView(getActivity());
                        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                        time.setPadding(0, 10, 50, 10);

                        // Add strikethrough and red to times that have passed
                        if (!value.equals("null") && isToday && LocalTime.parse(value, formatter).isBefore(LocalTime.now())) {
                            time.setPaintFlags(time.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                            time.setTextColor(ContextCompat.getColor(requireActivity(), R.color.accent));
                        }

                        // If the value is null, just leave it empty
                        if (value.equals("null")) value = "";
                        time.setText(value);

                        requireActivity().runOnUiThread(() -> tr.addView(time));
                    }
                    requireActivity().runOnUiThread(() -> tl_times.addView(tr));
                } else break;
            }
            requireActivity().runOnUiThread(() -> {
                viewMoreBtn.setOnClickListener(view1 -> {
                    tlTimetable.removeAllViews();
                    tl_times.removeAllViews();
                    viewMoreBtn.setVisibility(View.GONE);
                    new Thread(() -> setUpTimeTable(viewMoreTime, true)).start();
                });
                if (viewAll) {
//                    vScroll.post(() -> vScroll.scrollTo(0, 15));
                } else {
                    vScroll.post(() -> vScroll.fullScroll(View.FOCUS_DOWN));
                }

                // Set the max height of the time table if there are too many times
                DisplayMetrics displayMetrics = new DisplayMetrics();
                requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int height = displayMetrics.heightPixels;
                View view = requireActivity().findViewById(R.id.main_app_bar);
                if (view instanceof AppBarLayout) {
                    ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
                    if (viewTreeObserver.isAlive()) {
                        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                if (tl_times.getChildCount() * convertDpToPx(35) > height - view.getHeight() - convertDpToPx(32) - fabTimetable.getHeight())
                                    vScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height - view.getHeight() - convertDpToPx(300) - fabTimetable.getHeight()));
                                else
                                    vScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            }
                        });
                    }
                }
            });

            // Edge case when no more rows to view
            if (numRows + 1 >= timetableArray.length() - cnt) {
                requireActivity().runOnUiThread(() -> viewMoreBtn.setVisibility(View.GONE));
            }

            // If no more bus routes are going today
            if (numRows == 0) {
                TableRow noTimesLeftRow = new TableRow(getActivity());
                TextView noTime = new TextView(getActivity());
                noTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
                noTime.setPadding(0, 10, 10, 10);
                noTime.setText(R.string.no_more_buses);
                requireActivity().runOnUiThread(() -> {
                    viewMoreBtn.setVisibility(View.GONE);
                    tlTimetable.removeAllViews();
                    tl_times.removeAllViews();
                    noTimesLeftRow.addView(noTime);
                    tlTimetable.addView(noTimesLeftRow);
                });
                requireActivity().runOnUiThread(() -> fabTimetable.setVisibility(View.VISIBLE));

            } else {
                requireActivity().runOnUiThread(() -> fabTimetable.setVisibility(View.VISIBLE));
            }
            requireActivity().runOnUiThread(() -> dateProgress.setVisibility(View.INVISIBLE));
        } catch (JSONException e) {
            requireActivity().runOnUiThread(() -> dateProgress.setVisibility(View.INVISIBLE));
            e.printStackTrace();
        }

    }

    private int convertDpToPx(int dp) {
        DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /*
     * Method to update the list of bus routes in the bottom sheet
     */
    private void updateBusRoutes() {
        try {
            String str = getApiCall("https://transport.tamu.edu/BusRoutesFeed/api/Routes");
            if (str == null) return;  // TODO: Call dialog or retry
            JSONArray routes = new JSONArray(str);
            favoritesSet = new HashSet<>();  // Initialize the favorites set
            loadFavorites();  // Load the favorites set from sharedpreferences

            // Initialize lists and add the `ALL` Route
            favList = new ArrayList<>();
            onList = new ArrayList<>();
            offList = new ArrayList<>();
            gameDayList = new ArrayList<>();
            busRoutes = new ArrayList<>();

            if (!isAdded()) return;
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

            // If no favorites, don't show the list
            if (favList.size() == 1) {
                requireActivity().runOnUiThread(() -> {
                    favRoutes.setVisibility(View.GONE);
                    favoritesText.setVisibility(View.GONE);
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        busRoutes.addAll(favList);
        busRoutes.addAll(onList);
        busRoutes.addAll(offList);
        busRoutes.addAll(gameDayList);
        AggieBusRoutes aggieBusRoutes = new AggieBusRoutes(favList, onList, offList, gameDayList);
        AggieBusRoutes.writeData(requireActivity(), aggieBusRoutes, "routes");
    }

    /*
     * Method to create and display all bus routes on the bottom sheet
     */
    private void setUpBusRoutes() {
        if (!isAdded()) return;
        Map<String, AggieBusRoutes> cachedRoutes = AggieBusRoutes.getData(requireActivity());
        if (cachedRoutes.containsKey("routes")) {
            AggieBusRoutes aBRs = Objects.requireNonNull(cachedRoutes.get("routes"));
            if (aBRs.favList != null) favList = aBRs.favList;
            else favList = new ArrayList<>();
            onList = aBRs.onList;
            offList = aBRs.offList;
            gameDayList = aBRs.gameDayList;

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

            // If no favorites, don't show the list
            if (favList.size() == 1) {
                requireActivity().runOnUiThread(() -> {
                    favRoutes.setVisibility(View.GONE);
                    favoritesText.setVisibility(View.GONE);
                });
            }
        }
        updateBusRoutes();  // Always update the bus routes on start
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
        if (!isAdded()) return;
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

            // Remove a route from its list
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
                    new Thread(() -> {
                        AggieBusRoutes aggieBusRoutes = new AggieBusRoutes(favList, onList, offList, gameDayList);
                        AggieBusRoutes.writeData(requireActivity(), aggieBusRoutes, "routes");
                        saveFavorites();
                    }).start();
                    return;
            }

            // If not an unfavorite add it to the favorites list and save
            favoritesSet.add(busRoute.routeNumber);
            favList.add(busRoute);
            favAdapter.notifyItemInserted(favList.size() - 1);
            favRoutes.setVisibility(View.VISIBLE);
            favoritesText.setVisibility(View.VISIBLE);
            new Thread(() -> {
                AggieBusRoutes aggieBusRoutes = new AggieBusRoutes(favList, onList, offList, gameDayList);
                AggieBusRoutes.writeData(requireActivity(), aggieBusRoutes, "routes");
                saveFavorites();
            }).start();
            return;  // skip showing the route if
        }

        // When a route is clicked, close sheet, set route number and draw route
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mMap.clear(); // Clear map first
        if (busRoute.routeNumber.equals("All")) {
            fabTimetable.setVisibility(View.GONE);
            new Thread(() -> {
                switch (busRoute.routeName) {
                    case "Favorites":
                        for (int i = 1; i < favAdapter.getItemCount(); i++) {
                            int finalI = i;
                            drawBusRoute(favAdapter.getItem(finalI).routeNumber, favAdapter.getItem(finalI).color, false, false);
                        }
                        break;
                    case "On Campus":
                        for (int i = 1; i < onCampusAdapter.getItemCount(); i++) {
                            int finalI = i;
                            drawBusRoute(onCampusAdapter.getItem(finalI).routeNumber, onCampusAdapter.getItem(finalI).color, false, false);
                        }
                        break;
                    case "Off Campus":
                        for (int i = 1; i < offCampusAdapter.getItemCount(); i++) {
                            int finalI = i;
                            drawBusRoute(offCampusAdapter.getItem(finalI).routeNumber, offCampusAdapter.getItem(finalI).color, false, false);
                        }
                        break;
                    case "Game Day":
                        for (int i = 1; i < gameDayAdapter.getItemCount(); i++) {
                            int finalI = i;
                            drawBusRoute(gameDayAdapter.getItem(finalI).routeNumber, gameDayAdapter.getItem(finalI).color, false, false);
                        }
                        break;
                }
            }).start();
        } else {
            currentRouteNo = busRoute.routeNumber;

            // Set the values for the timetable right sheet
            tlTimetable.removeAllViews();
            tl_times.removeAllViews();
            new Thread(() -> setUpTimeTable(LocalDate.now().toString(), false)).start();

            // Draw the route
            new Thread(() -> drawBusRoute(busRoute.routeNumber, busRoute.color, true, true)).start();

            // Continuously draw the buses on the route
            handler = new Handler();
            busMarkers.clear();
            handler.post(runnable = () -> {
                handler.postDelayed(runnable, 3000);
                if (currentRouteNo.equals(busRoute.routeNumber))
                    new Thread(() -> drawBusesOnRoute(busRoute.routeNumber)).start();
            });
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
