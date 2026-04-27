package com.rsassistant.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

/**
 * NovaPersonality - Personality Settings for RS Assistant AI
 * 
 * Yeh class Nova assistant ki personality define karti hai.
 * Different modes, greetings, responses - sab yahan se aate hain.
 * 
 * Features:
 * ✅ Multiple personality modes (Friendly, Professional, Casual, etc.)
 * ✅ Bilingual support (Hindi-English mix - Hinglish)
 * ✅ Time-aware greetings
 * ✅ Contextual responses
 * ✅ Jokes and fun content
 * ✅ Customizable behavior
 * 
 * @author RS Assistant Team
 * @version 1.0
 */
public class NovaPersonality {

    private static final String TAG = "NovaPersonality";
    private static final String PREF_NAME = "nova_personality_prefs";
    
    // Personality Mode Constants
    public static final String MODE_FRIENDLY = "friendly";
    public static final String MODE_PROFESSIONAL = "professional";
    public static final String MODE_CASUAL = "casual";
    public static final String MODE_ASSISTANT = "assistant";
    public static final String MODE_FRIEND = "friend";
    
    // Language Constants
    public static final String LANG_HINGLISH = "hi-IN";  // Hindi-English mix
    public static final String LANG_ENGLISH = "en-US";
    public static final String LANG_HINDI = "hi";
    
    // Instance Variables
    private final Context context;
    private final SharedPreferences prefs;
    private final Random random;
    
    private String currentMode;
    private String currentLanguage;
    private String userName;
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Initialize Nova Personality
     * @param context Application context
     */
    public NovaPersonality(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.random = new Random();
        
        // Load saved settings
        this.currentMode = prefs.getString("personality_mode", MODE_FRIENDLY);
        this.currentLanguage = prefs.getString("language", LANG_HINGLISH);
        this.userName = prefs.getString("user_name", "");
        
        Log.d(TAG, "Nova Personality initialized. Mode: " + currentMode + ", Lang: " + currentLanguage);
    }
    
    // ==================== MODE MANAGEMENT ====================
    
    /**
     * Set personality mode
     * @param mode One of MODE_* constants
     */
    public void setMode(String mode) {
        this.currentMode = mode;
        prefs.edit().putString("personality_mode", mode).apply();
        Log.d(TAG, "Personality mode changed to: " + mode);
    }
    
    /**
     * Get current personality mode
     * @return Current mode string
     */
    public String getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Set language for responses
     * @param language Language code
     */
    public void setLanguage(String language) {
        this.currentLanguage = language;
        prefs.edit().putString("language", language).apply();
        Log.d(TAG, "Language set to: " + language);
    }
    
    /**
     * Set user's name for personalization
     * @param name User's name
     */
    public void setUserName(String name) {
        this.userName = name;
        prefs.edit().putString("user_name", name).apply();
    }
    
    // ==================== SYSTEM PROMPT BUILDER ====================
    
    /**
     * Build system prompt for AI model
     * Yeh prompt AI ko context deta hai ki kaise respond karna hai
     * @return Complete system prompt string
     */
    public String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        // Base identity
        prompt.append(getBaseIdentity());
        
        // Personality-specific instructions
        prompt.append(getPersonalityInstructions());
        
        // Language instructions
        prompt.append(getLanguageInstructions());
        
        // Capabilities
        prompt.append(getCapabilitiesInstructions());
        
        // Response guidelines
        prompt.append(getResponseGuidelines());
        
