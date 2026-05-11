package com.example.roamio.api;

import com.google.android.gms.maps.model.LatLng;
import java.util.List;
import java.util.ArrayList;

public class DirectionsApi {

    public interface DirectionsCallback {
        void onSuccess(List<LatLng> points);
        void onFailure(String error);
    }

    public static void getRoute(LatLng origin, LatLng destination, List<LatLng> waypoints, String mode, DirectionsCallback callback) {
        // Implementation for getting route. 
        // For now, providing a dummy success response to fix compilation.
        List<LatLng> dummyPoints = new ArrayList<>();
        dummyPoints.add(origin);
        if (waypoints != null) dummyPoints.addAll(waypoints);
        dummyPoints.add(destination);
        callback.onSuccess(dummyPoints);
    }
}
