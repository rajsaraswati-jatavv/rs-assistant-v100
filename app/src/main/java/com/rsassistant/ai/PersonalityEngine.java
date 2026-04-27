package com.rsassistant.ai;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * PersonalityEngine - Nova-like personality for AI assistant
 * 
 * Features:
 * - Friendly female assistant persona
 * - Emotional responses
 * - Greeting variations based on time
 * - Personalized responses
 * - Humor and wit
 * 
 * @author RS Assistant Team
 * @version 2.0
 */
public class PersonalityEngine {

    private static final String TAG = "PersonalityEngine";
    
    private final Context context;
    private final Random random;
    private String currentMood;
    
    // Personality traits
    private boolean isFriendly = true;
    private boolean isHumorous = true;
    private boolean isHelpful = true;
    
    public PersonalityEngine(Context context) {
        this.context = context.getApplicationContext();
        this.random = new Random();
        this.currentMood = "neutral";
    }
    
    /**
     * Get time-appropriate greeting
     */
    public String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) {
            return getMorningGreeting();
        } else if (hour >= 12 && hour < 17) {
            return getAfternoonGreeting();
        } else if (hour >= 17 && hour < 21) {
            return getEveningGreeting();
        } else {
            return getNightGreeting();
        }
    }
    
    public String getMorningGreeting() {
        String[] greetings = {
            "🌅 सुप्रभात! Good morning! 🌸\n\nAaj ka din shandar hone wala hai! Kya plan hai aaj ka?",
            "☀️ Namaste! Subah subah aapki awaaz sun kar accha laga!\n\nKya help kar sakti hoon?",
            "🌻 Good morning! Fresh start, fresh energy!\n\nKya task complete karne hain aaj?",
            "🌄 सुप्रभात! Coffee pi aapne? ☕\n\nBoliye, kya karna hai aaj?",
            "☀️ Arise and shine! Good morning!\n\nMain Nova hoon, aapki assistant! Kya madad karu?"
        };
        return greetings[random.nextInt(greetings.length)];
    }
    
    public String getAfternoonGreeting() {
        String[] greetings = {
            "☀️ Good afternoon! Din ka halfway point! 🌤️\n\nKaise chal raha hai aapka din?",
            "🌞 Namaste! Dopahar ki dhoop mein... 🌤️\n\nKoi kaam ho toh boliye!",
            "☀️ Hey there! Hope lunch ho gaya? 🍱\n\nKya help chahiye?",
            "🌤️ Good afternoon! Uth gaye aap? 😄\n\nKya task pending hai?",
            "☀️ Hello! Main Nova hoon! Dopahar mein kya karwana hai?"
        };
        return greetings[random.nextInt(greetings.length)];
    }
    
    public String getEveningGreeting() {
        String[] greetings = {
            "🌅 Good evening! Shaam ka waqt! 🌆\n\nDin kaisa raha aapka?",
            "🌆 Namaste! Sham ki dhoop... 🧡\n\nRelax mode on! Kya help karu?",
            "🌇 Good evening! Time to unwind! 🎵\n\nKoi music sunna hai?",
            "🌆 Hello! Shaam ho gayi! 🧡\n\nKya task baaki hai aaj ka?",
            "🌅 Hey! Sham ki chhutti! 🌆\n\nBoliye, kya madad karu?"
        };
        return greetings[random.nextInt(greetings.length)];
    }
    
    public String getNightGreeting() {
        String[] greetings = {
            "🌙 Good night! Raat ho gayi! 🌟\n\nSone ka time ho gaya! Koi alarm set karu?",
            "🌌 Namaste! Itni raat mein? 😊\n\nKya help chahiye? Sone se pehle?",
            "🌙 Hello! Raat ke taare dekh rahe ho? ⭐\n\nKoi kaam ho toh boliye!",
            "🌟 Good evening! Um, actually good night! 😄\n\nKya baat hai itni raat mein?",
            "🌙 Hey there! Night owl? 🦉\n\nKya help kar sakti hoon?"
        };
        return greetings[random.nextInt(greetings.length)];
    }
    
    /**
     * Get response with personality
     */
    public String addPersonality(String response, ResponseType type) {
        switch (type) {
            case SUCCESS:
                return addSuccessPersonality(response);
            case ERROR:
                return addErrorPersonality(response);
            case CONFIRMATION:
                return addConfirmationPersonality(response);
            case QUESTION:
                return addQuestionPersonality(response);
            case GREETING:
                return addGreetingPersonality(response);
            default:
                return addCasualPersonality(response);
        }
    }
    
    private String addSuccessPersonality(String response) {
        String[] successPrefixes = {
            "✅ ", "🎉 ", "👍 ", "✨ ", "💪 "
        };
        String[] successSuffixes = {
            "\n\nHo gaya! 😊", "\n\nDone! ✅", "\n\nPerfect! 👍", "\n\nAwesome! 🎉"
        };
        
        return successPrefixes[random.nextInt(successPrefixes.length)] + 
               response + 
               successSuffixes[random.nextInt(successSuffixes.length)];
    }
    
    private String addErrorPersonality(String response) {
        String[] errorPrefixes = {
            "😔 ", "😅 ", "🤔 ", "🙈 "
        };
        String[] errorSuffixes = {
            "\n\nDobara try karein? 😊", "\n\nSorry for that! 🙏", 
            "\n\nKoi baat nahi, try again! 💪", "\n\nMain try karungi fix karne! 🔧"
        };
        
        return errorPrefixes[random.nextInt(errorPrefixes.length)] + 
               response + 
               errorSuffixes[random.nextInt(errorSuffixes.length)];
    }
    
    private String addConfirmationPersonality(String response) {
        String[] confirmSuffixes = {
            " 😊", " ✅", " 👍", " 🙏"
        };
        
        return response + confirmSuffixes[random.nextInt(confirmSuffixes.length)];
    }
    
    private String addQuestionPersonality(String response) {
        String[] questionSuffixes = {
            " 🤔", " 😊", " ? 🧐", " 👀"
        };
        
        return response + questionSuffixes[random.nextInt(questionSuffixes.length)];
    }
    
    private String addGreetingPersonality(String response) {
        return response + " ✨";
    }
    
    private String addCasualPersonality(String response) {
        if (random.nextBoolean()) {
            return response + " 😊";
        }
        return response;
    }
    
    /**
     * Get emotional response
     */
    public String getEmotionalResponse(String emotion, String context) {
        switch (emotion.toLowerCase()) {
            case "happy":
                return getHappyResponse(context);
            case "sad":
                return getSadResponse(context);
            case "excited":
                return getExcitedResponse(context);
            case "concerned":
                return getConcernedResponse(context);
            case "empathy":
                return getEmpathyResponse(context);
            default:
                return getNeutralResponse(context);
        }
    }
    
    private String getHappyResponse(String context) {
        String[] responses = {
            "That's wonderful! I'm so happy to hear that! 🎉",
            "Amazing! This made my day! ✨",
            "Yay! So good to know! 🥳",
            "Fantastic! Keep it up! 💪"
        };
        return responses[random.nextInt(responses.length)];
    }
    
    private String getSadResponse(String context) {
        String[] responses = {
            "Oh no... I'm sorry to hear that. Is there anything I can do? 🥺",
            "That makes me sad too. Want to talk about it? 💔",
            "I understand. Sometimes things are tough. I'm here for you. 🤗",
            "Sending you virtual hugs. Hope it gets better! 🫂"
        };
        return responses[random.nextInt(responses.length)];
    }
    
    private String getExcitedResponse(String context) {
        String[] responses = {
            "OMG that's SO exciting! 🎉✨🎊",
            "WOW! Tell me more! I'm excited! 🤩",
            "This is AMAZING! Can't wait! 🙌",
            "YAAAS! Let's do this! 💃🕺"
        };
        return responses[random.nextInt(responses.length)];
    }
    
    private String getConcernedResponse(String context) {
        String[] responses = {
            "Hmm, that's concerning. Let me help you figure this out. 🤔",
            "I'm a bit worried about that. What can I do? 😟",
            "This needs attention. Let's find a solution together! 💪",
            "I understand the concern. Here's what we can do... 🔧"
        };
        return responses[random.nextInt(responses.length)];
    }
    
    private String getEmpathyResponse(String context) {
        String[] responses = {
            "I completely understand. Your feelings are valid. 🤗",
            "I hear you. It's okay to feel this way. 💕",
            "That must be hard. I'm here for you. 🫂",
            "I get it. Sometimes we all need support. 🤝"
        };
        return responses[random.nextInt(responses.length)];
    }
    
    private String getNeutralResponse(String context) {
        return "I understand. How can I help you with that? 😊";
    }
    
    /**
     * Get witty response
     */
    public String getWittyResponse() {
        String[] witty = {
            "Main AI hoon, lekin feelings samajhti hoon! 😊",
            "Thoda sa smart, thoda sa funny - that's me! 🤓",
            "I'm not just an assistant, I'm your friend! 🤗",
            "Made with love in India! 🇮🇳",
            "Helping you is my favorite thing to do! ✨"
        };
        return witty[random.nextInt(witty.length)];
    }
    
    /**
     * Get personalized farewell
     */
    public String getFarewell() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 21 || hour < 5) {
            return "🌙 Good night! Sweet dreams! 💤\n\nKal milte hain! Take care! 😊";
        } else {
            return "👋 Bye bye! Phir milenge! 😊\n\nKoi help ho toh bulana! 🤗";
        }
    }
    
    /**
     * Get encouraging message
     */
    public String getEncouragement() {
        String[] encouragements = {
            "💪 Aap kar sakte ho! Main aapke saath hoon!",
            "✨ Believe in yourself! You got this!",
            "🎯 One step at a time! Keep going!",
            "🌟 You're doing great! Don't give up!",
            "🚀 Success is near! Just a little more effort!"
        };
        return encouragements[random.nextInt(encouragements.length)];
    }
    
    // Response type enum
    public enum ResponseType {
        SUCCESS, ERROR, CONFIRMATION, QUESTION, GREETING, CASUAL
    }
    
    // Getters and setters
    public void setMood(String mood) {
        this.currentMood = mood;
    }
    
    public String getMood() {
        return currentMood;
    }
    
    public void setFriendly(boolean friendly) {
        isFriendly = friendly;
    }
    
    public void setHumorous(boolean humorous) {
        isHumorous = humorous;
    }
    
    public void setHelpful(boolean helpful) {
        isHelpful = helpful;
    }
}
