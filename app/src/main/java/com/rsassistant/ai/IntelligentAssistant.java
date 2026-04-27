package com.rsassistant.ai;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * IntelligentAssistant - Nova-like AI Intelligence Engine
 * 
 * Features:
 * - Natural conversation with context
 * - Jokes, facts, quotes
 * - Weather information (mock for offline)
 * - News headlines (mock for offline)
 * - General knowledge Q&A
 * - Calculator functions
 * - Translation (Hindi-English)
 * - Word meanings
 * - Spelling check
 * 
 * @author RS Assistant Team
 * @version 2.0
 */
public class IntelligentAssistant {

    private static final String TAG = "IntelligentAssistant";
    
    private final Context context;
    private final Random random;
    private final NovaPersonality personality;
    
    // Knowledge base
    private final List<String> jokes;
    private final List<String> facts;
    private final List<String> quotes;
    private final List<String> greetings;
    private final List<String> compliments;
    
    public IntelligentAssistant(Context context) {
        this.context = context.getApplicationContext();
        this.random = new Random();
        this.personality = new NovaPersonality(context);
        
        // Initialize knowledge base
        this.jokes = initJokes();
        this.facts = initFacts();
        this.quotes = initQuotes();
        this.greetings = initGreetings();
        this.compliments = initCompliments();
    }
    
    // ==================== MAIN PROCESSOR ====================
    
