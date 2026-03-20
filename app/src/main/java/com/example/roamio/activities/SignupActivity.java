package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;

import com.example.roamio.MainActivity;
import com.example.roamio.R;
import com.example.roamio.firebase.FirebaseAuthManager;
import com.example.roamio.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.example.roamio.utils.SessionManager;

public class SignupActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputEditText etName, etEmail, etAge, etPassword;
    private TextInputLayout   tilPassword;
    private Spinner           spinnerJobType;
    private MaterialButton    btnSignup;
    private TextView          tvLoginRedirect;
    private CardView          cardForm;

    // Trip checkboxes
    private CheckBox cbAdventure, cbCultural, cbBeach, cbNature;
    private CheckBox cbCity, cbSpiritual, cbLuxury, cbBudget;

    // Password strength segments
    private View seg1, seg2, seg3, seg4;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuthManager authManager;
    private SessionManager sessionManager;

    // ── Data ──────────────────────────────────────────────────────────────────
    private static final String[] JOB_OPTIONS = {
            "Select your profession",
            "Student",
            "Working Professional",
            "Freelancer",
            "Entrepreneur",
            "Retired",
            "Researcher / Academic",
            "Artist / Creative",
            "Other"
    };

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive glass effect — draw behind status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_signup);

        authManager = new FirebaseAuthManager();
        sessionManager = new SessionManager(this);

        bindViews();
        setupJobSpinner();
        setupPasswordStrength();
        setupClickListeners();
        playEntryAnimation();
    }

    // ── Bind Views ────────────────────────────────────────────────────────────
    private void bindViews() {
        etName           = findViewById(R.id.etName);
        etEmail          = findViewById(R.id.etEmail);
        etAge            = findViewById(R.id.etAge);
        etPassword       = findViewById(R.id.etPassword);
        tilPassword      = findViewById(R.id.tilPassword);
        spinnerJobType   = findViewById(R.id.spinnerJobType);
        btnSignup        = findViewById(R.id.btnSignup);
        tvLoginRedirect  = findViewById(R.id.tvLoginRedirect);
        cardForm         = findViewById(R.id.cardForm);

        cbAdventure  = findViewById(R.id.cbAdventure);
        cbCultural   = findViewById(R.id.cbCultural);
        cbBeach      = findViewById(R.id.cbBeach);
        cbNature     = findViewById(R.id.cbNature);
        cbCity       = findViewById(R.id.cbCity);
        cbSpiritual  = findViewById(R.id.cbSpiritual);
        cbLuxury     = findViewById(R.id.cbLuxury);
        cbBudget     = findViewById(R.id.cbBudget);

        seg1 = findViewById(R.id.strengthSeg1);
        seg2 = findViewById(R.id.strengthSeg2);
        seg3 = findViewById(R.id.strengthSeg3);
        seg4 = findViewById(R.id.strengthSeg4);
    }

    // ── Spinner ───────────────────────────────────────────────────────────────
    private void setupJobSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_glass,
                JOB_OPTIONS
        );
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerJobType.setAdapter(adapter);
    }

    // ── Password Strength ─────────────────────────────────────────────────────
    private void setupPasswordStrength() {
        Objects.requireNonNull(etPassword).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateStrengthBar(s.toString()); }
        });
    }

    private void updateStrengthBar(String pwd) {
        int score = 0;
        if (pwd.length() >= 6)  score++;
        if (pwd.length() >= 10) score++;
        if (pwd.matches(".*[A-Z].*") && pwd.matches(".*[0-9].*")) score++;
        if (pwd.matches(".*[^A-Za-z0-9].*")) score++;

        int colorWeak   = 0xFFEF4444;
        int colorMedium = 0xFFF4B942;
        int colorStrong = 0xFF00C9B1;
        int colorBlank  = 0x22FFFFFF;

        int color = score <= 1 ? colorWeak : score <= 2 ? colorMedium : colorStrong;

        seg1.setBackgroundColor(score >= 1 ? color : colorBlank);
        seg2.setBackgroundColor(score >= 2 ? color : colorBlank);
        seg3.setBackgroundColor(score >= 3 ? color : colorBlank);
        seg4.setBackgroundColor(score >= 4 ? color : colorBlank);
    }

    // ── Entry Animation ───────────────────────────────────────────────────────
    private void playEntryAnimation() {
        cardForm.setAlpha(0f);
        cardForm.setTranslationY(70f);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(cardForm, "alpha", 0f, 1f);
        ObjectAnimator slide = ObjectAnimator.ofFloat(cardForm, "translationY", 70f, 0f);
        slide.setInterpolator(new DecelerateInterpolator(2.2f));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, slide);
        set.setDuration(750);
        set.setStartDelay(180);
        set.start();
    }

    // ── Click Listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnSignup.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
            attemptSignup();
        });

        tvLoginRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);

            startActivity(
                    intent,
                    android.app.ActivityOptions
                            .makeCustomAnimation(this, R.anim.slide_left, R.anim.fade_out)
                            .toBundle()
            );
        });
    }

    // ── Signup Logic ──────────────────────────────────────────────────────────
    private void attemptSignup() {
        String name  = Objects.requireNonNull(etName.getText()).toString().trim();
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String ageStr = Objects.requireNonNull(etAge.getText()).toString().trim();
        String pwd   = Objects.requireNonNull(etPassword.getText()).toString().trim();
        int    jobPos = spinnerJobType.getSelectedItemPosition();

        // Validate
        if (name.isEmpty())  { etName.setError("Name is required");  etName.requestFocus();  return; }
        if (email.isEmpty()) { etEmail.setError("Email is required"); etEmail.requestFocus(); return; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email"); etEmail.requestFocus(); return;
        }
        if (ageStr.isEmpty()) { etAge.setError("Age is required"); etAge.requestFocus(); return; }
        if (jobPos == 0)      { showToast("Please select your job type"); return; }

        List<String> selectedTrips = getSelectedTrips();
        if (selectedTrips.isEmpty()) { showToast("Select at least one trip type ✈️"); return; }
        if (pwd.length() < 6) { etPassword.setError("Min 6 characters"); etPassword.requestFocus(); return; }

        int    age     = Integer.parseInt(ageStr);
        String jobType = JOB_OPTIONS[jobPos];

        // Lock UI
        btnSignup.setEnabled(false);
        btnSignup.setText("Creating profile…");

        // Build User model
        List<String> recommendations = TripRecommendationEngine.generate(age, jobType, selectedTrips);
        User user = new User(name, email, age, jobType, selectedTrips, recommendations);

        // Firebase sign up
        authManager.signUp(email, pwd, user, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                sessionManager.saveSession(uid, name);   // ← saves 30-day session
                showToast("Welcome, " + name + "! 🌍");
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                intent.putExtra("uid", uid);
                intent.putExtra("user_name", name);      // ← so greeting shows name
                intent.putStringArrayListExtra("recommendations", new ArrayList<>(recommendations));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    btnSignup.setEnabled(true);
                    btnSignup.setText("Create My Travel Profile");
                    showToast("Signup failed: " + errorMessage);
                });
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private List<String> getSelectedTrips() {
        List<String> list = new ArrayList<>();
        if (cbAdventure.isChecked())  list.add("adventure");
        if (cbCultural.isChecked())   list.add("cultural");
        if (cbBeach.isChecked())      list.add("beach");
        if (cbNature.isChecked())     list.add("nature");
        if (cbCity.isChecked())       list.add("city");
        if (cbSpiritual.isChecked())  list.add("spiritual");
        if (cbLuxury.isChecked())     list.add("luxury");
        if (cbBudget.isChecked())     list.add("budget");
        return list;
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ── Inner: Trip Recommendation Engine ────────────────────────────────────
    /**
     * Rule-based AI trip recommendation engine.
     * Factors: age group, job type, preferred trip styles.
     * Can later be extended to call a remote AI/ML endpoint.
     */
    public static class TripRecommendationEngine {

        public static List<String> generate(int age, String jobType, List<String> tripPrefs) {
            List<String> recs = new ArrayList<>();

            // --- Age-based base recommendations ---
            if (age >= 13 && age <= 22) {
                recs.add("Coorg Trekking & Camping Weekend 🏕️");
                recs.add("Goa Beach Party Circuit 🎉");
            } else if (age >= 23 && age <= 35) {
                recs.add("Rajasthan Heritage Motorcycle Ride 🐪");
                recs.add("Himachal Pradesh Adventure Trek 🏔️");
            } else if (age >= 36 && age <= 55) {
                recs.add("Kerala Backwaters Luxury Houseboat 🛥️");
                recs.add("Varanasi Spiritual Ghats Experience 🕯️");
            } else {
                recs.add("Ooty-Coorg Scenic Train Journey 🚂");
                recs.add("Rishikesh Yoga & Wellness Retreat 🧘");
            }

            // --- Job-based recommendations ---
            switch (jobType) {
                case "Student":
                    recs.add("Budget Backpacking — Northeast India 🎒");
                    break;
                case "Working Professional":
                    recs.add("Weekend Escape — Mahabaleshwar & Panchgani 🌸");
                    break;
                case "Freelancer":
                    recs.add("Digital Nomad Hub — North Goa 💻🌴");
                    break;
                case "Entrepreneur":
                    recs.add("Luxury Business Retreat — Maldives 💼");
                    break;
                case "Retired":
                    recs.add("Char Dham Pilgrimage Circuit 🙏");
                    break;
                case "Researcher / Academic":
                    recs.add("Historical Ruins Tour — Hampi & Belur 🏛️");
                    break;
                case "Artist / Creative":
                    recs.add("Udaipur Art & Culture Festival Tour 🎨");
                    break;
            }

            // --- Trip preference-based recommendations ---
            if (tripPrefs.contains("adventure"))  recs.add("Rishikesh White-Water Rafting 🚣");
            if (tripPrefs.contains("cultural"))   recs.add("Jaipur Pink City Walking Tour 🌆");
            if (tripPrefs.contains("beach"))      recs.add("Andaman Snorkelling & Island Hop 🐠");
            if (tripPrefs.contains("nature"))     recs.add("Jim Corbett Wildlife Safari 🐯");
            if (tripPrefs.contains("city"))       recs.add("Mumbai Street Food & Art Deco Walk 🏙️");
            if (tripPrefs.contains("spiritual"))  recs.add("Tirupati–Shirdi Heritage Pilgrimage 🛕");
            if (tripPrefs.contains("luxury"))     recs.add("Udaipur Palace Hotel Experience 👑");
            if (tripPrefs.contains("budget"))     recs.add("Spiti Valley Budget Expedition ❄️");

            // Deduplicate and cap at 6
            List<String> unique = new ArrayList<>();
            for (String r : recs) { if (!unique.contains(r)) unique.add(r); }
            return unique.subList(0, Math.min(6, unique.size()));
        }
    }
}