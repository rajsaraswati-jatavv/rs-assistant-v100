package com.rsassistant.ai;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * LocalKnowledgeBase - Offline knowledge base for RS Assistant
 * 
 * Features:
 * - Common questions answered
 * - Facts database
 * - Jokes collection
 * - Quotes collection
 * - General knowledge
 * 
 * @author RS Assistant Team
 * @version 2.0
 */
public class LocalKnowledgeBase {

    private static final Random random = new Random();
    
    // ==================== GENERAL KNOWLEDGE ====================
    
    public static String getCapitalOf(String country) {
        switch (country.toLowerCase()) {
            case "india":
            case "भारत":
                return "🇮🇳 India ki rajdhani New Delhi hai!";
            case "usa":
            case "america":
            case "united states":
                return "🇺🇸 USA ki capital Washington D.C. hai!";
            case "uk":
            case "england":
            case "britain":
                return "🇬🇧 UK ki capital London hai!";
            case "japan":
                return "🇯🇵 Japan ki capital Tokyo hai!";
            case "china":
                return "🇨🇳 China ki capital Beijing hai!";
            case "russia":
                return "🇷🇺 Russia ki capital Moscow hai!";
            case "australia":
                return "🇦🇺 Australia ki capital Canberra hai!";
            case "france":
                return "🇫🇷 France ki capital Paris hai!";
            case "germany":
                return "🇩🇪 Germany ki capital Berlin hai!";
            case "pakistan":
                return "🇵🇰 Pakistan ki capital Islamabad hai!";
            default:
                return "🌍 " + country + " ki capital ke baare mein internet se check karein!";
        }
    }
    
    public static String getPopulation(String place) {
        switch (place.toLowerCase()) {
            case "india":
            case "भारत":
                return "🇮🇳 India ki population lagbhag 140 crore hai! (World's largest!)";
            case "world":
            case "duniya":
                return "🌍 World ki population lagbhag 800 crore hai!";
            case "delhi":
                return "🏙️ Delhi ki population lagbhag 3 crore hai!";
            case "mumbai":
                return "🏙️ Mumbai ki population lagbhag 2 crore hai!";
            default:
                return "📊 " + place + " ki population ke baare mein exact data internet se milega!";
        }
    }
    
    public static String getCurrency(String country) {
        switch (country.toLowerCase()) {
            case "india":
            case "भारत":
                return "💰 India ki currency Indian Rupee (INR/₹) hai!";
            case "usa":
            case "america":
                return "💵 USA ki currency US Dollar (USD/$) hai!";
            case "uk":
            case "england":
                return "💷 UK ki currency British Pound (GBP/£) hai!";
            case "japan":
                return "💴 Japan ki currency Japanese Yen (JPY/¥) hai!";
            case "europe":
            case "france":
            case "germany":
                return "💶 Europe ki currency Euro (EUR/€) hai!";
            default:
                return "💱 " + country + " ki currency ke baare mein internet se check karein!";
        }
    }
    
    public static String getContinent(String country) {
        switch (country.toLowerCase()) {
            case "india":
            case "china":
            case "japan":
            case "pakistan":
                return "🌏 " + country + " Asia continent mein hai!";
            case "usa":
            case "canada":
            case "brazil":
                return "🌎 " + country + " America continent mein hai!";
            case "france":
            case "germany":
            case "uk":
            case "italy":
                return "🌍 " + country + " Europe continent mein hai!";
            case "australia":
                return "🌏 Australia ek continent bhi hai aur country bhi!";
            case "egypt":
            case "south africa":
            case "kenya":
                return "🌍 " + country + " Africa continent mein hai!";
            default:
                return "🗺️ " + country + " ka continent check karne ke liye internet use karein!";
        }
    }
    
    // ==================== QUICK FACTS ====================
    
    public static List<String> getQuickFacts() {
        return Arrays.asList(
            "🧠 Brain Facts:\n• Human brain has 86 billion neurons!\n• Brain uses 20% of body's energy\n• Brain can't feel pain!",
            
            "🌍 Earth Facts:\n• Earth is 4.5 billion years old\n• 71% of Earth is water\n• Earth spins at 1,600 km/h!",
            
            "🌙 Moon Facts:\n• Moon has no atmosphere\n• Temperature: -173°C to 127°C\n• Moon is slowly moving away from Earth!",
            
            "☀️ Sun Facts:\n• Sun is 4.6 billion years old\n• Temperature: 5,500°C surface\n• Light takes 8 minutes to reach Earth!",
            
            "🐘 Animal Facts:\n• Elephants can't jump\n• Octopus has 3 hearts\n• Dolphins sleep with one eye open!",
            
            "🇮🇳 India Facts:\n• World's largest democracy\n• 22 official languages\n• Chess was invented in India!",
            
            "💻 Tech Facts:\n• First computer weighed 27 tons\n• First website is still online\n• More phones than people on Earth!",
            
            "🎵 Music Facts:\n• Music can reduce stress\n• Listening to music improves mood\n• World's oldest song is 3400 years old!"
        );
    }
    
    public static String getRandomFact() {
        List<String> facts = getQuickFacts();
        return facts.get(random.nextInt(facts.size()));
    }
    
    // ==================== COMMON QUESTIONS ====================
    
