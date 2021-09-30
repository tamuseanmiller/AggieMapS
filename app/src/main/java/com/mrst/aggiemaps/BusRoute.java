package com.mrst.aggiemaps;

import java.io.Serializable;

enum BusRouteTag {
    FAVORITES,
    ON_CAMPUS,
    OFF_CAMPUS,
    GAME_DAY
}

public class BusRoute implements Serializable {

    public String routeNumber;
    public String routeName;
    public int color;
    public BusRouteTag tag;

    public BusRoute(String routeNumber, String routeName, int color, BusRouteTag tag) {
        this.routeNumber = routeNumber;
        this.routeName = routeName;
        this.color = color;
        this.tag = tag;
    }

}
