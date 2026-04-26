package com.rsassistant.util;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.rsassistant.service.RSAccessibilityService;

import java.util.Calendar;
import java.util.Locale;

public class CommandProcessor {

    private final Context context;
    private final PreferenceManager prefs;

    public CommandProcessor(Context context) {
        this.context = context;
        this.prefs = new PreferenceManager(context);
    }

    public String process(String command) {
        command = command.toLowerCase(Locale.getDefault()).trim();
        String lang = prefs.getLanguage();

        // === PHONE CONTROL ===

        if (containsAny(command, "call", "phone", "dial", "कॉल", "फोन")) {
            return handleCall(command, lang);
        }

        if (containsAny(command, "message", "sms", "text", "मैसेज", "संदेश")) {
            return handleMessage(command, lang);
        }

        // === NAVIGATION ===

        if (containsAny(command, "open", "start", "launch", "खोलो", "चालू")) {
            return handleOpenApp(command, lang);
        }

        if (containsAny(command, "close", "exit", "back", "बंद", "वापस")) {
            return handleBack(lang);
        }

        if (containsAny(command, "home", "होम", "go home")) {
            return handleHome(lang);
        }

        if (containsAny(command, "recents", "recent apps", "हाल के")) {
            return handleRecents(lang);
        }

        // === SCROLLING ===

        if (containsAny(command, "scroll up", "ऊपर स्क्रॉल", "upar scroll")) {
            return handleScrollUp(lang);
        }

        if (containsAny(command, "scroll down", "नीचे स्क्रॉल", "niche scroll")) {
            return handleScrollDown(lang);
        }

        // === SETTINGS ===

        if (containsAny(command, "wifi", "वाईफाई")) {
            return handleWifi(command, lang);
        }

        if (containsAny(command, "bluetooth", "ब्लूटूथ")) {
            return handleBluetooth(command, lang);
        }

        if (containsAny(command, "flashlight", "torch", "टॉर्च", "लाइट")) {
            return handleFlashlight(command, lang);
        }

        if (containsAny(command, "brightness", "चमक")) {
            return handleBrightness(lang);
        }

        if (containsAny(command, "volume", "आवाज़", "sound", "ध्वनि")) {
            return handleVolume(command, lang);
        }

        // === MEDIA ===

        if (containsAny(command, "camera", "कैमरा", "photo", "picture", "फोटो")) {
            return handleCamera(command, lang);
        }

        if (containsAny(command, "music", "song", "play", "गाना", "म्यूजिक")) {
            return handleMusic(command, lang);
        }

        // === TIME & ALARM ===

        if (containsAny(command, "time", "समय", "बजे", "कितने बजे")) {
            return handleTime(lang);
        }

        if (containsAny(command, "date", "तारीख", "आज की तारीख")) {
            return handleDate(lang);
        }

        if (containsAny(command, "alarm", "अलार्म", "set alarm")) {
            return handleAlarm(lang);
        }

        // === LOCATION ===

        if (containsAny(command, "location", "where am i", "मेरी लोकेशन", "कहाँ हूँ")) {
            return handleLocation(lang);
        }

        // === SEARCH ===

        if (containsAny(command, "search", "google", "खोजो", "find")) {
            return handleSearch(command, lang);
        }

        if (containsAny(command, "youtube", "यूट्यूब")) {
            return handleYoutube(command, lang);
        }

        // === SCREEN ===

        if (containsAny(command, "screenshot", "स्क्रीनशॉट")) {
            return handleScreenshot(lang);
        }

        if (containsAny(command, "lock screen", "screen lock", "लॉक स्क्रीन")) {
            return handleLockScreen(lang);
        }

        // === BATTERY ===

        if (containsAny(command, "battery", "बैटरी")) {
            return handleBattery(lang);
        }

        // === GREETING ===

        if (containsAny(command, "hello", "hi", "hey", "नमस्ते", "हेलो")) {
            return getResponse(lang, "Hello! How can I help you?", "नमस्ते! मैं आपकी कैसे मदद कर सकता हूं?");
        }

        if (containsAny(command, "how are you", "कैसे हो", "कैसी है आप")) {
            return getResponse(lang, "I'm doing great! How about you?", "मैं बढ़िया हूं! आप कैसे हैं?");
        }

        if (containsAny(command, "thank you", "thanks", "धन्यवाद", "शुक्रिया")) {
            return getResponse(lang, "You're welcome!", "आपका स्वागत है!");
        }

        // === HELP ===

        if (containsAny(command, "help", "मदद", "what can you do", "क्या कर सकते")) {
            return getHelpResponse(lang);
        }

        // === DEFAULT ===

        return getResponse(lang,
                "I heard: " + command + ". Try saying 'help' for available commands.",
                "मैंने सुना: " + command + "। 'मदद' बोलें उपलब्ध कमांड के लिए।");
    }

