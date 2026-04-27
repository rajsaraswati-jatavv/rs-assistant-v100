package com.rsassistant.ai;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * SmartResponses - Pre-built Response Templates for RS Assistant
 *
 * Provides context-aware, bilingual responses for various scenarios.
 * Supports Hindi-English mix (Hinglish) responses.
 *
 * Features:
 * - Pre-built response templates
 * - Hindi-English mix responses
 * - Context-aware responses
 * - Quick replies
 * - Time-based responses
 * - Emotional responses
 *
 * @author RS Assistant Team
 * @version 1.0
 */
public class SmartResponses {

    private static final String TAG = "SmartResponses";

    // Response categories
    public static final String CAT_GREETING = "greeting";
    public static final String CAT_GOODBYE = "goodbye";
    public static final String CAT_THANKS = "thanks";
    public static final String CAT_ERROR = "error";
    public static final String CAT_CONFIRM = "confirm";
    public static final String CAT_SUCCESS = "success";
    public static final String CAT_HELP = "help";
    public static final String CAT_UNKNOWN = "unknown";
    public static final String CAT_CLARIFICATION = "clarification";

    // Instance variables
    private final Context context;
    private final Random random;
    private final Map<String, List<String>> responseTemplates;
    private final Map<String, List<String>> hinglishTemplates;

    /**
     * Constructor
     */
    public SmartResponses(Context androidContext) {
        this.context = androidContext.getApplicationContext();
        this.random = new Random();
        this.responseTemplates = new HashMap<>();
        this.hinglishTemplates = new HashMap<>();

        initializeTemplates();
        Log.d(TAG, "SmartResponses initialized");
    }

    // ==================== TEMPLATE INITIALIZATION ====================

    private void initializeTemplates() {
        // Greeting responses
        List<String> greetings = new ArrayList<>();
        greetings.add("नमस्ते! 🙏 क्या मदद चाहिए?");
        greetings.add("Hello! 👋 कैसे मदद करूं?");
        greetings.add("Hey! 😊 क्या करना है आज?");
        greetings.add("Welcome! 🌟 Ready to help!");
        greetings.add("स्वागत है! 🙏 बताइए क्या काम है?");
        responseTemplates.put(CAT_GREETING, greetings);

        // Goodbye responses
        List<String> goodbyes = new ArrayList<>();
        goodbyes.add("Goodbye! 👋 फिर मिलेंगे!");
        goodbyes.add("Bye! 👋 जब चाहे बुला लेना!");
        goodbyes.add("अलविदा! 🙏 Take care!");
        goodbyes.add("See you soon! 👋 शुभकामनाएं!");
        goodbyes.add("Bye bye! 👋 Have a great day!");
        responseTemplates.put(CAT_GOODBYE, goodbyes);

        // Thank you responses
        List<String> thanks = new ArrayList<>();
        thanks.add("आपका स्वागत है! 😊");
        thanks.add("You're welcome! 🙏");
        thanks.add("कोई बात नहीं! 😊");
        thanks.add("My pleasure! ✨");
        thanks.add("खुशी हुई मदद करके! 🌟");
        thanks.add("Anytime! 😊");
        responseTemplates.put(CAT_THANKS, thanks);

        // Error responses
        List<String> errors = new ArrayList<>();
        errors.add("😅 Oops! कुछ गड़बड़ हो गई। दोबारा try करें!");
        errors.add("❌ Sorry! Technical issue। Please try again.");
        errors.add("🤔 Hmm, कुछ समझ नहीं आया। दोबारा बताएं?");
        errors.add("⚠️ Error! Let me try again...");
        errors.add("🔧 समस्या हो गई। एक minute रुकिए...");
        responseTemplates.put(CAT_ERROR, errors);

        // Confirmation responses
        List<String> confirms = new ArrayList<>();
        confirms.add("✅ Done! काम हो गया!");
        confirms.add("👍 Perfect! सब set!");
        confirms.add("✨ Completed! और कुछ?");
        confirms.add("🎯 Success! काम पूरा हुआ!");
        confirms.add("✔️ Done! बाकी कुछ चाहिए?");
        responseTemplates.put(CAT_CONFIRM, confirms);

        // Success responses
        List<String> success = new ArrayList<>();
        success.add("🎉 Great! सफलतापूर्वक हो गया!");
        success.add("✅ Perfect! सब ठीक है!");
        success.add("🌟 Excellent! काम पूरा!");
        success.add("💪 Super! और कुछ चाहिए?");
        success.add("🏆 Success! Great job!");
        responseTemplates.put(CAT_SUCCESS, success);

        // Help responses
        List<String> help = new ArrayList<>();
        help.add("📖 Help ke liye 'help' boliye!");
        help.add("❓ Kya help chahiye? Boliye!");
        help.add("🤖 Main yahan hoon madad ke liye!");
        help.add("💡 Need help? Just ask!");
        responseTemplates.put(CAT_HELP, help);

        // Unknown responses
        List<String> unknown = new ArrayList<>();
        unknown.add("🤔 Samajh nahi aaya। Thoda clear karke batao?");
        unknown.add("❓ Could you please rephrase that?");
        unknown.add("🤷‍♂️ Not sure I understand. Try again?");
        unknown.add("💭 Hmm, yeh nahi samjha। Alag se batao?");
        responseTemplates.put(CAT_UNKNOWN, unknown);

        // Clarification responses
        List<String> clarification = new ArrayList<>();
        clarification.add("🤔 Thoda detail mein batao please?");
        clarification.add("❓ Kya matlab? Clear karke batao?");
        clarification.add("💡 Can you explain more?");
        clarification.add("📝 Thoda aur info denge toh better help kar paungi!");
        responseTemplates.put(CAT_CLARIFICATION, clarification);

        // Hinglish templates
        initializeHinglishTemplates();
    }

