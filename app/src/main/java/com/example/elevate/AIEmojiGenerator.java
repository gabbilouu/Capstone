package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class AIEmojiGenerator {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    private final SharedPreferences prefs;
    private static final String PREF_NAME = "EmojiCache";

    public interface EmojiCallback {
        void onEmojiGenerated(String emoji);
        void onError(Exception e);
    }

    public AIEmojiGenerator(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void generateEmoji(String taskName, String taskType, @NonNull EmojiCallback callback) {
        Log.d("AIEmojiGenerator", "Loaded key: " + BuildConfig.OPENAI_API_KEY);

        String combinedPrompt = taskName + " - " + taskType;

        if (combinedPrompt.trim().isEmpty()) {
            callback.onEmojiGenerated("📝");
            return;
        }

        String cacheKey = combinedPrompt.trim().toLowerCase();
        String cachedEmoji = prefs.getString(cacheKey, null);
        if (cachedEmoji != null) {
            Log.d("AIEmojiGenerator", "Cache hit for: " + cacheKey);
            callback.onEmojiGenerated(cachedEmoji);
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("model", "gpt-3.5-turbo");
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are an assistant that assigns a single best emoji for describing a user’s activity based on the task name and its category. Respond with exactly one emoji character, no words."));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", "Task: " + taskName + " | Type: " + taskType));
            json.put("messages", messages);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(OPENAI_URL)
                    .header("Authorization", "Bearer " + BuildConfig.OPENAI_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("AIEmojiGenerator", "Failed: " + e.getMessage());
                    callback.onError(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError(new IOException("Unexpected code " + response));
                        return;
                    }

                    try {
                        JSONObject res = new JSONObject(response.body().string());
                        String emoji = res.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                                .trim();

                        prefs.edit().putString(cacheKey, emoji).apply();
                        callback.onEmojiGenerated(emoji);
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }
            });

        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
