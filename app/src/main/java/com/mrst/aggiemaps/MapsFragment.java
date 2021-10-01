package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.telephony.IccOpenLogicalChannelResponse;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.esri.core.geometry.Point;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executor;

public class MapsFragment extends Fragment {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 44;
    private OkHttpClient client;  // Client to make API requests
    private GoogleMap mMap;       // The Map itself
    private Handler handler = new Handler();
    private Runnable runnable;
    private ArrayList<Marker> busMarkers = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean  locationPermissionGranted;
    private Location lastKnownLocation;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

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
     * Helper method to convert a drawable to a BitmapDescriptor for use with a maps marker
     * Taken from somewhere similar to here
     * https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon
     */
    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId, int color) {
        // below line is use to generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        // below line is use to set bounds to our vector drawable.
        assert vectorDrawable != null;
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        vectorDrawable.setTintList(ColorStateList.valueOf(color));

        // below line is use to create a bitmap for our
        // drawable which we have added.

        if (vectorDrawable != null){
            Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            // below line is use to add bitmap in our canvas.
            Canvas canvas = new Canvas(bitmap);

            // below line is use to draw our
            // vector drawable in canvas.
            vectorDrawable.draw(canvas);

            // after generating our bitmap we are returning our bitmap.
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        }
        return null;
    }


    /*
     * Method to draw a bus route on the map
     */
    private void drawBusRoute(String routeNo) {

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
                Float busDirection = (float) currentBus.getDouble("direction") - 90;
                String occupancy = currentBus.getString("occupancy");
                int finalI = i;
                requireActivity().runOnUiThread(() -> {
                    // Initialize Markers
                    if (busMarkers.get(finalI) == null) {
                        MarkerOptions marker = new MarkerOptions();
                        marker.flat(true);
                        marker.icon(BitmapFromVector(getActivity(), R.drawable.bus_side,
                                ContextCompat.getColor(requireActivity(), R.color.white)));
                        marker.zIndex(100);
                        marker.anchor(0.5F, 0.8F);
                        marker.position(new LatLng(p.getY(), p.getX()));
                        marker.rotation(busDirection);
                        marker.title("Occupancy: "+occupancy);
                        busMarkers.set(finalI, mMap.addMarker(marker));
                    }
                    // Update the existing Markers
                    else {
                        busMarkers.get(finalI).setPosition(new LatLng(p.getY(), p.getX()));
                        busMarkers.get(finalI).setRotation(busDirection);
                        busMarkers.get(finalI).setTitle("Occupancy: "+occupancy);
                    }
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
     * Method to create array of a route from two latlng coordinates
     * returns a TripPlan obj
     */
    private TripPlan getRoute(LatLng src, LatLng dest) {

        return null;
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            Log.e("TEST", "Request Permission");
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.e("TEST", "In Req Permission");
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                Log.e("Req Code", String.valueOf(requestCode));

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                    Log.e("locationPermission", String.valueOf(locationPermissionGranted));
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
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
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
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), 14.0f));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            LatLng collegeStation = new LatLng(30.611812, -96.329767);
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(collegeStation, 14.0f));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
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
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.sin_city));

            // Turn on the My Location layer and the related control on the map.
            getLocationPermission();
            updateLocationUI();

            // Get the current location of the device and set the position of the map.
            getDeviceLocation();

            // Call drawBusesOnRoute repeatedly
            handler.postDelayed(runnable = () -> {
                handler.postDelayed(runnable, 3000);
                // Calling the drawBusesOnRoute in a new thread
                Thread t = new Thread(() -> {
                    drawBusesOnRoute("04");
                });
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, 3000);
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

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.getContext());

        return mView;
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
