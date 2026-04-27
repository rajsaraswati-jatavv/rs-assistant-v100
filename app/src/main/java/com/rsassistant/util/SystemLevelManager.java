package com.rsassistant.util;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.app.NotificationCompat;

import com.rsassistant.MainActivity;
import com.rsassistant.R;

import java.lang.reflect.Method;

/**
 * SystemLevelManager - System-Level Control Like Motorola
 * Once set up, controls work at system level
 * Works even when app is not in foreground
 */
public class SystemLevelManager {

    private static final String PREF_NAME = "system_level_prefs";
    private static final String KEY_SETUP_DONE = "setup_completed";
    private static final String KEY_TORCH_ENABLED = "torch_enabled";
    private static final String KEY_SHAKE_ENABLED = "shake_enabled";
    private static final String KEY_VOICE_ALWAYS_ON = "voice_always_on";

    private static final String CHANNEL_ID = "system_service_channel";
    private static final int NOTIFICATION_ID = 999;

    private static SystemLevelManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    
    private CameraManager cameraManager;
    private String cameraId;
    private AudioManager audioManager;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdmin;

    private boolean torchState = false;
    private TorchCallback torchCallback;

    public interface TorchCallback {
        void onTorchStateChanged(boolean isOn);
    }

    public static synchronized SystemLevelManager getInstance(Context context) {
        if (instance == null) {
            instance = new SystemLevelManager(context.getApplicationContext());
        }
        return instance;
    }

    private SystemLevelManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        initHardware();
        loadState();
    }

    private void initHardware() {
        // Camera for Torch
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            cameraId = null;
        }
        
        // Audio
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        // Device Admin
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(context, RSDeviceAdminReceiver.class);
    }

    private void loadState() {
        torchState = prefs.getBoolean(KEY_TORCH_ENABLED, false);
    }

    // ==================== SETUP ====================

    public boolean isSetupComplete() {
        return prefs.getBoolean(KEY_SETUP_DONE, false);
    }

    public void markSetupComplete() {
        prefs.edit().putBoolean(KEY_SETUP_DONE, true).apply();
        startSystemService();
    }

    public void startSystemService() {
        // Create persistent notification for system-level service
        createNotificationChannel();
        
        // This keeps the service running at system level
        Intent serviceIntent = new Intent(context, com.rsassistant.service.VoiceRecognitionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    // ==================== TORCH/FLASHLIGHT ====================

    public void setTorch(boolean enabled) {
        if (cameraManager == null || cameraId == null) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, enabled);
                torchState = enabled;
                prefs.edit().putBoolean(KEY_TORCH_ENABLED, enabled).apply();
                
                if (torchCallback != null) {
                    torchCallback.onTorchStateChanged(enabled);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void toggleTorch() {
        setTorch(!torchState);
    }

    public boolean isTorchOn() {
        return torchState;
    }

    public void setTorchCallback(TorchCallback callback) {
        this.torchCallback = callback;
    }

    // ==================== VOLUME CONTROL ====================

    public void setVolume(int level) {
        if (audioManager == null) return;
        
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = (int) (maxVolume * (level / 100.0f));
        
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            AudioManager.FLAG_SHOW_UI
        );
    }

    public void volumeUp() {
        if (audioManager == null) return;
        
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        
        if (current < max) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            );
        }
    }

    public void volumeDown() {
        if (audioManager == null) return;
        
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        );
    }

    public void volumeMax() {
        if (audioManager == null) return;
        
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            max,
            AudioManager.FLAG_SHOW_UI
        );
    }

    public void volumeMute() {
        if (audioManager == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_SHOW_UI
            );
        } else {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                0,
                AudioManager.FLAG_SHOW_UI
            );
        }
    }

    // ==================== RINGER MODE ====================

    public void setSilentMode() {
        if (audioManager == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } catch (SecurityException e) {
                // Need Do Not Disturb permission
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } else {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    public void setVibrateMode() {
        if (audioManager == null) return;
        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
    }

    public void setNormalMode() {
        if (audioManager == null) return;
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    // ==================== SCREEN CONTROL ====================

    public void lockScreen() {
        if (devicePolicyManager == null || deviceAdmin == null) return;
        
        if (devicePolicyManager.isAdminActive(deviceAdmin)) {
            devicePolicyManager.lockNow();
        }
    }

    // ==================== WIFI CONTROL ====================

    public void setWifi(boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);
        
        if (wifiManager != null) {
            wifiManager.setWifiEnabled(enabled);
        }
    }

    // ==================== NOTIFICATIONS ====================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "System Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("RS Assistant System Service");
            channel.setShowBadge(false);
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public Notification createPersistentNotification() {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("RS Assistant Active")
            .setContentText("System-level controls enabled | Shake for torch")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent);

        return builder.build();
    }

    // ==================== STATUS ====================

    public String getSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("🔧 System Level Status:\n\n");
        
        status.append("📱 Hardware:\n");
        status.append("• Camera: ").append(cameraId != null ? "Available ✓" : "Not Available").append("\n");
        status.append("• Torch: ").append(torchState ? "ON 💡" : "OFF").append("\n");
        
        if (audioManager != null) {
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            status.append("• Volume: ").append(vol).append("/").append(max).append("\n");
            
            String mode = "Normal";
            switch (audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT: mode = "Silent"; break;
                case AudioManager.RINGER_MODE_VIBRATE: mode = "Vibrate"; break;
            }
            status.append("• Ringer: ").append(mode).append("\n");
        }
        
        if (devicePolicyManager != null) {
            status.append("• Device Admin: ").append(
                devicePolicyManager.isAdminActive(deviceAdmin) ? "Active ✓" : "Not Active"
            ).append("\n");
        }
        
        return status.toString();
    }

    // ==================== CAPABILITIES ====================

    public String[] getCapabilities() {
        return new String[]{
            "💡 Torch/Flashlight Control",
            "🔊 Volume Control (Up/Down/Max/Mute)",
            "🔇 Silent Mode",
            "📳 Vibrate Mode",
            "📱 Screen Lock",
            "📶 WiFi Toggle",
            "📱 Persistent System Service",
            "📳 Shake Gesture Detection",
            "🎤 Always-On Voice Recognition",
            "🆘 Emergency SOS",
            "⏰ Scheduled Actions",
            "📊 Usage Memory & Learning"
        };
    }
}
