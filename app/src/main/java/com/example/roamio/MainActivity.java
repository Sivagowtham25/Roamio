package com.example.roamio;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roamio.activities.SignupActivity;   // ← THIS WAS MISSING

public class MainActivity extends AppCompatActivity {

    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = findViewById(R.id.btnSignup);

        btn.setOnClickListener(v -> {

            Intent i = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(i);

        });
    }
}
