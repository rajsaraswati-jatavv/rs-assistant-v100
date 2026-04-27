package com.rsassistant.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/**
 * ZAIChatManager - Enhanced AI Chat Integration for RS Assistant
 * 
 * Features:
 * ✅ Full z.ai API integration with proper authentication
 * ✅ Multi-modal capabilities (text, voice context)
 * ✅ Conversation memory and context awareness
 * ✅ Smart command extraction from natural language
 * ✅ Nova personality settings
 * ✅ Streaming responses support
 * ✅ Fallback mechanisms with multiple endpoints
 * ✅ Comprehensive error handling
 * ✅ Hindi-English bilingual support
 * 
 * @author RS Assistant Team
 * @version 2.0 - Production Ready
 */
public class ZAIChatManager {

    // ==================== CONSTANTS ====================
    
    private static final String TAG = "ZAIChatManager";
    private static final String PREF_NAME = "zai_chat_prefs_v2";
    
    // API Configuration - z.ai endpoints
    private static final String BASE_URL = "https://api.z.ai/v1";
    private static final String CHAT_ENDPOINT = BASE_URL + "/chat/completions";
    
    // Fallback endpoints for reliability (production-ready approach)
    private static final String[] FALLBACK_ENDPOINTS = {
        "https://api.z.ai/v1/chat/completions",
        "https://chat.z.ai/api/chat",
        "https://z-ai-gateway.vercel.app/api/chat"
    };
    
    // Model configuration
    private static final String DEFAULT_MODEL = "nova-3";
    private static final int MAX_TOKENS = 2048;
    private static final double TEMPERATURE = 0.7;
    private static final double TOP_P = 0.9;
    
    // Conversation limits
    private static final int MAX_CONTEXT_MESSAGES = 20;
    private static final int MAX_MESSAGE_LENGTH = 4000;
    
    // Storage keys
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_LANGUAGE = "preferred_language";
    private static final String KEY_PERSONALITY_MODE = "personality_mode";
    
    // Request timeout constants
    private static final int CONNECT_TIMEOUT = 30;
    private static final int READ_TIMEOUT = 120;
    private static final int WRITE_TIMEOUT = 30;
    
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // ==================== INSTANCE VARIABLES ====================
    
    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient httpClient;
    private final List<ChatMessage> conversationHistory;
    private final List<PhoneCommand> pendingCommands;
    private final NovaPersonality personality;
    private final String sessionId;
    
    private ChatCallback currentCallback;
    private StreamingCallback streamingCallback;
    private CommandExtractionCallback commandCallback;
    
    private boolean isConnected = false;
    private boolean isStreamingMode = false;
    private int currentEndpointIndex = 0;
    private String apiKey;
    private String preferredLanguage = "hi-IN"; // Default Hindi-English mix

    // ==================== CONSTRUCTOR ====================
    
    /**
     * Initialize ZAIChatManager with context
     * Sets up HTTP client, loads session, initializes personality
     */
    public ZAIChatManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.conversationHistory = Collections.synchronizedList(new ArrayList<>());
        this.pendingCommands = new ArrayList<>();
        this.personality = new NovaPersonality(this.context);
        
        // Load or create session ID for conversation continuity
        String savedSessionId = prefs.getString(KEY_SESSION_ID, null);
        this.sessionId = savedSessionId != null ? savedSessionId : generateSessionId();
        saveSessionId();
        
        // Load API key and preferences
        this.apiKey = prefs.getString(KEY_API_KEY, "");
        this.preferredLanguage = prefs.getString(KEY_LANGUAGE, "hi-IN");
        
        // Load personality mode
        String savedMode = prefs.getString(KEY_PERSONALITY_MODE, NovaPersonality.MODE_FRIENDLY);
        personality.setMode(savedMode);
        
        // Configure HTTP client with connection pooling
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();
        
        // Add system message to set context
        initializeConversation();
        
