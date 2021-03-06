package com.mrst.aggiemaps;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.rubensousa.decorator.LinearMarginDecoration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GarageFragment extends Fragment {

    private SwipeRefreshLayout swlRefresh;
    private RecyclerView garagesRecycler;
    private View gView;
    private Runnable runnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        gView = inflater.inflate(R.layout.fragment_garages, container, false);
        Toolbar t = gView.findViewById(R.id.tb);
        t.setBackgroundColor(Color.TRANSPARENT);
        t.setElevation(0);
        Drawable garage = ContextCompat.getDrawable(requireActivity(), R.drawable.garage);
        if (garage != null) {
            garage.setTint(ContextCompat.getColor(requireActivity(), R.color.foreground));
            t.setNavigationIcon(garage);
        }

        // Initialize layout elements
        swlRefresh = gView.findViewById(R.id.swl_garages);

        // recyclerview for the rows
        garagesRecycler = gView.findViewById(R.id.garages_recycler);

        // Converts 16 dip into its equivalent px
        float dip = 16f;
        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );

        // Initialize recycler
        garagesRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        garagesRecycler.addItemDecoration(new LinearMarginDecoration(px, px, px, px, RecyclerView.VERTICAL, false, false, false, null));
        garagesRecycler.suppressLayout(true);
        swlRefresh.setRefreshing(true);

        //Rerun updating GarageUI every 5 minutes
        Handler handler = new Handler();
        handler.post(runnable = () -> {
            handler.postDelayed(runnable, 300000);
            new Thread(this::updateGarageUI).start();
        });

        // Set onRefresh listener
        swlRefresh.setOnRefreshListener(() -> {
            swlRefresh.setRefreshing(true);
            new Thread(this::updateGarageUI).start();
        });

        return gView;
    }

    public void updateGarageUI() {
        // Get the live counts from the function
        HashMap<String, Integer> garageHashMap = getLiveCount();

        // Null check
        if (garageHashMap == null) return;

        // Convert the hashmap to lists
        List<Integer> values = new ArrayList<>(garageHashMap.values());
        List<String> keys = new ArrayList<>(garageHashMap.keySet());

        // Add the keys and values as pairs to the data list
        List<Pair<String, Integer>> data = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            data.add(new Pair<>(keys.get(i), values.get(i)));
        }

        // Use the data list to set the content of each row
        GaragesAdapter adapter = new GaragesAdapter(data);
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> garagesRecycler.setAdapter(adapter));
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        requireActivity().runOnUiThread(() -> swlRefresh.setRefreshing(false));
    }

    /*
    Function to web scrape the
     */
    public HashMap<String, Integer> getLiveCount() {
        HashMap<String, Integer> garageCounts = new HashMap<>();
        // Initialize data
        String garageUrl = "https://transport.tamu.edu/parking/realtimestatus.aspx/";
        Document doc = null;
        try {
            doc = Jsoup.connect(garageUrl).get();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // Parse for list elements in html and iterate through
        Elements garageData = doc.select("tr");
        for (Element garage : garageData) {

            // Parse for name and count in list element
            String garageName = garage.select(".small").text().trim();
            int garageCount = -1;
            try {
                garageCount = Integer.parseInt(garage.select(".badge").text().trim());
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            // Check to make sure both name and count classes were found in list element
            if (garageName.equals("")) {
                Log.d("GARAGE", "Element is missing name. Move to next.");
                continue;
            }
            if (garageCount < 0) {
                garageCount = 0;
            }
            garageCounts.put(garageName, garageCount);
        }

        return garageCounts;
    }


}