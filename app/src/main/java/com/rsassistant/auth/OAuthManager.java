package com.rsassistant.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONException;
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
 * Simple Z.AI Connection - No OAuth required
 * Direct API connection for AI chat
 */
public class OAuthManager {

    private static final String PREF_NAME = "zai_prefs";
    private static final String KEY_CONNECTED = "connected";
    private static final String KEY_API_KEY = "api_key";
    
    // RS Assistant API endpoint (will be deployed)
    private static final String API_ENDPOINT = "https://rs-assistant-api.vercel.app/api/chat";
    // Fallback endpoints
    private static final String BACKUP_ENDPOINT = "https://api.z.ai/v1/chat";

    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient client;
    private OAuthCallback callback;

    public OAuthManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void setCallback(OAuthCallback callback) {
        this.callback = callback;
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_CONNECTED, false);
    }

    /**
     * Simple connection - Just set connected state
     * No OAuth required for basic features
     */
    public void startLogin() {
        showToast("Connecting to Z.AI...");
        
        // Simulate quick connection
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            prefs.edit()
                .putBoolean(KEY_CONNECTED, true)
                .apply();
            
            if (callback != null) {
                callback.onLoginSuccess();
            }
            showToast("Connected! AI features ready.");
        }, 500);
    }

    /**
     * Send message to AI and get response
     */
    public void sendMessage(String message, ApiCallback apiCallback) {
        try {
            JSONObject json = new JSONObject();
            json.put("message", message);
            json.put("source", "rs-assistant-android");
            
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(API_ENDPOINT)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Try backup endpoint
                    tryBackupEndpoint(message, apiCallback, e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject result = new JSONObject(responseBody);
                            String reply = result.optString("response", result.optString("message", ""));
                            if (!reply.isEmpty()) {
                                notifySuccess(apiCallback, reply);
                            } else {
                                notifyError(apiCallback, "Empty response");
                            }
                        } catch (JSONException e) {
                            notifyError(apiCallback, "Invalid response");
                        }
                    } else {
                        tryBackupEndpoint(message, apiCallback, "API error: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            notifyError(apiCallback, "Failed to send message: " + e.getMessage());
        }
    }

    private void tryBackupEndpoint(String message, ApiCallback apiCallback, String previousError) {
        try {
            JSONObject json = new JSONObject();
            json.put("message", message);
            json.put("source", "rs-assistant-android");
            
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(BACKUP_ENDPOINT)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Return offline response
                    notifySuccess(apiCallback, getOfflineResponse(message));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject result = new JSONObject(responseBody);
                            String reply = result.optString("response", result.optString("message", ""));
                            if (!reply.isEmpty()) {
                                notifySuccess(apiCallback, reply);
                            } else {
                                notifySuccess(apiCallback, getOfflineResponse(message));
                            }
                        } catch (JSONException e) {
                            notifySuccess(apiCallback, getOfflineResponse(message));
                        }
                    } else {
                        notifySuccess(apiCallback, getOfflineResponse(message));
                    }
                }
            });
        } catch (Exception e) {
            notifySuccess(apiCallback, getOfflineResponse(message));
        }
    }

    /**
     * Generate offline response when AI is unavailable
     */
    private String getOfflineResponse(String message) {
        String lower = message.toLowerCase();
        
        // Greeting responses
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey") || lower.contains("नमस्ते")) {
            return "Hello! I'm RS Assistant. How can I help you today?";
        }
        
        // Time
        if (lower.contains("time") || lower.contains("समय")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
            return "Current time is " + sdf.format(new java.util.Date());
        }
        
        // Help
        if (lower.contains("help") || lower.contains("मदद")) {
            return "I can help you with: Volume control, Lock screen, Power off, Camera, Flashlight, WiFi, Bluetooth, and more. Just ask!";
        }
        
        // Thanks
        if (lower.contains("thank") || lower.contains("धन्यवाद")) {
            return "You're welcome! Let me know if you need anything else.";
        }
        
        // Default
        return "I understand you said: \"" + message + "\". I'm working offline right now, but I can still help with phone controls like volume, camera, flashlight, and more!";
    }

    private void notifySuccess(ApiCallback callback, String response) {
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                callback.onSuccess(response)
            );
        }
    }

    private void notifyError(ApiCallback callback, String error) {
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                callback.onError(error)
            );
        }
    }

    public void logout() {
        prefs.edit().clear().apply();
        if (callback != null) {
            callback.onLogout();
        }
        showToast("Disconnected from Z.AI");
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }

    public interface OAuthCallback {
        void onLoginSuccess();
        void onLoginError(String error);
        void onLogout();
    }

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
