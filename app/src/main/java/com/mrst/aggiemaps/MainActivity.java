package com.mrst.aggiemaps;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.appbar.AppBarLayout;
import com.lapism.search.widget.MaterialSearchBar;
import com.lapism.search.widget.MaterialSearchView;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MaterialSearchBar materialSearchBar;
    private MaterialSearchView materialSearchView;
    private OkHttpClient client;  // Client to make API requests

    private void clearFocusOnSearch() {
        materialSearchView.clearFocus();
        materialSearchView.setVisibility(View.GONE);
        materialSearchBar.setVisibility(View.VISIBLE);
        showSystemUI();
    }

    private void requestFocusOnSearch() {
        ScriptGroup.Binding binding;
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

            return body.string(); // Return the response as a string

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
        PlacesClient placesClient = Places.createClient(this);

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
        materialSearchBar.setBackgroundColor(getColor(R.color.background));
        materialSearchBar.setOnClickListener(v -> {
            requestFocusOnSearch();
        });
        materialSearchBar.setNavigationOnClickListener(v -> {
            requestFocusOnSearch();
        });

        // Set SearchView Settings
        ArrayList<SearchResult> l = new ArrayList<>();
        RecyclerViewAdapterRandom recyclerViewAdapterRandom = new RecyclerViewAdapterRandom(this, l, RecyclerViewAdapterRandom.SearchTag.LIST);
        RecyclerView recyclerRandom = new RecyclerView(this);
        recyclerRandom.setLayoutManager(new LinearLayoutManager(this));
        int val = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        recyclerRandom.setPadding(val, 0, val, 0);
        recyclerRandom.setAdapter(recyclerViewAdapterRandom);
        materialSearchView.addView(recyclerRandom);
        Drawable navigationIcon = ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24);
        navigationIcon.setTintList(ColorStateList.valueOf(getColor(R.color.foreground)));
        materialSearchView.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24));
        materialSearchView.setVisibility(View.GONE);
        materialSearchView.setHint("Try Building Numbers/Names");
        materialSearchView.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        // Set OnClick Listeners
        materialSearchView.setNavigationOnClickListener(v -> clearFocusOnSearch());

        materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(@NonNull CharSequence charSequence) {
                // Query GIS
                new Thread(() -> {
                    String resp = getApiCall("https://gis.tamu.edu/arcgis/rest/services/FCOR/" +
                            "TAMU_BaseMap/MapServer/1/query?where=Abbrev+LIKE+UPPER%28%27%25" +
                            charSequence.toString() + "%25%27%29+OR+UPPER%28BldgName%29" +
                            "+LIKE+UPPER%28%27%25" + charSequence.toString() + "%25%27%29&" +
                            "text=&objectIds=&time=&geometry=&geometryType=esriGeometryEnvelope&" +
                            "inSR=&spatialRel=esriSpatialRelIntersects&relationParam=&outFields=*&" +
                            "returnGeometry=true&returnTrueCurves=false&maxAllowableOffset=&" +
                            "geometryPrecision=&outSR=4326&having=&returnIdsOnly=false&" +
                            "returnCountOnly=false&orderByFields=&groupByFieldsForStatistics=&" +
                            "outStatistics=&returnZ=false&returnM=false&gdbVersion=&" +
                            "historicMoment=&returnDistinctValues=false&resultOffset=&" +
                            "resultRecordCount=&queryByDistance=&returnExtentOnly=false&" +
                            "datumTransformation=&parameterValues=&rangeValues=&" +
                            "quantizationParameters=&featureEncoding=esriDefault&f=pjson");
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        JSONArray features = jsonObject.getJSONArray("features");
                        l.clear();
                        for (int i = 0; i < features.length(); i++) {
                            String bldgName = features.getJSONObject(i).getJSONObject("attributes").getString("BldgName");
                            String address = features.getJSONObject(i).getJSONObject("attributes").getString("Address");
                            l.add(new SearchResult(bldgName, address, 0, null, RecyclerViewAdapterRandom.SearchTag.LIST));
                        }
                        runOnUiThread(() -> recyclerViewAdapterRandom.notifyDataSetChanged());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).start();
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(@NonNull CharSequence charSequence) {
                // Query GIS
                new Thread(() -> {
                    String resp = getApiCall("https://gis.tamu.edu/arcgis/rest/services/FCOR/" +
                            "TAMU_BaseMap/MapServer/1/query?where=Abbrev+LIKE+UPPER%28%27%25" +
                            charSequence.toString() + "%25%27%29+OR+UPPER%28BldgName%29" +
                            "+LIKE+UPPER%28%27%25" + charSequence.toString() + "%25%27%29&" +
                            "text=&objectIds=&time=&geometry=&geometryType=esriGeometryEnvelope&" +
                            "inSR=&spatialRel=esriSpatialRelIntersects&relationParam=&outFields=*&" +
                            "returnGeometry=true&returnTrueCurves=false&maxAllowableOffset=&" +
                            "geometryPrecision=&outSR=4326&having=&returnIdsOnly=false&" +
                            "returnCountOnly=false&orderByFields=&groupByFieldsForStatistics=&" +
                            "outStatistics=&returnZ=false&returnM=false&gdbVersion=&" +
                            "historicMoment=&returnDistinctValues=false&resultOffset=&" +
                            "resultRecordCount=&queryByDistance=&returnExtentOnly=false&" +
                            "datumTransformation=&parameterValues=&rangeValues=&" +
                            "quantizationParameters=&featureEncoding=esriDefault&f=pjson");
                    try {
                        JSONObject jsonObject = new JSONObject(resp);
                        JSONArray features = jsonObject.getJSONArray("features");
                        l.clear();
                        for (int i = 0; i < features.length(); i++) {
                            String bldgName = features.getJSONObject(i).getJSONObject("attributes").getString("BldgName");
                            l.add(new SearchResult(bldgName, null, 0, null, RecyclerViewAdapterRandom.SearchTag.LIST));
                        }
                        runOnUiThread(() -> recyclerViewAdapterRandom.notifyDataSetChanged());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).start();
                return true;
            }
        });

        materialSearchView.setOnFocusChangeListener(v -> {

        });


    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

}

