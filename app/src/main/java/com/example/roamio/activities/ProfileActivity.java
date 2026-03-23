package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;

import com.example.roamio.R;
import com.example.roamio.firebase.FirebaseAuthManager;
import com.example.roamio.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView        tvUserInitial, tvUserName, tvUserEmail;
    private TextView        tvAge, tvJobType;
    private LinearLayout    llTripPreferences, llAiRecommendations;
    private CardView        cardProfile, cardTrips, cardAI;
    private MaterialButton  btnLogout;
    private View            loadingView;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuthManager authManager;
    private com.example.roamio.utils.SessionManager sessionManager;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_profile);

        authManager    = new FirebaseAuthManager();
        sessionManager = new com.example.roamio.utils.SessionManager(this);

        bindViews();
        loadUserProfile();
        setupClickListeners();
    }

    // ── Bind Views ────────────────────────────────────────────────────────────
    private void bindViews() {
        tvUserInitial        = findViewById(R.id.tvUserInitial);
        tvUserName           = findViewById(R.id.tvUserName);
        tvUserEmail          = findViewById(R.id.tvUserEmail);
        tvAge                = findViewById(R.id.tvAge);
        tvJobType            = findViewById(R.id.tvJobType);
        llTripPreferences    = findViewById(R.id.llTripPreferences);
        llAiRecommendations  = findViewById(R.id.llAiRecommendations);
        cardProfile          = findViewById(R.id.cardProfile);
        cardTrips            = findViewById(R.id.cardTrips);
        cardAI               = findViewById(R.id.cardAI);
        btnLogout            = findViewById(R.id.btnLogout);
        loadingView          = findViewById(R.id.loadingView);
    }

    // ── Load Profile from Firestore ───────────────────────────────────────────
    private void loadUserProfile() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        setLoadingVisible(true);

        authManager.fetchUserProfile(currentUser.getUid(), new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    setLoadingVisible(false);
                    populateUI(user);
                    playEntryAnimation();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    setLoadingVisible(false);
                    Toast.makeText(ProfileActivity.this,
                            "Failed to load profile: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ── Populate UI with User data ────────────────────────────────────────────
    private void populateUI(User user) {
        // Avatar initial
        if (user.getName() != null && !user.getName().isEmpty()) {
            tvUserInitial.setText(String.valueOf(user.getName().charAt(0)).toUpperCase());
            tvUserName.setText(user.getName());
        }

        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "—");
        tvAge.setText(user.getAge() > 0 ? user.getAge() + " years" : "—");
        tvJobType.setText(user.getJobType() != null ? user.getJobType() : "—");

        // Trip preferences chips
        llTripPreferences.removeAllViews();
        if (user.getTripPreferences() != null && !user.getTripPreferences().isEmpty()) {
            for (String pref : user.getTripPreferences()) {
                llTripPreferences.addView(makeChip(pref));
            }
        } else {
            llTripPreferences.addView(makeEmptyText("No preferences set"));
        }

        // AI recommendations
        llAiRecommendations.removeAllViews();
        if (user.getAiRecommendations() != null && !user.getAiRecommendations().isEmpty()) {
            for (int i = 0; i < user.getAiRecommendations().size(); i++) {
                llAiRecommendations.addView(makeRecommendationRow(i + 1, user.getAiRecommendations().get(i)));
            }
        } else {
            llAiRecommendations.addView(makeEmptyText("No recommendations yet"));
        }
    }

    // ── Chip view for trip preferences ────────────────────────────────────────
    private TextView makeChip(String label) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 12);
        chip.setLayoutParams(params);
        chip.setText(label);
        chip.setTextColor(Color.parseColor("#EEF2FF"));
        chip.setTextSize(13f);
        chip.setPadding(28, 14, 28, 14);
        chip.setBackground(getDrawable(R.drawable.bg_trip_chip));
        return chip;
    }

    // ── Row view for AI recommendations ──────────────────────────────────────
    private TextView makeRecommendationRow(int index, String text) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        tv.setLayoutParams(params);
        tv.setText(index + ".  " + text);
        tv.setTextColor(Color.parseColor("#EEF2FF"));
        tv.setTextSize(14f);
        tv.setLineSpacing(4f, 1f);
        return tv;
    }

    // ── Empty state text ──────────────────────────────────────────────────────
    private TextView makeEmptyText(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextColor(Color.parseColor("#88AAC0CC"));
        tv.setTextSize(13f);
        return tv;
    }

    // ── Click Listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> navigateToLogin());
    }

    // ── Loading ───────────────────────────────────────────────────────────────
    private void setLoadingVisible(boolean visible) {
        loadingView.setVisibility(visible ? View.VISIBLE : View.GONE);
        cardProfile.setVisibility(visible ? View.GONE : View.VISIBLE);
        cardTrips.setVisibility(visible ? View.GONE : View.VISIBLE);
        cardAI.setVisibility(visible ? View.GONE : View.VISIBLE);
        btnLogout.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    // ── Entry Animation ───────────────────────────────────────────────────────
    private void playEntryAnimation() {
        View[] cards = {cardProfile, cardTrips, cardAI, btnLogout};
        for (int i = 0; i < cards.length; i++) {
            cards[i].setAlpha(0f);
            cards[i].setTranslationY(50f);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(cards[i], "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(cards[i], "translationY", 50f, 0f)
            );
            set.setDuration(400);
            set.setStartDelay(100L * i);
            set.setInterpolator(new DecelerateInterpolator());
            set.start();
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void navigateToLogin() {
        sessionManager.clearSession();   // ✅ clear 30-day session on logout
        authManager.signOut();           // ✅ ensure Firebase is also signed out
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}