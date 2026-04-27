package com.rsassistant.voice;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Nova TTS Engine - Advanced Human-like Female Text-to-Speech Engine
 * 
 * A sophisticated TTS engine designed for the RS Assistant Android app featuring:
 * - Nova-style female human-like voice with natural pitch and tone
 * - Emotion support (happy, sad, excited, calm, concerned, neutral)
 * - Natural speech patterns with intelligent pauses
 * - Hindi-English bilingual support with optimized pronunciation
 * - Multiple speaking modes (greeting, command, confirmation, error, casual)
 * - Context-aware voice variation
 * - Dynamic speed and pitch control
 * - SSML-like enhancements for natural speech synthesis
 * - Whisper mode for nighttime use
 * - Custom voice profiles (Nova, Classic, Assistant)
 * 
 * @author RS Assistant Team
 * @version 2.0.0
 */
public class TTSEngine {

    private static final String TAG = "NovaTTSEngine";
    private static final String PREFS_NAME = "nova_tts_prefs";
    private static final String KEY_VOICE_PROFILE = "voice_profile";
    private static final String KEY_WHISPER_MODE = "whisper_mode";
    private static final String KEY_CURRENT_EMOTION = "current_emotion";
    
    // Default voice parameters for Nova female voice
    private static final float DEFAULT_NOVA_PITCH = 1.18f;          // Slightly higher for feminine voice
    private static final float DEFAULT_NOVA_SPEED = 0.92f;          // Natural conversational speed
    private static final float WHISPER_PITCH = 0.85f;               // Lower pitch for whisper
    private static final float WHISPER_SPEED = 0.78f;               // Slower for whisper
    
    // Emotion-specific voice modifications
    private static final float EMOTION_HAPPY_PITCH_MOD = 0.08f;
    private static final float EMOTION_HAPPY_SPEED_MOD = 0.05f;
    private static final float EMOTION_SAD_PITCH_MOD = -0.15f;
    private static final float EMOTION_SAD_SPEED_MOD = -0.12f;
    private static final float EMOTION_EXCITED_PITCH_MOD = 0.15f;
    private static final float EMOTION_EXCITED_SPEED_MOD = 0.12f;
    private static final float EMOTION_CALM_PITCH_MOD = -0.05f;
    private static final float EMOTION_CALM_SPEED_MOD = -0.08f;
    private static final float EMOTION_CONCERNED_PITCH_MOD = -0.08f;
    private static final float EMOTION_CONCERNED_SPEED_MOD = -0.05f;
    private static final float EMOTION_EMPATHETIC_PITCH_MOD = -0.03f;
    private static final float EMOTION_EMPATHETIC_SPEED_MOD = -0.06f;
    private static final float EMOTION_THOUGHTFUL_PITCH_MOD = -0.02f;
    private static final float EMOTION_THOUGHTFUL_SPEED_MOD = -0.10f;

    // Singleton instance
    private static TTSEngine instance;
    
    // Core components
    private TextToSpeech textToSpeech;
    private Context context;
    private SharedPreferences preferences;
    private boolean isInitialized = false;
    
    // Voice state
    private VoiceProfile currentProfile = VoiceProfile.NOVA;
    private Emotion currentEmotion = Emotion.NEUTRAL;
    private boolean whisperModeEnabled = false;
    private SpeakingMode currentSpeakingMode = SpeakingMode.CASUAL;
    private Language currentLanguage = Language.HINGLISH;
    
    // Voice parameters
    private float basePitch = DEFAULT_NOVA_PITCH;
    private float baseSpeed = DEFAULT_NOVA_SPEED;
    private float currentPitch = DEFAULT_NOVA_PITCH;
    private float currentSpeed = DEFAULT_NOVA_SPEED;
    
    // Utterance tracking
    private final AtomicInteger utteranceCounter = new AtomicInteger(0);
    private final Map<String, SpeakCallback> callbackMap = new ConcurrentHashMap<>();
    private final Map<String, String> textMap = new ConcurrentHashMap<>();
    private final List<SpeakListener> speakListeners = new ArrayList<>();
    
