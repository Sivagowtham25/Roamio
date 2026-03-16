package com.example.roamio.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager
 * Persists login state for 30 days using SharedPreferences.
 * Firebase Auth itself keeps the auth token refreshed,
 * but this layer adds an explicit 30-day expiry on top.
 */
public class SessionManager {

    private static final String PREF_NAME          = "RoamioSession";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
    private static final String KEY_USER_UID        = "user_uid";
    private static final String KEY_USER_NAME       = "user_name";

    /** 30 days in milliseconds */
    public static final long SESSION_DURATION_MS = 30L * 24L * 60L * 60L * 1000L;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Save session on successful login / signup ─────────────────────────────
    public void saveSession(String uid, String name) {
        prefs.edit()
                .putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
                .putString(KEY_USER_UID, uid)
                .putString(KEY_USER_NAME, name)
                .apply();
    }

    // ── Check whether the 30-day session is still valid ───────────────────────
    public boolean isSessionValid() {
        long loginTime = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L);
        if (loginTime == 0L) return false;
        return (System.currentTimeMillis() - loginTime) < SESSION_DURATION_MS;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getSavedUid() {
        return prefs.getString(KEY_USER_UID, null);
    }

    public String getSavedName() {
        return prefs.getString(KEY_USER_NAME, "Traveller");
    }

    // ── Clear on logout ───────────────────────────────────────────────────────
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
