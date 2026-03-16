package com.example.roamio.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;

import com.example.roamio.MainActivity;
import com.example.roamio.R;
import com.example.roamio.firebase.FirebaseAuthManager;
import com.example.roamio.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton    btnLogin;
    private CardView          cardLogin;

    private FirebaseAuthManager authManager;
    private SessionManager      sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive — draw behind status bar to match signup style
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_login);

        authManager    = new FirebaseAuthManager();
        sessionManager = new SessionManager(this);

        bindViews();
        playEntryAnimation();
        setupClickListeners();
    }

    private void bindViews() {
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        cardLogin  = findViewById(R.id.cardLogin);

        TextView tvSignupRedirect  = findViewById(R.id.tvSignupRedirect);
        TextView tvForgotPassword  = findViewById(R.id.tvForgotPassword);

        tvSignupRedirect.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            overridePendingTransition(R.anim.slide_left, R.anim.fade_out);
        });

        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Password reset coming soon!", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
            attemptLogin();
        });
    }

    private void attemptLogin() {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String pwd   = Objects.requireNonNull(etPassword.getText()).toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }
        if (pwd.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in…");

        authManager.signIn(email, pwd, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                // Fetch name from Firestore for session storage
                authManager.fetchUserProfile(uid, new FirebaseAuthManager.ProfileCallback() {
                    @Override
                    public void onSuccess(com.example.roamio.models.User user) {
                        String name = (user != null && user.getName() != null)
                                ? user.getName() : "Traveller";
                        // ✅ Save 30-day session
                        sessionManager.saveSession(uid, name);
                        goHome(uid, name);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // Profile fetch failed but auth succeeded — still proceed
                        sessionManager.saveSession(uid, "Traveller");
                        goHome(uid, "Traveller");
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Sign In");
                    Toast.makeText(LoginActivity.this,
                            "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void goHome(String uid, String name) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Welcome back, " + name + "! \uD83C\uDF0D", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("uid", uid);
            intent.putExtra("user_name", name);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    private void playEntryAnimation() {
        cardLogin.setAlpha(0f);
        cardLogin.setTranslationY(70f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(cardLogin, "alpha", 0f, 1f);
        ObjectAnimator slide = ObjectAnimator.ofFloat(cardLogin, "translationY", 70f, 0f);
        slide.setInterpolator(new DecelerateInterpolator(2.2f));
        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, slide);
        set.setDuration(700);
        set.setStartDelay(150);
        set.start();
    }
}