package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;
import static com.mrst.aggiemaps.MainActivity.DEST_SEARCH_BAR;
import static com.mrst.aggiemaps.MainActivity.MAIN_SEARCH_BAR;
import static com.mrst.aggiemaps.MainActivity.SRC_SEARCH_BAR;
import static java.lang.Math.abs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.lapism.search.widget.MaterialSearchBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
    public FrameLayout sheet;
    private OkHttpClient client;  // Client to make API requests
    public GoogleMap mMap;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FloatingActionButton fabMyLocation;
    private ListItem srcItem;
    private ListItem destItem;
    public ChipGroup tripTypeGroup;
    private TextView tripTime;
    private TextView tripLength;
    private TextView etaClockTime;
    private ImageView tripTypeIcon;
    private MaterialButton directionsButton;
    private CircularProgressIndicator tripProgress;
    private ArrayList<ListItem> textDirections;
    private Chip adaChip;

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

    public void clearFocusOnSearch() {
        llSrcDestContainer.setVisibility(View.VISIBLE);
        if (srcItem != null && destItem != null)
            sheet.setVisibility(View.VISIBLE);

        // Set the status and nav bar color
        requireActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
        requireActivity().getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    private void requestFocusOnSearch(int whichSearchBar) {
        llSrcDestContainer.setVisibility(View.GONE);
        if (srcItem != null && destItem != null)
            sheet.setVisibility(View.GONE);
        ((MainActivity) requireActivity()).whichSearchBar = whichSearchBar;
        ((MainActivity) requireActivity()).requestFocusOnSearch(whichSearchBar);

        // Set the status and nav bar color
        requireActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireActivity(), R.color.background));
        requireActivity().getWindow().setNavigationBarColor(ContextCompat.getColor(requireActivity(), R.color.background));
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
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                System.out.println(body);
                return Objects.requireNonNull(body).string(); // Return the response as a string
            } else {
                // notify error
                String errorCode = Integer.toString(response.code());
                String errorMessage = response.message();
                Snackbar snackbar = Snackbar.make(requireActivity().findViewById(R.id.cl_main), "Error Code: " + errorCode + " " + errorMessage, Snackbar.LENGTH_LONG);
                snackbar.setAction("OK", view -> snackbar.dismiss());
                snackbar.setBackgroundTint(ContextCompat.getColor(requireActivity(), R.color.foreground));
                snackbar.setActionTextColor(ContextCompat.getColor(requireActivity(), R.color.background));
                snackbar.show();
                return "";
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private @DrawableRes
    int parseManeuverType(String maneuverType) {
        switch (maneuverType) {
            case "esriDMTStop":
                return R.drawable.map_marker_outline;
            case "esriDMTStraight":
                return R.drawable.arrow_up;
            case "esriDMTBearLeft":
            case "esriDMTRampLeft":
                return R.drawable.arrow_top_left;
            case "esriDMTBearRight":
            case "esriDMTRampRight":
                return R.drawable.arrow_top_right;
            case "esriDMTTurnLeft":
                return R.drawable.arrow_left_top;
            case "esriDMTTurnRight":
                return R.drawable.arrow_right_top;
            case "esriDMTSharpLeft":
                return R.drawable.arrow_left;
            case "esriDMTSharpRight":
                return R.drawable.arrow_right;
            case "esriDMTUTurn":
                return R.drawable.arrow_u_down_left;
            case "esriDMTFerry":
            case "esriDMTEndOfFerry":
                return R.drawable.ferry;
            case "esriDMTRoundabout":
                return R.drawable.rotate_left;
            case "esriDMTHighwayMerge":
                return R.drawable.call_merge;
            case "esriDMTHighwayExit":
            case "esriDMTForkCenter":
            case "esriDMTForkLeft":
            case "esriDMTForkRight":
                return R.drawable.call_split;
            case "esriDMTHighwayChange":
                return R.drawable.source_fork;
            case "esriDMTDepart":
                return R.drawable.map_marker_outline;
            case "esriDMTTripItem":
                return R.drawable.sign_direction;
            default:
                return R.drawable.checkbox_blank_circle;
        }
    }

    /*
     * Method to get zoom padding when zooming in on route
     */
    public int getZoomPadding(LatLngBounds routeBounds) {

        // Get ratio of route height to width
        double heightRoute = abs(routeBounds.northeast.latitude - routeBounds.southwest.latitude);
        double widthRoute = abs(routeBounds.northeast.longitude - routeBounds.southwest.longitude);
        double routeRatio = widthRoute / heightRoute;

        // Get screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) requireContext()).getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displayMetrics);
        int widthScreen = displayMetrics.widthPixels;

        // Set padding based on the route form factor in relation to width of the screen (might need to fix for tablets)
        int padding = (int) (widthScreen / (7 * routeRatio));
        return padding;
    }

    /*
     * Method to create array of a route from two latlng coordinates
     * returns a TripPlan obj
     */
    public TripPlan getTripPlan(LatLng src, LatLng dest, int tripType) {
        try {
            if (adaChip.isChecked() && tripType != TripType.BIKE) tripType++;  // Check for ADA
            String call = "https://gis.tamu.edu/arcgis/rest/services/Routing/20210825/NAServer/Route/solve?stops=%7B%22features%22%3A%5B%7B%22geometry%22%3A%7B%22spatialReference%22%3A%7B%22wkid%22%3A4326%7D%2C%22x%22%3A" + src.longitude + "%2C%22y%22%3A" + src.latitude + "%7D%2C%22symbol%22%3Anull%2C%22attributes%22%3A%7B%22routeName%22%3A8%2C%22stopName%22%3A%22Current+Location%22%7D%2C%22popupTemplate%22%3Anull%7D%2C%7B%22geometry%22%3A%7B%22spatialReference%22%3A%7B%22wkid%22%3A4326%7D%2C%22x%22%3A" + dest.longitude + "%2C%22y%22%3A" + dest.latitude + "%7D%2C%22symbol%22%3Anull%2C%22attributes%22%3A%7B%22routeName%22%3A8%2C%22stopName%22%3A%22Zachry+Engineering+Education+Complex%22%7D%2C%22popupTemplate%22%3Anull%7D%5D%7D&barriers=&polylineBarriers=&polygonBarriers=&outSR=4326&ignoreInvalidLocations=true&accumulateAttributeNames=Length%2C+Time&impedanceAttributeName=Time&restrictionAttributeNames=ADA%2C+Doors%2C+No+Bike%2C+No+Bus%2C+No+Drive%2C+One+Way%2C+Visitor%2C+No+Walk+OffCampus&attributeParameterValues=&restrictUTurns=esriNFSBAllowBacktrack&useHierarchy=false&returnDirections=true&returnRoutes=true&returnStops=false&returnBarriers=false&returnPolylineBarriers=false&returnPolygonBarriers=false&directionsLanguage=en&directionsStyleName=&outputLines=esriNAOutputLineTrueShape&findBestSequence=false&preserveFirstStop=true&preserveLastStop=true&useTimeWindows=false&timeWindowsAreUTC=false&startTime=0&startTimeIsUTC=true&outputGeometryPrecision=&outputGeometryPrecisionUnits=esriMeters&directionsOutputType=esriDOTComplete&directionsTimeAttributeName=Time&directionsLengthUnits=esriNAUMiles&returnZ=false&travelMode=" + tripType + "&overrides=&f=pjson";
            //String call = "https://gis.tamu.edu/arcgis/rest/services/Routing/20210825/NAServer/Route/solve?doNotLocateOnRestrictedElements=true&outputLines=esriNAOutputLineTrueShape&outSR=4326&returnBarriers=false&returnDirections=true&returnPolygonBarriers=false&returnPolylineBarriers=false&returnRoutes=true&returnStops=false&returnZ=false&startTimeIsUTC=true&stops=%7B%22features%22%3A%5B%7B%22geometry%22%3A%7B%22spatialReference%22%3A%7B%22wkid%22%3A4326%7D%2C%22x%22%3A" + src.longitude + "%2C%22y%22%3A" + src.latitude + "%7D%2C%22symbol%22%3Anull%2C%22attributes%22%3A%7B%22routeName%22%3A2%2C%22stopName%22%3A%22Current%20Location%22%7D%2C%22popupTemplate%22%3Anull%7D%2C%7B%22geometry%22%3A%7B%22spatialReference%22%3A%7B%22wkid%22%3A4326%7D%2C%22x%22%3A" + dest.longitude + "%2C%22y%22%3A" + dest.latitude + "%7D%2C%22symbol%22%3Anull%2C%22attributes%22%3A%7B%22routeName%22%3A2%2C%22stopName%22%3A%22Zachry%20Engineering%20Education%20Complex%22%7D%2C%22popupTemplate%22%3Anull%7D%5D%7D&travelMode=" + tripType + "&f=pjson";
            //String call = "https://gis.tamu.edu/arcgis/rest/services/Routing/ChrisRoutingTest/NAServer/Route/solve?stops=%7B%22features%22%3A%5B%7B%22geometry%22%3A%7B%22x%22%3A" + src.longitude + "%2C%22y%22%3A" + src.latitude + "%7D%2C%22attributes%22%3A%7B%22Name%22%3A%22From%22%2C%22RouteName%22%3A%22Route+A%22%7D%7D%2C%7B%22geometry%22%3A%7B%22x%22%3A" + dest.longitude + "%2C%22y%22%3A" + dest.latitude + "%7D%2C%22attributes%22%3A%7B%22Name%22%3A%22To%22%2C%22RouteName%22%3A%22Route+A%22%7D%7D%5D%7D&outSR=4326&ignoreInvalidLocations=true&accumulateAttributeNames=Length%2C+Time&impedanceAttributeName=Time&restrictUTurns=esriNFSBAllowBacktrack&useHierarchy=false&returnDirections=true&returnRoutes=true&returnStops=false&returnBarriers=false&returnPolylineBarriers=false&returnPolygonBarriers=false&directionsLanguage=en&outputLines=esriNAOutputLineTrueShapeWithMeasure&findBestSequence=true&preserveFirstStop=true&preserveLastStop=true&useTimeWindows=false&timeWindowsAreUTC=false&startTime=5&startTimeIsUTC=false&outputGeometryPrecisionUnits=esriMiles&directionsOutputType=esriDOTComplete&directionsTimeAttributeName=Time&directionsLengthUnits=esriNAUMiles&returnZ=false&travelMode=" + tripType + "&f=pjson";
            String result = getApiCall(call);
            System.out.println((result));

            JSONArray features_json = new JSONObject(result).getJSONArray("directions").getJSONObject(0).getJSONArray("features");

            Log.d("ROUTING", String.valueOf(features_json));
            // Parse every feature
            ArrayList<Feature> features = new ArrayList<>();
            for (int i = 0; i < features_json.length(); i++) {
                JSONObject attributes = features_json.getJSONObject(i).getJSONObject("attributes");
                @DrawableRes int manueverType = parseManeuverType(attributes.getString("maneuverType"));
                double length = attributes.getDouble("length");
                double time = attributes.getDouble("time");
                String text = attributes.getString("text");
                int ETA = attributes.getInt("ETA");

                // Add feature
                Feature new_feature = new Feature(length, time, text, ETA, manueverType);
                features.add(new_feature);

                // Add landmarks
                if (features_json.getJSONObject(i).has("events")) {
                    JSONObject events = features_json.getJSONObject(i).getJSONArray("events").getJSONObject(0);
                    // Look through all events
                    if (events.has("strings")) {
                        JSONArray strings = events.getJSONArray("strings");
                        // Look through all strings in events
                        for (int k = 0; k < strings.length(); k++) {
                            // If a landmark exists
                            if (strings.getJSONObject(k).has("stringType") &&
                                    strings.getJSONObject(k).getString("stringType").equals("esriDSTGeneral")) {
                                String landmarkText = strings.getJSONObject(k).getString("string");

                                // Change the text from "Make the maneuver" to "Pass"
                                if (landmarkText.startsWith("Make the maneuver"))
                                    landmarkText = landmarkText.replaceFirst("Make the maneuver", "Pass");

                                // Add landmark feature
                                features.add(new Feature(FeatureType.LANDMARK, landmarkText));
                            }
                        }
                    }
                }
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
            if (features_json.length() > 3) {
                // Create a builder for bounds to zoom to
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                // Draw line
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.width(10);
                polylineOptions.geodesic(true);
                polylineOptions.pattern(null);
                polylineOptions.clickable(true);
                polylineOptions.color(ContextCompat.getColor(requireActivity(), R.color.accent));
                MarkerOptions endMarker = new MarkerOptions();
                for (int i = 0; i < paths.length(); i++) {
                    double lat = paths.getJSONArray(i).getDouble(0);
                    double lng = paths.getJSONArray(i).getDouble(1);
                    LatLng latlng = new LatLng(lng, lat);
                    polylineOptions.add(latlng);
                    builder.include(latlng);
                    if (i == paths.length() - 1) {
                        endMarker.position(latlng);
                        endMarker.draggable(false);
                    }
                }

                // Animate the camera to the new bounds
                LatLngBounds bounds = builder.build();
                int padding = getZoomPadding(bounds); // TODO: offset this padding towards bottom
                final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 0);
                requireActivity().runOnUiThread(() -> {
                    mMap.setPadding(padding, (int) (padding * 1.5), padding, padding / 2);
                    mMap.animateCamera(cu);
                    mMap.setPadding(0, 0, 0, 0);
                    mMap.addPolyline(polylineOptions);  // Add polyline
                    mMap.addMarker(endMarker);
                });
            }
            // Parse summary information
            JSONObject summary = new JSONObject(result).getJSONArray("directions").getJSONObject(0).getJSONObject("summary");
            double totalTime = summary.getDouble("totalTime");
            double totalLength = summary.getDouble("totalLength");
            double totalDriveTime = summary.getDouble("totalDriveTime");

            return new TripPlan(geometry, features, totalLength, totalTime, totalDriveTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private final OnMapReadyCallback callback = new OnMapReadyCallback() {

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
        public void onMapReady(@NonNull GoogleMap googleMap) {
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

            // Setting a click event handler for the map
            mMap.setOnMapClickListener(latLng -> {

                // Don't do anything if directions are being shown
                if (srcItem != null && destItem != null) return;

                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Setting the position for the marker
                markerOptions.position(latLng);

                // Setting the title for the marker.
                // This will be displayed on taping the marker
                markerOptions.title(latLng.latitude + ", " + latLng.longitude);

                // Placing a marker on the touched position
                googleMap.addMarker(markerOptions);

                // Add location to one of the bars
                if (srcItem == null && destItem == null) {
                    srcItem = new ListItem(String.format("%.4f, %.4f", latLng.latitude, latLng.longitude), "", 0, MainActivity.SearchTag.RESULT, latLng);
                    srcSearchBar.setText(srcItem.title);
                } else if (srcItem != null && destItem == null) {
                    destItem = new ListItem(String.format("%.4f, %.4f", latLng.latitude, latLng.longitude), "", 0, MainActivity.SearchTag.RESULT, latLng);
                    destSearchBar.setText(destItem.title);
                    createDirections(destItem);
                } else {
                    srcItem = new ListItem(String.format("%.4f, %.4f", latLng.latitude, latLng.longitude), "", 0, MainActivity.SearchTag.RESULT, latLng);
                    srcSearchBar.setText(srcItem.title);
                    createDirections(srcItem);
                }
            });
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

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            locationPermissionGranted = false;
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_directions, container, false);

        new Thread(() -> {

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
            directionsRecycler.setHasFixedSize(true);
            textDirections = new ArrayList<>();
            directionsAdapter = new DirectionsAdapter(getActivity(), textDirections);
            requireActivity().runOnUiThread(() -> directionsRecycler.setAdapter(directionsAdapter));

            // 2. Initialize SearchBars
            srcSearchBar = mView.findViewById(R.id.src_search_bar);
            destSearchBar = mView.findViewById(R.id.dest_search_bar);

            // 4. Create the views for the SearchBars
            requireActivity().runOnUiThread(() -> {
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
            });
            srcItem = null;
            destItem = null;

            // 5. Set the SearchView Settings
            // reuse materialSearchView settings

            // 6. Initialize the BottomSheet
            sheet = mView.findViewById(R.id.directions_bottom_sheet);

            // 7. Get the BottomSheetBehavior
            bottomSheetBehavior = BottomSheetBehavior.from(sheet);
            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {

                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if (slideOffset < 0.05) {
                        directionsRecycler.setVisibility(View.INVISIBLE);
                    } else {
                        directionsRecycler.setVisibility(View.VISIBLE);
                    }
                }
            });

            // Initialize trip type icon for bottom bar
            tripTypeIcon = mView.findViewById(R.id.trip_type_image);

            // Initialize directions button
            directionsButton = mView.findViewById(R.id.directions_button);

            // 8. Set the settings of the BottomSheetBehavior
            requireActivity().runOnUiThread(() -> {
                bottomSheetBehavior.setSaveFlags(RightSheetBehavior.SAVE_ALL);
                bottomSheetBehavior.setHideable(false);
                bottomSheetBehavior.setPeekHeight(mView.findViewById(R.id.cl_directions).getMeasuredHeight() + convertDpToPx(110));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            });

            // Set the max height of the bottom sheet by putting it below the searchbar
            DisplayMetrics displayMetrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;

            // 9. Initialize Progress Indicator
            tripProgress = mView.findViewById(R.id.trip_progress);

            // 10. Initialize Main App Bar
            View view = requireActivity().findViewById(R.id.main_app_bar);
            if (view instanceof AppBarLayout) {
                ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
                if (viewTreeObserver.isAlive()) {
                    viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            bottomSheetBehavior.setMaxHeight(height - view.getHeight() * 2 - tripTypeGroup.getMeasuredHeight() + convertDpToPx(64));
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

            // Initialize the fab for swapping
            fabSwap = mView.findViewById(R.id.fab_swap);
            fabSwap.setOnClickListener(v -> swapDirections());

            // Initilize the trip type chip group
            tripTypeGroup = mView.findViewById(R.id.trip_type_group);
            tripTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (sheet.getVisibility() == View.VISIBLE) {
                    ((MainActivity) requireActivity()).whichSearchBar = DEST_SEARCH_BAR;
                    createDirections(destItem);
                }
            });

            // Initialize Accessibility chip
            adaChip = mView.findViewById(R.id.ada_chip);
            adaChip.setOnCheckedChangeListener((compoundButton, b) -> {
                if (srcItem != null && destItem != null) createDirections(destItem);
            });

            // Create Directions bottom sheet header items
            tripTime = mView.findViewById(R.id.eta_min);
            tripLength = mView.findViewById(R.id.trip_total_length);
            etaClockTime = mView.findViewById(R.id.eta_time);

        }).start();

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
        createDirections(destItem);
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

    // Return distance output that is readable and coherent
    protected String getDistanceText(double miles) {
        if (miles < 0.1) {
            return (int) (miles * 5280) + " feet";
        }
        return String.format("%.2f miles", miles);
    }

    // Return time output that is readable and consistent
    protected String getTimeText(double minutes) {
        if (minutes > 2) {
            return (int) minutes + " min";
        }
        return (int) (minutes * 60) + " sec";
    }

    protected String getETAText(double totalTime) {
        Calendar currentTime = Calendar.getInstance();
        currentTime.getTime();
        currentTime.add(Calendar.MINUTE, (int) totalTime);
        String finalTime = currentTime.get(Calendar.HOUR) + ":" + currentTime.get(Calendar.MINUTE);
        String afternoon = " PM";
        if (currentTime.get(Calendar.AM_PM) == Calendar.AM) {
            afternoon = " AM";
        }
        return "ETA " + finalTime + afternoon;
    }

    public void createDirections(ListItem itemTapped) {
        mMap.clear();
        if (itemTapped != null) {

            // Set source and destination items based on what is tapped
            int whichSearchBar = ((MainActivity) requireActivity()).whichSearchBar;
            if (whichSearchBar == MAIN_SEARCH_BAR && locationPermissionGranted) {
                LatLng currLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                srcSearchBar.setText("Current location");
                destSearchBar.setText(itemTapped.title);
                destItem = itemTapped;
                srcItem = new ListItem("Current Location", "", 0, MainActivity.SearchTag.RESULT, currLocation);
            } else if (whichSearchBar == SRC_SEARCH_BAR) {
                srcSearchBar.setText(itemTapped.title);
                srcItem = itemTapped;
            } else if (whichSearchBar == DEST_SEARCH_BAR) {
                destSearchBar.setText(itemTapped.title);
                destItem = itemTapped;
            }

            // If a source and destination exists, create directions
            if (destItem != null && srcItem != null) {

                // Show progress indicator
                tripProgress.setVisibility(View.VISIBLE);

                // Set bottom bar visibility to gone
                ((MainActivity) requireActivity()).bottomBar.setVisibility(View.GONE);

                // Get Trip Plan with specific trip type
                new Thread(() -> {
                    TripPlan newTripPlan;
                    int iconSrc;
                    Chip c = tripTypeGroup.findViewById(tripTypeGroup.getCheckedChipId());
                    switch (c.getText().toString()) {
                        case "Car": // Car
                            newTripPlan = getTripPlan(srcItem.position, destItem.position, TripType.DRIVE);
                            iconSrc = R.drawable.car;
                            break;
                        case "Bus": // Bus
                            newTripPlan = getTripPlan(srcItem.position, destItem.position, TripType.BUS);
                            iconSrc = R.drawable.bus;
                            break;
                        case "Bike": // Bike
                            newTripPlan = getTripPlan(srcItem.position, destItem.position, TripType.BIKE);
                            iconSrc = R.drawable.bike;
                            break;
                        case "Walk": // Walk
                            newTripPlan = getTripPlan(srcItem.position, destItem.position, TripType.WALK);
                            iconSrc = R.drawable.walk;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + tripTypeGroup.getCheckedChipId());
                    }

                    // Set bottom bar visibility to gone
                    requireActivity().runOnUiThread(() -> ((MainActivity) requireActivity()).bottomBar.setVisibility(View.GONE));
                    if (newTripPlan == null) {

                        Snackbar snackbar = Snackbar.make(requireActivity().findViewById(R.id.cl_main), "Invalid Route", Snackbar.LENGTH_LONG);
                        snackbar.setAction("OK", view -> {
                            snackbar.dismiss();
                        });
                        snackbar.setBackgroundTint(ContextCompat.getColor(requireActivity(), R.color.foreground));
                        snackbar.setActionTextColor(ContextCompat.getColor(requireActivity(), R.color.background));
                        snackbar.show();

                        requireActivity().runOnUiThread(() -> {
                            tripProgress.setVisibility(View.INVISIBLE);
                            // Set bottom bar visibility to visible
                            ((MainActivity) requireActivity()).bottomBar.setVisibility(View.VISIBLE);
                            fabMyLocation.setVisibility(View.VISIBLE);
                            sheet.setVisibility(View.GONE);
                        });


                    } else if (newTripPlan.getFeatures().size() <= 3) {
                        Snackbar snackbar = Snackbar.make(requireActivity().findViewById(R.id.cl_main), "Source and destination points are too close together.", Snackbar.LENGTH_LONG);
                        snackbar.setAction("OK", view -> {
                            snackbar.dismiss();
                        });
                        snackbar.setBackgroundTint(ContextCompat.getColor(requireActivity(), R.color.foreground));
                        snackbar.setActionTextColor(ContextCompat.getColor(requireActivity(), R.color.background));
                        snackbar.show();
                        requireActivity().runOnUiThread(() -> {
                            tripProgress.setVisibility(View.INVISIBLE);
                            // Set bottom bar visibility to visible
                            ((MainActivity) requireActivity()).bottomBar.setVisibility(View.VISIBLE);
                            fabMyLocation.setVisibility(View.VISIBLE);
                            sheet.setVisibility(View.GONE);
                            exitDirections();
                        });
                    } else {
                        ArrayList<ListItem> tempDirections = new ArrayList<>();
                        ArrayList<Feature> routeFeatures = newTripPlan.getFeatures();
                        routeFeatures.get(0).setText("Start at " + srcItem.title);  // Fix first text
                        routeFeatures.get(routeFeatures.size() - 1).setText(  // Fix second text
                                routeFeatures.get(routeFeatures.size() - 1).getText()
                                        .replaceFirst("Location 2", destItem.title));
                        for (int i = 0; i < routeFeatures.size(); i++) {
                            Feature currFeature = routeFeatures.get(i);
                            // If there's a landmark
                            if (currFeature.getType() == FeatureType.LANDMARK) {
                                tempDirections.add(new ListItem(currFeature.getText(), "", 0, MainActivity.SearchTag.CATEGORY));

                            } else {
                                // Convert feature length and time to readable formatted String
                                String distText = getDistanceText(currFeature.getLengthMiles());
                                String timeText = getTimeText(currFeature.getTimeMins());

                                // Add list item
                                if (i != 0 && i != routeFeatures.size() - 1) {
                                    tempDirections.add(new ListItem(currFeature.getText(),
                                            distText + " (" + timeText + ")",
                                            0, currFeature.getManeuverType(),
                                            MainActivity.SearchTag.RESULT, null));
                                } else {
                                    tempDirections.add(new ListItem(currFeature.getText(),
                                            null,
                                            0, currFeature.getManeuverType(),
                                            MainActivity.SearchTag.RESULT, null));
                                }
                            }
                        }

                        int size = textDirections.size();
                        textDirections.clear();
                        if (size > 0)
                            requireActivity().runOnUiThread(() -> directionsAdapter.notifyItemRangeRemoved(0, size));
                        textDirections.addAll(tempDirections);

                        // Set drawable for icon
                        Drawable iconFilled = ContextCompat.getDrawable(requireActivity(), iconSrc);
                        iconFilled.setTint(ContextCompat.getColor(requireActivity(), R.color.white));

                        // Directions button
                        directionsButton.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));

                        // Parse the trip plan into the Bottom Sheet
                        requireActivity().runOnUiThread(() -> {

                            // Add text directions to the adapter
                            directionsAdapter.notifyItemRangeInserted(0, textDirections.size());
                            directionsRecycler.setAdapter(directionsAdapter);

                            // Set bottom sheet header items
                            tripTime.setText(getTimeText(newTripPlan.getTotalTime()));
                            tripLength.setText(getDistanceText(newTripPlan.getTotalLength()));
                            etaClockTime.setText(getETAText(newTripPlan.getTotalTime()));
                            tripTypeIcon.setImageDrawable(iconFilled);

                            // Change the visibility of the BottomSheet to "visible"
                            sheet.setVisibility(View.VISIBLE);
                            fabMyLocation.setVisibility(View.GONE);
                            tripProgress.setVisibility(View.INVISIBLE);
                        });
                    }
                }).start();

            }
        }
    }

    public void exitDirections() {

        // Clear the map
        mMap.clear();

        // Change the visibility of the BottomBar to "gone"
        sheet.setVisibility(View.GONE);
        fabMyLocation.setVisibility(View.VISIBLE);
        tripProgress.setVisibility(View.GONE);

        // Reset the hints
        srcSearchBar.setText("");
        destSearchBar.setText("");
        srcSearchBar.setHint("Choose starting point");
        destSearchBar.setHint("Choose destination");
        srcItem = null;
        destItem = null;

    }
}