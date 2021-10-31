package com.mrst.aggiemaps;

import androidx.annotation.DrawableRes;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

enum FeatureType {
    LANDMARK,
    NOT_LANDMARK
}

class Feature {
    private double lengthMiles;
    private double timeMins;
    private String text;
    private long eta;
    private @DrawableRes
    int maneuverType;
    private FeatureType type;

    public Feature(double length, double time, String text, long eta, @DrawableRes int maneuverType) {
        this.lengthMiles = length;
        this.timeMins = time;
        this.text = text;
        this.eta = eta;
        this.maneuverType = maneuverType;
        this.type = FeatureType.NOT_LANDMARK;
    }

    public Feature(FeatureType type, String text) {
        this.type = type;
        this.text = text;
    }

    public double getLengthMiles() {
        return lengthMiles;
    }

    public double getTimeMins() {
        return timeMins;
    }

    public String getText() {
        return text;
    }

    public long getEta() {
        return eta;
    }

    public @DrawableRes
    int getManeuverType() {
        return maneuverType;
    }

    public FeatureType getType() {
        return type;
    }

    public void setText(String text) {
        this.text = text;
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