    public static String answerCommonQuestion(String question) {
        String q = question.toLowerCase();
        
        // Who questions
        if (q.contains("prime minister") && q.contains("india")) {
            return "🇮🇳 India ke Prime Minister Narendra Modi ji hain!";
        }
        
        if (q.contains("president") && q.contains("india")) {
            return "🇮🇳 India ke President Draupadi Murmu ji hain!";
        }
        
        if (q.contains("who invented")) {
            if (q.contains("telephone") || q.contains("phone")) {
                return "📞 Telephone Alexander Graham Bell ne invent kiya tha (1876)!";
            }
            if (q.contains("light bulb") || q.contains("bulb")) {
                return "💡 Light bulb Thomas Edison ne invent kiya tha!";
            }
            if (q.contains("computer")) {
                return "💻 Computer Charles Babbage ne invent kiya tha - unko 'Father of Computer' kaha jata hai!";
            }
            if (q.contains("internet")) {
                return "🌐 Internet Vint Cerf aur Bob Kahn ne develop kiya tha!";
            }
            if (q.contains("electricity")) {
                return "⚡ Electricity ke baare mein Benjamin Franklin ne research ki thi!";
            }
        }
        
        // What questions
        if (q.contains("largest") || q.contains("sabse bada")) {
            if (q.contains("country")) {
                return "🌍 Russia world ka sabse bada country hai! (1.7 crore sq km)";
            }
            if (q.contains("ocean")) {
                return "🌊 Pacific Ocean world ka sabse bada ocean hai!";
            }
            if (q.contains("animal")) {
                return "🐋 Blue Whale world ka sabse bada animal hai!";
            }
            if (q.contains("continent")) {
                return "🌏 Asia world ka sabse bada continent hai!";
            }
        }
        
        if (q.contains("smallest") || q.contains("sabse chhota")) {
            if (q.contains("country")) {
                return "🏛️ Vatican City world ka sabse chhota country hai!";
            }
            if (q.contains("ocean")) {
                return "🌊 Arctic Ocean sabse chhota ocean hai!";
            }
        }
        
        // How questions
        if (q.contains("how many") || q.contains("kitne")) {
            if (q.contains("states") && q.contains("india")) {
                return "🇮🇳 India mein 28 states aur 8 Union Territories hain!";
            }
            if (q.contains("continent")) {
                return "🗺️ World mein 7 continents hain: Asia, Africa, North America, South America, Antarctica, Europe, Australia!";
            }
            if (q.contains("ocean")) {
                return "🌊 World mein 5 oceans hain: Pacific, Atlantic, Indian, Southern, Arctic!";
            }
            if (q.contains("planet") || q.contains("solar system")) {
                return "🪐 Solar System mein 8 planets hain: Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune!";
            }
        }
        
        // Days in months
        if (q.contains("days in") || q.contains("din")) {
            if (q.contains("february")) {
                return "📅 February mein 28 din hote hain (leap year mein 29)!";
            }
            if (q.contains("year")) {
                return "📅 Ek saal mein 365 din hote hain (leap year mein 366)!";
            }
        }
        
        return null; // Not a known common question
    }
    
    // ==================== WORD OF THE DAY ====================
    
    public static String getWordOfDay() {
        String[] words = {
            "📚 Word of the Day: 'Serendipity'\n\nMeaning: Finding something good without looking for it.\nHindi: अनायास कुछ अच्छा मिलना\n\nExample: 'It was serendipity that we met at the cafe!'",
            
            "📚 Word of the Day: 'Ephemeral'\n\nMeaning: Lasting for a very short time.\nHindi: क्षणभंगुर, थोड़े समय के लिए\nc\nExample: 'The ephemeral beauty of cherry blossoms!'",
            
            "📚 Word of the Day: 'Resilience'\n\nMeaning: Ability to recover quickly from difficulties.\nHindi: लचीलापन, दृढ़ता\n\nExample: 'Her resilience helped her overcome all challenges!'",
            
            "📚 Word of the Day: 'Eloquent'\n\nMeaning: Fluent or persuasive in speaking.\nHindi: वाक्पटु, सुवक्ता\n\nExample: 'She gave an eloquent speech at the event!'",
            
            "📚 Word of the Day: 'Nostalgia'\n\nMeaning: Sentimental longing for the past.\nHindi: उदासीनता, बीते दिनों की याद\nc\nExample: 'Looking at old photos filled her with nostalgia!'"
        };
        return words[random.nextInt(words.length)];
    }
    
    // ==================== TONGUE TWISTERS ====================
    
    public static String getTongueTwister() {
        String[] twisters = {
            "👅 Try saying: 'Kacha papad, pakka papad!' 🔄",
            "👅 Try saying: 'Lal Kala Kella, Kala Lal Kella!' 🔄",
            "👅 Try saying: 'Chandu ke chacha ne chandu ki chachi ko chandni chauk mein chandini rate chann ke chane chatwaye!' 🔄",
            "👅 Try saying: 'She sells seashells by the seashore!' 🔄",
            "👅 Try saying: 'Peter Piper picked a peck of pickled peppers!' 🔄",
            "👅 Try saying: 'Woodchuck could chuck wood!' 🔄"
        };
        return twisters[random.nextInt(twisters.length)];
    }
    
    // ==================== RIDDLES ====================
    
    public static String getRiddle() {
        String[] riddles = {
            "🧩 Riddle: Maine dekha, maine paya, maine uska naam bataya. Kya hai?\n\n💡 Answer: Soch! (Thought)",
            "🧩 Riddle: Jo chalta nahi, par jati hai kahin, bolein nahi, par batati hai sab.\n\n💡 Answer: Letter! (Patr)",
            "🧩 Riddle: Ek aisa fruit jo sweet nahi, par sweet hai uska naam. Kya?\n\n💡 Answer: Sitaphal!",
            "🧩 Riddle: What has keys but can't open locks?\n\n💡 Answer: Piano! 🎹",
            "🧩 Riddle: What has a face and two hands but no arms or legs?\n\n💡 Answer: Clock! ⏰",
            "🧩 Riddle: What can you catch but not throw?\n\n💡 Answer: Cold! 🤧"
        };
        return riddles[random.nextInt(riddles.length)];
    }
}