    // Natural pause patterns (in milliseconds)
    private static final int SHORT_PAUSE = 150;
    private static final int MEDIUM_PAUSE = 300;
    private static final int LONG_PAUSE = 500;
    private static final int SENTENCE_PAUSE = 400;

    // ==================== ENUMS ====================
    
    /**
     * Emotion types for voice expression
     */
    public enum Emotion {
        NEUTRAL(0, "neutral"),
        HAPPY(1, "happy"),
        SAD(2, "sad"),
        EXCITED(3, "excited"),
        CALM(4, "calm"),
        CONCERNED(5, "concerned"),
        EMPATHETIC(6, "empathetic"),
        THOUGHTFUL(7, "thoughtful");
        
        private final int id;
        private final String name;
        
        Emotion(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        
        public static Emotion fromId(int id) {
            for (Emotion emotion : values()) {
                if (emotion.id == id) return emotion;
            }
            return NEUTRAL;
        }
    }
    
    /**
     * Speaking style/mode for different contexts
     */
    public enum SpeakingStyle {
        GREETING,       // Warm, welcoming
        COMMAND,        // Clear, authoritative
        CONFIRMATION,   // Reassuring, confident
        ERROR,          // Apologetic, soft
        CASUAL,         // Friendly, relaxed
        FORMAL,         // Professional, clear
        WARNING,        // Alert, urgent
        SUCCESS,        // Celebratory, upbeat
        QUESTION,       // Inquisitive, rising tone
        EMPATHY         // Soft, understanding
    }
    
    /**
     * Voice profiles with distinct characteristics
     */
    public enum VoiceProfile {
        NOVA("nova", 1.18f, 0.92f, "Nova - Personal Assistant"),
        CLASSIC("classic", 1.0f, 1.0f, "Classic - Standard Voice"),
        ASSISTANT("assistant", 1.12f, 0.95f, "Assistant - Professional"),
        COMPANION("companion", 1.22f, 0.88f, "Companion - Warm & Friendly"),
        MENTOR("mentor", 1.05f, 0.85f, "Mentor - Wise & Calm");
        
        private final String id;
        private final float defaultPitch;
        private final float defaultSpeed;
        private final String displayName;
        
        VoiceProfile(String id, float pitch, float speed, String displayName) {
            this.id = id;
            this.defaultPitch = pitch;
            this.defaultSpeed = speed;
            this.displayName = displayName;
        }
        
        public String getId() { return id; }
        public float getDefaultPitch() { return defaultPitch; }
        public float getDefaultSpeed() { return defaultSpeed; }
        public String getDisplayName() { return displayName; }
        
        public static VoiceProfile fromId(String id) {
            for (VoiceProfile profile : values()) {
                if (profile.id.equals(id)) return profile;
            }
            return NOVA;
        }
    }
    
    /**
     * Speaking mode for context-aware voice modulation
     */
    public enum SpeakingMode {
        NORMAL,         // Regular speech
        WHISPER,        // Quiet, nighttime mode
        ANNOUNCEMENT,   // Loud, clear
        PRIVATE,        // Earpiece only
        BLUETOOTH       // Bluetooth device
    }
    
    /**
     * Language support
     */
    public enum Language {
        HINDI("hi-IN", "Hindi"),
        ENGLISH("en-US", "English"),
        HINGLISH("hi-en", "Hindi-English Mix"),
        ENGLISH_IN("en-IN", "Indian English");
        
        private final String code;
        private final String displayName;
        
        Language(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
    }

    // ==================== CALLBACKS ====================
    
    /**
     * Callback interface for speech completion
     */
    public interface SpeakCallback {
        void onSpeakComplete();
        void onSpeakError(String error);
    }
    
    /**
     * Listener interface for speech events
     */
    public interface SpeakListener {
        void onSpeakStart(String text);
        void onSpeakComplete(String text);
        void onSpeakError(String text, String error);
    }

    // ==================== SINGLETON ====================
    
