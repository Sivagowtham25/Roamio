package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;

import com.example.roamio.BuildConfig;
import com.example.roamio.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ItineraryActivity extends AppCompatActivity {

    // ── Intent extra keys ────────────────────────────────────────────────────
    public static final String EXTRA_TRIP_ID     = "tripId";
    public static final String EXTRA_TRIP_NAME   = "tripName";
    public static final String EXTRA_DESTINATION = "destination";
    public static final String EXTRA_START_DATE  = "startDate";
    public static final String EXTRA_END_DATE    = "endDate";
    public static final String EXTRA_TRAVELLERS  = "travellers";
    public static final String EXTRA_BUDGET      = "budget";
    public static final String EXTRA_KEYWORDS    = "keywords";

    // Same URL as AiTripPlannerActivity
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:generateContent?key=";

    // Same system-prompt pattern as AiTripPlannerActivity (this is why it works there)
    private static final String SYSTEM_PROMPT =
            "You are Roamio AI, a mobile travel planner. Output ONLY structured itinerary text.\n\n" +
                    "ABSOLUTE RULES — no exceptions:\n" +
                    "1. NEVER write paragraphs. Every line must stand alone.\n" +
                    "2. Each activity line max 12 words.\n" +
                    "3. DO NOT greet, introduce yourself, or add commentary.\n" +
                    "4. Start IMMEDIATELY with 'Day 1 — [Theme]'.\n" +
                    "5. Each activity: 'HH:MM AM — emoji Short description'\n" +
                    "6. Generate EVERY day requested — do not skip or truncate.\n" +
                    "7. ALWAYS end with a 'Tips' section on its own line, followed by 4-5 short tips.\n" +
                    "8. DO NOT use markdown (no **, no ##, no ---, no backticks).\n" +
                    "9. Max 6 activities per day.\n";
    private static final String SYSTEM_REPLY =
            "Ready. I will generate every day requested. No paragraphs, short lines, emojis. Starting with Day 1. Ending with Tips.";

    // ── Trip data ─────────────────────────────────────────────────────────────
    private String tripId, tripName, destination, startDate, endDate,
            travellers, budget, keywords;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView          tvTripName, tvDestination, tvDates, tvTravellers,
            tvLoadingText;
    private LinearLayout      layoutOptions, layoutLoading, layoutManualInput,
            layoutItinerary, llDayCards;
    private TextInputEditText etManualItinerary;
    private MaterialButton    btnSaveManual, btnAdjust;

    // ── Stored itinerary text ─────────────────────────────────────────────────
    private String currentItineraryText = "";

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    // ── Network — same OkHttpClient as AiTripPlannerActivity ─────────────────
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_itinerary);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        tripId      = intent.getStringExtra(EXTRA_TRIP_ID);
        tripName    = intent.getStringExtra(EXTRA_TRIP_NAME);
        destination = intent.getStringExtra(EXTRA_DESTINATION);
        startDate   = intent.getStringExtra(EXTRA_START_DATE);
        endDate     = intent.getStringExtra(EXTRA_END_DATE);
        travellers  = intent.getStringExtra(EXTRA_TRAVELLERS);
        budget      = intent.getStringExtra(EXTRA_BUDGET);
        keywords    = intent.getStringExtra(EXTRA_KEYWORDS);

        bindViews();
        populateTripInfo();
        loadExistingItinerary();
    }

    // ── Bind views ────────────────────────────────────────────────────────────
    private void bindViews() {
        tvTripName        = findViewById(R.id.tvTripName);
        tvDestination     = findViewById(R.id.tvDestination);
        tvDates           = findViewById(R.id.tvDates);
        tvTravellers      = findViewById(R.id.tvTravellers);
        tvLoadingText     = findViewById(R.id.tvLoadingText);
        layoutOptions     = findViewById(R.id.layoutOptions);
        layoutLoading     = findViewById(R.id.layoutLoading);
        layoutManualInput = findViewById(R.id.layoutManualInput);
        layoutItinerary   = findViewById(R.id.layoutItinerary);
        llDayCards        = findViewById(R.id.llDayCards);
        etManualItinerary = findViewById(R.id.etManualItinerary);
        btnSaveManual     = findViewById(R.id.btnSaveManual);
        btnAdjust         = findViewById(R.id.btnAdjust);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.cardAiOption).setOnClickListener(v -> generateAiItinerary(null));
        findViewById(R.id.cardManualOption).setOnClickListener(v -> showManualInput());
        btnSaveManual.setOnClickListener(v -> saveManualItinerary());
        findViewById(R.id.btnRegenerate).setOnClickListener(v -> showConfirmRegenerate());
        btnAdjust.setOnClickListener(v -> showAdjustDialog());
    }

    // ── Populate trip info card ───────────────────────────────────────────────
    private void populateTripInfo() {
        tvTripName.setText(tripName != null ? tripName : "My Trip");
        tvDestination.setText("\ud83d\udccd " + (destination != null ? destination : "\u2014"));
        tvDates.setText("\ud83d\udcc5 " + (startDate != null ? startDate : "\u2014")
                + "  \u2192  " + (endDate != null ? endDate : "\u2014"));
        tvTravellers.setText("\ud83d\udc65 " + (travellers != null ? travellers + " traveller(s)" : "\u2014"));
    }

    // ── Load itinerary from Firestore ─────────────────────────────────────────
    private void loadExistingItinerary() {
        if (auth.getCurrentUser() == null || tripId == null) {
            showOptions();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("itinerary").document("plan")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String text = doc.getString("content");
                        if (text != null && !text.isEmpty()) {
                            currentItineraryText = text;
                            showItinerary(text);
                            return;
                        }
                    }
                    showOptions();
                })
                .addOnFailureListener(e -> showOptions());
    }

    // ── Show states ───────────────────────────────────────────────────────────

    private void showOptions() {
        layoutOptions.setVisibility(View.VISIBLE);
        layoutLoading.setVisibility(View.GONE);
        layoutManualInput.setVisibility(View.GONE);
        layoutItinerary.setVisibility(View.GONE);
        btnAdjust.setVisibility(View.GONE);
        animateIn(layoutOptions);
    }

    private void showManualInput() {
        layoutOptions.setVisibility(View.GONE);
        layoutManualInput.setVisibility(View.VISIBLE);
        animateIn(layoutManualInput);
    }

    private void showLoading(String msg) {
        layoutOptions.setVisibility(View.GONE);
        layoutManualInput.setVisibility(View.GONE);
        layoutItinerary.setVisibility(View.GONE);
        btnAdjust.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.VISIBLE);
        tvLoadingText.setText(msg);
    }

    // ── Manual save ───────────────────────────────────────────────────────────

    private void saveManualItinerary() {
        String text = Objects.requireNonNull(etManualItinerary.getText()).toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Please write your itinerary first", Toast.LENGTH_SHORT).show();
            return;
        }
        saveItineraryToFirestore(text, false);
    }

    // ── AI generation entry point ─────────────────────────────────────────────

    private void generateAiItinerary(String adjustmentNote) {
        showLoading(adjustmentNote != null
                ? "\u2728 Adjusting your itinerary..."
                : "\u2728 Generating your AI itinerary...");
        String prompt = buildGeminiPrompt(adjustmentNote);
        callGeminiApi(prompt);   // method name matches AiTripPlannerActivity
    }

    // ── Build the prompt ──────────────────────────────────────────────────────

    private String buildGeminiPrompt(String adjustmentNote) {
        Calendar now  = Calendar.getInstance();
        int month     = now.get(Calendar.MONTH) + 1;
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int hour      = now.get(Calendar.HOUR_OF_DAY);
        int year      = now.get(Calendar.YEAR);

        String[] months = {"","January","February","March","April","May","June",
                "July","August","September","October","November","December"};
        String[] days   = {"","Sunday","Monday","Tuesday","Wednesday",
                "Thursday","Friday","Saturday"};

        String season;
        if (month >= 3 && month <= 5)        season = "Summer (hot & dry, 35-45\u00b0C)";
        else if (month >= 6 && month <= 9)   season = "Monsoon (rainy season, 25-35\u00b0C)";
        else if (month >= 10 && month <= 11) season = "Post-monsoon (pleasant, 20-30\u00b0C)";
        else                                  season = "Winter (cool & pleasant, 15-25\u00b0C)";

        String timeOfDay;
        if (hour >= 5 && hour < 12)       timeOfDay = "morning";
        else if (hour >= 12 && hour < 17) timeOfDay = "afternoon";
        else if (hour >= 17 && hour < 21) timeOfDay = "evening";
        else                              timeOfDay = "night";

        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);

        int tripDays = 3;
        if (startDate != null && endDate != null) {
            try {
                String[] s = startDate.split("/");
                String[] e = endDate.split("/");
                Calendar sc = Calendar.getInstance();
                Calendar ec = Calendar.getInstance();
                sc.set(Integer.parseInt(s[2]), Integer.parseInt(s[1]) - 1, Integer.parseInt(s[0]));
                ec.set(Integer.parseInt(e[2]), Integer.parseInt(e[1]) - 1, Integer.parseInt(e[0]));
                tripDays = (int) ((ec.getTimeInMillis() - sc.getTimeInMillis()) / (1000L * 60 * 60 * 24));
                tripDays = tripDays + 1; // inclusive: end date counts as a travel day
                if (tripDays < 1) tripDays = 1;
                if (tripDays > 7) tripDays = 7;
            } catch (Exception ignored) {}
        }

        String travelMonth = months[month];
        if (startDate != null) {
            try {
                String[] s = startDate.split("/");
                int m = Integer.parseInt(s[1]);
                if (m >= 1 && m <= 12) travelMonth = months[m];
            } catch (Exception ignored) {}
        }

        String holidays = getHolidayNote(month, year);

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert Indian travel planner. Create a detailed, time-specific day-by-day itinerary.\n\n");

        sb.append("=== TRIP DETAILS ===\n");
        sb.append("Destination: ").append(destination).append("\n");
        sb.append("Trip Name: ").append(tripName).append("\n");
        sb.append("Dates: ").append(startDate).append(" to ").append(endDate)
                .append(" (").append(tripDays).append(" day(s))\n");
        sb.append("Travellers: ").append(travellers).append("\n");
        if (budget != null && !budget.isEmpty())
            sb.append("Budget: Rs.").append(budget).append("\n");
        if (keywords != null && !keywords.isEmpty())
            sb.append("Preferences & Interests: ").append(keywords).append("\n");

        sb.append("\n=== CONTEXT (factor these into the plan) ===\n");
        sb.append("Current date: ").append(days[dayOfWeek]).append(", ")
                .append(months[month]).append(" ").append(year).append("\n");
        sb.append("Current time: ").append(timeOfDay).append(" (").append(hour).append(":00)\n");
        sb.append("Is today a weekend: ").append(isWeekend ? "Yes" : "No").append("\n");
        sb.append("Season during travel month (").append(travelMonth).append("): ").append(season).append("\n");
        sb.append("Holidays & festivals in ").append(travelMonth).append(": ").append(holidays).append("\n");

        sb.append("\n=== OUTPUT FORMAT (the app parses this — follow exactly) ===\n");
        sb.append("Each day MUST start on its own line as: Day N \u2014 [Theme Title]\n");
        sb.append("Each activity MUST be on its own line as: HH:MM AM \u2014 [Activity description]\n");
        sb.append("The tips section MUST start on its own line as exactly: Tips\n");
        sb.append("DO NOT use markdown: no **, no ##, no ---, no ===\n");
        sb.append("DO NOT add any preamble or introduction before Day 1\n\n");
        sb.append("EXAMPLE FORMAT:\n");
        sb.append("Day 1 \u2014 Arrival & First Impressions\n");
        sb.append("07:00 AM \u2014 \ud83c\udf73 Breakfast at local café\n");
        sb.append("10:00 AM \u2014 \ud83c\udfe8 Check in to hotel, freshen up\n");
        sb.append("02:00 PM \u2014 \ud83c� Visit main attraction (entry: \u20b950, open 9AM\u20135PM)\n");
        sb.append("07:00 PM \u2014 \ud83c� Dinner at recommended local restaurant\n\n");
        sb.append("Day 2 \u2014 Culture & Heritage\n");
        sb.append("...\n\n");
        sb.append("Tips\n");
        sb.append("Best time to visit: October to March\n");
        sb.append("Carry cash for local market vendors\n\n");
        sb.append("=== CONTENT INSTRUCTIONS ===\n");
        sb.append("1. Generate EXACTLY ").append(tripDays).append(" day(s) — every day, no skipping\n");
        sb.append("2. Each activity line: 'HH:MM AM \u2014 emoji Short label' (max 12 words)\n");
        sb.append("3. NO paragraphs. NO sentences. ONE idea per line.\n");
        sb.append("4. Add entry cost & timing in brackets: (\u20b950, 9AM-5PM)\n");
        sb.append("5. Day 1 = arrival; Day ").append(tripDays).append(" = checkout/departure\n");
        sb.append("6. Mention season (\u2013").append(travelMonth).append(") or festival if relevant, inline\n");
        sb.append("7. Max 6 activities per day\n");
        sb.append("8. MANDATORY: last section must be exactly the word 'Tips' on its own line\n");
        sb.append("9. Under Tips: exactly 4-6 short one-line tips for ").append(travelMonth).append("\n");
        sb.append("10. DO NOT use markdown (**, ##, ---, backticks). No preamble before Day 1.\n");

        if (adjustmentNote != null && !adjustmentNote.isEmpty()) {
            sb.append("\n=== ADJUSTMENT REQUEST ===\n");
            sb.append(adjustmentNote).append("\n");
            sb.append("Revise keeping the trip dates, destination, and traveller count the same.\n");
            if (!currentItineraryText.isEmpty()) {
                sb.append("\nCURRENT ITINERARY TO ADJUST:\n").append(currentItineraryText);
            }
        }

        return sb.toString();
    }

    private String getHolidayNote(int month, int year) {
        switch (month) {
            case 1:  return "Republic Day (Jan 26), Pongal/Makar Sankranti (Jan 14)";
            case 2:  return "Maha Shivratri (varies), Valentine's Day (Feb 14)";
            case 3:  return "Holi (date varies), Ugadi, Gudi Padwa";
            case 4:  return "Ram Navami, Tamil New Year (Apr 14), Baisakhi, Good Friday";
            case 5:  return "Buddha Purnima (varies), Labour Day (May 1)";
            case 6:  return "Eid-ul-Adha may fall in June. Southwest Monsoon arrives.";
            case 7:  return "Guru Purnima, Muharram (varies). Monsoon in full swing.";
            case 8:  return "Independence Day (Aug 15), Janmashtami, Onam (Kerala, late Aug)";
            case 9:  return "Ganesh Chaturthi (major!), Navratri may begin late September";
            case 10: return "Navratri (9 nights), Dussehra/Vijayadashami, Diwali may fall in October";
            case 11: return "Diwali (biggest Indian festival, check exact date for " + year + "), Chhath Puja";
            case 12: return "Christmas (Dec 25), New Year Eve (Dec 31). Peak tourism season.";
            default: return "Check local festival calendar for " + year;
        }
    }

    // ── Gemini API call — exact same pattern as AiTripPlannerActivity ─────────
    private void callGeminiApi(String prompt) {
        try {
            // Build contents array with system user+model pair, then real request
            // This is the EXACT same structure used in AiTripPlannerActivity.callGeminiApi()
            JSONArray contents = new JSONArray();

            JSONObject sysUser = new JSONObject();
            sysUser.put("role", "user");
            sysUser.put("parts", new JSONArray()
                    .put(new JSONObject().put("text", SYSTEM_PROMPT)));
            contents.put(sysUser);

            JSONObject sysModel = new JSONObject();
            sysModel.put("role", "model");
            sysModel.put("parts", new JSONArray()
                    .put(new JSONObject().put("text", SYSTEM_REPLY)));
            contents.put(sysModel);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("parts", new JSONArray()
                    .put(new JSONObject().put("text", prompt)));
            contents.put(userMsg);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.8);
            genConfig.put("maxOutputTokens", 4096);

            JSONObject body = new JSONObject();
            body.put("contents", contents);
            body.put("generationConfig", genConfig);

            String apiKey = BuildConfig.GEMINI_API_KEY;
            if (apiKey.isEmpty()) {
                runOnUiThread(() -> {
                    showTyping(false);
                    Toast.makeText(this,
                            "Gemini API key not set. Add GEMINI_API_KEY to local.properties.",
                            Toast.LENGTH_LONG).show();
                    showOptions();
                });
                return;
            }

            Request request = new Request.Builder()
                    .url(GEMINI_URL + apiKey)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        showTyping(false);
                        Toast.makeText(ItineraryActivity.this,
                                "Connection failed. Check your internet.",
                                Toast.LENGTH_LONG).show();
                        showOptions();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    if (response.body() == null) {
                        runOnUiThread(() -> { showTyping(false); showOptions(); });
                        return;
                    }
                    String responseBody = response.body().string();
                    String result = parseGeminiResponse(responseBody);
                    runOnUiThread(() -> {
                        showTyping(false);
                        if (result.startsWith("Error:")) {
                            Toast.makeText(ItineraryActivity.this, result, Toast.LENGTH_LONG).show();
                            showOptions();
                        } else {
                            saveItineraryToFirestore(result, true);
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> { showTyping(false); showOptions(); });
        }
    }

    // Hides the loading layout — mirrors showTyping(false) naming from AiTripPlannerActivity
    private void showTyping(boolean show) {
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ── Parse Gemini JSON — identical to AiTripPlannerActivity ───────────────
    private String parseGeminiResponse(String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.has("error")) {
                String errMsg = root.getJSONObject("error").optString("message", "API error");
                return "Error: " + errMsg;
            }
            return root
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        } catch (Exception e) {
            return "I had trouble understanding that response. Please try again!";
        }
    }

    // ── Save to Firestore ─────────────────────────────────────────────────────
    private void saveItineraryToFirestore(String content, boolean isAi) {
        if (auth.getCurrentUser() == null || tripId == null) {
            currentItineraryText = content;
            showItinerary(content);
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("isAi", isAi);
        data.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("itinerary").document("plan")
                .set(data)
                .addOnSuccessListener(unused -> {
                    currentItineraryText = content;
                    showItinerary(content);
                    Toast.makeText(this, "Itinerary saved! \u2726", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    currentItineraryText = content;
                    showItinerary(content);
                    Toast.makeText(this, "Saved locally (sync failed)", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Render itinerary as expandable day cards ──────────────────────────────
    private void showItinerary(String content) {
        layoutOptions.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutManualInput.setVisibility(View.GONE);
        layoutItinerary.setVisibility(View.VISIBLE);
        btnAdjust.setVisibility(View.VISIBLE);

        llDayCards.removeAllViews();

        // ── 1. Split raw text into sections ──────────────────────────────
        // A new section starts when a line begins with "Day N" or looks like a Tips heading.
        java.util.List<String[]> sections = new java.util.ArrayList<>();
        // sections[i] = { headingLine, bodyText }

        String[] lines = content.split("\n");
        String   currentHeading = null;
        StringBuilder currentBody = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            boolean isHeading = isSectionHeading(line);

            if (isHeading) {
                if (currentHeading != null) {
                    sections.add(new String[]{ currentHeading, currentBody.toString().trim() });
                }
                currentHeading = line;
                currentBody = new StringBuilder();
            } else {
                if (currentHeading != null && !line.isEmpty()
                        && !line.startsWith("===") && !line.startsWith("---")) {
                    currentBody.append(line).append("\n");
                }
            }
        }
        // flush last section
        if (currentHeading != null) {
            sections.add(new String[]{ currentHeading, currentBody.toString().trim() });
        }

        // ── 2. Build one expandable card per section ──────────────────────
        int delay = 0;
        for (int i = 0; i < sections.size(); i++) {
            String heading  = sections.get(i)[0];
            String body     = sections.get(i)[1];
            boolean isTips  = heading.toLowerCase().contains("tip");
            boolean expandedByDefault = (i == 0); // Day 1 open, rest collapsed

            CardView card = buildExpandableCard(heading, body, isTips, expandedByDefault);
            llDayCards.addView(card);
            animateCard(card, delay);
            delay += 80;
        }

        animateIn(layoutItinerary);
        animateIn(btnAdjust);
    }

    /** Returns true if this line is a Day N / Tips section heading. */
    private boolean isSectionHeading(String line) {
        if (line.isEmpty()) return false;
        String clean = line.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
        // Matches "Day 1", "Day 1 — Title", "📅 Day 1", "Day 2 - Exploring", etc.
        if (clean.matches("day \\d.*")) return true;
        // Tips section — the AI may write "Tips", "Travel Tips", "Tip:", etc.
        if (clean.equals("tips") || clean.startsWith("tips ")
                || clean.startsWith("travel tips") || clean.startsWith("tip ")) return true;
        return false;
    }

    /** Builds a single expandable card: tappable header + collapsible body. */
    private CardView buildExpandableCard(
            String heading, String body, boolean isTips, boolean startExpanded) {

        String accentHex  = isTips ? "#00C9B1" : "#A78BFA";
        String cardBgHex  = isTips ? "#12007A5F" : "#14FFFFFF";
        String headerBgHex = isTips ? "#1A00C9B1" : "#1A6C63F7";

        // Outer card
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(14);
        card.setLayoutParams(cardLp);
        card.setRadius(dp(20));
        card.setCardElevation(dp(6));
        card.setCardBackgroundColor(Color.parseColor(cardBgHex));

        // Root vertical layout inside card
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        card.addView(root);

        // ── Header row (always visible) ───────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(14), dp(16));
        header.setBackgroundColor(Color.parseColor(headerBgHex));
        root.addView(header);

        // Emoji badge
        TextView tvEmoji = new TextView(this);
        tvEmoji.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        tvEmoji.setText(isTips ? "\uD83D\uDCA1" : "\uD83D\uDCC5");
        tvEmoji.setTextSize(18f);
        tvEmoji.setGravity(android.view.Gravity.CENTER);
        header.addView(tvEmoji);

        // Heading text
        TextView tvHeading = new TextView(this);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        hLp.setMarginStart(dp(10));
        tvHeading.setLayoutParams(hLp);
        // Clean up markdown symbols and extra punctuation from heading
        String cleanHeading = heading
                .replaceAll("[*_#]+", "")
                .replaceAll("📅", "")
                .replaceAll("💡", "")
                .trim();
        tvHeading.setText(cleanHeading);
        tvHeading.setTextColor(Color.parseColor(accentHex));
        tvHeading.setTextSize(15f);
        tvHeading.setTypeface(null, Typeface.BOLD);
        header.addView(tvHeading);

        // Chevron
        TextView tvChevron = new TextView(this);
        tvChevron.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tvChevron.setText(startExpanded ? "\u2303" : "\u2304"); // ⌃ / ⌄
        tvChevron.setTextColor(Color.parseColor(accentHex));
        tvChevron.setTextSize(18f);
        tvChevron.setPadding(dp(4), 0, dp(4), 0);
        header.addView(tvChevron);

        // ── Body (collapsible) ────────────────────────────────────────────
        LinearLayout bodyLayout = new LinearLayout(this);
        bodyLayout.setOrientation(LinearLayout.VERTICAL);
        bodyLayout.setPadding(dp(18), dp(12), dp(18), dp(16));
        bodyLayout.setVisibility(startExpanded ? View.VISIBLE : View.GONE);
        root.addView(bodyLayout);

        // Divider
        View divider = new View(this);
        LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divider.setLayoutParams(dvp);
        divider.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
        divider.setVisibility(startExpanded ? View.VISIBLE : View.GONE);
        root.addView(divider, 1); // insert between header and body

        // Body lines
        for (String rawLine : body.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                View spacer = new View(this);
                spacer.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(4)));
                bodyLayout.addView(spacer);
                continue;
            }
            // Skip separator lines
            if (line.startsWith("===") || line.startsWith("---")) continue;

            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            tv.setLayoutParams(lp);
            tv.setText(line);
            tv.setTextSize(13.5f);
            tv.setLineSpacing(2f, 1.3f);

            // Sub-headings (time labels like "7:00 AM", bold lines) get accent colour
            boolean isSubHeading = line.matches(".*\\d{1,2}:\\d{2}.*[AP]M.*")
                    || line.startsWith("**") || line.startsWith("##");
            tv.setTextColor(Color.parseColor(isSubHeading ? "#DDEEF2FF" : "#CCB8C4D4"));
            if (isSubHeading) tv.setTypeface(null, Typeface.BOLD);
            bodyLayout.addView(tv);
        }

        // ── Toggle click listener ─────────────────────────────────────────
        final boolean[] expanded = { startExpanded };
        header.setClickable(true);
        header.setFocusable(true);
        header.setOnClickListener(v -> {
            expanded[0] = !expanded[0];
            tvChevron.setText(expanded[0] ? "\u2303" : "\u2304");
            divider.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
            if (expanded[0]) {
                bodyLayout.setVisibility(View.VISIBLE);
                bodyLayout.setAlpha(0f);
                bodyLayout.animate().alpha(1f).setDuration(250).start();
            } else {
                bodyLayout.animate().alpha(0f).setDuration(180)
                        .withEndAction(() -> bodyLayout.setVisibility(View.GONE)).start();
            }
        });

        return card;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void showConfirmRegenerate() {
        new AlertDialog.Builder(this)
                .setTitle("Redo Itinerary")
                .setMessage("This will replace your current itinerary with a fresh AI plan. Continue?")
                .setPositiveButton("Yes, redo", (d, w) -> generateAiItinerary(null))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAdjustDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_adjust_itinerary, null);
        EditText etAdjust = dialogView.findViewById(R.id.etAdjustPrompt);

        new AlertDialog.Builder(this)
                .setTitle("\u2726 Adjust Itinerary")
                .setView(dialogView)
                .setPositiveButton("Adjust with AI", (d, w) -> {
                    String adjustPrompt = etAdjust.getText().toString().trim();
                    if (!adjustPrompt.isEmpty()) {
                        generateAiItinerary(adjustPrompt);
                    } else {
                        Toast.makeText(this, "Please describe what to adjust",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Animation helpers ─────────────────────────────────────────────────────

    private void animateIn(View view) {
        view.setAlpha(0f);
        view.setTranslationY(30f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "translationY", 30f, 0f)
        );
        set.setDuration(350);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    private void animateCard(View card, int delayMs) {
        card.setAlpha(0f);
        card.setTranslationY(30f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(card, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(card, "translationY", 30f, 0f)
        );
        set.setDuration(300);
        set.setStartDelay(delayMs);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}
