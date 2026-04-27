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
 * - SMS/WhatsApp: "Mummy ko message bhejo", "Raj ko WhatsApp bhejo"
 * - Media: "Music play karo", "Volume badhao", "Next song", "Play Arijit Singh"
 * - System: "WiFi on karo", "Flashlight jalao", "Bluetooth off karo"
 * - Navigation: "Maps kholo", "Delhi ka route batao"
 * - Camera: "Photo le lo", "Selfie click karo", "Flash on karo"
 * - Alarms: "Subah 6 baje alarm laga do"
 * - Reminders: "Mujhe yaad dilao meeting ka"
 * - Notifications: "Notifications padho"
 * - Settings: WiFi, Bluetooth, Mobile Data, Hotspot, Airplane mode, Rotation
 * - Volume control: "Volume badhao", "Silent mode on karo"
 * - Screen control: "Screen bright karo", "Screen dim karo"
 * - Battery management: "Battery saver on karo"
 */
public class VoiceCommandProcessor {

    private static final String TAG = "VoiceCommandProcessor";

    // Controllers
    private final CompleteVoiceController completeVoiceController;
    private final AppController appController;
    private final ContactManager contactManager;
    private final MediaController mediaController;
    private final NavigationController navigationController;
    private final SystemController systemController;
    private final CommunicationManager communicationManager;
    private final AppLauncher appLauncher;

    // Command patterns
    private static final Map<String, CommandType> COMMAND_PATTERNS = new HashMap<>();
    
