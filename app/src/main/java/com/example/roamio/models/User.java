package com.example.roamio.models;

import java.util.List;

/**
 * User model for Roamio.
 * Stores signup data and AI-generated trip recommendations.
 * Used for Firestore serialisation/deserialisation (requires no-arg constructor).
 */
public class User {

    private String       name;
    private String       email;
    private int          age;
    private String       jobType;
    private List<String> tripPreferences;    // e.g. ["adventure", "beach", "city"]
    private List<String> aiRecommendations;  // AI-generated suggestions

    // Required no-arg constructor for Firestore toObject()
    public User() {}

    public User(String name, String email, int age, String jobType,
                List<String> tripPreferences, List<String> aiRecommendations) {
        this.name             = name;
        this.email            = email;
        this.age              = age;
        this.jobType          = jobType;
        this.tripPreferences  = tripPreferences;
        this.aiRecommendations = aiRecommendations;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public String getName()               { return name; }
    public void   setName(String name)    { this.name = name; }

    public String getEmail()              { return email; }
    public void   setEmail(String email)  { this.email = email; }

    public int    getAge()                { return age; }
    public void   setAge(int age)         { this.age = age; }

    public String getJobType()                { return jobType; }
    public void   setJobType(String jobType)  { this.jobType = jobType; }

    public List<String> getTripPreferences()                         { return tripPreferences; }
    public void         setTripPreferences(List<String> prefs)       { this.tripPreferences = prefs; }

    public List<String> getAiRecommendations()                             { return aiRecommendations; }
    public void         setAiRecommendations(List<String> recommendations) { this.aiRecommendations = recommendations; }
}
