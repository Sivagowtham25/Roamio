package com.example.roamio.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class NearbyActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG                     = "NearbyActivity";
    private static final int    LOCATION_PERMISSION_REQUEST = 1001;
    private static final int    SEARCH_RADIUS_METERS        = 5000; // increased from 2000
    private static final float  DEFAULT_ZOOM                = 14.5f;

    // ── Views ─────────────────────────────────────────────────────────────────
    private ProgressBar  progressBar;
    private RecyclerView rvNearbyPlaces;
    private TextView     tvSectionTitle;
    private LinearLayout chipRestaurants, chipThingsToDo, chipHotels;
    private LinearLayout chipTemples, chipTourist, chipKids, chipShopping, chipSpas;

    // ── Map & Location ────────────────────────────────────────────────────────
    private GoogleMap                   googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng                      userLocation;
    private final List<Marker>          currentMarkers = new ArrayList<>();

    // ── Data ──────────────────────────────────────────────────────────────────
    private NearbyPlaceAdapter      adapter;
    private final List<NearbyPlace> placeList        = new ArrayList<>();
    private String                  selectedCategory = "restaurant";
    private boolean                 isDestinationMode = false;

    // ── Network ───────────────────────────────────────────────────────────────
    private final OkHttpClient httpClient = new OkHttpClient();

    // ── Category label map ────────────────────────────────────────────────────
    private static final String[][] CATEGORY_LABELS = {
            {"restaurant",         "Restaurants"},
            {"tourist_attraction", "Things to do"},
            {"lodging",            "Hotels"},
            {"hindu_temple",       "Temples"},
            {"museum",             "Tourist Spots"},
            {"amusement_park",     "Kids Play"},
            {"shopping_mall",      "Shopping"},
            {"spa",                "Spas"}
    };

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_nearby);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Support destination mode — passed from home page card click
        String destination = getIntent().getStringExtra("destination");
        double destLat = getIntent().getDoubleExtra("dest_lat", 0);
        double destLng = getIntent().getDoubleExtra("dest_lng", 0);
        if (destination != null && destLat != 0 && destLng != 0) {
            userLocation      = new LatLng(destLat, destLng);
            isDestinationMode = true;
        }

        bindViews();
        setupRecyclerView();
        setupCategoryChips();
        initMap();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLocateMe).setOnClickListener(v -> recenterMap());
    }

    // ── Bind Views ────────────────────────────────────────────────────────────
    private void bindViews() {
        progressBar    = findViewById(R.id.progressBar);
        rvNearbyPlaces = findViewById(R.id.rvNearbyPlaces);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);

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
                if (googleMap != null) {
                    LatLng latLng = new LatLng(place.getLat(), place.getLng());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
                }
            }

            @Override
            public void onFavouriteClick(NearbyPlace place, int position) {
                String msg = place.isFavourite()
                        ? place.getName() + " added to favourites ❤️"
                        : place.getName() + " removed from favourites";
                Toast.makeText(NearbyActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
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
            final String       type = types[i];
            final LinearLayout chip = chips[i];
            chip.setOnClickListener(v -> {
                selectedCategory = type;
                updateChipSelection(chips, chip);
                updateSectionTitle(type);
                if (userLocation != null) {
                    searchNearbyPlaces(userLocation.latitude, userLocation.longitude, type);
                } else {
                    Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateChipSelection(LinearLayout[] all, LinearLayout selected) {
        for (LinearLayout chip : all) {
            boolean isSelected = (chip == selected);
            chip.setBackgroundResource(isSelected
                    ? R.drawable.bg_category_chip_selected
                    : R.drawable.bg_category_chip);
            setChipTextColor(chip, isSelected ? "#FFFFFF" : "#AAAAAA");
        }
    }

    private void setChipTextColor(LinearLayout chip, String colorHex) {
        for (int i = 0; i < chip.getChildCount(); i++) {
            View child = chip.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(Color.parseColor(colorHex));
            }
        }
    }

    private void updateSectionTitle(String type) {
        if (tvSectionTitle == null) return;
        for (String[] pair : CATEGORY_LABELS) {
            if (pair[0].equals(type)) {
                tvSectionTitle.setText(pair[1] + " nearby");
                return;
            }
        }
        tvSectionTitle.setText("Nearby places");
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
            MapStyleOptions style =
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark);
            googleMap.setMapStyle(style);
        } catch (Exception e) {
            Log.e(TAG, "Map style load failed: " + e.getMessage());
        }

        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        googleMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return false;
        });

        if (isDestinationMode && userLocation != null) {
            onLocationObtained();
        } else {
            requestLocation();
        }
    }

    // ── Location ──────────────────────────────────────────────────────────────
    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "Location obtained: " + userLocation);
                    } else {
                        // getLastLocation() returns null when GPS was never used.
                        // Request a fresh location update instead of defaulting immediately.
                        Log.w(TAG, "Last location null — requesting fresh location");
                        requestFreshLocation();
                        return;
                    }
                    onLocationObtained();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Location failed: " + e.getMessage());
                    userLocation = new LatLng(13.0827, 80.2707); // fallback Chennai
                    Toast.makeText(this, "Could not get location. Using default.", Toast.LENGTH_SHORT).show();
                    onLocationObtained();
                });
    }

    /**
     * Requests a single fresh location update when getLastLocation() returns null.
     * This happens on emulators or when the device has never recorded a GPS fix.
     */
    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        com.google.android.gms.location.LocationRequest locationRequest =
                new com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000)
                        .setMaxUpdates(1)
                        .build();

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                new com.google.android.gms.location.LocationCallback() {
                    @Override
                    public void onLocationResult(
                            @NonNull com.google.android.gms.location.LocationResult result) {
                        fusedLocationClient.removeLocationUpdates(this);
                        if (result.getLastLocation() != null) {
                            userLocation = new LatLng(
                                    result.getLastLocation().getLatitude(),
                                    result.getLastLocation().getLongitude());
                        } else {
                            userLocation = new LatLng(13.0827, 80.2707);
                            Toast.makeText(NearbyActivity.this,
                                    "Using default location", Toast.LENGTH_SHORT).show();
                        }
                        onLocationObtained();
                    }
                },
                getMainLooper()
        );
    }

    private void onLocationObtained() {
        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));

        googleMap.addCircle(new CircleOptions()
                .center(userLocation)
                .radius(80)
                .fillColor(Color.argb(40, 0, 120, 255))
                .strokeColor(Color.argb(120, 0, 100, 255))
                .strokeWidth(2f));

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

        // Guard: API key must be present
        if (BuildConfig.MAPS_API_KEY.isEmpty()) {
            Toast.makeText(this,
                    "Maps API key missing. Add MAPS_API_KEY to local.properties.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);
        clearMarkers();
        placeList.clear();
        adapter.notifyDataSetChanged();

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=" + SEARCH_RADIUS_METERS
                + "&type=" + type
                + "&key=" + BuildConfig.MAPS_API_KEY;

        Log.d(TAG, "Searching: type=" + type + " radius=" + SEARCH_RADIUS_METERS + "m");

        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network failure: " + e.getMessage());
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(NearbyActivity.this,
                            "Network error. Check your connection.", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                if (response.body() == null) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(NearbyActivity.this,
                                "Empty response from server.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String body = response.body().string();
                Log.d(TAG, "Response received, length=" + body.length());

                // ── Check API status field ────────────────────────────────────
                try {
                    JSONObject root   = new JSONObject(body);
                    String     status = root.optString("status", "UNKNOWN");

                    Log.d(TAG, "Places API status: " + status);

                    if (status.equals("REQUEST_DENIED")) {
                        String msg = root.optString("error_message",
                                "Places API not enabled or key restricted.");
                        Log.e(TAG, "REQUEST_DENIED: " + msg);
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(NearbyActivity.this,
                                    "API Error: " + msg, Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    if (status.equals("OVER_QUERY_LIMIT")) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(NearbyActivity.this,
                                    "Query limit reached. Try again later.",
                                    Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    if (status.equals("ZERO_RESULTS")) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(NearbyActivity.this,
                                    "No " + getLabelForType(type) + " found nearby.",
                                    Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    if (!status.equals("OK")) {
                        String msg = root.optString("error_message", "Unknown API error: " + status);
                        Log.e(TAG, "API error: " + msg);
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(NearbyActivity.this, msg, Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Status parse error: " + e.getMessage());
                }

                // ── Parse results ─────────────────────────────────────────────
                List<NearbyPlace> results = parsePlaces(body, type);
                Log.d(TAG, "Parsed " + results.size() + " places for type: " + type);

                runOnUiThread(() -> {
                    showLoading(false);
                    placeList.clear();
                    placeList.addAll(results);
                    adapter.notifyDataSetChanged();
                    addMarkersToMap(results);

                    if (results.isEmpty()) {
                        Toast.makeText(NearbyActivity.this,
                                "No " + getLabelForType(type) + " found nearby.",
                                Toast.LENGTH_SHORT).show();
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

                String name    = obj.optString("name", "Unknown");
                String placeId = obj.optString("place_id", "");
                String address = obj.optString("vicinity", "");
                double rating  = obj.optDouble("rating", 0.0);

                JSONObject geometry = obj.optJSONObject("geometry");
                if (geometry == null) continue;
                JSONObject loc = geometry.optJSONObject("location");
                if (loc == null) continue;

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
            Log.e(TAG, "parsePlaces error: " + e.getMessage());
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

    // ── Custom green rating marker ────────────────────────────────────────────
    private BitmapDescriptor createRatingMarker(double rating) {
        int    size   = 80;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#2E6B2E"));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(3f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 1.5f, borderPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(26f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        String ratingStr = String.format(java.util.Locale.US, "%.1f", rating);
        float  y         = (size / 2f) - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(ratingStr, size / 2f, y, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showLoading(boolean show) {
        if (progressBar != null)
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private String getLabelForType(String type) {
        for (String[] pair : CATEGORY_LABELS) {
            if (pair[0].equals(type)) return pair[1];
        }
        return "places";
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
                        "Location permission needed to show nearby places.",
                        Toast.LENGTH_LONG).show();
                userLocation = new LatLng(13.0827, 80.2707);
                if (googleMap != null) onLocationObtained();
            }
        }
    }
}
