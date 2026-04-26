package com.rsassistant.util;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.view.WindowManager;

import com.rsassistant.service.RSAccessibilityService;

/**
 * Device Control Manager - Controls Volume, Power, Lock Screen
 * Supports voice commands for all hardware buttons
 */
public class DeviceControlManager {

    private final Context context;
    private final AudioManager audioManager;
    private final DevicePolicyManager devicePolicyManager;
    private final ComponentName adminComponent;
    private final PowerManager powerManager;
    private final KeyguardManager keyguardManager;

    public DeviceControlManager(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.adminComponent = new ComponentName(context, RSDeviceAdminReceiver.class);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    }

    // ==================== VOLUME CONTROL ====================

    /**
     * Increase volume - Volume badhao / volume jyada karo / volume up
     */
    public String volumeUp() {
        try {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            
            if (currentVolume < maxVolume) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND
                );
                return "Volume badha diya - " + (currentVolume + 1) + "/" + maxVolume;
            } else {
                return "Volume already maximum hai";
            }
        } catch (Exception e) {
            // Fallback - use accessibility
            return volumeUpAccessibility();
        }
    }

    /**
     * Decrease volume - Volume kam karo / volume down
     */
    public String volumeDown() {
        try {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            
            if (currentVolume > 0) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND
                );
                return "Volume kam kar diya - " + (currentVolume - 1);
            } else {
                return "Volume already minimum hai";
            }
        } catch (Exception e) {
            return volumeDownAccessibility();
        }
    }

    /**
     * Set volume to maximum
     */
    public String volumeMax() {
        try {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                maxVolume,
                AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND
            );
            return "Volume maximum kar diya";
        } catch (Exception e) {
            return "Volume max nahi kar saka";
        }
    }

    /**
     * Mute volume
     */
    public String volumeMute() {
        try {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                0,
                AudioManager.FLAG_SHOW_UI
            );
            return "Volume mute kar diya";
        } catch (Exception e) {
            return "Mute nahi kar saka";
        }
    }

    /**
     * Set specific volume level (0-100)
     */
    public String setVolume(int percentage) {
        try {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int targetVolume = (int) (maxVolume * (percentage / 100.0));
            
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                targetVolume,
                AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND
            );
            return "Volume set kar diya " + percentage + " percent";
        } catch (Exception e) {
            return "Volume set nahi kar saka";
        }
    }

    // Fallback using accessibility gestures
    private String volumeUpAccessibility() {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            // Simulate volume up key press
            return "Volume badhao - use volume button";
        }
        return "Accessibility service enable karo";
    }

    private String volumeDownAccessibility() {
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            return "Volume kam karo - use volume button";
        }
        return "Accessibility service enable karo";
    }

    // ==================== LOCK SCREEN ====================

    /**
     * Lock screen immediately - Lock karo / screen lock karo
     */
    public String lockScreen() {
        // Try accessibility service first (Android 9+)
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            boolean success = service.lockScreen();
            if (success) {
                return "Screen lock kar diya";
            }
        }

        // Try device admin
        if (isAdminActive()) {
            devicePolicyManager.lockNow();
            return "Screen lock kar diya";
        }

        // Fallback - use keyguard manager
        return lockScreenFallback();
    }

    private String lockScreenFallback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                if (keyguardManager != null) {
                    keyguardManager.requestDismissKeyguard((Activity) context, null);
                }
            }
            
            // Use power button simulation via accessibility
            RSAccessibilityService service = RSAccessibilityService.getInstance();
            if (service != null) {
                service.goHome();
            }
            return "Screen lock karne ke liye power button dabao";
        } catch (Exception e) {
            return "Screen lock nahi kar saka - Device Admin enable karo";
        }
    }

    // ==================== POWER OFF / RESTART ====================

    /**
     * Power off device - Power off karo / band karo
     * Requires Device Admin permission
     */
    public String powerOff() {
        if (isAdminActive()) {
            // Some devices support this
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Show power dialog instead
                    RSAccessibilityService service = RSAccessibilityService.getInstance();
                    if (service != null) {
                        service.openPowerDialog();
                        return "Power menu khul gaya - Power off select karo";
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Fallback - show power dialog
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.openPowerDialog();
            return "Power menu khul gaya - Power off button dabao";
        }
        
        return "Power off ke liye power button dabao";
    }

    /**
     * Restart device - Restart karo / reboot karo
     */
    public String restart() {
        if (isAdminActive()) {
            try {
                devicePolicyManager.reboot(adminComponent);
                return "Phone restart ho raha hai";
            } catch (SecurityException e) {
                // Not all devices allow this
            }
        }
        
        // Fallback - show power dialog
        RSAccessibilityService service = RSAccessibilityService.getInstance();
        if (service != null) {
            service.openPowerDialog();
            return "Power menu khul gaya - Restart select karo";
        }
        
        return "Restart ke liye power menu use karo";
    }

    // ==================== SCREEN CONTROL ====================

    /**
     * Turn screen on
     */
    public String screenOn() {
        try {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "rsassistant:screenon"
            );
            wakeLock.acquire(5000);
            wakeLock.release();
            return "Screen on kar diya";
        } catch (Exception e) {
            return "Screen on nahi kar saka";
        }
    }

    /**
     * Keep screen on for specified duration
     */
    public String keepScreenOn(int seconds) {
        try {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "rsassistant:keepon"
            );
            wakeLock.acquire(seconds * 1000L);
            return "Screen " + seconds + " seconds ke liye on rahega";
        } catch (Exception e) {
            return "Screen on nahi rakh saka";
        }
    }

    // ==================== DEVICE ADMIN ====================

    /**
     * Check if device admin is active
     */
    public boolean isAdminActive() {
        return devicePolicyManager != null && 
               devicePolicyManager.isAdminActive(adminComponent);
    }

    /**
     * Get intent to request device admin permission
     */
    public Intent getDeviceAdminIntent() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "RS Assistant ko phone control karne ke liye ye permission chahiye");
        return intent;
    }

    /**
     * Enable device admin
     */
    public String enableDeviceAdmin() {
        if (isAdminActive()) {
            return "Device Admin already enabled hai";
        }
        try {
            context.startActivity(getDeviceAdminIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return "Device Admin permission enable karo";
        } catch (Exception e) {
            return "Device Admin enable nahi kar saka";
        }
    }

    // ==================== RINGER MODE ====================

    /**
     * Set silent mode
     */
    public String setSilent() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
            return "Silent mode on kar diya";
        } catch (Exception e) {
            return "Silent mode nahi kar saka";
        }
    }

    /**
     * Set vibrate mode
     */
    public String setVibrate() {
        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            return "Vibrate mode on kar diya";
        } catch (Exception e) {
            return "Vibrate mode nahi kar saka";
        }
    }

    /**
     * Set normal ringer mode
     */
    public String setNormal() {
        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            return "Normal mode on kar diya";
        } catch (Exception e) {
            return "Normal mode nahi kar saka";
        }
    }

    // ==================== MEDIA CONTROL ====================

    /**
     * Simulate media play/pause
     */
    public String mediaPlayPause() {
        try {
            audioManager.dispatchMediaKeyEvent(
                new android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                )
            );
            audioManager.dispatchMediaKeyEvent(
                new android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                )
            );
            return "Play/Pause kar diya";
        } catch (Exception e) {
            return "Media control nahi kar saka";
        }
    }

    /**
     * Next track
     */
    public String mediaNext() {
        try {
            audioManager.dispatchMediaKeyEvent(
                new android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                )
            );
            audioManager.dispatchMediaKeyEvent(
                new android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                )
            );
            return "Next song play kar raha hai";
        } catch (Exception e) {
            return "Next nahi kar saka";
        }
    }

    /**
     * Previous track
     */
    public String mediaPrevious() {
        try {
            audioManager.dispatchMediaKeyEvent(
                new android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                )
            );
            audioManager.dispatchMediaKeyEvent(
                new android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                )
            );
            return "Previous song play kar raha hai";
        } catch (Exception e) {
            return "Previous nahi kar saka";
        }
    }
}
