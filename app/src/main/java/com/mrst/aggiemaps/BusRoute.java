package com.mrst.aggiemaps;

enum BusRouteTag {
    FAVORITES,
    ON_CAMPUS,
    OFF_CAMPUS,
    GAME_DAY
}

public class BusRoute {

    public String routeNumber;
    public String routeName;
    public int color;

    public BusRoute(String routeNumber, String routeName, int color) {
        this.routeNumber = routeNumber;
        this.routeName = routeName;
        this.color = color;
    }

}
