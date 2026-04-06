package com.example.roamio;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.example.roamio.activities.MapActivity;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int  LOCATION_PERMISSION_REQUEST = 1002;
    private static final int  PLACES_FETCH_LIMIT          = 20;
    private static final int  POPULAR_RADIUS_METERS       = 1500;
    private static final int  PREF_RADIUS_METERS          = 10000;
    private static final int  MIN_REVIEWS_POPULAR         = 50;
    private static final long CACHE_TTL_MS                = 24 * 60 * 60 * 1000L;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
            "gemini-2.5-flash:generateContent?key=";

    private static final Map<String, String[]> PREF_FALLBACK_TYPES =
            new HashMap<String, String[]>() {{
        put("adventure", new String[]{"park", "campground"});
        put("cultural",  new String[]{"museum", "art_gallery"});
        put("beach",     new String[]{"beach", "natural_feature"});
        put("nature",    new String[]{"park", "zoo"});
        put("city",      new String[]{"tourist_attraction", "shopping_mall"});
        put("spiritual", new String[]{"hindu_temple", "place_of_worship"});
        put("luxury",    new String[]{"spa", "casino"});
        put("budget",    new String[]{"restaurant", "market"});
    }};

    private static final Map<String, String> PREF_TITLE =
            new HashMap<String, String>() {{
        put("adventure", "For adventure lovers \uD83C\uDFD5\uFE0F");
        put("cultural",  "For culture lovers \uD83C\uDFA8");
        put("beach",     "For beach lovers \uD83C\uDFD6\uFE0F");
        put("nature",    "For nature lovers \uD83C\uDF3F");
        put("city",      "For city explorers \uD83C\uDFD9\uFE0F");
        put("spiritual", "For spiritual seekers \uD83D\uDED5");
        put("luxury",    "For luxury travellers \uD83D\uDC8E");
        put("budget",    "For budget explorers \uD83D\uDCB0");
    }};

    // ── Auth / Session ────────────────────────────────────────────────────────
    private SessionManager      sessionManager;
    private FirebaseAuthManager authManager;

    // ── Views ─────────────────────────────────────────────────────────────────
    private LinearLayout layoutRecommendations;
    private TextView     tvYouMightLikeTitle;
    private RecyclerView rvPopularNearYou;
    private RecyclerView rvYouMightLike;

    // ── Data ──────────────────────────────────────────────────────────────────
    private NearbyPlaceAdapter popularAdapter;
    private NearbyPlaceAdapter likeAdapter;
    private final List<NearbyPlace> popularPlaces = new ArrayList<>();
    private final List<NearbyPlace> likePlaces    = new ArrayList<>();

    // ── Location ──────────────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 13.0827;
    private double userLng = 80.2707;

    // ── State (all three gates must open before "lovers" fetch fires) ─────────
    private boolean locationReady    = false;
    private boolean preferencesReady = false;
    private boolean tagsReady        = false;

    // ── Prefs + Gemini tags ───────────────────────────────────────────────────
    private List<String>              userPrefs  = new ArrayList<>();
    private Map<String, List<String>> geminiTags = new HashMap<>();

    // ── Network ───────────────────────────────────────────────────────────────
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Random       random     = new Random();

    // ─────────────────────────────────────────────────────────────────────────
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

        View contentContainer = findViewById(R.id.contentContainer);
        ViewCompat.setOnApplyWindowInsetsListener(contentContainer, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top + dp(16), v.getPaddingRight(), v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        layoutRecommendations = findViewById(R.id.layoutRecommendations);
        tvYouMightLikeTitle   = findViewById(R.id.tvYouMightLikeTitle);

        String userName = getIntent().getStringExtra("user_name");
        if (userName == null || userName.isEmpty()) userName = sessionManager.getSavedName();
        setupGreeting(userName);

        // Bell → AI planner inspiration prompt
        ImageView ivBell = findViewById(R.id.ivBell);
        ivBell.setOnClickListener(v -> {
            Intent i = new Intent(this, AiTripPlannerActivity.class);
            i.putExtra(AiTripPlannerActivity.EXTRA_PROMPT,
                    "Suggest some trending travel destinations in India right now \u2708\uFE0F");
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Search bar
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // Popular Near You recycler
        rvPopularNearYou = findViewById(R.id.rvPopularNearYou);
        popularAdapter   = new NearbyPlaceAdapter(this, popularPlaces);
        popularAdapter.setOnPlaceClickListener(new NearbyPlaceAdapter.OnPlaceClickListener() {
            @Override public void onPlaceClick(NearbyPlace p)          { openMapActivity(p); }
            @Override public void onFavouriteClick(NearbyPlace p, int pos) {}
        });
        rvPopularNearYou.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPopularNearYou.setAdapter(popularAdapter);

        // You Might Like recycler
        rvYouMightLike = findViewById(R.id.rvYouMightLike);
        likeAdapter    = new NearbyPlaceAdapter(this, likePlaces);
        likeAdapter.setOnPlaceClickListener(new NearbyPlaceAdapter.OnPlaceClickListener() {
            @Override public void onPlaceClick(NearbyPlace p)          { openMapActivity(p); }
            @Override public void onFavouriteClick(NearbyPlace p, int pos) {}
        });
        rvYouMightLike.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvYouMightLike.setAdapter(likeAdapter);

        loadUserProfileThenFetch();

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

    // ── Load profile → recs + prefs ───────────────────────────────────────────
    private void loadUserProfileThenFetch() {
        String uid = sessionManager.getSavedUid();
        if (uid == null) return;

        authManager.fetchUserProfile(uid, new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null) return;

                List<String> recs = com.example.roamio.activities.SignupActivity
                        .TripRecommendationEngine
                        .generate(user.getAge(), user.getJobType(), user.getTripPreferences());
                runOnUiThread(() -> populateRecommendations(recs));

                List<String> prefs = user.getTripPreferences();
                if (prefs != null && !prefs.isEmpty()) {
                    userPrefs = new ArrayList<>(prefs);
                }
                preferencesReady = true;
                maybeFetchPreferencePlaces();
            }

            @Override
            public void onFailure(String err) {
                preferencesReady = true;
                maybeFetchPreferencePlaces();
            }
        });
    }

    // ── Gate: all three ready → pick random pref → fetch ─────────────────────
    private synchronized void maybeFetchPreferencePlaces() {
        if (!locationReady || !preferencesReady || !tagsReady) return;

        List<String> prefs = userPrefs.isEmpty()
                ? Collections.singletonList("city") : userPrefs;
        String chosenPref = prefs.get(random.nextInt(prefs.size()));
        String placeType  = resolveTypeForPref(chosenPref);

        String title = PREF_TITLE.containsKey(chosenPref)
                ? PREF_TITLE.get(chosenPref) : "You might like";
        runOnUiThread(() -> tvYouMightLikeTitle.setText(title));

        fetchPreferencePlaces(userLat, userLng, placeType);
    }

    private String resolveTypeForPref(String pref) {
        List<String> types = geminiTags.get(pref);
        if (types != null && !types.isEmpty())
            return types.get(random.nextInt(types.size()));
        String[] fallback = PREF_FALLBACK_TYPES.get(pref);
        if (fallback != null && fallback.length > 0)
            return fallback[random.nextInt(fallback.length)];
        return "tourist_attraction";
    }

    // ── Permission result ──────────────────────────────────────────────────────
    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == LOCATION_PERMISSION_REQUEST && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED)
            fetchUserLocationAndLoadPlaces();
    }

    // ── Get location ──────────────────────────────────────────────────────────
    private void fetchUserLocationAndLoadPlaces() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) { userLat = loc.getLatitude(); userLng = loc.getLongitude(); }
            locationReady = true;
            fetchPopularNearYou(userLat, userLng);
            loadGeminiLocationTags(userLat, userLng);
        }).addOnFailureListener(e -> {
            locationReady = true;
            fetchPopularNearYou(userLat, userLng);
            loadGeminiLocationTags(userLat, userLng);
        });
    }

    // ── Gemini location tags (cached 24 h) ────────────────────────────────────
    private void loadGeminiLocationTags(double lat, double lng) {
        SharedPreferences cache = getSharedPreferences("roamio_cache", MODE_PRIVATE);
        String cacheKey = "gemini_tags_v1_" + (int)(lat * 10) + "_" + (int)(lng * 10);
        String tsKey    = cacheKey + "_ts";
        long   ts       = cache.getLong(tsKey, 0);
        String cached   = cache.getString(cacheKey, null);

        if (cached != null && System.currentTimeMillis() - ts < CACHE_TTL_MS) {
            parseGeminiTagsJson(cached);
            tagsReady = true;
            maybeFetchPreferencePlaces();
            return;
        }

        String prompt =
                "I am at coordinates lat=" + String.format("%.2f", lat)
                + " lng=" + String.format("%.2f", lng) + " (an Indian city).\n"
                + "Return ONLY a raw JSON object mapping these 8 travel preference keys "
                + "to 2-3 relevant Google Places API type strings for this specific location.\n"
                + "Keys: adventure, cultural, beach, nature, city, spiritual, luxury, budget.\n"
                + "Use only types from: park, museum, art_gallery, beach, natural_feature, "
                + "tourist_attraction, shopping_mall, hindu_temple, place_of_worship, spa, "
                + "restaurant, campground, zoo, amusement_park, aquarium, night_club, lodging.\n"
                + "No markdown, no explanation. Only the JSON object.";

        try {
            JSONObject body = new JSONObject();
            JSONArray  contents = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("parts", new JSONArray().put(new JSONObject().put("text", prompt)));
            contents.put(msg);
            body.put("contents", contents);
            body.put("generationConfig",
                    new JSONObject().put("maxOutputTokens", 256).put("temperature", 0.2));

            Request req = new Request.Builder()
                    .url(GEMINI_URL + BuildConfig.GEMINI_API_KEY)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.w(TAG, "Gemini tags failed: " + e.getMessage());
                    tagsReady = true;
                    maybeFetchPreferencePlaces();
                }

                @Override public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    try {
                        if (response.body() == null) throw new IOException("empty");
                        String raw = response.body().string();
                        String text = new JSONObject(raw)
                                .getJSONArray("candidates").getJSONObject(0)
                                .getJSONObject("content").getJSONArray("parts")
                                .getJSONObject(0).getString("text");
                        text = text.replaceAll("```json", "").replaceAll("```", "").trim();
                        parseGeminiTagsJson(text);
                        cache.edit().putString(cacheKey, text)
                                .putLong(tsKey, System.currentTimeMillis()).apply();
                        Log.d(TAG, "Gemini tags cached: " + cacheKey);
                    } catch (Exception e) {
                        Log.w(TAG, "Gemini tags parse error: " + e.getMessage());
                    } finally {
                        tagsReady = true;
                        maybeFetchPreferencePlaces();
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Gemini tags build error: " + e.getMessage());
            tagsReady = true;
            maybeFetchPreferencePlaces();
        }
    }

    private void parseGeminiTagsJson(String json) {
        try {
            JSONObject root = new JSONObject(json);
            for (String pref : new String[]{"adventure","cultural","beach","nature",
                    "city","spiritual","luxury","budget"}) {
                if (!root.has(pref)) continue;
                JSONArray arr = root.getJSONArray(pref);
                List<String> types = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) types.add(arr.getString(i));
                geminiTags.put(pref, types);
            }
            Log.d(TAG, "Gemini tags: " + geminiTags.size() + " prefs loaded");
        } catch (Exception e) {
            Log.w(TAG, "parseGeminiTagsJson: " + e.getMessage());
        }
    }

    // ── Popular Near You: 1.5 km, weighted sort ───────────────────────────────
    private void fetchPopularNearYou(double lat, double lng) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=" + POPULAR_RADIUS_METERS
                + "&type=tourist_attraction"
                + "&key=" + BuildConfig.MAPS_API_KEY;

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call c, @NonNull Response r) throws IOException {
                if (!r.isSuccessful() || r.body() == null) return;
                List<NearbyPlace> raw      = parsePlaces(r.body().string(), "tourist_attraction");
                List<NearbyPlace> filtered = filterAndSortPopular(raw);
                runOnUiThread(() -> {
                    popularPlaces.clear();
                    popularPlaces.addAll(filtered);
                    popularAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    /**
     * score = rating x log10(reviewCount + 1)
     * Heavy review-count bias: 4★/1500 reviews beats 5★/100 reviews.
     */
    private List<NearbyPlace> filterAndSortPopular(List<NearbyPlace> raw) {
        List<NearbyPlace> eligible = new ArrayList<>();
        for (NearbyPlace p : raw) {
            if (p.getUserRatingsTotal() >= MIN_REVIEWS_POPULAR) eligible.add(p);
        }
        Collections.sort(eligible, (a, b) -> {
            double sa = a.getRating() * Math.log10(a.getUserRatingsTotal() + 1);
            double sb = b.getRating() * Math.log10(b.getUserRatingsTotal() + 1);
            return Double.compare(sb, sa);
        });
        return eligible.subList(0, Math.min(10, eligible.size()));
    }

    // ── You Might Like: 10 km city-wide, Gemini-driven type ──────────────────
    private void fetchPreferencePlaces(double lat, double lng, String type) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=" + PREF_RADIUS_METERS
                + "&type=" + type
                + "&key=" + BuildConfig.MAPS_API_KEY;

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call c, @NonNull Response r) throws IOException {
                if (!r.isSuccessful() || r.body() == null) return;
                List<NearbyPlace> results = parsePlaces(r.body().string(), type);
                runOnUiThread(() -> {
                    likePlaces.clear();
                    likePlaces.addAll(results.subList(0, Math.min(10, results.size())));
                    likeAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    // ── Parse Places JSON (includes userRatingsTotal) ─────────────────────────
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
                int        reviews = obj.optInt("user_ratings_total", 0);

                JSONObject geometry = obj.optJSONObject("geometry");
                if (geometry == null) continue;
                JSONObject loc = geometry.optJSONObject("location");
                if (loc == null) continue;

                String photoRef = "";
                if (obj.has("photos"))
                    photoRef = obj.getJSONArray("photos").getJSONObject(0)
                            .optString("photo_reference", "");

                list.add(new NearbyPlace(name, address,
                        loc.getDouble("lat"), loc.getDouble("lng"),
                        rating, reviews, placeId, photoRef, category));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ── Open MapActivity ──────────────────────────────────────────────────────
    private void openMapActivity(NearbyPlace place) {
        Intent i = new Intent(this, MapActivity.class);
        i.putExtra(MapActivity.EXTRA_NAME,     place.getName());
        i.putExtra(MapActivity.EXTRA_ADDRESS,  place.getAddress());
        i.putExtra(MapActivity.EXTRA_LAT,      place.getLat());
        i.putExtra(MapActivity.EXTRA_LNG,      place.getLng());
        i.putExtra(MapActivity.EXTRA_RATING,   place.getRating());
        i.putExtra(MapActivity.EXTRA_REVIEWS,  place.getUserRatingsTotal());
        i.putExtra(MapActivity.EXTRA_PLACE_ID, place.getPlaceId());
        i.putExtra(MapActivity.EXTRA_CATEGORY, place.getCategory());
        i.putExtra(MapActivity.EXTRA_USER_LAT, userLat);
        i.putExtra(MapActivity.EXTRA_USER_LNG, userLng);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    // ── Session redirect ──────────────────────────────────────────────────────
    private void redirectToLogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    // ── Greeting ──────────────────────────────────────────────────────────────
    private void setupGreeting(String name) {
        TextView tv = findViewById(R.id.tvWhereTo);
        if (name != null && !name.isEmpty() && !name.equals("Traveller")) {
            tv.setText("Where to,\n" + name.trim().split("\\s+")[0] + "?");
        } else {
            tv.setText("Where to today?");
        }
    }

    // ── Recommended for you ───────────────────────────────────────────────────
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

            int[] attrs = new int[]{android.R.attr.selectableItemBackground};
            android.content.res.TypedArray ta = obtainStyledAttributes(attrs);
            row.setForeground(ta.getDrawable(0));
            ta.recycle();

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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

            final String prompt = "Tell me more about: " + rec
                    + ". Give me a full itinerary, estimated budget, best time to go, and top tips.";
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, AiTripPlannerActivity.class);
                intent.putExtra(AiTripPlannerActivity.EXTRA_PROMPT, prompt);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });

            layoutRecommendations.addView(row);
        }
    }

    // ── Bottom navigation ─────────────────────────────────────────────────────
    private void setupBottomNav() {
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            View sv = findViewById(R.id.nestedScrollView);
            if (sv instanceof androidx.core.widget.NestedScrollView)
                ((androidx.core.widget.NestedScrollView) sv).smoothScrollTo(0, 0);
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
                startActivity(new Intent(this, com.example.roamio.activities.ProfileActivity.class)));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
