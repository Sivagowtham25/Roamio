package com.example.roamio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roamio.activities.AiTripPlannerActivity;
import com.example.roamio.activities.LoginActivity;
import com.example.roamio.activities.NearbyActivity;
import com.example.roamio.activities.ReviewActivity;
import com.example.roamio.adapters.NearbyPlaceAdapter;
import com.example.roamio.firebase.FirebaseAuthManager;
import com.example.roamio.models.NearbyPlace;
import com.example.roamio.models.User;
import com.example.roamio.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * MainActivity — the Home Screen of Roamio.
 *
 * SESSION LOGIC (30-day persistence):
 *   On every cold launch, the app checks two things:
 *   1. SessionManager.isSessionValid()  — verifies that the 30-day window hasn't expired.
 *   2. FirebaseAuthManager.isLoggedIn() — verifies the Firebase token is still present locally.
 *
 *   If BOTH pass → stay on Home (this activity).
 *   If EITHER fails → redirect to LoginActivity and finish().
 */
public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1002;
    private static final int PLACES_FETCH_LIMIT          = 10;
    private static final int SEARCH_RADIUS_METERS        = 3000;

    // Maps user trip preferences → Google Places API type + human-readable label
    private static final Map<String, String[]> PREF_TO_PLACES = new HashMap<String, String[]>() {{
        put("adventure",  new String[]{"park",             "For adventure lovers"});
        put("cultural",   new String[]{"museum",           "For culture lovers"});
        put("beach",      new String[]{"natural_feature",  "For beach lovers"});
        put("nature",     new String[]{"park",             "For nature lovers"});
        put("city",       new String[]{"tourist_attraction","For city explorers"});
        put("spiritual",  new String[]{"hindu_temple",     "For spiritual seekers"});
        put("luxury",     new String[]{"spa",              "For luxury travellers"});
        put("budget",     new String[]{"restaurant",       "For budget explorers"});
    }};

    private SessionManager      sessionManager;
    private FirebaseAuthManager authManager;

    private LinearLayout layoutRecommendations;
    private TextView     tvYouMightLikeTitle;

    // Popular near you
    private RecyclerView       rvPopularNearYou;
    private NearbyPlaceAdapter popularAdapter;
    private final List<NearbyPlace> popularPlaces = new ArrayList<>();

    // You might like (preference-based)
    private RecyclerView       rvYouMightLike;
    private NearbyPlaceAdapter likeAdapter;
    private final List<NearbyPlace> likePlaces = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private final OkHttpClient httpClient = new OkHttpClient();

    // Stored after location callback; preference fetch waits for both to be ready
    private double  userLat              = 13.0827;
    private double  userLng              = 80.2707;
    private String  preferredPlacesType  = "tourist_attraction"; // default
    private boolean locationReady        = false;
    private boolean preferencesReady     = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);

        sessionManager = new SessionManager(this);
        authManager    = new FirebaseAuthManager();

        if (!sessionManager.isSessionValid() || !authManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        // ── Apply status-bar inset to content so nothing hides under the status bar ──
        View contentContainer = findViewById(R.id.contentContainer);
        ViewCompat.setOnApplyWindowInsetsListener(contentContainer, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(
                    v.getPaddingLeft(),
                    statusBarHeight + dp(16),
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return WindowInsetsCompat.CONSUMED;
        });

        layoutRecommendations = findViewById(R.id.layoutRecommendations);
        tvYouMightLikeTitle   = findViewById(R.id.tvYouMightLikeTitle);

        // Greet user
        String userName = getIntent().getStringExtra("user_name");
        if (userName == null || userName.isEmpty()) userName = sessionManager.getSavedName();
        setupGreeting(userName);

        // ── Bell icon — opens AI Planner with a travel-inspiration prompt ──────
        // TODO: Replace with a real NotificationsActivity once
        //       FirebaseNotificationManager is implemented.
        ImageView ivBell = findViewById(R.id.ivBell);
        ivBell.setOnClickListener(v -> {
            Intent intent = new Intent(this, AiTripPlannerActivity.class);
            intent.putExtra(AiTripPlannerActivity.EXTRA_PROMPT,
                    "Suggest some trending travel destinations in India right now ✈️");
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // ── Search bar — wire IME "Search" action ──────────────────────────────
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = v.getText().toString().trim();
                if (!query.isEmpty()) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    // TODO: launch search results screen with query
                }
                return true;
            }
            return false;
        });

        // ── Popular Near You ───────────────────────────────────────────────────
        rvPopularNearYou = findViewById(R.id.rvPopularNearYou);
        popularAdapter   = new NearbyPlaceAdapter(this, popularPlaces);
        popularAdapter.setOnPlaceClickListener(new NearbyPlaceAdapter.OnPlaceClickListener() {
            @Override public void onPlaceClick(NearbyPlace place) { openNearby(place); }
            @Override public void onFavouriteClick(NearbyPlace place, int position) {}
        });
        rvPopularNearYou.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPopularNearYou.setAdapter(popularAdapter);

        // ── You Might Like ─────────────────────────────────────────────────────
        rvYouMightLike = findViewById(R.id.rvYouMightLike);
        likeAdapter    = new NearbyPlaceAdapter(this, likePlaces);
        likeAdapter.setOnPlaceClickListener(new NearbyPlaceAdapter.OnPlaceClickListener() {
            @Override public void onPlaceClick(NearbyPlace place) { openNearby(place); }
            @Override public void onFavouriteClick(NearbyPlace place, int position) {}
        });
        rvYouMightLike.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvYouMightLike.setAdapter(likeAdapter);

        // ── Load user profile → drives recommendations + preference section ───────
        loadUserProfileThenFetch();

        // ── Location + Popular Near You ────────────────────────────────────────
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocationAndLoadPlaces();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }

        setupBottomNav();
    }

    // ── Load Firestore profile — drives recommendations and preference section ─
    private void loadUserProfileThenFetch() {
        String uid = sessionManager.getSavedUid();
        if (uid == null) return;

        authManager.fetchUserProfile(uid, new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null) return;

                // Recommended for you chips
                List<String> freshRecs = com.example.roamio.activities.SignupActivity
                        .TripRecommendationEngine
                        .generate(user.getAge(), user.getJobType(), user.getTripPreferences());
                runOnUiThread(() -> populateRecommendations(freshRecs));

                // Resolve preferred place type from top preference
                List<String> prefs = user.getTripPreferences();
                if (prefs != null && !prefs.isEmpty()) {
                    String topPref = prefs.get(0);
                    String[] mapping = PREF_TO_PLACES.get(topPref);
                    if (mapping != null) {
                        preferredPlacesType = mapping[0];
                        String sectionTitle = mapping[1];
                        runOnUiThread(() -> tvYouMightLikeTitle.setText(sectionTitle));
                    }
                }

                preferencesReady = true;
                maybeFetchPreferencePlaces();
            }

            @Override
            public void onFailure(String errorMessage) {
                // Silent fail — section stays empty
            }
        });
    }

    /**
     * Fires the "You might like" fetch exactly once, only when BOTH location
     * and user preferences are resolved.
     */
    private synchronized void maybeFetchPreferencePlaces() {
        if (locationReady && preferencesReady) {
            fetchPreferencePlaces(userLat, userLng, preferredPlacesType);
        }
    }

    // ── Permission result ──────────────────────────────────────────────────────
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocationAndLoadPlaces();
        }
    }

    // ── Get location → Popular Near You, then trigger preference fetch ─────────
    private void fetchUserLocationAndLoadPlaces() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLng = location.getLongitude();
            }
            locationReady = true;
            fetchPopularNearYou(userLat, userLng);
            maybeFetchPreferencePlaces();
        }).addOnFailureListener(e -> {
            locationReady = true;
            fetchPopularNearYou(userLat, userLng);
            maybeFetchPreferencePlaces();
        });
    }

    // ── Popular Near You ───────────────────────────────────────────────────────
    private void fetchPopularNearYou(double lat, double lng) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=" + SEARCH_RADIUS_METERS
                + "&type=tourist_attraction"
                + "&rankby=prominence"
                + "&key=" + BuildConfig.MAPS_API_KEY;

        fetchPlaces(url, popularPlaces, popularAdapter);
    }

    // ── You Might Like ─────────────────────────────────────────────────────────
    private void fetchPreferencePlaces(double lat, double lng, String type) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=" + SEARCH_RADIUS_METERS
                + "&type=" + type
                + "&rankby=prominence"
                + "&key=" + BuildConfig.MAPS_API_KEY;

        fetchPlaces(url, likePlaces, likeAdapter);
    }

    // ── Shared fetch + parse + notify ─────────────────────────────────────────
    private void fetchPlaces(String url, List<NearbyPlace> targetList,
                             NearbyPlaceAdapter targetAdapter) {
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                String type = url.contains("type=")
                        ? url.split("type=")[1].split("&")[0]
                        : "tourist_attraction";
                List<NearbyPlace> results = parsePlaces(response.body().string(), type);
                runOnUiThread(() -> {
                    targetList.clear();
                    targetList.addAll(results);
                    targetAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    // ── Parse Places JSON ──────────────────────────────────────────────────────
    private List<NearbyPlace> parsePlaces(String json, String category) {
        List<NearbyPlace> list = new ArrayList<>();
        try {
            JSONArray results = new JSONObject(json).getJSONArray("results");
            for (int i = 0; i < Math.min(results.length(), PLACES_FETCH_LIMIT); i++) {
                JSONObject obj     = results.getJSONObject(i);
                String     name    = obj.optString("name", "Unknown");
                String     placeId = obj.optString("place_id", "");
                String     address = obj.optString("vicinity", "");
                double     rating  = obj.optDouble("rating", 0.0);

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

    // ── Open NearbyActivity centred on a place ─────────────────────────────────
    private void openNearby(NearbyPlace place) {
        Intent i = new Intent(this, NearbyActivity.class);
        i.putExtra("destination", place.getName());
        i.putExtra("dest_lat", place.getLat());
        i.putExtra("dest_lng", place.getLng());
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    // ── Session redirect ───────────────────────────────────────────────────────
    private void redirectToLogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    // ── Greeting ───────────────────────────────────────────────────────────────
    private void setupGreeting(String name) {
        TextView tvWhereTo = findViewById(R.id.tvWhereTo);
        if (name != null && !name.isEmpty() && !name.equals("Traveller")) {
            String firstName = "";
            if (name != null && !name.trim().isEmpty()) {
                firstName = name.trim().split("\\s+")[0];
            }

            tvWhereTo.setText("Where to,\n" + firstName + "?");
        } else {
            tvWhereTo.setText("Where to today?");
        }
    }

    // ── Recommended for you chips ──────────────────────────────────────────────
    private void populateRecommendations(List<String> recommendations) {
        layoutRecommendations.removeAllViews();
        for (String rec : recommendations) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(androidx.core.content.ContextCompat.getDrawable(
                    this, R.drawable.bg_recently_viewed));
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            row.setClickable(true);
            row.setFocusable(true);

            // Ripple feedback on tap
            int[] attrs = new int[]{android.R.attr.selectableItemBackground};
            android.content.res.TypedArray ta = obtainStyledAttributes(attrs);
            row.setForeground(ta.getDrawable(0));
            ta.recycle();

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(10);
            row.setLayoutParams(rowLp);

            TextView icon = new TextView(this);
            icon.setText("\u2708");
            icon.setTextSize(15f);
            icon.setPadding(0, 0, dp(12), 0);
            row.addView(icon);

            LinearLayout textBlock = new LinearLayout(this);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            textBlock.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView title = new TextView(this);
            title.setText(rec);
            title.setTextColor(Color.parseColor("#EEF2FF"));
            title.setTextSize(13.5f);
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textBlock.addView(title);

            TextView tag = new TextView(this);
            tag.setText("AI pick \u00b7 Tap to explore");
            tag.setTextColor(Color.parseColor("#666688"));
            tag.setTextSize(11f);
            LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tagLp.topMargin = dp(3);
            tag.setLayoutParams(tagLp);
            textBlock.addView(tag);

            row.addView(textBlock);

            TextView chevron = new TextView(this);
            chevron.setText("\u203a");
            chevron.setTextColor(Color.parseColor("#444466"));
            chevron.setTextSize(22f);
            chevron.setPadding(dp(8), 0, 0, 0);
            row.addView(chevron);

            // ── Tap → open AI Planner with this recommendation as the prompt ──
            final String prompt = "Tell me more about: " + rec
                    + ". Give me a full itinerary, estimated budget, best time to go, "
                    + "and top tips.";
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, AiTripPlannerActivity.class);
                intent.putExtra(AiTripPlannerActivity.EXTRA_PROMPT, prompt);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });

            layoutRecommendations.addView(row);
        }
    }

    // ── Bottom navigation ──────────────────────────────────────────────────────
    private void setupBottomNav() {
        // Home — scroll to top
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            View scrollView = findViewById(R.id.nestedScrollView);
            if (scrollView instanceof androidx.core.widget.NestedScrollView) {
                ((androidx.core.widget.NestedScrollView) scrollView).smoothScrollTo(0, 0);
            }
        });
        findViewById(R.id.navNearby).setOnClickListener(v -> {
            startActivity(new Intent(this, NearbyActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        findViewById(R.id.navTrips).setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.roamio.activities.MyTripsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        findViewById(R.id.navReview).setOnClickListener(v -> {
            startActivity(new Intent(this, ReviewActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        findViewById(R.id.navAccount).setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.roamio.activities.ProfileActivity.class)));
    }

    // ── dp helper ──────────────────────────────────────────────────────────────
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
