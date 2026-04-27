package com.rsassistant.ai;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ConversationManager - Multi-turn conversation support
 * 
 * Features:
 * - Context retention across messages
 * - Follow-up questions handling
 * - Clarification requests
 * - Conversation history management
 * 
 * @author RS Assistant Team
 * @version 2.0
 */
public class ConversationManager {

    private static final String TAG = "ConversationManager";
    private static final int MAX_HISTORY = 20;
    
    private final Context context;
    private final List<ConversationTurn> history;
    private String currentTopic;
    private String lastIntent;
    private String pendingAction;
    
    public ConversationManager(Context context) {
        this.context = context.getApplicationContext();
        this.history = new ArrayList<>();
        this.currentTopic = null;
        this.lastIntent = null;
        this.pendingAction = null;
    }
    
    /**
     * Process message with context awareness
     */
    public String processWithContext(String userMessage) {
        // Add to history
        ConversationTurn turn = new ConversationTurn(userMessage, System.currentTimeMillis());
        history.add(turn);
        
        // Trim history if needed
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
        
        // Check for follow-up
        if (isFollowUp(userMessage)) {
            return handleFollowUp(userMessage);
        }
        
        // Check for clarification
        if (needsClarification(userMessage)) {
            return requestClarification(userMessage);
        }
        
        // Check for continuation
        if (isContinuation(userMessage)) {
            return handleContinuation(userMessage);
        }
        
        // New conversation
        return processNewMessage(userMessage);
    }
    
    private boolean isFollowUp(String message) {
        String lower = message.toLowerCase();
        return lower.contains("aur") || lower.contains("bhi") || 
               lower.contains("another") || lower.contains("more") ||
               lower.contains("phir") || lower.contains("dusra") ||
               lower.equals("ha") || lower.equals("yes") ||
               lower.equals("ok") || lower.equals("theek hai");
    }
    
    private boolean needsClarification(String message) {
        String lower = message.toLowerCase();
        return lower.contains("kaun sa") || lower.contains("konsa") ||
               lower.contains("which one") || lower.contains("kiska") ||
               (lower.contains("usko") && lastIntent != null) ||
               (lower.contains("us") && lastIntent != null);
    }
    
    private boolean isContinuation(String message) {
        // Check if message references previous conversation
        if (history.size() < 2) return false;
        
        String lower = message.toLowerCase();
        return lower.contains("uske baare mein") || lower.contains("about that") ||
               lower.contains("uska") || lower.contains("uski") ||
               lower.contains("vo wala") || lower.contains("that one");
    }
    
    private String handleFollowUp(String message) {
        if (lastIntent == null) {
            return "Pehle bataiye, aap kya karna chahte hain? 😊";
        }
        
        // Handle based on last intent
        if (lastIntent.contains("joke")) {
            IntelligentAssistant ai = new IntelligentAssistant(context);
            return "Aur ek joke sunna hai? 😄\n\n" + ai.getJoke();
        }
        
        if (lastIntent.contains("fact")) {
            IntelligentAssistant ai = new IntelligentAssistant(context);
            return "Aur ek fact sunna hai? 🧠\n\n" + ai.getFact();
        }
        
        if (lastIntent.contains("quote")) {
            IntelligentAssistant ai = new IntelligentAssistant(context);
            return "Aur ek quote sunna hai? 💪\n\n" + ai.getQuote();
        }
        
        return "Haan, boliye! Main sun rahi hoon. 😊";
    }
    
    private String requestClarification(String message) {
        // Ask for clarification
        String lower = message.toLowerCase();
        
        if (lower.contains("kaun sa") || lower.contains("konsa")) {
            return "Kaun sa bataiye? Options:\n\n1. Pehla wala\n2. Dusra wala\n\nNumber boliye! 😊";
        }
        
        if (lower.contains("usko")) {
            return "Kis baare mein baat kar rahe hain? Name bataiye? 🤔";
        }
        
        return "Thoda detail mein bataiye? Main samajhna chahti hoon! 😊";
    }
    
    private String handleContinuation(String message) {
        // Continue previous conversation
        ConversationTurn lastTurn = history.size() > 1 ? 
            history.get(history.size() - 2) : null;
        
        if (lastTurn != null) {
            return "Aap " + lastTurn.message + " ke baare mein pooch rahe hain?\n\n" +
                   "Ha, main yaad rakhti hoon! Aur kya janana hai? 😊";
        }
        
        return "Main sun rahi hoon! Aage boliye? 😊";
    }
    
    private String processNewMessage(String message) {
        // Update topic and intent
        updateTopic(message);
        updateIntent(message);
        
        // Use IntelligentAssistant for response
        IntelligentAssistant ai = new IntelligentAssistant(context);
        return ai.processQuery(message);
    }
    
    private void updateTopic(String message) {
        String lower = message.toLowerCase();
        
        if (lower.contains("call") || lower.contains("message") || lower.contains("sms")) {
            currentTopic = "communication";
        } else if (lower.contains("music") || lower.contains("song") || lower.contains("play")) {
            currentTopic = "media";
        } else if (lower.contains("camera") || lower.contains("photo") || lower.contains("selfie")) {
            currentTopic = "camera";
        } else if (lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("setting")) {
            currentTopic = "settings";
        } else if (lower.contains("joke") || lower.contains("fact") || lower.contains("quote")) {
            currentTopic = "entertainment";
        } else {
            currentTopic = "general";
        }
    }
    
    private void updateIntent(String message) {
        String lower = message.toLowerCase();
        
        if (lower.contains("joke")) {
            lastIntent = "joke";
        } else if (lower.contains("fact")) {
            lastIntent = "fact";
        } else if (lower.contains("quote")) {
            lastIntent = "quote";
        } else if (lower.contains("call")) {
            lastIntent = "call";
        } else if (lower.contains("message") || lower.contains("sms")) {
            lastIntent = "message";
        } else if (lower.contains("play")) {
            lastIntent = "play_media";
        }
    }
    
    public void setPendingAction(String action) {
        this.pendingAction = action;
    }
    
    public String getPendingAction() {
        return pendingAction;
    }
    
    public void clearPendingAction() {
        this.pendingAction = null;
    }
    
    public String getCurrentTopic() {
        return currentTopic;
    }
    
    public String getLastIntent() {
        return lastIntent;
    }
    
    public List<ConversationTurn> getHistory() {
        return new ArrayList<>(history);
    }
    
    public void clearHistory() {
        history.clear();
        currentTopic = null;
        lastIntent = null;
        pendingAction = null;
    }
    
    /**
     * Inner class for conversation turns
     */
    public static class ConversationTurn {
        public final String message;
        public final long timestamp;
        public String response;
        
        public ConversationTurn(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
            this.response = null;
        }
    }
}
