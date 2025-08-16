package com.example.mymatauapp;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all the matatu route data for the application.
 * This file contains real-world coordinates for the Roysambu to Nairobi route.
 */
public class MatatuRouteManager {

    private final Map<String, List<LatLng>> allRoutes;
    private final List<LatLng> allStops;
    private static final double ROUTE_TOLERANCE = 100.0; // 100 meters tolerance for matching stops

    public MatatuRouteManager() {
        this.allRoutes = new HashMap<>();
        this.allStops = new ArrayList<>();
        initializeRoutes();
        initializeStops();
    }

    /**
     * Initializes a hard-coded list of popular matatu routes with their coordinates.
     * The Roysambu to Nairobi Town route has been updated with the provided real coordinates.
     */
    private void initializeRoutes() {
        // Detailed Royambu to Nairobi Town (Odeon) route with real coordinates
        List<LatLng> royambuToNairobi = new ArrayList<>();
        royambuToNairobi.add(new LatLng(-1.22039, 23.89101)); // Roysambu
        royambuToNairobi.add(new LatLng(-1.22593, 36.88504)); // Safari Park Hotel
        royambuToNairobi.add(new LatLng(-1.23038, 36.87896)); // Garden City Mall
        royambuToNairobi.add(new LatLng(-1.23456, 36.87397)); // Mountain Mall
        royambuToNairobi.add(new LatLng(-1.24411, 36.86805)); // AllSops
        royambuToNairobi.add(new LatLng(-1.24872, 36.86352)); // Drive In
        royambuToNairobi.add(new LatLng(-1.25338, 36.85902)); // KCA University
        royambuToNairobi.add(new LatLng(-1.26038, 36.84358)); // Muthaiga
        royambuToNairobi.add(new LatLng(-1.26399, 36.83721)); // Pangani
        royambuToNairobi.add(new LatLng(-1.27465, 36.82437)); // Ngara
        royambuToNairobi.add(new LatLng(-1.28329, 36.82475)); // Odeon (Nairobi Town)
        allRoutes.put("Roysambu to Nairobi Town", royambuToNairobi);

        // CBD to Rongai route
        // This route still uses placeholder data.
        List<LatLng> cbdToRongai = new ArrayList<>();
        cbdToRongai.add(new LatLng(-1.2889, 36.8208)); // CBD
        cbdToRongai.add(new LatLng(-1.3000, 36.8150)); // Lang'ata
        cbdToRongai.add(new LatLng(-1.3200, 36.7900)); // Galleria
        cbdToRongai.add(new LatLng(-1.3500, 36.7700)); // Kiserian Road
        cbdToRongai.add(new LatLng(-1.3800, 36.7600)); // Rongai
        allRoutes.put("CBD to Rongai", cbdToRongai);
    }

    /**
     * Initializes a hard-coded list of all matatu stops by collecting them from all routes.
     */
    private void initializeStops() {
        for (List<LatLng> route : allRoutes.values()) {
            for (LatLng stop : route) {
                if (!allStops.contains(stop)) {
                    allStops.add(stop);
                }
            }
        }
    }

    /**
     * Finds the nearest matatu stop to a given location.
     * @param location The current location.
     * @return The LatLng of the nearest stop, or null if no stops are available.
     */
    public LatLng findNearestStop(LatLng location) {
        LatLng nearestStop = null;
        double minDistance = Double.MAX_VALUE;

        for (LatLng stop : allStops) {
            double distance = SphericalUtil.computeDistanceBetween(location, stop);
            if (distance < minDistance) {
                minDistance = distance;
                nearestStop = stop;
            }
        }
        return nearestStop;
    }

    /**
     * Finds and returns the route that contains both the start and end stops.
     * @param start The starting LatLng of the matatu route.
     * @param end The ending LatLng of the matatu route.
     * @return A List of LatLng points representing the route, or null if no common route is found.
     */
    public List<LatLng> findRouteBetween(LatLng start, LatLng end) {
        for (List<LatLng> route : allRoutes.values()) {
            boolean hasStart = false;
            boolean hasEnd = false;

            for (LatLng stop : route) {
                if (SphericalUtil.computeDistanceBetween(start, stop) < ROUTE_TOLERANCE) {
                    hasStart = true;
                }
                if (SphericalUtil.computeDistanceBetween(end, stop) < ROUTE_TOLERANCE) {
                    hasEnd = true;
                }
            }

            if (hasStart && hasEnd) {
                return route;
            }
        }
        return null;
    }

    /**
     * Retrieves the waypoints (intermediate stops) between two given stops on a matatu route.
     * @param start The starting LatLng of the matatu route.
     * @param end The ending LatLng of the matatu route.
     * @return A list of LatLng points representing the waypoints between the start and end,
     * or an empty list if no route or waypoints are found.
     */
    public List<LatLng> getWaypointsBetween(LatLng start, LatLng end) {
        List<LatLng> waypoints = new ArrayList<>();
        List<LatLng> route = findRouteBetween(start, end);

        if (route != null && route.size() > 1) {
            int startIndex = -1;
            int endIndex = -1;

            for (int i = 0; i < route.size(); i++) {
                if (SphericalUtil.computeDistanceBetween(start, route.get(i)) < ROUTE_TOLERANCE) {
                    startIndex = i;
                }
                if (SphericalUtil.computeDistanceBetween(end, route.get(i)) < ROUTE_TOLERANCE) {
                    endIndex = i;
                }
            }

            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                waypoints = route.subList(startIndex + 1, endIndex);
            }
        }
        return waypoints;
    }
}
