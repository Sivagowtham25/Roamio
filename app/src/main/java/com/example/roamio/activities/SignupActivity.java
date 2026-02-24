package com.example.roamio.activities;

import android.os.Bundle;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roamio.R;

public class SignupActivity extends AppCompatActivity {

    Spinner sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        sp = findViewById(R.id.spJob);

        String[] jobs = {"Student","Business","Employee"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        jobs);

        sp.setAdapter(adapter);
    }
}
