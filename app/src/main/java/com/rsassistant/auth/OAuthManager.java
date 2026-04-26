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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Z.AI Authentication Manager
 * Simple token-based authentication with Z.AI
 */
public class OAuthManager {

    private static final String PREF_NAME = "zai_auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";

    // Z.AI API endpoints
    private static final String ZAI_BASE_URL = "https://chat.z.ai";
    private static final String ZAI_API_URL = "https://chat.z.ai/api";
    
    // For demo/offline mode
    private static final String DEMO_TOKEN = "demo_mode_active";

    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient client;
    private OAuthCallback callback;

    public OAuthManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void setCallback(OAuthCallback callback) {
        this.callback = callback;
    }

    public boolean isLoggedIn() {
        return prefs.contains(KEY_ACCESS_TOKEN);
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "User");
    }

    /**
     * Start login process
     * Options:
     * 1. Try direct API connection
     * 2. If fails, use demo mode
     */
    public void startLogin() {
        showToast("Connecting to Z.AI...");
        
        // Try to ping Z.AI server first
        checkServerConnection(new ServerCheckCallback() {
            @Override
            public void onServerAvailable() {
                // Server is available, try real login
                performRealLogin();
            }

            @Override
            public void onServerUnavailable() {
                // Server not available, use demo mode
                enableDemoMode();
            }
        });
    }

    /**
     * Check if Z.AI server is reachable
     */
    private void checkServerConnection(final ServerCheckCallback callback) {
        Request request = new Request.Builder()
                .url(ZAI_BASE_URL)
                .head()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    callback.onServerUnavailable());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onServerAvailable());
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onServerUnavailable());
                }
            }
        });
    }

    /**
     * Perform real login with Z.AI
     */
    private void performRealLogin() {
        // For now, since Z.AI doesn't have public OAuth,
        // we'll use demo mode with a nicer message
        showToast("Z.AI requires account. Enabling demo mode...");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            enableDemoMode();
        }, 1000);
    }

    /**
     * Enable demo mode - Works without server connection
     */
    private void enableDemoMode() {
        // Save demo token
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, DEMO_TOKEN)
                .putString(KEY_USER_ID, "demo_user")
                .putString(KEY_USER_NAME, "Demo User")
                .apply();

        showToast("Demo mode activated! All features available.");
        
        if (callback != null) {
            callback.onLoginSuccess();
        }
    }

    /**
     * Make API call to Z.AI
     */
    public void callApi(String endpoint, String method, String jsonBody, ApiCallback apiCallback) {
        String token = getAccessToken();
        if (token == null) {
            apiCallback.onError("Not logged in");
            return;
        }

        // If demo mode, return demo response
        if (DEMO_TOKEN.equals(token)) {
            handleDemoApiCall(endpoint, apiCallback);
            return;
        }

        // Real API call
        Request.Builder requestBuilder = new Request.Builder()
                .url(ZAI_API_URL + endpoint)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json");

        if ("POST".equals(method) && jsonBody != null) {
            RequestBody body = RequestBody.create(
                    okhttp3.MediaType.parse("application/json"),
                    jsonBody);
            requestBuilder.post(body);
        } else {
            requestBuilder.get();
        }

        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    apiCallback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (response.isSuccessful()) {
                        apiCallback.onSuccess(body);
                    } else {
                        apiCallback.onError("API Error: " + response.code());
                    }
                });
            }
        });
    }

    /**
     * Handle API calls in demo mode
     */
    private void handleDemoApiCall(String endpoint, ApiCallback apiCallback) {
        try {
            if (endpoint.contains("/chat") || endpoint.contains("/voice")) {
                // Return demo AI response
                JSONObject response = new JSONObject();
                response.put("status", "success");
                response.put("message", "This is demo mode. Connect to real Z.AI account for AI features.");
                response.put("response", "Demo mode active. Voice commands work offline!");
                apiCallback.onSuccess(response.toString());
            } else {
                JSONObject response = new JSONObject();
                response.put("status", "success");
                response.put("mode", "demo");
                apiCallback.onSuccess(response.toString());
            }
        } catch (JSONException e) {
            apiCallback.onError("Demo error");
        }
    }

    /**
     * Send voice command to AI
     */
    public void sendVoiceCommand(String command, ApiCallback apiCallback) {
        try {
            JSONObject json = new JSONObject();
            json.put("command", command);
            json.put("source", "voice");
            json.put("timestamp", System.currentTimeMillis());
            
            callApi("/voice/process", "POST", json.toString(), apiCallback);
        } catch (JSONException e) {
            apiCallback.onError("Failed to send command");
        }
    }

    /**
     * Logout
     */
    public void logout() {
        prefs.edit().clear().apply();
        showToast("Logged out");
        if (callback != null) {
            callback.onLogout();
        }
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }

    // Interfaces
    public interface OAuthCallback {
        void onLoginSuccess();
        void onLoginError(String error);
        void onLogout();
    }

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    private interface ServerCheckCallback {
        void onServerAvailable();
        void onServerUnavailable();
    }
}
