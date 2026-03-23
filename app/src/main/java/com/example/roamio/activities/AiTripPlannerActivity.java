package com.example.roamio.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roamio.BuildConfig;
import com.example.roamio.R;
import com.example.roamio.adapters.ChatAdapter;
import com.example.roamio.models.ChatMessage;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;

import okhttp3.Call;
import okhttp3.Callback;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AiTripPlannerActivity extends AppCompatActivity {

    // ── Gemini API endpoint ───────────────────────────────────────────────────
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.0-flash:generateContent?key=";

    private static final String SYSTEM_PROMPT =
            "You are Roamio AI, a friendly and expert travel planning assistant. " +
                    "Your ONLY purpose is to help users plan trips, create itineraries, " +
                    "suggest destinations, estimate budgets, recommend hotels and restaurants, " +
                    "give packing tips, and provide travel-related advice. " +
                    "If the user asks anything NOT related to travel or trip planning " +
                    "(like coding, math, politics, health, relationships, or general knowledge), " +
                    "you must politely decline and redirect them to travel topics. " +
                    "Say something like: 'I am only able to help with trip planning! " +
                    "Ask me about destinations, itineraries, or travel tips.' " +
                    "Always be warm, enthusiastic about travel, and keep responses concise and helpful.";

    private static final String SYSTEM_REPLY =
            "Understood! I am Roamio AI, your personal trip planning assistant. " +
                    "I can help you plan itineraries, find destinations, estimate budgets, " +
                    "suggest hotels and restaurants, and give travel tips. " +
                    "Where would you like to travel? 🌍";

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView   rvChat;
    private EditText       etMessage;
    private LinearLayout   layoutTyping;

    // ── Data ──────────────────────────────────────────────────────────────────
    private ChatAdapter        chatAdapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    // Conversation history sent to Gemini (excludes the invisible system turn)
    private final List<JSONObject> conversationHistory = new ArrayList<>();

    // ── Network ───────────────────────────────────────────────────────────────
    private final OkHttpClient httpClient = new OkHttpClient();

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_ai_trip_planner);

        bindViews();
        setupRecyclerView();
        showWelcomeMessage();
    }

    // ── Bind & wire views ─────────────────────────────────────────────────────
    private void bindViews() {
        rvChat       = findViewById(R.id.rvChat);
        etMessage    = findViewById(R.id.etMessage);
        layoutTyping = findViewById(R.id.layoutTyping);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnSend).setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                etMessage.setText("");
                sendMessage(text);
            }
        });

        findViewById(R.id.btnClearChat).setOnClickListener(v -> clearChat());
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this, messages);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvChat.setLayoutManager(llm);
        rvChat.setAdapter(chatAdapter);
    }

    // ── Welcome message on first open ─────────────────────────────────────────
    private void showWelcomeMessage() {
        addAiMessage("Hi! I'm Roamio AI ✦ — your personal trip planning assistant.\n\n" +
                "I can help you:\n" +
                "• 🗺️  Plan full itineraries\n" +
                "• 🏨  Find hotels & restaurants\n" +
                "• 💰  Estimate trip budgets\n" +
                "• 🎒  Give packing tips\n" +
                "• ✈️  Suggest destinations\n\n" +
                "Where would you like to travel?");
    }

    // ── Send a message ────────────────────────────────────────────────────────
    private void sendMessage(String text) {
        // Add user bubble
        addUserMessage(text);

        // Show typing indicator
        showTyping(true);

        // Add to conversation history for context
        try {
            JSONObject userTurn = new JSONObject();
            userTurn.put("role", "user");
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", text));
            userTurn.put("parts", parts);
            conversationHistory.add(userTurn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        callGeminiApi(text);
    }

    // ── Gemini API call ───────────────────────────────────────────────────────
    private void callGeminiApi(String userText) {
        try {
            // Build contents array
            // Slot 0: system prompt as user+model pair (focuses AI on trips only)
            // Slot 1..N: real conversation history
            JSONArray contents = new JSONArray();

            // System instruction pair (invisible to UI)
            JSONObject sysUser = new JSONObject();
            sysUser.put("role", "user");
            sysUser.put("parts", new JSONArray()
                    .put(new JSONObject().put("text", SYSTEM_PROMPT)));
            contents.put(sysUser);

            JSONObject sysModel = new JSONObject();
            sysModel.put("role", "model");
            sysModel.put("parts", new JSONArray()
                    .put(new JSONObject().put("text", SYSTEM_REPLY)));
            contents.put(sysModel);

            // Add full real conversation history
            for (JSONObject turn : conversationHistory) {
                contents.put(turn);
            }

            // Generation config
            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.8);
            genConfig.put("maxOutputTokens", 1024);

            // Request body
            JSONObject body = new JSONObject();
            body.put("contents", contents);
            body.put("generationConfig", genConfig);

            String apiKey = BuildConfig.GEMINI_API_KEY;
            if (apiKey.isEmpty()) {
                runOnUiThread(() -> {
                    showTyping(false);
                    addAiMessage("⚠️ Gemini API key not set. Please add GEMINI_API_KEY to local.properties.");
                });
                return;
            }

            Request request = new Request.Builder()
                    .url(GEMINI_URL + apiKey)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        showTyping(false);
                        addAiMessage("Sorry, I couldn't connect to the server. " +
                                "Please check your internet connection and try again.");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    if (response.body() == null) {
                        runOnUiThread(() -> {
                            showTyping(false);
                            addAiMessage("Something went wrong. Please try again.");
                        });
                        return;
                    }

                    String responseBody = response.body().string();
                    String aiText = parseGeminiResponse(responseBody);

                    runOnUiThread(() -> {
                        showTyping(false);
                        addAiMessage(aiText);

                        // Add AI turn to history for multi-turn context
                        try {
                            JSONObject modelTurn = new JSONObject();
                            modelTurn.put("role", "model");
                            modelTurn.put("parts", new JSONArray()
                                    .put(new JSONObject().put("text", aiText)));
                            conversationHistory.add(modelTurn);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showTyping(false);
            addAiMessage("An error occurred. Please try again.");
        }
    }

    // ── Parse Gemini JSON response ────────────────────────────────────────────
    private String parseGeminiResponse(String json) {
        try {
            JSONObject root = new JSONObject(json);

            // Check for API error
            if (root.has("error")) {
                String errMsg = root.getJSONObject("error").optString("message", "API error");
                return "Error: " + errMsg;
            }

            return root
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (Exception e) {
            return "I had trouble understanding that response. Please try again!";
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private void addUserMessage(String text) {
        messages.add(new ChatMessage(text, ChatMessage.Sender.USER));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void addAiMessage(String text) {
        messages.add(new ChatMessage(text, ChatMessage.Sender.AI));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        rvChat.post(() -> rvChat.smoothScrollToPosition(
                Math.max(0, messages.size() - 1)));
    }

    private void showTyping(boolean show) {
        layoutTyping.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void clearChat() {
        messages.clear();
        conversationHistory.clear();
        chatAdapter.notifyDataSetChanged();
        showWelcomeMessage();
        Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
    }
}
