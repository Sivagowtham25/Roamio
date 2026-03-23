package com.example.roamio;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.roamio.activities.LoginActivity;
import com.example.roamio.firebase.FirebaseAuthManager;
import com.example.roamio.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

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
 *
 *   On a successful login or signup, SessionManager.saveSession() is called,
 *   which stores the current timestamp. The session remains valid for exactly
 *   30 days (30 × 24 × 60 × 60 × 1 000 ms). After that, the user must log in again.
 */
public class MainActivity extends AppCompatActivity {

    private SessionManager      sessionManager;
    private FirebaseAuthManager authManager;

    private LinearLayout layoutRecommendations;
    private TextView     tvNearbyLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Draw behind status bar for a fully immersive look
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);

        sessionManager = new SessionManager(this);
        authManager    = new FirebaseAuthManager();

        // ── Session Gate ───────────────────────────────────────────────────────
        // If the 30-day session has expired OR Firebase has no local user,
        // send the user to Login and do not show the home screen at all.
        if (!sessionManager.isSessionValid() || !authManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        // ── Home Screen ────────────────────────────────────────────────────────
        setContentView(R.layout.activity_main);

        layoutRecommendations = findViewById(R.id.layoutRecommendations);
        tvNearbyLocation      = findViewById(R.id.tvNearbyLocation);

        // Greet user
        String userName = getIntent().getStringExtra("user_name");
        if (userName == null || userName.isEmpty()) {
            userName = sessionManager.getSavedName();
        }
        setupGreeting(userName);

        // Populate AI recommendations passed from Signup, or load from session
        ArrayList<String> recs = getIntent().getStringArrayListExtra("recommendations");
        if (recs != null && !recs.isEmpty()) {
            populateRecommendations(recs);
        } else {
            loadRecommendationsFromFirestore();
        }

        // Wire Explore Nearby card
        View cardExploreNearby = findViewById(R.id.cardExploreNearby);
        cardExploreNearby.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.roamio.activities.NearbyActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Wire destination cards
        findViewById(R.id.cardDestChennai).setOnClickListener(v -> {
            Intent i = new Intent(this, com.example.roamio.activities.NearbyActivity.class);
            i.putExtra("destination", "Chennai");
            i.putExtra("dest_lat", 13.0827);
            i.putExtra("dest_lng", 80.2707);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        findViewById(R.id.cardDestAlternatives).setOnClickListener(v -> {
            Intent i = new Intent(this, com.example.roamio.activities.NearbyActivity.class);
            i.putExtra("destination", "Mahabalipuram");
            i.putExtra("dest_lat", 12.6269);
            i.putExtra("dest_lng", 80.1927);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        findViewById(R.id.cardDestPondicherry).setOnClickListener(v -> {
            Intent i = new Intent(this, com.example.roamio.activities.NearbyActivity.class);
            i.putExtra("destination", "Pondicherry");
            i.putExtra("dest_lat", 11.9416);
            i.putExtra("dest_lng", 79.8083);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Bottom nav click listeners
        setupBottomNav();
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

    // ── Update greeting subtitle (optional future use) ─────────────────────────
    private void setupGreeting(String name) {
        TextView tvWhereTo = findViewById(R.id.tvWhereTo);
        if (name != null && !name.isEmpty() && !name.equals("Traveller")) {
            tvWhereTo.setText("Where to,\n" + name + "?");
        }
    }

    // ── Populate recommendation chips dynamically ──────────────────────────────
    private void populateRecommendations(List<String> recommendations) {
        layoutRecommendations.removeAllViews();
        for (String rec : recommendations) {
            TextView chip = new TextView(this);
            chip.setText("✈  " + rec);
            chip.setTextColor(Color.parseColor("#EEF2FF"));
            chip.setTextSize(13.5f);
            chip.setPadding(dp(16), dp(14), dp(16), dp(14));
            chip.setBackground(getResources().getDrawable(R.drawable.bg_recently_viewed, null));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            chip.setLayoutParams(lp);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setClickable(true);
            chip.setFocusable(true);

            layoutRecommendations.addView(chip);
        }
    }

    // ── Fallback: load from Firestore if no intent data ───────────────────────
    private void loadRecommendationsFromFirestore() {
        String uid = sessionManager.getSavedUid();
        if (uid == null) return;

        authManager.fetchUserProfile(uid, new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(com.example.roamio.models.User user) {
                if (user != null && user.getAiRecommendations() != null
                        && !user.getAiRecommendations().isEmpty()) {
                    runOnUiThread(() -> populateRecommendations(user.getAiRecommendations()));
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Silent fail — recommendations section stays empty
            }
        });
    }

    // ── Bottom navigation ─────────────────────────────────────────────────────
    private void setupBottomNav() {
        View navNearby  = findViewById(R.id.navNearby);
        View navTrips   = findViewById(R.id.navTrips);
        View navReview  = findViewById(R.id.navReview);
        View navAccount = findViewById(R.id.navAccount);

        navNearby.setOnClickListener(v -> {
            Intent nearbyIntent = new Intent(this, com.example.roamio.activities.NearbyActivity.class);
            startActivity(nearbyIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        navTrips.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.roamio.activities.TripActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        navReview.setOnClickListener(v ->
                Toast.makeText(this, "Reviews — coming soon!", Toast.LENGTH_SHORT).show());
        navAccount.setOnClickListener(v ->
                startActivity(new Intent(this, com.example.roamio.activities.ProfileActivity.class)));
    }

    // ── dp helper ─────────────────────────────────────────────────────────────
    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
