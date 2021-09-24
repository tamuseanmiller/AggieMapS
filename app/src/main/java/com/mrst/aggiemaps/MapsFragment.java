package com.mrst.aggiemaps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.esri.core.geometry.Point;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.rubensousa.decorator.ColumnProvider;
import com.rubensousa.decorator.GridMarginDecoration;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsFragment extends Fragment implements OnCampusAdapter.ItemClickListener, FavAdapter.ItemClickListener, OffCampusAdapter.ItemClickListener, GameDayAdapter.ItemClickListener {

    private OkHttpClient client;  // Client to make API requests
    private GoogleMap mMap;       // The Map itself
    public static String routeNo;
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

            return body.string(); // Return the response as a string

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Method to draw a bus route on the map
     */
    private void drawBusRoute(int color) {
        try {
            Request request = new Request.Builder()
                    .url("https://transport.tamu.edu/BusRoutesFeed/api/route/" + routeNo + "/pattern")
                    .build();

            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();

            String str = body.string();
            JSONArray stops = new JSONArray(str);
            PolylineOptions polylineOptions = new PolylineOptions();
            LatLng first = null;

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
                    CircleOptions circleOptions = new CircleOptions();
                    circleOptions.center(new LatLng(x, y));
                    circleOptions.radius(55);
                    circleOptions.fillColor(color);
                    circleOptions.strokeWidth(0);
                    requireActivity().runOnUiThread(() -> mMap.addCircle(circleOptions));
                }

            }

            polylineOptions.add(first);
            polylineOptions.color(color);
            polylineOptions.width(10);
            polylineOptions.geodesic(true);
            polylineOptions.pattern(null);
            requireActivity().runOnUiThread(() -> mMap.addPolyline(polylineOptions));
        } catch (JSONException | IOException jsonException) {
            jsonException.printStackTrace();
        }
    }

    /*
     * Method to draw all buses on a given route
     */
    private void drawBusesOnRoute() {

    }

    /*
     * Method to create array of a route from two latlng coordinates
     * returns a TripPlan obj
     */
    private TripPlan getRoute(LatLng src, LatLng dest) {

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
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.sin_city));

            // Draw Initial Bus Route
            routeNo = "27";
            new Thread(() -> drawBusRoute(ContextCompat.getColor(requireActivity(), R.color.light_blue_300))).start();
        }
    };

    /*
     * When the view is created, what happens
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate View
        View mView = inflater.inflate(R.layout.fragment_maps, container, false);
        client = new OkHttpClient(); // Create OkHttpClient to be used in API requests
        favoritesText = mView.findViewById(R.id.favorites_text); // Initialize favorites text

        // Set up recyclers
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
        col = () -> 2;
        onCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
        onCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        offCampusRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
        offCampusRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));
        gameDayRoutes.setLayoutManager(new GridLayoutManager(getActivity(), 2, GridLayoutManager.HORIZONTAL, false));
        gameDayRoutes.addItemDecoration(new GridMarginDecoration(0, 0, col, GridLayoutManager.HORIZONTAL, false, null));

        // Initialize bus button
        MaterialCardView busButton = mView.findViewById(R.id.bus_button);

        // Set up the bottom sheet
        View standardBottomSheet = mView.findViewById(R.id.standard_bottom_sheet);
        standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet);
        standardBottomSheetBehavior.setSaveFlags(BottomSheetBehavior.SAVE_ALL);
        standardBottomSheetBehavior.setHideable(false);
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        standardBottomSheetBehavior.setPeekHeight(0);

        // Then set up the bus routes on the bottom sheet
        new Thread(this::setUpBusRoutes).start();

        // Set the Button to open the routes sheet
        busButton.setOnClickListener(v -> {
            standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        return mView;
    }

    private void setUpBusRoutes() {
        try {
            String str = getApiCall("https://transport.tamu.edu/BusRoutesFeed/api/Routes");
            JSONArray routes = new JSONArray(str);

            // Initialize lists and add the `ALL` Route
            List<BusRoute> favList = new ArrayList<>();
            List<BusRoute> onList = new ArrayList<>();
            List<BusRoute> offList = new ArrayList<>();
            List<BusRoute> gameDayList = new ArrayList<>();
            favList.add(new BusRoute("All", "Favorites", ContextCompat.getColor(getActivity(), R.color.all_color)));
            onList.add(new BusRoute("All", "On Campus", ContextCompat.getColor(getActivity(), R.color.all_color)));
            offList.add(new BusRoute("All", "Off Campus", ContextCompat.getColor(getActivity(), R.color.all_color)));
            gameDayList.add(new BusRoute("All", "Game Day", ContextCompat.getColor(getActivity(), R.color.all_color)));

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
                    default: // Favorites
                        favList.add(new BusRoute(routes.getJSONObject(i).getString("ShortName"), routes.getJSONObject(i).getString("Name"), color));
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

    @Override
    public void onItemClick(View view, int position) {
        // When a route is clicked, check which view it comes from and draw that route
        if (view == onCampusRoutes) {
            routeNo = onCampusAdapter.getItem(position).routeNumber;
        } else if (view == offCampusRoutes) {
            routeNo = offCampusAdapter.getItem(position).routeNumber;
        } else if (view == favRoutes) {
            routeNo = favAdapter.getItem(position).routeNumber;
        } else if (view == gameDayRoutes) {
            routeNo = gameDayAdapter.getItem(position).routeNumber;
        }
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mMap.clear();
        new Thread(() -> drawBusRoute(gameDayAdapter.getItem(position).color)).start();
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