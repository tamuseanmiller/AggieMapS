package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
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

import me.ibrahimsn.lib.OnItemSelectedListener;
import me.ibrahimsn.lib.SmoothBottomBar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity implements GISSearchAdapter.ItemClickListener, GoogleSearchAdapter.ItemClickListener, BusRoutesSearchAdapter.ItemClickListener {

    private static final int SPEECH_REQUEST_CODE = 0;
    private MaterialSearchBar materialSearchBar;
    private MaterialSearchView materialSearchView;
    private OkHttpClient client;  // Client to make API requests
    private GISSearchAdapter gisSearchAdapter;
    private GoogleSearchAdapter googleSearchAdapter;
    private BusRoutesSearchAdapter busRoutesSearchAdapter;
    private ArrayList<SearchResult> gisSearchResults;
    private ArrayList<SearchResult> googleSearchResults;
    private PlacesClient placesClient;
    private RecyclerView gisSearchRecycler;
    private RecyclerView googleSearchRecycler;
    private RecyclerView busRoutesSearchRecycler;
    private ArrayList<SearchResult> busRoutesSearchResults;
    final MapsFragment mapsFragment1 = new MapsFragment();
    final MapsFragment mapsFragment2 = new MapsFragment();
    final BlankFragment blankFragment = new BlankFragment();
    final Fragment[] active = {mapsFragment1};

    enum SearchTag {
        CATEGORY,
        RESULT
    }

    // Checks if there is an internet connection. If not, it keeps checking for internet until it is connected.
    int networkCheckCount = 1;
    private void haveNetworkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null) {
            if (networkCheckCount > 1) {
                Toast.makeText(getApplicationContext(),"Connected!", Toast.LENGTH_LONG).show();
            }
        }
       else {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.cl_main),"Your network is unavailable. Check your data or wifi connection.",Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("RETRY", view -> {
                haveNetworkConnection();
                networkCheckCount += 1;
            });
            snackbar.show();
       }
    }

    private void clearFocusOnSearch() {
        materialSearchView.clearFocus();
        materialSearchView.setVisibility(View.GONE);
        materialSearchBar.setVisibility(View.VISIBLE);
        showSystemUI();
    }

    private void requestFocusOnSearch() {
        materialSearchView.requestFocus();
        materialSearchView.setVisibility(View.VISIBLE);
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
        client = new OkHttpClient();  // Create OkHttpClient to be used in API request
        haveNetworkConnection();
        // Set current theme
        SharedPreferences sharedPref = getSharedPreferences("com.mrst.aggiemaps.preferences", Context.MODE_PRIVATE);
        String theme = sharedPref.getString("theme", "system_theme");
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system_theme":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

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
        busRoutesSearchResults = new ArrayList<>();
        NestedScrollView nSV = new NestedScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        gisSearchAdapter = new GISSearchAdapter(this, gisSearchResults);
        gisSearchAdapter.setClickListener(this);
        googleSearchAdapter = new GoogleSearchAdapter(this, googleSearchResults);
        googleSearchAdapter.setClickListener(this);
        busRoutesSearchAdapter = new BusRoutesSearchAdapter(this, busRoutesSearchResults);
        busRoutesSearchAdapter.setClickListener(this);
        gisSearchRecycler = new RecyclerView(this);
        gisSearchRecycler.suppressLayout(true);
        gisSearchRecycler.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        gisSearchRecycler.setAdapter(gisSearchAdapter);
        googleSearchRecycler = new RecyclerView(this);
        googleSearchRecycler.suppressLayout(true);
        googleSearchRecycler.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        googleSearchRecycler.setAdapter(googleSearchAdapter);
        busRoutesSearchRecycler = new RecyclerView(this);
        busRoutesSearchRecycler.suppressLayout(true);
        busRoutesSearchRecycler.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        busRoutesSearchRecycler.setAdapter(busRoutesSearchAdapter);
        ll.addView(gisSearchRecycler);
        ll.addView(busRoutesSearchRecycler);
        ll.addView(googleSearchRecycler);
        nSV.addView(ll);

        // Set SearchView Settings
        materialSearchView.addView(nSV);
        Drawable navigationIcon = ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24);
        navigationIcon.setTintList(ColorStateList.valueOf(getColor(R.color.foreground)));
        materialSearchView.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24));
        materialSearchView.setVisibility(View.GONE);
        materialSearchView.setHint("Try Building Numbers/Names");
        materialSearchView.setBackgroundColor(ContextCompat.getColor(this, R.color.background));
        Drawable clearIcon = ContextCompat.getDrawable(this, R.drawable.close);
        clearIcon.setTintList(ColorStateList.valueOf(getColor(R.color.foreground)));
        materialSearchView.setClearIcon(clearIcon);
        materialSearchView.setDividerColor(ContextCompat.getColor(this, android.R.color.transparent));
        materialSearchView.setTextClearOnBackPressed(true);

        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling fetchPlace()).
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        // Set OnClick Listeners
        materialSearchView.setNavigationOnClickListener(v -> clearFocusOnSearch());

        materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(@NonNull CharSequence charSequence) {
                if (charSequence.length() == 0) return true;
                queryGIS(charSequence); // Query GIS, Google
                queryBusRoutes(charSequence);
                queryGoogle(charSequence, token);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(@NonNull CharSequence charSequence) {
                if (charSequence.length() == 0) return true;
                queryGIS(charSequence); // Query GIS, Google, Bus Routes
                queryBusRoutes(charSequence);
                queryGoogle(charSequence, token);
                return true;
            }
        });

        // Set up BottomBar
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(R.id.ll_main, blankFragment, "0").hide(blankFragment).commit();
        fm.beginTransaction().add(R.id.ll_main, mapsFragment2, "1").hide(mapsFragment2).commit();
        fm.beginTransaction().add(R.id.ll_main, mapsFragment1, "2").commit();

        SmoothBottomBar bottomBar = findViewById(R.id.bottom_bar);
        bottomBar.setOnItemReselectedListener(i -> {
            if (i == 2) {
                mapsFragment1.standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            }
        });

        bottomBar.setOnItemSelectedListener((OnItemSelectedListener) i -> {
            if (i == 1) {
                fm.beginTransaction().hide(active[0]).show(mapsFragment2).commit();
                active[0] = mapsFragment2;
            } else if (i == 0) {
                fm.beginTransaction().hide(active[0]).show(blankFragment).commit();
                active[0] = blankFragment;
            } else if (i == 2) {
                fm.beginTransaction().hide(active[0]).show(mapsFragment1).commit();
                active[0] = mapsFragment1;
            }
            return false;
        });

        /*
         * TODO: Initialize UI for Directions
         *  Look above for help, most of this has been done once already above
         * 1. Create new ArrayList of SearchResults
         * 2. Initialize SearchBar and SearchView (You may be able to use the same view)
         * 3. Set toolbar and action bar
         * 4. Create the views for the SearchView
         * 5. Set the SearchView Settings
         * 6. Initialize the BottomSheet
         * 7. Get the BottomSheetBehavior
         * 8. Set the settings of the BottomSheetBehavior
         * 9. Initialize Progress Indicator
         * 10. Initialize Main App Bar
         * 11. Initialize Source and Dest Container
         */
        materialSearchView.setOnFocusChangeListener(v -> {

        });
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // This starts the activity and populates the intent with the speech text.
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            // Add the spoken text to the searchbar
            requestFocusOnSearch();
            materialSearchView.setTextQuery(spokenText, true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_menu, menu);
        menu.getItem(0).setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.foreground)));
        menu.getItem(1).setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.foreground)));
        return true;
    }

    /*
     * Handle menu actions
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_microphone:
                displaySpeechRecognizer();
                break;
            case R.id.action_settings:
                Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
                MainActivity.this.startActivity(myIntent);
                break;
        }
        if (id == R.id.action_microphone) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Queries List of Bus Routes
     */
    private void queryBusRoutes(CharSequence charSequence) {
        new Thread(() -> {

            // Initialize temporary array and add the category
            ArrayList<SearchResult> tempList = new ArrayList<>();
            tempList.add(new SearchResult("Bus Routes", "", 0, null, SearchTag.CATEGORY, null));

            // Loop through every bus route, check to see if the
            // name or number contains the given char sequence
            MapsFragment mapsFragment = (MapsFragment) mapsFragment1;
            if (mapsFragment != null) {
                for (int i = 1; i < mapsFragment.busRoutes.size(); i++) {
                    String routeNumber = mapsFragment.busRoutes.get(i).routeNumber.toLowerCase();
                    String routeName = mapsFragment.busRoutes.get(i).routeName.toLowerCase();
                    if ((routeNumber.contains(charSequence) || routeName.contains(charSequence)) && !routeNumber.equals("all"))
                        tempList.add(new SearchResult(mapsFragment.busRoutes.get(i).routeNumber, mapsFragment.busRoutes.get(i).routeName, 0, null, SearchTag.RESULT, null));
                }
            }

            // If no bus routes, clear it
            if (tempList.size() == 1) {
                tempList.clear();
            }

            // Set all values
            runOnUiThread(() -> {
                busRoutesSearchResults.clear();
                busRoutesSearchResults.addAll(tempList);
                busRoutesSearchAdapter = new BusRoutesSearchAdapter(this, busRoutesSearchResults);
                busRoutesSearchAdapter.setClickListener(this);
                busRoutesSearchRecycler.setAdapter(busRoutesSearchAdapter);
            });
        }).start();
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
            ArrayList<SearchResult> tempList = new ArrayList<>();
            if (!response.getAutocompletePredictions().isEmpty()) {
                tempList.add(new SearchResult("Google Maps", "", 0, null, SearchTag.CATEGORY, null));
            }
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                Log.i(TAG, prediction.getPlaceId());
                Log.i(TAG, prediction.getPrimaryText(null).toString());

                // Specify the fields to return.
                final List<Place.Field> placeFields = Collections.singletonList(Place.Field.LAT_LNG);

                // Construct a request object, passing the place ID and fields array.
                final FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(prediction.getPlaceId(), placeFields);

                placesClient.fetchPlace(placeRequest).addOnSuccessListener((placeResponse) -> {
                    tempList.add(new SearchResult(prediction.getPrimaryText(null).toString(), prediction.getFullText(null).toString(), 0, null, SearchTag.RESULT, placeResponse.getPlace().getLatLng()));
                    googleSearchResults.clear();
                    googleSearchResults.addAll(tempList);
                    googleSearchAdapter = new GoogleSearchAdapter(this, googleSearchResults);
                    googleSearchAdapter.setClickListener(this);
                    googleSearchRecycler.setAdapter(googleSearchAdapter);
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
                    ArrayList<SearchResult> tempList = new ArrayList<>();
                    JSONObject jsonObject = new JSONObject(resp);
                    JSONArray features = jsonObject.getJSONArray("features");
                    for (int i = 0; i < features.length(); i++) {
                        if (i == 0)
                            tempList.add(new SearchResult("On Campus", "", 0, null, SearchTag.CATEGORY, null));
                        String bldgName = features.getJSONObject(i).getJSONObject("attributes").getString("BldgName");
                        String address = features.getJSONObject(i).getJSONObject("attributes").getString("Address");
                        double lat = features.getJSONObject(i).getJSONObject("attributes").getDouble("Latitude");
                        double lng = features.getJSONObject(i).getJSONObject("attributes").getDouble("Longitude");
                        if (address.equals("null"))
                            address = lat + ", " + lng; // If no address, use lat/lng instead
                        String finalAddress = address;
                        tempList.add(new SearchResult(bldgName, finalAddress, 0, null, SearchTag.RESULT, new LatLng(lat, lng)));
                    }
                    runOnUiThread(() -> {
                        gisSearchResults.clear();
                        gisSearchResults.addAll(tempList);
                        gisSearchAdapter = new GISSearchAdapter(this, gisSearchResults);
                        gisSearchAdapter.setClickListener(this);
                        gisSearchRecycler.setAdapter(gisSearchAdapter);
                    });
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
        clearFocusOnSearch();
    }

    /*
     * Un-Show the navigation bar and get out of full screen
     */
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
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
        View decorView = getWindow().getDecorView();

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

    /*
     * When a Bus Route Search Result is tapped, show the bus route
     */
    @Override
    public void onBusRouteClick(View view, int position) {
        MapsFragment.mMap.clear();
        MapsFragment mapsFragment = (MapsFragment) mapsFragment1;
        if (mapsFragment != null) {
            for (BusRoute i : mapsFragment.busRoutes) {
                if (i.routeNumber.equals(busRoutesSearchAdapter.getItem(position).title)) {
                    MapsFragment.currentRouteNo = i.routeNumber;
                    new Thread(() -> mapsFragment.drawBusesOnRoute(i.routeNumber)).start();
                    new Thread(() -> mapsFragment.drawBusRoute(i.routeNumber, i.color, true)).start();
                    break;
                }
            }
        }
        clearFocusOnSearch();
    }
}

