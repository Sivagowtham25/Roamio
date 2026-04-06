package com.example.roamio.firebase;

import com.example.roamio.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * FirebaseAuthManager
 * Handles Authentication + Firestore user profile operations
 */
public class FirebaseAuthManager {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private static final String USERS_COLLECTION = "users";

    public FirebaseAuthManager() {
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
    }

    // ── Auth Callback ─────────────────────────────────────────────
    public interface AuthCallback {
        void onSuccess(String uid);
        void onFailure(String errorMessage);
    }

    // ── Profile Callback ──────────────────────────────────────────
    public interface ProfileCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    // ── SIGN UP ───────────────────────────────────────────────────
    public void signUp(String email, String password, User user, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();

                    if (firebaseUser == null) {
                        callback.onFailure("Authentication failed.");
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    // Save full profile
                    saveUserProfile(uid, user, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── SIGN IN ───────────────────────────────────────────────────
    public void signIn(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();

                    if (firebaseUser != null) {
                        callback.onSuccess(firebaseUser.getUid());
                    } else {
                        callback.onFailure("Login failed.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── SIGN OUT ──────────────────────────────────────────────────
    public void signOut() {
        mAuth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    // ── SAVE USER PROFILE (SAFE - MERGE) ──────────────────────────
    private void saveUserProfile(String uid, User user, AuthCallback callback) {

        Map<String, Object> data = new HashMap<>();
        data.put("name",              user.getName());
        data.put("email",             user.getEmail());
        data.put("age",               user.getAge());
        data.put("jobType",           user.getJobType());
        data.put("tripPreferences",   user.getTripPreferences());
        data.put("aiRecommendations", user.getAiRecommendations());
        data.put("createdAt",         Timestamp.now());

        // 🔥 IMPORTANT: merge prevents overwriting existing data
        db.collection(USERS_COLLECTION)
                .document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(uid))
                .addOnFailureListener(e ->
                        callback.onFailure("Profile save failed: " + e.getMessage()));
    }

    // ── FETCH USER PROFILE (SAFE + NO OVERWRITE) ──────────────────
    public void fetchUserProfile(String uid, ProfileCallback callback) {

        db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        User user = documentSnapshot.toObject(User.class);

                        if (user == null) {
                            callback.onFailure("User data corrupted.");
                            return;
                        }

                        // 🔥 Fill missing fields safely
                        if (user.getName() == null) user.setName("Traveller");
                        if (user.getEmail() == null) user.setEmail("");
                        if (user.getJobType() == null) user.setJobType("Not specified");
                        if (user.getTripPreferences() == null)
                            user.setTripPreferences(new ArrayList<>());
                        if (user.getAiRecommendations() == null)
                            user.setAiRecommendations(new ArrayList<>());

                        callback.onSuccess(user);
                        return;
                    }

                    // ❗ DO NOT overwrite DB — just fallback locally
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();

                    if (firebaseUser != null) {
                        User fallbackUser = new User();

                        fallbackUser.setName("Traveller");
                        fallbackUser.setEmail(firebaseUser.getEmail());
                        fallbackUser.setAge(0);
                        fallbackUser.setJobType("Not specified");
                        fallbackUser.setTripPreferences(new ArrayList<>());
                        fallbackUser.setAiRecommendations(new ArrayList<>());

                        callback.onSuccess(fallbackUser);
                    } else {
                        callback.onFailure("User not authenticated.");
                    }

                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── DELETE ACCOUNT (WITH PASSWORD RE-AUTH) ────────────────────
    public void deleteAccountWithPassword(String password, AuthCallback callback) {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            callback.onFailure("User not authenticated.");
            return;
        }

        String email = user.getEmail();
        String uid   = user.getUid();

        AuthCredential credential =
                EmailAuthProvider.getCredential(email, password);

        // 🔐 Re-authenticate first
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {

                    // Delete Firestore profile
                    db.collection(USERS_COLLECTION)
                            .document(uid)
                            .delete()
                            .addOnSuccessListener(unused1 -> {

                                // Delete Auth user
                                user.delete()
                                        .addOnSuccessListener(aVoid ->
                                                callback.onSuccess(uid))
                                        .addOnFailureListener(e ->
                                                callback.onFailure(e.getMessage()));

                            })
                            .addOnFailureListener(e ->
                                    callback.onFailure(e.getMessage()));

                })
                .addOnFailureListener(e ->
                        callback.onFailure("Re-authentication failed: " + e.getMessage()));
    }
}