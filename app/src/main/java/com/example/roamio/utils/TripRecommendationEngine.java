package com.example.roamio.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * TripRecommendationEngine — rule-based AI-feel recommendation generator.
 * Factors: age, job type, trip preferences.
 * Moved out of SignupActivity so it can be called from any context.
 *
 * Compatible with cross-city scope expansion: when cross-city trips are added,
 * pass originCity and destinationCity as parameters and add city-pair rules below
 * without touching the existing logic.
 */
public class TripRecommendationEngine {

    private TripRecommendationEngine() {}

    public static List<String> generate(int age, String jobType, List<String> tripPrefs) {
        List<String> recs = new ArrayList<>();

        // Age-based base recommendations
        if (age >= 13 && age <= 22) {
            recs.add("Coorg Trekking & Camping Weekend 🏕️");
            recs.add("Goa Beach Party Circuit 🎉");
        } else if (age >= 23 && age <= 35) {
            recs.add("Rajasthan Heritage Motorcycle Ride 🐪");
            recs.add("Himachal Pradesh Adventure Trek 🏔️");
        } else if (age >= 36 && age <= 55) {
            recs.add("Kerala Backwaters Luxury Houseboat 🛥️");
            recs.add("Varanasi Spiritual Ghats Experience 🕯️");
        } else {
            recs.add("Ooty-Coorg Scenic Train Journey 🚂");
            recs.add("Rishikesh Yoga & Wellness Retreat 🧘");
        }

        // Job-based
        if (jobType != null) {
            switch (jobType) {
                case "Student":
                    recs.add("Budget Backpacking — Northeast India 🎒"); break;
                case "Working Professional":
                    recs.add("Weekend Escape — Mahabaleshwar & Panchgani 🌸"); break;
                case "Freelancer":
                    recs.add("Digital Nomad Hub — North Goa 💻🌴"); break;
                case "Entrepreneur":
                    recs.add("Luxury Business Retreat — Maldives 💼"); break;
                case "Retired":
                    recs.add("Char Dham Pilgrimage Circuit 🙏"); break;
                case "Researcher / Academic":
                    recs.add("Historical Ruins Tour — Hampi & Belur 🏛️"); break;
                case "Artist / Creative":
                    recs.add("Udaipur Art & Culture Festival Tour 🎨"); break;
            }
        }

        // Preference-based
        if (tripPrefs != null) {
            if (tripPrefs.contains("adventure"))  recs.add("Rishikesh White-Water Rafting 🚣");
            if (tripPrefs.contains("cultural"))   recs.add("Jaipur Pink City Walking Tour 🌆");
            if (tripPrefs.contains("beach"))      recs.add("Andaman Snorkelling & Island Hop 🐠");
            if (tripPrefs.contains("nature"))     recs.add("Jim Corbett Wildlife Safari 🐯");
            if (tripPrefs.contains("city"))       recs.add("Mumbai Street Food & Art Deco Walk 🏙️");
            if (tripPrefs.contains("spiritual"))  recs.add("Tirupati–Shirdi Heritage Pilgrimage 🛕");
            if (tripPrefs.contains("luxury"))     recs.add("Udaipur Palace Hotel Experience 👑");
            if (tripPrefs.contains("budget"))     recs.add("Spiti Valley Budget Expedition ❄️");
        }

        // Deduplicate and cap at 6
        List<String> unique = new ArrayList<>();
        for (String r : recs) { if (!unique.contains(r)) unique.add(r); }
        return unique.subList(0, Math.min(6, unique.size()));
    }
}