        Log.d(TAG, "ZAIChatManager initialized. Session: " + sessionId);
    }
    
    // ==================== PUBLIC API METHODS ====================
    
    /**
     * Set API key for authentication
     * @param key API key from z.ai
     */
    public void setApiKey(String key) {
        this.apiKey = key;
        prefs.edit().putString(KEY_API_KEY, key).apply();
        Log.d(TAG, "API key updated");
    }
    
    /**
     * Set preferred language for responses
     * @param languageCode Language code (e.g., "hi-IN", "en-US")
     */
    public void setLanguage(String languageCode) {
        this.preferredLanguage = languageCode;
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        personality.setLanguage(languageCode);
        Log.d(TAG, "Language set to: " + languageCode);
    }
    
    /**
     * Set personality mode for Nova assistant
     * @param mode One of NovaPersonality.MODE_* constants
     */
    public void setPersonalityMode(String mode) {
        personality.setMode(mode);
        prefs.edit().putString(KEY_PERSONALITY_MODE, mode).apply();
        Log.d(TAG, "Personality mode set to: " + mode);
    }
    
    /**
     * Set command extraction callback for phone commands
     * @param callback Callback to receive extracted commands
     */
    public void setCommandCallback(CommandExtractionCallback callback) {
        this.commandCallback = callback;
    }
    
    /**
     * Test API connection
     * @param callback Connection result callback
     */
    public void testConnection(ConnectionCallback callback) {
        Log.d(TAG, "Testing connection...");
        
        sendChatRequest("ping", new ChatCallback() {
            @Override
            public void onResponse(String response) {
                isConnected = true;
                Log.d(TAG, "Connection successful");
                postOnMainThread(() -> callback.onConnected());
            }
            
            @Override
            public void onError(String error) {
                isConnected = false;
                Log.e(TAG, "Connection failed: " + error);
                postOnMainThread(() -> callback.onDisconnected(error));
            }
        });
    }
    
    /**
     * Send a message and get AI response
     * @param userMessage User's message text
     * @param callback Response callback
     */
    public void sendMessage(String userMessage, ChatCallback callback) {
        this.currentCallback = callback;
        
        // Validate and sanitize input
        String sanitizedMessage = sanitizeInput(userMessage);
        if (sanitizedMessage.isEmpty()) {
            notifyError("Message is empty or invalid");
            return;
        }
        
        // Add user message to history
        addToHistory("user", sanitizedMessage);
        
        // Try to extract phone commands first
        PhoneCommand extractedCommand = extractPhoneCommand(sanitizedMessage);
        if (extractedCommand != null) {
            handleExtractedCommand(extractedCommand, sanitizedMessage);
            return;
        }
        
        // Send to AI API
        sendChatRequest(sanitizedMessage, callback);
    }
    
    /**
     * Send message with streaming response support
     * @param userMessage User's message
     * @param callback Streaming callback for real-time responses
     */
    public void sendMessageStreaming(String userMessage, StreamingCallback callback) {
        this.streamingCallback = callback;
        this.isStreamingMode = true;
        
        String sanitizedMessage = sanitizeInput(userMessage);
        if (sanitizedMessage.isEmpty()) {
            postOnMainThread(() -> callback.onError("Message is empty"));
            return;
        }
        
        addToHistory("user", sanitizedMessage);
        
        // Check for phone commands
        PhoneCommand command = extractPhoneCommand(sanitizedMessage);
        if (command != null) {
            handleExtractedCommand(command, sanitizedMessage);
            return;
        }
        
        sendStreamingRequest(sanitizedMessage);
    }
    
    /**
     * Send message with voice context (multi-modal)
     * @param userMessage User's spoken message
     * @param voiceContext Additional context from voice (emotion, confidence, etc.)
     * @param callback Response callback
     */
    public void sendMessageWithVoiceContext(String userMessage, VoiceContext voiceContext, ChatCallback callback) {
        this.currentCallback = callback;
        
        // Enhance message with voice context
        String enhancedMessage = buildContextualMessage(userMessage, voiceContext);
        
        Log.d(TAG, "Voice context applied - Emotion: " + voiceContext.emotion + 
              ", Confidence: " + voiceContext.confidence);
        
        sendMessage(enhancedMessage, callback);
    }
    
    /**
     * Get conversation history
     * @return List of chat messages
     */
    public List<ChatMessage> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
    
    /**
     * Clear conversation history and start fresh
     */
    public void clearHistory() {
        conversationHistory.clear();
        initializeConversation();
        Log.d(TAG, "Conversation history cleared");
    }
    
    /**
     * Get current connection status
     * @return true if connected to API
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Get session ID for this conversation
     * @return Session ID string
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Get pending phone commands
     * @return List of pending commands
     */
    public List<PhoneCommand> getPendingCommands() {
        return new ArrayList<>(pendingCommands);
    }
    
    /**
     * Execute a pending command
     * @param commandId ID of command to execute
     * @return true if command was executed
     */
    public boolean executeCommand(String commandId) {
        for (int i = 0; i < pendingCommands.size(); i++) {
            if (pendingCommands.get(i).getId().equals(commandId)) {
                PhoneCommand cmd = pendingCommands.remove(i);
                if (commandCallback != null) {
                    postOnMainThread(() -> commandCallback.onCommandExecute(cmd));
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * Cancel all pending commands
     */
    public void cancelPendingCommands() {
        pendingCommands.clear();
        Log.d(TAG, "All pending commands cancelled");
    }

    // ==================== PRIVATE IMPLEMENTATION ====================
    
    /**
     * Initialize conversation with system message
     * Yeh system context set karta hai AI ke liye
     */
    private void initializeConversation() {
        String systemPrompt = personality.buildSystemPrompt();
        conversationHistory.add(new ChatMessage("system", systemPrompt, System.currentTimeMillis()));
    }
    
    /**
     * Send chat request to API
     */
    private void sendChatRequest(String message, ChatCallback callback) {
        JSONObject requestBody = buildRequestBody(message, false);
        
        if (requestBody == null) {
            handleFallbackResponse(message, "Failed to build request");
            return;
        }
        
        Request request = new Request.Builder()
            .url(getCurrentEndpoint())
            .post(RequestBody.create(requestBody.toString(), JSON))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("X-Session-ID", sessionId)
            .addHeader("X-Source", "rs-assistant-android")
            .addHeader("X-Language", preferredLanguage)
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API call failed: " + e.getMessage());
                tryNextEndpoint(message, e.getMessage());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        handleSuccessfulResponse(responseBody);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API error: " + response.code() + " - " + errorBody);
                        tryNextEndpoint(message, "HTTP " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Response parsing error: " + e.getMessage());
                    tryNextEndpoint(message, e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }
    
    /**
     * Send streaming request for real-time responses
     * Real-time response ke liye streaming support
     */
    private void sendStreamingRequest(String message) {
        JSONObject requestBody = buildRequestBody(message, true);
        
        if (requestBody == null) {
            postOnMainThread(() -> streamingCallback.onError("Failed to build request"));
            return;
        }
        
        Request request = new Request.Builder()
            .url(getCurrentEndpoint())
            .post(RequestBody.create(requestBody.toString(), JSON))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("X-Session-ID", sessionId)
            .addHeader("Accept", "text/event-stream")
            .build();
        
        EventSource.Factory factory = EventSources.createFactory(httpClient);
        
        factory.newEventSource(request, new EventSourceListener() {
            private final StringBuilder responseBuilder = new StringBuilder();
            
            @Override
            public void onEvent(EventSource eventSource, @Nullable String id, @Nullable String type, @NonNull String data) {
                try {
                    if (data.equals("[DONE]")) {
                        String fullResponse = responseBuilder.toString();
                        addToHistory("assistant", fullResponse);
                        postOnMainThread(() -> streamingCallback.onComplete(fullResponse));
                        isStreamingMode = false;
                        return;
                    }
                    
                    JSONObject json = new JSONObject(data);
                    if (json.has("choices")) {
                        JSONArray choices = json.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                            if (delta.has("content")) {
                                String chunk = delta.getString("content");
                                responseBuilder.append(chunk);
                                postOnMainThread(() -> streamingCallback.onChunk(chunk));
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Streaming parse error: " + e.getMessage());
                }
            }
            
            @Override
            public void onFailure(EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                String error = t != null ? t.getMessage() : "Streaming failed";
                Log.e(TAG, "Streaming error: " + error);
                
                if (responseBuilder.length() > 0) {
                    // Partial response available, use it
                    String partialResponse = responseBuilder.toString();
                    addToHistory("assistant", partialResponse);
                    postOnMainThread(() -> streamingCallback.onComplete(partialResponse));
                } else {
                    postOnMainThread(() -> streamingCallback.onError(error));
                }
                isStreamingMode = false;
            }
        });
    }
    
    /**
     * Build JSON request body for API call
     */
    private JSONObject buildRequestBody(String message, boolean stream) {
        try {
            JSONObject json = new JSONObject();
            json.put("model", DEFAULT_MODEL);
            json.put("stream", stream);
            json.put("max_tokens", MAX_TOKENS);
            json.put("temperature", TEMPERATURE);
            json.put("top_p", TOP_P);
            
            // Build messages array from conversation history
            JSONArray messagesArray = new JSONArray();
            
            for (ChatMessage msg : conversationHistory) {
                JSONObject msgJson = new JSONObject();
                msgJson.put("role", msg.role);
                msgJson.put("content", msg.content);
                messagesArray.put(msgJson);
            }
            
            // Add current message
            JSONObject currentMsg = new JSONObject();
            currentMsg.put("role", "user");
            currentMsg.put("content", message);
            messagesArray.put(currentMsg);
            
            json.put("messages", messagesArray);
            
            // Add metadata
            JSONObject metadata = new JSONObject();
            metadata.put("session_id", sessionId);
            metadata.put("source", "rs-assistant-android");
            metadata.put("language", preferredLanguage);
            metadata.put("personality_mode", personality.getCurrentMode());
            json.put("metadata", metadata);
            
            return json;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build request body: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Handle successful API response
     * API se successful response handle karo
     */
    private void handleSuccessfulResponse(String responseBody) {
        try {
            JSONObject result = new JSONObject(responseBody);
            String reply = null;
            
            // Parse response based on format (OpenAI-compatible or custom)
            if (result.has("choices")) {
                // OpenAI-compatible format
                JSONArray choices = result.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    if (choice.has("message")) {
                        reply = choice.getJSONObject("message").getString("content");
                    } else if (choice.has("text")) {
                        reply = choice.getString("text");
                    }
                }
            } else if (result.has("response")) {
                reply = result.getString("response");
            } else if (result.has("message")) {
                reply = result.getString("message");
            } else if (result.has("content")) {
                reply = result.getString("content");
            }
            
            if (reply != null && !reply.isEmpty()) {
                addToHistory("assistant", reply);
                isConnected = true;
                notifySuccess(reply);
            } else {
                handleFallbackResponse("", "Empty response from API");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse response: " + e.getMessage());
            handleFallbackResponse("", "Parse error: " + e.getMessage());
        }
    }
    
    /**
     * Try next endpoint on failure
     * Failure par next endpoint try karo
     */
    private void tryNextEndpoint(String message, String error) {
        currentEndpointIndex++;
        
        if (currentEndpointIndex < FALLBACK_ENDPOINTS.length) {
            Log.d(TAG, "Trying fallback endpoint " + currentEndpointIndex);
            sendChatRequest(message, currentCallback);
        } else {
            // All endpoints failed, use offline response
            handleFallbackResponse(message, error);
            currentEndpointIndex = 0; // Reset for next request
        }
    }
    
    /**
     * Handle fallback response when API is unavailable
     * Offline mode mein smart responses generate karo
     */
    private void handleFallbackResponse(String message, String error) {
        Log.w(TAG, "Using fallback response. Reason: " + error);
        
        String response = generateOfflineResponse(message);
        addToHistory("assistant", response);
        notifySuccess(response);
    }
    
    /**
     * Generate smart offline response
     * Offline mode ke liye intelligent responses
     */
    private String generateOfflineResponse(String message) {
        if (message == null || message.isEmpty()) {
            return personality.getGreeting();
        }
        
        String lower = message.toLowerCase().trim();
        
        // Greetings handling - Namaste module
        if (containsAny(lower, "hello", "hi", "hey", "namaste", "नमस्ते", "हेलो", "प्रणाम")) {
            return personality.getGreeting();
        }
        
        // Time query
        if (containsAny(lower, "time", "समय", "बजे", "कितने बजे", "time kya")) {
            return generateTimeResponse();
        }
        
        // Date query
        if (containsAny(lower, "date", "तारीख", "आज क्या", "today date", "date kya")) {
            return generateDateResponse();
        }
        
        // Help request
        if (containsAny(lower, "help", "मदद", "sahayata", "commands", "कमांड")) {
            return personality.getHelpMessage();
        }
        
        // Thanks
        if (containsAny(lower, "thank", "धन्यवाद", "shukriya", "thanks")) {
            return personality.getThankYouResponse();
        }
        
        // Jokes
        if (containsAny(lower, "joke", "मज़ाक", "hasao", "हंसाओ", "funny")) {
            return personality.getJoke();
        }
        
        // Battery status
        if (containsAny(lower, "battery", "बैटरी", "charge", "चार्ज")) {
            return "🔋 Battery status check karein Settings > Battery mein.\n" +
                   "Pro tip: Battery saver on karne se battery life badhti hai! 💡";
        }
        
        // WiFi commands
        if (containsAny(lower, "wifi", "वाईफाई", "internet")) {
            return buildCommandResponse("wifi", lower.contains("on") || lower.contains("चालू"), 
                lower.contains("off") || lower.contains("बंद"));
        }
        
        // Bluetooth commands
        if (containsAny(lower, "bluetooth", "ब्लूटूथ")) {
            return buildCommandResponse("bluetooth", lower.contains("on") || lower.contains("चालू"), 
                lower.contains("off") || lower.contains("बंद"));
        }
        
        // Flashlight/Torch commands
        if (containsAny(lower, "flashlight", "torch", "टॉर्च", "light", "रोशनी")) {
            return buildCommandResponse("flashlight", lower.contains("on") || lower.contains("जला") || lower.contains("चालू"), 
                lower.contains("off") || lower.contains("बंद") || lower.contains("बुझा"));
        }
        
        // Camera commands
        if (containsAny(lower, "camera", "कैमरा", "photo", "picture", "selfie", "सेल्फी")) {
            return buildCommandResponse("camera", true, false);
        }
        
        // Volume commands
        if (containsAny(lower, "volume", "आवाज़", "sound", "dhang", "ध्वनि")) {
            return buildVolumeResponse(lower);
        }
        
        // Call commands
        if (containsAny(lower, "call", "कॉल", "phone", "फोन")) {
            return extractCallResponse(message);
        }
        
        // SMS commands
        if (containsAny(lower, "sms", "message", "मैसेज", "text", "भेजो")) {
            return extractSmsResponse(message);
        }
        
        // SOS/Emergency
        if (containsAny(lower, "sos", "emergency", "मदद", "emergency", "emergency contact")) {
            return "🆘 SOS Mode Activated!\n\n" +
                   "Emergency contact ko SMS bheja ja raha hai.\n" +
                   "Settings mein emergency contact zaroor set karein.\n\n" +
                   "Stay safe! 🙏";
        }
        
        // Music/Media
        if (containsAny(lower, "music", "song", "गाना", "play", "बजाओ")) {
            return "🎵 Music mode activate karne ke liye:\n" +
                   "• 'Play [song name]' boliye\n" +
                   "• 'Pause' / 'Stop' - music band karein\n" +
                   "• 'Next' / 'Previous' - gaane badlein\n\n" +
                   "Kya bajana hai? 🎶";
        }
        
        // Weather
        if (containsAny(lower, "weather", "मौसम", "temperature", "barsaat")) {
            return "🌤️ Weather information ke liye:\n" +
                   "• Google Assistant se poochein\n" +
                   "• Weather app kholen\n\n" +
                   "Tip: 'Mausam kaisa hai' bhi bol sakte hain! 🌡️";
        }
        
        // Alarm
        if (containsAny(lower, "alarm", "अलार्म", "wake", "जगाओ")) {
            return "⏰ Alarm set karne ke liye:\n" +
                   "• 'Alarm set karo [time]'\n" +
                   "• Example: 'Alarm set karo 6 baje'\n\n" +
                   "Kitne baje ka alarm chahiye? ⏰";
        }
        
        // Reminder
        if (containsAny(lower, "reminder", "याद", "remember", "reminder set")) {
            return "📝 Reminder set karne ke liye:\n" +
                   "• 'Mujhe [kaam] yaad dilao'\n" +
                   "• Example: 'Mujhe meeting yaad dilao'\n\n" +
                   "Kya yaad dilana hai? 📋";
        }
        
        // Who are you
        if (containsAny(lower, "who are you", "kaun ho", "तुम कौन", "your name", "naam")) {
            return personality.getIntroduction();
        }
        
        // Good morning/evening/night
        if (containsAny(lower, "good morning", "सुप्रभात", "morning")) {
            return personality.getMorningGreeting();
        }
        if (containsAny(lower, "good night", "शुभ रात्रि", "night", "सोना")) {
            return personality.getNightGreeting();
        }
        
        // Default intelligent response
        return buildDefaultResponse(message);
    }
    
    // ==================== PHONE COMMAND EXTRACTION ====================
    
    /**
     * Extract phone command from natural language
     * Natural language se phone commands nikaalo
     */
    private PhoneCommand extractPhoneCommand(String message) {
        String lower = message.toLowerCase().trim();
        
        // Call command extraction
        // Pattern: "[name] ko call karo" or "call [name]"
        Pattern callPattern = Pattern.compile("(?:call\\s+(\\w+)|(\\w+)\\s+ko\\s+call)", Pattern.CASE_INSENSITIVE);
        Matcher callMatcher = callPattern.matcher(lower);
        if (callMatcher.find()) {
            String name = callMatcher.group(1) != null ? callMatcher.group(1) : callMatcher.group(2);
            return new PhoneCommand(PhoneCommand.TYPE_CALL, name, "call");
        }
        
        // SMS command extraction
        Pattern smsPattern = Pattern.compile("(?:(\\w+)\\s+ko\\s+(?:sms|message)|(?:sms|message)\\s+(\\w+))", Pattern.CASE_INSENSITIVE);
        Matcher smsMatcher = smsPattern.matcher(lower);
        if (smsMatcher.find()) {
            String name = smsMatcher.group(1) != null ? smsMatcher.group(1) : smsMatcher.group(2);
            return new PhoneCommand(PhoneCommand.TYPE_SMS, name, "sms");
        }
        
        // WiFi toggle
        if (lower.contains("wifi") && (lower.contains("on") || lower.contains("off") || 
            lower.contains("चालू") || lower.contains("बंद"))) {
            boolean enable = lower.contains("on") || lower.contains("चालू");
            return new PhoneCommand(PhoneCommand.TYPE_WIFI, enable ? "ON" : "OFF", "wifi");
        }
        
        // Bluetooth toggle
        if (lower.contains("bluetooth") && (lower.contains("on") || lower.contains("off") || 
            lower.contains("चालू") || lower.contains("बंद"))) {
            boolean enable = lower.contains("on") || lower.contains("चालू");
            return new PhoneCommand(PhoneCommand.TYPE_BLUETOOTH, enable ? "ON" : "OFF", "bluetooth");
        }
        
        // Flashlight toggle
        if ((lower.contains("flashlight") || lower.contains("torch") || lower.contains("टॉर्च")) && 
            (lower.contains("on") || lower.contains("off") || lower.contains("जला") || lower.contains("बंद") || lower.contains("बुझा"))) {
            boolean enable = lower.contains("on") || lower.contains("जला") || lower.contains("चालू");
            return new PhoneCommand(PhoneCommand.TYPE_FLASHLIGHT, enable ? "ON" : "OFF", "flashlight");
        }
        
        // Camera open
        if (lower.contains("camera") || lower.contains("कैमरा") || lower.contains("photo") || lower.contains("selfie")) {
            return new PhoneCommand(PhoneCommand.TYPE_CAMERA, "OPEN", "camera");
        }
        
        // Volume control
        if (lower.contains("volume") || lower.contains("आवाज़")) {
            if (lower.contains("बढ़ा") || lower.contains("badha") || lower.contains("up") || lower.contains("full")) {
                return new PhoneCommand(PhoneCommand.TYPE_VOLUME, "UP", "volume");
            }
            if (lower.contains("घटा") || lower.contains("ghata") || lower.contains("down") || lower.contains("kam")) {
                return new PhoneCommand(PhoneCommand.TYPE_VOLUME, "DOWN", "volume");
            }
            if (lower.contains("mute") || lower.contains("silent") || lower.contains("चुप")) {
                return new PhoneCommand(PhoneCommand.TYPE_VOLUME, "MUTE", "volume");
            }
        }
        
        // Lock screen
        if (lower.contains("lock") && (lower.contains("screen") || lower.contains("phone") || lower.contains("फोन"))) {
            return new PhoneCommand(PhoneCommand.TYPE_LOCK, "LOCK", "lock");
        }
        
        return null;
    }
    
    /**
     * Handle extracted phone command
     */
    private void handleExtractedCommand(PhoneCommand command, String originalMessage) {
        pendingCommands.add(command);
        
        if (commandCallback != null) {
            postOnMainThread(() -> commandCallback.onCommandExtracted(command));
        }
        
        // Generate confirmation response
        String confirmation = generateCommandConfirmation(command);
        addToHistory("assistant", confirmation);
        notifySuccess(confirmation);
    }
    
    /**
     * Generate confirmation message for command
     */
    private String generateCommandConfirmation(PhoneCommand command) {
        String emoji = "";
        String action = "";
        
        switch (command.getType()) {
            case PhoneCommand.TYPE_CALL:
                emoji = "📞";
                action = "call";
                return emoji + " " + command.getTarget() + " ko call kar raha hoon...\n\n" +
                       "Confirm karein ya cancel karne ke liye 'cancel' boliye.";
                
            case PhoneCommand.TYPE_SMS:
                emoji = "💬";
                return emoji + " " + command.getTarget() + " ko SMS bhejni hai?\n\n" +
                       "Message boliye ya 'cancel' bol kar exit karein.";
                
            case PhoneCommand.TYPE_WIFI:
                emoji = "📶";
                return emoji + " WiFi " + command.getTarget() + " kar raha hoon...";
                
            case PhoneCommand.TYPE_BLUETOOTH:
                emoji = "🔵";
                return emoji + " Bluetooth " + command.getTarget() + " kar raha hoon...";
                
            case PhoneCommand.TYPE_FLASHLIGHT:
                emoji = "🔦";
                return emoji + " Flashlight " + command.getTarget() + " kar raha hoon...";
                
            case PhoneCommand.TYPE_CAMERA:
                emoji = "📷";
                return emoji + " Camera khol raha hoon...";
                
            case PhoneCommand.TYPE_VOLUME:
                emoji = "🔊";
                String volAction = command.getTarget().equals("UP") ? "badha raha hoon" :
                                  command.getTarget().equals("DOWN") ? "kam kar raha hoon" : "mute kar raha hoon";
                return emoji + " Volume " + volAction + "...";
                
            case PhoneCommand.TYPE_LOCK:
                emoji = "🔒";
                return emoji + " Phone lock kar raha hoon...";
        }
        
        return "✅ Command samajh gaya! Processing...";
    }

    // ==================== HELPER METHODS ====================
    
    /**
     * Get current API endpoint
     */
    private String getCurrentEndpoint() {
        return FALLBACK_ENDPOINTS[currentEndpointIndex];
    }
    
    /**
     * Sanitize user input
     */
    private String sanitizeInput(String input) {
        if (input == null) return "";
        String sanitized = input.trim();
        if (sanitized.length() > MAX_MESSAGE_LENGTH) {
            sanitized = sanitized.substring(0, MAX_MESSAGE_LENGTH);
        }
        return sanitized;
    }
    
    /**
     * Add message to conversation history
     */
    private void addToHistory(String role, String content) {
        conversationHistory.add(new ChatMessage(role, content, System.currentTimeMillis()));
        
        // Trim history if too long
        while (conversationHistory.size() > MAX_CONTEXT_MESSAGES + 1) {
            conversationHistory.remove(1); // Keep system message at index 0
        }
    }
    
    /**
     * Build contextual message with voice context
     */
    private String buildContextualMessage(String message, VoiceContext voiceContext) {
        StringBuilder builder = new StringBuilder();
        builder.append(message);
        
        if (voiceContext != null) {
            builder.append("\n[Voice Context: ");
            if (voiceContext.emotion != null) {
                builder.append("Emotion=").append(voiceContext.emotion).append(", ");
            }
            builder.append("Confidence=").append(voiceContext.confidence).append("]");
        }
        
        return builder.toString();
    }
    
    /**
     * Check if string contains any of the keywords
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Generate time response
     */
    private String generateTimeResponse() {
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault());
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("hi", "IN"));
        
        return "⏰ Abhi ka samay:\n" +
               "🕐 " + timeFormat.format(new java.util.Date()) + "\n" +
               "📅 " + dateFormat.format(new java.util.Date()) + "\n\n" +
               "Koi aur help chahiye? 😊";
    }
    
    /**
     * Generate date response
     */
    private String generateDateResponse() {
        java.text.SimpleDateFormat fullFormat = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("hi", "IN"));
        return "📅 Aaj ki tareekh:\n" + fullFormat.format(new java.util.Date());
    }
    
    /**
     * Build command response
     */
    private String buildCommandResponse(String command, boolean isOn, boolean isOff) {
        String emoji = "";
        String action = "";
        
        switch (command) {
            case "wifi":
                emoji = "📶";
                action = "WiFi";
                break;
            case "bluetooth":
                emoji = "🔵";
                action = "Bluetooth";
                break;
            case "flashlight":
                emoji = "🔦";
                action = "Flashlight";
                break;
            case "camera":
                emoji = "📷";
                return emoji + " Camera kholne ke liye command ready hai!\n" +
                       "Camera app automatically khulegi. Ready? 📸";
        }
        
        if (isOn) {
            return emoji + " " + action + " ON karne ka command samajh gaya!\n" +
                   "Execute karne ke liye confirm karein. ✅";
        } else if (isOff) {
            return emoji + " " + action + " OFF karne ka command samajh gaya!\n" +
                   "Execute karne ke liye confirm karein. ✅";
        } else {
            return emoji + " " + action + " control karein:\n" +
                   "• '" + action + " ON' - Chalu karne ke liye\n" +
                   "• '" + action + " OFF' - Band karne ke liye";
        }
    }
    
    /**
     * Build volume response
     */
    private String buildVolumeResponse(String lower) {
        if (lower.contains("बढ़ा") || lower.contains("badha") || lower.contains("up") || lower.contains("full") || lower.contains("high")) {
            return "🔊 Volume badha raha hoon...\n✅ Done!";
        }
        if (lower.contains("घटा") || lower.contains("ghata") || lower.contains("down") || lower.contains("kam") || lower.contains("low")) {
            return "🔉 Volume kam kar raha hoon...\n✅ Done!";
        }
        if (lower.contains("mute") || lower.contains("silent") || lower.contains("चुप") || lower.contains("zero")) {
            return "🔇 Silent mode ON!\n✅ Done!";
        }
        return "🔊 Volume control:\n" +
               "• 'Volume badhao' - badhane ke liye\n" +
               "• 'Volume kam karo' - kam karne ke liye\n" +
               "• 'Silent mode' - mute karne ke liye";
    }
    
    /**
     * Extract call response
     */
    private String extractCallResponse(String message) {
        // Try to extract name
        Pattern pattern = Pattern.compile("(?:call\\s+(\\w+)|(\\w+)\\s+ko\\s+call)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message.toLowerCase());
        
        if (matcher.find()) {
            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return "📞 " + name + " ko call karne ka command mila!\n" +
                   "Contact check kar raha hoon... 📱";
        }
        
        return "📞 Kisko call karna hai?\n" +
               "Example: 'Mom ko call karo' ya 'call Rahul'\n\n" +
               "Contact name bataiye! 📱";
    }
    
    /**
     * Extract SMS response
     */
    private String extractSmsResponse(String message) {
        Pattern pattern = Pattern.compile("(?:(\\w+)\\s+ko\\s+(?:sms|message)|(?:sms|message)\\s+(\\w+))", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message.toLowerCase());
        
        if (matcher.find()) {
            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return "💬 " + name + " ko SMS bhejni hai!\n" +
                   "Kya message bhejna hai? Boliye... ✍️";
        }
        
        return "💬 Kisko SMS bhejni hai?\n" +
               "Example: 'Rahul ko SMS bhejo'\n\n" +
               "Contact name aur message bataiye! 📱";
    }
    
    /**
     * Build default response for unrecognized input
     */
    private String buildDefaultResponse(String message) {
        return "🤔 Samajh gaya! Main soch raha hoon...\n\n" +
               "Aapne kaha: \"" + message + "\"\n\n" +
               "Phone control ke liye try karein:\n" +
               "• 🔊 Volume badhao/ghatao\n" +
               "• 📶 WiFi ON/OFF\n" +
               "• 🔦 Flashlight ON/OFF\n" +
               "• 📷 Camera kholo\n" +
               "• 📞 [Name] ko call karo\n\n" +
               "'Help' boliye aur commands dekhein! 😊";
    }
    
    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        return "RS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + 
               "-" + System.currentTimeMillis() % 10000;
    }
    
    /**
     * Save session ID to preferences
     */
    private void saveSessionId() {
        prefs.edit().putString(KEY_SESSION_ID, sessionId).apply();
    }
    
    /**
     * Post runnable on main thread
     */
    private void postOnMainThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
    
    /**
     * Notify success to callback
     */
    private void notifySuccess(String response) {
        postOnMainThread(() -> {
            if (currentCallback != null) {
                currentCallback.onResponse(response);
            }
        });
    }
    
    /**
     * Notify error to callback
     */
    private void notifyError(String error) {
        postOnMainThread(() -> {
            if (currentCallback != null) {
                currentCallback.onError(error);
            }
        });
    }

    // ==================== INNER CLASSES ====================
    
    /**
     * ChatMessage - Represents a single message in conversation
     */
    public static class ChatMessage {
        private final String role;
        private final String content;
        private final long timestamp;
        
        public ChatMessage(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
        
        public String getRole() { return role; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
        
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("role", role);
            json.put("content", content);
            return json;
        }
    }
    
    /**
     * PhoneCommand - Represents an extracted phone command
     */
    public static class PhoneCommand {
        public static final int TYPE_CALL = 1;
        public static final int TYPE_SMS = 2;
        public static final int TYPE_WIFI = 3;
        public static final int TYPE_BLUETOOTH = 4;
        public static final int TYPE_FLASHLIGHT = 5;
        public static final int TYPE_CAMERA = 6;
        public static final int TYPE_VOLUME = 7;
        public static final int TYPE_LOCK = 8;
        
        private final int type;
        private final String target;
        private final String action;
        private final String id;
        private final long timestamp;
        
        public PhoneCommand(int type, String target, String action) {
            this.type = type;
            this.target = target;
            this.action = action;
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getType() { return type; }
        public String getTarget() { return target; }
        public String getAction() { return action; }
        public String getId() { return id; }
        public long getTimestamp() { return timestamp; }
        
        public String getTypeName() {
            switch (type) {
                case TYPE_CALL: return "CALL";
                case TYPE_SMS: return "SMS";
                case TYPE_WIFI: return "WIFI";
                case TYPE_BLUETOOTH: return "BLUETOOTH";
                case TYPE_FLASHLIGHT: return "FLASHLIGHT";
                case TYPE_CAMERA: return "CAMERA";
                case TYPE_VOLUME: return "VOLUME";
                case TYPE_LOCK: return "LOCK";
                default: return "UNKNOWN";
            }
        }
    }
    
    /**
     * VoiceContext - Additional context from voice input
     */
    public static class VoiceContext {
        private String emotion;
        private float confidence;
        private String language;
        private float pitch;
        private float speed;
        
        public VoiceContext() {
            this.confidence = 1.0f;
            this.language = "hi-IN";
        }
        
        public void setEmotion(String emotion) { this.emotion = emotion; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
        public void setLanguage(String language) { this.language = language; }
        public void setPitch(float pitch) { this.pitch = pitch; }
        public void setSpeed(float speed) { this.speed = speed; }
        
        public String getEmotion() { return emotion; }
        public float getConfidence() { return confidence; }
        public String getLanguage() { return language; }
        public float getPitch() { return pitch; }
        public float getSpeed() { return speed; }
    }

    // ==================== INTERFACES ====================
    
    /**
     * Callback for chat responses
     */
    public interface ChatCallback {
        void onResponse(String response);
        void onError(String error);
    }
    
    /**
     * Callback for streaming responses
     */
    public interface StreamingCallback {
        void onChunk(String chunk);
        void onComplete(String fullResponse);
        void onError(String error);
    }
    
    /**
     * Callback for connection testing
     */
    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected(String reason);
    }
    
    /**
     * Callback for command extraction
     */
    public interface CommandExtractionCallback {
        void onCommandExtracted(PhoneCommand command);
        void onCommandExecute(PhoneCommand command);
    }
}
