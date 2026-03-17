package com.example.roamio.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roamio.BuildConfig;
import com.example.roamio.R;
import com.example.roamio.adapters.NearbyPlaceAdapter;
import com.example.roamio.models.NearbyPlace;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * NearbyActivity
 * Shows a Google Map with nearby places based on user's current location.
 * Category chips filter the map markers and the place cards list below.
 *
 * SETUP REQUIRED:
 *   1. Get a Google Maps API key from https://console.cloud.google.com
 *   2. Enable "Maps SDK for Android" and "Places API" in your project
 *   3. In local.properties add: MAPS_API_KEY=your_key_here
 *   4. In app/build.gradle.kts defaultConfig add:
 *      buildConfigField("String", "MAPS_API_KEY", "\"${project.findProperty("MAPS_API_KEY") ?: ""}\"")
 *      manifestPlaceholders["mapsApiKey"] = project.findProperty("MAPS_API_KEY") ?: ""
 *   5. In AndroidManifest.xml inside <application>:
 *      <meta-data android:name="com.google.android.geo.API_KEY"
 *                 android:value="${mapsApiKey}"/>
 */
public class NearbyActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int    LOCATION_PERMISSION_REQUEST = 1001;
    private static final int    SEARCH_RADIUS_METERS        = 2000;
    private static final float  DEFAULT_ZOOM                = 14.5f;

    // Places API type → display name mapping
    private static final String[][] CATEGORIES = {
            {"restaurant",          "Restaurants"},
            {"tourist_attraction",  "Things to do"},
            {"lodging",             "Hotels"},
            {"hindu_temple",        "Temples"},
            {"museum",              "Tourist Spots"},
            {"amusement_park",      "Kids Play"},
            {"shopping_mall",       "Shopping"},
            {"spa",                 "Spas"}
    };

    // ── Views ─────────────────────────────────────────────────────────────────
    private ProgressBar        progressBar;
    private RecyclerView       rvNearbyPlaces;
    private LinearLayout       chipRestaurants, chipThingsToDo, chipHotels;
    private LinearLayout       chipTemples, chipTourist, chipKids, chipShopping, chipSpas;

    // ── Map & Location ────────────────────────────────────────────────────────
    private GoogleMap                   googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng                      userLocation;
    private final List<Marker>          currentMarkers = new ArrayList<>();

    // ── Data ──────────────────────────────────────────────────────────────────
    private NearbyPlaceAdapter adapter;
    private final List<NearbyPlace> placeList = new ArrayList<>();
    private String selectedCategory = "restaurant"; // default

    // ── Network ───────────────────────────────────────────────────────────────
    private final OkHttpClient httpClient = new OkHttpClient();

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_nearby);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();
        setupRecyclerView();
        setupCategoryChips();
        initMap();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLocateMe).setOnClickListener(v -> recenterMap());
    }

    // ── Bind Views ────────────────────────────────────────────────────────────
    private void bindViews() {
        progressBar     = findViewById(R.id.progressBar);
        rvNearbyPlaces  = findViewById(R.id.rvNearbyPlaces);
        chipRestaurants = findViewById(R.id.chipRestaurants);
        chipThingsToDo  = findViewById(R.id.chipThingsToDo);
        chipHotels      = findViewById(R.id.chipHotels);
        chipTemples     = findViewById(R.id.chipTemples);
        chipTourist     = findViewById(R.id.chipTourist);
        chipKids        = findViewById(R.id.chipKids);
        chipShopping    = findViewById(R.id.chipShopping);
        chipSpas        = findViewById(R.id.chipSpas);
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new NearbyPlaceAdapter(this, placeList);
        adapter.setOnPlaceClickListener(new NearbyPlaceAdapter.OnPlaceClickListener() {
            @Override
            public void onPlaceClick(NearbyPlace place) {
                // Pan map to selected place
                if (googleMap != null) {
                    LatLng latLng = new LatLng(place.getLat(), place.getLng());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
                }
            }
            @Override
            public void onFavouriteClick(NearbyPlace place, int position) {
                String msg = place.isFavourite()
                        ? place.getName() + " added to favourites"
                        : place.getName() + " removed from favourites";
                Toast.makeText(NearbyActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvNearbyPlaces.setLayoutManager(llm);
        rvNearbyPlaces.setAdapter(adapter);
    }

    // ── Category Chips ────────────────────────────────────────────────────────
    private void setupCategoryChips() {
        LinearLayout[] chips = {
                chipRestaurants, chipThingsToDo, chipHotels, chipTemples,
                chipTourist, chipKids, chipShopping, chipSpas
        };
        String[] types = {
                "restaurant", "tourist_attraction", "lodging", "hindu_temple",
                "museum", "amusement_park", "shopping_mall", "spa"
        };

        for (int i = 0; i < chips.length; i++) {
            final String type = types[i];
            final LinearLayout chip = chips[i];
            chip.setOnClickListener(v -> {
                selectedCategory = type;
                updateChipSelection(chips, chip);
                if (userLocation != null) {
                    searchNearbyPlaces(userLocation.latitude, userLocation.longitude, type);
                }
            });
        }
    }

    private void updateChipSelection(LinearLayout[] all, LinearLayout selected) {
        for (LinearLayout chip : all) {
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.bg_category_chip_selected);
                // Make text white
                setChipTextColor(chip, "#FFFFFF");
            } else {
                chip.setBackgroundResource(R.drawable.bg_category_chip);
                setChipTextColor(chip, "#AAAAAA");
            }
        }
    }

    private void setChipTextColor(LinearLayout chip, String colorHex) {
        for (int i = 0; i < chip.getChildCount(); i++) {
            View child = chip.getChildAt(i);
            if (child instanceof android.widget.TextView) {
                ((android.widget.TextView) child).setTextColor(Color.parseColor(colorHex));
            }
        }
    }

    // ── Google Maps Init ──────────────────────────────────────────────────────
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Apply dark style
        try {
            MapStyleOptions style = MapStyleOptions
                    .loadRawResourceStyle(this, R.raw.map_style_dark);
            googleMap.setMapStyle(style);
        } catch (Exception e) {
            e.printStackTrace();
        }

        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // Marker click → show place name
        googleMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return false;
        });

        // Now get location
        requestLocation();
    }

    // ── Location ──────────────────────────────────────────────────────────────
    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        // Enable blue dot on map
        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                onLocationObtained();
            } else {
                // Fallback: default to Chennai if location unavailable
                userLocation = new LatLng(13.0827, 80.2707);
                Toast.makeText(this, "Using default location", Toast.LENGTH_SHORT).show();
                onLocationObtained();
            }
        }).addOnFailureListener(e -> {
            userLocation = new LatLng(13.0827, 80.2707);
            onLocationObtained();
        });
    }

    private void onLocationObtained() {
        // Move camera to user location
        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));

        // Draw accuracy circle around user
        googleMap.addCircle(new CircleOptions()
                .center(userLocation)
                .radius(80)
                .fillColor(Color.argb(40, 0, 120, 255))
                .strokeColor(Color.argb(120, 0, 100, 255))
                .strokeWidth(2f));

        // Search with default category
        searchNearbyPlaces(userLocation.latitude, userLocation.longitude, selectedCategory);
    }

    private void recenterMap() {
        if (userLocation != null && googleMap != null) {
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
        }
    }

    // ── Places API: Nearby Search ─────────────────────────────────────────────
    private void searchNearbyPlaces(double lat, double lng, String type) {
        showLoading(true);
        clearMarkers();
        placeList.clear();
        adapter.notifyDataSetChanged();

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=" + SEARCH_RADIUS_METERS
                + "&type=" + type
                + "&key=" + BuildConfig.MAPS_API_KEY;

        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(NearbyActivity.this,
                            "Network error. Check your connection.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> showLoading(false));
                    return;
                }

                String body = response.body().string();
                List<NearbyPlace> results = parsePlaces(body, type);

                runOnUiThread(() -> {
                    showLoading(false);
                    placeList.clear();
                    placeList.addAll(results);
                    adapter.notifyDataSetChanged();
                    addMarkersToMap(results);

                    if (results.isEmpty()) {
                        Toast.makeText(NearbyActivity.this,
                                "No " + type + "s found nearby", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // ── Parse Places JSON ─────────────────────────────────────────────────────
    private List<NearbyPlace> parsePlaces(String json, String category) {
        List<NearbyPlace> list = new ArrayList<>();
        try {
            JSONObject root    = new JSONObject(json);
            JSONArray  results = root.getJSONArray("results");

            for (int i = 0; i < Math.min(results.length(), 20); i++) {
                JSONObject obj = results.getJSONObject(i);

                String name     = obj.optString("name", "Unknown");
                String placeId  = obj.optString("place_id", "");
                String address  = obj.optString("vicinity", "");
                double rating   = obj.optDouble("rating", 0.0);

                JSONObject loc = obj.getJSONObject("geometry").getJSONObject("location");
                double pLat = loc.getDouble("lat");
                double pLng = loc.getDouble("lng");

                String photoRef = "";
                if (obj.has("photos")) {
                    photoRef = obj.getJSONArray("photos")
                            .getJSONObject(0)
                            .optString("photo_reference", "");
                }

                list.add(new NearbyPlace(name, address, pLat, pLng,
                        rating, placeId, photoRef, category));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── Map Markers ───────────────────────────────────────────────────────────
    private void addMarkersToMap(List<NearbyPlace> places) {
        clearMarkers();
        for (NearbyPlace place : places) {
            LatLng latLng = new LatLng(place.getLat(), place.getLng());

            BitmapDescriptor icon = place.getRating() > 0
                    ? createRatingMarker(place.getRating())
                    : BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(place.getName())
                    .snippet(place.getAddress())
                    .icon(icon));

            if (marker != null) currentMarkers.add(marker);
        }
    }

    private void clearMarkers() {
        for (Marker m : currentMarkers) m.remove();
        currentMarkers.clear();
    }

    /**
     * Creates a custom green circular marker with rating text.
     * Mimics the green rating badges in the screenshot.
     */
    private BitmapDescriptor createRatingMarker(double rating) {
        int size = 80;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Green filled circle
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#2E6B2E"));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint);

        // White border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(3f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 1.5f, borderPaint);

        // Rating text
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(26f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        String ratingStr = String.format("%.1f", rating);
        float y = (size / 2f) - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(ratingStr, size / 2f, y, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ── Permission Result ─────────────────────────────────────────────────────
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocation();
            } else {
                Toast.makeText(this,
                        "Location permission is needed to show nearby places",
                        Toast.LENGTH_LONG).show();
                // Use Chennai as default
                userLocation = new LatLng(13.0827, 80.2707);
                if (googleMap != null) onLocationObtained();
            }
        }
    }
}
