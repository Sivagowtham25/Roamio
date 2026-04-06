package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;

import com.example.roamio.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

public class TripActivity extends AppCompatActivity {

    // ── Destination list ──────────────────────────────────────────────────────
    private static final String[] DESTINATIONS = {
            // Tamil Nadu
            "Chennai", "Pondicherry", "Madurai", "Kumbakonam",
            "Mahabalipuram", "Srirangam", "Tenkasi", "Ooty", "Kodaikanal",
            "Rameswaram", "Kanyakumari", "Thanjavur", "Coimbatore",
            // Other popular
            "Goa", "Kerala", "Rajasthan", "Jaipur", "Manali",
            "Shimla", "Mumbai", "Delhi", "Varanasi", "Agra",
            "Darjeeling", "Mysore", "Hampi", "Coorg", "Andaman Islands"
    };

    // ── Keyword chips ─────────────────────────────────────────────────────────
    private static final String[] KEYWORDS = {
            "🥗 Vegetarian food", "🍖 Non-veg food", "🏖️ Beach activities",
            "🛕 Temple visits", "🧘 Yoga & wellness", "🏄 Water sports",
            "🛍️ Shopping", "🎭 Cultural shows", "🌿 Nature walks",
            "🏕️ Camping", "📸 Photography spots", "👨‍👩‍👧 Family-friendly",
            "💑 Honeymoon", "🎒 Budget travel", "✨ Luxury stay",
            "🚗 Road trip", "🚂 Train journey", "🐘 Wildlife safari"
    };

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputEditText etDestination, etTripName, etStartDate,
            etEndDate, etTravellers, etBudget, etNotes;
    private TextInputLayout   tilStartDate, tilEndDate;
    private MaterialButton    btnCreateTrip, btnPlanWithAI;
    private CardView          cardCreate, cardAI, cardAIResult;
    private LinearLayout      llAIResult, llKeywords;
    private TextView          tvAITitle;
    private ScrollView        scrollView;

    // ── Date state ────────────────────────────────────────────────────────────
    private int startDay, startMonth, startYear;
    private int endDay,   endMonth,   endYear;

