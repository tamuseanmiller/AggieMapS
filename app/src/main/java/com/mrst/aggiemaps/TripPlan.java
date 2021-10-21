package com.mrst.aggiemaps;

import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

// TODO: change maneuverType to enum in the future
class Feature {
    private double length;
    private double time;
    private String text;
    private long eta;
    private Drawable maneuverType;

    public Feature(double length, double time, String text, long eta, Drawable maneuverType) {
        this.length = length;
        this.time = time;
        this.text = text;
        this.eta = eta;
        this.maneuverType = maneuverType;
    }

    public double getLength() {
        return length;
    }

    public double getTime() {
        return time;
    }

    public String getText() {
        return text;
    }

    public long getEta() {
        return eta;
    }

    public Drawable getManeuverType() {
        return maneuverType;
    }
}

public class TripPlan {
    private ArrayList<LatLng> geometry;
    private ArrayList<Feature> features;
    private double totalLength;
    private double totalTime;
    private double totalDriveTime;

    public TripPlan(ArrayList<LatLng> geometry, ArrayList<Feature> features, double totalLength, double totalTime, double totalDriveTime) {
        this.geometry = geometry;
        this.features = features;
        this.totalLength = totalLength;
        this.totalTime = totalTime;
        this.totalDriveTime = totalDriveTime;
    }

    public ArrayList<LatLng> getGeometry() {
        return geometry;
    }

    public ArrayList<Feature> getFeatures() {
        return features;
    }

    public double getTotalLength() {
        return totalLength;
    }

    public double getTotalTime() {
        return totalTime;
    }

    public double getTotalDriveTime() {
        return totalDriveTime;
    }
}
