package com.rsassistant.ai;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.SmsManager;

import androidx.core.app.NotificationCompat;

import com.rsassistant.R;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SmartAssistantManager - Advanced AI Features
 * Auto-reply, Smart Suggestions, Emergency SOS, Location Share
 * Custom Commands, Performance Optimization
 */
public class SmartAssistantManager {

    private static final String PREF_NAME = "smart_assistant_prefs";
    private static final String KEY_AUTO_REPLY = "auto_reply_enabled";
    private static final String KEY_AUTO_REPLY_MSG = "auto_reply_message";
    private static final String KEY_EMERGENCY_CONTACT = "emergency_contact";
    private static final String KEY_CUSTOM_COMMANDS = "custom_commands";
    private static final String KEY_SOS_ENABLED = "sos_enabled";
    private static final String KEY_SMART_SUGGESTIONS = "smart_suggestions";

    private static final String CHANNEL_SOS = "sos_channel";
    private static final String CHANNEL_SMART = "smart_channel";

    private final Context context;
    private final SharedPreferences prefs;
    private final Map<String, String> customCommands;
    private final List<String> recentActions;

    public SmartAssistantManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.customCommands = new HashMap<>();
        this.recentActions = new ArrayList<>();

        loadCustomCommands();
        createNotificationChannels();
    }

    // ==================== AUTO REPLY ====================

    public void setAutoReply(boolean enabled, String message) {
        prefs.edit()
            .putBoolean(KEY_AUTO_REPLY, enabled)
            .putString(KEY_AUTO_REPLY_MSG, message)
            .apply();
    }

    public boolean isAutoReplyEnabled() {
        return prefs.getBoolean(KEY_AUTO_REPLY, false);
    }

    public String getAutoReplyMessage() {
        return prefs.getString(KEY_AUTO_REPLY_MSG,
            "मैं अभी उपलब्ध नहीं हूं। बाद में संपर्क करें। - RS Assistant");
    }

    public String getAutoReplyForContext(String context) {
        String lower = context.toLowerCase();

        // Context-aware auto replies
        if (lower.contains("meeting") || lower.contains("मीटिंग")) {
            return "मैं meeting में हूं। 1 घंटे बाद call back करें।";
        }
        if (lower.contains("driving") || lower.contains("ड्राइविंग")) {
            return "मैं drive कर रहा हूं। RS Assistant संदेश ले रहा है।";
        }
        if (lower.contains("sleeping") || lower.contains("सो रहा")) {
            return "मैं सो रहा हूं। सुबह बात करते हैं।";
        }

        return getAutoReplyMessage();
    }

    // ==================== EMERGENCY SOS ====================

    public void setEmergencyContact(String phoneNumber) {
        prefs.edit().putString(KEY_EMERGENCY_CONTACT, phoneNumber).apply();
    }

    public String getEmergencyContact() {
        return prefs.getString(KEY_EMERGENCY_CONTACT, "");
    }

    public void triggerSOS() {
        String emergencyContact = getEmergencyContact();
        if (emergencyContact.isEmpty()) {
            sendSOSNotification("No emergency contact set!");
            return;
        }

        // Get current location
        String locationInfo = getCurrentLocationInfo();

        // Send SMS to emergency contact
        String sosMessage = "🆘 EMERGENCY! मुझे मदद चाहिए!\n" +
            "Location: " + locationInfo + "\n" +
            "Time: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()) +
            "\n\n- Sent via RS Assistant SOS";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(sosMessage);
            smsManager.sendMultipartTextMessage(emergencyContact, null, parts, null, null);
            sendSOSNotification("SOS SMS sent to " + emergencyContact);
        } catch (Exception e) {
            sendSOSNotification("Failed to send SOS: " + e.getMessage());
        }
    }

    private String getCurrentLocationInfo() {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (location != null) {
                    String lat = String.format("%.6f", location.getLatitude());
                    String lng = String.format("%.6f", location.getLongitude());
                    return "https://maps.google.com/?q=" + lat + "," + lng;
                }
            }
        } catch (SecurityException e) {
            // Location permission not granted
        }
        return "Location unavailable (enable GPS)";
    }

    private void sendSOSNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SOS)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("🆘 SOS Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(9999, builder.build());
        }
    }

    // ==================== CUSTOM COMMANDS ====================

    public void addCustomCommand(String trigger, String action) {
        customCommands.put(trigger.toLowerCase(), action);
        saveCustomCommands();
    }

    public void removeCustomCommand(String trigger) {
        customCommands.remove(trigger.toLowerCase());
        saveCustomCommands();
    }

    public String getCustomCommandAction(String voiceInput) {
        String lower = voiceInput.toLowerCase();
        for (Map.Entry<String, String> entry : customCommands.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Map<String, String> getAllCustomCommands() {
        return new HashMap<>(customCommands);
    }

    private void saveCustomCommands() {
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, String> entry : customCommands.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            prefs.edit().putString(KEY_CUSTOM_COMMANDS, json.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCustomCommands() {
        try {
            String saved = prefs.getString(KEY_CUSTOM_COMMANDS, "{}");
            JSONObject json = new JSONObject(saved);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                customCommands.put(key, json.getString(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== SMART SUGGESTIONS ====================

    public List<String> getSmartSuggestions() {
        List<String> suggestions = new ArrayList<>();

        // Time-based suggestions
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            suggestions.add("🌅 सुप्रभात! Alarm बंद करना है?");
            suggestions.add("☕ Morning! क्या weather देखना है?");
        } else if (hour >= 12 && hour < 17) {
            suggestions.add("🌞 दोपहर! कुछ खाना order करें?");
            suggestions.add("📞 किसी को call करना है?");
        } else if (hour >= 17 && hour < 21) {
            suggestions.add("🌆 शाम हो गई! Music बजाएं?");
            suggestions.add("📱 Social media check करें?");
        } else {
            suggestions.add("🌙 रात है! जल्दी सो जाइए");
            suggestions.add("😴 Good night! Alarm set करें?");
        }

        // Action-based suggestions
        if (!recentActions.isEmpty()) {
            String lastAction = recentActions.get(recentActions.size() - 1);
            suggestions.add("🔄 फिर से: " + lastAction + "?");
        }

        return suggestions;
    }

    public void recordAction(String action) {
        recentActions.add(action);
        // Keep only last 10 actions
        while (recentActions.size() > 10) {
            recentActions.remove(0);
        }
    }

    // ==================== REMINDERS & SCHEDULE ====================

    public void setReminder(String message, int minutesFromNow) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent("com.rsassistant.REMINDER");
        intent.putExtra("message", message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            (int) System.currentTimeMillis(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + (minutesFromNow * 60 * 1000L);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }

        sendReminderNotification("⏰ Reminder set for " + minutesFromNow + " minutes");
    }

    private void sendReminderNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SMART)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("RS Assistant")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    // ==================== PERFORMANCE INFO ====================

    public String getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMem = runtime.maxMemory() / (1024 * 1024);

        return "📊 System Info:\n" +
               "• Memory: " + usedMem + "MB / " + maxMem + "MB\n" +
               "• Available CPUs: " + runtime.availableProcessors() + "\n" +
               "• Android: " + Build.VERSION.RELEASE + "\n" +
               "• Device: " + Build.MODEL;
    }

    public String getBatteryTip() {
        return "🔋 Battery Tips:\n" +
               "• Screen brightness कम करें\n" +
               "• Background apps बंद करें\n" +
               "• Battery saver on करें\n" +
               "• WiFi/Bluetooth off रखें जब जरूरत न हो";
    }

    // ==================== QUICK ACTIONS ====================

    public String[] getQuickActions() {
        return new String[]{
            "🔊 Volume Full",
            "🔇 Silent Mode",
            "🔦 Flashlight",
            "📷 Camera",
            "📞 Last Call",
            "🎵 Music",
            "🔒 Lock Screen",
            "🆘 SOS"
        };
    }

    public void performQuickAction(int index) {
        switch (index) {
            case 0: // Volume Full
                recordAction("Volume Full");
                break;
            case 1: // Silent Mode
                recordAction("Silent Mode");
                break;
            case 2: // Flashlight
                recordAction("Flashlight Toggle");
                break;
            case 3: // Camera
                recordAction("Camera Open");
                break;
            case 7: // SOS
                triggerSOS();
                break;
        }
    }

    // ==================== NOTIFICATION CHANNELS ====================

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // SOS Channel
            NotificationChannel sosChannel = new NotificationChannel(
                CHANNEL_SOS,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            sosChannel.setDescription("Emergency SOS notifications");
            manager.createNotificationChannel(sosChannel);

            // Smart Channel
            NotificationChannel smartChannel = new NotificationChannel(
                CHANNEL_SMART,
                "Smart Features",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            smartChannel.setDescription("Smart assistant notifications");
            manager.createNotificationChannel(smartChannel);
        }
    }
}
