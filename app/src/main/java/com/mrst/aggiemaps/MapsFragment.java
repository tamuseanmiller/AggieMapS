package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esri.core.geometry.Point;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
    private RightSheetBehavior<View> rightSheetBehavior;
    private TableLayout tlTimetable;
    private String currentRouteNo;
    private FloatingActionButton fabTimetable;
    public static List<BusRoute> busRoutes;
    public static GoogleMap mMap;       // The Map itself
    private Handler handler = new Handler();
    private Runnable runnable;
    private ArrayList<Marker> busMarkers = new ArrayList<>();
    private FloatingActionButton fabMyLocation;
    private LinearProgressIndicator dateProgress;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private NestedScrollView nsv;

    @Override
    public void onItemClick(View view, int position) {
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        if (rightSheetBehavior.getState() != RightSheetBehavior.STATE_COLLAPSED)
            rightSheetBehavior.setState(RightSheetBehavior.STATE_COLLAPSED);
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
            for (int i = 0; i < busData_jsonArray.length(); i++) {
                busMarkers.add(null);
                // Retrieving Data
                JSONObject currentBus = busData_jsonArray.getJSONObject(i);
                Point p = convertWebMercatorToLatLng(currentBus.getDouble("lng"),
                        currentBus.getDouble("lat"));
                float busDirection = (float) currentBus.getDouble("direction") - 90;
                String occupancy = currentBus.getString("occupancy");
                int finalI = i;
                if (!isAdded()) return;
                // Initialize Markers
                if (busMarkers.get(i) == null) {
                    MarkerOptions marker = new MarkerOptions();
                    marker.flat(true);
                    marker.icon(BitmapFromVector(getActivity(), R.drawable.bus_side,
                            ContextCompat.getColor(requireActivity(), R.color.foreground), 0));
                    marker.zIndex(100);
                    marker.anchor(0.5F, 0.8F);
                    marker.position(new LatLng(p.getY(), p.getX()));
                    marker.rotation(busDirection);
                    marker.title("Occupancy: " + occupancy);
                    requireActivity().runOnUiThread(() -> {
                        busMarkers.set(finalI, mMap.addMarker(marker));
                    });
                }
                // Update the existing Markers
                else {
                    requireActivity().runOnUiThread(() -> {
                        busMarkers.get(finalI).setPosition(new LatLng(p.getY(), p.getX()));
                        busMarkers.get(finalI).setRotation(busDirection);
                        busMarkers.get(finalI).setTitle("Occupancy: " + occupancy);
                    });
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
     * Method to create array of a route from two latlng coordinates
     * returns a TripPlan obj
     */
    private TripPlan getTripPlan(LatLng src, LatLng dest, TripType type) {

        return null;
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
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

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), 14.0f));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            LatLng collegeStation = new LatLng(30.611812, -96.329767);
                            mMap.animateCamera(CameraUpdateFactory
                                    .newLatLngZoom(collegeStation, 14.0f));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
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
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.light));

            // Turn on the My Location layer and the related control on the map.
            updateLocationUI();

            // Get the current location of the device and set the position of the map.
            getDeviceLocation();
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

        nsv = mView.findViewById(R.id.nsv);
        nsv.setNestedScrollingEnabled(false);
        nsv.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (view, i, i1, i2, i3) -> {
            if (i1 == 0) {
                // NestedScrollView reached top
                if (!standardBottomSheetBehavior.isDraggable()) {
                    // now draggable
                    standardBottomSheetBehavior.setDraggable(true);
                }
            } else {
                if (standardBottomSheetBehavior.isDraggable()) {
                    standardBottomSheetBehavior.setDraggable(false);
                }
            }
        });

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        client = new OkHttpClient.Builder()  // Create OkHttpClient to be used in API requests
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

        // Set up the bus swiping action
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
                if (rightSheetBehavior.getState() != RightSheetBehavior.STATE_COLLAPSED)
                    rightSheetBehavior.setState(RightSheetBehavior.STATE_COLLAPSED);
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
        // Vary the number of rows based on screen height
        if (metrics.heightPixels < convertDpToPx((215 * 1) + 20 + 15 + 325)) {
            col = () -> 1;
            onCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            offCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            gameDayRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            gameDayRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        } else if (metrics.heightPixels < convertDpToPx((215 * 2) + 20 + 15 + 325)) {
            onCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            col = () -> 1;
            offCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            gameDayRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 1, GridLayoutManager.HORIZONTAL, false));
            gameDayRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        } else if (metrics.heightPixels < convertDpToPx((215 * 3) + 20 + 15 + 325)) {
            onCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            offCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
            offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
            col = () -> 3;
            gameDayRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 3, GridLayoutManager.HORIZONTAL, false));
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
        FrameLayout standardBottomSheet = mView.findViewById(R.id.standard_bottom_sheet);
        standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet);
        standardBottomSheetBehavior.setSaveFlags(BottomSheetBehavior.SAVE_ALL);
        standardBottomSheetBehavior.setHideable(false);
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        standardBottomSheetBehavior.setPeekHeight(0);
        standardBottomSheetBehavior.setHalfExpandedRatio(0.49f);
        standardBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;

                    case BottomSheetBehavior.STATE_EXPANDED:
                        nsv.setNestedScrollingEnabled(true);
//                        standardBottomSheetBehavior.setDraggable(false);
                        nsv.scrollTo(0, 1);