    // === HANDLERS ===

    private String handleCall(String command, String lang) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Opening dialer", "डायलर खोल रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not open dialer", "डायलर नहीं खुल सका");
        }
    }

    private String handleMessage(String command, String lang) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Opening messages", "मैसेज खोल रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not open messages", "मैसेज नहीं खुल सके");
        }
    }

    private String handleOpenApp(String command, String lang) {
        String appName = extractAppName(command);

        try {
            // Try to find app
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(appName);
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return getResponse(lang, "Opening " + appName, appName + " खोल रहा हूं");
            }

            // Try common package names
            String[] commonApps = {
                    "com.whatsapp", "com.instagram.android", "com.facebook.katana",
                    "com.twitter.android", "com.google.android.youtube",
                    "com.google.android.gm", "com.android.chrome"
            };

            String lowerApp = appName.toLowerCase();
            for (String pkg : commonApps) {
                if (pkg.contains(lowerApp) || lowerApp.contains(pkg.split("\\.")[1])) {
                    Intent pkgIntent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                    if (pkgIntent != null) {
                        pkgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(pkgIntent);
                        return getResponse(lang, "Opening app", "ऐप खोल रहा हूं");
                    }
                }
            }

            // Open Play Store search
            Intent storeIntent = new Intent(Intent.ACTION_VIEW);
            storeIntent.setData(Uri.parse("market://search?q=" + Uri.encode(appName)));
            storeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(storeIntent);
            return getResponse(lang, "Searching for " + appName, appName + " खोज रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not open " + appName, appName + " नहीं खुल सका");
        }
    }

    private String handleBack(String lang) {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.goBack();
            return getResponse(lang, "Going back", "वापस जा रहा हूं");
        }
        return getResponse(lang, "Enable accessibility service for this feature", "इस सुविधा के लिए accessibility service चालू करें");
    }

    private String handleHome(String lang) {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.goHome();
            return getResponse(lang, "Going home", "होम जा रहा हूं");
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Going home", "होम जा रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not go home", "होम नहीं जा सका");
        }
    }

    private String handleRecents(String lang) {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.openRecents();
            return getResponse(lang, "Opening recent apps", "हाल के ऐप्स खोल रहा हूं");
        }
        return getResponse(lang, "Enable accessibility service", "Accessibility service चालू करें");
    }

    private String handleScrollUp(String lang) {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.scrollUp();
            return getResponse(lang, "Scrolling up", "ऊपर स्क्रॉल कर रहा हूं");
        }
        return getResponse(lang, "Enable accessibility service", "Accessibility service चालू करें");
    }

    private String handleScrollDown(String lang) {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.scrollDown();
            return getResponse(lang, "Scrolling down", "नीचे स्क्रॉल कर रहा हूं");
        }
        return getResponse(lang, "Enable accessibility service", "Accessibility service चालू करें");
    }

    private String handleWifi(String command, String lang) {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Opening WiFi settings", "WiFi settings खोल रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not open WiFi settings", "WiFi settings नहीं खुल सकी");
        }
    }

    private String handleBluetooth(String command, String lang) {
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Opening Bluetooth settings", "Bluetooth settings खोल रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not open Bluetooth settings", "Bluetooth settings नहीं खुल सकी");
        }
    }

    private String handleFlashlight(String command, String lang) {
        try {
            boolean turnOn = containsAny(command, "on", "चालू", "enable", "जला");
            boolean turnOff = containsAny(command, "off", "बंद", "disable", "बुझा");

            // Use CameraService if available
            Intent intent = new Intent("com.rsassistant.FLASHLIGHT_TOGGLE");
            intent.putExtra("enable", turnOn && !turnOff);
            context.sendBroadcast(intent);

            if (turnOn && !turnOff) {
                return getResponse(lang, "Flashlight on", "टॉर्च चालू");
            } else if (turnOff) {
                return getResponse(lang, "Flashlight off", "टॉर्च बंद");
            }
            return getResponse(lang, "Toggling flashlight", "टॉर्च टॉगल कर रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not control flashlight", "टॉर्च नियंत्रित नहीं हो सकी");
        }
    }

    private String handleBrightness(String lang) {
        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Opening display settings", "Display settings खोल रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not open display settings", "Display settings नहीं खुल सकी");
        }
    }

    private String handleVolume(String command, String lang) {
        try {
            Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Opening sound settings", "Sound settings खोल रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not open sound settings", "Sound settings नहीं खुल सकी");
        }
    }

    private String handleCamera(String command, String lang) {
        try {
            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            if (containsAny(command, "selfie", "front", "सेल्फी")) {
                return getResponse(lang, "Opening camera for selfie", "सेल्फी के लिए कैमरा खोल रहा हूं");
            }
            return getResponse(lang, "Opening camera", "कैमरा खोल रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not open camera", "कैमरा नहीं खुल सका");
        }
    }

    private String handleMusic(String command, String lang) {
        return getResponse(lang, "Playing music", "म्यूजिक चला रहा हूं");
    }

    private String handleTime(String lang) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        String ampm = hour >= 12 ? "PM" : "AM";
        hour = hour % 12;
        if (hour == 0) hour = 12;

        if ("hindi".equals(lang)) {
            return String.format("अभी का समय %d बजके %d मिनट है %s", hour, minute, ampm);
        }
        return String.format("Current time is %d:%02d %s", hour, minute, ampm);
    }

    private String handleDate(String lang) {
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        String month = now.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        int year = now.get(Calendar.YEAR);

        if ("hindi".equals(lang)) {
            return String.format("आज की तारीख %d %s %d है", day, month, year);
        }
        return String.format("Today is %s %d, %d", month, day, year);
    }

    private String handleAlarm(String lang) {
        try {
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Opening alarm settings", "Alarm settings खोल रहा हूं");
        } catch (Exception e) {
            return getResponse(lang, "Could not open alarm settings", "Alarm settings नहीं खुल सकी");
        }
    }

    private String handleLocation(String lang) {
        return getResponse(lang, "Getting your location...", "आपकी लोकेशन पता कर रहा हूं...");
    }

    private String handleSearch(String command, String lang) {
        String query = extractSearchQuery(command);

        try {
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra("query", query);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Searching for: " + query, "खोज रहा हूं: " + query);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return getResponse(lang, "Searching for: " + query, "खोज रहा हूं: " + query);
            } catch (Exception ex) {
                return getResponse(lang, "Could not perform search", "खोज नहीं हो सकी");
            }
        }
    }

    private String handleYoutube(String command, String lang) {
        String query = command.replace("youtube", "").replace("यूट्यूब", "").trim();

        try {
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage("com.google.android.youtube");
            intent.putExtra("query", query);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return getResponse(lang, "Searching YouTube for: " + query, "YouTube पर खोज रहा हूं: " + query);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return getResponse(lang, "Opening YouTube", "YouTube खोल रहा हूं");
            } catch (Exception ex) {
                return getResponse(lang, "Could not open YouTube", "YouTube नहीं खुल सका");
            }
        }
    }

    private String handleScreenshot(String lang) {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.takeScreenshot();
            return getResponse(lang, "Taking screenshot", "Screenshot ले रहा हूं");
        }
        return getResponse(lang, "Enable accessibility for screenshot", "Screenshot के लिए accessibility चालू करें");
    }

    private String handleLockScreen(String lang) {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.lockScreen();
            return getResponse(lang, "Locking screen", "स्क्रीन लॉक कर रहा हूं");
        }
        return getResponse(lang, "Enable accessibility for screen lock", "Screen lock के लिए accessibility चालू करें");
    }

    private String handleBattery(String lang) {
        return getResponse(lang, "Checking battery status...", "बैटरी स्टेटस चेक कर रहा हूं...");
    }

    // === UTILITIES ===

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String getResponse(String lang, String english, String hindi) {
        return "hindi".equals(lang) ? hindi : english;
    }

    private String getHelpResponse(String lang) {
        if ("hindi".equals(lang)) {
            return "मैं ये कर सकता हूं: कॉल करना, मैसेज भेजना, ऐप्स खोलना, WiFi और Bluetooth control, " +
                    "टॉर्च on/off, कैमरा, time बताना, alarm set करना, YouTube search, और बहुत कुछ!";
        }
        return "I can: make calls, send messages, open apps, control WiFi & Bluetooth, " +
                "toggle flashlight, open camera, tell time, set alarms, search YouTube, and more!";
    }

    private String extractAppName(String command) {
        String[] prefixes = {"open ", "start ", "launch ", "खोलो ", "चालू करो "};
        for (String prefix : prefixes) {
            if (command.contains(prefix)) {
                return command.substring(command.indexOf(prefix) + prefix.length()).trim();
            }
        }
        return command;
    }

    private String extractSearchQuery(String command) {
        String[] prefixes = {"search ", "google ", "find ", "खोजो "};
        for (String prefix : prefixes) {
            if (command.contains(prefix)) {
                return command.substring(command.indexOf(prefix) + prefix.length()).trim();
            }
        }
        return command;
    }
}
