package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class TripActivity extends AppCompatActivity {

    // ── Views — Create Trip ───────────────────────────────────────────────────
    private TextInputEditText etDestination, etTripName, etStartDate,
            etEndDate, etTravellers, etBudget, etNotes;
    private TextInputLayout   tilStartDate, tilEndDate;
    private MaterialButton    btnCreateTrip, btnPlanWithAI;
    private CardView          cardCreate, cardAI;
    private ScrollView        scrollView;

    // ── Views — AI Result ─────────────────────────────────────────────────────
    private CardView     cardAIResult;
    private LinearLayout llAIResult;
    private TextView     tvAITitle;

    // ── Date state ────────────────────────────────────────────────────────────
    private int startDay, startMonth, startYear;
    private int endDay,   endMonth,   endYear;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_trip);

        bindViews();
        setupDatePickers();
        setupClickListeners();
        playEntryAnimation();
    }

    // ── Bind Views ────────────────────────────────────────────────────────────
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
        tvAITitle     = findViewById(R.id.tvAITitle);
        scrollView    = findViewById(R.id.scrollView);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
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
            String date = String.format("%02d/%02d/%04d", day, month + 1, year);
            if (isStart) {
                etStartDate.setText(date);
                startDay = day; startMonth = month; startYear = year;
            } else {
                etEndDate.setText(date);
                endDay = day; endMonth = month; endYear = year;
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ── Click Listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {

        // Create Trip button — validates and saves
        btnCreateTrip.setOnClickListener(v -> {
            if (validateForm()) {
                String destination = Objects.requireNonNull(etDestination.getText()).toString().trim();
                String tripName    = Objects.requireNonNull(etTripName.getText()).toString().trim();
                Toast.makeText(this,
                        "Trip \"" + tripName + "\" to " + destination + " created! ✈️",
                        Toast.LENGTH_LONG).show();
            }
        });

        // Plan with AI button — generates a smart itinerary locally
        btnPlanWithAI.setOnClickListener(v -> {
            if (validateForm()) {
                generateAIPlan();
            }
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
            etDestination.setError("Enter a destination");
            etDestination.requestFocus();
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
        return true;
    }

    // ── AI Plan Generator (fully local, no API key needed) ────────────────────
    private void generateAIPlan() {
        String destination = Objects.requireNonNull(etDestination.getText()).toString().trim();
        String budget      = Objects.requireNonNull(etBudget.getText()).toString().trim();
        String travellers  = Objects.requireNonNull(etTravellers.getText()).toString().trim();
        String notes       = Objects.requireNonNull(etNotes.getText()).toString().trim();

        // Calculate trip duration in days
        int days = 3; // default
        if (startYear > 0 && endYear > 0) {
            Calendar s = Calendar.getInstance();
            Calendar e = Calendar.getInstance();
            s.set(startYear, startMonth, startDay);
            e.set(endYear, endMonth, endDay);
            long diffMs = e.getTimeInMillis() - s.getTimeInMillis();
            days = Math.max(1, (int)(diffMs / (1000 * 60 * 60 * 24)));
        }

        List<String> plan = LocalAIPlanner.generatePlan(destination, days, budget, travellers, notes);

        // Show result card
        showAIResult(destination, days, plan);
    }

    // ── Show AI Result ────────────────────────────────────────────────────────
    private void showAIResult(String destination, int days, List<String> plan) {
        cardAIResult.setVisibility(View.VISIBLE);
        tvAITitle.setText("✦  AI Plan · " + destination + " · " + days + " day" + (days > 1 ? "s" : ""));

        llAIResult.removeAllViews();
        for (String item : plan) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            tv.setLayoutParams(lp);
            tv.setText(item);
            tv.setTextColor(Color.parseColor("#EEF2FF"));
            tv.setTextSize(14f);
            tv.setLineSpacing(4f, 1f);
            llAIResult.addView(tv);
        }

        // Animate result card in
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

        // Scroll to result
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
    // Generates a day-by-day itinerary based on destination + duration + prefs
    // =========================================================================
    public static class LocalAIPlanner {

        public static List<String> generatePlan(String destination, int days,
                                                String budget, String travellers,
                                                String notes) {
            List<String> plan = new ArrayList<>();
            String dest = destination.toLowerCase();

            // Personalise header
            String budgetNote = budget.isEmpty() ? "" : "  •  Budget: ₹" + budget;
            String travelNote = travellers.isEmpty() ? "" : "  •  " + travellers + " traveller(s)";
            plan.add("📋  " + destination.toUpperCase() + budgetNote + travelNote);
            plan.add("─────────────────────────────────");

            // Generate day-by-day plan
            for (int day = 1; day <= Math.min(days, 7); day++) {
                plan.add("📅  Day " + day);
                plan.addAll(getDayActivities(dest, day, days));
                if (day < days) plan.add("");
            }

            // Add notes if provided
            if (!notes.isEmpty()) {
                plan.add("─────────────────────────────────");
                plan.add("📝  Your notes: " + notes);
            }

            // Travel tips
            plan.add("─────────────────────────────────");
            plan.addAll(getTravelTips(dest, budget));

            return plan;
        }

        private static List<String> getDayActivities(String dest, int day, int totalDays) {
            List<String> acts = new ArrayList<>();

            // Day 1 — Arrival + local orientation
            if (day == 1) {
                acts.add("🛬  Morning: Arrive & check in to hotel");
                acts.add("🍽️  Afternoon: Try local cuisine at a popular restaurant");
                if (dest.contains("goa")) {
                    acts.add("🌅  Evening: Sunset at Calangute or Baga Beach");
                } else if (dest.contains("kerala")) {
                    acts.add("🌿  Evening: Stroll along the backwaters");
                } else if (dest.contains("rajasthan") || dest.contains("jaipur")) {
                    acts.add("🏰  Evening: Amer Fort light & sound show");
                } else if (dest.contains("mumbai")) {
                    acts.add("🌃  Evening: Marine Drive Gateway of India walk");
                } else if (dest.contains("delhi")) {
                    acts.add("🕌  Evening: Explore Connaught Place & India Gate");
                } else if (dest.contains("ooty") || dest.contains("coorg")) {
                    acts.add("🌱  Evening: Walk through tea/coffee plantations");
                } else if (dest.contains("pondicherry") || dest.contains("puducherry")) {
                    acts.add("🌊  Evening: Promenade Beach & French Quarter stroll");
                } else if (dest.contains("chennai") || dest.contains("madras")) {
                    acts.add("🏖️  Evening: Marina Beach sunset walk");
                } else if (dest.contains("varanasi")) {
                    acts.add("🕯️  Evening: Ganga Aarti ceremony at Dashashwamedh Ghat");
                } else if (dest.contains("manali") || dest.contains("shimla")) {
                    acts.add("🏔️  Evening: Mall Road walk & local café");
                } else {
                    acts.add("🗺️  Evening: Explore the local market area");
                }
                return acts;
            }

            // Last day — departure
            if (day == totalDays) {
                acts.add("🛍️  Morning: Last-minute souvenir shopping");
                acts.add("🍵  Afternoon: Farewell meal at a local favourite");
                acts.add("✈️  Evening: Depart — safe travels!");
                return acts;
            }

            // Middle days — destination-specific activities
            if (dest.contains("goa")) {
                if (day % 2 == 0) {
                    acts.add("🏄  Morning: Water sports at Anjuna Beach");
                    acts.add("🐟  Afternoon: Seafood lunch at a shack");
                    acts.add("🎵  Evening: Night market at Arpora");
                } else {
                    acts.add("⛪  Morning: Old Goa churches — Basilica of Bom Jesus");
                    acts.add("🥗  Afternoon: Café lunch in Panjim");
                    acts.add("🌊  Evening: Sunset cruise on Mandovi River");
                }
            } else if (dest.contains("kerala")) {
                if (day % 2 == 0) {
                    acts.add("🛥️  Morning: Houseboat ride through Alleppey backwaters");
                    acts.add("🍛  Afternoon: Kerala Sadya thali lunch");
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
                    acts.add("🛒  Evening: Johari Bazaar shopping for handicrafts");
                } else {
                    acts.add("🏜️  Morning: Sam Sand Dunes jeep safari");
                    acts.add("🍢  Afternoon: Street food tour — dal baati churma");
                    acts.add("🌌  Evening: Stargazing in the desert");
                }
            } else if (dest.contains("manali") || dest.contains("himachal")) {
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
                    acts.add("🥐  Afternoon: French Quarter café lunch");
                    acts.add("🌊  Evening: Rock Beach & Boulevard stroll");
                } else {
                    acts.add("🛕  Morning: Manakula Vinayagar Temple darshan");
                    acts.add("🎨  Afternoon: Art galleries in White Town");
                    acts.add("🍷  Evening: Rooftop dinner with sea view");
                }
            } else if (dest.contains("chennai") || dest.contains("madras")) {
                if (day % 2 == 0) {
                    acts.add("🛕  Morning: Kapaleeshwarar Temple & Mylapore walk");
                    acts.add("🍜  Afternoon: Filter coffee & idli at Saravana Bhavan");
                    acts.add("🏖️  Evening: Elliot's Beach & local food stalls");
                } else {
                    acts.add("🏛️  Morning: Government Museum & Fort St. George");
                    acts.add("🛍️  Afternoon: T-Nagar shopping — silk sarees & more");
                    acts.add("🎭  Evening: Chennai music & dance performance");
                }
            } else if (dest.contains("mumbai")) {
                if (day % 2 == 0) {
                    acts.add("🏛️  Morning: Elephanta Caves ferry trip");
                    acts.add("🥘  Afternoon: Lunch in Colaba Causeway");
                    acts.add("🌃  Evening: Bandra-Worli Sea Link & street food");
                } else {
                    acts.add("🎬  Morning: Dharavi & Bollywood studio tour");
                    acts.add("🍱  Afternoon: Dabbawala experience & local lunch");
                    acts.add("🍻  Evening: Juhu Beach sunset & chaat");
                }
            } else if (dest.contains("varanasi")) {
                if (day % 2 == 0) {
                    acts.add("🚣  Morning: Sunrise boat ride on the Ganges");
                    acts.add("🍮  Afternoon: Kashi chaat & lassi at Vishwanath Gali");
                    acts.add("🛕  Evening: Kashi Vishwanath Temple darshan");
                } else {
                    acts.add("🌄  Morning: Sarnath Buddhist ruins tour");
                    acts.add("🎨  Afternoon: Banarasi silk weaving workshop");
                    acts.add("🕯️  Evening: Dashaswamedh Ghat Aarti");
                }
            } else {
                // Generic multi-day activities
                acts.add("🗺️  Morning: Key landmark & heritage site visit");
                acts.add("🍽️  Afternoon: Local cuisine experience");
                acts.add("🌆  Evening: Explore local market & nightlife");
            }

            return acts;
        }

        private static List<String> getTravelTips(String dest, String budget) {
            List<String> tips = new ArrayList<>();
            tips.add("💡  Travel Tips");

            // Budget-based tips
            if (!budget.isEmpty()) {
                try {
                    int b = Integer.parseInt(budget.replaceAll("[^0-9]", ""));
                    if (b < 10000) {
                        tips.add("💰  Budget tip: Use state transport buses & hostels to save");
                    } else if (b < 30000) {
                        tips.add("💳  Mid-range: Mix of budget stays & occasional splurges works well");
                    } else {
                        tips.add("✨  Premium: Book hotels & experiences in advance for best rates");
                    }
                } catch (Exception ignored) {}
            }

            // Destination-specific tips
            if (dest.contains("goa")) {
                tips.add("🛵  Rent a scooter — best way to explore beaches");
                tips.add("☀️  November to February is the ideal season");
            } else if (dest.contains("kerala")) {
                tips.add("🌧️  Avoid June–August (heavy monsoon)");
                tips.add("🛥️  Book houseboat at least 2 weeks in advance");
            } else if (dest.contains("rajasthan")) {
                tips.add("🌡️  October to March is best — avoid peak summer heat");
                tips.add("🤝  Always bargain at local bazaars");
            } else if (dest.contains("manali") || dest.contains("himachal")) {
                tips.add("🧥  Carry warm clothes even in summer");
                tips.add("🚗  Book a cab for Rohtang Pass — roads are tricky");
            } else if (dest.contains("pondicherry") || dest.contains("puducherry")) {
                tips.add("🚲  Rent a cycle to explore White Town & French Quarter");
                tips.add("🌊  November to February is the best time to visit");
            } else if (dest.contains("chennai")) {
                tips.add("🌡️  November to February is the most pleasant season");
                tips.add("🚇  Use Chennai Metro for quick city travel");
            } else {
                tips.add("📱  Download offline maps before you travel");
                tips.add("🎫  Book popular attractions online to skip queues");
            }

            tips.add("🏥  Carry basic medication & travel insurance");

            return tips;
        }
    }
}