    static {
        // ==================== APP COMMANDS ====================
        // Open app commands - Hindi
        COMMAND_PATTERNS.put("(.*)\\s+kholo", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("(.*)\\s+chalao", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("(.*)\\s+on karo", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("(.*)\\s+start karo", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("(.*)\\s+run karo", CommandType.OPEN_APP);
        
        // Open app commands - English
        COMMAND_PATTERNS.put("open\\s+(.*)", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("start\\s+(.*)", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("launch\\s+(.*)", CommandType.OPEN_APP);
        COMMAND_PATTERNS.put("run\\s+(.*)", CommandType.OPEN_APP);
        
        // Close app
        COMMAND_PATTERNS.put("(.*)\\s+band\\s+karo", CommandType.CLOSE_APP);
        COMMAND_PATTERNS.put("close\\s+(.*)", CommandType.CLOSE_APP);
        COMMAND_PATTERNS.put("kill\\s+(.*)", CommandType.CLOSE_APP);
        
        // ==================== CALL COMMANDS ====================
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+call\\s+karo", CommandType.CALL_CONTACT);
        COMMAND_PATTERNS.put("call\\s+(.*)", CommandType.CALL_CONTACT);
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+phone\\s+karo", CommandType.CALL_CONTACT);
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+call\\s+kijiye", CommandType.CALL_CONTACT);
        
        // ==================== SMS/MESSAGE COMMANDS ====================
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+(?:sms|message)\\s+bhejo", CommandType.SEND_SMS);
        COMMAND_PATTERNS.put("send\\s+(?:sms|message)\\s+to\\s+(.*)", CommandType.SEND_SMS);
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+msg\\s+karo", CommandType.SEND_SMS);
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+message\\s+karo", CommandType.SEND_SMS);
        
        // ==================== WHATSAPP COMMANDS ====================
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+whatsapp\\s+(?:bhejo|message)\\s*", CommandType.SEND_WHATSAPP);
        COMMAND_PATTERNS.put("whatsapp\\s+(?:message\\s+)?(?:to\\s+)?(.*)", CommandType.SEND_WHATSAPP);
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+wa\\s+bhejo", CommandType.SEND_WHATSAPP);
        
        // ==================== EMAIL COMMANDS ====================
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+email\\s+bhejo", CommandType.SEND_EMAIL);
        COMMAND_PATTERNS.put("send\\s+email\\s+to\\s+(.*)", CommandType.SEND_EMAIL);
        COMMAND_PATTERNS.put("(.*)\\s+ko\\s+mail\\s+bhejo", CommandType.SEND_EMAIL);
        
        // ==================== MEDIA COMMANDS ====================
        // Play music
        COMMAND_PATTERNS.put("music\\s+play\\s+karo", CommandType.PLAY_MUSIC);
        COMMAND_PATTERNS.put("play\\s+music", CommandType.PLAY_MUSIC);
        COMMAND_PATTERNS.put("gaana\\s+(?:bajao|chalao)", CommandType.PLAY_MUSIC);
        COMMAND_PATTERNS.put("gaane\\s+chalao", CommandType.PLAY_MUSIC);
        COMMAND_PATTERNS.put("song\\s+play\\s+karo", CommandType.PLAY_MUSIC);
        
        // Play specific artist/song
        COMMAND_PATTERNS.put("play\\s+(.*)\\s+songs?", CommandType.PLAY_ARTIST);
        COMMAND_PATTERNS.put("(.*)\\s+ke\\s+gaane\\s+bajao", CommandType.PLAY_ARTIST);
        COMMAND_PATTERNS.put("(.*)\\s+ka\\s+gaana\\s+bajao", CommandType.PLAY_ARTIST);
        COMMAND_PATTERNS.put("(.*)\\s+bajao", CommandType.PLAY_SONG);
        
        // Pause/Stop
        COMMAND_PATTERNS.put("pause\\s+karo", CommandType.PAUSE_MUSIC);
        COMMAND_PATTERNS.put("rukho", CommandType.PAUSE_MUSIC);
        COMMAND_PATTERNS.put("stop\\s+karo", CommandType.STOP_MUSIC);
        COMMAND_PATTERNS.put("band\\s+karo", CommandType.STOP_MUSIC);
        
        // Next/Previous
        COMMAND_PATTERNS.put("next\\s+(?:song|track)", CommandType.NEXT_TRACK);
        COMMAND_PATTERNS.put("agli\\s+(?:gana|song)", CommandType.NEXT_TRACK);
        COMMAND_PATTERNS.put("aage\\s+wala\\s+gaana", CommandType.NEXT_TRACK);
        COMMAND_PATTERNS.put("previous\\s+(?:song|track)", CommandType.PREVIOUS_TRACK);
        COMMAND_PATTERNS.put("pichli\\s+(?:gana|song)", CommandType.PREVIOUS_TRACK);
        COMMAND_PATTERNS.put("peeche\\s+wala\\s+gaana", CommandType.PREVIOUS_TRACK);
        
        // Shuffle
        COMMAND_PATTERNS.put("shuffle\\s+karo", CommandType.SHUFFLE);
        COMMAND_PATTERNS.put("random\\s+gaane", CommandType.SHUFFLE);
        
        // ==================== VOLUME COMMANDS ====================
        COMMAND_PATTERNS.put("volume\\s+badhao", CommandType.VOLUME_UP);
        COMMAND_PATTERNS.put("volume\\s+up", CommandType.VOLUME_UP);
        COMMAND_PATTERNS.put("aawaz\\s+badhao", CommandType.VOLUME_UP);
        COMMAND_PATTERNS.put("volume\\s+kam\\s+karo", CommandType.VOLUME_DOWN);
        COMMAND_PATTERNS.put("volume\\s+down", CommandType.VOLUME_DOWN);
        COMMAND_PATTERNS.put("aawaz\\s+kam\\s+karo", CommandType.VOLUME_DOWN);
        COMMAND_PATTERNS.put("volume\\s+(\\d+)(?:\\s*%|\\s*percent)?", CommandType.SET_VOLUME);
        COMMAND_PATTERNS.put("mute\\s+karo", CommandType.MUTE);
        COMMAND_PATTERNS.put("silent\\s+(?:mode\\s+)?(?:on\\s+)?karo", CommandType.SILENT);
        COMMAND_PATTERNS.put("vibrate\\s+(?:mode\\s+)?(?:on\\s+)?karo", CommandType.VIBRATE);
        COMMAND_PATTERNS.put("normal\\s+(?:mode\\s+)?(?:on\\s+)?karo", CommandType.NORMAL_MODE);
        
        // ==================== WIFI COMMANDS ====================
        COMMAND_PATTERNS.put("wifi\\s+(?:on|chalao)\\s*karo*", CommandType.WIFI_ON);
        COMMAND_PATTERNS.put("turn\\s+on\\s+wifi", CommandType.WIFI_ON);
        COMMAND_PATTERNS.put("wifi\\s+(?:off|band)\\s*karo*", CommandType.WIFI_OFF);
        COMMAND_PATTERNS.put("turn\\s+off\\s+wifi", CommandType.WIFI_OFF);
        
        // ==================== BLUETOOTH COMMANDS ====================
        COMMAND_PATTERNS.put("bluetooth\\s+(?:on|chalao)\\s*karo*", CommandType.BLUETOOTH_ON);
        COMMAND_PATTERNS.put("turn\\s+on\\s+bluetooth", CommandType.BLUETOOTH_ON);
        COMMAND_PATTERNS.put("bluetooth\\s+(?:off|band)\\s*karo*", CommandType.BLUETOOTH_OFF);
        COMMAND_PATTERNS.put("turn\\s+off\\s+bluetooth", CommandType.BLUETOOTH_OFF);
        
        // ==================== FLASHLIGHT COMMANDS ====================
        COMMAND_PATTERNS.put("(?:flashlight|torch)\\s+(?:on|jalao|jalaao)\\s*karo*", CommandType.FLASHLIGHT_ON);
        COMMAND_PATTERNS.put("light\\s+on\\s+karo", CommandType.FLASHLIGHT_ON);
        COMMAND_PATTERNS.put("flash\\s+on\\s+karo", CommandType.FLASHLIGHT_ON);
        COMMAND_PATTERNS.put("torch\\s+(?:off|band)\\s*karo*", CommandType.FLASHLIGHT_OFF);
        COMMAND_PATTERNS.put("(?:flashlight|light)\\s+off\\s+karo", CommandType.FLASHLIGHT_OFF);
        COMMAND_PATTERNS.put("flash\\s+off\\s+karo", CommandType.FLASHLIGHT_OFF);
        
        // ==================== BRIGHTNESS COMMANDS ====================
        COMMAND_PATTERNS.put("brightness\\s+badhao", CommandType.BRIGHTNESS_UP);
        COMMAND_PATTERNS.put("brightness\\s+kam\\s+karo", CommandType.BRIGHTNESS_DOWN);
        COMMAND_PATTERNS.put("screen\\s+(?:light\\s+)?badhao", CommandType.BRIGHTNESS_UP);
        COMMAND_PATTERNS.put("screen\\s+(?:light\\s+)?kam\\s+karo", CommandType.BRIGHTNESS_DOWN);
        COMMAND_PATTERNS.put("screen\\s+bright\\s+karo", CommandType.BRIGHTNESS_UP);
        COMMAND_PATTERNS.put("screen\\s+dim\\s+karo", CommandType.BRIGHTNESS_DOWN);
        
        // ==================== NAVIGATION COMMANDS ====================
        COMMAND_PATTERNS.put("maps\\s+kholo", CommandType.OPEN_MAPS);
        COMMAND_PATTERNS.put("open\\s+maps", CommandType.OPEN_MAPS);
        COMMAND_PATTERNS.put("(.*)\\s+ka\\s+route\\s+batao", CommandType.NAVIGATE_TO);
        COMMAND_PATTERNS.put("navigate\\s+to\\s+(.*)", CommandType.NAVIGATE_TO);
        COMMAND_PATTERNS.put("(.*)\\s+kaise\\s+jaun", CommandType.NAVIGATE_TO);
        COMMAND_PATTERNS.put("(.*)\\s+keliye\\s+navigation\\s+(?:shuru\\s+)?karo", CommandType.NAVIGATE_TO);
        
        // Location
        COMMAND_PATTERNS.put("mera\\s+location\\s+batao", CommandType.GET_LOCATION);
        COMMAND_PATTERNS.put("where\\s+am\\s+i", CommandType.GET_LOCATION);
        COMMAND_PATTERNS.put("main\\s+kahan\\s+hun", CommandType.GET_LOCATION);
        
        // Nearby
        COMMAND_PATTERNS.put("nearby\\s+(.*)\\s+dhoondo", CommandType.FIND_NEARBY);
        COMMAND_PATTERNS.put("paas\\s+mein\\s+(.*)", CommandType.FIND_NEARBY);
        COMMAND_PATTERNS.put("nazdeek\\s+mein\\s+(.*)", CommandType.FIND_NEARBY);
        
        // ==================== CAMERA COMMANDS ====================
        COMMAND_PATTERNS.put("camera\\s+kholo", CommandType.OPEN_CAMERA);
        COMMAND_PATTERNS.put("open\\s+camera", CommandType.OPEN_CAMERA);
        COMMAND_PATTERNS.put("photo\\s+lo", CommandType.TAKE_PHOTO);
        COMMAND_PATTERNS.put("photo\\s+le\\s+lo", CommandType.TAKE_PHOTO);
        COMMAND_PATTERNS.put("click\\s+photo", CommandType.TAKE_PHOTO);
        COMMAND_PATTERNS.put("picture\\s+lo", CommandType.TAKE_PHOTO);
        COMMAND_PATTERNS.put("selfie\\s+(?:lo|click\\s+karo|le\\s+lo)", CommandType.TAKE_SELFIE);
        COMMAND_PATTERNS.put("front\\s+camera\\s+kholo", CommandType.TAKE_SELFIE);
        
        // ==================== ALARM COMMANDS ====================
        COMMAND_PATTERNS.put("(?:subah|morning)\\s+(\\d+)\\s+baje\\s+alarm\\s+laga\\s*(?:do|dalo)?", CommandType.SET_ALARM);
        COMMAND_PATTERNS.put("(\\d+)\\s*baje\\s+alarm\\s+(?:laga|set)\\s*(?:do|dalo|karo)?", CommandType.SET_ALARM);
        COMMAND_PATTERNS.put("set\\s+alarm\\s+(?:for\\s+)?(.*)", CommandType.SET_ALARM);
        COMMAND_PATTERNS.put("alarm\\s+laga\\s+do\\s+(.+)", CommandType.SET_ALARM);
        
        // Timer
        COMMAND_PATTERNS.put("(\\d+)\\s*(?:minute|second|hour)\\s*(?:ka\\s+)?timer\\s+(?:set\\s+)?karo", CommandType.SET_TIMER);
        COMMAND_PATTERNS.put("set\\s+timer\\s+(?:for\\s+)?(.*)", CommandType.SET_TIMER);
        
        // Reminder
        COMMAND_PATTERNS.put("mujhe\\s+(.+)\\s+yaad\\s+dilao", CommandType.SET_REMINDER);
        COMMAND_PATTERNS.put("remind\\s+me\\s+(?:to\\s+)?(.*)", CommandType.SET_REMINDER);
        COMMAND_PATTERNS.put("reminder\\s+laga\\s+do\\s+(.+)", CommandType.SET_REMINDER);
        
        // ==================== NOTIFICATION COMMANDS ====================
        COMMAND_PATTERNS.put("notifications?\\s+padho", CommandType.READ_NOTIFICATIONS);
        COMMAND_PATTERNS.put("messages?\\s+padho", CommandType.READ_MESSAGES);
        COMMAND_PATTERNS.put("saari\\s+notifications?\\s+padho", CommandType.READ_NOTIFICATIONS);
        COMMAND_PATTERNS.put("clear\\s+(?:all\\s+)?notifications?", CommandType.CLEAR_NOTIFICATIONS);
        
        // ==================== SETTINGS COMMANDS ====================
        COMMAND_PATTERNS.put("settings\\s+kholo", CommandType.OPEN_SETTINGS);
        COMMAND_PATTERNS.put("open\\s+settings", CommandType.OPEN_SETTINGS);
        COMMAND_PATTERNS.put("setting\\s+mein\\s+jao", CommandType.OPEN_SETTINGS);
        
        // Mobile data
        COMMAND_PATTERNS.put("mobile\\s+data\\s+(?:on|off)\\s*karo*", CommandType.MOBILE_DATA);
        COMMAND_PATTERNS.put("data\\s+(?:on|off)\\s*karo*", CommandType.MOBILE_DATA);
        
        // Hotspot
        COMMAND_PATTERNS.put("hotspot\\s+(?:on|off)\\s*karo*", CommandType.HOTSPOT);
        COMMAND_PATTERNS.put("tethering\\s+(?:on|off)\\s*karo*", CommandType.HOTSPOT);
        
        // Airplane mode
        COMMAND_PATTERNS.put("airplane\\s+mode\\s+(?:on|off)\\s*karo*", CommandType.AIRPLANE_MODE);
        COMMAND_PATTERNS.put("flight\\s+mode\\s+(?:on|off)\\s*karo*", CommandType.AIRPLANE_MODE);
        COMMAND_PATTERNS.put("havai\\s+jahaz\\s+mode\\s+(?:on|off)\\s*karo*", CommandType.AIRPLANE_MODE);
        
        // Rotation
        COMMAND_PATTERNS.put("(?:auto\\s+)?rotation\\s+(?:on|off)\\s*karo*", CommandType.ROTATION);
        COMMAND_PATTERNS.put("rotate\\s+(?:on|off)\\s*karo*", CommandType.ROTATION);
        
        // DND
        COMMAND_PATTERNS.put("do\\s+not\\s+disturb\\s+(?:on|off)\\s*karo*", CommandType.DND);
        COMMAND_PATTERNS.put("dnd\\s+(?:on|off)\\s*karo*", CommandType.DND);
        
        // Battery saver
        COMMAND_PATTERNS.put("battery\\s+saver\\s+(?:on|off)\\s*karo*", CommandType.BATTERY_SAVER);
        COMMAND_PATTERNS.put("power\\s+saver\\s+(?:on|off)\\s*karo*", CommandType.BATTERY_SAVER);
        
        // ==================== TIME/DATE ====================
        COMMAND_PATTERNS.put("time\\s+batao", CommandType.TELL_TIME);
        COMMAND_PATTERNS.put("kitne\\s+baje\\s+hain", CommandType.TELL_TIME);
        COMMAND_PATTERNS.put("what\\s+time\\s+is\\s+it", CommandType.TELL_TIME);
        COMMAND_PATTERNS.put("aaj\\s+ki\\s+date\\s+batao", CommandType.TELL_DATE);
        COMMAND_PATTERNS.put("today'?s?\\s+date", CommandType.TELL_DATE);
        COMMAND_PATTERNS.put("tarikh\\s+batao", CommandType.TELL_DATE);
        
        // ==================== BATTERY ====================
        COMMAND_PATTERNS.put("battery\\s+kitni\\s+bachi\\s+hain", CommandType.BATTERY_STATUS);
        COMMAND_PATTERNS.put("battery\\s+status", CommandType.BATTERY_STATUS);
        COMMAND_PATTERNS.put("charge\\s+kitna\\s+hain", CommandType.BATTERY_STATUS);
        COMMAND_PATTERNS.put("battery\\s+batao", CommandType.BATTERY_STATUS);
        
        // ==================== YOUTUBE ====================
        COMMAND_PATTERNS.put("youtube\\s+pe\\s+(.+)\\s+dhoondo", CommandType.SEARCH_YOUTUBE);
        COMMAND_PATTERNS.put("youtube\\s+pe\\s+(.+)\\s+search\\s+karo", CommandType.SEARCH_YOUTUBE);
        COMMAND_PATTERNS.put("(.+)\\s+youtube\\s+pe\\s+bajao", CommandType.SEARCH_YOUTUBE);
        
        // ==================== SPOTIFY ====================
        COMMAND_PATTERNS.put("spotify\\s+(?:kholo|open)", CommandType.OPEN_SPOTIFY);
        COMMAND_PATTERNS.put("spotify\\s+pe\\s+(.+)\\s+bajao", CommandType.PLAY_SPOTIFY);
        
        // ==================== GMAIL ====================
        COMMAND_PATTERNS.put("gmail\\s+kholo", CommandType.OPEN_GMAIL);
        COMMAND_PATTERNS.put("email\\s+check\\s+karo", CommandType.OPEN_GMAIL);
    }

    private enum CommandType {
        OPEN_APP, CLOSE_APP,
        CALL_CONTACT, SEND_SMS, SEND_WHATSAPP, SEND_EMAIL,
        PLAY_MUSIC, PAUSE_MUSIC, STOP_MUSIC, NEXT_TRACK, PREVIOUS_TRACK,
        PLAY_ARTIST, PLAY_SONG, SHUFFLE,
        VOLUME_UP, VOLUME_DOWN, SET_VOLUME, MUTE, SILENT, VIBRATE, NORMAL_MODE,
        WIFI_ON, WIFI_OFF,
        BLUETOOTH_ON, BLUETOOTH_OFF,
        FLASHLIGHT_ON, FLASHLIGHT_OFF,
        BRIGHTNESS_UP, BRIGHTNESS_DOWN,
        OPEN_MAPS, NAVIGATE_TO, GET_LOCATION, FIND_NEARBY,
        OPEN_CAMERA, TAKE_PHOTO, TAKE_SELFIE,
        SET_ALARM, SET_TIMER, SET_REMINDER,
        READ_NOTIFICATIONS, READ_MESSAGES, CLEAR_NOTIFICATIONS,
        OPEN_SETTINGS, MOBILE_DATA, HOTSPOT, AIRPLANE_MODE, ROTATION, DND, BATTERY_SAVER,
        TELL_TIME, TELL_DATE, BATTERY_STATUS,
        SEARCH_YOUTUBE, OPEN_SPOTIFY, PLAY_SPOTIFY, OPEN_GMAIL
    }

    public interface CommandCallback {
        void onSuccess(String message);
        void onError(String error);
        void onPartialResult(String partial);
    }

    public VoiceCommandProcessor(Context context) {
        this.completeVoiceController = new CompleteVoiceController(context);
        this.appController = new AppController(context);
        this.contactManager = new ContactManager(context);
        this.mediaController = new MediaController(context);
        this.navigationController = new NavigationController(context);
        this.systemController = new SystemController(context);
        this.communicationManager = new CommunicationManager(context);
        this.appLauncher = new AppLauncher(context);
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
                
                executeCommand(type, param, input, callback);
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
    private void executeCommand(CommandType type, String param, String originalInput, CommandCallback callback) {
        switch (type) {
            // ==================== APP CONTROL ====================
            case OPEN_APP:
                completeVoiceController.openApp(param, new CompleteVoiceController.VoiceControlCallback() {
                    @Override
                    public void onSuccess(String message) { callback.onSuccess(message); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                    @Override
                    public void onPartialResult(String partial) { callback.onPartialResult(partial); }
                    @Override
                    public void onRequiresConfirmation(String action, String confirmationMessage) {
                        callback.onPartialResult(confirmationMessage);
                    }
                });
                break;
                
            case CLOSE_APP:
                completeVoiceController.closeApp(param != null ? param : "", callback::onSuccess);
                break;
                
            // ==================== COMMUNICATION ====================
            case CALL_CONTACT:
                completeVoiceController.makeCall(param, new CompleteVoiceController.VoiceControlCallback() {
                    @Override
                    public void onSuccess(String message) { callback.onSuccess(message); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                    @Override
                    public void onPartialResult(String partial) { callback.onPartialResult(partial); }
                    @Override
                    public void onRequiresConfirmation(String action, String confirmationMessage) {
                        callback.onPartialResult(confirmationMessage);
                    }
                });
                break;
                
            case SEND_SMS:
                callback.onPartialResult("Kya message bhejna hai " + param + " ko?");
                break;
                
            case SEND_WHATSAPP:
                completeVoiceController.sendWhatsApp(param, null, new CompleteVoiceController.VoiceControlCallback() {
                    @Override
                    public void onSuccess(String message) { callback.onSuccess(message); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                    @Override
                    public void onPartialResult(String partial) { callback.onPartialResult(partial); }
                    @Override
                    public void onRequiresConfirmation(String action, String confirmationMessage) {
                        callback.onPartialResult(confirmationMessage);
                    }
                });
                break;
                
            case SEND_EMAIL:
                completeVoiceController.sendEmail(param, null, null, new CompleteVoiceController.VoiceControlCallback() {
                    @Override
                    public void onSuccess(String message) { callback.onSuccess(message); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                    @Override
                    public void onPartialResult(String partial) { callback.onPartialResult(partial); }
                    @Override
                    public void onRequiresConfirmation(String action, String confirmationMessage) {
                        callback.onPartialResult(confirmationMessage);
                    }
                });
                break;
                
            // ==================== MEDIA ====================
            case PLAY_MUSIC:
                mediaController.play(callback::onSuccess);
                break;
                
            case PLAY_ARTIST:
            case PLAY_SONG:
                mediaController.smartPlay(param, callback::onSuccess);
                break;
                
            case PAUSE_MUSIC:
                mediaController.pause(callback::onSuccess);
                break;
                
            case STOP_MUSIC:
                mediaController.stop(callback::onSuccess);
                break;
                
            case NEXT_TRACK:
                mediaController.nextTrack(callback::onSuccess);
                break;
                
            case PREVIOUS_TRACK:
                mediaController.previousTrack(callback::onSuccess);
                break;
                
            case SHUFFLE:
                mediaController.playShuffle(callback::onSuccess);
                break;
                
            // ==================== VOLUME ====================
            case VOLUME_UP:
                mediaController.volumeUp(callback::onSuccess);
                break;
                
            case VOLUME_DOWN:
                mediaController.volumeDown(callback::onSuccess);
                break;
                
            case SET_VOLUME:
                try {
                    int vol = Integer.parseInt(param.replaceAll("[^0-9]", ""));
                    mediaController.setVolume(vol, callback::onSuccess);
                } catch (NumberFormatException e) {
                    mediaController.volumeUp(callback::onSuccess);
                }
                break;
                
            case MUTE:
            case SILENT:
                systemController.setSilentMode(callback::onSuccess);
                break;
                
            case VIBRATE:
                systemController.setVibrateMode(callback::onSuccess);
                break;
                
            case NORMAL_MODE:
                systemController.setNormalMode(callback::onSuccess);
                break;
                
            // ==================== WIFI ====================
            case WIFI_ON:
                completeVoiceController.controlWiFi(true, callback::onSuccess);
                break;
                
            case WIFI_OFF:
                completeVoiceController.controlWiFi(false, callback::onSuccess);
                break;
                
            // ==================== BLUETOOTH ====================
            case BLUETOOTH_ON:
                completeVoiceController.controlBluetooth(true, callback::onSuccess);
                break;
                
            case BLUETOOTH_OFF:
                completeVoiceController.controlBluetooth(false, callback::onSuccess);
                break;
                
            // ==================== FLASHLIGHT ====================
            case FLASHLIGHT_ON:
                completeVoiceController.flashOn(callback::onSuccess);
                break;
                
            case FLASHLIGHT_OFF:
                completeVoiceController.flashOff(callback::onSuccess);
                break;
                
            // ==================== BRIGHTNESS ====================
            case BRIGHTNESS_UP:
                completeVoiceController.increaseBrightness(callback::onSuccess);
                break;
                
            case BRIGHTNESS_DOWN:
                completeVoiceController.decreaseBrightness(callback::onSuccess);
                break;
                
            // ==================== NAVIGATION ====================
            case OPEN_MAPS:
                completeVoiceController.openMaps(callback::onSuccess);
                break;
                
            case NAVIGATE_TO:
                completeVoiceController.startNavigation(param, callback::onSuccess);
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
                completeVoiceController.findNearby(param, callback::onSuccess);
                break;
                
            // ==================== CAMERA ====================
            case OPEN_CAMERA:
            case TAKE_PHOTO:
                completeVoiceController.takePhoto(callback::onSuccess);
                break;
                
            case TAKE_SELFIE:
                completeVoiceController.takeSelfie(callback::onSuccess);
                break;
                
            // ==================== ALARMS & REMINDERS ====================
            case SET_ALARM:
                completeVoiceController.setAlarm(param, callback::onSuccess);
                break;
                
            case SET_TIMER:
                completeVoiceController.setTimer(param, callback::onSuccess);
                break;
                
            case SET_REMINDER:
                completeVoiceController.setReminder(param, null, callback::onSuccess);
                break;
                
            // ==================== NOTIFICATIONS ====================
            case READ_NOTIFICATIONS:
            case READ_MESSAGES:
                completeVoiceController.readNotifications(callback::onSuccess);
                break;
                
            case CLEAR_NOTIFICATIONS:
                completeVoiceController.clearNotifications(callback::onSuccess);
                break;
                
            // ==================== SETTINGS ====================
            case OPEN_SETTINGS:
                completeVoiceController.openSettings(callback::onSuccess);
                break;
                
            case MOBILE_DATA:
                completeVoiceController.controlMobileData(callback::onSuccess);
                break;
                
            case HOTSPOT:
                completeVoiceController.controlHotspot(callback::onSuccess);
                break;
                
            case AIRPLANE_MODE:
                completeVoiceController.controlAirplaneMode(callback::onSuccess);
                break;
                
            case ROTATION:
                completeVoiceController.controlRotation(true, callback::onSuccess);
                break;
                
            case DND:
                completeVoiceController.enableDND(callback::onSuccess);
                break;
                
            case BATTERY_SAVER:
                completeVoiceController.enableBatterySaver(callback::onSuccess);
                break;
                
            // ==================== TIME/DATE ====================
            case TELL_TIME:
                completeVoiceController.tellTime(callback::onSuccess);
                break;
                
            case TELL_DATE:
                completeVoiceController.tellDate(callback::onSuccess);
                break;
                
            // ==================== BATTERY ====================
            case BATTERY_STATUS:
                completeVoiceController.getBatteryStatus(callback::onSuccess);
                break;
                
            // ==================== YOUTUBE ====================
            case SEARCH_YOUTUBE:
                completeVoiceController.searchYouTube(param, callback::onSuccess);
                break;
                
            // ==================== SPOTIFY ====================
            case OPEN_SPOTIFY:
                mediaController.openSpotify(callback::onSuccess);
                break;
                
            case PLAY_SPOTIFY:
                mediaController.playOnSpotify(param, callback::onSuccess);
                break;
                
            // ==================== GMAIL ====================
            case OPEN_GMAIL:
                communicationManager.openEmailApp(new CommunicationManager.CommunicationCallback() {
                    @Override
                    public void onSuccess(String message) { callback.onSuccess(message); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                    @Override
                    public void onContactFound(String name, String identifier) {}
                });
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
                executeCommand(CommandType.OPEN_APP, app, input, callback);
                return app;
            }
        }
        
        if (input.contains("call") || input.contains("phone")) {
            String contact = extractContact(input);
            if (contact != null) {
                executeCommand(CommandType.CALL_CONTACT, contact, input, callback);
                return contact;
            }
        }
        
        if (input.contains("torch") || input.contains("flashlight") || input.contains("flash")) {
            if (input.contains("on") || input.contains("jalao")) {
                executeCommand(CommandType.FLASHLIGHT_ON, null, input, callback);
                return "torch on";
            } else if (input.contains("off") || input.contains("band")) {
                executeCommand(CommandType.FLASHLIGHT_OFF, null, input, callback);
                return "torch off";
            }
        }
        
        if (input.contains("music") || input.contains("gaana") || input.contains("song")) {
            if (input.contains("play") || input.contains("bajao") || input.contains("chalao")) {
                executeCommand(CommandType.PLAY_MUSIC, null, input, callback);
                return "play music";
            }
        }
        
        if (input.contains("volume") || input.contains("aawaz")) {
            if (input.contains("badhao") || input.contains("up") || input.contains("high")) {
                executeCommand(CommandType.VOLUME_UP, null, input, callback);
                return "volume up";
            } else if (input.contains("kam") || input.contains("down") || input.contains("low")) {
                executeCommand(CommandType.VOLUME_DOWN, null, input, callback);
                return "volume down";
            }
        }
        
        if (input.contains("selfie") || input.contains("front camera")) {
            executeCommand(CommandType.TAKE_SELFIE, null, input, callback);
            return "selfie";
        }
        
        if (input.contains("photo") || input.contains("picture")) {
            executeCommand(CommandType.TAKE_PHOTO, null, input, callback);
            return "photo";
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
     * Send SMS with message
     */
    public void sendSMS(String contact, String message, CommandCallback callback) {
        completeVoiceController.sendSMS(contact, message, new CompleteVoiceController.VoiceControlCallback() {
            @Override
            public void onSuccess(String msg) { callback.onSuccess(msg); }
            @Override
            public void onError(String error) { callback.onError(error); }
            @Override
            public void onPartialResult(String partial) { callback.onPartialResult(partial); }
            @Override
            public void onRequiresConfirmation(String action, String confirmationMessage) {
                callback.onPartialResult(confirmationMessage);
            }
        });
    }

    /**
     * Get all controllers
     */
    public CompleteVoiceController getCompleteVoiceController() { return completeVoiceController; }
    public AppController getAppController() { return appController; }
    public ContactManager getContactManager() { return contactManager; }
    public MediaController getMediaController() { return mediaController; }
    public NavigationController getNavigationController() { return navigationController; }
    public SystemController getSystemController() { return systemController; }
    public CommunicationManager getCommunicationManager() { return communicationManager; }
    public AppLauncher getAppLauncher() { return appLauncher; }
}
