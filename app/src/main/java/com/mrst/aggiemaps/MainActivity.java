package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcelable;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
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

import github.com.st235.lib_expandablebottombar.ExpandableBottomBar;
import eu.okatrych.rightsheet.RightSheetBehavior;
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
    private ArrayList<ListItem> gisListItems;
    private ArrayList<ListItem> googleListItems;
    private PlacesClient placesClient;
    private RecyclerView gisSearchRecycler;
    private RecyclerView googleSearchRecycler;
    private RecyclerView busRoutesSearchRecycler;
    final MapsFragment mapsFragment = new MapsFragment();
    final DirectionsFragment directionsFragment = new DirectionsFragment();
    final BlankFragment blankFragment = new BlankFragment();
    final SettingsFragment settingsFragment = new SettingsFragment();
    Fragment active = mapsFragment;
    private ArrayList<ListItem> busRoutesListItems;
    private MaterialSearchBar srcSearchBar;
    private MaterialSearchBar destSearchBar;
    private AppBarLayout defaultSearchBar;
    private ExpandableBottomBar bottomBar;
    public int whichSearchBar;

    public static final int MAIN_SEARCH_BAR = 1;
    public static final int SRC_SEARCH_BAR = 2;
    public static final int DEST_SEARCH_BAR = 3;