    private void initializeHinglishTemplates() {
        // Common phrases in Hinglish
        List<String> hinglish = new ArrayList<>();
        hinglish.add("Haan bilkul! ✅");
        hinglish.add("Nahi, yeh nahi kar sakti 😅");
        hinglish.add("Theek hai! 👍");
        hinglish.add("Accha! 👌 Samajh gayi!");
        hinglish.add("Wait kar rahi hoon... ⏳");
        hinglish.add("Ho gaya! ✅");
        hinglish.add("Ek second... ⏱️");
        hinglish.add("Perfect! 🌟");
        hinglish.add("Nice! 😊");
        hinglish.add("Super! 🚀");
        hinglishTemplates.put("common", hinglish);

        // Command responses
        List<String> commands = new ArrayList<>();
        commands.add("Command samajh gayi! 🎯");
        commands.add("Okay! Kar rahi hoon... 💪");
        commands.add("Sure! Abhi karta hoon! ✨");
        commands.add("Processing... ⚡");
        commands.add("Working on it! 🔧");
        hinglishTemplates.put("commands", commands);

        // Emotional responses
        List<String> emotional = new ArrayList<>();
        emotional.add("Oh! 😮 Accha...");
        emotional.add("Wow! 🤩 Amazing!");
        emotional.add("Aww! 🥺 Samajh gayi!");
        emotional.add("Yay! 🎉 Great news!");
        emotional.add("Oh no! 😢 Kya hua?");
        hinglishTemplates.put("emotional", emotional);
    }

    // ==================== RESPONSE GETTERS ====================

    /**
     * Get a random response from a category
     */
    public String getResponse(String category) {
        List<String> responses = responseTemplates.get(category);
        if (responses == null || responses.isEmpty()) {
            return getResponse(CAT_UNKNOWN);
        }
        return responses.get(random.nextInt(responses.size()));
    }

