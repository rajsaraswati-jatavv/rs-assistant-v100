package com.rsassistant.control;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VoiceCommandProcessor - Main Command Processing Engine
 * 
 * Processes voice commands in Hindi/English and routes to appropriate controllers
 * 
 * Supported Commands:
 * - App Control: "WhatsApp kholo", "Open Chrome", "Camera chalao"
 * - Calling: "Raj ko call karo", "Call Mom"
 * - SMS: "Mummy ko message bhejo", "Send SMS to Dad"
 * - Media: "Music play karo", "Volume badhao", "Next song"
 * - System: "WiFi on karo", "Flashlight jalao", "Bluetooth off karo"
 * - Navigation: "Maps kholo", "Delhi ka route batao"
 */
public class VoiceCommandProcessor {

    private static final String TAG = "VoiceCommandProcessor";

    // Controllers
    private final AppController appController;
    private final ContactManager contactManager;
    private final MediaController mediaController;
    private final NavigationController navigationController;
    private final SystemController systemController;

    // Command patterns
    private static final Map<String, CommandType> COMMAND_PATTERNS = new HashMap<>();
    
    static {
        // App commands
        COMMAND_PATTERNS.put("(.*)\\s+kholo", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("open\\s+(.*)", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("start\\s+(.*)", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("chalaao\\s+(.*)", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("run\\s+(.*)", CommandType.OPEN_APP);
        
        // Close app
        COMMAND_PATTERNS.put("(.*)\\s+band\\s+karo", CommandType.CLOSE_APP);
        COMMAND_PATTERNS.put("close\\s+(.*)", CommandType.CLOSE_APP);
        COMMAND_PATTERNS.put("kill\\s+(.*)", CommandType.CLOSE_APP);
        
        // Call commands
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+call\\s+karo", CommandType.CALL_CONTACT);
        COMMAND_PATTERNS.put("call\\s+(.*)", CommandType.CALL_CONTACT);
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+phone\\s+karo", CommandType.CALL_CONTACT);
        
        // SMS commands
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+(?:sms|message)\\s+bhejo", CommandType.SEND_SMS);
        COMMAND_PATTERNS.put("send\\s+(?:sms|message)\\s+to\\s+(.*)", CommandType.SEND_SMS);
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+msg\\s+karo", CommandType.SEND_SMS);
        
        // Media commands
        COMMAND_PATTERNS.put("music\\s+play\\s+karo", CommandType.PLAY_MUSIC);
        COMMAND_PATTERNS.put("play\\s+music", CommandType.PLAY_MUSIC);
        COMMAND_PATTERNS.put("gaana\\s+chalaao", CommandType.PLAY_MUSIC);
        COMMAND_PATTERNS.put("pause\\s+karo", CommandType.PAUSE_MUSIC);
        COMMAND_PATTERNS.put("rukho", CommandType.PAUSE_MUSIC);
        COMMAND_PATTERNS.put("next\\s+(?:song|track)", CommandType.NEXT_TRACK);
        COMMAND_PATTERNS.put("agli\\s+gana", CommandType.NEXT_TRACK);
        COMMAND_PATTERNS.put("previous\\s+(?:song|track)", CommandType.PREVIOUS_TRACK);
        COMMAND_PATTERNS.put("pichli\\s+gana", CommandType.PREVIOUS_TRACK);
        
        // Volume commands
        COMMAND_PATTERNS.put("volume\\s+badhao", CommandType.VOLUME_UP);
        COMMAND_PATTERNS.put("volume\\s+up", CommandType.VOLUME_UP);
        COMMAND_PATTERNS.put("volume\\s+aawaz\\s+badhao", CommandType.VOLUME_UP);
        COMMAND_PATTERNS.put("volume\\s+kam\\s+karo", CommandType.VOLUME_DOWN);
        COMMAND_PATTERNS.put("volume\\s+down", CommandType.VOLUME_DOWN);
        COMMAND_PATTERNS.put("aawaz\\s+kam", CommandType.VOLUME_DOWN);
        COMMAND_PATTERNS.put("mute\\s+karo", CommandType.MUTE);
        COMMAND_PATTERNS.put("silent\\s+karo", CommandType.SILENT);
        
        // WiFi commands
        COMMAND_PATTERNS.put("wifi\\s+(?:on|chalao)\\s*karo*", CommandType.WIFI_ON);
        COMMAND_PATTERNS.put("turn\\s+on\\s+wifi", CommandType.WIFI_ON);
        COMMAND_PATTERNS.put("wifi\\s+(?:off|band)\\s*karo*", CommandType.WIFI_OFF);
        COMMAND_PATTERNS.put("turn\\s+off\\s+wifi", CommandType.WIFI_OFF);
        
        // Bluetooth commands
        COMMAND_PATTERNS.put("bluetooth\\s+on\\s*karo*", CommandType.BLUETOOTH_ON);
        COMMAND_PATTERNS.put("bluetooth\\s+(?:off|band)\\s*karo*", CommandType.BLUETOOTH_OFF);
        
        // Flashlight commands
        COMMAND_PATTERNS.put("(?:flashlight|torch)\\s+(?:on|jalao|jalaao)\\s*karo*", CommandType.FLASHLIGHT_ON);
        COMMAND_PATTERNS.put("light\\s+on\\s+karo", CommandType.FLASHLIGHT_ON);
        COMMAND_PATTERNS.put("torch\\s+(?:off|band)\\s*karo*", CommandType.FLASHLIGHT_OFF);
        COMMAND_PATTERNS.put("(?:flashlight|light)\\s+off\\s+karo", CommandType.FLASHLIGHT_OFF);
        
        // Brightness commands
        COMMAND_PATTERNS.put("brightness\\s+badhao", CommandType.BRIGHTNESS_UP);
        COMMAND_PATTERNS.put("brightness\\s+kam\\s+karo", CommandType.BRIGHTNESS_DOWN);
        COMMAND_PATTERNS.put("screen\\s+light\\s+badhao", CommandType.BRIGHTNESS_UP);
        
        // Navigation commands
        COMMAND_PATTERNS.put("maps\\s+kholo", CommandType.OPEN_MAPS);
        COMMAND_PATTERNS.put("open\\s+maps", CommandType.OPEN_MAPS);
        COMMAND_PATTERNS.put("(.*)\\s+ka\\s+route\\s+batao", CommandType.NAVIGATE_TO);
        COMMAND_PATTERNS.put("navigate\\s+to\\s+(.*)", CommandType.NAVIGATE_TO);
        COMMAND_PATTERNS.put("(.*)\\s+kaise\\s+jaun", CommandType.NAVIGATE_TO);
        
        // Location commands
        COMMAND_PATTERNS.put("mera\\s+location\\s+batao", CommandType.GET_LOCATION);
        COMMAND_PATTERNS.put("where\\s+am\\s+i", CommandType.GET_LOCATION);
        COMMAND_PATTERNS.put("nearby\\s+(.*)\\s+dhoondo", CommandType.FIND_NEARBY);
        COMMAND_PATTERNS.put("paas\\s+mein\\s+(.*)", CommandType.FIND_NEARBY);
        
        // Camera commands
        COMMAND_PATTERNS.put("camera\\s+kholo", CommandType.OPEN_CAMERA);
        COMMAND_PATTERNS.put("open\\s+camera", CommandType.OPEN_CAMERA);
        COMMAND_PATTERNS.put("photo\\s+lo", CommandType.OPEN_CAMERA);
        COMMAND_PATTERNS.put("selfie\\s+lo", CommandType.OPEN_CAMERA_SELFIE);
        
        // Time/Date
        COMMAND_PATTERNS.put("time\\s+batao", CommandType.TELL_TIME);
        COMMAND_PATTERNS.put("kitne\\s+baje\\s+hain", CommandType.TELL_TIME);
        COMMAND_PATTERNS.put("aaj\\s+ki\\s+date\\s+batao", CommandType.TELL_DATE);
        COMMAND_PATTERNS.put("what\\s+time\\s+is\\s+it", CommandType.TELL_TIME);
        COMMAND_PATTERNS.put("today'?s?\\s+date", CommandType.TELL_DATE);
        
        // Battery
        COMMAND_PATTERNS.put("battery\\s+kitni\\s+bachi\\s+hain", CommandType.BATTERY_STATUS);
        COMMAND_PATTERNS.put("battery\\s+status", CommandType.BATTERY_STATUS);
        COMMAND_PATTERNS.put("charge\\s+kitna\\s+hain", CommandType.BATTERY_STATUS);
    }

    private enum CommandType {
        OPEN_APP, CLOSE_APP,
        CALL_CONTACT, SEND_SMS,
        PLAY_MUSIC, PAUSE_MUSIC, NEXT_TRACK, PREVIOUS_TRACK,
        VOLUME_UP, VOLUME_DOWN, MUTE, SILENT,
        WIFI_ON, WIFI_OFF,
        BLUETOOTH_ON, BLUETOOTH_OFF,
        FLASHLIGHT_ON, FLASHLIGHT_OFF,
        BRIGHTNESS_UP, BRIGHTNESS_DOWN,
        OPEN_MAPS, NAVIGATE_TO, GET_LOCATION, FIND_NEARBY,
        OPEN_CAMERA, OPEN_CAMERA_SELFIE,
        TELL_TIME, TELL_DATE, BATTERY_STATUS
    }

    public interface CommandCallback {
        void onSuccess(String message);
        void onError(String error);
        void onPartialResult(String partial);
    }

    public VoiceCommandProcessor(Context context) {
        this.appController = new AppController(context);
        this.contactManager = new ContactManager(context);
        this.mediaController = new MediaController(context);
        this.navigationController = new NavigationController(context);
        this.systemController = new SystemController(context);
    }

    /**
     * Process voice command
     */
    public void processCommand(String voiceInput, CommandCallback callback) {
        String input = voiceInput.toLowerCase().trim();
        Log.d(TAG, "Processing: " + input);

        // Find matching command
        for (Map.Entry<String, CommandType> entry : COMMAND_PATTERNS.entrySet()) {
            Pattern pattern = Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(input);
            
            if (matcher.find()) {
                CommandType type = entry.getValue();
                String param = matcher.groupCount() > 0 ? matcher.group(1) : null;
                
                executeCommand(type, param, callback);
                return;
            }
        }

        // No match found - try intelligent parsing
        String result = intelligentParse(input, callback);
        if (result != null) {
            return;
        }

        callback.onError("Samajh nahi aaya. Dobara boliye.");
    }

    /**
     * Execute command based on type
     */
    private void executeCommand(CommandType type, String param, CommandCallback callback) {
        switch (type) {
            // App control
            case OPEN_APP:
                appController.openApp(param, new AppController.AppCallback() {
                    @Override
                    public void onSuccess(String message) { callback.onSuccess(message); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                    @Override
                    public void onAppList(java.util.List<AppController.AppInfo> apps) {}
                });
                break;
                
            case CLOSE_APP:
                appController.closeApp(param, new AppController.AppCallback() {
                    @Override
                    public void onSuccess(String message) { callback.onSuccess(message); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                    @Override
                    public void onAppList(java.util.List<AppController.AppInfo> apps) {}
                });
                break;
                
            // Calling
            case CALL_CONTACT:
                contactManager.callContact(param, new ContactManager.ContactCallback() {
                    @Override
                    public void onSuccess(String message) { callback.onSuccess(message); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                    @Override
                    public void onContactList(java.util.List<ContactManager.ContactInfo> contacts) {}
                    @Override
                    public void onContactFound(ContactManager.ContactInfo contact) {}
                });
                break;
                
            // SMS
            case SEND_SMS:
                // Need to parse message content
                callback.onPartialResult("Kya message bhejna hai?");
                break;
                
            // Media
            case PLAY_MUSIC:
                mediaController.play(callback::onSuccess);
                break;
                
            case PAUSE_MUSIC:
                mediaController.pause(callback::onSuccess);
                break;
                
            case NEXT_TRACK:
                mediaController.nextTrack(callback::onSuccess);
                break;
                
            case PREVIOUS_TRACK:
                mediaController.previousTrack(callback::onSuccess);
                break;
                
            // Volume
            case VOLUME_UP:
                mediaController.volumeUp(callback::onSuccess);
                break;
                
            case VOLUME_DOWN:
                mediaController.volumeDown(callback::onSuccess);
                break;
                
            case MUTE:
            case SILENT:
                systemController.setSilentMode(callback::onSuccess);
                break;
                
            // WiFi
            case WIFI_ON:
                systemController.enableWiFi(callback::onSuccess);
                break;
                
            case WIFI_OFF:
                systemController.disableWiFi(callback::onSuccess);
                break;
                
            // Bluetooth
            case BLUETOOTH_ON:
                systemController.enableBluetooth(callback::onSuccess);
                break;
                
            case BLUETOOTH_OFF:
                systemController.disableBluetooth(callback::onSuccess);
                break;
                
            // Flashlight
            case FLASHLIGHT_ON:
                systemController.enableFlashlight(callback::onSuccess);
                break;
                
            case FLASHLIGHT_OFF:
                systemController.disableFlashlight(callback::onSuccess);
                break;
                
            // Brightness
            case BRIGHTNESS_UP:
                systemController.increaseBrightness(callback::onSuccess);
                break;
                
            case BRIGHTNESS_DOWN:
                systemController.decreaseBrightness(callback::onSuccess);
                break;
                
            // Navigation
            case OPEN_MAPS:
                navigationController.openMaps(callback::onSuccess);
                break;
                
            case NAVIGATE_TO:
                navigationController.navigateTo(param, callback::onSuccess);
                break;
                
            case GET_LOCATION:
                navigationController.getCurrentLocation(new NavigationController.NavigationCallback() {
                    @Override
                    public void onSuccess(String message) { callback.onSuccess(message); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                    @Override
                    public void onLocation(NavigationController.LocationInfo location) {
                        callback.onSuccess("Aap " + location.address + " par hain");
                    }
                    @Override
                    public void onPlaces(java.util.List<NavigationController.PlaceInfo> places) {}
                });
                break;
                
            case FIND_NEARBY:
                navigationController.findNearby(param, callback::onSuccess);
                break;
                
            // Camera
            case OPEN_CAMERA:
                openCamera(false, callback);
                break;
                
            case OPEN_CAMERA_SELFIE:
                openCamera(true, callback);
                break;
                
            // Time/Date
            case TELL_TIME:
                String time = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    .format(new java.util.Date());
                callback.onSuccess("Abhi " + time + " baje hain");
                break;
                
            case TELL_DATE:
                String date = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", new java.util.Locale("hi", "IN"))
                    .format(new java.util.Date());
                callback.onSuccess("Aaj " + date + " hai");
                break;
                
            // Battery
            case BATTERY_STATUS:
                SystemController.SystemStatus status = systemController.getSystemStatus();
                callback.onSuccess("Battery " + status.batteryLevel + "% bachi hai");
                break;
        }
    }

    /**
     * Intelligent parsing for unrecognized commands
     */
    private String intelligentParse(String input, CommandCallback callback) {
        // Check for common keywords
        if (input.contains("kholo") || input.contains("open")) {
            String app = extractApp(input);
            if (app != null) {
                executeCommand(CommandType.OPEN_APP, app, callback);
                return app;
            }
        }
        
        if (input.contains("call") || input.contains("phone")) {
            String contact = extractContact(input);
            if (contact != null) {
                executeCommand(CommandType.CALL_CONTACT, contact, callback);
                return contact;
            }
        }
        
        if (input.contains("torch") || input.contains("flashlight") || input.contains("light")) {
            if (input.contains("on") || input.contains("jalao")) {
                executeCommand(CommandType.FLASHLIGHT_ON, null, callback);
                return "torch on";
            } else if (input.contains("off") || input.contains("band")) {
                executeCommand(CommandType.FLASHLIGHT_OFF, null, callback);
                return "torch off";
            }
        }
        
        return null;
    }

    /**
     * Extract app name from command
     */
    private String extractApp(String input) {
        String[] words = input.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals("kholo") && i > 0) {
                return words[i - 1];
            }
            if (words[i].equals("open") && i < words.length - 1) {
                return words[i + 1];
            }
        }
        return null;
    }

    /**
     * Extract contact name from command
     */
    private String extractContact(String input) {
        String[] words = input.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if ((words[i].equals("ko") || words[i].equals("call")) && i > 0) {
                return words[i - 1];
            }
        }
        return null;
    }

    /**
     * Open camera
     */
    private void openCamera(boolean selfie, CommandCallback callback) {
        try {
            android.content.Intent intent = new android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (selfie) {
                intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
            }
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            callback.onSuccess("Camera khul raha hai!");
        } catch (Exception e) {
            callback.onError("Camera nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Send SMS with message
     */
    public void sendSMS(String contact, String message, CommandCallback callback) {
        contactManager.sendSMS(contact, message, new ContactManager.ContactCallback() {
            @Override
            public void onSuccess(String msg) { callback.onSuccess(msg); }
            @Override
            public void onError(String error) { callback.onError(error); }
            @Override
            public void onContactList(java.util.List<ContactManager.ContactInfo> contacts) {}
            @Override
            public void onContactFound(ContactManager.ContactInfo c) {}
        });
    }

    /**
     * Get all controllers
     */
    public AppController getAppController() { return appController; }
    public ContactManager getContactManager() { return contactManager; }
    public MediaController getMediaController() { return mediaController; }
    public NavigationController getNavigationController() { return navigationController; }
    public SystemController getSystemController() { return systemController; }
}
