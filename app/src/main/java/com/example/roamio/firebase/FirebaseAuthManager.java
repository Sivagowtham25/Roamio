package com.example.roamio.firebase;

import com.example.roamio.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * FirebaseAuthManager
 * Centralises all Firebase Authentication + Firestore operations for Roamio.
 */
public class FirebaseAuthManager {

    private final FirebaseAuth      mAuth;
    private final FirebaseFirestore db;

    // Collection name in Firestore
    private static final String USERS_COLLECTION = "users";

    public FirebaseAuthManager() {
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
    }

    // ── Callback Interface ────────────────────────────────────────────────────
    public interface AuthCallback {
        void onSuccess(String uid);
        void onFailure(String errorMessage);
    }

    // ── Sign Up ───────────────────────────────────────────────────────────────
    /**
     * Creates a Firebase Auth user then saves the full User profile to Firestore.
     * @param email    User's email address
     * @param password User's chosen password
     * @param user     Populated User model (name, age, jobType, tripPreferences, recommendations)
     * @param callback Success delivers UID; failure delivers error message
     */
    public void signUp(String email, String password, User user, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onFailure("Authentication failed: no user returned.");
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    saveUserProfile(uid, user, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Sign In ───────────────────────────────────────────────────────────────
    public void signIn(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        callback.onSuccess(firebaseUser.getUid());
                    } else {
                        callback.onFailure("Login failed: no user found.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────
    public void signOut() {
        mAuth.signOut();
    }

    // ── Get Current User ──────────────────────────────────────────────────────
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    // ── Save User Profile to Firestore ────────────────────────────────────────
    private void saveUserProfile(String uid, User user, AuthCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("name",              user.getName());
        data.put("email",             user.getEmail());
        data.put("age",               user.getAge());
        data.put("jobType",           user.getJobType());
        data.put("tripPreferences",   user.getTripPreferences());
        data.put("aiRecommendations", user.getAiRecommendations());
        data.put("createdAt",         com.google.firebase.Timestamp.now());

        db.collection(USERS_COLLECTION)
                .document(uid)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(uid))
                .addOnFailureListener(e -> callback.onFailure("Profile save failed: " + e.getMessage()));
    }

    // ── Fetch User Profile from Firestore ─────────────────────────────────────
    public interface ProfileCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public void fetchUserProfile(String uid, ProfileCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure("User profile not found.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
