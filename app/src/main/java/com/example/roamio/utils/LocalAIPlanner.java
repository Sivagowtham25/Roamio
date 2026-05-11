package com.example.roamio.utils;

import java.util.ArrayList;
import java.util.List;

public class LocalAIPlanner {
    public static List<String> generatePlan(String destination, int days, String budget, String travellers, String notes) {
        List<String> plan = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            plan.add("Day " + i + " — Discovering " + destination);
            plan.add("08:30 AM — 🍳 Authentic local breakfast");
            plan.add("10:00 AM — 🏛️ Guided heritage walk");
            plan.add("01:30 PM — 🍱 Traditional lunch experience");
            plan.add("04:00 PM — 📸 Iconic photography spots");
            plan.add("07:30 PM — 🍽️ Fine dining / Street food tour");
        }
        plan.add("Tips");
        plan.add("• Use local transport for an authentic feel.");
        plan.add("• Carry a power bank for long days.");
        return plan;
    }
}
