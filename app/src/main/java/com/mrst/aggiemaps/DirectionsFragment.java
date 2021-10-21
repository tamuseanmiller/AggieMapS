package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;

import static com.mrst.aggiemaps.MainActivity.DEST_SEARCH_BAR;
import static com.mrst.aggiemaps.MainActivity.MAIN_SEARCH_BAR;
import static com.mrst.aggiemaps.MainActivity.SRC_SEARCH_BAR;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lapism.search.widget.MaterialSearchBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import eu.okatrych.rightsheet.RightSheetBehavior;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DirectionsFragment extends Fragment {

    private MaterialSearchBar srcSearchBar;
    private MaterialSearchBar destSearchBar;
    private RecyclerView directionsRecycler;
    private LinearLayout llSrcDestContainer;
    private FloatingActionButton fabCancel;
    private FloatingActionButton fabSwap;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private DirectionsAdapter directionsAdapter;
    private FrameLayout sheet;
    private OkHttpClient client;  // Client to make API requests

    private static final String KEY_LOCATION = "location";
    private static final int DEFAULT_DIRECTION = 0;
    public GoogleMap mMap;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FloatingActionButton fabMyLocation;
    private ListItem srcItem;
    private ListItem destItem;
    public ChipGroup tripTypeGroup;

    public void clearFocusOnSearch() {
        llSrcDestContainer.setVisibility(View.VISIBLE);
        if (srcItem != null && destItem != null)
            sheet.setVisibility(View.VISIBLE);
        showSystemUI();
    }

    private void requestFocusOnSearch(int whichSearchBar) {
        llSrcDestContainer.setVisibility(View.GONE);
        if (srcItem != null && destItem != null)
            sheet.setVisibility(View.GONE);
        hideSystemUI();
        ((MainActivity) requireActivity()).whichSearchBar = whichSearchBar;
        ((MainActivity) requireActivity()).requestFocusOnSearch(whichSearchBar);
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

    private Drawable parseManeuverType(String maneuverType) {
        switch (maneuverType) {
            case "esriDMTStop":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.close_octagon);
            case "esriDMTStraight":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.arrow_up);
            case "esriDMTBearLeft":
            case "esriDMTRampLeft":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.arrow_top_left);
            case "esriDMTBearRight":
            case "esriDMTRampRight":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.arrow_top_right);
            case "esriDMTTurnLeft":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.arrow_left_top);
            case "esriDMTTurnRight":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.arrow_right_top);
            case "esriDMTSharpLeft":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.arrow_left);
            case "esriDMTSharpRight":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.arrow_right);
            case "esriDMTUTurn":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.arrow_u_down_left);
            case "esriDMTFerry":
            case "esriDMTEndOfFerry":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.ferry);
            case "esriDMTRoundabout":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.rotate_left);
            case "esriDMTHighwayMerge":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.call_merge);
            case "esriDMTHighwayExit":
            case "esriDMTForkCenter":
            case "esriDMTForkLeft":
            case "esriDMTForkRight":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.call_split);
            case "esriDMTHighwayChange":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.source_fork);
            case "esriDMTDepart":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.car);
            case "esriDMTTripItem":
                return ContextCompat.getDrawable(requireActivity(), R.drawable.sign_direction);
            default:
                return ContextCompat.getDrawable(requireActivity(), R.drawable.nuke);
        }
    }

    /*
     * Method to create array of a route from two latlng coordinates
     * returns a TripPlan obj
     */
    private TripPlan getTripPlan(LatLng src, LatLng dest, int tripType) {
        try {

            String call = "https://gis.tamu.edu/arcgis/rest/services/Routing/ChrisRoutingTest/NAServer/Route/solve?stops=%7B%22features%22%3A%5B%7B%22geometry%22%3A%7B%22x%22%3A" + src.longitude + "%2C%22y%22%3A" + src.latitude + "%7D%2C%22attributes%22%3A%7B%22Name%22%3A%22From%22%2C%22RouteName%22%3A%22Route+A%22%7D%7D%2C%7B%22geometry%22%3A%7B%22x%22%3A" + dest.longitude + "%2C%22y%22%3A" + dest.latitude + "%7D%2C%22attributes%22%3A%7B%22Name%22%3A%22To%22%2C%22RouteName%22%3A%22Route+A%22%7D%7D%5D%7D&outSR=4326&ignoreInvalidLocations=true&accumulateAttributeNames=Length%2C+Time&impedanceAttributeName=Time&restrictUTurns=esriNFSBAllowBacktrack&useHierarchy=false&returnDirections=true&returnRoutes=true&returnStops=false&returnBarriers=false&returnPolylineBarriers=false&returnPolygonBarriers=false&directionsLanguage=en&outputLines=esriNAOutputLineTrueShapeWithMeasure&findBestSequence=true&preserveFirstStop=true&preserveLastStop=true&useTimeWindows=false&timeWindowsAreUTC=false&startTime=5&startTimeIsUTC=false&outputGeometryPrecisionUnits=esriMiles&directionsOutputType=esriDOTComplete&directionsTimeAttributeName=Time&directionsLengthUnits=esriNAUMiles&returnZ=false&travelMode=" + tripType + "&f=pjson";
            String result = getApiCall(call);
            System.out.println((result));
            JSONArray features_json = new JSONObject(result).getJSONArray("directions").getJSONObject(0).getJSONArray("features");

            // Parse every feature
            ArrayList<Feature> features = new ArrayList<>();
            for (int i = 0; i < features_json.length(); i++) {
                JSONObject attributes = features_json.getJSONObject(i).getJSONObject("attributes");
                Drawable manueverType = parseManeuverType(attributes.getString("maneuverType"));
                int length = attributes.getInt("length");
                int time = attributes.getInt("time");
                String text = attributes.getString("text");
                int ETA = attributes.getInt("ETA");
                Feature new_feature = new Feature(length, time, text, ETA, manueverType);
                features.add(new_feature);
            }
//            JSONArray routes = new JSONObject(result).getJSONObject("routes").getJSONArray("features");
//            // parsing routes
//            JSONObject spatialReference = new JSONObject(result).getJSONObject("routes").getJSONObject("spatialReference");
//            String geometryType = new JSONObject(result).getJSONObject("routes").getString("geometryType");
//            JSONObject attributes = new JSONObject(result).getJSONObject("routes").getJSONArray("features").getJSONObject(0).getJSONObject("attributes");
//            JSONObject geometry_json = new JSONObject(result).getJSONObject("routes").getJSONArray("features").getJSONObject(0).getJSONObject("geometry");
//            JSONArray directions = new JSONObject(result).getJSONArray("directions");

            // Parse all of the geometry
            JSONArray paths = new JSONObject(result).getJSONObject("routes").getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("paths").getJSONArray(0);
            ArrayList<LatLng> geometry = new ArrayList<>();
            for (int i = 0; i < paths.length(); i++) {
                LatLng new_latlng = new LatLng(paths.getJSONArray(i).getDouble(1), paths.getJSONArray(i).getDouble(0));
                geometry.add(new_latlng);
            }

            // Create a builder for bounds to zoom to
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            // Draw line
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(10);
            polylineOptions.geodesic(true);
            polylineOptions.pattern(null);
            polylineOptions.clickable(true);
            polylineOptions.color(ContextCompat.getColor(requireActivity(), R.color.accent));
            for (int i = 0; i < paths.length(); i++) {
                double lat = paths.getJSONArray(i).getDouble(0);
                double lng = paths.getJSONArray(i).getDouble(1);
                LatLng latlng = new LatLng(lng, lat);
                polylineOptions.add(latlng);
                builder.include(latlng);
            }

            // Animate the camera to the new bounds
            int padding = 70;
            LatLngBounds bounds = builder.build();
            final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            requireActivity().runOnUiThread(() -> mMap.animateCamera(cu));
            requireActivity().runOnUiThread(() -> {
                mMap.addPolyline(polylineOptions);  // Add polylin
                mMap.animateCamera(cu);
            });

            // Parse summary information
            JSONObject summary = new JSONObject(result).getJSONArray("directions").getJSONObject(0).getJSONObject("summary");
            double totalTime = summary.getDouble("totalTime");
            double totalLength = summary.getDouble("totalLength");
            double totalDriveTime = summary.getDouble("totalDriveTime");


            return new TripPlan(geometry, features, totalLength, totalTime, totalDriveTime);
        } catch (JSONException e) {
            Log.e("MYAPP", "unexpected JSON exception", e);
            e.printStackTrace();
            // Do something to recover ...
        }
        return null;
    }

    /*
     * Un-Show the navigation bar and get out of full screen
     */
    private void hideSystemUI() {
        View decorView = requireActivity().getWindow().getDecorView();
        int currentNightMode = Configuration.UI_MODE_NIGHT_MASK & getResources().getConfiguration().uiMode;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                // Night mode is not active, we're using the light theme
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                // Night mode is active, we're using dark theme
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
                break;
        }
    }

    /*
     * Show the navigation bar and get out of full screen
     */
    private void showSystemUI() {
        View decorView = requireActivity().getWindow().getDecorView();

        int currentNightMode = Configuration.UI_MODE_NIGHT_MASK & getResources().getConfiguration().uiMode;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                // Night mode is not active, we're using the light theme
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                // Night mode is active, we're using dark theme
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                break;
        }
    }

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
        }
    };

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
                            LatLng deviceLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(deviceLatLng, 14.0f));
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.");
                        Log.e(TAG, "Exception: %s", task.getException());
                        LatLng collegeStation = new LatLng(30.611812, -96.329767);
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

    private void updateLocationUI() {
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

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_directions, container, false);

        client = new OkHttpClient();  // Create OkHttpClient to be used in API request

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize my location FAB
        fabMyLocation = mView.findViewById(R.id.fab_mylocation);
        fabMyLocation.setOnClickListener(v -> {
            LatLng currentLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 13.0f));
        });

        directionsRecycler = mView.findViewById(R.id.directions_recycler);
        directionsRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        // 2. Initialize SearchBars
        srcSearchBar = mView.findViewById(R.id.src_search_bar);
        destSearchBar = mView.findViewById(R.id.dest_search_bar);

        // 4. Create the views for the SearchBars
        srcSearchBar.setOnClickListener(v -> requestFocusOnSearch(SRC_SEARCH_BAR));
        destSearchBar.setOnClickListener(v -> requestFocusOnSearch(DEST_SEARCH_BAR));
        srcSearchBar.setElevation(5);
        srcSearchBar.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.background));
        srcSearchBar.setNavigationOnClickListener(v -> requestFocusOnSearch(SRC_SEARCH_BAR));
        destSearchBar.setElevation(5);
        destSearchBar.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.background));
        destSearchBar.setNavigationOnClickListener(v -> requestFocusOnSearch(DEST_SEARCH_BAR));
        srcSearchBar.setHint("Choose starting point");
        destSearchBar.setHint("Choose destination");
        srcItem = null;
        destItem = null;

        // 5. Set the SearchView Settings
        // reuse materialSearchView settings

        // 6. Initialize the BottomSheet
        sheet = mView.findViewById(R.id.directions_bottom_sheet);

        // 7. Get the BottomSheetBehavior
        bottomSheetBehavior = BottomSheetBehavior.from(sheet);

        // 8. Set the settings of the BottomSheetBehavior
        bottomSheetBehavior.setSaveFlags(RightSheetBehavior.SAVE_ALL);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(mView.findViewById(R.id.cl_directions).getMeasuredHeight() + convertDpToPx(135));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Set the max height of the bottom sheet by putting it below the searchbar
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;

        // 9. Initialize Progress Indicator
        //tripProgress = findViewById(R.id.trip_progress);

        // 10. Initialize Main App Bar
        View view = requireActivity().findViewById(R.id.main_app_bar);
        if (view instanceof AppBarLayout) {
            ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        bottomSheetBehavior.setMaxHeight(height - view.getHeight() * 2);
                    }
                });
            }

        } else {
            bottomSheetBehavior.setMaxHeight(height - convertDpToPx(80));
        }

        // 11. Initialize Source and Dest Container
        llSrcDestContainer = mView.findViewById(R.id.ll_srcdest);

        // Initialize cancel fab and click listener
        fabCancel = mView.findViewById(R.id.fab_cancel);
        fabCancel.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).exitDirectionsMode();
            exitDirections();
        });

        fabSwap = mView.findViewById(R.id.fab_swap);
        fabSwap.setOnClickListener(v -> swapDirections());

        tripTypeGroup = mView.findViewById(R.id.trip_type_group);
        tripTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (sheet.getVisibility() == View.VISIBLE) {
                ((MainActivity)requireActivity()).whichSearchBar = DEST_SEARCH_BAR;
                createDirections(destItem);
            }
        });

        return mView;
    }

    private void swapDirections() {
        if (srcItem != null && destItem != null) {
            srcSearchBar.setText(destItem.title);
            destSearchBar.setText(srcItem.title);
            ListItem tempItem = srcItem;
            srcItem = destItem;
            destItem = tempItem;
        } else if (srcItem != null) {
            destItem = srcItem;
            destSearchBar.setText(srcItem.title);
            srcItem = null;
            srcSearchBar.setText("");
            srcSearchBar.setHint("Choose starting point");
        } else if (destItem != null) {
            srcItem = destItem;
            srcSearchBar.setText(destItem.title);
            destItem = null;
            destSearchBar.setText("");
            destSearchBar.setHint("Choose destination");
        }

    }

    private int convertDpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    public void createDirections(ListItem itemTapped) {
        mMap.clear();
        if (itemTapped != null) {
            int whichSearchBar = ((MainActivity) requireActivity()).whichSearchBar;
            if (whichSearchBar == MAIN_SEARCH_BAR && srcItem == null && locationPermissionGranted) {
                LatLng currLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                srcSearchBar.setText("Current location");
                destSearchBar.setText(itemTapped.title);
                destItem = itemTapped;
                srcItem = new ListItem("Current Location", "", 0, null, MainActivity.SearchTag.RESULT, currLocation);
            } else if (whichSearchBar == SRC_SEARCH_BAR) {
                srcSearchBar.setText(itemTapped.title);
                srcItem = itemTapped;
            } else if (whichSearchBar == DEST_SEARCH_BAR) {
                destSearchBar.setText(itemTapped.title);
                destItem = itemTapped;
            }
            
            if (destItem != null && srcItem != null) {

                // Get Trip Plan and input into
                new Thread(() -> {
                    TripPlan newTripPlan;
                    switch(tripTypeGroup.getCheckedChipId()) {
                        case 1: // Car
                            newTripPlan = getTripPlan(srcItem.position, destItem.position, MapsFragment.TripType.DRIVE);
                            break;
                        case 2: // Bus
                            newTripPlan = getTripPlan(srcItem.position, destItem.position, MapsFragment.TripType.BUS);
                            break;
                        case 3: // Bike
                            newTripPlan = getTripPlan(srcItem.position, destItem.position, MapsFragment.TripType.BIKE);
                            break;
                        case 4: // Walk
                            newTripPlan = getTripPlan(srcItem.position, destItem.position, MapsFragment.TripType.WALK);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + tripTypeGroup.getCheckedChipId());
                    }
                    ArrayList<ListItem> textDirections = new ArrayList<>();
                    ArrayList<Feature> routeFeatures = newTripPlan.getFeatures();
                    for (int i = 0; i < routeFeatures.size(); i++) {
                        Feature currFeature = routeFeatures.get(i);
                        // TODO: fix to add parsing direction type
                        textDirections.add(new ListItem(currFeature.getText(), String.valueOf(currFeature.getLength()), 0, currFeature.getManeuverType(), MainActivity.SearchTag.RESULT, null));
                    }

                    // Parse the trip plan into the BottomBar
                    requireActivity().runOnUiThread(() -> {
                        directionsAdapter = new DirectionsAdapter(getActivity(), textDirections);
                        directionsRecycler.setAdapter(directionsAdapter);
                    });
                }).start();

                // Change the visibility of the BottomBar to "visible"
                sheet.setVisibility(View.VISIBLE);
                fabMyLocation.setVisibility(View.GONE);

            }
        }
    }

    private void exitDirections() {

        // Clear the map
        mMap.clear();

        // Change the visibility of the BottomBar to "gone"
        sheet.setVisibility(View.GONE);
        fabMyLocation.setVisibility(View.VISIBLE);

        // Reset the hints
        srcSearchBar.setText("");
        destSearchBar.setText("");
        srcSearchBar.setHint("Choose starting point");
        destSearchBar.setHint("Choose destination");
        srcItem = null;
        destItem = null;

    }
}