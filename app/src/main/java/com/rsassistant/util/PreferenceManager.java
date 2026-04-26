package com.rsassistant.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    private static final String PREF_NAME = "rs_assistant_prefs_v101";

    // Keys
    private static final String KEY_WAKE_WORD = "wake_word_enabled";
    private static final String KEY_ALWAYS_ON = "always_on_enabled";
    private static final String KEY_VOICE_RESPONSE = "voice_response_enabled";
    private static final String KEY_GESTURE_CONTROL = "gesture_control_enabled";
    private static final String KEY_FACE_DETECTION = "face_detection_enabled";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_WAKE_WORD_PHRASE = "wake_word_phrase";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_FIRST_RUN = "first_run";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Wake Word
    public boolean isWakeWordEnabled() {
        return prefs.getBoolean(KEY_WAKE_WORD, false);
    }

    public void setWakeWordEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WAKE_WORD, enabled).apply();
    }

    // Always On
    public boolean isAlwaysOnEnabled() {
        return prefs.getBoolean(KEY_ALWAYS_ON, true);
    }

    public void setAlwaysOnEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALWAYS_ON, enabled).apply();
    }

    // Voice Response
    public boolean isVoiceResponseEnabled() {
        return prefs.getBoolean(KEY_VOICE_RESPONSE, true);
    }

    public void setVoiceResponseEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_RESPONSE, enabled).apply();
    }

    // Gesture Control
    public boolean isGestureControlEnabled() {
        return prefs.getBoolean(KEY_GESTURE_CONTROL, false);
    }

    public void setGestureControlEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GESTURE_CONTROL, enabled).apply();
    }

    // Face Detection
    public boolean isFaceDetectionEnabled() {
        return prefs.getBoolean(KEY_FACE_DETECTION, false);
    }

    public void setFaceDetectionEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FACE_DETECTION, enabled).apply();
    }

    // Language
    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "english");
    }

    public void setLanguage(String language) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    // Wake Word Phrase
    public String getWakeWordPhrase() {
        return prefs.getString(KEY_WAKE_WORD_PHRASE, "hey assistant");
    }

    public void setWakeWordPhrase(String phrase) {
        prefs.edit().putString(KEY_WAKE_WORD_PHRASE, phrase.toLowerCase()).apply();
    }

    // Login
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    // Access Token
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public void setAccessToken(String token) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    // Refresh Token
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public void setRefreshToken(String token) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply();
    }

    // First Run
    public boolean isFirstRun() {
        return prefs.getBoolean(KEY_FIRST_RUN, true);
    }

    public void setFirstRunComplete() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
    }

    // Clear All
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
