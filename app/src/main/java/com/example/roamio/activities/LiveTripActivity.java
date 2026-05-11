package com.example.roamio.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.roamio.R;
import com.example.roamio.services.LocationService;
import com.example.roamio.services.LocationUpdateCallback;
import com.example.roamio.utils.Constants;
import com.example.roamio.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LiveTripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String tripId, destination, tripName, startDate, endDate;
    private TextView tvTripHeader, tvDayLabel, tvHotelLink;
    private LinearLayout llActivities;
    private GoogleMap gMap;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LocationService locationService;
    private boolean serviceBound = false;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    private final List<String> todayActivities = new ArrayList<>();
    private LatLng destLatLng = null;
    private String hotelLink = null;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            LocationService.LocalBinder lb = (LocationService.LocalBinder) binder;
            locationService = lb.getService();
            serviceBound = true;
            locationService.setCallback(loc -> runOnUiThread(() -> updateUserPosition(loc)));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_live_trip);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tripId = getIntent().getStringExtra(Constants.EXTRA_TRIP_ID);
        destination = getIntent().getStringExtra(Constants.EXTRA_DESTINATION);
        tripName = getIntent().getStringExtra(Constants.EXTRA_TRIP_NAME);
        startDate = getIntent().getStringExtra(Constants.EXTRA_START_DATE);
        endDate = getIntent().getStringExtra(Constants.EXTRA_END_DATE);

        if (tripId == null) {
            tripId = new SessionManager(this).getActiveTripId();
        }

        bindViews();
        SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFrag != null) mapFrag.getMapAsync(this);
        loadTripData();
    }

    private void bindViews() {
        tvTripHeader = findViewById(R.id.tvTripHeader);
        tvDayLabel = findViewById(R.id.tvDayLabel);
        tvHotelLink = findViewById(R.id.tvHotelLink);
        llActivities = findViewById(R.id.llActivities);

        tvTripHeader.setText(tripName != null ? tripName : (destination != null ? destination : "Live Trip"));
        tvDayLabel.setText(String.format(Locale.US, "Day %d of %d", calcDayNumber(), calcTotalDays()));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnNearbyNow).setOnClickListener(v -> {
            Intent i = new Intent(this, NearbyActivity.class);
            i.putExtra(Constants.EXTRA_DESTINATION, destination);
            i.putExtra(Constants.EXTRA_TRIP_ID, tripId);
            if (destLatLng != null) {
                i.putExtra(Constants.EXTRA_DEST_LAT, destLatLng.latitude);
                i.putExtra(Constants.EXTRA_DEST_LNG, destLatLng.longitude);
            }
            startActivity(i);
        });
        tvHotelLink.setOnClickListener(v -> openHotel());
    }

    private void loadTripData() {
        if (auth.getCurrentUser() == null || tripId == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection(Constants.COL_USERS).document(uid)
                .collection(Constants.COL_TRIPS).document(tripId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    hotelLink = doc.getString("hotelLink");
                    Double lat = doc.getDouble("destLat");
                    Double lng = doc.getDouble("destLng");
                    if (lat != null && lng != null) {
                        destLatLng = new LatLng(lat, lng);
                        if (gMap != null) focusMap(destLatLng);
                    }
                    updateHotelButton();
                });

        db.collection(Constants.COL_USERS).document(uid)
                .collection(Constants.COL_TRIPS).document(tripId)
                .collection(Constants.COL_ITINERARY).document(Constants.DOC_PLAN)
                .get()
                .addOnSuccessListener(doc -> {
                    String content = doc.exists() ? doc.getString("content") : null;
                    if (content != null) parseTodayActivities(content);
                });
    }

    private void parseTodayActivities(String content) {
        int dayNumber = calcDayNumber();
        String[] lines = content.split("\n");
        boolean inToday = false;
        todayActivities.clear();

        for (String line : lines) {
            String clean = line.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
            if (clean.matches("day " + dayNumber + ".*")) { inToday = true; continue; }
            if (inToday && clean.matches("day \\d.*")) break;
            if (inToday && !line.trim().isEmpty() && !line.startsWith("===") && !line.startsWith("---")) {
                todayActivities.add(line.trim());
            }
        }
        runOnUiThread(this::renderActivities);
    }

    private void renderActivities() {
        llActivities.removeAllViews();
        if (todayActivities.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No activities found for today.");
            tv.setTextColor(Color.parseColor("#88AAC0CC"));
            llActivities.addView(tv);
            return;
        }

        for (String line : todayActivities) {
            boolean isTimeSlot = line.matches(".*\\d{1,2}:\\d{2}.*[AP]M.*");
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(10);
            tv.setLayoutParams(lp);
            tv.setText(line);
            tv.setTextColor(Color.parseColor(isTimeSlot ? "#EEF2FF" : "#AAC0CC"));
            if (isTimeSlot) {
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setOnClickListener(v -> openDirections(line.replaceAll(".*AM —|.*PM —", "").trim()));
            }
            llActivities.addView(tv);
        }
    }

    private void openDirections(String placeName) {
        String query = destination != null ? placeName + " " + destination : placeName;
        Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(query)));
        i.setPackage("com.google.android.apps.maps");
        try { startActivity(i); } catch (Exception e) { Toast.makeText(this, "Maps not available", Toast.LENGTH_SHORT).show(); }
    }

    private void openHotel() {
        String url = (hotelLink != null && !hotelLink.isEmpty()) ? hotelLink : Constants.BOOKING_BASE + (destination != null ? destination.replace(" ", "%20") : "");
        try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))); } catch (Exception e) {}
    }

    private void updateHotelButton() {
        boolean hasLink = hotelLink != null && !hotelLink.isEmpty();
        tvHotelLink.setText(hasLink ? "🏨  Hotel Check-in →" : "🏨  Find Hotel →");
        tvHotelLink.setTextColor(Color.parseColor(hasLink ? "#00C9B1" : "#F4B942"));
    }

    @Override public void onMapReady(GoogleMap map) {
        gMap = map;
        gMap.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        if (destLatLng != null) focusMap(destLatLng);
    }

    private void focusMap(LatLng pos) {
        if (gMap == null) return;
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 13f));
        gMap.addMarker(new MarkerOptions().position(pos).title(destination).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
    }

    private void updateUserPosition(LatLng loc) {
        if (gMap == null) return;
        gMap.addMarker(new MarkerOptions().position(loc).title("You").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    private int calcDayNumber() {
        try {
            Date start = startDate != null ? sdf.parse(startDate) : null;
            Date today = sdf.parse(sdf.format(new Date()));
            if (start == null || today == null) return 1;
            return (int) ((today.getTime() - start.getTime()) / 86400000L) + 1;
        } catch (Exception e) { return 1; }
    }

    private int calcTotalDays() {
        try {
            Date start = startDate != null ? sdf.parse(startDate) : null;
            Date end = endDate != null ? sdf.parse(endDate) : null;
            if (start == null || end == null) return 1;
            return (int) ((end.getTime() - start.getTime()) / 86400000L) + 1;
        } catch (Exception e) { return 1; }
    }

    @Override protected void onStart() { super.onStart(); bindService(new Intent(this, LocationService.class), serviceConnection, BIND_AUTO_CREATE); }
    @Override protected void onDestroy() { super.onDestroy(); if (serviceBound) unbindService(serviceConnection); }
    private int dp(int val) { return Math.round(val * getResources().getDisplayMetrics().density); }
}
