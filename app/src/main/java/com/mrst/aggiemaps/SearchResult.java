package com.mrst.aggiemaps;

import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.LatLng;

public class SearchResult {
    public String title;
    public String subtitle;
    public int color;
    public Drawable direction;
    public RecyclerViewAdapterRandom.SearchTag tag;
    public LatLng position;

    public SearchResult(String title, String subtitle, int color, Drawable direction, RecyclerViewAdapterRandom.SearchTag tag, LatLng position) {
        this.title = title;
        this.subtitle = subtitle;
        this.color = color;
        this.direction = direction;
        this.tag = tag;
        this.position = position;
    }
}
