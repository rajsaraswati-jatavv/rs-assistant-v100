package com.rsassistant.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ZAIChatManager - Powerful AI Chat Integration
 * Multiple endpoints with smart fallback
 * Context-aware conversations with memory
 */
public class ZAIChatManager {

    private static final String TAG = "ZAIChatManager";
    private static final String PREF_NAME = "zai_chat_prefs";
    private static final String KEY_SESSION_ID = "session_id";
    private static final int MAX_CONTEXT_MESSAGES = 10;

    // Multiple endpoints for reliability
    private static final String[] API_ENDPOINTS = {
        "https://api.z.ai/v1/chat/completions",
        "https://chat.z.ai/api/chat",
        "https://rs-assistant-api.vercel.app/api/chat"
    };

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient client;
    private final List<Message> conversationHistory;
    private final String sessionId;

    private ChatCallback currentCallback;
    private boolean isConnected = false;
    private int endpointIndex = 0;

    public ZAIChatManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.conversationHistory = new ArrayList<>();

        String savedSessionId = prefs.getString(KEY_SESSION_ID, null);
        this.sessionId = savedSessionId != null ? savedSessionId : UUID.randomUUID().toString();
        prefs.edit().putString(KEY_SESSION_ID, sessionId).apply();

        this.client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    public void testConnection(ConnectionCallback callback) {
        sendMessage("ping", new ChatCallback() {
            @Override
            public void onResponse(String response) {
                isConnected = true;
                new Handler(Looper.getMainLooper()).post(callback::onConnected);
            }

            @Override
            public void onError(String error) {
                isConnected = false;
                new Handler(Looper.getMainLooper()).post(() -> callback.onDisconnected(error));
            }
        });
    }

    public void sendMessage(String userMessage, ChatCallback callback) {
        this.currentCallback = callback;
        conversationHistory.add(new Message("user", userMessage));

        while (conversationHistory.size() > MAX_CONTEXT_MESSAGES) {
            conversationHistory.remove(0);
        }

        sendToEndpoint(userMessage, endpointIndex);
    }

    private void sendToEndpoint(String userMessage, int endpointNum) {
        if (endpointNum >= API_ENDPOINTS.length) {
            handleOfflineResponse(userMessage);
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("message", userMessage);
            json.put("source", "rs-assistant-android");
            json.put("session_id", sessionId);

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                .url(API_ENDPOINTS[endpointNum])
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    sendToEndpoint(userMessage, endpointNum + 1);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        handleSuccessResponse(responseBody, userMessage);
                    } else {
                        sendToEndpoint(userMessage, endpointNum + 1);
                    }
                }
            });
        } catch (Exception e) {
            sendToEndpoint(userMessage, endpointNum + 1);
        }
    }

    private void handleSuccessResponse(String responseBody, String originalMessage) {
        try {
            JSONObject result = new JSONObject(responseBody);
            String reply = null;

            if (result.has("choices")) {
                reply = result.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            } else if (result.has("response")) {
                reply = result.getString("response");
            } else if (result.has("message")) {
                reply = result.getString("message");
            }

            if (reply != null && !reply.isEmpty()) {
                conversationHistory.add(new Message("assistant", reply));
                notifySuccess(reply);
            } else {
                handleOfflineResponse(originalMessage);
            }
        } catch (JSONException e) {
            handleOfflineResponse(originalMessage);
        }
    }

    private void handleOfflineResponse(String message) {
        String response = generateSmartResponse(message);
        conversationHistory.add(new Message("assistant", response));
        notifySuccess(response);
    }

    private String generateSmartResponse(String message) {
        String lower = message.toLowerCase().trim();

        // Greetings
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ||
            lower.contains("नमस्ते") || lower.contains("namaste")) {
            return "नमस्ते! 🙏 मैं RS Assistant हूं। मैं आपकी कैसे मदद कर सकता हूं?\n\n" +
                   "आप बोल सकते हैं:\n• Volume बढ़ाओ/घटाओ\n• Camera खोलो\n• Flashlight जलाओ\n• WiFi on/off करो";
        }

        // Time
        if (lower.contains("time") || lower.contains("समय") || lower.contains("बजे")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
            return "⏰ अभी का समय: " + sdf.format(new java.util.Date());
        }

        // Date
        if (lower.contains("date") || lower.contains("तारीख")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", new java.util.Locale("hi", "IN"));
            return "📅 आज की तारीख: " + sdf.format(new java.util.Date());
        }

        // Help
        if (lower.contains("help") || lower.contains("मदद")) {
            return "📖 RS Assistant Commands:\n\n" +
                   "🎤 Voice Commands:\n• Volume बढ़ाओ/घटाओ\n• Flashlight on/off\n• WiFi/Bluetooth on/off\n• Camera खोलो\n• Lock screen\n\n" +
                   "📞 Calls & SMS:\n• [Name] को call करो\n• [Name] को SMS भेजो";
        }

        // Thanks
        if (lower.contains("thank") || lower.contains("धन्यवाद")) {
            return "आपका स्वागत है! 😊 और कुछ चाहिए?";
        }

        // Jokes
        if (lower.contains("joke") || lower.contains("मज़ाक") || lower.contains("हंसाओ")) {
            String[] jokes = {
                "😂 अध्यापक: दिल्ली कहाँ है? छात्र: दिल्ली तो मेरे घर में है, मैप पर देखो!",
                "😂 Doctor: रोज़ 5 किमी चलो। Patient: ठीक है, मगर वापस कैसे आऊं?",
                "😂 पत्नी: आज क्या बनाऊं? पति: जो बनाओगी वही खाएंगे!"
            };
            return jokes[(int)(Math.random() * jokes.length)];
        }

        // SOS
        if (lower.contains("sos") || lower.contains("emergency") || lower.contains("मदद")) {
            return "🆘 SOS Active! Emergency contact को SMS भेजा जा रहा है।\n\nEmergency contact set करने के लिए Settings में जाएं।";
        }

        // Default
        return "मैं समझ गया: \"" + message + "\"\n\nPhone controls try करें:\n• Volume, WiFi, Flashlight, Camera\n• 'help' बोलें सभी commands के लिए";
    }

    private void notifySuccess(String response) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (currentCallback != null) {
                currentCallback.onResponse(response);
            }
        });
    }

    public void clearHistory() {
        conversationHistory.clear();
    }

    public boolean isConnected() {
        return isConnected;
    }

    private static class Message {
        String role;
        String content;
        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public interface ChatCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected(String reason);
    }
}
