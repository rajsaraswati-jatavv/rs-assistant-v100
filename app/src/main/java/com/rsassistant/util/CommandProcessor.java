package com.rsassistant.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
    private final PreferenceManager prefManager;
    private final DeviceControlManager deviceControl;

    public CommandProcessor(Context context) {
        this.context = context;
        this.prefManager = new PreferenceManager(context);
        this.deviceControl = new DeviceControlManager(context);
    }

    public String process(String command) {
        command = command.toLowerCase(Locale.getDefault()).trim();

        // Check language for responses
        String lang = prefManager.getLanguage();

        // ==================== VOLUME COMMANDS ====================
        // Volume up: "volume jyada karo", "volume badhao", "volume up", "aawaz badhao"
        if (containsAny(command, "volume", "आवाज़", "aawaz", "sound", "ध्वनि")) {
            if (containsAny(command, "badhao", "badao", "badhao", "jyada", "zyada", "up", "बढ़ा", "ऊंचा", "increase", "high", "max", "maximum")) {
                if (containsAny(command, "max", "maximum", "full", "poora", "पूरा")) {
                    return deviceControl.volumeMax();
                }
                return deviceControl.volumeUp();
            }
            // Volume down: "volume kam karo", "volume down", "aawaz kam karo"
            if (containsAny(command, "kam", "down", "neeche", "low", "min", "minimum", "कम", "कम करो", "decrease")) {
                if (containsAny(command, "mute", "zero", "silent", "band", "बंद")) {
                    return deviceControl.volumeMute();
                }
                return deviceControl.volumeDown();
            }
            // Volume mute
            if (containsAny(command, "mute", "chup", "शांत", "silent")) {
                return deviceControl.volumeMute();
            }
            // Default volume command - open settings
            return deviceControl.volumeUp();
        }

        // ==================== LOCK SCREEN COMMANDS ====================
        // Lock screen: "lock karo", "screen lock", "phone lock", "lock screen"
        if (containsAny(command, "lock", "लॉक", "screen lock", "phone lock", "lock screen", "lock karo", "screen band")) {
            if (!containsAny(command, "unlock", "open", "खोलो")) {
                return deviceControl.lockScreen();
            }
        }

        // ==================== POWER OFF COMMANDS ====================
        // Power off: "power off", "band karo", "phone band", "switch off", "shutdown"
        if (containsAny(command, "power off", "poweroff", "band karo", "phone band", "switch off", 
                       "shutdown", "बंद करो", "phone बंद", "power band")) {
            return deviceControl.powerOff();
        }

        // Restart: "restart karo", "reboot", "phone restart"
        if (containsAny(command, "restart", "reboot", "रीस्टार्ट", "restart karo", "phone restart")) {
            return deviceControl.restart();
        }

        // ==================== RINGER MODE COMMANDS ====================
        // Silent mode
        if (containsAny(command, "silent", "साइलेंट", "silent mode", "quiet")) {
            return deviceControl.setSilent();
        }

        // Vibrate mode
        if (containsAny(command, "vibrate", "वाइब्रेट", "vibration", "vibrat")) {
            return deviceControl.setVibrate();
        }

        // Normal mode
        if (containsAny(command, "normal mode", "ringing", "ringer", "general mode")) {
            return deviceControl.setNormal();
        }

        // ==================== MEDIA CONTROL COMMANDS ====================
        // Play/Pause
        if (containsAny(command, "play", "pause", "प्ले", "play pause", "song play", "music play")) {
            if (containsAny(command, "next", "agla", "अगला")) {
                return deviceControl.mediaNext();
            }
            if (containsAny(command, "previous", "pichla", "पिछला", "back")) {
                return deviceControl.mediaPrevious();
            }
            return deviceControl.mediaPlayPause();
        }

        // Next/Previous track
        if (containsAny(command, "next song", "next track", "agla gana", "अगला गाना")) {
            return deviceControl.mediaNext();
        }
        if (containsAny(command, "previous song", "pichla gana", "पिछला गाना", "last song")) {
            return deviceControl.mediaPrevious();
        }

        // ==================== PHONE CONTROL COMMANDS ====================
        if (containsAny(command, "call", "phone", "कॉल", "कॉल करो")) {
            return handleCallCommand(command);
        }

        if (containsAny(command, "message", "sms", "text", "मैसेज", "संदेश")) {
            return handleMessageCommand(command);
        }

        // ==================== NAVIGATION COMMANDS ====================
        if (containsAny(command, "open", "खोलो", "start", "चालू करो", "launch")) {
            return handleOpenCommand(command);
        }

        if (containsAny(command, "close", "बंद करो", "exit", "back", "वापस")) {
            return handleBackCommand();
        }

        if (containsAny(command, "home", "होम", "go home")) {
            return handleHomeCommand();
        }

        // ==================== SETTINGS COMMANDS ====================
        if (containsAny(command, "wifi", "वाईफाई")) {
            return handleWifiCommand(command);
        }

        if (containsAny(command, "bluetooth", "ब्लूटूथ")) {
            return handleBluetoothCommand(command);
        }

        if (containsAny(command, "flashlight", "torch", "टॉर्च", "लाइट", "flash")) {
            return handleFlashlightCommand(command);
        }

        if (containsAny(command, "brightness", "चमक", "डिस्प्ले", "display")) {
            return handleBrightnessCommand(command);
        }

        // ==================== CAMERA COMMANDS ====================
        if (containsAny(command, "camera", "कैमरा", "photo", "picture", "फोटो", "selfie", "picture")) {
            return handleCameraCommand(command);
        }

        // ==================== TIME AND ALARM ====================
        if (containsAny(command, "time", "समय", "बजे", "कितना बजा")) {
            return handleTimeCommand();
        }

        if (containsAny(command, "alarm", "अलार्म", "set alarm", "set timer")) {
            return handleAlarmCommand(command);
        }

        // ==================== ACCESSIBILITY GESTURES ====================
        if (containsAny(command, "scroll up", "ऊपर स्क्रॉल", "upar scroll")) {
            return handleScrollUp();
        }

        if (containsAny(command, "scroll down", "नीचे स्क्रॉल", "neeche scroll")) {
            return handleScrollDown();
        }

        // ==================== SEARCH AND WEB ====================
        if (containsAny(command, "search", "खोजो", "google", "find")) {
            return handleSearchCommand(command);
        }

        // ==================== DEVICE ADMIN ====================
        if (containsAny(command, "device admin", "admin enable", "permission enable")) {
            return deviceControl.enableDeviceAdmin();
        }

        // ==================== GREETING AND HELP ====================
        if (containsAny(command, "hello", "hi", "hey", "नमस्ते", "हेलो", "hola")) {
            return getResponse(lang, "Hello! How can I help you?", "नमस्ते! मैं आपकी कैसे मदद कर सकता हूं?");
        }

        if (containsAny(command, "help", "मदद", "what can you do", "kya kar sakte")) {
            return getHelpResponse(lang);
        }

        // Default response
        return getResponse(lang,
                "I heard: " + command + ". I'm still learning this command.",
                "मैंने सुना: " + command + "। मैं इस कमांड को सीख रहा हूं।");
    }

    private String handleCallCommand(String command) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Dialer khol raha hai";
        } catch (Exception e) {
            return "Dialer nahi khul saka";
        }
    }

    private String handleMessageCommand(String command) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Messages khol raha hai";
        } catch (Exception e) {
            return "Messages nahi khul saka";
        }
    }

    private String handleOpenCommand(String command) {
        String appName = extractAppName(command);

        try {
            Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage(appName);
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return "Opening " + appName;
            }

            // Try to find app by name
            Intent searchIntent = new Intent(Intent.ACTION_VIEW);
            searchIntent.setData(Uri.parse("market://search?q=" + appName));
            searchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(searchIntent);
            return "Searching for " + appName;
        } catch (Exception e) {
            return "Could not open " + appName;
        }
    }

    private String handleBackCommand() {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.goBack();
            return "Going back";
        }
        return "Accessibility service not enabled";
    }

    private String handleHomeCommand() {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.goHome();
            return "Going home";
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Going home";
        } catch (Exception e) {
            return "Could not go home";
        }
    }

    private String handleWifiCommand(String command) {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "WiFi settings khol raha hai";
        } catch (Exception e) {
            return "WiFi settings nahi khul saka";
        }
    }

    private String handleBluetoothCommand(String command) {
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Bluetooth settings khol raha hai";
        } catch (Exception e) {
            return "Bluetooth settings nahi khul saka";
        }
    }

    private String handleFlashlightCommand(String command) {
        try {
            Intent intent = new Intent("com.rsassistant.FLASHLIGHT_TOGGLE");
            context.sendBroadcast(intent);

            boolean turnOn = containsAny(command, "on", "चालू", "enable");
            boolean turnOff = containsAny(command, "off", "बंद", "disable");

            if (turnOn) {
                return "Flashlight on kar diya";
            } else if (turnOff) {
                return "Flashlight band kar diya";
            }
            return "Toggling flashlight";
        } catch (Exception e) {
            return "Flashlight control nahi kar saka";
        }
    }

    private String handleBrightnessCommand(String command) {
        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Display settings khol raha hai";
        } catch (Exception e) {
            return "Display settings nahi khul saka";
        }
    }

    private String handleCameraCommand(String command) {
        try {
            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            if (containsAny(command, "selfie", "front")) {
                return "Selfie ke liye camera khol raha hai";
            }
            return "Camera khol raha hai";
        } catch (Exception e) {
            return "Camera nahi khul saka";
        }
    }

    private String handleTimeCommand() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        String ampm = hour >= 12 ? "PM" : "AM";
        hour = hour % 12;
        if (hour == 0) hour = 12;

        String lang = prefManager.getLanguage();
        if ("hindi".equals(lang)) {
            return String.format("अभी का समय %d बजके %d मिनट है %s", hour, minute, ampm);
        }
        return String.format("Current time is %d:%02d %s", hour, minute, ampm);
    }

    private String handleAlarmCommand(String command) {
        try {
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Alarm settings khol raha hai";
        } catch (Exception e) {
            return "Alarm settings nahi khul saka";
        }
    }

    private String handleScrollUp() {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.scrollUp();
            return "Upar scroll kar raha hai";
        }
        return "Accessibility service enable karo";
    }

    private String handleScrollDown() {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.scrollDown();
            return "Neeche scroll kar raha hai";
        }
        return "Accessibility service enable karo";
    }

    private String handleSearchCommand(String command) {
        String query = command.replace("search", "")
                .replace("google", "")
                .replace("खोजो", "")
                .trim();

        try {
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra("query", query);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Searching for: " + query;
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return "Searching for: " + query;
            } catch (Exception ex) {
                return "Search nahi kar saka";
            }
        }
    }

    private String getResponse(String lang, String english, String hindi) {
        if ("hindi".equals(lang)) {
            return hindi;
        }
        return english;
    }

    private String getHelpResponse(String lang) {
        if ("hindi".equals(lang)) {
            return "मैं ये सब कर सकता हूं:\n" +
                    "• Volume बढ़ाना/कम करना - 'volume jyada karo' ya 'volume kam karo'\n" +
                    "• Screen lock - 'lock karo'\n" +
                    "• Power off - 'power off karo'\n" +
                    "• कॉल करना\n" +
                    "• मैसेज भेजना\n" +
                    "• कैमरा खोलना\n" +
                    "• WiFi, Bluetooth control\n" +
                    "• टॉर्च ऑन/ऑफ\n" +
                    "• समय बताना\n" +
                    "• अलार्म सेट करना\n" +
                    "• Silent/Vibrate mode\n" +
                    "• Music play/pause/next/previous";
        }
        return "I can help you with:\n" +
                "• Volume up/down - 'volume jyada karo' or 'volume kam karo'\n" +
                "• Lock screen - 'lock karo'\n" +
                "• Power off - 'power off karo'\n" +
                "• Make calls and send messages\n" +
                "• Open apps and camera\n" +
                "• Control WiFi, Bluetooth\n" +
                "• Flashlight on/off\n" +
                "• Check time and set alarms\n" +
                "• Silent/Vibrate mode\n" +
                "• Media controls (play/pause/next/previous)\n" +
                "And much more!";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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
}
