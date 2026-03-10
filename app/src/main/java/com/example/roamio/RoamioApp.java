package com.example.roamio;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class RoamioApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialises Firebase for the entire app at startup
        FirebaseApp.initializeApp(this);
    }
}