package com.rsassistant.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    private static final String PREF_NAME = "rs_assistant_prefs";
    private static final String KEY_WAKE_WORD = "wake_word_enabled";
    private static final String KEY_BACKGROUND_SERVICE = "background_service_enabled";
    private static final String KEY_GESTURE_CONTROL = "gesture_control_enabled";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_WAKE_WORD_PHRASE = "wake_word_phrase";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isWakeWordEnabled() {
        return prefs.getBoolean(KEY_WAKE_WORD, false);
    }

    public void setWakeWordEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WAKE_WORD, enabled).apply();
    }

    public boolean isBackgroundServiceEnabled() {
        return prefs.getBoolean(KEY_BACKGROUND_SERVICE, false);
    }

    public void setBackgroundServiceEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BACKGROUND_SERVICE, enabled).apply();
    }

    public boolean isGestureControlEnabled() {
        return prefs.getBoolean(KEY_GESTURE_CONTROL, false);
    }

    public void setGestureControlEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GESTURE_CONTROL, enabled).apply();
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "english");
    }

    public void setLanguage(String language) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    public String getWakeWordPhrase() {
        return prefs.getString(KEY_WAKE_WORD_PHRASE, "hey assistant");
    }

    public void setWakeWordPhrase(String phrase) {
        prefs.edit().putString(KEY_WAKE_WORD_PHRASE, phrase.toLowerCase()).apply();
    }
}