    /**
     * Get greeting based on time
     */
    public String getTimeBasedGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            String[] morning = {
                "🌅 सुप्रभात! Good morning! आज का दिन शानदार हो!",
                "☀️ Good morning! नाश्ता किया?",
                "🌄 Morning! Fresh start! क्या प्लान है आज?"
            };
            return morning[random.nextInt(morning.length)];
        } else if (hour >= 12 && hour < 17) {
            String[] afternoon = {
                "🌞 Good afternoon! दोपहर हो गई!",
                "🌤️ Afternoon! खाना खाया?",
                "☀️ Hey! कैसा दिन जा रहा है?"
            };
            return afternoon[random.nextInt(afternoon.length)];
        } else if (hour >= 17 && hour < 21) {
            String[] evening = {
                "🌆 Good evening! शाम हो गई!",
                "🌅 Evening! Relax mode ON!",
                "🌇 Hey! दिन कैसा गुज़रा?"
            };
            return evening[random.nextInt(evening.length)];
        } else {
            String[] night = {
                "🌙 Good night! सोने का टाइम!",
                "🌟 Late night! कुछ जरूरी काम है?",
                "😴 इतनी रात को? सो जाओ ना!"
            };
            return night[random.nextInt(night.length)];
        }
    }

    /**
     * Get Hinglish response
     */
    public String getHinglishResponse(String type) {
        List<String> responses = hinglishTemplates.get(type);
        if (responses == null || responses.isEmpty()) {
            return getHinglishResponse("common");
        }
        return responses.get(random.nextInt(responses.size()));
    }

    // ==================== CONTEXT-AWARE RESPONSES ====================

    /**
     * Get response for command execution
     */
    public String getCommandResponse(String command, boolean success) {
        if (success) {
            String[] successResponses = {
                "✅ " + command + " - Ho gaya! 👍",
                "🎯 " + command + " - Done! ✨",
                "✔️ " + command + " - Complete! 🌟",
                "💪 " + command + " - Success! aur kuch?"
            };
            return successResponses[random.nextInt(successResponses.length)];
        } else {
            String[] failResponses = {
                "❌ " + command + " - Nahi ho paya. Dobara try karein!",
                "⚠️ " + command + " mein problem. Please try again.",
                "😅 " + command + " - kuch gadbad. Ek minute..."
            };
            return failResponses[random.nextInt(failResponses.length)];
        }
    }

    /**
     * Get response based on user sentiment
     */
    public String getSentimentResponse(float sentiment) {
        if (sentiment > 0.3f) {
            // Positive sentiment
            String[] positive = {
                "Great! 😊 Aap khush lag rahe ho!",
                "Awesome! 🎉 Good vibes!",
                "Nice! ✨ Positive energy!"
            };
            return positive[random.nextInt(positive.length)];
        } else if (sentiment < -0.3f) {
            // Negative sentiment
            String[] negative = {
                "🙏 Koi baat nahi. Main yahan hoon!",
                "💙 Theek ho jayega. Don't worry!",
                "🤗 Sab theek ho jayega. Cheer up!"
            };
            return negative[random.nextInt(negative.length)];
        } else {
            // Neutral sentiment
            return "Okay! 👍";
        }
    }

    /**
     * Get battery-based response
     */
    public String getBatteryResponse(int percentage) {
        if (percentage <= 5) {
            return "🚨 Battery critical! " + percentage + "% - Turant charger lagao!";
        } else if (percentage <= 15) {
            return "⚠️ Battery low! " + percentage + "% - Charger dhundho!";
        } else if (percentage <= 30) {
            return "🔋 Battery: " + percentage + "% - Jaldi charge karlo!";
        } else if (percentage <= 50) {
            return "🔋 Battery: " + percentage + "% - Theek hai!";
        } else if (percentage <= 80) {
            return "🔋 Battery: " + percentage + "% - Good! 👍";
        } else {
            return "🔋 Battery: " + percentage + "% - Excellent! 🌟";
        }
    }

    /**
     * Get network status response
     */
    public String getNetworkResponse(boolean connected) {
        if (connected) {
            String[] connectedResponses = {
                "📶 Online! Internet connected!",
                "✅ Network available! Ready to go!",
                "🌐 Connected! All systems go!"
            };
            return connectedResponses[random.nextInt(connectedResponses.length)];
        } else {
            String[] offlineResponses = {
                "📴 Offline mode! Limited features available.",
                "⚠️ No internet! Working offline...",
                "🔌 Disconnected! Offline mode active."
            };
            return offlineResponses[random.nextInt(offlineResponses.length)];
        }
    }

    // ==================== QUICK REPLIES ====================

    /**
     * Get quick reply suggestions
     */
    public List<String> getQuickReplies() {
        List<String> quickReplies = new ArrayList<>();
        quickReplies.add("🔊 Volume control");
        quickReplies.add("🔦 Flashlight");
        quickReplies.add("📷 Camera");
        quickReplies.add("📶 WiFi toggle");
        quickReplies.add("📞 Call someone");
        quickReplies.add("💬 Send message");
        quickReplies.add("⏰ Set reminder");
        quickReplies.add("🤖 Tell me a joke");
        quickReplies.add("📊 Battery status");
        quickReplies.add("❓ Help");
        return quickReplies;
    }

    /**
     * Get action suggestions based on time
     */
    public List<String> getTimeBasedSuggestions() {
        List<String> suggestions = new ArrayList<>();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 9) {
            suggestions.add("⏰ Alarm band karna hai?");
            suggestions.add("🎵 Morning playlist?");
            suggestions.add("📰 News update?");
        } else if (hour >= 9 && hour < 12) {
            suggestions.add("📞 Important calls?");
            suggestions.add("📅 Calendar check?");
            suggestions.add("🔇 Meeting mode?");
        } else if (hour >= 12 && hour < 14) {
            suggestions.add("🍕 Food order?");
            suggestions.add("📍 Restaurant nearby?");
            suggestions.add("🍽️ Lunch break reminder?");
        } else if (hour >= 14 && hour < 17) {
            suggestions.add("📧 Email check?");
            suggestions.add("📝 Notes update?");
            suggestions.add("☕ Break time?");
        } else if (hour >= 17 && hour < 20) {
            suggestions.add("🎵 Music time?");
            suggestions.add("🏃 Exercise reminder?");
            suggestions.add("📺 Entertainment?");
        } else if (hour >= 20 && hour < 23) {
            suggestions.add("🌙 Night mode?");
            suggestions.add("📖 Reading time?");
            suggestions.add("😴 Sleep timer?");
        } else {
            suggestions.add("⏰ Morning alarm?");
            suggestions.add("😴 Bedtime reminder?");
            suggestions.add("🔇 Do not disturb?");
        }

        return suggestions;
    }

    // ==================== VOICE COMMAND RESPONSES ====================

    /**
     * Get response for voice command recognition
     */
    public String getVoiceRecognitionResponse(boolean recognized, String command) {
        if (recognized) {
            return "👂 Sun liya! \"" + command + "\" - Sahi hai? 🎯";
        } else {
            String[] notRecognized = {
                "🤔 Samajh nahi aaya. Dobara boliye?",
                "❓ Kya kaha? Thora clearly boliye...",
                "🔇 Clear nahi suna. Ek aur try?"
            };
            return notRecognized[random.nextInt(notRecognized.length)];
        }
    }

    /**
     * Get TTS (Text-to-Speech) ready response
     */
    public String getTTSReadyResponse() {
        String[] responses = {
            "🗣️ Bolo! Sun rahi hoon!",
            "🎤 Ready to listen! Boliye!",
            "👂 Haan? Kya kehna hai?"
        };
        return responses[random.nextInt(responses.length)];
    }

    // ==================== SPECIAL OCCASION RESPONSES ====================

    /**
     * Check for special day and get appropriate response
     */
    public String getSpecialDayResponse() {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);

        // Check for special days
        if (month == Calendar.JANUARY && day == 1) {
            return "🎉 Happy New Year! नव वर्ष की शुभकामनाएं! 🎊";
        } else if (month == Calendar.JANUARY && day == 26) {
            return "🇮🇳 Happy Republic Day! गणतंत्र दिवस की शुभकामनाएं! 🙏";
        } else if (month == Calendar.AUGUST && day == 15) {
            return "🇮🇳 Happy Independence Day! स्वतंत्रता दिवस की शुभकामनाएं! 🙏";
        } else if (month == Calendar.OCTOBER && day == 2) {
            return "🕊️ Gandhi Jayanti! सत्यमेव जयते! 🙏";
        } else if (month == Calendar.DECEMBER && day == 25) {
            return "🎄 Merry Christmas! शुभ क्रिसमस! 🎅";
        } else if (month == Calendar.NOVEMBER) {
            // Diwali approximation (varies each year)
            if (day >= 1 && day <= 15) {
                return "🪔 Happy Diwali! शुभ दीपावली! ✨";
            }
        }

        return null; // No special day
    }

    // ==================== CONVERSATION FILLERS ====================

    /**
     * Get filler response for thinking
     */
    public String getThinkingFiller() {
        String[] fillers = {
            "🤔 Soch rahi hoon...",
            "⏳ Ek second...",
            "💭 Hmm...",
            "🔍 Check karti hoon...",
            "⚡ Processing..."
        };
        return fillers[random.nextInt(fillers.length)];
    }

    /**
     * Get acknowledgment response
     */
    public String getAcknowledgment() {
        String[] acks = {
            "👍 Okay!",
            "👍 Samajh gayi!",
            "✅ Got it!",
            "👌 Theek hai!"
        };
        return acks[random.nextInt(acks.length)];
    }

    /**
     * Get encouragement response
     */
    public String getEncouragement() {
        String[] encouragements = {
            "💪 You can do it!",
            "🌟 Aap kar sakte ho!",
            "🚀 Keep going!",
            "✨ Best of luck!"
        };
        return encouragements[random.nextInt(encouragements.length)];
    }

    // ==================== UTILITY ====================

    /**
     * Add custom response template
     */
    public void addCustomTemplate(String category, String response) {
        List<String> templates = responseTemplates.get(category);
        if (templates == null) {
            templates = new ArrayList<>();
            responseTemplates.put(category, templates);
        }
        templates.add(response);
    }

    /**
     * Get formatted help response
     */
    public String getHelpResponse() {
        return "🤖 RS Assistant - Help\n\n" +
               "📱 PHONE CONTROLS:\n" +
               "• 'Volume badhao/ghatao'\n" +
               "• 'WiFi ON/OFF karo'\n" +
               "• 'Bluetooth ON/OFF'\n" +
               "• 'Flashlight/Torch ON'\n" +
               "• 'Camera kholo'\n\n" +
               "📞 CALLS & SMS:\n" +
               "• '[Name] ko call karo'\n" +
               "• '[Name] ko message bhejo'\n\n" +
               "🧠 AI FEATURES:\n" +
               "• 'Joke sunao'\n" +
               "• 'Fact batao'\n" +
               "• 'Motivation quote'\n" +
               "• 'Time kya hua'\n" +
               "• 'Calculate 5 plus 3'\n\n" +
               "📝 TASKS & NOTES:\n" +
               "• 'Add task [task name]'\n" +
               "• 'Shopping list mein [item] add karo'\n" +
               "• 'Remind me to [action]'\n\n" +
               "💡 Tip: Natural language mein bolo - main samajh jaoonga! 😊";
    }
}
