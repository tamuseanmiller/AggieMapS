package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.lapism.search.widget.MaterialSearchBar;
import com.lapism.search.widget.MaterialSearchView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity implements GISSearchAdapter.ItemClickListener, GoogleSearchAdapter.ItemClickListener {

    private MaterialSearchBar materialSearchBar;
    private MaterialSearchView materialSearchView;
    private OkHttpClient client;  // Client to make API requests
    private GISSearchAdapter gisSearchAdapter;
    private GoogleSearchAdapter googleSearchAdapter;
    private ArrayList<SearchResult> gisSearchResults;
    private ArrayList<SearchResult> googleSearchResults;
    private PlacesClient placesClient;
    private RecyclerView gisSearchRecycler;
    private RecyclerView googleSearchRecycler;

    enum SearchTag {
        CATEGORY,
        RESULT
    }

    private void clearFocusOnSearch() {
        materialSearchView.clearFocus();
        materialSearchView.setVisibility(View.GONE);
        materialSearchBar.setVisibility(View.VISIBLE);
        showSystemUI();
    }

    private void requestFocusOnSearch() {
        materialSearchView.setVisibility(View.VISIBLE);
        materialSearchView.requestFocus();
        materialSearchBar.setVisibility(View.GONE);
        hideSystemUI();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        client = new OkHttpClient(); // Create OkHttpClient to be used in API request

        // Set the status bar to be transparent
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Initialize Places
        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.maps_api_key));

        // Create a new PlacesClient instance
        placesClient = Places.createClient(this);

        // Initialize the SearchBar and View
        materialSearchBar = findViewById(R.id.material_search_bar);
        materialSearchView = findViewById(R.id.material_search_view);

        // Set the toolbar and actionbar
        Toolbar toolbar = materialSearchBar.getToolbar();
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        Drawable nav = ContextCompat.getDrawable(this, R.drawable.magnify);
        if (nav != null && actionBar != null) {
            nav.setTint(getColor(R.color.foreground));
            actionBar.setIcon(nav);
        }

        // Set Search Bar Settings
        materialSearchBar.setHint("Aggie MapS");
        materialSearchBar.setElevation(5);
        materialSearchBar.setBackgroundColor(getColor(R.color.background));
        materialSearchBar.setOnClickListener(v -> requestFocusOnSearch());
        materialSearchBar.setNavigationOnClickListener(v -> requestFocusOnSearch());

        // Set recyclers
        gisSearchResults = new ArrayList<>();
        googleSearchResults = new ArrayList<>();
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        gisSearchAdapter = new GISSearchAdapter(this, gisSearchResults);
        gisSearchAdapter.setClickListener(this);
        googleSearchAdapter = new GoogleSearchAdapter(this, googleSearchResults);
        googleSearchAdapter.setClickListener(this);
        gisSearchRecycler = new RecyclerView(this);
        gisSearchRecycler.setLayoutManager(new LinearLayoutManager(this));
        gisSearchRecycler.setAdapter(gisSearchAdapter);
        googleSearchRecycler = new RecyclerView(this);
        googleSearchRecycler.setLayoutManager(new LinearLayoutManager(this));
        googleSearchRecycler.setAdapter(googleSearchAdapter);
        ll.addView(gisSearchRecycler);
        ll.addView(googleSearchRecycler);

        // Set SearchView Settings
        materialSearchView.addView(ll);
        Drawable navigationIcon = ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24);
        navigationIcon.setTintList(ColorStateList.valueOf(getColor(R.color.foreground)));
        materialSearchView.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24));
        materialSearchView.setVisibility(View.GONE);
        materialSearchView.setHint("Try Building Numbers/Names");
        materialSearchView.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling fetchPlace()).
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        // Set OnClick Listeners
        materialSearchView.setNavigationOnClickListener(v -> clearFocusOnSearch());

        materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(@NonNull CharSequence charSequence) {
                if (charSequence.length() == 0) return true;

                // Delays the query call so that the recyclers can keep up
                // This is just a fix for fast typing
                gisSearchRecycler.post(() -> {
                    queryGIS(charSequence); // Query GIS, Google
                    queryGoogle(charSequence, token);
                });
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(@NonNull CharSequence charSequence) {
                if (charSequence.length() == 0) return true;

                // Delays the query call so that the recyclers can keep up
                // This is just a fix for fast typing
                gisSearchRecycler.post(() -> {
                    queryGIS(charSequence); // Query GIS, Google
                    queryGoogle(charSequence, token);
                });
                return true;
            }
        });

        materialSearchView.setOnFocusChangeListener(v -> {

        });
    }

    /*
     * Queries Google's Autocomplete Places API to search with GIS
     */
    private void queryGoogle(CharSequence charSequence, AutocompleteSessionToken token) {
        // Create a RectangularBounds object.
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(30.56329, -96.44175),
                new LatLng(30.66670, -96.26854));

        // Use the builder to create a FindAutocompletePredictionsRequest.
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationRestriction(bounds)
                .setSessionToken(token)
                .setQuery(charSequence.toString())
                .setCountries("US")
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            if (!response.getAutocompletePredictions().isEmpty()) {
                int size = googleSearchResults.size();
                googleSearchResults.clear();
                googleSearchAdapter.notifyItemRangeRemoved(0, size);
                googleSearchResults.add(new SearchResult("Google Maps", "", 0, null, SearchTag.CATEGORY, null));
                googleSearchAdapter.notifyItemInserted(googleSearchResults.size() - 1);
            }
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                Log.i(TAG, prediction.getPlaceId());
                Log.i(TAG, prediction.getPrimaryText(null).toString());

                // Specify the fields to return.
                final List<Place.Field> placeFields = Collections.singletonList(Place.Field.LAT_LNG);

                // Construct a request object, passing the place ID and fields array.
                final FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(prediction.getPlaceId(), placeFields);

                placesClient.fetchPlace(placeRequest).addOnSuccessListener((placeResponse) -> {
                    googleSearchResults.add(new SearchResult(prediction.getPrimaryText(null).toString(), prediction.getFullText(null).toString(), 0, null, SearchTag.RESULT, placeResponse.getPlace().getLatLng()));
                    googleSearchAdapter.notifyItemInserted(googleSearchResults.size() - 1);
                });
            }

        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e(TAG, "Place not found: " + apiException.getStatusCode());
            }
        });
    }

    /*
     * First query GIS, then query google places
     */
    private void queryGIS(CharSequence charSequence) {
        new Thread(() -> {
            String resp = getApiCall("https://gis.tamu.edu/arcgis/rest/services/FCOR/" +
                    "TAMU_BaseMap/MapServer/1/query?where=UPPER%28Abbrev%29+LIKE+UPPER%28" +
                    "%27%25" + charSequence.toString() + "%25%27%29+OR+UPPER%28BldgName%29+" +
                    "LIKE+UPPER%28%27%25" + charSequence.toString() + "%25%27%29&text=&" +
                    "objectIds=&time=&geometry=&geometryType=esriGeometryEnvelope&inSR=&" +
                    "spatialRel=esriSpatialRelIntersects&relationParam=&outFields=*&" +
                    "returnGeometry=true&returnTrueCurves=false&maxAllowableOffset=&" +
                    "geometryPrecision=&outSR=4326&having=&returnIdsOnly=false&" +
                    "returnCountOnly=false&orderByFields=&groupByFieldsForStatistics=&" +
                    "outStatistics=&returnZ=false&returnM=false&gdbVersion=&historicMoment=&" +
                    "returnDistinctValues=false&resultOffset=&resultRecordCount=5&" +
                    "queryByDistance=&returnExtentOnly=false&datumTransformation=&" +
                    "parameterValues=&rangeValues=&quantizationParameters=&" +
                    "featureEncoding=esriDefault&f=pjson");
            try {
                if (resp != null) {
                    int size = gisSearchResults.size();
                    gisSearchResults.clear();
                    JSONObject jsonObject = new JSONObject(resp);
                    JSONArray features = jsonObject.getJSONArray("features");
                    for (int i = 0; i < features.length(); i++) {
                        if (i == 0)
                            gisSearchResults.add(new SearchResult("On Campus", "", 0, null, SearchTag.CATEGORY, null));
                        String bldgName = features.getJSONObject(i).getJSONObject("attributes").getString("BldgName");
                        String address = features.getJSONObject(i).getJSONObject("attributes").getString("Address");
                        double lat = features.getJSONObject(i).getJSONObject("attributes").getDouble("Latitude");
                        double lng = features.getJSONObject(i).getJSONObject("attributes").getDouble("Longitude");
                        if (address.equals("null"))
                            address = lat + ", " + lng; // If no address, use lat/lng instead
                        String finalAddress = address;
                        gisSearchResults.add(new SearchResult(bldgName, finalAddress, 0, null, SearchTag.RESULT, new LatLng(lat, lng)));
                    }
                    runOnUiThread(() -> gisSearchAdapter.notifyDataSetChanged());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /*
     * When back is pressed, make sure that it clears the searchview focus
     * instead of closing the app
     */
    @Override
    public void onBackPressed() {
        if (materialSearchView.hasFocus()) {
            clearFocusOnSearch();
        }
    }

    /*
     * Un-Show the navigation bar and get out of full screen
     */
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    /*
     * Show the navigation bar and get out of full screen
     */
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    /*
     * When a GIS Search Result is tapped, create marker and animate to position
     */
    @Override
    public void onGISClick(View view, int position) {
        MarkerOptions selectedResult = new MarkerOptions();
        selectedResult.position(gisSearchAdapter.getItem(position).position);
        selectedResult.title(gisSearchAdapter.getItem(position).title);
        MapsFragment.mMap.addMarker(selectedResult);
        MapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gisSearchAdapter.getItem(position).position, 18.0f));
        clearFocusOnSearch();
    }

    /*
     * When a Google Search Result is tapped, create marker and animate to position
     */
    @Override
    public void onGoogleClick(View view, int position) {
        MarkerOptions selectedResult = new MarkerOptions();
        selectedResult.position(googleSearchAdapter.getItem(position).position);
        selectedResult.title(googleSearchAdapter.getItem(position).title);
        MapsFragment.mMap.addMarker(selectedResult);
        MapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(googleSearchAdapter.getItem(position).position, 18.0f));
        clearFocusOnSearch();
    }

}

