package com.mrst.aggiemaps;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;

public class GarageFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        new Thread(() -> {
            HashMap testMap = getLiveCount();
            Log.d("GARAGE", testMap.toString());
        }).start();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blank, container, false);
    }

    /*
    Function to web scrape the
     */
    private HashMap<String, Integer> getLiveCount() {

        // Initialize data
        HashMap<String, Integer> garageCounts = new HashMap<>();
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
            garageCounts.put( garageName, garageCount);
        }
        return garageCounts;
    }


}