package com.example.roamio.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.roamio.R;

public class TripsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_trips);

        // Create a trip manually
        findViewById(R.id.btnCreateTrip).setOnClickListener(v -> {
            startActivity(new Intent(this, CreateTripActivity.class));
            overridePendingTransition(R.anim.slide_up, R.anim.fade_out);
        });

        // Plan with Gemini AI
        findViewById(R.id.btnPlanWithAi).setOnClickListener(v -> {
            startActivity(new Intent(this, AiTripPlannerActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }
}
