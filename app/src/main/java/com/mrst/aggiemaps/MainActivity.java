package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.lapism.search.widget.MaterialSearchBar;
import com.lapism.search.widget.MaterialSearchView;
import com.permissionx.guolindev.PermissionX;
import com.takusemba.spotlight.OnSpotlightListener;
import com.takusemba.spotlight.OnTargetListener;
import com.takusemba.spotlight.Spotlight;
import com.takusemba.spotlight.Target;
import com.takusemba.spotlight.effet.RippleEffect;
import com.takusemba.spotlight.shape.Circle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import eu.okatrych.rightsheet.RightSheetBehavior;
import github.com.st235.lib_expandablebottombar.ExpandableBottomBar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity implements GISSearchAdapter.ItemClickListener, GoogleSearchAdapter.ItemClickListener, BusRoutesSearchAdapter.ItemClickListener, RecentSearchesAdapter.ItemClickListener {

    private static final int SPEECH_REQUEST_CODE = 0;
    private MaterialSearchBar materialSearchBar;  // The default search bar
    private MaterialSearchView materialSearchView;  // The view that is shown when the bar is tapped
    private OkHttpClient client;  // Client to make API requests
    private GISSearchAdapter gisSearchAdapter;
    private GoogleSearchAdapter googleSearchAdapter;
    private BusRoutesSearchAdapter busRoutesSearchAdapter;
    private ArrayList<ListItem> gisListItems;
    private ArrayList<ListItem> googleListItems;
    private PlacesClient placesClient;  // The client for querying places via Google Maps
    private RecyclerView gisSearchRecycler;
    private RecyclerView googleSearchRecycler;
    private RecyclerView busRoutesSearchRecycler;
    final SettingsFragment settingsFragment = new SettingsFragment();
    private ArrayList<ListItem> busRoutesListItems;
    private ArrayList<ListItem> recentSearchesListItems;
    private RecentSearchesAdapter recentSearchesAdapter;
    private RecyclerView recentSearchesRecycler;
    public ExpandableBottomBar bottomBar;  // The bottom navigation bar
    public int whichSearchBar;
    private ViewPager2 viewPager;  // The viewpager that hold all fragments
    private LinkedList<ListItem> recentSearchesTemp;  // The queue of all recent searches
    private FragmentStateAdapter pagerAdapter;  // the adapter for the viewpager above
    private static final int GIS_ADAPTER = 1;
    private static final int GOOGLE_ADAPTER = 2;
    private static final int RECENTS_ADAPTER = 3;
    public static final int MAIN_SEARCH_BAR = 1;
    public static final int SRC_SEARCH_BAR = 2;
    public static final int DEST_SEARCH_BAR = 3;
    private FusedLocationProviderClient fusedLocationProviderClient;
    public int gisId;
    public int recentId;
    private Spotlight s;

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

    private int convertDpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void clearFocusOnSearch() {

        // Change visibility of search view and bar
        materialSearchView.clearFocus();
        materialSearchView.setVisibility(View.GONE);

        // Set color of status bar back to transparent
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // Get rid of bottom bar
        bottomBar.setVisibility(View.VISIBLE);

        // Case for if it is in the directions fragment
        DirectionsFragment directionsFragment = (DirectionsFragment) getSupportFragmentManager().findFragmentByTag("f1");
        if (whichSearchBar > 1 && directionsFragment != null) {
            directionsFragment.clearFocusOnSearch();
        } else {
            materialSearchBar.setVisibility(View.VISIBLE);
        }
    }

    public void requestFocusOnSearch(int searchBar) {

        // Change visibility of search view and bar
        materialSearchView.requestFocus();
        materialSearchView.setVisibility(View.VISIBLE);
        materialSearchBar.setVisibility(View.GONE);

        // Set the status and nav bar color
        getWindow().setStatusBarColor(getColor(R.color.background));
        getWindow().setNavigationBarColor(getColor(R.color.background));

        // Set which search bar is being called and get rid of the bottom bar
        whichSearchBar = searchBar;
        bottomBar.setVisibility(View.GONE);

        // Collapse route sheet if it is open
        MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentByTag("f2");
        if (mapsFragment != null) {
            mapsFragment.standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            mapsFragment.standardBottomSheet.setVisibility(View.INVISIBLE);
        }
    }

    public void setBar(Toolbar tb) {
        setSupportActionBar(tb);
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
                return Objects.requireNonNull(body).string(); // Return the response as a string
            } else {
                // notify error
                String errorCode = Integer.toString(response.code());
                String errorMessage = response.message();
                Snackbar snackbar = Snackbar.make(findViewById(R.id.cl_main), "Error Code: " + errorCode + " " + errorMessage, Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("Try Again", view -> {
                    snackbar.dismiss();
                });
                snackbar.show();
                return "";
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void setWindowFlag(final int bits, boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    /*
     * Function to set the default map padding based on bottom bar
     */
    public int getDefaultBottomPadding() {

        // Get bottom bar height
        int bottomBarHeight = 0;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int usableHeight = displayMetrics.heightPixels;
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int realHeight = displayMetrics.heightPixels;
        if (realHeight > usableHeight)
            bottomBarHeight = realHeight - usableHeight;

        return bottomBarHeight;
    }

    private void transparentStatusAndNavigation() {
        Window window = getWindow();

        // make full transparent statusBar
        int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        visibility = visibility | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        window.getDecorView().setSystemUiVisibility(visibility);
        int windowManager = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        windowManager = windowManager | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        setWindowFlag(windowManager, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        // Set light/dark icons
        int nightModeFlags = getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                int flags = getWindow().getDecorView().getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                getWindow().getDecorView().setSystemUiVisibility(flags);
                break;

            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                flags = getWindow().getDecorView().getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                getWindow().getDecorView().setSystemUiVisibility(flags);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

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
        transparentStatusAndNavigation();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Set the status bar to be transparent
//        Window w = getWindow();
//        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        client = new OkHttpClient();  // Create OkHttpClient to be used in API request
        new Thread(this::haveNetworkConnection).start();

        new Thread(() -> {

            // Set up BottomBar with viewpager
            viewPager = findViewById(R.id.pager);
            pagerAdapter = new BottomPagerAdapter(this);
            runOnUiThread(() -> {
                viewPager.setAdapter(pagerAdapter);
                viewPager.setUserInputEnabled(false);
                viewPager.setOffscreenPageLimit(3);
            });
            viewPager.setCurrentItem(2);
            pagerAdapter.createFragment(0);
            pagerAdapter.createFragment(1);
            pagerAdapter.createFragment(2);

            // Construct a FusedLocationProviderClient.
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            // Initialize Places
            // Initialize the SDK
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);

            // Create a new PlacesClient instance
            placesClient = Places.createClient(this);

            // Initialize the SearchBar and View
            materialSearchBar = findViewById(R.id.material_search_bar);
            materialSearchView = findViewById(R.id.material_search_view);

            // Set Default Search Bar Settings
            runOnUiThread(() -> {
                // Set the default toolbar and actionbar
                Toolbar toolbar = materialSearchBar.getToolbar();
                runOnUiThread(() -> setSupportActionBar(toolbar));
                ActionBar actionBar = getSupportActionBar();
                Drawable nav = ContextCompat.getDrawable(this, R.drawable.magnify);
                if (nav != null && actionBar != null) {
                    nav.setTint(getColor(R.color.foreground));
                }
                materialSearchBar.setNavigationIcon(nav);
                materialSearchBar.setHint("Aggie MapS");
                materialSearchBar.setElevation(5);
                materialSearchBar.setBackgroundColor(getColor(R.color.background));
                materialSearchBar.setOnClickListener(v -> requestFocusOnSearch(MAIN_SEARCH_BAR));
                materialSearchBar.setNavigationOnClickListener(v -> requestFocusOnSearch(MAIN_SEARCH_BAR));
            });

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
            recentSearchesListItems.add(new ListItem("Current Location", "Last Known Location", ContextCompat.getColor(this, R.color.blue_500), R.drawable.crosshairs_gps, SearchTag.RESULT, null));

            if (cachedRecentSearches.containsKey("recentSearches")) {
                Queue<ListItem> recentSearchesList = Objects.requireNonNull(cachedRecentSearches.get("recentSearches")).recentSearchesList;
                if (recentSearchesList != null) {
                    recentSearchesListItems.addAll(recentSearchesList);
                }
            }
            recentSearchesAdapter = new RecentSearchesAdapter(this, recentSearchesListItems);
            recentSearchesAdapter.setClickListener(this);

            // Create recyclerviews and their layout managers
            gisSearchRecycler = new RecyclerView(this);
            gisId = 93174320;
            gisSearchRecycler.setId(gisId);
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
            recentId = 93174420;
            recentSearchesRecycler.setId(recentId);

            // Add divider
            MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(this, DividerItemDecoration.HORIZONTAL);
            divider.setDividerInsetEnd(20);
            divider.setDividerInsetStart(20);
            recentSearchesRecycler.addItemDecoration(divider);
            runOnUiThread(() -> recentSearchesRecycler.setAdapter(recentSearchesAdapter));

            // Add views into the material search view
            ll.addView(recentSearchesRecycler);
            ll.addView(gisSearchRecycler);
            ll.addView(busRoutesSearchRecycler);
            ll.addView(googleSearchRecycler);
            nSV.addView(ll);

            // Set SearchView Settings
            runOnUiThread(() -> {
                materialSearchView.addView(nSV);
                Drawable navigationIcon = ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24);
                if (navigationIcon != null) {
                    navigationIcon.setTintList(ColorStateList.valueOf(getColor(R.color.foreground)));
                }
                materialSearchView.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24));
                materialSearchView.setVisibility(View.GONE);
                materialSearchView.setHint("Try Building Numbers/Names");
                materialSearchView.setBackgroundColor(ContextCompat.getColor(this, R.color.background));
                Drawable clearIcon = ContextCompat.getDrawable(this, R.drawable.close);
                if (clearIcon != null) {
                    clearIcon.setTintList(ColorStateList.valueOf(getColor(R.color.foreground)));
                }
                materialSearchView.setClearIcon(clearIcon);
                materialSearchView.setDividerColor(ContextCompat.getColor(this, android.R.color.transparent));
                materialSearchView.setTextClearOnBackPressed(false);
            });

            // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
            // and once again when the user makes a selection (for example when calling fetchPlace()).
            AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

            // Set OnClick Listeners
            materialSearchView.setNavigationOnClickListener(v -> clearFocusOnSearch());

            // Set listeners for when someone tries to type into the searchview
            materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(@NonNull CharSequence charSequence) {
                    if (charSequence.length() == 0) return true;
                    queryGIS(charSequence); // Query GIS, Google
                    if (whichSearchBar == MAIN_SEARCH_BAR) queryBusRoutes(charSequence);
                    queryGoogle(charSequence, token);
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(@NonNull CharSequence charSequence) {
                    if (charSequence.length() == 0) return true;
                    queryGIS(charSequence); // Query GIS, Google, Bus Routes
                    if (whichSearchBar == MAIN_SEARCH_BAR) queryBusRoutes(charSequence);
                    queryGoogle(charSequence, token);
                    return true;
                }
            });

            // Add the settings fragment to the fragment manager
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().add(R.id.ll_main, settingsFragment, "3").hide(settingsFragment).commit();

            // Create bottom bar
            bottomBar = findViewById(R.id.bottom_bar);
            runOnUiThread(() -> {
                bottomBar.getMenu().select(R.id.buses);

                // Set padding to match navigation bar height
                RelativeLayout.LayoutParams bottomParams = new RelativeLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        convertDpToPx(65)
                );
                bottomParams.setMargins(convertDpToPx(20), convertDpToPx(20), convertDpToPx(20), getDefaultBottomPadding() + convertDpToPx(16));
                bottomBar.setLayoutParams(bottomParams);
            });

            // Get bus routes on tap
            bottomBar.setOnItemReselectedListener((i, j, k) -> {
                if (j.getId() == R.id.buses) {

                    // Finish spotlight
                    if (s != null) s.finish();

                    // If settings is visible, get rid of it and make the bar visible
                    if (getSupportFragmentManager().findFragmentByTag("3").isVisible()) {
                        materialSearchBar.setVisibility(View.VISIBLE);
                        fm.beginTransaction().hide(settingsFragment).commit();
                    } else {
                        // Open the bus routes bottom sheet
                        MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentByTag("f2");
                        if (mapsFragment != null) {
                            mapsFragment.standardBottomSheet.setVisibility(View.VISIBLE);
                            mapsFragment.standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                            mapsFragment.rightSheetBehavior.setState(RightSheetBehavior.STATE_COLLAPSED);
                        }
                    }
                }
                return null;
            });

            // Switch between fragments on tap
            bottomBar.setOnItemSelectedListener((i, j, k) -> {
                fm.beginTransaction().hide(settingsFragment).commit();
                if (j.getId() == R.id.directions) {
                    materialSearchBar.setVisibility(View.GONE);
                    viewPager.setCurrentItem(1);
                } else if (j.getId() == R.id.garages) {
                    materialSearchBar.setVisibility(View.GONE);
                    viewPager.setCurrentItem(0);
                } else if (j.getId() == R.id.buses) {
                    materialSearchBar.setVisibility(View.VISIBLE);
                    viewPager.setCurrentItem(2);
                }
                return null;
            });

        }).start();
    }

    private void createSpotlight(PointF anchor, @LayoutRes int layout) {

        // Show bus route selection spotlight
        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);
        View overlay = getLayoutInflater().inflate(layout, viewGroup, false);
        Target.Builder busRoutesTarget = new Target.Builder()
                .setAnchor(anchor)
                .setShape(new Circle(100f))
                .setEffect(new RippleEffect(100f, 200f, getColor(R.color.cyan_400)))
                .setOverlay(overlay)
                .setOnTargetListener(new OnTargetListener() {
                    @Override
                    public void onStarted() {

                    }

                    @Override
                    public void onEnded() {

                    }
                });

        Spotlight.Builder spotlight = new Spotlight.Builder(this)
                .setTargets(busRoutesTarget.build())
                .setBackgroundColor(getColor(R.color.background_60))
                .setContainer(viewGroup)
                .setDuration(1000L)
                .setAnimation(new DecelerateInterpolator(2f))
                .setOnSpotlightListener(new OnSpotlightListener() {
                    @Override
                    public void onStarted() {

                    }

                    @Override
                    public void onEnded() {

                    }
                });

        s = spotlight.build();

        runOnUiThread(s::start);

        Handler mHandler = new Handler();
        mHandler.postDelayed((Runnable) () -> {
            runOnUiThread(s::finish);
        }, 6000);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Get the preferences for the spotlight
        SharedPreferences sharedPref = getSharedPreferences("com.mrst.aggiemaps.preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (!sharedPref.getBoolean("bus_routes_spotlight", false)) {

            // Set the value to true so it doesn't show again
            editor.putBoolean("bus_routes_spotlight", true);
            editor.apply();

            // Create the location of the spotlight
            PointF bottomBarAnchor = new PointF();
            int[] bottomBarLocation = new int[2];
            bottomBar.getLocationInWindow(bottomBarLocation);
            float x = bottomBarLocation[0] + bottomBar.getWidth() / 2f + convertDpToPx(40);
            float y = bottomBarLocation[1] + bottomBar.getHeight() / 2f;
            bottomBarAnchor.set(x, y);

            // Add a spotlight to the bottom bar
            createSpotlight(bottomBarAnchor, R.layout.bus_routes_target);
        }
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
                getSupportFragmentManager().beginTransaction().show(settingsFragment).commit();
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
            MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentByTag("f2");
            if (mapsFragment != null) {
                for (int i = 1; i < mapsFragment.busRoutes.size(); i++) {
                    String routeNumber = mapsFragment.busRoutes.get(i).routeNumber.toLowerCase();
                    String routeName = mapsFragment.busRoutes.get(i).routeName.toLowerCase();
                    if ((routeNumber.contains(charSequence) || routeName.contains(charSequence)) && !routeNumber.equals("all") && tempList.size() < 6)
                        tempList.add(new ListItem(mapsFragment.busRoutes.get(i).routeNumber, mapsFragment.busRoutes.get(i).routeName, 0, R.drawable.bus, SearchTag.RESULT, null));
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
                if (!resp.isEmpty()) {
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
        // Hide settings fragment if open
        if (!getSupportFragmentManager().findFragmentByTag("3").isHidden()) {
            getSupportFragmentManager().beginTransaction().hide(settingsFragment).commit();
        }
        // End directions on back pressed if directions are being shown
        DirectionsFragment directionsFragment = (DirectionsFragment) getSupportFragmentManager().findFragmentByTag("f1");
        if (directionsFragment.directionsSheet.getVisibility() == View.VISIBLE) {
            directionsFragment.exitDirections();
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
        clearFocusOnSearch();
        DirectionsFragment directionsFragment = (DirectionsFragment) getSupportFragmentManager().findFragmentByTag("f1");
        MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentByTag("f2");

        // If not the main search bar, enter for directions
        if (whichSearchBar != MAIN_SEARCH_BAR) {
            directionsFragment.mMap.addMarker(selectedResult);
            enterDirectionsMode(gisSearchAdapter.getItem(position));
        } else {
            mapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gisSearchAdapter.getItem(position).position, 18.0f));
            mapsFragment.mMap.addMarker(selectedResult);
            Drawable directions = ContextCompat.getDrawable(this, R.drawable.directions);
            directions.setTint(ContextCompat.getColor(this, R.color.foreground));
            mapsFragment.mMap.setOnMarkerClickListener(marker -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Directions")
                        .setMessage("Would you like to find directions to " + marker.getTitle())
                        .setCancelable(true)
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            directionsFragment.mMap.addMarker(selectedResult);
                            enterDirectionsMode(new ListItem(marker.getTitle(), null, 0, 0, SearchTag.RESULT, marker.getPosition()));
                        })
                        .setNegativeButton("No", null)
                        .setIcon(directions)
                        .show();
                return false;
            });
        }
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
        clearFocusOnSearch();
        DirectionsFragment directionsFragment = (DirectionsFragment) getSupportFragmentManager().findFragmentByTag("f1");
        MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentByTag("f2");

        // If not the main search bar, enter for directions
        if (whichSearchBar != MAIN_SEARCH_BAR) {
            directionsFragment.mMap.addMarker(selectedResult);
            enterDirectionsMode(googleSearchAdapter.getItem(position));
        } else {
            mapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(googleSearchAdapter.getItem(position).position, 18.0f));
            mapsFragment.mMap.addMarker(selectedResult);
            mapsFragment.mMap.setOnMarkerClickListener(marker -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Directions")
                        .setMessage("Would you like to find directions to " + marker.getTitle())
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            directionsFragment.mMap.addMarker(selectedResult);
                            enterDirectionsMode(new ListItem(marker.getTitle(), null, 0, SearchTag.RESULT, marker.getPosition()));
                        })
                        .setIcon(R.drawable.directions)
                        .setCancelable(true)
                        .setNegativeButton("No", null)
                        .show();
                return false;
            });
        }
    }

    public void switchTheme(String newValue) {
        switch (newValue) {
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
    }

    /*
     * When a Bus Route Search Result is tapped, show the bus route
     */
    @Override
    public void onBusRouteClick(View view, int position) {
        MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentByTag("f2");
        mapsFragment.mMap.clear();
        for (BusRoute i : mapsFragment.busRoutes) {
            if (i.routeNumber.equals(busRoutesSearchAdapter.getItem(position).title)) {
                mapsFragment.currentRouteNo = i.routeNumber;
                new Thread(() -> mapsFragment.drawBusesOnRoute(i.routeNumber)).start();
                new Thread(() -> mapsFragment.drawBusRoute(i.routeNumber, i.color, true, true)).start();
                break;
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
        DirectionsFragment directionsFragment = (DirectionsFragment) getSupportFragmentManager().findFragmentByTag("f1");
        MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentByTag("f2");

        // If not the main search bar, enter for directions
        if (whichSearchBar != MAIN_SEARCH_BAR) {
            // Set the position if current pos is tapped
            if (position == 0) {
                /*
                 * Get the best and most recent location of the device, which may be null in rare
                 * cases when a location is not available.
                 */
                try {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
                            Location lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                LatLng curLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                recentSearchesAdapter.getItem(position).position = curLocation;

                                // Always attempt to enter directions mode when possible
                                enterDirectionsMode(recentSearchesAdapter.getItem(position));
                            }
                        });
                    } else {
                        PermissionX.init(this)
                                .permissions(Manifest.permission.ACCESS_FINE_LOCATION)
                                .request((allGranted, grantedList, deniedList) -> {
                                    if (allGranted) {
                                        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
                                            Location lastKnownLocation = task.getResult();
                                            if (lastKnownLocation != null) {

                                                // Set the current position
                                                LatLng curLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                                recentSearchesAdapter.getItem(position).position = curLocation;

                                                // Update maps and directions
                                                mapsFragment.updateLocationUI();
                                                mapsFragment.getDeviceLocation();
                                                directionsFragment.updateLocationUI();
                                                directionsFragment.getDeviceLocation();

                                                // Always attempt to enter directions mode when possible
                                                enterDirectionsMode(recentSearchesAdapter.getItem(position));
                                            }
                                        });
                                    }
                                });
                    }
                } catch (SecurityException e) {
                    Log.e("Exception: %s", e.getMessage(), e);
                }

                // Otherwise just add marker
            } else {
                MarkerOptions selectedResult = new MarkerOptions();
                selectedResult.position(recentSearch.position);
                selectedResult.title(recentSearch.title);
                directionsFragment.mMap.addMarker(selectedResult);
                // Always attempt to enter directions mode when possible
                enterDirectionsMode(recentSearchesAdapter.getItem(position));
            }
        } else {

            // Zooms to position if current location is tapped
            if (position == 0) {
                /*
                 * Get the best and most recent location of the device, which may be null in rare
                 * cases when a location is not available.
                 */
                try {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
                            Location lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                LatLng curLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                mapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curLocation, 14.0f));
                            }
                        });

                        // If permission doesn't exist
                    } else {
                        PermissionX.init(this)
                                .permissions(Manifest.permission.ACCESS_FINE_LOCATION)
                                .request((allGranted, grantedList, deniedList) -> {
                                    if (allGranted) {
                                        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
                                            Location lastKnownLocation = task.getResult();
                                            if (lastKnownLocation != null) {
                                                LatLng curLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                                mapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curLocation, 14.0f));

                                                // Update maps and directions
                                                mapsFragment.updateLocationUI();
                                                mapsFragment.getDeviceLocation();
                                                directionsFragment.updateLocationUI();
                                                directionsFragment.getDeviceLocation();
                                            }
                                        });
                                    }
                                });
                    }
                } catch (SecurityException e) {
                    Log.e("Exception: %s", e.getMessage(), e);
                }

            } else {
                // Repeat the same thing as the other on clicks if a recent is tapped from the main search bar
                MarkerOptions selectedResult = new MarkerOptions();
                selectedResult.position(recentSearch.position);
                selectedResult.title(recentSearch.title);
                mapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(recentSearchesAdapter.getItem(position).position, 18.0f));
                mapsFragment.mMap.addMarker(selectedResult);
                mapsFragment.mMap.setOnMarkerClickListener(marker -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Directions")
                            .setMessage("Would you like to find directions to " + marker.getTitle())
                            .setPositiveButton("Yes", (dialogInterface, i) -> {
                                directionsFragment.mMap.addMarker(selectedResult);
                                enterDirectionsMode(new ListItem(marker.getTitle(), null, 0, SearchTag.RESULT, marker.getPosition()));
                            })
                            .setIcon(R.drawable.directions)
                            .setCancelable(true)
                            .setNegativeButton("No", null)
                            .show();
                    return false;
                });
            }
        }

        clearFocusOnSearch();
        addRecentSearches(RECENTS_ADAPTER, position);

    }

    public void enterDirectionsMode(ListItem destItem) {

        // Set the visibility of the default searchbar to "gone"
        materialSearchBar.setVisibility(View.GONE);

        // Create the directions on the directions fragment
        DirectionsFragment directionsFragment = (DirectionsFragment) getSupportFragmentManager().findFragmentByTag("f1");
        directionsFragment.createDirections(destItem);

        // Show the directions fragment
        viewPager.setCurrentItem(1);

        // Set the bottom bar selection and visibility
        bottomBar.getMenu().select(R.id.directions);
    }

    public void exitDirectionsMode() {

        // Show the directions fragment
        viewPager.setCurrentItem(2);

        // Set the bottom bar selection and visibility
        bottomBar.getMenu().select(R.id.buses);
        bottomBar.setVisibility(View.VISIBLE);
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
        } else if (adapter == RECENTS_ADAPTER && position != 0) {
            recentSearchesTemp.remove(position - 1);
            recentSearchesTemp.addFirst(recentSearchesAdapter.getItem(position));
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
