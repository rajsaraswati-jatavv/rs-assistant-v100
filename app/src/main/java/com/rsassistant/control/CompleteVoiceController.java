package com.rsassistant.control;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CompleteVoiceController - Full Phone Control by Voice Commands
 * 
 * Comprehensive voice control system supporting:
 * - App Control: Open any app by name
 * - Communication: Calls, SMS, WhatsApp, Email
 * - Navigation: Google Maps navigation
 * - Media: Music, YouTube, Spotify control
 * - System: WiFi, Bluetooth, Settings control
 * - Camera: Photo, Selfie, Flashlight
 * - Alarms & Reminders
 * - Notifications
 * - Battery & Power management
 * 
 * Supports Hindi-English bilingual commands
 */
public class CompleteVoiceController {

    private static final String TAG = "CompleteVoiceController";
    private final Context context;
    
    // Sub-controllers
    private final AppLauncher appLauncher;
    private final CommunicationManager communicationManager;
    private final MediaController mediaController;
    private final SystemController systemController;
    private final NavigationController navigationController;
    private final ContactManager contactManager;
    
    // System managers
    private final AudioManager audioManager;
    private final CameraManager cameraManager;
    private final PowerManager powerManager;
    private final NotificationManager notificationManager;
    private final AlarmManager alarmManager;
    
    // Camera state
    private String backCameraId = null;
    private String frontCameraId = null;
    private boolean isFlashOn = false;

    public interface VoiceControlCallback {
        void onSuccess(String message);
        void onError(String error);
        void onPartialResult(String partial);
        void onRequiresConfirmation(String action, String confirmationMessage);
    }

    public CompleteVoiceController(Context context) {
        this.context = context;
        
        // Initialize sub-controllers
        this.appLauncher = new AppLauncher(context);
        this.communicationManager = new CommunicationManager(context);
        this.mediaController = new MediaController(context);
        this.systemController = new SystemController(context);
        this.navigationController = new NavigationController(context);
        this.contactManager = new ContactManager(context);
        
        // Initialize system managers
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Initialize camera IDs
        initCameraIds();
    }

