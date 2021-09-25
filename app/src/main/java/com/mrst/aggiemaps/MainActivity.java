package com.mrst.aggiemaps;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.lapism.search.widget.MaterialSearchBar;
import com.lapism.search.widget.MaterialSearchView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity implements RecyclerViewAdapterRandom.ItemClickListener {

    private MaterialSearchBar materialSearchBar;
    private MaterialSearchView materialSearchView;
    private OkHttpClient client;  // Client to make API requests
    private Thread searchThread;
    private RecyclerViewAdapterRandom recyclerViewAdapterRandom;

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
        materialSearchBar.setElevation(5);
        materialSearchBar.setBackgroundColor(getColor(R.color.background));
        materialSearchBar.setOnClickListener(v -> requestFocusOnSearch());
        materialSearchBar.setNavigationOnClickListener(v -> requestFocusOnSearch());

        // Set SearchView Settings
        ArrayList<SearchResult> l = new ArrayList<>();
        recyclerViewAdapterRandom = new RecyclerViewAdapterRandom(this, l, RecyclerViewAdapterRandom.SearchTag.LIST);
        recyclerViewAdapterRandom.setClickListener(this);
        RecyclerView recyclerRandom = new RecyclerView(this);
        recyclerRandom.setLayoutManager(new LinearLayoutManager(this));
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
                if (charSequence.length() == 0) return true;
                // Query GIS
                searchThread = new Thread(() -> {
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
                            "returnDistinctValues=false&resultOffset=&resultRecordCount=10&" +
                            "queryByDistance=&returnExtentOnly=false&datumTransformation=&" +
                            "parameterValues=&rangeValues=&quantizationParameters=&" +
                            "featureEncoding=esriDefault&f=pjson");
                    try {
                        if (resp != null) {
                            int lSize = l.size();
                            l.clear();
                            //runOnUiThread(() -> recyclerViewAdapterRandom.notifyItemRangeRemoved(0, lSize));
                            JSONObject jsonObject = new JSONObject(resp);
                            JSONArray features = jsonObject.getJSONArray("features");
                            for (int i = 0; i < features.length(); i++) {
                                String bldgName = features.getJSONObject(i).getJSONObject("attributes").getString("BldgName");
                                String address = features.getJSONObject(i).getJSONObject("attributes").getString("Address");
                                double lat = features.getJSONObject(i).getJSONObject("attributes").getDouble("Latitude");
                                double lng = features.getJSONObject(i).getJSONObject("attributes").getDouble("Longitude");
                                l.add(new SearchResult(bldgName, address, 0, null, RecyclerViewAdapterRandom.SearchTag.LIST, new LatLng(lat, lng)));
                            }
                            //runOnUiThread(() -> recyclerViewAdapterRandom.notifyItemRangeInserted(0, features.length()));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(recyclerViewAdapterRandom::notifyDataSetChanged);
                });
                searchThread.start();
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(@NonNull CharSequence charSequence) {
                if (charSequence.length() == 0) return true;
                // Query GIS
                searchThread = new Thread(() -> {
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
                            "returnDistinctValues=false&resultOffset=&resultRecordCount=10&" +
                            "queryByDistance=&returnExtentOnly=false&datumTransformation=&" +
                            "parameterValues=&rangeValues=&quantizationParameters=&" +
                            "featureEncoding=esriDefault&f=pjson");
                    try {
                        if (resp != null) {
                            int lSize = l.size();
                            l.clear();
                            //runOnUiThread(() -> recyclerViewAdapterRandom.notifyItemRangeRemoved(0, lSize));
                            JSONObject jsonObject = new JSONObject(resp);
                            JSONArray features = jsonObject.getJSONArray("features");
                            for (int i = 0; i < features.length(); i++) {
                                String bldgName = features.getJSONObject(i).getJSONObject("attributes").getString("BldgName");
                                String address = features.getJSONObject(i).getJSONObject("attributes").getString("Address");
                                double lat = features.getJSONObject(i).getJSONObject("attributes").getDouble("Latitude");
                                double lng = features.getJSONObject(i).getJSONObject("attributes").getDouble("Longitude");
                                l.add(new SearchResult(bldgName, address, 0, null, RecyclerViewAdapterRandom.SearchTag.LIST, new LatLng(lat, lng)));
                            }
                            //runOnUiThread(() -> recyclerViewAdapterRandom.notifyItemRangeInserted(0, features.length()));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(recyclerViewAdapterRandom::notifyDataSetChanged);
                });
                searchThread.start();
                return true;
            }
        });

        materialSearchView.setOnFocusChangeListener(v -> {

        });

    }

    @Override
    public void onBackPressed() {
        if (materialSearchView.hasFocus()) {
            clearFocusOnSearch();
        }
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

    @Override
    public void onItemClick(View view, int position) {
        MarkerOptions selectedResult = new MarkerOptions();
        selectedResult.flat(true);
        selectedResult.position(recyclerViewAdapterRandom.getItem(position).position);
        selectedResult.title(recyclerViewAdapterRandom.getItem(position).title);
        MapsFragment.mMap.addMarker(selectedResult);
        MapsFragment.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(recyclerViewAdapterRandom.getItem(position).position, 18.0f));
        clearFocusOnSearch();
    }
}

