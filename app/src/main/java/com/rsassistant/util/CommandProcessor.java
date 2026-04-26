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

    public CommandProcessor(Context context) {
        this.context = context;
        this.prefManager = new PreferenceManager(context);
    }

    public String process(String command) {
        command = command.toLowerCase(Locale.getDefault()).trim();

        // Check language for responses
        String lang = prefManager.getLanguage();

        // Phone control commands
        if (containsAny(command, "call", "phone", "कॉल", "कॉल करो")) {
            return handleCallCommand(command);
        }

        if (containsAny(command, "message", "sms", "text", "मैसेज", "संदेश")) {
            return handleMessageCommand(command);
        }

        // Navigation commands
        if (containsAny(command, "open", "खोलो", "start", "चालू करो")) {
            return handleOpenCommand(command);
        }

        if (containsAny(command, "close", "बंद करो", "exit", "back", "वापस")) {
            return handleBackCommand();
        }

        if (containsAny(command, "home", "होम", "go home")) {
            return handleHomeCommand();
        }

        // Settings commands
        if (containsAny(command, "wifi", "वाईफाई")) {
            return handleWifiCommand(command);
        }

        if (containsAny(command, "bluetooth", "ब्लूटूथ")) {
            return handleBluetoothCommand(command);
        }

        if (containsAny(command, "flashlight", "torch", "टॉर्च", "लाइट")) {
            return handleFlashlightCommand(command);
        }

        if (containsAny(command, "brightness", "चमक", "डिस्प्ले")) {
            return handleBrightnessCommand(command);
        }

        if (containsAny(command, "volume", "आवाज़", "sound", "ध्वनि")) {
            return handleVolumeCommand(command);
        }

        // Media commands
        if (containsAny(command, "camera", "कैमरा", "photo", "picture", "फोटो", "selfie")) {
            return handleCameraCommand(command);
        }

        if (containsAny(command, "music", "song", "play", "गाना", "music")) {
            return handleMediaCommand(command);
        }

        // Time and alarm
        if (containsAny(command, "time", "समय", "बजे")) {
            return handleTimeCommand();
        }

        if (containsAny(command, "alarm", "अलार्म", "set alarm")) {
            return handleAlarmCommand(command);
        }

        // Accessibility gestures
        if (containsAny(command, "scroll up", "ऊपर स्क्रॉल")) {
            return handleScrollUp();
        }

        if (containsAny(command, "scroll down", "नीचे स्क्रॉल")) {
            return handleScrollDown();
        }

        // Search and web
        if (containsAny(command, "search", "खोजो", "google")) {
            return handleSearchCommand(command);
        }

        // Greeting and help
        if (containsAny(command, "hello", "hi", "hey", "नमस्ते", "हेलो")) {
            return getResponse(lang, "Hello! How can I help you?", "नमस्ते! मैं आपकी कैसे मदद कर सकता हूं?");
        }

        if (containsAny(command, "help", "मदद", "what can you do")) {
            return getHelpResponse(lang);
        }

        // Default response
        return getResponse(lang,
                "I heard: " + command + ". I'm still learning this command.",
                "मैंने सुना: " + command + "। मैं इस कमांड को सीख रहा हूं।");
    }

    private String handleCallCommand(String command) {
        // Extract contact name and attempt to call
        String response = "Opening dialer for call";
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            response = "Could not open dialer";
        }
        return response;
    }

    private String handleMessageCommand(String command) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Opening messages";
        } catch (Exception e) {
            return "Could not open messages";
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
            return "Opening WiFi settings";
        } catch (Exception e) {
            return "Could not open WiFi settings";
        }
    }

    private String handleBluetoothCommand(String command) {
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Opening Bluetooth settings";
        } catch (Exception e) {
            return "Could not open Bluetooth settings";
        }
    }

    private String handleFlashlightCommand(String command) {
        try {
            Intent intent = new Intent("com.rsassistant.FLASHLIGHT_TOGGLE");
            context.sendBroadcast(intent);

            boolean turnOn = containsAny(command, "on", "चालू", "enable");
            boolean turnOff = containsAny(command, "off", "बंद", "disable");

            if (turnOn) {
                return "Flashlight on";
            } else if (turnOff) {
                return "Flashlight off";
            }
            return "Toggling flashlight";
        } catch (Exception e) {
            return "Could not control flashlight";
        }
    }

    private String handleBrightnessCommand(String command) {
        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Opening display settings";
        } catch (Exception e) {
            return "Could not open display settings";
        }
    }

    private String handleVolumeCommand(String command) {
        try {
            Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Opening sound settings";
        } catch (Exception e) {
            return "Could not open sound settings";
        }
    }

    private String handleCameraCommand(String command) {
        try {
            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            if (containsAny(command, "selfie", "front")) {
                return "Opening camera for selfie";
            }
            return "Opening camera";
        } catch (Exception e) {
            return "Could not open camera";
        }
    }

    private String handleMediaCommand(String command) {
        return "Media playback command received";
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
            return "Opening alarm settings";
        } catch (Exception e) {
            return "Could not open alarm settings";
        }
    }

    private String handleScrollUp() {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.scrollUp();
            return "Scrolling up";
        }
        return "Accessibility service not enabled";
    }

    private String handleScrollDown() {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.scrollDown();
            return "Scrolling down";
        }
        return "Accessibility service not enabled";
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
                return "Could not perform search";
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
            return "मैं कॉल करने, मैसेज भेजने, कैमरा खोलने, WiFi, Bluetooth, टॉर्च नियंत्रित करने, " +
                    "समय बताने, अलार्म सेट करने और बहुत कुछ कर सकता हूं।";
        }
        return "I can help you make calls, send messages, open apps, control WiFi, Bluetooth, " +
                "flashlight, check time, set alarms, search the web, and much more!";
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
