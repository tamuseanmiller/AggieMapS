package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;

import android.content.Context;
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
import android.util.DisplayMetrics;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.lapism.search.widget.MaterialSearchBar;
import com.lapism.search.widget.MaterialSearchView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import eu.okatrych.rightsheet.RightSheetBehavior;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity implements GISSearchAdapter.ItemClickListener, GoogleSearchAdapter.ItemClickListener, BusRoutesSearchAdapter.ItemClickListener, RecentSearchesAdapter.ItemClickListener {

    private static final int SPEECH_REQUEST_CODE = 0;
    private MaterialSearchBar materialSearchBar;
    private MaterialSearchView materialSearchView;
    private OkHttpClient client;  // Client to make API requests
    private GISSearchAdapter gisSearchAdapter;
    private GoogleSearchAdapter googleSearchAdapter;
    private BusRoutesSearchAdapter busRoutesSearchAdapter;
    private ArrayList<ListItem> gisListItems;
    private ArrayList<ListItem> googleListItems;
    private PlacesClient placesClient;
    private RecyclerView gisSearchRecycler;
    private RecyclerView googleSearchRecycler;
    private RecyclerView busRoutesSearchRecycler;
    private ArrayList<ListItem> busRoutesListItems;
    private ArrayList<ListItem> recentSearchesListItems;
    private RecentSearchesAdapter recentSearchesAdapter;
    private RecyclerView recentSearchesRecycler;
    private MaterialSearchBar srcSearchBar;
    private MaterialSearchBar destSearchBar;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private DirectionsAdapter directionsAdapter;
    private FrameLayout sheet;
    private CircularProgressIndicator tripProgress;
    private AppBarLayout defaultSearchBar;
    private LinearLayout llSrcDestContainer;
    private boolean inDirectionsMode;
    private FloatingActionButton fabCancel;
    private FloatingActionButton fabSwap;
    private int SearchBar;
    private String srcBarText = "";
    private String destBarText = "";
    private RecyclerView directionsRecycler;
    private static final int MAIN_SEARCH_BAR = 1;
    private static final int SRC_SEARCH_BAR = 2;
    private static final int DEST_SEARCH_BAR = 3;
    private static final int GIS_ADAPTER = 1;
    private static final int GOOGLE_ADAPTER = 2;
    private LinkedList<ListItem> recentSearchesTemp;

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
                Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
            }
        } else {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.cl_main), "Your network is unavailable. Check your data or wifi connection.", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("RETRY", view -> {
                haveNetworkConnection();
                networkCheckCount += 1;
            });
            snackbar.show();
        }
    }

    enum ManeuverType {
        Unknown,
        Stop,
        Straight,
        BearLeft,
        BearRight,
        TurnLeft,
        TurnRight,
        SharpLeft,
        SharpRight,
        UTurn,
        Ferry,
        Roundabout,
        HighwayMerge,
        HighwayExit,
        HighwayChange,
        ForkCenter,
        ForkLeft,
        ForkRight,
        Depart,
        TripItem,
        EndOfFerry,
        RampRight,
        RampLeft
    }

    private @DrawableRes
    int parseManeuverType(String maneuverType) {
        switch (maneuverType) {
            case "esriDMTStop":
                return R.drawable.close_octagon;
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
                return R.drawable.car;
            case "esriDMTTripItem":
                return R.drawable.sign_direction;
            default:
                return R.drawable.nuke;
        }
    }

    private int convertDpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /*
     * Method to create array of a route from two latlng coordinates
     * returns a TripPlan obj
     */
    private TripPlan getTripPlan(LatLng src, LatLng dest, int tripType) {
        try {

            String call = "https://gis.tamu.edu/arcgis/rest/services/Routing/ChrisRoutingTest/NAServer/Route/solve?stops=%7B%22features%22%3A%5B%7B%22geometry%22%3A%7B%22x%22%3A" + src.longitude + "%2C%22y%22%3A" + src.latitude + "%7D%2C%22attributes%22%3A%7B%22Name%22%3A%22From%22%2C%22RouteName%22%3A%22Route+A%22%7D%7D%2C%7B%22geometry%22%3A%7B%22x%22%3A" + dest.longitude + "%2C%22y%22%3A" + dest.latitude + "%7D%2C%22attributes%22%3A%7B%22Name%22%3A%22To%22%2C%22RouteName%22%3A%22Route+A%22%7D%7D%5D%7D&outSR=4326&ignoreInvalidLocations=true&accumulateAttributeNames=Length%2C+Time&impedanceAttributeName=Time&restrictUTurns=esriNFSBAllowBacktrack&useHierarchy=false&returnDirections=true&returnRoutes=true&returnStops=false&returnBarriers=false&returnPolylineBarriers=false&returnPolygonBarriers=false&directionsLanguage=en&outputLines=esriNAOutputLineTrueShapeWithMeasure&findBestSequence=true&preserveFirstStop=true&preserveLastStop=true&useTimeWindows=false&timeWindowsAreUTC=false&startTime=5&startTimeIsUTC=false&outputGeometryPrecisionUnits=esriMiles&directionsOutputType=esriDOTComplete&directionsTimeAttributeName=Time&directionsLengthUnits=esriNAUMiles&returnZ=false&travelMode=" + tripType + "&f=pjson";
            String result = getApiCall(call);
//            System.out.println((result));
            JSONArray features_json = new JSONObject(result).getJSONArray("directions").getJSONObject(0).getJSONArray("features");

            // Parse every feature
            ArrayList<Feature> features = new ArrayList<>();
            for (int i = 0; i < features_json.length(); i++) {
                JSONObject attributes = features_json.getJSONObject(i).getJSONObject("attributes");
                @DrawableRes int manueverType = parseManeuverType(attributes.getString("maneuverType"));
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

            // Draw line
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(10);
            polylineOptions.geodesic(true);
            polylineOptions.pattern(null);
            polylineOptions.clickable(true);
            polylineOptions.color(ContextCompat.getColor(this, R.color.accent));
            for (int i = 0; i < paths.length(); i++) {
                double lat = paths.getJSONArray(i).getDouble(0);
                double lng = paths.getJSONArray(i).getDouble(1);
                LatLng latlng = new LatLng(lng, lat);
                polylineOptions.add(latlng);
            }
            runOnUiThread(() -> MapsFragment.mMap.addPolyline(polylineOptions));

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

    private void clearFocusOnSearch() {
        materialSearchView.clearFocus();
        materialSearchView.setVisibility(View.GONE);
        if (inDirectionsMode) {
            llSrcDestContainer.setVisibility(View.VISIBLE);
            sheet.setVisibility(View.VISIBLE);
        } else {
            materialSearchBar.setVisibility(View.VISIBLE);
        }
        showSystemUI();

    }

    private void requestFocusOnSearch(int whichSearchBar) {
        materialSearchView.requestFocus();
        materialSearchView.setVisibility(View.VISIBLE);
        materialSearchBar.setVisibility(View.GONE);
        llSrcDestContainer.setVisibility(View.GONE);
        sheet.setVisibility(View.GONE);
        hideSystemUI();
        SearchBar = whichSearchBar;
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

        // Set the default toolbar and actionbar
        Toolbar toolbar = materialSearchBar.getToolbar();
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        Drawable nav = ContextCompat.getDrawable(this, R.drawable.magnify);
        if (nav != null && actionBar != null) {
            nav.setTint(getColor(R.color.foreground));
            actionBar.setIcon(nav);
        }

        // Set Default Search Bar Settings
        materialSearchBar.setHint("Aggie MapS");
        materialSearchBar.setElevation(5);
        materialSearchBar.setBackgroundColor(getColor(R.color.background));
        materialSearchBar.setOnClickListener(v -> requestFocusOnSearch(MAIN_SEARCH_BAR));
        materialSearchBar.setNavigationOnClickListener(v -> requestFocusOnSearch(MAIN_SEARCH_BAR));

        // Set recyclers
        gisListItems = new ArrayList<>();
        googleListItems = new ArrayList<>();
        busRoutesListItems = new ArrayList<>();
        recentSearchesListItems = new ArrayList<>();
        NestedScrollView nSV = new NestedScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        gisSearchAdapter = new GISSearchAdapter(this, gisListItems);
        gisSearchAdapter.setClickListener(this);
        googleSearchAdapter = new GoogleSearchAdapter(this, googleListItems);
        googleSearchAdapter.setClickListener(this);
        busRoutesSearchAdapter = new BusRoutesSearchAdapter(this, busRoutesListItems);
        busRoutesSearchAdapter.setClickListener(this);
        Map<String, RecentSearches> cachedRecentSearches = RecentSearches.getData(getApplicationContext());
        // Add Recent Searches
        if (cachedRecentSearches.containsKey("recentSearches")) {
            recentSearchesListItems.add(new ListItem("Current Location", "Last Known Location", ContextCompat.getColor(this, R.color.blue_500), R.drawable.crosshairs_gps, SearchTag.RESULT, null));
            Queue<ListItem> recentSearchesList = Objects.requireNonNull(cachedRecentSearches.get("recentSearches")).recentSearchesList;
            if (recentSearchesList != null) {
                recentSearchesListItems.addAll(recentSearchesList);
            }
        }
        recentSearchesAdapter = new RecentSearchesAdapter(this, recentSearchesListItems);
        recentSearchesAdapter.setClickListener(this);

        // Create recyclerviews and their layout managers
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

        recentSearchesRecycler = new RecyclerView(this);
        recentSearchesRecycler.suppressLayout(true);
        recentSearchesRecycler.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        // Add divider
        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(this, DividerItemDecoration.HORIZONTAL);
        divider.setDividerInsetEnd(20);
        divider.setDividerInsetStart(20);
        recentSearchesRecycler.addItemDecoration(divider);
        recentSearchesRecycler.setAdapter(recentSearchesAdapter);

        ll.addView(recentSearchesRecycler);
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

        // 1. Create new ArrayList of SearchResults
//        searchResultsItems = new ArrayList<>();
//        directionsAdapter = new DirectionsAdapter(this, searchResultsItems);
        directionsRecycler = findViewById(R.id.directions_recycler);
        directionsRecycler.setLayoutManager(new LinearLayoutManager(this));

        // 2. Initialize SearchBars
        srcSearchBar = findViewById(R.id.src_search_bar);
        destSearchBar = findViewById(R.id.dest_search_bar);

        // 3. Set toolbar and action bar
        toolbar = srcSearchBar.getToolbar();
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (nav != null && actionBar != null) {
            nav.setTint(getColor(R.color.foreground));
            actionBar.setIcon(nav);
        }
        toolbar = destSearchBar.getToolbar();
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (nav != null && actionBar != null) {
            nav.setTint(getColor(R.color.foreground));
            actionBar.setIcon(nav);
        }

        // 4. Create the views for the SearchBars
        srcSearchBar.setOnClickListener(v -> requestFocusOnSearch(SRC_SEARCH_BAR));
        destSearchBar.setOnClickListener(v -> requestFocusOnSearch(DEST_SEARCH_BAR));
        srcSearchBar.setElevation(5);
        srcSearchBar.setBackgroundColor(getColor(R.color.background));
        srcSearchBar.setNavigationOnClickListener(v -> requestFocusOnSearch(SRC_SEARCH_BAR));
        destSearchBar.setElevation(5);
        destSearchBar.setBackgroundColor(getColor(R.color.background));
        destSearchBar.setNavigationOnClickListener(v -> requestFocusOnSearch(DEST_SEARCH_BAR));

        // 5. Set the SearchView Settings
        // reuse materialSearchView settings

        // 6. Initialize the BottomSheet
        sheet = findViewById(R.id.directions_bottom_sheet);

        // 7. Get the BottomSheetBehavior
        bottomSheetBehavior = BottomSheetBehavior.from(sheet);

        // 8. Set the settings of the BottomSheetBehavior
        bottomSheetBehavior.setSaveFlags(RightSheetBehavior.SAVE_ALL);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(findViewById(R.id.cl_directions).getMeasuredHeight() + convertDpToPx(135));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Set the max height of the bottom sheet by putting it below the searchbar
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        View destBar = findViewById(R.id.dest_app_bar);
        View srcBar = findViewById(R.id.src_app_bar);
//        bottomSheetBehavior.setMaxHeight(height - findViewById(R.id.main_app_bar).getHeight() * 2 - convertDpToPx(32));

        // 9. Initialize Progress Indicator
        tripProgress = findViewById(R.id.trip_progress);

        // 10. Initialize Main App Bar
        defaultSearchBar = findViewById(R.id.main_app_bar);
        ViewTreeObserver viewTreeObserver = defaultSearchBar.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    defaultSearchBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    bottomSheetBehavior.setMaxHeight(height - defaultSearchBar.getHeight() * 2);
                }
            });
        }

        // 11. Initialize Source and Dest Container
        llSrcDestContainer = findViewById(R.id.ll_srcdest);

        // Initialize cancel fab and click listener
        fabCancel = findViewById(R.id.fab_cancel);
        fabCancel.setOnClickListener(v -> exitDirectionsMode());

        fabSwap = findViewById(R.id.fab_swap);
        fabSwap.setOnClickListener(v -> swapDirections());

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
            requestFocusOnSearch(MAIN_SEARCH_BAR);
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
            ArrayList<ListItem> tempList = new ArrayList<>();
            tempList.add(new ListItem("Bus Routes", "", 0, SearchTag.CATEGORY));

            // Loop through every bus route, check to see if the
            // name or number contains the given char sequence
            MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentById(R.id.maps_fragment);
            if (mapsFragment != null) {
                for (int i = 1; i < mapsFragment.busRoutes.size(); i++) {
                    String routeNumber = mapsFragment.busRoutes.get(i).routeNumber.toLowerCase();
                    String routeName = mapsFragment.busRoutes.get(i).routeName.toLowerCase();
                    if ((routeNumber.contains(charSequence) || routeName.contains(charSequence)) && !routeNumber.equals("all"))
                        tempList.add(new ListItem(mapsFragment.busRoutes.get(i).routeNumber, mapsFragment.busRoutes.get(i).routeName, 0, SearchTag.RESULT));
                }
            }

            // If no bus routes, clear it
            if (tempList.size() == 1) {
                tempList.clear();
            }

            // Set all values
            runOnUiThread(() -> {
                busRoutesListItems.clear();
                busRoutesListItems.addAll(tempList);
                busRoutesSearchAdapter = new BusRoutesSearchAdapter(this, busRoutesListItems);
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
            ArrayList<ListItem> tempList = new ArrayList<>();
            if (!response.getAutocompletePredictions().isEmpty()) {
                tempList.add(new ListItem("Google Maps", "", 0, SearchTag.CATEGORY));
            }
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                Log.i(TAG, prediction.getPlaceId());
                Log.i(TAG, prediction.getPrimaryText(null).toString());

                // Specify the fields to return.
                final List<Place.Field> placeFields = Collections.singletonList(Place.Field.LAT_LNG);

                // Construct a request object, passing the place ID and fields array.
                final FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(prediction.getPlaceId(), placeFields);

                placesClient.fetchPlace(placeRequest).addOnSuccessListener((placeResponse) -> {
                    tempList.add(new ListItem(prediction.getPrimaryText(null).toString(), prediction.getFullText(null).toString(), 0, SearchTag.RESULT, placeResponse.getPlace().getLatLng()));
                    googleListItems.clear();
                    googleListItems.addAll(tempList);
                    googleSearchAdapter = new GoogleSearchAdapter(this, googleListItems);
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
                    ArrayList<ListItem> tempList = new ArrayList<>();
                    JSONObject jsonObject = new JSONObject(resp);
                    JSONArray features = jsonObject.getJSONArray("features");
                    for (int i = 0; i < features.length(); i++) {
                        if (i == 0)
                            tempList.add(new ListItem("On Campus", "", 0, SearchTag.CATEGORY));
                        String bldgName = features.getJSONObject(i).getJSONObject("attributes").getString("BldgName");
                        String address = features.getJSONObject(i).getJSONObject("attributes").getString("Address");
                        double lat = features.getJSONObject(i).getJSONObject("attributes").getDouble("Latitude");
                        double lng = features.getJSONObject(i).getJSONObject("attributes").getDouble("Longitude");
                        if (address.equals("null"))
                            address = lat + ", " + lng; // If no address, use lat/lng instead
                        String finalAddress = address;
                        tempList.add(new ListItem(bldgName, finalAddress, 0, SearchTag.RESULT, new LatLng(lat, lng)));
                    }
                    runOnUiThread(() -> {
                        gisListItems.clear();
                        gisListItems.addAll(tempList);
                        gisSearchAdapter = new GISSearchAdapter(this, gisListItems);
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
        Log.e("search bar test", String.valueOf(materialSearchBar.getVisibility()));
        if (materialSearchView.hasFocus()) {
            clearFocusOnSearch();
        } else if (materialSearchBar.getVisibility() == View.GONE) {
            exitDirectionsMode();
        }
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
        addRecentSearches(GIS_ADAPTER, position);
        MarkerOptions selectedResult = new MarkerOptions();
        selectedResult.position(gisSearchAdapter.getItem(position).position);
        selectedResult.title(gisSearchAdapter.getItem(position).title);
        // clear the map
        MapsFragment.mMap.clear();
        MapsFragment.mMap.addMarker(selectedResult);
        MapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gisSearchAdapter.getItem(position).position, 18.0f));
        enterDirectionsMode(gisSearchAdapter.getItem(position));

        clearFocusOnSearch();
    }

    /*
     * When a Google Search Result is tapped, create marker and animate to position
     */
    @Override
    public void onGoogleClick(View view, int position) {
        addRecentSearches(GOOGLE_ADAPTER, position);
        MarkerOptions selectedResult = new MarkerOptions();
        selectedResult.position(googleSearchAdapter.getItem(position).position);
        selectedResult.title(googleSearchAdapter.getItem(position).title);
        // clear the map
        MapsFragment.mMap.clear();
        MapsFragment.mMap.addMarker(selectedResult);
        MapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(googleSearchAdapter.getItem(position).position, 18.0f));
        enterDirectionsMode(googleSearchAdapter.getItem(position));

        clearFocusOnSearch();
    }

    /*
     * When a Bus Route Search Result is tapped, show the bus route
     */
    @Override
    public void onBusRouteClick(View view, int position) {
        MapsFragment.mMap.clear();
        MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentById(R.id.maps_fragment);
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


    /*
     * When a Recent Searches Result is tapped, show the recent searched
     */
    @Override
    public void onRecentSearchClick(View view, int position) {
        ListItem recentSearch = recentSearchesAdapter.getItem(position);

        if (recentSearch.position == null) {
            MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentById(R.id.maps_fragment);
            mapsFragment.getDeviceLocation();
        } else {

            MarkerOptions selectedResult = new MarkerOptions();
            selectedResult.position(recentSearch.position);
            selectedResult.title(recentSearch.title);
            // clear the map
            MapsFragment.mMap.clear();
            MapsFragment.mMap.addMarker(selectedResult);
            MapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(recentSearch.position, 18.0f));
            enterDirectionsMode(recentSearch);
        }

        clearFocusOnSearch();
    }


    /*
     * TODO: Method to enter the directions screen from the main activity
     */
    public void enterDirectionsMode(ListItem destItem) {
        // Set the boolean value
        inDirectionsMode = true;

        // Set the visibility of the default searchbar to "gone"
        materialSearchBar.setVisibility(View.GONE);

        // Set the visibility of the src,dest searchbars to "visible"
        llSrcDestContainer.setVisibility(View.VISIBLE);

        // Set text for src,dest
        if (destItem != null) {
            if (SearchBar == MAIN_SEARCH_BAR) {
                srcSearchBar.setText("Current location");
                destSearchBar.setText(destItem.title);
                srcBarText = "Current location";
                destBarText = destItem.title;
            } else if (SearchBar == SRC_SEARCH_BAR) {
                srcSearchBar.setText(destItem.title);
                srcBarText = destItem.title;
            } else if (SearchBar == DEST_SEARCH_BAR) {
                destSearchBar.setText(destItem.title);
                destBarText = destItem.title;
            }
        }

        // Get rid of buses button, timetable button, and find me button
        MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentById(R.id.maps_fragment);
        mapsFragment.fabTimetable.setVisibility(View.GONE);
        mapsFragment.fabMyLocation.setVisibility(View.INVISIBLE);
        mapsFragment.swipeRecycler.setVisibility(View.INVISIBLE);
        mapsFragment.fab_directions.setVisibility(View.INVISIBLE);
        mapsFragment.fab_directions.setVisibility(View.INVISIBLE);

        // Hide the routes bottomsheet if it is open
        mapsFragment.standardBottomSheet.setVisibility(View.GONE);
        mapsFragment.standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        if (destItem != null) {
            // Start a progress indicator in one of the searchviews
            tripProgress.setVisibility(View.VISIBLE);

            // Get Trip Plan and input into
            new Thread(() -> {
                TripPlan newTripPlan = getTripPlan(mapsFragment.deviceLatLng, destItem.position, 1);
                ArrayList<ListItem> textDirections = new ArrayList<>();
                ArrayList<Feature> routeFeatures = newTripPlan.getFeatures();
                for (int i = 0; i < routeFeatures.size(); i++) {
                    Feature currFeature = routeFeatures.get(i);
                    // TODO: fix to add parsing direction type
                    textDirections.add(new ListItem(currFeature.getText(), String.valueOf(currFeature.getLength()), 0, currFeature.getManeuverType(), SearchTag.RESULT, null));
                }

                // Parse the trip plan into the BottomBar
                runOnUiThread(() -> {
                    directionsAdapter = new DirectionsAdapter(this, textDirections);
                    directionsRecycler.setAdapter(directionsAdapter);
                });
            }).start();

            // Change the visibility of the BottomBar to "visible"
            sheet.setVisibility(View.VISIBLE);

            // End the progress indicator
            tripProgress.setVisibility(View.INVISIBLE);
        }
    }

    /*
     * TODO: Method to exit the directions screen from the main activity
     */
    public void exitDirectionsMode() {
        // Set the boolean value to false
        inDirectionsMode = false;

        // Set the visibility of the default searchbar to "visible"
        materialSearchBar.setVisibility(View.VISIBLE);

        // Set the visibility of the src,dest searchbars to "gone"
        llSrcDestContainer.setVisibility(View.GONE);

        // Add back the buses button, timetable button, find me button, and close the bus routes bottom sheet.
        MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentById(R.id.maps_fragment);
        mapsFragment.fabMyLocation.setVisibility(View.VISIBLE);
        mapsFragment.swipeRecycler.setVisibility(View.VISIBLE);
        mapsFragment.fab_directions.setVisibility(View.VISIBLE);
        mapsFragment.standardBottomSheet.setVisibility(View.VISIBLE);

        // clear the map
        mapsFragment.mMap.clear();

        // Change the visibility of the BottomBar to "gone"
        bottomSheetBehavior.setState(bottomSheetBehavior.STATE_COLLAPSED);
        sheet.setVisibility(View.GONE);
    }

    private void swapDirections() {
        srcSearchBar.setText(destBarText);
        destSearchBar.setText(srcBarText);
        String temp_text = srcBarText;
        srcBarText = destBarText;
        destBarText = temp_text;

    }

    /*
     * A helper function to update the arraylist of recent searches and shared prefs
     */
    private void addRecentSearches(int adapter, int position) {

        Map<String, RecentSearches> cachedRecentSearches = RecentSearches.getData(getApplicationContext());
        // Get the cached recent searches list otherwise create a new temp linkedlist
        if (cachedRecentSearches.containsKey("recentSearches")) {
            LinkedList<ListItem> recentSearchesList = Objects.requireNonNull(cachedRecentSearches.get("recentSearches")).recentSearchesList;
            if (recentSearchesList != null) {
                recentSearchesTemp = recentSearchesList;
            }
        } else {
            recentSearchesTemp = new LinkedList<>();
        }

        // Remove the first search as it is the most least recent.
        if (recentSearchesTemp.size() > 5) {
            recentSearchesTemp.removeLast();
        }

        // Get the data from the appropriate adapter and add it to the temp list
        if (adapter == GIS_ADAPTER) {
            if (!recentSearchesTemp.contains(gisSearchAdapter.getItem(position))) {
                ListItem temp = gisSearchAdapter.getItem(position);
                temp.setDirection(R.drawable.clock);
                temp.setColor(ContextCompat.getColor(this, R.color.grey_500));
                recentSearchesTemp.addFirst(temp);
            }
        } else if (adapter == GOOGLE_ADAPTER) {
            if (!recentSearchesTemp.contains(googleSearchAdapter.getItem(position))) {
                ListItem temp = googleSearchAdapter.getItem(position);
                temp.setDirection(R.drawable.clock);
                temp.setColor(ContextCompat.getColor(this, R.color.grey_500));
                recentSearchesTemp.addFirst(temp);
            }
        }

        // Update the search view
        recentSearchesListItems.clear();
        recentSearchesListItems.add(new ListItem("Current Location", "Last Known Location", ContextCompat.getColor(this, R.color.blue_500), R.drawable.crosshairs_gps, SearchTag.RESULT, null));
        recentSearchesListItems.addAll(recentSearchesTemp);
        recentSearchesAdapter = new RecentSearchesAdapter(this, recentSearchesListItems);
        recentSearchesAdapter.setClickListener(this);
        recentSearchesRecycler.setAdapter(recentSearchesAdapter);

        // Write to shared Prefs
        RecentSearches rs = new RecentSearches(recentSearchesTemp);
        RecentSearches.writeData(getApplicationContext(), rs, "recentSearches");
    }
}
