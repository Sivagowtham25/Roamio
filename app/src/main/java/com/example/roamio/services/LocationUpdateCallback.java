package com.example.roamio.services;

import com.google.android.gms.maps.model.LatLng;

public interface LocationUpdateCallback {
    void onLocationUpdate(LatLng location);
}
