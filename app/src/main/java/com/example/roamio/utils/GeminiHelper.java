package com.example.roamio.utils;

import com.example.roamio.BuildConfig;
import com.example.roamio.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GeminiHelper — shared Gemini API logic.
 */
public class GeminiHelper {

    private GeminiHelper() {}

    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public interface GeminiCallback {
        void onSuccess(String text);
        void onFailure(String error);
    }

    public static void call(JSONArray contents, String systemPrompt, String systemReply,
                            double temperature, int maxTokens,
                            GeminiCallback callback) {
        try {
            JSONArray full = new JSONArray();

            JSONObject su = new JSONObject();
            su.put("role", "user");
            su.put("parts", new JSONArray().put(new JSONObject().put("text", systemPrompt)));
            full.put(su);

            JSONObject sm = new JSONObject();
            sm.put("role", "model");
            sm.put("parts", new JSONArray().put(new JSONObject().put("text", systemReply)));
            full.put(sm);

            for (int i = 0; i < contents.length(); i++) full.put(contents.get(i));

            JSONObject cfg = new JSONObject();
            cfg.put("temperature", temperature);
            cfg.put("maxOutputTokens", maxTokens);

            JSONObject body = new JSONObject();
            body.put("contents", full);
            body.put("generationConfig", cfg);

            Request req = new Request.Builder()
                    .url(Constants.GEMINI_BASE_URL + BuildConfig.GEMINI_API_KEY)
                    .post(RequestBody.create(body.toString(),
                            MediaType.parse("application/json")))
                    .build();

            HTTP_CLIENT.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    callback.onFailure("Connection failed: " + e.getMessage());
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() == null) { callback.onFailure("Empty response"); return; }
                    String raw = response.body().string();
                    callback.onSuccess(parseResponse(raw));
                }
            });
        } catch (Exception e) {
            callback.onFailure("Build error: " + e.getMessage());
        }
    }

    public static String parseResponse(String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.has("error")) {
                return "Error: " + root.getJSONObject("error").optString("message", "API error");
            }
            return root.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        } catch (Exception e) {
            return "Error parsing response. Please try again.";
        }
    }

    public static JSONArray singleTurn(String userText) throws Exception {
        JSONArray arr = new JSONArray();
        JSONObject msg = new JSONObject();
        msg.put("role", "user");
        msg.put("parts", new JSONArray().put(new JSONObject().put("text", userText)));
        arr.put(msg);
        return arr;
    }

    public static boolean isItinerary(String text) {
        if (text == null) return false;
        for (String line : text.split("\n")) {
            String clean = line.trim().toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
            if (clean.matches("day 1.*")) return true;
        }
        return false;
    }
}