    /**
     * Get singleton instance of TTSEngine
     * @param context Application context
     * @return TTSEngine instance
     */
    public static synchronized TTSEngine getInstance(Context context) {
        if (instance == null) {
            instance = new TTSEngine(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor for singleton pattern
     */
    private TTSEngine(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadSavedSettings();
        initTTS();
    }

    // ==================== INITIALIZATION ====================
    
    /**
     * Initialize the Text-to-Speech engine with Nova voice settings
     */
    private void initTTS() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                setupTTSVoice();
                setupUtteranceListener();
                isInitialized = true;
                Log.i(TAG, "Nova TTS Engine initialized successfully");
            } else {
                Log.e(TAG, "TTS initialization failed with status: " + status);
                isInitialized = false;
            }
        });
    }
    
    /**
     * Setup TTS voice with Nova female characteristics
     */
    private void setupTTSVoice() {
        // Try to set Hindi locale first, fallback to Indian English, then US English
        trySetLocale(
            new Locale("hi", "IN"),
            new Locale("en", "IN"),
            Locale.US
        );
        
        // Apply Nova voice profile
        applyVoiceProfile(currentProfile);
        
        // Set female voice if available
        selectFemaleVoice();
        
        Log.d(TAG, "Voice setup complete - Pitch: " + currentPitch + ", Speed: " + currentSpeed);
    }
    
    /**
     * Try multiple locales in order of preference
     */
    private void trySetLocale(Locale... locales) {
        for (Locale locale : locales) {
            int result = textToSpeech.setLanguage(locale);
            if (result != TextToSpeech.LANG_MISSING_DATA && 
                result != TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "Set locale to: " + locale);
                return;
            }
        }
    }
    
    /**
     * Select female voice from available voices
     */
    private void selectFemaleVoice() {
        try {
            Set<Voice> voices = textToSpeech.getVoices();
            if (voices != null) {
                for (Voice voice : voices) {
                    String name = voice.getName().toLowerCase();
                    // Look for female voice indicators
                    if ((name.contains("female") || name.contains("woman") || 
                         name.contains("girl") || name.contains("hindi-female") ||
                         name.contains("hi-in") || name.contains("en-in-female")) &&
                        !voice.isNetworkConnectionRequired()) {
                        textToSpeech.setVoice(voice);
                        Log.d(TAG, "Selected female voice: " + voice.getName());
                        return;
                    }
                }
                // If no female voice found, try to get any Hindi or Indian English voice
                for (Voice voice : voices) {
                    String name = voice.getName().toLowerCase();
                    if ((name.contains("hindi") || name.contains("hi-in") || name.contains("en-in")) &&
                        !voice.isNetworkConnectionRequired()) {
                        textToSpeech.setVoice(voice);
                        Log.d(TAG, "Selected Hindi/Indian voice: " + voice.getName());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not select specific voice: " + e.getMessage());
        }
    }
    
    /**
     * Setup utterance progress listener for callbacks
     */
    private void setupUtteranceListener() {
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                String text = textMap.get(utteranceId);
                notifySpeakStart(text != null ? text : "");
            }
            
            @Override
            public void onDone(String utteranceId) {
                String text = textMap.remove(utteranceId);
                SpeakCallback callback = callbackMap.remove(utteranceId);
                if (callback != null) {
                    callback.onSpeakComplete();
                }
                notifySpeakComplete(text != null ? text : "");
            }
            
            @Override
            public void onError(String utteranceId) {
                onError(utteranceId, -1);
            }
            
            @Override
            public void onError(String utteranceId, int errorCode) {
                String text = textMap.remove(utteranceId);
                SpeakCallback callback = callbackMap.remove(utteranceId);
                String error = "TTS Error code: " + errorCode;
                if (callback != null) {
                    callback.onSpeakError(error);
                }
                notifySpeakError(text != null ? text : "", error);
            }
        });
    }
    
    // ==================== CORE SPEAK METHODS ====================
    
    /**
     * Speak with current settings
     */
    public void speak(String text) {
        speak(text, null);
    }
    
    /**
     * Speak with completion callback
     */
    public void speak(String text, SpeakCallback callback) {
        speak(text, currentEmotion, currentSpeakingMode, callback);
    }
    
