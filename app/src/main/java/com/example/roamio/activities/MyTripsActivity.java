package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
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
            startActivity(new android.content.Intent(this, TripActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

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
                        String destination = doc.getString("destination");
                        String tripName    = doc.getString("tripName");
                        String startDate   = doc.getString("startDate");
                        String endDate     = doc.getString("endDate");
                        String travellers  = doc.getString("travellers");
                        String budget      = doc.getString("budget");
                        String notes       = doc.getString("notes");

                        View card = buildTripCard(
                                destination, tripName, startDate,
                                endDate, travellers, budget, notes, doc.getId(), uid);

                        llTrips.addView(card);
                        animateCard(card, delay);
                        delay += 80;
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load trips: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── Build a trip card dynamically ─────────────────────────────────────────
    private View buildTripCard(String destination, String tripName,
                               String startDate, String endDate,
                               String travellers, String budget,
                               String notes, String docId, String uid) {

        // Outer card
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(20));
        card.setCardElevation(dp(8));
        card.setCardBackgroundColor(Color.parseColor("#14FFFFFF"));

        // Inner layout
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(20), dp(18), dp(20), dp(18));

        // ── Destination header row ────────────────────────────────────────────
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hrp.bottomMargin = dp(10);
        headerRow.setLayoutParams(hrp);

        // Destination emoji circle
        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(getDestinationEmoji(destination));
        tvEmoji.setTextSize(22f);
        tvEmoji.setGravity(android.view.Gravity.CENTER);
        tvEmoji.setBackground(getDrawable(R.drawable.bg_logo_circle));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(dp(44), dp(44));
        ep.setMarginEnd(dp(12));
        tvEmoji.setLayoutParams(ep);

        // Trip name + destination
        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(tripName != null ? tripName : "My Trip");
        tvName.setTextColor(Color.parseColor("#EEF2FF"));
        tvName.setTextSize(16f);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDest = new TextView(this);
        tvDest.setText("📍  " + (destination != null ? destination : "—"));
        tvDest.setTextColor(Color.parseColor("#00C9B1"));
        tvDest.setTextSize(13f);
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dp2.topMargin = dp(3);
        tvDest.setLayoutParams(dp2);

        nameCol.addView(tvName);
        nameCol.addView(tvDest);
        headerRow.addView(tvEmoji);
        headerRow.addView(nameCol);

        // Delete button
        TextView btnDelete = new TextView(this);
        btnDelete.setText("🗑");
        btnDelete.setTextSize(18f);
        btnDelete.setPadding(dp(8), dp(4), dp(4), dp(4));
        btnDelete.setClickable(true);
        btnDelete.setFocusable(true);
        btnDelete.setOnClickListener(v -> deleteTrip(docId, uid, card));
        headerRow.addView(btnDelete);

        inner.addView(headerRow);

        // ── Divider ───────────────────────────────────────────────────────────
        View divider = new View(this);
        LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        dvp.bottomMargin = dp(12);
        divider.setLayoutParams(dvp);
        divider.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
        inner.addView(divider);

        // ── Info grid ─────────────────────────────────────────────────────────
        LinearLayout infoRow1 = buildInfoRow(
                "📅  " + (startDate != null ? startDate : "—") + "  →  " + (endDate != null ? endDate : "—"),
                "👥  " + (travellers != null ? travellers + " traveller(s)" : "—"));
        inner.addView(infoRow1);

        if (budget != null && !budget.isEmpty()) {
            LinearLayout infoRow2 = buildInfoRow("💰  ₹" + budget, "");
            inner.addView(infoRow2);
        }

        // ── Notes ─────────────────────────────────────────────────────────────
        if (notes != null && !notes.isEmpty()) {
            TextView tvNotes = new TextView(this);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            np.topMargin = dp(8);
            tvNotes.setLayoutParams(np);
            tvNotes.setText("🗒  " + notes);
            tvNotes.setTextColor(Color.parseColor("#88AAC0CC"));
            tvNotes.setTextSize(12f);
            tvNotes.setMaxLines(2);
            tvNotes.setEllipsize(android.text.TextUtils.TruncateAt.END);
            inner.addView(tvNotes);
        }

        card.addView(inner);
        return card;
    }

    private LinearLayout buildInfoRow(String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dp(6);
        row.setLayoutParams(rp);

        TextView tvLeft = new TextView(this);
        tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvLeft.setText(left);
        tvLeft.setTextColor(Color.parseColor("#AAC0CC"));
        tvLeft.setTextSize(13f);

        TextView tvRight = new TextView(this);
        tvRight.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvRight.setText(right);
        tvRight.setTextColor(Color.parseColor("#AAC0CC"));
        tvRight.setTextSize(13f);

        row.addView(tvLeft);
        row.addView(tvRight);
        return row;
    }

    // ── Delete trip ───────────────────────────────────────────────────────────
    private void deleteTrip(String docId, String uid, View card) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to delete this trip?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("users").document(uid)
                            .collection("trips").document(docId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                llTrips.removeView(card);
                                if (llTrips.getChildCount() == 0) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                }
                                Toast.makeText(this, "Trip deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Animate card in ───────────────────────────────────────────────────────
    private void animateCard(View card, int delayMs) {
        card.setAlpha(0f);
        card.setTranslationY(40f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(card, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(card, "translationY", 40f, 0f)
        );
        set.setDuration(350);
        set.setStartDelay(delayMs);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    // ── Emoji per destination ─────────────────────────────────────────────────
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

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}