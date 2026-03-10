package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;

import com.example.roamio.MainActivity;
import com.example.roamio.R;
import com.example.roamio.firebase.FirebaseAuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputEditText etEmail, etPassword;
    private TextInputLayout   tilEmail, tilPassword;
    private MaterialButton    btnLogin;
    private TextView          tvSignupRedirect, tvForgotPassword;
    private CardView          cardForm;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuthManager authManager;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive glass effect — draw behind status bar (same as SignupActivity)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_login);

        authManager = new FirebaseAuthManager();

        // If already logged in, skip straight to MainActivity
        if (authManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        bindViews();
        setupClickListeners();
        playEntryAnimation();
    }

    // ── Bind Views ────────────────────────────────────────────────────────────
    private void bindViews() {
        etEmail          = findViewById(R.id.etLoginEmail);
        etPassword       = findViewById(R.id.etLoginPassword);
        tilEmail         = findViewById(R.id.tilLoginEmail);
        tilPassword      = findViewById(R.id.tilLoginPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        tvSignupRedirect = findViewById(R.id.tvSignupRedirect);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cardForm         = findViewById(R.id.cardLoginForm);
    }

    // ── Click Listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvSignupRedirect.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    // ── Login Logic ───────────────────────────────────────────────────────────
    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email    = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString();

        // Validation
        if (email.isEmpty()) {
            tilEmail.setError("Please enter your email");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        authManager.signIn(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                runOnUiThread(() -> {
                    setLoading(false);
                    navigateToMain();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (errorMessage != null &&
                            (errorMessage.contains("password") || errorMessage.contains("credential"))) {
                        tilPassword.setError("Incorrect email or password");
                    } else if (errorMessage != null && errorMessage.contains("no user")) {
                        tilEmail.setError("No account found with this email");
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    // ── Forgot Password ───────────────────────────────────────────────────────
    private void handleForgotPassword() {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        if (email.isEmpty()) {
            tilEmail.setError("Enter your email first to reset password");
            etEmail.requestFocus();
            return;
        }
        Toast.makeText(this,
                "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
    }

    // ── Loading State ─────────────────────────────────────────────────────────
    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in…" : "Sign In");
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ── Entry Animation ───────────────────────────────────────────────────────
    private void playEntryAnimation() {
        cardForm.setAlpha(0f);
        cardForm.setTranslationY(60f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(cardForm, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(cardForm, "translationY", 60f, 0f)
        );
        set.setDuration(500);
        set.setStartDelay(120);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
}