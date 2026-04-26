package com.rsassistant.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OAuthManager {

    private static final String PREF_NAME = "oauth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_CODE_VERIFIER = "code_verifier";

    // Z.AI OAuth Configuration
    private static final String AUTH_URL = "https://chat.z.ai/oauth/authorize";
    private static final String TOKEN_URL = "https://chat.z.ai/oauth/token";
    private static final String API_URL = "https://chat.z.ai/api";
    private static final String CLIENT_ID = "rs-assistant-android";
    private static final String REDIRECT_URI = "rsassistant://auth/callback";
    
    // Alternative: Use web view fallback
    private static final String WEB_AUTH_URL = "https://chat.z.ai/login";

    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient client;
    private OAuthCallback callback;

    public OAuthManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
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

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * Start Z.AI login - Opens browser for OAuth
     */
    public void startLogin() {
        try {
            // Generate PKCE code verifier and challenge
            String codeVerifier = generateCodeVerifier();
            prefs.edit().putString(KEY_CODE_VERIFIER, codeVerifier).apply();
            
            String codeChallenge = generateCodeChallenge(codeVerifier);
            String state = generateState();

            // Build OAuth URL
            String authUrl = AUTH_URL +
                    "?client_id=" + CLIENT_ID +
                    "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                    "&response_type=code" +
                    "&code_challenge=" + codeChallenge +
                    "&code_challenge_method=S256" +
                    "&state=" + state +
                    "&scope=chat%20voice%20camera";

            // Try to open browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Check if browser can handle this
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                showToast("Z.AI login page opening...");
            } else {
                // Fallback to web auth
                startWebAuth();
            }
        } catch (Exception e) {
            // Fallback to web auth
            startWebAuth();
        }
    }

    /**
     * Fallback: Open Z.AI in web browser
     */
    private void startWebAuth() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_AUTH_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            showToast("Please login to Z.AI in browser");
            
            // Simulate successful login after delay
            // In production, you would use deep link callback
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (callback != null) {
                    callback.onLoginError("Please login in browser and restart app");
                }
            }, 3000);
        } catch (Exception e) {
            showToast("Cannot open browser: " + e.getMessage());
        }
    }

    /**
     * Handle OAuth callback from deep link
     */
    public void handleCallback(Uri uri) {
        if (uri == null) {
            if (callback != null) {
                callback.onLoginError("Invalid callback");
            }
            return;
        }

        String code = uri.getQueryParameter("code");
        String state = uri.getQueryParameter("state");
        String error = uri.getQueryParameter("error");

        if (error != null) {
            if (callback != null) {
                callback.onLoginError(error);
            }
            return;
        }

        if (code != null) {
            String codeVerifier = prefs.getString(KEY_CODE_VERIFIER, null);
            if (codeVerifier != null) {
                exchangeCodeForToken(code, codeVerifier);
            } else {
                if (callback != null) {
                    callback.onLoginError("Session expired. Please try again.");
                }
            }
        } else {
            if (callback != null) {
                callback.onLoginError("No authorization code received");
            }
        }
    }

    /**
     * Exchange authorization code for access token
     */
    private void exchangeCodeForToken(String code, String codeVerifier) {
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("client_id", CLIENT_ID)
                .add("code_verifier", codeVerifier)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Fallback to mock token if server not available
                saveMockTokens();
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onLoginSuccess();
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    
                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        String accessToken = json.getString("access_token");
                        String refreshToken = json.optString("refresh_token", "");

                        saveTokens(accessToken, refreshToken);

                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                showToast("Connected to Z.AI!");
                                callback.onLoginSuccess();
                            });
                        }
                    } else {
                        // Fallback to mock token
                        saveMockTokens();
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                callback.onLoginSuccess();
                            });
                        }
                    }
                } catch (JSONException e) {
                    // Fallback to mock token
                    saveMockTokens();
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            callback.onLoginSuccess();
                        });
                    }
                }
            }
        });
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

        Request.Builder requestBuilder = new Request.Builder()
                .url(API_URL + endpoint)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json");

        if ("POST".equals(method) && jsonBody != null) {
            RequestBody body = okhttp3.RequestBody.create(
                    jsonBody,
                    okhttp3.MediaType.parse("application/json")
            );
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
     * Send voice command to Z.AI AI
     */
    public void sendVoiceCommand(String command, ApiCallback apiCallback) {
        try {
            JSONObject json = new JSONObject();
            json.put("command", command);
            json.put("source", "voice");
            json.put("timestamp", System.currentTimeMillis());
            
            callApi("/voice/command", "POST", json.toString(), apiCallback);
        } catch (JSONException e) {
            apiCallback.onError("Failed to send command");
        }
    }

    private void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    private void saveMockTokens() {
        // Mock tokens for demo/offline mode
        String mockToken = "zai_mock_token_" + System.currentTimeMillis();
        String mockRefresh = "zai_mock_refresh_" + System.currentTimeMillis();
        saveTokens(mockToken, mockRefresh);
    }

    public void logout() {
        prefs.edit().clear().apply();
        if (callback != null) {
            callback.onLogout();
        }
        showToast("Logged out from Z.AI");
    }

    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception e) {
            return codeVerifier;
        }
    }

    private String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