//    private void haveNetworkConnection() {
//        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
//        System.out.println(activeNetwork);
//        if (activeNetwork != null) {
//            // connected to the internet
//            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
//                // connected to wifi
//            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
//                // connected to mobile data
//            }
//        } else {
//            // not connected to the internet
//            Snackbar snackbar = Snackbar.make(findViewById(R.id.cl_main),"Your network is unavailable. Check your data or wifi connection.",Snackbar.LENGTH_INDEFINITE);
//            snackbar.setAction("RETRY", new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    haveNetworkConnection();
//                }
//            });
//            snackbar.show();
//        }
//    }


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

    private Drawable parseManeuverType(String maneuverType) {
        switch (maneuverType) {
            case "esriDMTStop":
                return ContextCompat.getDrawable(this, R.drawable.close_octagon);
            case "esriDMTStraight":
                return ContextCompat.getDrawable(this, R.drawable.arrow_up);
            case "esriDMTBearLeft":
            case "esriDMTRampLeft":
                return ContextCompat.getDrawable(this, R.drawable.arrow_top_left);
            case "esriDMTBearRight":
            case "esriDMTRampRight":
                return ContextCompat.getDrawable(this, R.drawable.arrow_top_right);
            case "esriDMTTurnLeft":
                return ContextCompat.getDrawable(this, R.drawable.arrow_left_top);
            case "esriDMTTurnRight":
                return ContextCompat.getDrawable(this, R.drawable.arrow_right_top);
            case "esriDMTSharpLeft":
                return ContextCompat.getDrawable(this, R.drawable.arrow_left);
            case "esriDMTSharpRight":
                return ContextCompat.getDrawable(this, R.drawable.arrow_right);
            case "esriDMTUTurn":
                return ContextCompat.getDrawable(this, R.drawable.arrow_u_down_left);
            case "esriDMTFerry":
            case "esriDMTEndOfFerry":
                return ContextCompat.getDrawable(this, R.drawable.ferry);
            case "esriDMTRoundabout":
                return ContextCompat.getDrawable(this, R.drawable.rotate_left);
            case "esriDMTHighwayMerge":
                return ContextCompat.getDrawable(this, R.drawable.call_merge);
            case "esriDMTHighwayExit":
            case "esriDMTForkCenter":
            case "esriDMTForkLeft":
            case "esriDMTForkRight":
                return ContextCompat.getDrawable(this, R.drawable.call_split);
            case "esriDMTHighwayChange":
                return ContextCompat.getDrawable(this, R.drawable.source_fork);
            case "esriDMTDepart":
                return ContextCompat.getDrawable(this, R.drawable.car);
            case "esriDMTTripItem":
                return ContextCompat.getDrawable(this, R.drawable.sign_direction);
            default:
                return ContextCompat.getDrawable(this, R.drawable.nuke);
        }
    }

    private int convertDpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void clearFocusOnSearch() {
        materialSearchView.clearFocus();
        materialSearchView.setVisibility(View.GONE);
        showSystemUI();

        if (whichSearchBar > 1) {
            directionsFragment.clearFocusOnSearch();
        } else {
            materialSearchBar.setVisibility(View.VISIBLE);
        }
    }

    public void requestFocusOnSearch(int searchBar) {
        materialSearchView.requestFocus();
        materialSearchView.setVisibility(View.VISIBLE);
        materialSearchBar.setVisibility(View.GONE);
        hideSystemUI();
        whichSearchBar = searchBar;
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
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        client = new OkHttpClient();  // Create OkHttpClient to be used in API request
        haveNetworkConnection();

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
        NestedScrollView nSV = new NestedScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        gisSearchAdapter = new GISSearchAdapter(this, gisListItems);
        gisSearchAdapter.setClickListener(this);
        googleSearchAdapter = new GoogleSearchAdapter(this, googleListItems);
        googleSearchAdapter.setClickListener(this);
        busRoutesSearchAdapter = new BusRoutesSearchAdapter(this, busRoutesListItems);
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
        RelativeLayout curLocationRow = (RelativeLayout) getLayoutInflater().inflate(R.layout.list_row, null);
        TextView currTitleText = curLocationRow.findViewById(R.id.title_text);
        currTitleText.setText("Current location");
        currTitleText.setOnClickListener(v -> {
            mapsFragment.getDeviceLocation();
            clearFocusOnSearch();
        });
        ll.addView(curLocationRow);
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
        fm.beginTransaction().add(R.id.ll_main, settingsFragment, "3").hide(settingsFragment).commit();
        fm.beginTransaction().add(R.id.ll_main, blankFragment, "0").hide(blankFragment).commit();
        fm.beginTransaction().add(R.id.ll_main, directionsFragment, "1").hide(directionsFragment).commit();
        fm.beginTransaction().add(R.id.ll_main, mapsFragment, "2").commit();

        // Create bottom bar
        bottomBar = findViewById(R.id.bottom_bar);
        bottomBar.getMenu().select(R.id.item0);

        // Get bus routes on tap
        bottomBar.setOnItemReselectedListener((i, j, k) -> {
            if (j.getId() == R.id.item0) {
                if (active == settingsFragment) {
                    materialSearchBar.setVisibility(View.VISIBLE);
                    fm.beginTransaction().hide(active).show(mapsFragment).commit();
                    active = mapsFragment;
                } else {
                    mapsFragment.standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                }
            }
            return null;
        });

        // Switch between fragments on tap
        bottomBar.setOnItemSelectedListener((i, j, k) -> {
            if (j.getId() == R.id.blank) {
                materialSearchBar.setVisibility(View.GONE);
                fm.beginTransaction().hide(active).show(directionsFragment).commit();
                active = directionsFragment;
            } else if (j.getId() == R.id.maps) {
                materialSearchBar.setVisibility(View.GONE);
                fm.beginTransaction().hide(active).show(blankFragment).commit();
                active = blankFragment;
            } else if (j.getId() == R.id.item0) {
                materialSearchBar.setVisibility(View.VISIBLE);
                fm.beginTransaction().hide(active).show(mapsFragment).commit();
                active = mapsFragment;
            }
            return null;
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
                getSupportFragmentManager().beginTransaction().hide(active).show(settingsFragment).commit();
                active = settingsFragment;
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
            tempList.add(new ListItem("Bus Routes", "", 0, null, SearchTag.CATEGORY, null));

            // Loop through every bus route, check to see if the
            // name or number contains the given char sequence
            MapsFragment mapsFragment = (MapsFragment) this.mapsFragment;
            if (mapsFragment != null) {
                for (int i = 1; i < mapsFragment.busRoutes.size(); i++) {
                    String routeNumber = mapsFragment.busRoutes.get(i).routeNumber.toLowerCase();
                    String routeName = mapsFragment.busRoutes.get(i).routeName.toLowerCase();
                    if ((routeNumber.contains(charSequence) || routeName.contains(charSequence)) && !routeNumber.equals("all"))
                        tempList.add(new ListItem(mapsFragment.busRoutes.get(i).routeNumber, mapsFragment.busRoutes.get(i).routeName, 0, null, SearchTag.RESULT, null));
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
                tempList.add(new ListItem("Google Maps", "", 0, null, SearchTag.CATEGORY, null));
            }
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                Log.i(TAG, prediction.getPlaceId());
                Log.i(TAG, prediction.getPrimaryText(null).toString());

                // Specify the fields to return.
                final List<Place.Field> placeFields = Collections.singletonList(Place.Field.LAT_LNG);

                // Construct a request object, passing the place ID and fields array.
                final FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(prediction.getPlaceId(), placeFields);

                placesClient.fetchPlace(placeRequest).addOnSuccessListener((placeResponse) -> {
                    tempList.add(new ListItem(prediction.getPrimaryText(null).toString(), prediction.getFullText(null).toString(), 0, null, SearchTag.RESULT, placeResponse.getPlace().getLatLng()));
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
                            tempList.add(new ListItem("On Campus", "", 0, null, SearchTag.CATEGORY, null));
                        String bldgName = features.getJSONObject(i).getJSONObject("attributes").getString("BldgName");
                        String address = features.getJSONObject(i).getJSONObject("attributes").getString("Address");
                        double lat = features.getJSONObject(i).getJSONObject("attributes").getDouble("Latitude");
                        double lng = features.getJSONObject(i).getJSONObject("attributes").getDouble("Longitude");
                        if (address.equals("null"))
                            address = lat + ", " + lng; // If no address, use lat/lng instead
                        String finalAddress = address;
                        tempList.add(new ListItem(bldgName, finalAddress, 0, null, SearchTag.RESULT, new LatLng(lat, lng)));
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
        if (active == settingsFragment) {
            getSupportFragmentManager().beginTransaction().hide(active).show(mapsFragment).commit();
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
        MarkerOptions selectedResult = new MarkerOptions();
        selectedResult.position(gisSearchAdapter.getItem(position).position);
        selectedResult.title(gisSearchAdapter.getItem(position).title);
        clearFocusOnSearch();

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
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            directionsFragment.mMap.addMarker(selectedResult);
                            enterDirectionsMode(new ListItem(marker.getTitle(), null, 0, null, SearchTag.RESULT, marker.getPosition()));
                        })
                        .setIcon(directions)
                        .setCancelable(true)
                        .setNegativeButton("No", null)
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
        MarkerOptions selectedResult = new MarkerOptions();
        selectedResult.position(googleSearchAdapter.getItem(position).position);
        selectedResult.title(googleSearchAdapter.getItem(position).title);
        clearFocusOnSearch();

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
                            enterDirectionsMode(new ListItem(marker.getTitle(), null, 0, null, SearchTag.RESULT, marker.getPosition()));
                        })
                        .setIcon(R.drawable.directions)
                        .setCancelable(true)
                        .show();
                return false;
            });
        }
    }

    public void switchTheme(String newValue) {
        switch(newValue) {
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
        mapsFragment.mMap.clear();
        for (BusRoute i : mapsFragment.busRoutes) {
            if (i.routeNumber.equals(busRoutesSearchAdapter.getItem(position).title)) {
                MapsFragment.currentRouteNo = i.routeNumber;
                new Thread(() -> mapsFragment.drawBusesOnRoute(i.routeNumber)).start();
                new Thread(() -> mapsFragment.drawBusRoute(i.routeNumber, i.color, true)).start();
                break;
            }
        }
        clearFocusOnSearch();
    }

    /*
     * TODO: Method to enter the directions screen from the main activity
     */
    public void enterDirectionsMode(ListItem destItem) {

        // Set the visibility of the default searchbar to "gone"
        materialSearchBar.setVisibility(View.GONE);

        // Create the directions on the directions fragment
        directionsFragment.createDirections(destItem);

        // Show the directions fragment
        getSupportFragmentManager().beginTransaction().hide(active).show(directionsFragment).commit();
        active = directionsFragment;

        // Set the bottom bar selection and visibility
        bottomBar.getMenu().select(R.id.blank);
        bottomBar.setVisibility(View.GONE);

    }

    /*
     * TODO: Method to exit the directions screen from the main activity
     */
    public void exitDirectionsMode() {

        // Show the directions fragment
        getSupportFragmentManager().beginTransaction().hide(active).show(mapsFragment).commit();
        active = mapsFragment;

        // Set the bottom bar selection and visibility
        bottomBar.getMenu().select(R.id.item0);
        bottomBar.setVisibility(View.VISIBLE);
    }

}