    private void initCameraIds() {
        try {
            if (cameraManager != null) {
                String[] cameraIds = cameraManager.getCameraIdList();
                for (String id : cameraIds) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null) {
                        if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                            backCameraId = id;
                        } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                            frontCameraId = id;
                        }
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error initializing cameras: " + e.getMessage());
        }
    }

    // ==================== APP CONTROL ====================

    /**
     * Open app by voice command
     * Supports: "WhatsApp kholo", "Open Instagram", "YouTube chalao"
     */
    public void openApp(String appName, VoiceControlCallback callback) {
        appLauncher.launchApp(appName, new AppLauncher.LaunchCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onMultipleMatches(List<String> matches) {
                StringBuilder sb = new StringBuilder("Multiple apps mil gayi: ");
                for (int i = 0; i < Math.min(3, matches.size()); i++) {
                    sb.append(matches.get(i));
                    if (i < matches.size() - 1) sb.append(", ");
                }
                callback.onPartialResult(sb.toString());
            }
        });
    }

    /**
     * Close current app
     */
    public void closeApp(String appName, VoiceControlCallback callback) {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(homeIntent);
            callback.onSuccess(appName + " band ho gaya!");
        } catch (Exception e) {
            callback.onError("App band nahi ho pa raha: " + e.getMessage());
        }
    }

    // ==================== COMMUNICATION ====================

    /**
     * Make a call
     * Supports: "Raj ko call karo", "Call mom", "9876543210 ko phone karo"
     */
    public void makeCall(String nameOrNumber, VoiceControlCallback callback) {
        communicationManager.makeCall(nameOrNumber, new CommunicationManager.CommunicationCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onContactFound(String name, String number) {
                callback.onRequiresConfirmation("call", 
                    "Kya aap " + name + " (" + number + ") ko call karna chahte hain?");
            }
        });
    }

    /**
     * Send SMS
     * Supports: "Raj ko message bhejo", "Send SMS to mom saying hello"
     */
    public void sendSMS(String contact, String message, VoiceControlCallback callback) {
        communicationManager.sendSMS(contact, message, new CommunicationManager.CommunicationCallback() {
            @Override
            public void onSuccess(String msg) {
                callback.onSuccess(msg);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onContactFound(String name, String number) {
                callback.onPartialResult("Message: \"" + message + "\" bhejna hai " + name + " ko?");
            }
        });
    }

    /**
     * Send WhatsApp message
     * Supports: "Raj ko WhatsApp bhejo", "WhatsApp message to mom"
     */
    public void sendWhatsApp(String contact, String message, VoiceControlCallback callback) {
        communicationManager.sendWhatsApp(contact, message, new CommunicationManager.CommunicationCallback() {
            @Override
            public void onSuccess(String msg) {
                callback.onSuccess(msg);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onContactFound(String name, String number) {
                callback.onPartialResult("WhatsApp message ready for " + name);
            }
        });
    }

    /**
     * Send Email
     * Supports: "Raj ko email bhejo", "Send email to boss"
     */
    public void sendEmail(String contact, String subject, String body, VoiceControlCallback callback) {
        communicationManager.sendEmail(contact, subject, body, new CommunicationManager.CommunicationCallback() {
            @Override
            public void onSuccess(String msg) {
                callback.onSuccess(msg);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onContactFound(String name, String email) {
                callback.onPartialResult("Email ready for " + name + " (" + email + ")");
            }
        });
    }

    /**
     * Read last messages
     * Supports: "Last message padho", "Notifications padho"
     */
    public void readLastMessages(VoiceControlCallback callback) {
        communicationManager.readLastMessages(new CommunicationManager.CommunicationCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onContactFound(String name, String message) {
                // Not used
            }
        });
    }

    // ==================== NAVIGATION ====================

    /**
     * Start navigation
     * Supports: "Navigation shuru karo Delhi ke liye", "Navigate to Mumbai"
     */
    public void startNavigation(String destination, VoiceControlCallback callback) {
        navigationController.navigateTo(destination, new NavigationController.NavigationCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onLocation(NavigationController.LocationInfo location) {
                // Not used
            }

            @Override
            public void onPlaces(List<NavigationController.PlaceInfo> places) {
                // Not used
            }
        });
    }

    /**
     * Open maps
     */
    public void openMaps(VoiceControlCallback callback) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Maps khul gaya!");
            } else {
                callback.onError("Google Maps installed nahi hai");
            }
        } catch (Exception e) {
            callback.onError("Maps nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Find nearby places
     * Supports: "Paas mein restaurant dhoondo", "Nearby petrol pump"
     */
    public void findNearby(String placeType, VoiceControlCallback callback) {
        navigationController.findNearby(placeType, new NavigationController.NavigationCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onLocation(NavigationController.LocationInfo location) {
                // Not used
            }

            @Override
            public void onPlaces(List<NavigationController.PlaceInfo> places) {
                // Not used
            }
        });
    }

    // ==================== MEDIA CONTROL ====================

    /**
     * Play music
     * Supports: "Gaana bajao", "Play music", "Play Arijit Singh"
     */
    public void playMusic(String query, VoiceControlCallback callback) {
        if (query != null && !query.isEmpty() && !query.equals("music")) {
            // Specific song/artist requested
            mediaController.playOnSpotify(query, new MediaController.MediaCallback() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(message);
                }

                @Override
                public void onError(String error) {
                    // Try YouTube as fallback
                    mediaController.searchYouTube(query, new MediaController.MediaCallback() {
                        @Override
                        public void onSuccess(String msg) {
                            callback.onSuccess(msg);
                        }

                        @Override
                        public void onError(String err) {
                            callback.onError("Music play nahi ho pa raha");
                        }
                    });
                }
            });
        } else {
            // Just play/pause
            mediaController.play(new MediaController.MediaCallback() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(message);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        }
    }

    /**
     * Pause music
     */
    public void pauseMusic(VoiceControlCallback callback) {
        mediaController.pause(new MediaController.MediaCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Next track
     * Supports: "Next song", "Agle gaane pe jao"
     */
    public void nextTrack(VoiceControlCallback callback) {
        mediaController.nextTrack(new MediaController.MediaCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Previous track
     * Supports: "Previous song", "Pichle gaane pe jao"
     */
    public void previousTrack(VoiceControlCallback callback) {
        mediaController.previousTrack(new MediaController.MediaCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Open YouTube with search
     * Supports: "YouTube pe gaana search karo", "Play video on YouTube"
     */
    public void searchYouTube(String query, VoiceControlCallback callback) {
        mediaController.searchYouTube(query, new MediaController.MediaCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Open Spotify
     */
    public void openSpotify(VoiceControlCallback callback) {
        mediaController.openSpotify(new MediaController.MediaCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ==================== ALARMS & REMINDERS ====================

    /**
     * Set alarm
     * Supports: "Subah 6 baje alarm laga do", "Set alarm for 7 AM"
     */
    public void setAlarm(String timeInput, VoiceControlCallback callback) {
        try {
            int hour = -1, minute = 0;
            
            // Parse time input
            // Pattern: "6 baje", "6:30", "6:30 AM", "subah 6", "shaam 7"
            String lowerInput = timeInput.toLowerCase().trim();
            
            // Check for AM/PM indicators
            boolean isPM = lowerInput.contains("pm") || lowerInput.contains("shaam") || lowerInput.contains("evening");
            boolean isAM = lowerInput.contains("am") || lowerInput.contains("subah") || lowerInput.contains("morning");
            
            // Extract numbers
            Pattern pattern = Pattern.compile("(\\d+)(?::(\\d+))?");
            Matcher matcher = pattern.matcher(lowerInput);
            
            if (matcher.find()) {
                hour = Integer.parseInt(matcher.group(1));
                if (matcher.group(2) != null) {
                    minute = Integer.parseInt(matcher.group(2));
                }
                
                // Adjust for PM
                if (isPM && hour < 12) {
                    hour += 12;
                } else if (isAM && hour == 12) {
                    hour = 0;
                }
                
                // Create alarm intent
                Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
                alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hour);
                alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
                alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "Voice Alarm");
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                context.startActivity(alarmIntent);
                
                String timeStr = String.format("%02d:%02d", hour, minute);
                callback.onSuccess("Alarm set ho gaya " + timeStr + " baje!");
            } else {
                // Open alarm app
                Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(alarmIntent);
                callback.onSuccess("Alarm app khul gaya!");
            }
        } catch (Exception e) {
            callback.onError("Alarm set nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set timer
     * Supports: "5 minute ka timer set karo", "Set timer for 10 minutes"
     */
    public void setTimer(String durationInput, VoiceControlCallback callback) {
        try {
            int seconds = 0;
            
            // Parse duration
            Pattern pattern = Pattern.compile("(\\d+)\\s*(second|minute|hour|sec|min|hr)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(durationInput);
            
            while (matcher.find()) {
                int value = Integer.parseInt(matcher.group(1));
                String unit = matcher.group(2).toLowerCase();
                
                if (unit.startsWith("sec")) {
                    seconds += value;
                } else if (unit.startsWith("min")) {
                    seconds += value * 60;
                } else if (unit.startsWith("h")) {
                    seconds += value * 3600;
                }
            }
            
            if (seconds > 0) {
                Intent timerIntent = new Intent(AlarmClock.ACTION_SET_TIMER);
                timerIntent.putExtra(AlarmClock.EXTRA_LENGTH, seconds);
                timerIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "Voice Timer");
                timerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                context.startActivity(timerIntent);
                callback.onSuccess("Timer " + seconds + " seconds ke liye set ho gaya!");
            } else {
                // Just open timer
                Intent timerIntent = new Intent(AlarmClock.ACTION_SET_TIMER);
                timerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(timerIntent);
                callback.onSuccess("Timer app khul gaya!");
            }
        } catch (Exception e) {
            callback.onError("Timer set nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set reminder
     * Supports: "Mujhe yaad dilao meeting ka", "Remind me to call mom"
     */
    public void setReminder(String reminderText, String timeText, VoiceControlCallback callback) {
        try {
            // Open Google Assistant / reminders
            Intent reminderIntent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, reminderText)
                .putExtra(CalendarContract.Events.DESCRIPTION, "Voice Reminder")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(reminderIntent);
            callback.onSuccess("Reminder set ho gaya: " + reminderText);
        } catch (Exception e) {
            callback.onError("Reminder set nahi ho pa raha: " + e.getMessage());
        }
    }

    // ==================== CAMERA CONTROL ====================

    /**
     * Take photo
     * Supports: "Photo le lo", "Click picture"
     */
    public void takePhoto(VoiceControlCallback callback) {
        try {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(cameraIntent);
            callback.onSuccess("Camera khul gaya! Photo le lo!");
        } catch (Exception e) {
            callback.onError("Camera nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Take selfie
     * Supports: "Selfie click karo", "Front camera kholo"
     */
    public void takeSelfie(VoiceControlCallback callback) {
        try {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(cameraIntent);
            callback.onSuccess("Front camera khul gaya! Selfie le lo!");
        } catch (Exception e) {
            callback.onError("Selfie mode nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Open camera
     */
    public void openCamera(VoiceControlCallback callback) {
        try {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(cameraIntent);
            callback.onSuccess("Camera khul gaya!");
        } catch (Exception e) {
            callback.onError("Camera nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Toggle flash/flashlight
     * Supports: "Flash on karo", "Flashlight jalao"
     */
    public void toggleFlash(VoiceControlCallback callback) {
        try {
            if (backCameraId != null && cameraManager != null) {
                isFlashOn = !isFlashOn;
                cameraManager.setTorchMode(backCameraId, isFlashOn);
                callback.onSuccess(isFlashOn ? "Flashlight jal gaya!" : "Flashlight band ho gaya!");
            } else {
                callback.onError("Flashlight support nahi hai");
            }
        } catch (CameraAccessException e) {
            callback.onError("Flashlight control nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Turn flash on
     */
    public void flashOn(VoiceControlCallback callback) {
        try {
            if (backCameraId != null && cameraManager != null) {
                cameraManager.setTorchMode(backCameraId, true);
                isFlashOn = true;
                callback.onSuccess("Flashlight jal gaya!");
            } else {
                callback.onError("Flashlight support nahi hai");
            }
        } catch (CameraAccessException e) {
            callback.onError("Flashlight on nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Turn flash off
     */
    public void flashOff(VoiceControlCallback callback) {
        try {
            if (backCameraId != null && cameraManager != null) {
                cameraManager.setTorchMode(backCameraId, false);
                isFlashOn = false;
                callback.onSuccess("Flashlight band ho gaya!");
            }
        } catch (CameraAccessException e) {
            callback.onError("Flashlight off nahi ho pa raha: " + e.getMessage());
        }
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Read notifications
     * Supports: "Notifications padho", "Padho saari notifications"
     */
    public void readNotifications(VoiceControlCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
                
                if (notifications.length == 0) {
                    callback.onSuccess("Koi notification nahi hai");
                    return;
                }
                
                StringBuilder sb = new StringBuilder();
                sb.append(notifications.length).append(" notifications hain:\n");
                
                for (int i = 0; i < Math.min(5, notifications.length); i++) {
                    StatusBarNotification sbn = notifications[i];
                    Notification notification = sbn.getNotification();
                    if (notification != null) {
                        CharSequence ticker = notification.tickerText;
                        if (ticker != null) {
                            sb.append(i + 1).append(". ").append(ticker).append("\n");
                        }
                    }
                }
                
                callback.onSuccess(sb.toString());
            } else {
                callback.onError("Notification access permission chahiye. Settings mein jao.");
            }
        } else {
            callback.onError("Notification reading available nahi hai");
        }
    }

    /**
     * Clear all notifications
     */
    public void clearNotifications(VoiceControlCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                notificationManager.cancelAll();
                callback.onSuccess("Saari notifications clear ho gayi!");
            } else {
                callback.onError("Notification access permission chahiye");
            }
        } else {
            callback.onError("Feature available nahi hai");
        }
    }

    // ==================== SETTINGS CONTROL ====================

    /**
     * Control WiFi
     * Supports: "WiFi on karo", "WiFi band karo"
     */
    public void controlWiFi(boolean enable, VoiceControlCallback callback) {
        if (enable) {
            systemController.enableWiFi(new SystemController.SystemCallback() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(message);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }

                @Override
                public void onStatus(SystemController.SystemStatus status) {
                    // Not used
                }
            });
        } else {
            systemController.disableWiFi(new SystemController.SystemCallback() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(message);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }

                @Override
                public void onStatus(SystemController.SystemStatus status) {
                    // Not used
                }
            });
        }
    }

    /**
     * Control Bluetooth
     * Supports: "Bluetooth on karo", "Bluetooth off karo"
     */
    public void controlBluetooth(boolean enable, VoiceControlCallback callback) {
        if (enable) {
            systemController.enableBluetooth(new SystemController.SystemCallback() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(message);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }

                @Override
                public void onStatus(SystemController.SystemStatus status) {
                    // Not used
                }
            });
        } else {
            systemController.disableBluetooth(new SystemController.SystemCallback() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(message);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }

                @Override
                public void onStatus(SystemController.SystemStatus status) {
                    // Not used
                }
            });
        }
    }

    /**
     * Control Mobile Data
     * Supports: "Mobile data on karo"
     */
    public void controlMobileData(VoiceControlCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_NETWORK_AND_INTERNET_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Network settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    /**
     * Control Hotspot
     * Supports: "Hotspot on karo"
     */
    public void controlHotspot(VoiceControlCallback callback) {
        systemController.openHotspotSettings(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    /**
     * Control Airplane Mode
     * Supports: "Airplane mode on karo"
     */
    public void controlAirplaneMode(VoiceControlCallback callback) {
        systemController.toggleAirplaneMode(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    /**
     * Control Rotation
     * Supports: "Rotation on karo", "Auto rotate on karo"
     */
    public void controlRotation(boolean enable, VoiceControlCallback callback) {
        if (enable) {
            systemController.enableAutoRotation(new SystemController.SystemCallback() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(message);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }

                @Override
                public void onStatus(SystemController.SystemStatus status) {
                    // Not used
                }
            });
        } else {
            systemController.disableAutoRotation(new SystemController.SystemCallback() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(message);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }

                @Override
                public void onStatus(SystemController.SystemStatus status) {
                    // Not used
                }
            });
        }
    }

    // ==================== VOLUME CONTROL ====================

    /**
     * Volume up
     * Supports: "Volume badhao", "Aawaz badhao"
     */
    public void volumeUp(VoiceControlCallback callback) {
        mediaController.volumeUp(new MediaController.MediaCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Volume down
     * Supports: "Volume kam karo", "Aawaz kam karo"
     */
    public void volumeDown(VoiceControlCallback callback) {
        mediaController.volumeDown(new MediaController.MediaCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Set volume
     */
    public void setVolume(int percent, VoiceControlCallback callback) {
        mediaController.setVolume(percent, new MediaController.MediaCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Silent mode
     * Supports: "Silent mode on karo", "Phone silent karo"
     */
    public void setSilentMode(VoiceControlCallback callback) {
        systemController.setSilentMode(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    /**
     * Vibrate mode
     */
    public void setVibrateMode(VoiceControlCallback callback) {
        systemController.setVibrateMode(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    /**
     * Normal mode
     */
    public void setNormalMode(VoiceControlCallback callback) {
        systemController.setNormalMode(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    // ==================== SCREEN CONTROL ====================

    /**
     * Increase brightness
     * Supports: "Screen bright karo", "Brightness badhao"
     */
    public void increaseBrightness(VoiceControlCallback callback) {
        systemController.increaseBrightness(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    /**
     * Decrease brightness
     * Supports: "Screen dim karo", "Brightness kam karo"
     */
    public void decreaseBrightness(VoiceControlCallback callback) {
        systemController.decreaseBrightness(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    /**
     * Set brightness
     */
    public void setBrightness(int percent, VoiceControlCallback callback) {
        systemController.setBrightnessPercent(percent, new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    // ==================== BATTERY MANAGEMENT ====================

    /**
     * Enable battery saver
     * Supports: "Battery saver on karo"
     */
    public void enableBatterySaver(VoiceControlCallback callback) {
        systemController.enableBatterySaver(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    /**
     * Get battery status
     */
    public void getBatteryStatus(VoiceControlCallback callback) {
        SystemController.SystemStatus status = systemController.getSystemStatus();
        callback.onSuccess("Battery " + status.batteryLevel + "% bachi hai" + 
            (status.powerSaveMode ? " (Power Save Mode on hai)" : ""));
    }

    // ==================== DO NOT DISTURB ====================

    /**
     * Enable DND
     * Supports: "Do not disturb on karo"
     */
    public void enableDND(VoiceControlCallback callback) {
        systemController.enableDND(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    /**
     * Disable DND
     */
    public void disableDND(VoiceControlCallback callback) {
        systemController.disableDND(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    // ==================== TIME & DATE ====================

    /**
     * Tell time
     * Supports: "Time batao", "Kitne baje hain"
     */
    public void tellTime(VoiceControlCallback callback) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String time = sdf.format(new Date());
        callback.onSuccess("Abhi " + time + " baje hain");
    }

    /**
     * Tell date
     * Supports: "Aaj ki date batao", "Today's date"
     */
    public void tellDate(VoiceControlCallback callback) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("hi", "IN"));
        String date = sdf.format(new Date());
        callback.onSuccess("Aaj " + date + " hai");
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Open settings
     */
    public void openSettings(VoiceControlCallback callback) {
        systemController.openSettings(new SystemController.SystemCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(SystemController.SystemStatus status) {
                // Not used
            }
        });
    }

    /**
     * Get system status summary
     */
    public String getSystemStatusSummary() {
        SystemController.SystemStatus status = systemController.getSystemStatus();
        StringBuilder sb = new StringBuilder();
        
        sb.append("WiFi: ").append(status.wifiEnabled ? "On" : "Off").append("\n");
        sb.append("Bluetooth: ").append(status.bluetoothEnabled ? "On" : "Off").append("\n");
        sb.append("Battery: ").append(status.batteryLevel).append("%\n");
        sb.append("Flashlight: ").append(status.torchOn ? "On" : "Off").append("\n");
        sb.append("Airplane Mode: ").append(status.airplaneMode ? "On" : "Off");
        
        return sb.toString();
    }

    // Getters for sub-controllers
    public AppLauncher getAppLauncher() { return appLauncher; }
    public CommunicationManager getCommunicationManager() { return communicationManager; }
    public MediaController getMediaController() { return mediaController; }
    public SystemController getSystemController() { return systemController; }
    public NavigationController getNavigationController() { return navigationController; }
    public ContactManager getContactManager() { return contactManager; }
}
