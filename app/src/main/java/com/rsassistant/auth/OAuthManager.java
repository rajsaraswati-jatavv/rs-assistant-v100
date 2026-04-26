package com.rsassistant.auth;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

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

    private static final String AUTH_URL = "https://chat.z.ai/oauth/authorize";
    private static final String TOKEN_URL = "https://chat.z.ai/oauth/token";
    private static final String CLIENT_ID = "rs-assistant";
    private static final String REDIRECT_URI = "rsassistant://callback";

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

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public void startLogin() {
        String codeVerifier = generateCodeVerifier();
        prefs.edit().putString(KEY_CODE_VERIFIER, codeVerifier).apply();

        String codeChallenge = generateCodeChallenge(codeVerifier);

        String authUrl = AUTH_URL +
                "?client_id=" + CLIENT_ID +
                "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                "&response_type=code" +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256";

        // In a real app, you would open this URL in a browser
        // For now, we'll simulate successful login
        simulateLogin();
    }

    private void simulateLogin() {
        // Simulate OAuth login for demo purposes
        // In production, this would be handled via browser redirect
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String mockToken = "mock_access_token_" + System.currentTimeMillis();
            String mockRefresh = "mock_refresh_token_" + System.currentTimeMillis();
            saveTokens(mockToken, mockRefresh);

            if (callback != null) {
                callback.onLoginSuccess();
            }
        }, 1000);
    }

    public void handleCallback(Uri uri) {
        String code = uri.getQueryParameter("code");
        String codeVerifier = prefs.getString(KEY_CODE_VERIFIER, null);

        if (code != null && codeVerifier != null) {
            exchangeCodeForToken(code, codeVerifier);
        }
    }

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
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onLoginError(e.getMessage()));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);

                    if (response.isSuccessful()) {
                        String accessToken = json.getString("access_token");
                        String refreshToken = json.optString("refresh_token");

                        saveTokens(accessToken, refreshToken);

                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onLoginSuccess());
                        }
                    } else {
                        String error = json.optString("error_description", "Login failed");
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onLoginError(error));
                        }
                    }
                } catch (JSONException e) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onLoginError("Invalid response"));
                    }
                }
            }
        });
    }

    private void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public void logout() {
        prefs.edit().clear().apply();
        if (callback != null) {
            callback.onLogout();
        }
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

    public interface OAuthCallback {
        void onLoginSuccess();
        void onLoginError(String error);
        void onLogout();
    }
}
