package com.mrst.aggiemaps;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class POIPickerModalSheet extends BottomSheetDialogFragment implements MarkerPickerAdapter.ItemClickListener {

    public static final String POI = "POIPickerModalSheet";
    private List<MarkerStruct> markers;
    private MarkerPickerAdapter markerPickerAdapter;

    @Override
    public void onItemClick(View view, int position) {
        markers.get(position).selected = !markers.get(position).selected;
        markerPickerAdapter.notifyItemChanged(position);

        // Add markers
        MapsFragment mapsFragment = (MapsFragment) requireActivity().getSupportFragmentManager().findFragmentByTag("f2");
        if (mapsFragment != null) {
            if (markers.get(position).selected) {
                switch (markers.get(position).text) {
                    case "Points of Interest":
                        mapsFragment.getPOIs();
                        break;
                    case "Restrooms":
                        mapsFragment.getRestrooms();
                        break;
                    case "Emergency Phones":
                        mapsFragment.getEPhones();
                        break;
                    case "Parking":
                        mapsFragment.getParking();
                        break;
                }
            } else {
                switch (markers.get(position).text) {
                    case "Points of Interest":
                        mapsFragment.poiVisible = false;
                        mapsFragment.markerCollectionPOIs.clear();
                        break;
                    case "Restrooms":
                        mapsFragment.restroomsVisible = false;
                        mapsFragment.markerCollectionRestrooms.clear();
                        break;
                    case "Emergency Phones":
                        mapsFragment.ePhonesVisible = false;
                        mapsFragment.markerCollectionEPhones.clear();
                        break;
                    case "Parking":
                        mapsFragment.kiosksVisible = false;
                        mapsFragment.markerCollectionParking.clear();
                        break;
                }
            }
        }
    }

    public static class MarkerStruct {
        Drawable drawable;
        String text;
        boolean selected;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.sheet_poi_picker , null);
        super.onCreateView(inflater, container, savedInstanceState);

        MarkerStruct s0 = new MarkerStruct();
        s0.drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.poi_illustration);
        s0.text = "Points of Interest";
        s0.selected = false;
        MarkerStruct s1 = new MarkerStruct();
        s1.drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.bathroom_illustration);
        s1.text = "Restrooms";
        s1.selected = false;
        MarkerStruct s2 = new MarkerStruct();
        s2.drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.emergency_illustration);
        s2.text = "Emergency Phones";
        s2.selected = false;
        MarkerStruct s3 = new MarkerStruct();
        s3.drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.parking_illustration);
        s3.text = "Parking";
        s3.selected = false;

        // Set selected
        MapsFragment mapsFragment = (MapsFragment) requireActivity().getSupportFragmentManager().findFragmentByTag("f2");
        if (mapsFragment != null) {
            s0.selected = mapsFragment.poiVisible;
            s1.selected = mapsFragment.restroomsVisible;
            s2.selected = mapsFragment.ePhonesVisible;
            s3.selected = mapsFragment.kiosksVisible;
        }

        markers = new ArrayList<>();
        markers.add(s0);
        markers.add(s1);
        markers.add(s2);
        markers.add(s3);

        markerPickerAdapter = new MarkerPickerAdapter(getActivity(), markers);
        markerPickerAdapter.setClickListener(this);
        RecyclerView markersRecycler = mView.findViewById(R.id.markers_recycler);
        markersRecycler.setLayoutManager(new GridLayoutManager(getActivity(), 4, GridLayoutManager.VERTICAL, false));
        markersRecycler.setAdapter(markerPickerAdapter);

        return mView;
    }
}
