package com.example.roamio.activities;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.roamio.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ── Intent extra keys ─────────────────────────────────────────────────────
    public static final String EXTRA_NAME     = "map_name";
    public static final String EXTRA_ADDRESS  = "map_address";
    public static final String EXTRA_LAT      = "map_lat";
    public static final String EXTRA_LNG      = "map_lng";
    public static final String EXTRA_RATING   = "map_rating";
    public static final String EXTRA_REVIEWS  = "map_reviews";
    public static final String EXTRA_PLACE_ID = "map_place_id";
    public static final String EXTRA_CATEGORY = "map_category";
    public static final String EXTRA_USER_LAT = "map_user_lat";
    public static final String EXTRA_USER_LNG = "map_user_lng";

    // ── Place data ────────────────────────────────────────────────────────────
    private String name, address, category;
    private double placeLat, placeLng, userLat, userLng;
    private double rating;
    private int    reviews;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView tvPlaceName, tvAddress, tvRating, tvDistance;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_map);

        // Read intent data
        name     = getIntent().getStringExtra(EXTRA_NAME);
        address  = getIntent().getStringExtra(EXTRA_ADDRESS);
        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        placeLat = getIntent().getDoubleExtra(EXTRA_LAT, 0);
        placeLng = getIntent().getDoubleExtra(EXTRA_LNG, 0);
        rating   = getIntent().getDoubleExtra(EXTRA_RATING, 0);
        reviews  = getIntent().getIntExtra(EXTRA_REVIEWS, 0);
        userLat  = getIntent().getDoubleExtra(EXTRA_USER_LAT, placeLat);
        userLng  = getIntent().getDoubleExtra(EXTRA_USER_LNG, placeLng);

        // Bind views
        tvPlaceName = findViewById(R.id.tvMapPlaceName);
        tvAddress   = findViewById(R.id.tvMapAddress);
        tvRating    = findViewById(R.id.tvMapRating);
        tvDistance  = findViewById(R.id.tvMapDistance);

        findViewById(R.id.btnMapBack).setOnClickListener(v -> finish());

        // Populate bottom card
        tvPlaceName.setText(name != null ? name : "Place");
        tvAddress.setText(address != null && !address.isEmpty() ? address : "");

        if (rating > 0) {
            String ratingStr;
            if (reviews >= 1000) {
                ratingStr = String.format("\u2605 %.1f  \u00b7  %.1fk reviews",
                        rating, reviews / 1000.0);
            } else if (reviews > 0) {
                ratingStr = String.format("\u2605 %.1f  \u00b7  %d reviews", rating, reviews);
            } else {
                ratingStr = String.format("\u2605 %.1f", rating);
            }
            tvRating.setText(ratingStr);
            tvRating.setVisibility(View.VISIBLE);
        } else {
            tvRating.setVisibility(View.GONE);
        }

        // Distance
        float[] distResult = new float[1];
        Location.distanceBetween(userLat, userLng, placeLat, placeLng, distResult);
        float distMeters = distResult[0];
        if (distMeters < 1000) {
            tvDistance.setText(String.format("\uD83D\uDCCD %.0f m away", distMeters));
        } else {
            tvDistance.setText(String.format("\uD83D\uDCCD %.1f km away", distMeters / 1000f));
        }
        tvDistance.setVisibility(View.VISIBLE);

        // Init map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragmentView);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        // Apply dark style
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        } catch (Exception ignored) {}

        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);

        LatLng placeLatLng = new LatLng(placeLat, placeLng);

        map.addMarker(new MarkerOptions()
                .position(placeLatLng)
                .title(name)
                .snippet(address)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, 16f));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, 16f), 800, null);
    }
}
