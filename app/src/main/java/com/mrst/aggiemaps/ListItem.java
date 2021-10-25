package com.mrst.aggiemaps;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListItem implements Serializable {
    public String title;
    public String subtitle;
    public int color;
    public @DrawableRes int direction;
    public MainActivity.SearchTag tag;
    public LatLng position;

    public ListItem(String title, String subtitle, int color, @DrawableRes int direction, MainActivity.SearchTag tag, LatLng position) {
        this.title = title;
        this.subtitle = subtitle;
        this.color = color;
        this.direction = direction;
        this.tag = tag;
        this.position = position;
    }

    public ListItem(String title, String subtitle, int color, MainActivity.SearchTag tag, LatLng position) {
        this.title = title;
        this.subtitle = subtitle;
        this.color = color;
        this.tag = tag;
        this.position = position;
    }

    public ListItem(String title, String subtitle, int color, MainActivity.SearchTag tag) {
        this.title = title;
        this.subtitle = subtitle;
        this.color = color;
        this.tag = tag;
    }

    public void setDirection(@DrawableRes int direction) {
        this.direction = direction;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