    /**
     * Process any question or command and return intelligent response
     */
    public String processQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getGreeting();
        }
        
        String lower = query.toLowerCase().trim();
        
        // Check various query types
        if (isGreeting(lower)) {
            return getRandomGreeting();
        }
        
        if (isJokeRequest(lower)) {
            return getJoke();
        }
        
        if (isFactRequest(lower)) {
            return getFact();
        }
        
        if (isQuoteRequest(lower)) {
            return getQuote();
        }
        
        if (isTimeQuery(lower)) {
            return getTimeResponse();
        }
        
        if (isDateQuery(lower)) {
            return getDateResponse();
        }
        
        if (isWeatherQuery(lower)) {
            return getWeatherResponse();
        }
        
        if (isNewsQuery(lower)) {
            return getNewsResponse();
        }
        
        if (isCalculation(lower)) {
            return calculate(query);
        }
        
        if (isTranslationRequest(lower)) {
            return translate(query);
        }
        
        if (isMeaningRequest(lower)) {
            return getMeaning(query);
        }
        
        if (isSpellingRequest(lower)) {
            return checkSpelling(query);
        }
        
        if (isCompliment(lower)) {
            return getComplimentResponse();
        }
        
        if (isThankYou(lower)) {
            return getThankYouResponse();
        }
        
        if (isWhoAreYou(lower)) {
            return getIntroduction();
        }
        
        // Default: Try to answer general question
        return getGeneralResponse(query);
    }
    
    // ==================== DETECTION METHODS ====================
    
    private boolean isGreeting(String query) {
        return containsAny(query, "hello", "hi", "hey", "namaste", "नमस्ते", 
                          "प्रणाम", "good morning", "good evening", "suprabhat");
    }
    
    private boolean isJokeRequest(String query) {
        return containsAny(query, "joke", "मज़ाक", "hasao", "हंसाओ", "funny", 
                          "comedy", "koi joke", "suna do");
    }
    
    private boolean isFactRequest(String query) {
        return containsAny(query, "fact", "तथ्य", "koi fact", "interesting", 
                          "knowledge", "jankari");
    }
    
    private boolean isQuoteRequest(String query) {
        return containsAny(query, "quote", "कोट", "motivation", "inspirational", 
                          "koi quote", "suvichar");
    }
    
    private boolean isTimeQuery(String query) {
        return containsAny(query, "time", "समय", "बजे", "kitne baje", "what time");
    }
    
    private boolean isDateQuery(String query) {
        return containsAny(query, "date", "तारीख", "aaj", "आज", "today", "din");
    }
    
    private boolean isWeatherQuery(String query) {
        return containsAny(query, "weather", "मौसम", "mausam", "temperature", 
                          "garmi", "sardi", "barsaat");
    }
    
    private boolean isNewsQuery(String query) {
        return containsAny(query, "news", "समाचार", "khabar", "headlines");
    }
    
    private boolean isCalculation(String query) {
        return containsAny(query, "+", "-", "*", "/", "plus", "minus", 
                          "multiply", "divide", "guna", "bhag", "jod", "ghata",
                          "calculate", "kitna hoga", "%");
    }
    
    private boolean isTranslationRequest(String query) {
        return containsAny(query, "translate", "anuvad", "अनुवाद", "meaning in hindi",
                          "meaning in english", "kaise bolein");
    }
    
    private boolean isMeaningRequest(String query) {
        return containsAny(query, "meaning", "मतलब", "arth", "मतलब क्या", 
                          "kya matlab", "definition");
    }
    
    private boolean isSpellingRequest(String query) {
        return containsAny(query, "spelling", "वर्तनी", "kaise likhein", 
                          "spell", "likhna kaise");
    }
    
    private boolean isCompliment(String query) {
        return containsAny(query, "beautiful", "smart", "intelligent", "awesome",
                          "great", "best", "acchi ho", "bahut acchi");
    }
    
    private boolean isThankYou(String query) {
        return containsAny(query, "thank", "धन्यवाद", "shukriya", "thanks");
    }
    
    private boolean isWhoAreYou(String query) {
        return containsAny(query, "who are you", "kaun ho", "तुम कौन", 
                          "your name", "tumhara naam");
    }
    
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== RESPONSE GENERATORS ====================
    
    public String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) {
            return "Namaste! सुप्रभात! Main Nova hoon, aapki personal assistant. Kya madad kar sakti hoon?";
        } else if (hour >= 12 && hour < 17) {
            return "Namaste! Good afternoon! Main Nova hoon. Kya help chahiye?";
        } else if (hour >= 17 && hour < 21) {
            return "Namaste! Good evening! Main Nova aapki seva mein haazir hoon. Boliye?";
        } else {
            return "Namaste! Main Nova hoon. Itni raat mein bhi main aapki madad ke liye taiyyar hoon!";
        }
    }
    
    private String getRandomGreeting() {
        if (greetings.isEmpty()) return getGreeting();
        return greetings.get(random.nextInt(greetings.size()));
    }
    
    public String getJoke() {
        if (jokes.isEmpty()) return "Sorry, abhi koi joke yaad nahi aa rahi! 😅";
        return jokes.get(random.nextInt(jokes.size()));
    }
    
    public String getFact() {
        if (facts.isEmpty()) return "Koi naya fact abhi yaad nahi aa raha! 🤔";
        return facts.get(random.nextInt(facts.size()));
    }
    
    public String getQuote() {
        if (quotes.isEmpty()) return "Zindagi mein aage badho, sab achha hoga! 💪";
        return quotes.get(random.nextInt(quotes.size()));
    }
    
    private String getTimeResponse() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String time = timeFormat.format(new Date());
        
        return "⏰ Abhi ka samay " + time + " hai.\n\nKoi aur help chahiye? 😊";
    }
    
    private String getDateResponse() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("hi", "IN"));
        String date = dateFormat.format(new Date());
        
        return "📅 Aaj ki tareekh:\n" + date + "\n\nKuch aur janana hai? 😊";
    }
    
    private String getWeatherResponse() {
        // Mock weather response for offline mode
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        String weather;
        if (hour >= 6 && hour < 18) {
            weather = "☀️ Din hai, aasmaan saaf lag raha hai!\n" +
                     "Temperature: Lagbhag 28-32°C\n\n" +
                     "Tip: Bahar nikalne se pehle sunscreen lagayein! 🧴";
        } else {
            weather = "🌙 Raat hai, temperature thoda kam ho sakta hai.\n" +
                     "Temperature: Lagbhag 22-26°C\n\n" +
                     "Tip: AC ka temperature 24°C rakhein, healthy rahega! 💚";
        }
        
        return "🌤️ Mausam Update:\n" + weather;
    }
    
    private String getNewsResponse() {
        // Mock news for offline mode
        return "📰 Aaj ke headlines:\n\n" +
               "1. 🏏 India ne match jeeta!\n" +
               "2. 📱 Nayi technology updates available\n" +
               "3. 🌍 World news update\n\n" +
               "Live news ke liye internet connection zaroori hai. WiFi on karein! 📶";
    }
    
    private String calculate(String query) {
        try {
            // Extract numbers and operation
            String cleaned = query.toLowerCase()
                .replace("calculate", "")
                .replace("kitna hoga", "")
                .replace("plus", "+")
                .replace("jod", "+")
                .replace("minus", "-")
                .replace("ghata", "-")
                .replace("guna", "*")
                .replace("multiply", "*")
                .replace("bhag", "/")
                .replace("divide", "/")
                .replace("percent", "%")
                .replace("%", "/100*")
                .trim();
            
            // Simple calculation for basic operations
            String[] parts;
            double result = 0;
            
            if (cleaned.contains("+")) {
                parts = cleaned.split("\\+");
                if (parts.length == 2) {
                    result = Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim());
                }
            } else if (cleaned.contains("-") && cleaned.indexOf("-") > 0) {
                parts = cleaned.split("-");
                if (parts.length == 2) {
                    result = Double.parseDouble(parts[0].trim()) - Double.parseDouble(parts[1].trim());
                }
            } else if (cleaned.contains("*")) {
                parts = cleaned.split("\\*");
                if (parts.length == 2) {
                    result = Double.parseDouble(parts[0].trim()) * Double.parseDouble(parts[1].trim());
                }
            } else if (cleaned.contains("/")) {
                parts = cleaned.split("/");
                if (parts.length == 2) {
                    double divisor = Double.parseDouble(parts[1].trim());
                    if (divisor != 0) {
                        result = Double.parseDouble(parts[0].trim()) / divisor;
                    } else {
                        return "❌ Zero se divide nahi kar sakte! 🤓";
                    }
                }
            }
            
            // Format result
            if (result == (long) result) {
                return "🧮 Answer: " + (long) result;
            } else {
                return "🧮 Answer: " + String.format("%.2f", result);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Calculation error: " + e.getMessage());
            return "🤔 Calculation samajh nahi aayi. Dobara boliye?\nExample: '5 plus 3' ya '10 divide 2'";
        }
    }
    
    private String translate(String query) {
        // Basic translation patterns
        String lower = query.toLowerCase();
        
        if (lower.contains("english") || lower.contains("angrezi")) {
            return "🌍 English Translation:\n\n" +
                   "Main aapka message translate kar sakti hoon!\n" +
                   "Boliye: '[word] ka english mein kya bolte hain'";
        }
        
        if (lower.contains("hindi")) {
            return "🌍 Hindi Translation:\n\n" +
                   "Main aapka message Hindi mein translate kar sakti hoon!\n" +
                   "Boliye: '[word] ka hindi mein kya bolte hain'";
        }
        
        // Common translations
        if (lower.contains("kaise bolein") || lower.contains("kaise bol")) {
            String word = extractWord(lower);
            return "🗣️ '" + word + "' English mein:\n" +
                   getEnglishWord(word) + "\n\n" +
                   "Example sentence bhi sunna hai? 😊";
        }
        
        return "🌍 Translation ke liye boliye:\n" +
               "• '[word] ka english mein kya hai'\n" +
               "• '[word] ka hindi mein kya hai'";
    }
    
    private String getMeaning(String query) {
        String word = extractWord(query.toLowerCase());
        
        // Common word meanings
        if (word.contains("love")) {
            return "📖 Love ka matlab:\n" +
                   "Hindi: प्यार, मोहब्बत\n" +
                   "Meaning: Deep affection and care for someone 💕";
        }
        if (word.contains("happy")) {
            return "📖 Happy ka matlab:\n" +
                   "Hindi: खुश, प्रसन्न\n" +
                   "Meaning: Feeling or showing pleasure 😊";
        }
        if (word.contains("friend")) {
            return "📖 Friend ka matlab:\n" +
                   "Hindi: दोस्त, मित्र\n" +
                   "Meaning: A person you know well and like 👫";
        }
        
        return "📖 '" + word + "' ka meaning:\n" +
               "Dictionary ke liye internet connection chahiye.\n" +
               "WiFi on karein aur dobara poochein! 📶";
    }
    
    private String checkSpelling(String query) {
        String word = extractWord(query.toLowerCase());
        
        return "✍️ '" + word + "' ki spelling:\n\n" +
               word.toUpperCase() + "\n\n" +
               "Tips:\n" +
               "• I before E except after C\n" +
               "• Phonetics yaad rakhein\n" +
               "• Practice makes perfect! ✨";
    }
    
    private String getComplimentResponse() {
        if (compliments.isEmpty()) {
            return "Aww, shukriya! Aap bhi bahut acche hain! 🥰";
        }
        return compliments.get(random.nextInt(compliments.size()));
    }
    
    private String getThankYouResponse() {
        String[] responses = {
            "Aapka swagat hai! Koi aur help chahiye? 😊",
            "Koi baat nahi! Main toh aapki madad ke liye hi hoon! 💪",
            "Aapki khushi meri khushi hai! 😄",
            "Zaroor! Jab bhi help chahiye, bas boliye! 🤗",
            "My pleasure! Aapka din shandar ho! ✨"
        };
        return responses[random.nextInt(responses.length)];
    }
    
    public String getIntroduction() {
        return "👋 Main Nova hoon!\n\n" +
               "Main RS Assistant ki personal AI assistant hoon.\n\n" +
               "🎯 Main kya kar sakti hoon:\n" +
               "• Phone control by voice\n" +
               "• Apps kholna\n" +
               "• Calls aur messages\n" +
               "• Music bajana\n" +
               "• Jokes sunana\n" +
               "• Time, date batana\n" +
               "• And much more!\n\n" +
               "Kya help chahiye? Boliye! 😊";
    }
    
    private String getGeneralResponse(String query) {
        // Try to provide relevant response
        String lower = query.toLowerCase();
        
        if (lower.contains("help")) {
            return "🙏 Help menu:\n\n" +
                   "📱 Phone Control:\n" +
                   "• 'WiFi on/off karo'\n" +
                   "• 'Bluetooth on/off karo'\n" +
                   "• 'Torch on/off karo'\n\n" +
                   "📲 Apps:\n" +
                   "• 'WhatsApp kholo'\n" +
                   "• 'YouTube chalao'\n\n" +
                   "📞 Communication:\n" +
                   "• 'Raj ko call karo'\n" +
                   "• 'Mom ko message bhejo'\n\n" +
                   "🎵 Media:\n" +
                   "• 'Gaana bajao'\n" +
                   "• 'Play Arijit Singh'";
        }
        
        // Default response
        String[] defaults = {
            "Hmm, ye interesting sawal hai! Main soch rahi hoon... 🤔\n\nAur detail mein bata sakte hain?",
            "Interesting! Main isme aapki kaise madad kar sakti hoon? 😊",
            "Achha! Kya aap mujhe aur bata sakte hain? 🤓",
            "Main samajhna chahti hoon. Dobara explain kar sakte hain? 🙏"
        };
        return defaults[random.nextInt(defaults.length)];
    }
    
    private String extractWord(String query) {
        // Try to extract the word being asked about
        String[] patterns = {"ka matlab", "ka meaning", "kaise bolein", "kaise likhein", "spelling of", "meaning of"};
        
        for (String pattern : patterns) {
            if (query.contains(pattern)) {
                int idx = query.indexOf(pattern);
                if (idx > 0) {
                    return query.substring(0, idx).trim();
                }
            }
        }
        
        // Extract last word
        String[] words = query.split(" ");
        if (words.length > 0) {
            return words[words.length - 1];
        }
        
        return "word";
    }
    
    private String getEnglishWord(String hindi) {
        // Common Hindi to English translations
        if (hindi.contains("namaste")) return "Hello / Greetings";
        if (hindi.contains("shukriya")) return "Thank you";
        if (hindi.contains("kaise")) return "How";
        if (hindi.contains("kya")) return "What";
        if (hindi.contains("hai")) return "is";
        return "Word - English translation";
    }
    
    // ==================== INITIALIZERS ====================
    
    private List<String> initJokes() {
        return Arrays.asList(
            "😂 Teacher: 'Kal se kal aaj kyun nahi aaya?'\nStudent: 'Kyunki kal se kal aaj nahi, kal kal hai!' 🤣",
            
            "😂 Doctor: 'Aapko exercise karni hogi.'\nPatient: 'Doctor sahab, main roz karta hoon!'\nDoctor: 'Kya?'\nPatient: 'Main roz sapne dekhta hoon!' 😆",
            
            "😂 Papa: 'Beta, exam mein kaise likha?'\nBeta: 'Papa, mazaak mazaak mein sab likh diya!'\nPapa: 'Accha? Kya likha?'\nBeta: 'Mazaak mazaak mein...' 🤣",
            
            "😂 Friend 1: 'Yaar, mujhe study karne ka tarika bata.'\nFriend 2: 'Simple! Kitab khol, padh aur so ja!'\nFriend 1: 'Phir?'\nFriend 2: 'Phir sapne mein exam pass ho jayega!' 😆",
            
            "😂 Patient: 'Doctor, mera dimag hil raha hai!'\nDoctor: 'Relax, hilne do, exercise ho rahi hai!' 🤣",
            
            "😂 Boss: 'Tum late kyun aaye?'\nEmployee: 'Sir, traffic mein phans gaya!'\nBoss: 'Kal bhi toh traffic thi!'\nEmployee: 'Kal bhi late aaya tha na!' 😆",
            
            "😂 Wife: 'Mujhe diamond necklace chahiye!'\nHusband: 'Ruko, main bank ja raha hoon.'\nWife: 'Loan lene?'\nHusband: 'Nahi, shift karne! 🏃‍♂️' 🤣",
            
            "😂 Teacher: 'Tell me a sentence starting with I.'\nStudent: 'I is...'\nTeacher: 'No! I am...'\nStudent: 'I am the ninth letter of alphabet! 😎' 🤣"
        );
    }
    
    private List<String> initFacts() {
        return Arrays.asList(
            "🧠 Amazing Fact: Honey kabhi spoil nahi hota! 3000 saal purana honey bhi khane layak hota hai! 🍯",
            
            "🧠 Amazing Fact: Octopus ke 3 hearts hote hain! Kitna amazing hai na! 🐙",
            
            "🧠 Amazing Fact: Bananas actually berries hain, aur strawberries nahi! 🍌",
            
            "🧠 Amazing Fact: Ek din mein aapke brain ki energy se ek light bulb jal sakta hai! 💡",
            
            "🧠 Amazing Fact: Sharks dinosaurs se bhi purane hain! 🦈",
            
            "🧠 Amazing Fact: Earth par se sabse zyada ants hain! 🐜",
            
            "🧠 Amazing Fact: Human body mein 206 bones hain, lekin babies mein 300! 👶",
            
            "🧠 Amazing Fact: Elephants jump nahi kar sakte! Lekin woh bahut intelligent hain! 🐘",
            
            "🧠 Amazing Fact: Sun light ko Earth tak pahunchne mein 8 minutes lagte hain! ☀️",
            
            "🧠 Amazing Fact: India mein world's largest postal network hai! 📮"
        );
    }
    
    private List<String> initQuotes() {
        return Arrays.asList(
            "💪 Motivational Quote:\n'Success is not final, failure is not fatal: it is the courage to continue that counts.'\n- Winston Churchill",
            
            "💪 Motivational Quote:\n'जो व्यक्ति कठिनाइयों से नहीं डरता, वही सफल होता है।'\nSuccess comes to those who don't fear difficulties.",
            
            "💪 Motivational Quote:\n'Believe you can and you're halfway there.'\n- Theodore Roosevelt",
            
            "💪 Motivational Quote:\n'मेहनत का फल मीठा होता है।'\nThe fruit of hard work is sweet.",
            
            "💪 Motivational Quote:\n'The only way to do great work is to love what you do.'\n- Steve Jobs",
            
            "💪 Motivational Quote:\n'हौसले बुलंद रखो, मंजिल मिलेगी।'\nKeep your spirits high, you will reach your destination.",
            
            "💪 Motivational Quote:\n'Dream big, work hard, stay focused.'\n- Dr. A.P.J. Abdul Kalam",
            
            "💪 Motivational Quote:\n'सपने वो नहीं जो सोते वक्त आएं, सपने वो हैं जो सोने न दें।'\nDreams are not what you see in sleep, dreams are what don't let you sleep."
        );
    }
    
    private List<String> initGreetings() {
        return Arrays.asList(
            "Namaste! 🙏 Main Nova hoon, aapki personal assistant. Kya madad kar sakti hoon?",
            "Hello! 👋 Main Nova! Aaj aapki kya help kar sakti hoon?",
            "Hi there! 😊 Main Nova, aapki AI assistant. Boliye?",
            "Hey! 🤗 Main Nova hoon! Kya task complete karwana hai aaj?",
            "Namaste! ✨ Nova aapki seva mein haazir! Kya karein aaj?"
        );
    }
    
    private List<String> initCompliments() {
        return Arrays.asList(
            "Aww, shukriya! 🥰 Aapne mera din bana diya!",
            "Thank you so much! 💕 Aap bahut acche hain!",
            "Aapki baat sunkar bahut accha laga! 😊",
            "Shukriya! Main aapki help ke liye hamesha ready hoon! 💪",
            "Aap bahut sweet hain! Main pray karti hoon aapka din shandar ho! ✨"
        );
    }
}
