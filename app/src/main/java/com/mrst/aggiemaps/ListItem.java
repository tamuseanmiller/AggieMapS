package com.mrst.aggiemaps;

import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.LatLng;

public class ListItem {
    public String title;
    public String subtitle;
    public int color;
    public Drawable direction;
    public MainActivity.SearchTag tag;
    public LatLng position;

    public ListItem(String title, String subtitle, int color, Drawable direction, MainActivity.SearchTag tag, LatLng position) {
        this.title = title;
        this.subtitle = subtitle;
        this.color = color;
        this.direction = direction;
        this.tag = tag;
        this.position = position;
    }
}