    /**
     * Speak with specific emotion and mode
     */
    public void speak(String text, Emotion emotion, SpeakingMode mode, SpeakCallback callback) {
        if (!isInitialized || textToSpeech == null) {
            if (callback != null) callback.onSpeakError("TTS not initialized");
            return;
        }
        
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onSpeakComplete();
            return;
        }
        
        // Apply emotion modifications
        applyEmotionModifications(emotion);
        
        // Process text for natural speech
        String processedText = processText(text);
        
        // Generate unique utterance ID
        String utteranceId = generateUtteranceId();
        callbackMap.put(utteranceId, callback);
        textMap.put(utteranceId, text);
        
        // Speak
        textToSpeech.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }
    
    /**
     * Process text for natural speech
     */
    private String processText(String text) {
        // Clean up text
        String processed = text.trim();
        
        // Add natural breaks for punctuation
        processed = processed.replace(". ", ". ... ");
        processed = processed.replace("! ", "! ... ");
        processed = processed.replace("? ", "? ... ");
        processed = processed.replace(", ", ", .. ");
        
        return processed;
    }
    
    // ==================== EMOTION METHODS ====================
    
    /**
     * Set current emotion for voice expression
     */
    public void setEmotion(Emotion emotion) {
        this.currentEmotion = emotion;
        preferences.edit().putInt(KEY_CURRENT_EMOTION, emotion.getId()).apply();
        Log.d(TAG, "Emotion set to: " + emotion.getName());
    }
    
    public Emotion getEmotion() {
        return currentEmotion;
    }
    
    /**
     * Apply emotion modifications to voice parameters
     */
    private void applyEmotionModifications(Emotion emotion) {
        float pitchMod = 0f;
        float speedMod = 0f;
        
        switch (emotion) {
            case HAPPY:
                pitchMod = EMOTION_HAPPY_PITCH_MOD;
                speedMod = EMOTION_HAPPY_SPEED_MOD;
                break;
            case SAD:
                pitchMod = EMOTION_SAD_PITCH_MOD;
                speedMod = EMOTION_SAD_SPEED_MOD;
                break;
            case EXCITED:
                pitchMod = EMOTION_EXCITED_PITCH_MOD;
                speedMod = EMOTION_EXCITED_SPEED_MOD;
                break;
            case CALM:
                pitchMod = EMOTION_CALM_PITCH_MOD;
                speedMod = EMOTION_CALM_SPEED_MOD;
                break;
            case CONCERNED:
                pitchMod = EMOTION_CONCERNED_PITCH_MOD;
                speedMod = EMOTION_CONCERNED_SPEED_MOD;
                break;
            case EMPATHETIC:
                pitchMod = EMOTION_EMPATHETIC_PITCH_MOD;
                speedMod = EMOTION_EMPATHETIC_SPEED_MOD;
                break;
            case THOUGHTFUL:
                pitchMod = EMOTION_THOUGHTFUL_PITCH_MOD;
                speedMod = EMOTION_THOUGHTFUL_SPEED_MOD;
                break;
            default:
                break;
        }
        
        // Apply modifications with whisper mode consideration
        float targetPitch = whisperModeEnabled ? WHISPER_PITCH : basePitch;
        float targetSpeed = whisperModeEnabled ? WHISPER_SPEED : baseSpeed;
        
        currentPitch = targetPitch + pitchMod;
        currentSpeed = targetSpeed + speedMod;
        
        // Clamp values to valid range
        currentPitch = Math.max(0.5f, Math.min(2.0f, currentPitch));
        currentSpeed = Math.max(0.5f, Math.min(2.0f, currentSpeed));
        
        // Apply to TTS
        if (textToSpeech != null) {
            textToSpeech.setPitch(currentPitch);
            textToSpeech.setSpeechRate(currentSpeed);
        }
    }
    
    // ==================== SPEAKING STYLE METHODS ====================
    
    /**
     * Speak with specific style
     */
    public void speakWithStyle(String text, SpeakingStyle style) {
        speakWithStyle(text, style, null);
    }
    
    /**
     * Speak with style and callback
     */
    public void speakWithStyle(String text, SpeakingStyle style, SpeakCallback callback) {
        String processedText = applyStyleModifiers(text, style);
        Emotion styleEmotion = getStyleEmotion(style);
        speak(processedText, styleEmotion, currentSpeakingMode, callback);
    }
    
    /**
     * Apply style-specific modifications to text
     */
    private String applyStyleModifiers(String text, SpeakingStyle style) {
        if (text == null || text.isEmpty()) return "";
        
        switch (style) {
            case GREETING:
                return addGreetingWarmth(text);
            case COMMAND:
                return clarifyCommand(text);
            case CONFIRMATION:
                return addConfirmation(text);
            case ERROR:
                return softenError(text);
            case CASUAL:
                return text; // Keep as-is
            case FORMAL:
                return formalize(text);
            case WARNING:
                return addUrgency(text);
            case SUCCESS:
                return addCelebration(text);
            case QUESTION:
                return text; // Keep as-is
            case EMPATHY:
                return addEmpathy(text);
            default:
                return text;
        }
    }
    
    /**
     * Get emotion associated with speaking style
     */
    private Emotion getStyleEmotion(SpeakingStyle style) {
        switch (style) {
            case GREETING:
            case SUCCESS:
                return Emotion.HAPPY;
            case COMMAND:
                return Emotion.CALM;
            case CONFIRMATION:
                return Emotion.NEUTRAL;
            case ERROR:
                return Emotion.CONCERNED;
            case WARNING:
                return Emotion.EXCITED;
            case EMPATHY:
                return Emotion.EMPATHETIC;
            case QUESTION:
                return Emotion.THOUGHTFUL;
            default:
                return Emotion.NEUTRAL;
        }
    }
    
    private String addGreetingWarmth(String text) {
        if (!text.contains("!") && !text.endsWith(".")) {
            return text + "!";
        }
        return text;
    }
    
    private String clarifyCommand(String text) {
        // Commands should be clear
        return text;
    }
    
    private String addConfirmation(String text) {
        return text;
    }
    
    private String softenError(String text) {
        // Add apologetic tone
        if (!text.toLowerCase().startsWith("sorry") && 
            !text.toLowerCase().startsWith("maaf") &&
            !text.toLowerCase().startsWith("i apologize")) {
            return "I'm sorry, " + text.toLowerCase();
        }
        return text;
    }
    
    private String formalize(String text) {
        return text;
    }
    
    private String addUrgency(String text) {
        return text;
    }
    
    private String addCelebration(String text) {
        if (!text.contains("!")) {
            return text + "!";
        }
        return text;
    }
    
    private String addEmpathy(String text) {
        return text;
    }
    
    // ==================== NATURAL PAUSES ====================
    
    /**
     * Speak with natural pauses between sentences
     */
    public void speakWithNaturalPauses(String text) {
        speakWithNaturalPauses(text, null);
    }
    
    /**
     * Speak with natural pauses and callback
     */
    public void speakWithNaturalPauses(String text, SpeakCallback callback) {
        if (!isInitialized || textToSpeech == null || text == null || text.isEmpty()) {
            if (callback != null) callback.onSpeakComplete();
            return;
        }
        
        // Split into sentences
        String[] sentences = splitIntoSentences(text);
        speakWithNaturalPauses(sentences, callback);
    }
    
    /**
     * Speak an array of sentences with natural pauses between them
     */
    public void speakWithNaturalPauses(String[] sentences) {
        speakWithNaturalPauses(sentences, null);
    }
    
    /**
     * Speak sentences with pauses and callback
     */
    public void speakWithNaturalPauses(String[] sentences, SpeakCallback callback) {
        if (!isInitialized || sentences == null || sentences.length == 0) {
            if (callback != null) callback.onSpeakComplete();
            return;
        }
        
        // Queue all sentences with pauses
        textToSpeech.stop();
        
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (sentence.isEmpty()) continue;
            
            String utteranceId = generateUtteranceId();
            
            if (i == sentences.length - 1) {
                callbackMap.put(utteranceId, callback);
            }
            textMap.put(utteranceId, sentence);
            
            // Queue each sentence
            int queueMode = (i == 0) ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
            textToSpeech.speak(sentence, queueMode, null, utteranceId);
        }
    }
    
    /**
     * Split text into sentences
     */
    private String[] splitIntoSentences(String text) {
        // Split on sentence boundaries while keeping the delimiter
        return text.split("(?<=[.!?])\\s+");
    }
    
    // ==================== VOICE PROFILE METHODS ====================
    
    /**
     * Set voice profile
     */
    public void setVoiceProfile(VoiceProfile profile) {
        this.currentProfile = profile;
        applyVoiceProfile(profile);
        
        // Save preference
        preferences.edit()
            .putString(KEY_VOICE_PROFILE, profile.getId())
            .apply();
        
        Log.d(TAG, "Voice profile set to: " + profile.getDisplayName());
    }
    
    public VoiceProfile getVoiceProfile() {
        return currentProfile;
    }
    
    /**
     * Apply voice profile settings
     */
    private void applyVoiceProfile(VoiceProfile profile) {
        basePitch = profile.getDefaultPitch();
        baseSpeed = profile.getDefaultSpeed();
        
        // Reset current to base
        currentPitch = basePitch;
        currentSpeed = baseSpeed;
        
        if (textToSpeech != null) {
            textToSpeech.setPitch(currentPitch);
            textToSpeech.setSpeechRate(currentSpeed);
        }
    }
    
    // ==================== WHISPER MODE ====================
    
    /**
     * Enable/disable whisper mode for nighttime
     */
    public void enableWhisperMode(boolean enable) {
        this.whisperModeEnabled = enable;
        
        // Save preference
        preferences.edit()
            .putBoolean(KEY_WHISPER_MODE, enable)
            .apply();
        
        if (enable) {
            applyWhisperMode();
        } else {
            restoreNormalVoice();
        }
        
        Log.d(TAG, "Whisper mode " + (enable ? "enabled" : "disabled"));
    }
    
    public boolean isWhisperModeEnabled() {
        return whisperModeEnabled;
    }
    
    /**
     * Apply whisper mode voice settings
     */
    private void applyWhisperMode() {
        if (textToSpeech != null) {
            textToSpeech.setPitch(WHISPER_PITCH);
            textToSpeech.setSpeechRate(WHISPER_SPEED);
        }
    }
    
    /**
     * Restore normal voice
     */
    private void restoreNormalVoice() {
        applyVoiceProfile(currentProfile);
    }
    
    // ==================== LANGUAGE METHODS ====================
    
    /**
     * Speak in Hindi with optimized pronunciation
     */
    public void speakInHindi(String text) {
        speakInLanguage(text, Language.HINDI);
    }
    
    /**
     * Speak in English with optimization
     */
    public void speakInEnglish(String text) {
        speakInLanguage(text, Language.ENGLISH);
    }
    
    /**
     * Speak in Hinglish (Hindi-English mix)
     */
    public void speakInHinglish(String text) {
        speakInLanguage(text, Language.HINGLISH);
    }
    
    /**
     * Speak in specified language
     */
    private void speakInLanguage(String text, Language language) {
        if (!isInitialized || textToSpeech == null) {
            return;
        }
        
        Locale targetLocale;
        switch (language) {
            case HINDI:
                targetLocale = new Locale("hi", "IN");
                break;
            case ENGLISH:
                targetLocale = Locale.US;
                break;
            case HINGLISH:
            case ENGLISH_IN:
                targetLocale = new Locale("en", "IN");
                break;
            default:
                targetLocale = Locale.US;
        }
        
        int result = textToSpeech.setLanguage(targetLocale);
        if (result == TextToSpeech.LANG_MISSING_DATA) {
            Log.w(TAG, "Language data missing for: " + language);
            // Fallback to English
            textToSpeech.setLanguage(Locale.US);
        }
        
        // Speak
        speak(text);
    }
    
    /**
     * Set language for subsequent speech
     */
    public void setLanguage(Language language) {
        this.currentLanguage = language;
        
        if (textToSpeech != null) {
            Locale locale;
            switch (language) {
                case HINDI:
                    locale = new Locale("hi", "IN");
                    break;
                case ENGLISH:
                    locale = Locale.US;
                    break;
                case HINGLISH:
                case ENGLISH_IN:
                    locale = new Locale("en", "IN");
                    break;
                default:
                    locale = Locale.US;
            }
            textToSpeech.setLanguage(locale);
        }
    }
    
    public Language getLanguage() {
        return currentLanguage;
    }
    
    // ==================== SPEED & PITCH CONTROL ====================
    
    /**
     * Set speech rate (0.5f to 2.0f)
     */
    public void setSpeed(float speed) {
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 2.0f) speed = 2.0f;
        
        currentSpeed = speed;
        baseSpeed = speed;
        
        if (textToSpeech != null) {
            textToSpeech.setSpeechRate(currentSpeed);
        }
    }
    
    /**
     * Set pitch (0.5f to 2.0f)
     */
    public void setPitch(float pitch) {
        if (pitch < 0.5f) pitch = 0.5f;
        if (pitch > 2.0f) pitch = 2.0f;
        
        currentPitch = pitch;
        basePitch = pitch;
        
        if (textToSpeech != null) {
            textToSpeech.setPitch(currentPitch);
        }
    }
    
    public float getPitch() {
        return currentPitch;
    }
    
    public float getSpeed() {
        return currentSpeed;
    }
    
    // ==================== QUICK SPEAK METHODS ====================
    
    /**
     * Speak a greeting message
     */
    public void speakGreeting() {
        setEmotion(Emotion.HAPPY);
        speak("Namaste! Main Nova hoon, aapki personal assistant.");
    }
    
    /**
     * Speak a confirmation message
     */
    public void speakConfirmation() {
        setEmotion(Emotion.NEUTRAL);
        speak("Zaroor, main kar deti hoon!");
    }
    
    /**
     * Speak an error message
     */
    public void speakError() {
        setEmotion(Emotion.CONCERNED);
        speak("Maaf kijiye, thoda problem ho gayi.");
    }
    
    /**
     * Speak a success message
     */
    public void speakSuccess() {
        setEmotion(Emotion.HAPPY);
        speak("Ho gaya!");
    }
    
    /**
     * Speak a command acknowledgment
     */
    public void speakCommandAck() {
        setEmotion(Emotion.CALM);
        speak("Theek hai, samajh gayi.");
    }
    
    /**
     * Speak goodbye message
     */
    public void speakGoodbye() {
        setEmotion(Emotion.CALM);
        speak("Alvida! Phir milenge.");
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Generate unique utterance ID
     */
    private String generateUtteranceId() {
        return "nova_utt_" + System.currentTimeMillis() + "_" + utteranceCounter.incrementAndGet();
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
            textToSpeech = null;
        }
        isInitialized = false;
        callbackMap.clear();
        textMap.clear();
    }
    
    public boolean isSpeaking() {
        return textToSpeech != null && textToSpeech.isSpeaking();
    }
    
    // ==================== LISTENERS ====================
    
    public void addSpeakListener(SpeakListener listener) {
        if (!speakListeners.contains(listener)) {
            speakListeners.add(listener);
        }
    }
    
    public void removeSpeakListener(SpeakListener listener) {
        speakListeners.remove(listener);
    }
    
    private void notifySpeakStart(String text) {
        for (SpeakListener listener : new ArrayList<>(speakListeners)) {
            listener.onSpeakStart(text);
        }
    }
    
    private void notifySpeakComplete(String text) {
        for (SpeakListener listener : new ArrayList<>(speakListeners)) {
            listener.onSpeakComplete(text);
        }
    }
    
    private void notifySpeakError(String text, String error) {
        for (SpeakListener listener : new ArrayList<>(speakListeners)) {
            listener.onSpeakError(text, error);
        }
    }
    
    // ==================== PERSISTENCE ====================
    
    private void loadSavedSettings() {
        String profileId = preferences.getString(KEY_VOICE_PROFILE, VoiceProfile.NOVA.getId());
        currentProfile = VoiceProfile.fromId(profileId);
        
        whisperModeEnabled = preferences.getBoolean(KEY_WHISPER_MODE, false);
        currentEmotion = Emotion.fromId(preferences.getInt(KEY_CURRENT_EMOTION, 0));
    }
}