    // ── Selected keywords ─────────────────────────────────────────────────────
    private final List<String> selectedKeywords = new ArrayList<>();

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_trip);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();
        setupDestinationDropdown();
        setupDatePickers();
        setupKeywordChips();
        setupClickListeners();
        playEntryAnimation();
    }

    private void bindViews() {
        etDestination = findViewById(R.id.etDestination);
        etTripName    = findViewById(R.id.etTripName);
        etStartDate   = findViewById(R.id.etStartDate);
        etEndDate     = findViewById(R.id.etEndDate);
        etTravellers  = findViewById(R.id.etTravellers);
        etBudget      = findViewById(R.id.etBudget);
        etNotes       = findViewById(R.id.etNotes);
        tilStartDate  = findViewById(R.id.tilStartDate);
        tilEndDate    = findViewById(R.id.tilEndDate);
        btnCreateTrip = findViewById(R.id.btnCreateTrip);
        btnPlanWithAI = findViewById(R.id.btnPlanWithAI);
        cardCreate    = findViewById(R.id.cardCreate);
        cardAI        = findViewById(R.id.cardAI);
        cardAIResult  = findViewById(R.id.cardAIResult);
        llAIResult    = findViewById(R.id.llAIResult);
        llKeywords    = findViewById(R.id.llKeywords);
        tvAITitle     = findViewById(R.id.tvAITitle);
        scrollView    = findViewById(R.id.scrollView);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ── Destination Dropdown ──────────────────────────────────────────────────
    private void setupDestinationDropdown() {
        etDestination.setFocusable(false);
        etDestination.setClickable(true);
        etDestination.setOnClickListener(v -> showDestinationPicker());
        findViewById(R.id.tilDestination).setOnClickListener(v -> showDestinationPicker());
    }

    private void showDestinationPicker() {
        new AlertDialog.Builder(this)
                .setTitle("Choose Destination")
                .setItems(DESTINATIONS, (dialog, which) -> {
                    etDestination.setText(DESTINATIONS[which]);
                    etDestination.setError(null);
                })
                .show();
    }

    // ── Date Pickers ──────────────────────────────────────────────────────────
    private void setupDatePickers() {
        etStartDate.setFocusable(false);
        etStartDate.setClickable(true);
        etStartDate.setOnClickListener(v -> showDatePicker(true));
        tilStartDate.setEndIconOnClickListener(v -> showDatePicker(true));

        etEndDate.setFocusable(false);
        etEndDate.setClickable(true);
        etEndDate.setOnClickListener(v -> showDatePicker(false));
        tilEndDate.setEndIconOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStart) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            if (isStart) {
                String date = String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year);
                etStartDate.setText(date);
                startDay = day; startMonth = month; startYear = year;
                // Invalidate end date if it's now out of range
                if (endYear > 0) {
                    Calendar s = Calendar.getInstance();
                    Calendar e = Calendar.getInstance();
                    s.set(year, month, day);
                    e.set(endYear, endMonth, endDay);
                    long diff = (e.getTimeInMillis() - s.getTimeInMillis()) / (1000L * 60 * 60 * 24);
                    if (diff < 0 || diff > 7) {
                        etEndDate.setText("");
                        endYear = 0;
                        Toast.makeText(this, "End date cleared — max 7-day trip", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (startYear == 0) {
                    Toast.makeText(this, "Please select a start date first", Toast.LENGTH_SHORT).show();
                    return;
                }
                Calendar s = Calendar.getInstance();
                Calendar chosen = Calendar.getInstance();
                s.set(startYear, startMonth, startDay);
                chosen.set(year, month, day);
                long diffDays = (chosen.getTimeInMillis() - s.getTimeInMillis()) / (1000L * 60 * 60 * 24);
                if (diffDays < 0) {
                    Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (diffDays > 7) {
                    chosen.setTimeInMillis(s.getTimeInMillis() + (7L * 24 * 60 * 60 * 1000));
                    Toast.makeText(this, "Maximum 7-day trip — end date adjusted", Toast.LENGTH_LONG).show();
                }
                int ey = chosen.get(Calendar.YEAR);
                int em = chosen.get(Calendar.MONTH);
                int ed = chosen.get(Calendar.DAY_OF_MONTH);
                etEndDate.setText(String.format(Locale.US, "%02d/%02d/%04d", ed, em + 1, ey));
                endDay = ed; endMonth = em; endYear = ey;
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ── Keyword Chips ─────────────────────────────────────────────────────────
    private void setupKeywordChips() {
        llKeywords.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < KEYWORDS.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rp.bottomMargin = dp(8);
                row.setLayoutParams(rp);
                row.setWeightSum(2f);
                llKeywords.addView(row);
            }

            final String keyword = KEYWORDS[i];
            TextView chip = new TextView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 2 == 0) cp.setMarginEnd(dp(6));
            chip.setLayoutParams(cp);
            chip.setText(keyword);
            chip.setTextSize(12f);
            chip.setPadding(dp(10), dp(10), dp(10), dp(10));
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setBackground(getDrawable(R.drawable.bg_trip_chip));
            chip.setTextColor(Color.parseColor("#88AAC0CC"));
            chip.setClickable(true);
            chip.setFocusable(true);

            chip.setOnClickListener(v -> {
                if (selectedKeywords.contains(keyword)) {
                    selectedKeywords.remove(keyword);
                    chip.setBackground(getDrawable(R.drawable.bg_trip_chip));
                    chip.setTextColor(Color.parseColor("#88AAC0CC"));
                } else {
                    selectedKeywords.add(keyword);
                    chip.setBackgroundColor(Color.parseColor("#2000C9B1"));
                    chip.setTextColor(Color.parseColor("#00C9B1"));
                }
                updateNotesFromKeywords();
            });

            if (row != null) row.addView(chip);
        }
    }

    private void updateNotesFromKeywords() {
        if (selectedKeywords.isEmpty()) {
            etNotes.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedKeywords.size(); i++) {
            String clean = selectedKeywords.get(i)
                    .replaceAll("[^\\p{L}\\p{N} &]", "").trim();
            sb.append(clean);
            if (i < selectedKeywords.size() - 1) sb.append(", ");
        }
        etNotes.setText(sb.toString());
    }

    // ── Click Listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnCreateTrip.setOnClickListener(v -> {
            if (validateForm()) saveTrip();
        });
        btnPlanWithAI.setOnClickListener(v -> {
            if (validateForm()) generateAIPlan();
        });
    }

    // ── Validation ────────────────────────────────────────────────────────────
    private boolean validateForm() {
        String destination = Objects.requireNonNull(etDestination.getText()).toString().trim();
        String tripName    = Objects.requireNonNull(etTripName.getText()).toString().trim();
        String startDate   = Objects.requireNonNull(etStartDate.getText()).toString().trim();
        String endDate     = Objects.requireNonNull(etEndDate.getText()).toString().trim();
        String travellers  = Objects.requireNonNull(etTravellers.getText()).toString().trim();

        if (destination.isEmpty()) {
            Toast.makeText(this, "Please choose a destination", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (tripName.isEmpty()) {
            etTripName.setError("Enter a trip name");
            etTripName.requestFocus();
            return false;
        }
        if (startDate.isEmpty()) {
            Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (endDate.isEmpty()) {
            Toast.makeText(this, "Please select an end date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (travellers.isEmpty()) {
            etTravellers.setError("Enter number of travellers");
            etTravellers.requestFocus();
            return false;
        }

        // ── Date range validation ─────────────────────────────────────────────
        if (startYear > 0 && endYear > 0) {
            Calendar s = Calendar.getInstance();
            Calendar e = Calendar.getInstance();
            s.set(startYear, startMonth, startDay);
            e.set(endYear, endMonth, endDay);
            long diffDays = (e.getTimeInMillis() - s.getTimeInMillis()) / (1000L * 60 * 60 * 24);
            if (diffDays < 0) {
                Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (diffDays > 7) {
                Toast.makeText(this, "Maximum trip duration is 7 days", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // ── Budget per person warning (non-blocking) ──────────────────────────
        String budgetStr = Objects.requireNonNull(etBudget.getText()).toString().trim();
        if (!budgetStr.isEmpty()) {
            try {
                int totalBudget  = Integer.parseInt(budgetStr);
                int numPeople    = Integer.parseInt(travellers);
                int tripDays     = 1;
                if (startYear > 0 && endYear > 0) {
                    Calendar s = Calendar.getInstance();
                    Calendar e = Calendar.getInstance();
                    s.set(startYear, startMonth, startDay);
                    e.set(endYear, endMonth, endDay);
                    tripDays = Math.max(1, (int)((e.getTimeInMillis() - s.getTimeInMillis()) / (1000L * 60 * 60 * 24)));
                }
                if (numPeople > 0) {
                    int perPersonPerDay = totalBudget / (numPeople * tripDays);
                    if (perPersonPerDay < 500) {
                        Toast.makeText(this,
                            "⚠️ Budget is very low — ₹" + perPersonPerDay + "/person/day. Consider revising.",
                            Toast.LENGTH_LONG).show();
                    } else if (perPersonPerDay > 15000) {
                        Toast.makeText(this,
                            "✨ Premium budget — ₹" + String.format(Locale.US, "%,d", perPersonPerDay) + "/person/day",
                            Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        return true;
    }

    // ── Save Trip to Firestore ────────────────────────────────────────────────
    private void saveTrip() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to save trips", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid         = auth.getCurrentUser().getUid();
        String destination = Objects.requireNonNull(etDestination.getText()).toString().trim();
        String tripName    = Objects.requireNonNull(etTripName.getText()).toString().trim();
        String startDate   = Objects.requireNonNull(etStartDate.getText()).toString().trim();
        String endDate     = Objects.requireNonNull(etEndDate.getText()).toString().trim();
        String travellers  = Objects.requireNonNull(etTravellers.getText()).toString().trim();
        String budget      = Objects.requireNonNull(etBudget.getText()).toString().trim();
        String notes       = Objects.requireNonNull(etNotes.getText()).toString().trim();

        btnCreateTrip.setEnabled(false);
        btnCreateTrip.setText("Saving...");

        Map<String, Object> trip = new HashMap<>();
        trip.put("destination", destination);
        trip.put("tripName",    tripName);
        trip.put("startDate",   startDate);
        trip.put("endDate",     endDate);
        trip.put("travellers",  travellers);
        trip.put("budget",      budget);
        trip.put("notes",       notes);
        trip.put("keywords",    new ArrayList<>(selectedKeywords));
        trip.put("createdAt",   com.google.firebase.Timestamp.now());

        // Saved under: users/{uid}/trips/{auto-id}
        db.collection("users").document(uid)
                .collection("trips")
                .add(trip)
                .addOnSuccessListener(ref -> {
                    btnCreateTrip.setEnabled(true);
                    btnCreateTrip.setText("Create Trip");
                    // Navigate straight to itinerary planning
                    Intent intent = new Intent(this, ItineraryActivity.class);
                    intent.putExtra(ItineraryActivity.EXTRA_TRIP_ID,     ref.getId());
                    intent.putExtra(ItineraryActivity.EXTRA_TRIP_NAME,   tripName);
                    intent.putExtra(ItineraryActivity.EXTRA_DESTINATION, destination);
                    intent.putExtra(ItineraryActivity.EXTRA_START_DATE,  startDate);
                    intent.putExtra(ItineraryActivity.EXTRA_END_DATE,    endDate);
                    intent.putExtra(ItineraryActivity.EXTRA_TRAVELLERS,  travellers);
                    intent.putExtra(ItineraryActivity.EXTRA_BUDGET,      budget);
                    intent.putExtra(ItineraryActivity.EXTRA_KEYWORDS,    notes);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_up, R.anim.fade_out);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateTrip.setEnabled(true);
                    btnCreateTrip.setText("Create Trip");
                    Toast.makeText(this,
                            "Failed to save: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── AI Plan Generator ─────────────────────────────────────────────────────
    private void generateAIPlan() {
        String destination = Objects.requireNonNull(etDestination.getText()).toString().trim();
        String budget      = Objects.requireNonNull(etBudget.getText()).toString().trim();
        String travellers  = Objects.requireNonNull(etTravellers.getText()).toString().trim();
        String notes       = Objects.requireNonNull(etNotes.getText()).toString().trim();

        int days = 3;
        if (startYear > 0 && endYear > 0) {
            Calendar s = Calendar.getInstance();
            Calendar e = Calendar.getInstance();
            s.set(startYear, startMonth, startDay);
            e.set(endYear, endMonth, endDay);
            long diffMs = e.getTimeInMillis() - s.getTimeInMillis();
            days = Math.min(7, Math.max(1, (int)(diffMs / (1000 * 60 * 60 * 24))));
        }

        List<String> plan = LocalAIPlanner.generatePlan(destination, days, budget, travellers, notes);
        showAIResult(destination, days, plan);
    }

    // ── Show AI Result — rendered as structured Day sections ─────────────────
    private void showAIResult(String destination, int days, List<String> plan) {
        cardAIResult.setVisibility(View.VISIBLE);
        tvAITitle.setText("✦  AI Preview · " + destination + " · " + days + " day" + (days > 1 ? "s" : ""));

        llAIResult.removeAllViews();

        for (String rawLine : plan) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("─") || line.startsWith("===")) continue;

            String lower = line.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
            boolean isDayHeading  = lower.matches("day \\d.*");
            boolean isTipsHeading = lower.equals("tips") || lower.startsWith("travel tip") || lower.startsWith("tip ");
            boolean isHeading     = isDayHeading || isTipsHeading
                    || line.startsWith("📅") || line.startsWith("💡");

            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            if (isHeading) {
                lp.topMargin    = dp(14);
                lp.bottomMargin = dp(4);
                tv.setTextColor(Color.parseColor(isTipsHeading ? "#00C9B1" : "#A78BFA"));
                tv.setTextSize(14f);
                tv.setTypeface(null, Typeface.BOLD);
            } else {
                lp.bottomMargin  = dp(4);
                lp.setMarginStart(dp(8));
                tv.setTextColor(Color.parseColor("#CCB8C4D4"));
                tv.setTextSize(13f);
                tv.setLineSpacing(2f, 1.2f);
            }

            tv.setLayoutParams(lp);
            tv.setText(line);
            llAIResult.addView(tv);
        }

        cardAIResult.setAlpha(0f);
        cardAIResult.setTranslationY(40f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(cardAIResult, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(cardAIResult, "translationY", 40f, 0f)
        );
        set.setDuration(400);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();

        scrollView.postDelayed(() ->
                scrollView.smoothScrollTo(0, cardAIResult.getTop()), 300);
    }

    // ── Entry Animation ───────────────────────────────────────────────────────
    private void playEntryAnimation() {
        View[] cards = {cardCreate, cardAI};
        for (int i = 0; i < cards.length; i++) {
            cards[i].setAlpha(0f);
            cards[i].setTranslationY(50f);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(cards[i], "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(cards[i], "translationY", 50f, 0f)
            );
            set.setDuration(400);
            set.setStartDelay(120L * i);
            set.setInterpolator(new DecelerateInterpolator());
            set.start();
        }
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // LOCAL AI PLANNER — fully offline, no API key, no expiry
    // =========================================================================
    public static class LocalAIPlanner {

        public static List<String> generatePlan(String destination, int days,
                                                String budget, String travellers,
                                                String notes) {
            List<String> plan = new ArrayList<>();
            String dest = destination.toLowerCase();
            String budgetNote = budget.isEmpty() ? "" : "  •  Budget: ₹" + budget;
            String travelNote = travellers.isEmpty() ? "" : "  •  " + travellers + " traveller(s)";
            plan.add("📋  " + destination.toUpperCase() + budgetNote + travelNote);
            plan.add("─────────────────────────────────");

            for (int day = 1; day <= Math.min(days, 7); day++) {
                plan.add("📅  Day " + day);
                plan.addAll(getDayActivities(dest, day, days, notes));
                if (day < days) plan.add("");
            }

            if (!notes.isEmpty()) {
                plan.add("─────────────────────────────────");
                plan.add("📝  Your preferences: " + notes);
            }

            plan.add("─────────────────────────────────");
            plan.addAll(getTravelTips(dest, budget, notes));
            return plan;
        }

        private static List<String> getDayActivities(String dest, int day,
                                                     int totalDays, String notes) {
            List<String> acts = new ArrayList<>();
            boolean isVeg      = notes.toLowerCase().contains("vegetarian")
                    && !notes.toLowerCase().contains("non-veg");
            boolean wantsBeach = notes.toLowerCase().contains("beach");

            if (day == 1) {
                acts.add("🛬  Morning: Arrive & check in to hotel");
                acts.add(isVeg
                        ? "🥗  Afternoon: Try local vegetarian thali at a popular restaurant"
                        : "🍽️  Afternoon: Try local cuisine at a popular restaurant");
                if (dest.contains("madurai"))
                    acts.add("🛕  Evening: Meenakshi Amman Temple evening darshan");
                else if (dest.contains("kumbakonam"))
                    acts.add("🛕  Evening: Adi Kumbeswarar Temple visit");
                else if (dest.contains("mahabalipuram"))
                    acts.add("🌊  Evening: Shore Temple sunset view");
                else if (dest.contains("srirangam"))
                    acts.add("🛕  Evening: Ranganathaswamy Temple — one of the largest in India");
                else if (dest.contains("tenkasi"))
                    acts.add("🌊  Evening: Courtallam waterfalls visit");
                else if (dest.contains("rameswaram"))
                    acts.add("🛕  Evening: Ramanathaswamy Temple corridor walk");
                else if (dest.contains("kanyakumari"))
                    acts.add("🌅  Evening: Vivekananda Rock Memorial sunset view");
                else if (dest.contains("thanjavur"))
                    acts.add("🛕  Evening: Brihadeeswarar Temple light show");
                else if (dest.contains("ooty") || dest.contains("kodaikanal"))
                    acts.add("🌱  Evening: Walk through tea/coffee plantations");
                else if (dest.contains("goa"))
                    acts.add("🌅  Evening: Sunset at Calangute or Baga Beach");
                else if (dest.contains("kerala"))
                    acts.add("🌿  Evening: Stroll along the backwaters");
                else if (dest.contains("rajasthan") || dest.contains("jaipur"))
                    acts.add("🏰  Evening: Amer Fort light & sound show");
                else if (dest.contains("mumbai"))
                    acts.add("🌃  Evening: Marine Drive & Gateway of India walk");
                else if (dest.contains("delhi"))
                    acts.add("🕌  Evening: Connaught Place & India Gate");
                else if (dest.contains("pondicherry") || dest.contains("puducherry"))
                    acts.add("🌊  Evening: Promenade Beach & French Quarter stroll");
                else if (dest.contains("chennai") || dest.contains("madras"))
                    acts.add("🏖️  Evening: Marina Beach sunset walk");
                else if (dest.contains("varanasi"))
                    acts.add("🕯️  Evening: Ganga Aarti at Dashashwamedh Ghat");
                else if (dest.contains("manali") || dest.contains("shimla"))
                    acts.add("🏔️  Evening: Mall Road walk & local café");
                else
                    acts.add("🗺️  Evening: Explore the local market area");
                return acts;
            }

            if (day == totalDays) {
                acts.add("🛍️  Morning: Last-minute souvenir shopping");
                acts.add(isVeg
                        ? "🥗  Afternoon: Farewell vegetarian meal at a local favourite"
                        : "🍵  Afternoon: Farewell meal at a local favourite");
                acts.add("✈️  Evening: Depart — safe travels!");
                return acts;
            }

            // Middle days
            if (dest.contains("madurai")) {
                if (day % 2 == 0) {
                    acts.add("🛕  Morning: Meenakshi Amman Temple full tour");
                    acts.add("🍛  Afternoon: Kari Dosa & Jigarthanda at famous stalls");
                    acts.add("🏛️  Evening: Thirumalai Nayakkar Mahal light & sound show");
                } else {
                    acts.add("🌊  Morning: Vandiyur Mariamman Teppakulam tank visit");
                    acts.add("🎨  Afternoon: Madurai craft bazaar — silk & bronze");
                    acts.add("🛕  Evening: Azhakar Kovil hillside temple");
                }
            } else if (dest.contains("kumbakonam")) {
                if (day % 2 == 0) {
                    acts.add("🛕  Morning: Sarangapani & Kumbeswarar temples tour");
                    acts.add("🎭  Afternoon: Traditional bronze casting workshop visit");
                    acts.add("🌿  Evening: Boat ride on Cauvery river");
                } else {
                    acts.add("🛕  Morning: Darasuram Airavatesvara Temple — UNESCO site");
                    acts.add("🍯  Afternoon: Famous Kumbakonam filter coffee & sweets");
                    acts.add("🏛️  Evening: Mahamaham tank — largest sacred tank in TN");
                }
            } else if (dest.contains("mahabalipuram")) {
                if (day % 2 == 0) {
                    acts.add("🏛️  Morning: Five Rathas & Arjuna's Penance rock carvings");
                    acts.add("🐟  Afternoon: Fresh seafood lunch at beach shack");
                    acts.add("🌊  Evening: Shore Temple & beach walk at sunset");
                } else {
                    acts.add("🎨  Morning: Stone sculpture workshop");
                    acts.add(wantsBeach
                            ? "🏄  Afternoon: Swimming & water sports at beach"
                            : "🐊  Afternoon: Crocodile Bank & snake park visit");
                    acts.add("📸  Evening: Golden hour photography at Tiger Cave");
                }
            } else if (dest.contains("srirangam")) {
                if (day % 2 == 0) {
                    acts.add("🛕  Morning: Ranganathaswamy Temple inner sanctum darshan");
                    acts.add("🍛  Afternoon: Traditional Iyengar thali lunch");
                    acts.add("🌿  Evening: Cauvery river ghat walk");
                } else {
                    acts.add("🛕  Morning: Jambukeswarar Temple — Pancha Bhuta Stalas");
                    acts.add("🏛️  Afternoon: Rock Fort Temple, Trichy — 83m climb");
                    acts.add("🎭  Evening: Classical Carnatic music performance");
                }
            } else if (dest.contains("tenkasi")) {
                if (day % 2 == 0) {
                    acts.add("🌊  Morning: Courtallam main falls — Peraruvi");
                    acts.add("🍃  Afternoon: Five Falls & Old Courtallam waterfalls");
                    acts.add("🛕  Evening: Kasi Viswanathar Temple darshan");
                } else {
                    acts.add("🌿  Morning: Papanasam dam & reservoir visit");
                    acts.add("🎋  Afternoon: Western Ghats forest trekking");
                    acts.add("🦋  Evening: Butterfly garden & local nature walk");
                }
            } else if (dest.contains("rameswaram")) {
                if (day % 2 == 0) {
                    acts.add("🛕  Morning: 22 sacred theerthams (wells) inside temple");
                    acts.add("🐟  Afternoon: Fresh fish lunch at seaside restaurant");
                    acts.add("🌅  Evening: Pamban Bridge & Dhanushkodi sunset");
                } else {
                    acts.add("🏝️  Morning: Dhanushkodi ghost town exploration");
                    acts.add("🚢  Afternoon: Adam's Bridge (Ram Setu) viewpoint");
                    acts.add("🛕  Evening: Gandhamadana Parvatham — Ram's footprints");
                }
            } else if (dest.contains("kanyakumari")) {
                if (day % 2 == 0) {
                    acts.add("🌅  Morning: Sunrise at the tip of India — 3 oceans meet");
                    acts.add("⛵  Afternoon: Ferry to Vivekananda Rock Memorial");
                    acts.add("🛕  Evening: Kumari Amman Temple darshan");
                } else {
                    acts.add("🌊  Morning: Sunset Point & Thiruvalluvar Statue view");
                    acts.add("🏛️  Afternoon: Gandhi Memorial Mandapam");
                    acts.add("📸  Evening: Wax museum & local seafood dinner");
                }
            } else if (dest.contains("thanjavur")) {
                if (day % 2 == 0) {
                    acts.add("🛕  Morning: Brihadeeswarar Temple — UNESCO World Heritage");
                    acts.add("🎨  Afternoon: Thanjavur painting & Tanjore doll workshop");
                    acts.add("🏛️  Evening: Saraswathi Mahal Library & Royal Palace");
                } else {
                    acts.add("🛕  Morning: Gangaikonda Cholapuram temple");
                    acts.add("🍚  Afternoon: Traditional Chettiar cuisine experience");
                    acts.add("🎭  Evening: Bharatanatyam classical dance performance");
                }
            } else if (dest.contains("ooty")) {
                if (day % 2 == 0) {
                    acts.add("🚂  Morning: Nilgiri Mountain Railway toy train ride");
                    acts.add("🌹  Afternoon: Government Rose Garden & Botanical Garden");
                    acts.add("🌅  Evening: Doddabetta Peak — highest in Nilgiris");
                } else {
                    acts.add("🫖  Morning: Tea factory tour & tasting");
                    acts.add("🚣  Afternoon: Ooty Lake boating");
                    acts.add("🛍️  Evening: Ooty homemade chocolates & local market");
                }
            } else if (dest.contains("kodaikanal")) {
                if (day % 2 == 0) {
                    acts.add("🚣  Morning: Kodai Lake cycling & boating");
                    acts.add("🌲  Afternoon: Coaker's Walk & Bryant Park");
                    acts.add("🌅  Evening: Pillar Rocks & Green Valley View");
                } else {
                    acts.add("💧  Morning: Silver Cascade & Bear Shola Falls");
                    acts.add("🍫  Afternoon: Homemade chocolate shops");
                    acts.add("🌌  Evening: Star gazing at Solar Observatory");
                }
            } else if (dest.contains("goa")) {
                if (day % 2 == 0) {
                    acts.add("🏄  Morning: Water sports at Anjuna Beach");
                    acts.add(isVeg ? "🥗  Afternoon: Veg café lunch in Assagao" : "🐟  Afternoon: Seafood lunch at a shack");
                    acts.add("🎵  Evening: Night market at Arpora");
                } else {
                    acts.add("⛪  Morning: Old Goa churches — Basilica of Bom Jesus");
                    acts.add("🥗  Afternoon: Café lunch in Panjim");
                    acts.add("🌊  Evening: Sunset cruise on Mandovi River");
                }
            } else if (dest.contains("kerala")) {
                if (day % 2 == 0) {
                    acts.add("🛥️  Morning: Houseboat ride through Alleppey backwaters");
                    acts.add(isVeg ? "🥗  Afternoon: Kerala Sadya thali (veg feast)" : "🍛  Afternoon: Kerala Sadya thali lunch");
                    acts.add("💆  Evening: Ayurvedic massage & wellness session");
                } else {
                    acts.add("🐘  Morning: Periyar Wildlife Sanctuary visit");
                    acts.add("🫖  Afternoon: Munnar tea estate tour");
                    acts.add("🎭  Evening: Kathakali dance performance");
                }
            } else if (dest.contains("rajasthan") || dest.contains("jaipur")) {
                if (day % 2 == 0) {
                    acts.add("🏰  Morning: City Palace & Hawa Mahal tour");
                    acts.add("🐫  Afternoon: Camel ride & Jantar Mantar");
                    acts.add("🛒  Evening: Johari Bazaar — handicrafts & jewellery");
                } else {
                    acts.add("🏜️  Morning: Sam Sand Dunes jeep safari");
                    acts.add(isVeg ? "🥗  Afternoon: Dal baati churma — classic Rajasthani veg" : "🍢  Afternoon: Street food tour — dal baati churma");
                    acts.add("🌌  Evening: Stargazing in the desert");
                }
            } else if (dest.contains("manali") || dest.contains("himachal") || dest.contains("shimla")) {
                if (day % 2 == 0) {
                    acts.add("🎿  Morning: Rohtang Pass snow activities");
                    acts.add("☕  Afternoon: Café hopping in Old Manali");
                    acts.add("🏔️  Evening: Hadimba Temple & Solang Valley");
                } else {
                    acts.add("🚵  Morning: Mountain biking through apple orchards");
                    acts.add("🍏  Afternoon: Local apple farm visit & lunch");
                    acts.add("🌄  Evening: Sunset view from Naggar Castle");
                }
            } else if (dest.contains("pondicherry") || dest.contains("puducherry")) {
                if (day % 2 == 0) {
                    acts.add("🏛️  Morning: Auroville & Matrimandir visit");
                    acts.add(isVeg ? "🥗  Afternoon: Vegan café lunch in White Town" : "🥐  Afternoon: French Quarter café lunch");
                    acts.add("🌊  Evening: Rock Beach & Boulevard stroll");
                } else {
                    acts.add("🛕  Morning: Manakula Vinayagar Temple darshan");
                    acts.add("🎨  Afternoon: Art galleries in White Town");
                    acts.add("🍷  Evening: Rooftop dinner with sea view");
                }
            } else if (dest.contains("chennai") || dest.contains("madras")) {
                if (day % 2 == 0) {
                    acts.add("🛕  Morning: Kapaleeshwarar Temple & Mylapore walk");
                    acts.add(isVeg ? "🍜  Afternoon: Filter coffee & idli at Saravana Bhavan" : "🍜  Afternoon: Filter coffee & local lunch");
                    acts.add("🏖️  Evening: Elliot's Beach & local food stalls");
                } else {
                    acts.add("🏛️  Morning: Government Museum & Fort St. George");
                    acts.add("🛍️  Afternoon: T-Nagar shopping — silk sarees & more");
                    acts.add("🎭  Evening: Chennai music & dance performance");
                }
            } else if (dest.contains("mumbai")) {
                if (day % 2 == 0) {
                    acts.add("🏛️  Morning: Elephanta Caves ferry trip");
                    acts.add(isVeg ? "🥗  Afternoon: Veg thali at Colaba" : "🥘  Afternoon: Lunch in Colaba Causeway");
                    acts.add("🌃  Evening: Bandra-Worli Sea Link & street food");
                } else {
                    acts.add("🎬  Morning: Dharavi & Bollywood studio tour");
                    acts.add("🍱  Afternoon: Dabbawala experience & local lunch");
                    acts.add("🍻  Evening: Juhu Beach sunset & chaat");
                }
            } else if (dest.contains("varanasi")) {
                if (day % 2 == 0) {
                    acts.add("🚣  Morning: Sunrise boat ride on the Ganges");
                    acts.add(isVeg ? "🍮  Afternoon: Kashi chaat & lassi — 100% pure veg" : "🍮  Afternoon: Kashi chaat & lassi at Vishwanath Gali");
                    acts.add("🛕  Evening: Kashi Vishwanath Temple darshan");
                } else {
                    acts.add("🌄  Morning: Sarnath Buddhist ruins tour");
                    acts.add("🎨  Afternoon: Banarasi silk weaving workshop");
                    acts.add("🕯️  Evening: Dashaswamedh Ghat Aarti");
                }
            } else {
                acts.add("🗺️  Morning: Key landmark & heritage site visit");
                acts.add(isVeg ? "🥗  Afternoon: Local vegetarian cuisine experience" : "🍽️  Afternoon: Local cuisine experience");
                acts.add("🌆  Evening: Explore local market & evening stroll");
            }
            return acts;
        }

        private static List<String> getTravelTips(String dest, String budget, String notes) {
            List<String> tips = new ArrayList<>();
            tips.add("💡  Travel Tips");

            if (!budget.isEmpty()) {
                try {
                    int b = Integer.parseInt(budget.replaceAll("[^0-9]", ""));
                    if (b < 10000)      tips.add("💰  Budget tip: Use state transport buses & hostels");
                    else if (b < 30000) tips.add("💳  Mid-range: Mix of budget stays & occasional splurges");
                    else                tips.add("✨  Premium: Book hotels & experiences well in advance");
                } catch (Exception ignored) {}
            }

            if (dest.contains("madurai"))
            { tips.add("🌡️  October to March is ideal — summers are very hot");
                tips.add("🛕  Meenakshi Temple closes 12:30–4 PM — plan accordingly"); }
            else if (dest.contains("kumbakonam"))
            { tips.add("🛕  Mahamaham festival happens every 12 years — check dates");
                tips.add("🛺  Auto-rickshaws are the best way to hop between temples"); }
            else if (dest.contains("mahabalipuram"))
            { tips.add("☀️  October to February is the best time to visit");
                tips.add("🏛️  Buy an ASI ticket for all monuments — saves money"); }
            else if (dest.contains("srirangam"))
            { tips.add("🛕  Friday evening puja at Ranganathaswamy is spectacular");
                tips.add("🚗  Combine with Trichy Rock Fort for a full day"); }
            else if (dest.contains("tenkasi"))
            { tips.add("🌊  July to September is best for Courtallam waterfalls");
                tips.add("👗  Carry a change of clothes for the falls"); }
            else if (dest.contains("rameswaram"))
            { tips.add("🌊  April to October has calmer seas for Dhanushkodi");
                tips.add("👗  Modest dress required inside the temple"); }
            else if (dest.contains("kanyakumari"))
            { tips.add("🌅  Check sunrise/sunset times — they vary by season");
                tips.add("⛵  Ferry to Vivekananda Rock stops at 4 PM — go early"); }
            else if (dest.contains("thanjavur"))
            { tips.add("🛕  Brihadeeswarar best photographed at sunrise");
                tips.add("🎨  Buy authentic Tanjore paintings from certified shops only"); }
            else if (dest.contains("ooty"))
            { tips.add("🚂  Book Nilgiri toy train tickets well in advance");
                tips.add("🧥  Carry a light jacket — evenings are cool year-round"); }
            else if (dest.contains("kodaikanal"))
            { tips.add("🌧️  Avoid June to August — very foggy during monsoon");
                tips.add("🚗  Hire a local taxi for all viewpoints in one day"); }
            else if (dest.contains("goa"))
            { tips.add("🛵  Rent a scooter — best way to explore beaches");
                tips.add("☀️  November to February is the ideal season"); }
            else if (dest.contains("kerala"))
            { tips.add("🌧️  Avoid June–August (heavy monsoon)");
                tips.add("🛥️  Book houseboat at least 2 weeks in advance"); }
            else if (dest.contains("rajasthan"))
            { tips.add("🌡️  October to March is best — avoid peak summer heat");
                tips.add("🤝  Always bargain at local bazaars"); }
            else if (dest.contains("manali") || dest.contains("himachal") || dest.contains("shimla"))
            { tips.add("🧥  Carry warm clothes even in summer");
                tips.add("🚗  Book a cab for Rohtang Pass — roads are tricky"); }
            else if (dest.contains("pondicherry"))
            { tips.add("🚲  Rent a cycle to explore White Town & French Quarter");
                tips.add("🌊  November to February is the best time to visit"); }
            else if (dest.contains("chennai"))
            { tips.add("🌡️  November to February is the most pleasant season");
                tips.add("🚇  Use Chennai Metro for quick city travel"); }
            else
            { tips.add("📱  Download offline maps before you travel");
                tips.add("🎫  Book popular attractions online to skip queues"); }

            if (notes.toLowerCase().contains("vegetarian")
                    && !notes.toLowerCase().contains("non-veg"))
                tips.add("🥗  Look for 'Pure Veg' restaurants — widely available in South India");
            if (notes.toLowerCase().contains("family"))
                tips.add("👨‍👩‍👧  Book ground floor or accessible rooms for elderly family members");
            if (notes.toLowerCase().contains("honeymoon"))
                tips.add("💑  Request couple decorations & candlelight dinner at hotel in advance");

            tips.add("🏥  Carry basic medication & travel insurance");
            return tips;
        }
    }
}