//                        nsv.scrollTo(0, 0);
                    break;

                    case BottomSheetBehavior.STATE_COLLAPSED:
                        standardBottomSheetBehavior.setDraggable(true);
                    break;

                    case BottomSheetBehavior.STATE_DRAGGING:
                        nsv.setNestedScrollingEnabled(false);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        // Set up right sheet for timetable
        View sheet = mView.findViewById(R.id.timetable_sheet);
        rightSheetBehavior = RightSheetBehavior.from(sheet);
        rightSheetBehavior.setSaveFlags(RightSheetBehavior.SAVE_ALL);
        rightSheetBehavior.setHideable(false);
        rightSheetBehavior.setPeekWidth(0);
        rightSheetBehavior.setState(RightSheetBehavior.STATE_COLLAPSED);
        tlTimetable = mView.findViewById(R.id.tl_timetable);

        // Initialize the fab to open the timetable
        fabTimetable = mView.findViewById(R.id.fab_timetable);
        fabTimetable.setVisibility(View.GONE);
        fabTimetable.setOnClickListener(v -> {
            if (rightSheetBehavior.getState() == RightSheetBehavior.STATE_COLLAPSED) {
                rightSheetBehavior.setState(RightSheetBehavior.STATE_EXPANDED);
            } else {
                rightSheetBehavior.setState(RightSheetBehavior.STATE_COLLAPSED);
            }
        });

        // Initialize the my current location FAB
        fabMyLocation = mView.findViewById(R.id.fab_mylocation);
        fabMyLocation.setVisibility(View.GONE);
        fabMyLocation.setOnClickListener(v -> {
            getDeviceLocation();
        });

        // Initialize the Date Picker for the TimeTable
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            String header = datePicker.getHeaderText();
            LocalDate ld;
            if (header.length() == 11) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
                ld = LocalDate.parse(datePicker.getHeaderText(), dateFormatter);
            } else {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                ld = LocalDate.parse(datePicker.getHeaderText(), dateFormatter);
            }
            tlTimetable.removeAllViews();
            new Thread(() -> setUpTimeTable(ld.toString())).start();

        });

        // Initialize Try Another Date Button
        MaterialButton tryAnotherDate = mView.findViewById(R.id.try_another_date_button);
        tryAnotherDate.setOnClickListener(view -> datePicker.show(requireActivity().getSupportFragmentManager(), "tag"));

        // Initialize the Progress Bar for after a user picks a date
        dateProgress = mView.findViewById(R.id.date_progress);
        dateProgress.setVisibility(View.INVISIBLE);

        // Then set up the bus routes on the bottom sheet
        new Thread(this::setUpBusRoutes).start();

        return mView;
    }

    /*
     * Method to set the values within the table layout for the timetable
     */
    private void setUpTimeTable(String viewMoreTime) {
        try {
            requireActivity().runOnUiThread(() -> dateProgress.setVisibility(View.VISIBLE));
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
                });
                return;
            }

            // Add the stops as a header row to the table layout
            TableRow headerRow = new TableRow(getActivity());
            for (int i = 0; i < timetableArray.getJSONObject(0).names().length(); i++) {
                String header = timetableArray.getJSONObject(0).names().getString(i).substring(36);
                headerRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                TextView headerTV = new TextView(getActivity());
                headerTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
                headerTV.setPadding(0, 10, 40, 10);
                Typeface face = ResourcesCompat.getFont(requireActivity(), R.font.roboto_bold);
                headerTV.setTypeface(face);
                headerTV.setText(header);
                requireActivity().runOnUiThread(() -> {
                    headerRow.addView(headerTV);
                });
            }
            requireActivity().runOnUiThread(() -> tlTimetable.addView(headerRow));

            // Loop through every row
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
                if (!input.isBefore(LocalTime.now()) && numRows++ <= 4) {
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
                        if (!value.equals("null") && LocalTime.parse(value, formatter).isBefore(LocalTime.now())) {
                            time.setPaintFlags(time.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                            time.setTextColor(ContextCompat.getColor(requireActivity(), R.color.accent));
                        }

                        // If the value is null, just leave it empty
                        if (value.equals("null")) value = "";
                        time.setText(value);
                        requireActivity().runOnUiThread(() -> tr.addView(time));
                    }
                    requireActivity().runOnUiThread(() -> tlTimetable.addView(tr));
                }
                if (numRows == 0) {
                    requireActivity().runOnUiThread(() -> fabTimetable.setVisibility(View.GONE));
                } else {
                    requireActivity().runOnUiThread(() -> fabTimetable.setVisibility(View.VISIBLE));
                }
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
     * Method to create and display all bus routes on the bottom sheet
     */
    private void setUpBusRoutes() {
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

        busRoutes.addAll(favList);
        busRoutes.addAll(onList);
        busRoutes.addAll(offList);
        busRoutes.addAll(gameDayList);
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
            fabTimetable.setVisibility(View.GONE);
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
            currentRouteNo = busRoute.routeNumber;

            // Set the values for the timetable right sheet
            tlTimetable.removeAllViews();
            new Thread(() -> setUpTimeTable("")).start();

            // Draw the route
            new Thread(() -> drawBusRoute(busRoute.routeNumber, busRoute.color)).start();

            // Continuously draw the buses on the route
            handler.post(runnable = () -> {
                handler.postDelayed(runnable, 3000);
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
