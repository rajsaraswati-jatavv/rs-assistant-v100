package com.rsassistant.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * TTSEngine - Simple Text-to-Speech Engine
 */
public class TTSEngine {

    private static TTSEngine instance;
    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;
    private Context context;

    public interface SpeakCallback {
        void onSpeakComplete();
    }

    public static synchronized TTSEngine getInstance(Context context) {
        if (instance == null) {
            instance = new TTSEngine(context.getApplicationContext());
        }
        return instance;
    }

    private TTSEngine(Context context) {
        this.context = context;
        initTTS();
    }

    private void initTTS() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                isInitialized = (result != TextToSpeech.LANG_MISSING_DATA && 
                                 result != TextToSpeech.LANG_NOT_SUPPORTED);
                
                // Set female voice parameters
                textToSpeech.setPitch(1.15f);  // Feminine pitch
                textToSpeech.setSpeechRate(0.95f);  // Natural speed
            }
        });
    }

    public void speak(String text) {
        speak(text, null);
    }

    public void speak(String text, SpeakCallback callback) {
        if (!isInitialized || textToSpeech == null) {
            if (callback != null) callback.onSpeakComplete();
            return;
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance");
    }

    public void speakWithEmotion(String text, String emotion, SpeakCallback callback) {
        speak(text, callback);
    }

    public void speakGreeting() {
        speak("Namaste! Main Nova hoon, aapki personal assistant.");
    }

    public void speakConfirmation() {
        speak("Zaroor, main kar deti hoon!");
    }

    public void speakError() {
        speak("Maaf kijiye, thoda problem ho gayi.");
    }

    public void speakSuccess() {
        speak("Ho gaya!");
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public void setPitch(float pitch) {
        if (textToSpeech != null) {
            textToSpeech.setPitch(pitch);
        }
    }

    public void setSpeechRate(float rate) {
        if (textToSpeech != null) {
            textToSpeech.setSpeechRate(rate);
        }
    }
}
