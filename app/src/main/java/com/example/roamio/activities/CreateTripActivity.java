package com.example.roamio.activities;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.roamio.R;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class CreateTripActivity extends AppCompatActivity {

    private TextInputEditText etDestination, etTripName;
    private TextInputEditText etStartDate, etEndDate;
    private TextInputEditText etTravellers, etBudget, etNotes;

    private final Calendar calStart = Calendar.getInstance();
    private final Calendar calEnd   = Calendar.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_create_trip);

        bindViews();
        setupDatePickers();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveTrip).setOnClickListener(v -> saveTrip());
    }

    private void bindViews() {
        etDestination = findViewById(R.id.etDestination);
        etTripName    = findViewById(R.id.etTripName);
        etStartDate   = findViewById(R.id.etStartDate);
        etEndDate     = findViewById(R.id.etEndDate);
        etTravellers  = findViewById(R.id.etTravellers);
        etBudget      = findViewById(R.id.etBudget);
        etNotes       = findViewById(R.id.etNotes);
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (dp, y, m, d) -> {
                        calStart.set(y, m, d);
                        etStartDate.setText(sdf.format(calStart.getTime()));
                    },
                    calStart.get(Calendar.YEAR),
                    calStart.get(Calendar.MONTH),
                    calStart.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
            dialog.show();
        });

        etEndDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (dp, y, m, d) -> {
                        calEnd.set(y, m, d);
                        etEndDate.setText(sdf.format(calEnd.getTime()));
                    },
                    calEnd.get(Calendar.YEAR),
                    calEnd.get(Calendar.MONTH),
                    calEnd.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(calStart.getTimeInMillis());
            dialog.show();
        });
    }

    private void saveTrip() {
        String destination = Objects.requireNonNull(etDestination.getText()).toString().trim();
        String tripName    = Objects.requireNonNull(etTripName.getText()).toString().trim();
        String startDate   = Objects.requireNonNull(etStartDate.getText()).toString().trim();
        String endDate     = Objects.requireNonNull(etEndDate.getText()).toString().trim();

        if (destination.isEmpty()) {
            etDestination.setError("Please enter a destination");
            etDestination.requestFocus();
            return;
        }
        if (tripName.isEmpty()) {
            etTripName.setError("Please enter a trip name");
            etTripName.requestFocus();
            return;
        }
        if (startDate.isEmpty()) {
            Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endDate.isEmpty()) {
            Toast.makeText(this, "Please select an end date", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Save to Firestore under the user's trip collection
        Toast.makeText(this,
                "Trip to " + destination + " saved! ✈️", Toast.LENGTH_SHORT).show();
        finish();
    }
}
