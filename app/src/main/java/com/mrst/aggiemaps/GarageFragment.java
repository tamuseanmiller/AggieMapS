package com.mrst.aggiemaps;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class GarageFragment extends Fragment {

    private LinearProgressIndicator progressIndicator;
    private Button refreshBtn;
    private RecyclerView garagesRecycler;
    private View gView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        gView = inflater.inflate(R.layout.fragment_garages, container, false);

        // Initialize layout elements
        progressIndicator = gView.findViewById(R.id.garages_progress);
        refreshBtn = gView.findViewById(R.id.garages_refreshBtn);
        refreshBtn.setOnClickListener(view -> { });
        progressIndicator.setVisibility(View.VISIBLE);
        new Thread(() -> {
            // Get the live counts from the function
            HashMap garageHashMap = getLiveCount();
            // Converr the hashmap to lists
            List<Integer> values = new ArrayList<Integer>(garageHashMap.values());
            List<String> keys = new ArrayList<String>(garageHashMap.keySet());

            // Add the keys and values as pairs to the data list
            List<Pair<String, Integer>> data = new ArrayList<>();
            for(int i=0; i<values.size(); i++){
                data.add(new Pair(keys.get(i), values.get(i)));
            }

            // recyclerview for the rows
            garagesRecycler = gView.findViewById(R.id.garages_recycler);
            // Use the data list to set the content of each row
            GaragesAdapter adapter = new GaragesAdapter(data);
            garagesRecycler.setAdapter(adapter);
            garagesRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        }).start();
        progressIndicator.setVisibility(View.INVISIBLE);

        return gView;
    }

    /*
    Function to web scrape the
     */
    private HashMap<String, Integer> getLiveCount() {
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
            }

            // Check to make sure both name and count classes were found in list element
            if (garageName.equals("") || garageCount == -1) {
                Log.d("GARAGE", "Element is missing name or count. Move to next.");
                continue;
            }
            garageCounts.put(garageName, garageCount);
        }

        return garageCounts;
    }


}