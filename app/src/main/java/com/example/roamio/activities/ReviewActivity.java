package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roamio.R;
import com.example.roamio.adapters.ReviewAdapter;
import com.example.roamio.models.Review;
import com.example.roamio.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ReviewActivity extends AppCompatActivity {

    // ── Reviewable places list ────────────────────────────────────────────────
    private static final String[] PLACES = {
            "Marina Beach, Chennai",
            "Kapaleeshwarar Temple, Chennai",
            "Meenakshi Amman Temple, Madurai",
            "Shore Temple, Mahabalipuram",
            "Brihadeeswarar Temple, Thanjavur",
            "Vivekananda Rock Memorial, Kanyakumari",
            "Promenade Beach, Pondicherry",
            "Auroville, Pondicherry",
            "Courtallam Waterfalls, Tenkasi",
            "Nilgiri Mountain Railway, Ooty",
            "Ooty Lake & Botanical Garden",
            "Kodai Lake, Kodaikanal",
            "Dhanushkodi, Rameswaram",
            "Ramanathaswamy Temple, Rameswaram",
            "Fort Aguada, Goa",
            "Calangute Beach, Goa",
            "Alleppey Backwaters, Kerala",
            "Periyar Wildlife Sanctuary, Kerala",
            "Amer Fort, Jaipur",
            "City Palace, Jaipur",
            "Manali Snow Point, Himachal",
            "Marine Drive, Mumbai",
            "Gateway of India, Mumbai",
            "Dashaswamedh Ghat, Varanasi",
            "Hampi Ruins, Karnataka",
            "Mysore Palace, Karnataka"
    };

    // ── Rating labels ─────────────────────────────────────────────────────────
    private static final String[] RATING_LABELS = {
            "", "Poor 😞", "Fair 😐", "Good 🙂", "Great 😊", "Excellent 🤩"
    };

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputEditText etPlace, etReview;
    private TextView          tvRatingLabel, tvAvgRating, tvReviewCount;
    private TextView          star1, star2, star3, star4, star5;
    private TextView          filterAll, filterMine;
    private TextView          tvEmpty;
    private LinearLayout      llAvgStars;
    private View              bar1, bar2, bar3, bar4, bar5;
    private TextView          tvCount1, tvCount2, tvCount3, tvCount4, tvCount5;
    private MaterialButton    btnSubmitReview;
    private CardView          cardWriteReview;
    private ProgressBar       progressBar;
    private RecyclerView      rvReviews;

    // ── State ─────────────────────────────────────────────────────────────────
    private int            selectedRating  = 0;
    private String         selectedPlace   = "";
    private boolean        showingMineOnly = false;

    // ── Data ──────────────────────────────────────────────────────────────────
    private ReviewAdapter     adapter;
    private final List<Review> allReviews  = new ArrayList<>();
    private final List<Review> myReviews   = new ArrayList<>();
    private final List<Review> displayList = new ArrayList<>();

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth      auth;
    private SessionManager    sessionManager;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_review);

        db             = FirebaseFirestore.getInstance();
        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        setupRecyclerView();
        setupPlacePicker();
        setupStarPicker();
        setupFilterChips();
        setupClickListeners();
        loadReviews();
        playEntryAnimation();
    }

    // ── Bind Views ────────────────────────────────────────────────────────────
    private void bindViews() {
        etPlace          = findViewById(R.id.etPlace);
        etReview         = findViewById(R.id.etReview);
        tvRatingLabel    = findViewById(R.id.tvRatingLabel);
        tvAvgRating      = findViewById(R.id.tvAvgRating);
        tvReviewCount    = findViewById(R.id.tvReviewCount);
        llAvgStars       = findViewById(R.id.llAvgStars);
        star1            = findViewById(R.id.star1);
        star2            = findViewById(R.id.star2);
        star3            = findViewById(R.id.star3);
        star4            = findViewById(R.id.star4);
        star5            = findViewById(R.id.star5);
        filterAll        = findViewById(R.id.filterAll);
        filterMine       = findViewById(R.id.filterMine);
        tvEmpty          = findViewById(R.id.tvEmpty);
        bar1             = findViewById(R.id.bar1);
        bar2             = findViewById(R.id.bar2);
        bar3             = findViewById(R.id.bar3);
        bar4             = findViewById(R.id.bar4);
        bar5             = findViewById(R.id.bar5);
        tvCount1         = findViewById(R.id.tvCount1);
        tvCount2         = findViewById(R.id.tvCount2);
        tvCount3         = findViewById(R.id.tvCount3);
        tvCount4         = findViewById(R.id.tvCount4);
        tvCount5         = findViewById(R.id.tvCount5);
        btnSubmitReview  = findViewById(R.id.btnSubmitReview);
        cardWriteReview  = findViewById(R.id.cardWriteReview);
        progressBar      = findViewById(R.id.progressBar);
        rvReviews        = findViewById(R.id.rvReviews);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new ReviewAdapter(this, displayList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(adapter);
        rvReviews.setNestedScrollingEnabled(false);
        adapter.setOnItemLongClickListener((review) -> {

            if (auth.getCurrentUser() == null) return;

            if (!review.getUserId().equals(auth.getCurrentUser().getUid())) {
                Toast.makeText(this, "You can only delete your own review", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete review?")
                    .setPositiveButton("Delete", (d, w) -> {
                        db.collection("reviews")
                                .document(review.getDocId())
                                .delete()
                                .addOnSuccessListener(unused -> loadReviews());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // ── Place Picker ──────────────────────────────────────────────────────────
    private void setupPlacePicker() {
        etPlace.setFocusable(false);
        etPlace.setClickable(true);
        etPlace.setOnClickListener(v -> showPlacePicker());
        findViewById(R.id.tilPlace).setOnClickListener(v -> showPlacePicker());
    }

    private void showPlacePicker() {
        new AlertDialog.Builder(this)
                .setTitle("Choose a Place to Review")
                .setItems(PLACES, (dialog, which) -> {
                    selectedPlace = PLACES[which];
                    etPlace.setText(selectedPlace);
                    etPlace.setError(null);
                })
                .show();
    }

    // ── Star Picker ───────────────────────────────────────────────────────────
    private void setupStarPicker() {
        TextView[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedRating = rating;
                highlightStars(rating);
                tvRatingLabel.setText(RATING_LABELS[rating]);
                tvRatingLabel.setTextColor(Color.parseColor("#F4B942"));
            });
        }
    }

    private void highlightStars(int count) {
        TextView[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            final int index = i;

            stars[i].setTextColor(i < count
                    ? Color.parseColor("#F4B942")
                    : Color.parseColor("#333333"));

            if (i < count) {
                stars[i].animate().scaleX(1.2f).scaleY(1.2f).setDuration(80)
                        .withEndAction(() ->
                                stars[index].animate()
                                        .scaleX(1f).scaleY(1f).setDuration(100).start())
                        .start();
            }
        }
    }

    // ── Filter chips ──────────────────────────────────────────────────────────
    private void setupFilterChips() {
        filterAll.setOnClickListener(v -> {
            showingMineOnly = false;
            filterAll.setBackgroundResource(R.drawable.bg_category_chip_selected);
            filterAll.setTextColor(Color.parseColor("#0A1628"));
            filterMine.setBackgroundResource(R.drawable.bg_category_chip);
            filterMine.setTextColor(Color.parseColor("#AAAAAA"));
            applyFilter();
        });

        filterMine.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, "Log in to see your reviews", Toast.LENGTH_SHORT).show();
                return;
            }
            showingMineOnly = true;
            filterMine.setBackgroundResource(R.drawable.bg_category_chip_selected);
            filterMine.setTextColor(Color.parseColor("#0A1628"));
            filterAll.setBackgroundResource(R.drawable.bg_category_chip);
            filterAll.setTextColor(Color.parseColor("#AAAAAA"));
            applyFilter();
        });
    }

    private void applyFilter() {
        displayList.clear();
        displayList.addAll(showingMineOnly ? myReviews : allReviews);
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Click Listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    // ── Submit Review ─────────────────────────────────────────────────────────
    private void submitReview() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to submit a review", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPlace.isEmpty()) {
            Toast.makeText(this, "Please choose a place to review", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a star rating ⭐", Toast.LENGTH_SHORT).show();
            return;
        }
        String reviewText = Objects.requireNonNull(etReview.getText()).toString().trim();
        if (reviewText.isEmpty()) {
            etReview.setError("Please write your review");
            etReview.requestFocus();
            return;
        }

        btnSubmitReview.setEnabled(false);
        btnSubmitReview.setText("Submitting…");

        String uid  = auth.getCurrentUser().getUid();
        String name = sessionManager.getSavedName();

        // Sanitise place name for Firestore document ID
        String placeId = selectedPlace
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");

        Map<String, Object> data = new HashMap<>();
        data.put("userId",     uid);
        data.put("userName",   name);
        data.put("placeName",  selectedPlace);
        data.put("placeId",    placeId);
        data.put("reviewText", reviewText);
        data.put("rating",     (float) selectedRating);
        data.put("createdAt",  com.google.firebase.Timestamp.now());

        db.collection("reviews")
                .add(data)
                .addOnSuccessListener(ref -> {
                    btnSubmitReview.setEnabled(true);
                    btnSubmitReview.setText("Submit Review");
                    Toast.makeText(this,
                            "Review submitted! Thank you 🙏", Toast.LENGTH_SHORT).show();
                    resetForm();
                    loadReviews();          // refresh list
                })
                .addOnFailureListener(e -> {
                    btnSubmitReview.setEnabled(true);
                    btnSubmitReview.setText("Submit Review");
                    Toast.makeText(this,
                            "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetForm() {
        etPlace.setText("");
        etReview.setText("");
        selectedPlace  = "";
        selectedRating = 0;
        highlightStars(0);
        tvRatingLabel.setText("Tap a star to rate");
        tvRatingLabel.setTextColor(Color.parseColor("#88AAC0CC"));
    }

    // ── Load Reviews from Firestore ───────────────────────────────────────────
    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);

                    allReviews.clear();
                    myReviews.clear();

                    String myUid = auth.getCurrentUser() != null
                            ? auth.getCurrentUser().getUid() : "";

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Review r = doc.toObject(Review.class);
                        r.setDocId(doc.getId());
                        allReviews.add(r);
                        if (r.getUserId() != null && r.getUserId().equals(myUid)) {
                            myReviews.add(r);
                        }
                    }

                    applyFilter();
                    updateSummaryBanner();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Failed to load reviews: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ── Summary Banner ────────────────────────────────────────────────────────
    private void updateSummaryBanner() {
        if (allReviews.isEmpty()) {
            tvAvgRating.setText("—");
            tvReviewCount.setText("0 reviews");
            updateRatingBars(0, 0, 0, 0, 0, 0);
            buildAvgStars(0);
            return;
        }

        int[] counts = new int[6]; // index 1-5
        float total  = 0;
        for (Review r : allReviews) {
            int rating = Math.round(r.getRating());
            if (rating >= 1 && rating <= 5) counts[rating]++;
            total += r.getRating();
        }

        float avg = total / allReviews.size();
        tvAvgRating.setText(String.format(Locale.getDefault(), "%.1f", avg));
        tvReviewCount.setText(allReviews.size() + " review" + (allReviews.size() == 1 ? "" : "s"));
        buildAvgStars(avg);

        int max = 1;
        for (int i = 1; i <= 5; i++) max = Math.max(max, counts[i]);
        updateRatingBars(counts[5], counts[4], counts[3], counts[2], counts[1], max);
    }

    private void updateRatingBars(int c5, int c4, int c3, int c2, int c1, int max) {
        float maxF = Math.max(max, 1);
        scaleBar(bar5, c5 / maxF); tvCount5.setText(String.valueOf(c5));
        scaleBar(bar4, c4 / maxF); tvCount4.setText(String.valueOf(c4));
        scaleBar(bar3, c3 / maxF); tvCount3.setText(String.valueOf(c3));
        scaleBar(bar2, c2 / maxF); tvCount2.setText(String.valueOf(c2));
        scaleBar(bar1, c1 / maxF); tvCount1.setText(String.valueOf(c1));
    }

    private void scaleBar(View bar, float fraction) {
        // Each bar lives in a horizontal LinearLayout with weight=1
        // Animate via scaleX from its parent's width fraction
        bar.setScaleX(fraction == 0 ? 0.04f : fraction);
        bar.setPivotX(0f);
    }

    private void buildAvgStars(float avg) {
        llAvgStars.removeAllViews();
        for (int i = 1; i <= 5; i++) {
            TextView star = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(1);
            star.setLayoutParams(lp);
            star.setTextSize(13f);
            star.setText("★");
            star.setTextColor(i <= Math.round(avg)
                    ? Color.parseColor("#F4B942")
                    : Color.parseColor("#333333"));
            llAvgStars.addView(star);
        }
    }

    // ── Entry Animation ───────────────────────────────────────────────────────
    private void playEntryAnimation() {
        cardWriteReview.setAlpha(0f);
        cardWriteReview.setTranslationY(40f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(cardWriteReview, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(cardWriteReview, "translationY", 40f, 0f)
        );
        set.setDuration(400);
        set.setStartDelay(150);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
}