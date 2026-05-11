package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;

import com.example.roamio.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class MyTripsActivity extends AppCompatActivity {

    private LinearLayout llTrips;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_my_trips);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        llTrips     = findViewById(R.id.llTrips);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddTrip).setOnClickListener(v -> {
            startActivity(new Intent(this, TripActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrips();
    }

    private void loadTrips() {
        if (auth.getCurrentUser() == null) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Please log in to view your trips");
            progressBar.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        llTrips.removeAllViews();

        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .collection("trips")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);

                    if (snapshots.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    int delay = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String docId      = doc.getId();
                        String destination = doc.getString("destination");
                        String tripName    = doc.getString("tripName");
                        String startDate   = doc.getString("startDate");
                        String endDate     = doc.getString("endDate");
                        String travellers  = doc.getString("travellers");
                        String budget      = doc.getString("budget");
                        String notes       = doc.getString("notes");
                        
                        String keywords = null;
                        Object kwObj = doc.get("keywords");
                        if (kwObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> kwList = (List<String>) kwObj;
                            if (kwList != null && !kwList.isEmpty()) {
                                keywords = android.text.TextUtils.join(", ", kwList);
                            }
                        } else if (kwObj instanceof String) {
                            keywords = (String) kwObj;
                        }

                        View card = buildTripCard(destination, tripName, startDate,
                                endDate, travellers, budget, notes, keywords, docId, uid);

                        llTrips.addView(card);
                        animateCard(card, delay);
                        delay += 80;

                        checkItineraryExists(docId, uid, card);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load trips: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void checkItineraryExists(String docId, String uid, View card) {
        db.collection("users").document(uid)
                .collection("trips").document(docId)
                .collection("itinerary").document("plan")
                .get()
                .addOnSuccessListener(doc -> {
                    TextView btnItinerary = card.findViewWithTag("btnItinerary");
                    if (btnItinerary == null) return;
                    if (doc.exists() && doc.getString("content") != null
                            && !doc.getString("content").isEmpty()) {
                        btnItinerary.setText("📋  View Itinerary");
                        btnItinerary.setTextColor(Color.parseColor("#00C9B1"));
                    } else {
                        btnItinerary.setText("✦  Create Itinerary");
                        btnItinerary.setTextColor(Color.parseColor("#88AAC0CC"));
                    }
                });
    }

    private View buildTripCard(String destination, String tripName,
                               String startDate, String endDate,
                               String travellers, String budget,
                               String notes, String keywords,
                               String docId, String uid) {

        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(20));
        card.setCardElevation(dp(8));
        card.setCardBackgroundColor(Color.parseColor("#14FFFFFF"));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(20), dp(18), dp(20), dp(18));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(-1, -2);
        hrp.bottomMargin = dp(10);
        headerRow.setLayoutParams(hrp);

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(getDestinationEmoji(destination));
        tvEmoji.setTextSize(22f);
        tvEmoji.setGravity(android.view.Gravity.CENTER);
        tvEmoji.setBackground(getDrawable(R.drawable.bg_logo_circle));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(dp(44), dp(44));
        ep.setMarginEnd(dp(12));
        tvEmoji.setLayoutParams(ep);

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(tripName != null ? tripName : "My Trip");
        tvName.setTextColor(Color.parseColor("#EEF2FF"));
        tvName.setTextSize(16f);
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvDest = new TextView(this);
        tvDest.setText("📍  " + (destination != null ? destination : "—"));
        tvDest.setTextColor(Color.parseColor("#00C9B1"));
        tvDest.setTextSize(13f);
        nameCol.addView(tvName);
        nameCol.addView(tvDest);
        headerRow.addView(tvEmoji);
        headerRow.addView(nameCol);

        TextView btnDelete = new TextView(this);
        btnDelete.setText("🗑");
        btnDelete.setTextSize(18f);
        btnDelete.setPadding(dp(8), dp(4), dp(4), dp(4));
        btnDelete.setOnClickListener(v -> deleteTrip(docId, uid, card));
        headerRow.addView(btnDelete);

        inner.addView(headerRow);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        divider.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
        inner.addView(divider);

        inner.addView(buildInfoRow("📅 " + (startDate != null ? startDate : "—") + " → " + (endDate != null ? endDate : "—"), "👥 " + (travellers != null ? travellers : "—")));
        if (budget != null) inner.addView(buildInfoRow("💰 ₹" + budget, ""));

        View divider2 = new View(this);
        divider2.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        divider2.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
        inner.addView(divider2);

        TextView btnItinerary = new TextView(this);
        btnItinerary.setTag("btnItinerary");
        btnItinerary.setText("✦  Itinerary");
        btnItinerary.setTextColor(Color.parseColor("#00C9B1"));
        btnItinerary.setGravity(android.view.Gravity.CENTER);
        btnItinerary.setPadding(dp(12), dp(10), dp(12), dp(4));
        btnItinerary.setOnClickListener(v -> {
            Intent intent = new Intent(this, ItineraryActivity.class);
            intent.putExtra(ItineraryActivity.EXTRA_TRIP_ID, docId);
            intent.putExtra(ItineraryActivity.EXTRA_DESTINATION, destination);
            startActivity(intent);
        });

        inner.addView(btnItinerary);
        card.addView(inner);
        return card;
    }

    private LinearLayout buildInfoRow(String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(6), 0, 0);
        TextView tvL = new TextView(this); tvL.setText(left); tvL.setTextColor(Color.parseColor("#AAC0CC")); tvL.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tvR = new TextView(this); tvR.setText(right); tvR.setTextColor(Color.parseColor("#AAC0CC")); tvR.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(tvL); row.addView(tvR);
        return row;
    }

    private void deleteTrip(String docId, String uid, View card) {
        db.collection("users").document(uid).collection("trips").document(docId).delete().addOnSuccessListener(u -> llTrips.removeView(card));
    }

    private void animateCard(View card, int delay) {
        card.setAlpha(0); card.setTranslationY(40);
        card.animate().alpha(1).translationY(0).setDuration(300).setStartDelay(delay).start();
    }

    private String getDestinationEmoji(String dest) {
        if (dest == null) return "✈️";
        String d = dest.toLowerCase();
        if (d.contains("goa"))            return "🏖️";
        if (d.contains("kerala"))         return "🛥️";
        if (d.contains("rajasthan") || d.contains("jaipur")) return "🏰";
        if (d.contains("manali") || d.contains("shimla"))    return "🏔️";
        if (d.contains("mumbai"))         return "🌃";
        if (d.contains("delhi"))          return "🕌";
        if (d.contains("varanasi"))       return "🕯️";
        if (d.contains("pondicherry"))    return "🌊";
        if (d.contains("chennai"))        return "🏖️";
        if (d.contains("madurai"))        return "🛕";
        if (d.contains("kumbakonam"))     return "🛕";
        if (d.contains("mahabalipuram")) return "🏛️";
        if (d.contains("srirangam"))      return "🛕";
        if (d.contains("tenkasi"))        return "🌊";
        if (d.contains("rameswaram"))     return "🛕";
        if (d.contains("kanyakumari"))    return "🌅";
        if (d.contains("thanjavur"))      return "🛕";
        if (d.contains("ooty"))           return "🚂";
        if (d.contains("kodaikanal"))     return "🌿";
        if (d.contains("andaman"))        return "🐠";
        if (d.contains("mysore"))         return "🏯";
        if (d.contains("hampi"))          return "🏛️";
        return "✈️";
    }

    private int dp(int val) { return Math.round(val * getResources().getDisplayMetrics().density); }
}