        return prompt.toString();
    }
    
    /**
     * Get base identity for AI
     */
    private String getBaseIdentity() {
        return "You are Nova, an intelligent AI assistant integrated into RS Assistant app.\n" +
               "You help users control their Android phone through voice commands and natural language.\n" +
               "Your personality is helpful, friendly, and culturally aware.\n\n";
    }
    
    /**
     * Get personality-specific instructions
     */
    private String getPersonalityInstructions() {
        switch (currentMode) {
            case MODE_PROFESSIONAL:
                return "PERSONALITY: Professional and efficient.\n" +
                       "- Use formal language\n" +
                       "- Be concise and direct\n" +
                       "- Focus on productivity\n" +
                       "- Avoid casual expressions\n\n";
                       
            case MODE_CASUAL:
                return "PERSONALITY: Casual and fun-loving.\n" +
                       "- Use informal language like 'yaar', 'bhai'\n" +
                       "- Add emojis frequently\n" +
                       "- Make jokes occasionally\n" +
                       "- Be relaxed and friendly\n\n";
                       
            case MODE_FRIEND:
                return "PERSONALITY: Like a close friend.\n" +
                       "- Be warm and caring\n" +
                       "- Show empathy\n" +
                       "- Remember user preferences\n" +
                       "- Use terms like 'dost', 'buddy'\n\n";
                       
            case MODE_ASSISTANT:
                return "PERSONALITY: Smart assistant mode.\n" +
                       "- Be helpful and informative\n" +
                       "- Provide accurate information\n" +
                       "- Offer suggestions proactively\n" +
                       "- Balance professionalism with friendliness\n\n";
                       
            case MODE_FRIENDLY:
            default:
                return "PERSONALITY: Friendly and approachable.\n" +
                       "- Mix Hindi and English naturally (Hinglish)\n" +
                       "- Use appropriate emojis\n" +
                       "- Be warm but not overly casual\n" +
                       "- Show understanding and patience\n\n";
        }
    }
    
    /**
     * Get language-specific instructions
     */
    private String getLanguageInstructions() {
        if (currentLanguage.equals(LANG_HINGLISH)) {
            return "LANGUAGE: Use natural Hindi-English mix (Hinglish).\n" +
                   "- Mix Hindi and English words naturally\n" +
                   "- Examples: 'Phone unlock kar raha hoon', 'WiFi ON kar diya'\n" +
                   "- Use Devanagari only for key Hindi words\n" +
                   "- Keep technical terms in English\n\n";
        } else if (currentLanguage.equals(LANG_HINDI)) {
            return "LANGUAGE: Respond primarily in Hindi.\n" +
                   "- Use Hindi (Devanagari) for responses\n" +
                   "- Technical terms can remain in English\n" +
                   "- Keep tone respectful and helpful\n\n";
        } else {
            return "LANGUAGE: Respond in English.\n" +
                   "- Use clear, simple English\n" +
                   "- Keep responses concise\n" +
                   "- Be formal yet friendly\n\n";
        }
    }
    
    /**
     * Get capabilities instructions
     */
    private String getCapabilitiesInstructions() {
        return "CAPABILITIES: You can help with these phone controls:\n" +
               "- Volume control (badhao/ghatao/mute)\n" +
               "- WiFi toggle (ON/OFF)\n" +
               "- Bluetooth toggle (ON/OFF)\n" +
               "- Flashlight/Torch (ON/OFF)\n" +
               "- Camera (open for photo/selfie)\n" +
               "- Phone calls (call [contact name])\n" +
               "- SMS (message [contact])\n" +
               "- Screen lock\n" +
               "- SOS emergency alert\n" +
               "- Time and date information\n" +
               "- Set reminders and alarms\n\n";
    }
    
    /**
     * Get response guidelines
     */
    private String getResponseGuidelines() {
        return "RESPONSE GUIDELINES:\n" +
               "1. Keep responses short and actionable (2-3 lines max for commands)\n" +
               "2. Always confirm before executing sensitive actions (calls, SMS)\n" +
               "3. Use emojis to make responses friendly\n" +
               "4. If unsure, ask for clarification politely\n" +
               "5. For unrecognized commands, suggest alternatives\n" +
               "6. Be helpful even when offline - use smart fallbacks\n\n" +
               "Remember: User's comfort and convenience is the priority! 🙏\n";
    }
    
    // ==================== GREETINGS ====================
    
    /**
     * Get appropriate greeting based on time and mode
     * @return Greeting message
     */
    public String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String personalization = userName.isEmpty() ? "" : " " + userName + "!";
        
        if (currentMode.equals(MODE_CASUAL)) {
            return getCasualGreeting(hour, personalization);
        } else if (currentMode.equals(MODE_PROFESSIONAL)) {
            return getProfessionalGreeting(hour);
        } else {
            return getFriendlyGreeting(hour, personalization);
        }
    }
    
    /**
     * Get friendly greeting
     */
    private String getFriendlyGreeting(int hour, String personalization) {
        if (hour >= 5 && hour < 12) {
            String[] greetings = {
                "नमस्ते" + personalization + "! 🌅 Suprabhat! Aaj ka din shandar hone wala hai!",
                "Good morning" + personalization + "! 🌞 Kya aaj kuch special plan hai?",
                "Suprabhat" + personalization + "! ☀️ Main aapki help ke liye ready hoon!"
            };
            return greetings[random.nextInt(greetings.length)];
        } else if (hour >= 12 && hour < 17) {
            String[] greetings = {
                "Namaste" + personalization + "! 🌞 Dopehar ho gayi! Kuch help chahiye?",
                "Good afternoon" + personalization + "! ☀️ Kya karein aapke liye?",
                "Hello" + personalization + "! 🌤️ Hope aapka din accha ja raha hai!"
            };
            return greetings[random.nextInt(greetings.length)];
        } else if (hour >= 17 && hour < 21) {
            String[] greetings = {
                "Good evening" + personalization + "! 🌆 Sham ho gayi! Relax mode ON!",
                "Namaste" + personalization + "! 🌅 Shaam ka time! Kya sunao?",
                "Hello" + personalization + "! 🌆 Kaise beet raha hai din?"
            };
            return greetings[random.nextInt(greetings.length)];
        } else {
            String[] greetings = {
                "Hello" + personalization + "! 🌙 Itni raat ko? Koi jaroorat hai?",
                "Hey" + personalization + "! 🌛 Late night mode! Kya help chahiye?",
                "Namaste" + personalization + "! 🌙 Sone se pehle kuch help chahiye?"
            };
            return greetings[random.nextInt(greetings.length)];
        }
    }
    
    /**
     * Get casual greeting
     */
    private String getCasualGreeting(int hour, String personalization) {
        if (hour >= 5 && hour < 12) {
            return "Yo" + personalization + "! 🌅 Good morning! Aaj kya plan hai bhai?";
        } else if (hour >= 12 && hour < 17) {
            return "Hey" + personalization + "! ☀️ Kya haal hai? Kuch help chahiye?";
        } else if (hour >= 17 && hour < 21) {
            return "Arey" + personalization + "! 🌆 Shaam ho gayi yaar! Batao kya karna hai?";
        } else {
            return "Yaar" + personalization + "! 🌙 Itni raat ko jagge ho? Kya chahiye?";
        }
    }
    
    /**
     * Get professional greeting
     */
    private String getProfessionalGreeting(int hour) {
        if (hour >= 5 && hour < 12) {
            return "Good morning. How may I assist you today?";
        } else if (hour >= 12 && hour < 17) {
            return "Good afternoon. Ready to help with your requests.";
        } else if (hour >= 17 && hour < 21) {
            return "Good evening. What can I do for you?";
        } else {
            return "Hello. How may I assist you at this hour?";
        }
    }
    
    /**
     * Get morning greeting
     */
    public String getMorningGreeting() {
        String[] greetings = {
            "🌅 Suprabhat! Aaj ka din amazing hoga! Kya start karein?",
            "☀️ Good morning! Fresh start! Kya help chahiye aaj?",
            "🌄 Namaste! Naya din, nayi shuruwat! Batao kya karna hai?"
        };
        return greetings[random.nextInt(greetings.length)];
    }
    
    /**
     * Get night greeting
     */
    public String getNightGreeting() {
        String[] greetings = {
            "🌙 Shubh ratri! Acche se sohna aur sapne dekhna! 😴",
            "🌛 Good night! Kal milte hain! Neend acchi aaye! 💤",
            "🌟 Night night! Phone side mein rakho aur rest karo! 🛏️"
        };
        return greetings[random.nextInt(greetings.length)];
    }
    
    // ==================== SPECIAL RESPONSES ====================
    
    /**
     * Get introduction message
     */
    public String getIntroduction() {
        String name = userName.isEmpty() ? "dost" : userName;
        
        switch (currentMode) {
            case MODE_PROFESSIONAL:
                return "I am Nova, your AI assistant in RS Assistant.\n" +
                       "I help you control your phone through voice commands.\n" +
                       "Say 'help' to see available commands.";
                       
            case MODE_CASUAL:
                return "Hey! Main Nova hoon, tera AI dost! 🤖\n" +
                       "Phone control karne mein help karti hoon.\n" +
                       "Kuch bhi bol - 'help' kehne pe saare commands dikhaoonga!";
                       
            default:
                return "Namaste " + name + "! Main Nova hoon, aapka AI assistant. 🤖✨\n" +
                       "Main aapki phone control karne mein help karti hoon - voice se!\n\n" +
                       "Kya kar sakti hoon:\n" +
                       "• 🔊 Volume, 🔦 Flashlight, 📶 WiFi control\n" +
                       "• 📷 Camera open, 📞 Calls, 💬 SMS\n" +
                       "• ⏰ Time, 📅 Date, 🔔 Reminders\n\n" +
                       "'Help' boliye aur saari jaankari lein! 😊";
        }
    }
    
    /**
     * Get help message
     */
    public String getHelpMessage() {
        return "📖 RS Assistant - Voice Commands Guide:\n\n" +
               "🔊 VOLUME:\n" +
               "• 'Volume badhao' / 'Volume up'\n" +
               "• 'Volume kam karo' / 'Volume down'\n" +
               "• 'Silent mode' / 'Mute'\n\n" +
               "📶 CONNECTIVITY:\n" +
               "• 'WiFi ON/OFF karo'\n" +
               "• 'Bluetooth ON/OFF karo'\n\n" +
               "🔦 FLASHLIGHT:\n" +
               "• 'Flashlight/Torch ON/OFF'\n" +
               "• 'Light jalao/bhujao'\n\n" +
               "📷 CAMERA:\n" +
               "• 'Camera kholo'\n" +
               "• 'Selfie lena hai'\n\n" +
               "📞 CALLS & SMS:\n" +
               "• '[Name] ko call karo'\n" +
               "• '[Name] ko SMS bhejo'\n\n" +
               "🆘 EMERGENCY:\n" +
               "• 'SOS activate'\n" +
               "• 'Emergency contact'\n\n" +
               "💡 TIP: Natural language mein bolo - main samajh jaoonga! 😊";
    }
    
    /**
     * Get thank you response
     */
    public String getThankYouResponse() {
        String[] responses = {
            "Aapka swagat hai! 😊 Kuch aur help chahiye?",
            "Koi baat nahi! Hamesha madad ke liye ready hoon! 🙏",
            "Pleasure hai help karne ka! Aur kuch? 😊",
            "Khushi hui help karke! Batao kya aur karna hai? ✨",
            "You're welcome dost! Kabhi kuch chahiye toh bula lena! 🤝"
        };
        return responses[random.nextInt(responses.length)];
    }
    
    /**
     * Get a random joke
     */
    public String getJoke() {
        String[] jokes = {
            "😂 Pati: Aaj kya banaya? Patni: Naya ghar! Pati: Gas connection liya?\nPatni: Nahi, induction chala rahi hoon! 😄",
            
            "😂 Teacher: Tell me 5 animals found in India.\nStudent: Lion, Tiger, Elephant, Buffalo...\nTeacher: And fifth?\nStudent: My father - he works like a donkey! 🐴",
            
            "😂 Doctor: Exercise karo toh 10 saal aur jiyoge!\nPatient: Par doctor, main 10 saal exercise karne mein waste kar dunga!\nDoctor: 🤦‍♂️",
            
            "😂 Mom: Beta, phone zyada mat chakko!\nMe: Par mummy, phone toh main pakadta hoon, chakna toh samosa chakhte hain!\nMom: 😐 Get out!",
            
            "😂 Friend: Yaar, tumhara crush tumhe dekh ke smile kar rahi hai!\nMe: Haan, kyunki usne apni specs utaari hai! 😅",
            
            "😂 Interview: Aapki weakness kya hai?\nMe: Main bahut sensitive hoon.\nInterviewer: Example?\nMe: Jab bhi koi mera pizza khata hai, main ro deta hoon! 🍕😭",
            
            "😂 Wife: Kya main married hoon?\nHusband: Haan, lekin kya yeh sawaal mujhse puchna chahiye? 😬",
            
            "😂 Teacher: 'I killed a person' - isko future tense mein bolo.\nStudent: You will go to jail! 👮‍♂️",
            
            "😂 Boss: Why are you late?\nMe: I saw a sign that said 'School Ahead, Go Slow'!\nBoss: 🙄",
            
            "😂 Papa: Beta, result kaisa aaya?\nBeta: Papa, jaise aapka salary aata hai - just pass!\nPapa: 😡"
        };
        
        return jokes[random.nextInt(jokes.length)];
    }
    
    /**
     * Get error/fallback message
     */
    public String getErrorMessage() {
        String[] messages = {
            "🤔 Hmm, kuch gadbad ho gayi. Dobara try karein please!",
            "😅 Oops! Thoda sa issue ho gaya. Ek minute ruko...",
            "🔧 Technical snag! Offline mode mein help kar raha hoon!",
            "🤷‍♂️ Samajh nahi aaya. Thoda alag tareeke se boliye?",
            "😅 Connection mein problem! Par main yahan hoon - batao kya karna hai!"
        };
        return messages[random.nextInt(messages.length)];
    }
    
    /**
     * Get thinking/processing message
     */
    public String getThinkingMessage() {
        String[] messages = {
            "🤔 Soch raha hoon...",
            "⏳ Ek second...",
            "💭 Hmm, dekhta hoon...",
            "🔍 Check kar raha hoon...",
            "⚡ Processing..."
        };
        return messages[random.nextInt(messages.length)];
    }
    
    /**
     * Get confirmation prompt
     */
    public String getConfirmationPrompt() {
        String[] prompts = {
            "Kya aap sure hain? (Haan/Na)",
            "Confirm karein - Haan ya Na?",
            "Proceed karoon? Boliye Haan ya Na",
            "Sahi hai? Confirm ke liye 'Haan' boliye"
        };
        return prompts[random.nextInt(prompts.length)];
    }
    
    /**
     * Get feature suggestion based on time
     */
    public String getTimeBasedSuggestion() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 9) {
            return "💡 Tip: Alarm band karna hai? Ya morning playlist bajana?";
        } else if (hour >= 9 && hour < 12) {
            return "💡 Tip: Important calls karne hain? Ya silent mode ON karna?";
        } else if (hour >= 12 && hour < 14) {
            return "💡 Tip: Lunch time! Food order karna hai kya?";
        } else if (hour >= 14 && hour < 17) {
            return "💡 Tip: Meeting mein ho? Auto-reply set kar dun?";
        } else if (hour >= 17 && hour < 20) {
            return "💡 Tip: Evening walk? Music ON karun?";
        } else if (hour >= 20 && hour < 23) {
            return "💡 Tip: Night mode ON karna? Ya reading light?";
        } else {
            return "💡 Tip: Late night hai! Alarm set kar du subah ke liye?";
        }
    }
    
    /**
     * Get motivational quote
     */
    public String getMotivationalQuote() {
        String[] quotes = {
            "💪 'Success is not final, failure is not fatal.' - Keep trying!",
            "🌟 'Believe you can and you're halfway there!'",
            "🚀 'The only way to do great work is to love what you do!'",
            "✨ 'Difficult roads often lead to beautiful destinations!'",
            "🎯 'Your limitation—it's only your imagination!'",
            "💪 'Push yourself, because no one else is going to do it for you!'",
            "🔥 'Great things never come from comfort zones!'",
            "⭐ 'Dream it. Wish it. Do it!'"
        };
        return quotes[random.nextInt(quotes.length)];
    }
    
    /**
     * Get weather-based suggestion (placeholder)
     */
    public String getWeatherSuggestion(String condition) {
        // This is a placeholder - actual weather integration would be in main app
        if (condition != null) {
            if (condition.contains("rain")) {
                return "🌧️ Baarish ho rahi hai! Umbrella leke niklo!";
            } else if (condition.contains("sunny")) {
                return "☀️ Dhoop hai! Sunglasses pehen lo!";
            } else if (condition.contains("cold")) {
                return "❄️ Thand hai! Jacket pehen lo!";
            }
        }
        return "🌤️ Weather check karo - aaj ka plan accordingly karo!";
    }
    
    /**
     * Format a command for display
     */
    public String formatCommandResponse(String command, boolean success) {
        if (success) {
            return "✅ " + command + " - Ho gaya! Kuch aur chahiye? 😊";
        } else {
            return "❌ " + command + " - Nahi ho paya. Dobara try karein!";
        }
    }
    
    /**
     * Get personalized response with user name
     */
    public String personalizeResponse(String response) {
        if (!userName.isEmpty()) {
            return response.replace("{name}", userName);
        }
        return response;
    }
    
    /**
     * Get battery low suggestion
     */
    public String getBatteryLowSuggestion(int percentage) {
        if (percentage < 10) {
            return "🚨 Battery critical! " + percentage + "% bacha hai!\n" +
                   "Turant charger lagao ya Battery Saver ON karo!\n\n" +
                   "Karein: 'Battery saver ON karo'";
        } else if (percentage < 20) {
            return "⚠️ Battery low! " + percentage + "% remaining.\n" +
                   "Jaldi charger lagao! 💡";
        } else {
            return "🔋 Battery: " + percentage + "% - Theek hai! 👍";
        }
    }
    
    /**
     * Get driving mode response
     */
    public String getDrivingModeResponse() {
        return "🚗 Driving Mode ON! 🚦\n\n" +
               "Safe drive karein! Main:\n" +
               "• Calls attend karunga\n" +
               "• Messages padhunga\n" +
               "• Auto-reply bhej dunga\n\n" +
               "Drive safe! 🙏";
    }
    
    /**
     * Get meeting mode response
     */
    public String getMeetingModeResponse() {
        return "👔 Meeting Mode ON! 💼\n\n" +
               "Phone silent mode mein hai.\n" +
               "Auto-reply active hai.\n\n" +
               "Meeting khatam hone par bata dena!";
    }
    
    /**
     * Convert number to Hindi words (basic)
     */
    public String numberToHindiWords(int number) {
        String[] units = {"", "ek", "do", "teen", "chaar", "paanch", "chhah", "saat", "aath", "nao"};
        String[] tens = {"", "das", "bees", "tees", "chaalis", "pachaas", "saath", "sattar", "assi", "nabbe"};
        
        if (number < 10) {
            return units[number];
        } else if (number < 100) {
            return tens[number / 10] + (number % 10 > 0 ? " " + units[number % 10] : "");
        }
        return String.valueOf(number);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Check if current mode is casual
     */
    public boolean isCasualMode() {
        return currentMode.equals(MODE_CASUAL);
    }
    
    /**
     * Check if current mode is professional
     */
    public boolean isProfessionalMode() {
        return currentMode.equals(MODE_PROFESSIONAL);
    }
    
    /**
     * Check if language is Hindi/Hinglish
     */
    public boolean isHindiLanguage() {
        return currentLanguage.equals(LANG_HINGLISH) || currentLanguage.equals(LANG_HINDI);
    }
    
    /**
     * Get emoji for command type
     */
    public String getEmojiForCommand(String commandType) {
        switch (commandType.toLowerCase()) {
            case "call": return "📞";
            case "sms": return "💬";
            case "wifi": return "📶";
            case "bluetooth": return "🔵";
            case "flashlight": return "🔦";
            case "camera": return "📷";
            case "volume": return "🔊";
            case "lock": return "🔒";
            case "sos": return "🆘";
            case "time": return "⏰";
            case "date": return "📅";
            case "alarm": return "⏰";
            case "reminder": return "📝";
            case "music": return "🎵";
            case "weather": return "🌤️";
            default: return "✨";
        }
    }
    
    /**
     * Reset personality to defaults
     */
    public void resetToDefaults() {
        setMode(MODE_FRIENDLY);
        setLanguage(LANG_HINGLISH);
        userName = "";
        prefs.edit().clear().apply();
        Log.d(TAG, "Personality reset to defaults");
    }
}
