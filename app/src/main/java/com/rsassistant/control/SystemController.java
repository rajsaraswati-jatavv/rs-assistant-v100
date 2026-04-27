package com.rsassistant.control;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.lang.reflect.Method;

/**
 * SystemController - Voice-based System Control Manager
 * 
 * Supported Commands:
 * - "WiFi on karo" / "Turn on WiFi"
 * - "Bluetooth off karo" / "Turn off Bluetooth"
 * - "Flashlight jalao" / "Turn on flashlight"
 * - "Brightness badhao" / "Increase brightness"
 */
public class SystemController {

    private static final String TAG = "SystemController";
    private final Context context;

    // System managers
    private final WifiManager wifiManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final AudioManager audioManager;
    private final CameraManager cameraManager;
    private final PowerManager powerManager;
    private final NotificationManager notificationManager;
    private final ActivityManager activityManager;

    // Camera torch
    private String cameraId;
    private boolean isTorchOn = false;

    public interface SystemCallback {
        void onSuccess(String message);
        void onError(String error);
        void onStatus(SystemStatus status);
    }

    public static class SystemStatus {
        public boolean wifiEnabled;
        public boolean bluetoothEnabled;
        public boolean torchOn;
        public int brightness;
        public int volume;
        public boolean airplaneMode;
        public boolean dndEnabled;
        public boolean powerSaveMode;
        public int batteryLevel;
    }

    public SystemController(Context context) {
        this.context = context;

        // Initialize managers
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.bluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        // Get camera ID for torch
        try {
            if (cameraManager != null) {
                String[] cameraIds = cameraManager.getCameraIdList();
                for (String id : cameraIds) {
                    if (cameraManager.getCameraCharacteristics(id)
                            .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                        cameraId = id;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access error: " + e.getMessage());
        }
    }

    // ==================== WiFi Controls ====================

    /**
     * Turn WiFi on
     */
    @SuppressLint("MissingPermission")
    public void enableWiFi(SystemCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ need to open settings
                openWiFiSettings(callback);
            } else {
                wifiManager.setWifiEnabled(true);
                callback.onSuccess("WiFi on ho gaya!");
            }
        } catch (Exception e) {
            callback.onError("WiFi on nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Turn WiFi off
     */
    @SuppressLint("MissingPermission")
    public void disableWiFi(SystemCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                openWiFiSettings(callback);
            } else {
                wifiManager.setWifiEnabled(false);
                callback.onSuccess("WiFi band ho gaya!");
            }
        } catch (Exception e) {
            callback.onError("WiFi off nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Toggle WiFi
     */
    public void toggleWiFi(SystemCallback callback) {
        if (wifiManager.isWifiEnabled()) {
            disableWiFi(callback);
        } else {
            enableWiFi(callback);
        }
    }

    /**
     * Check WiFi status
     */
    public boolean isWiFiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    /**
     * Open WiFi settings
     */
    public void openWiFiSettings(SystemCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("WiFi settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    // ==================== Bluetooth Controls ====================

    /**
     * Turn Bluetooth on
     */
    public void enableBluetooth(SystemCallback callback) {
        try {
            if (bluetoothAdapter != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ needs permission
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                            != PackageManager.PERMISSION_GRANTED) {
                        openBluetoothSettings(callback);
                        return;
                    }
                }
                bluetoothAdapter.enable();
                callback.onSuccess("Bluetooth on ho gaya!");
            } else {
                callback.onError("Bluetooth support nahi hai");
            }
        } catch (Exception e) {
            callback.onError("Bluetooth on nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Turn Bluetooth off
     */
    public void disableBluetooth(SystemCallback callback) {
        try {
            if (bluetoothAdapter != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                            != PackageManager.PERMISSION_GRANTED) {
                        openBluetoothSettings(callback);
                        return;
                    }
                }
                bluetoothAdapter.disable();
                callback.onSuccess("Bluetooth band ho gaya!");
            }
        } catch (Exception e) {
            callback.onError("Bluetooth off nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Toggle Bluetooth
     */
    public void toggleBluetooth(SystemCallback callback) {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            disableBluetooth(callback);
        } else {
            enableBluetooth(callback);
        }
    }

    /**
     * Check Bluetooth status
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Open Bluetooth settings
     */
    public void openBluetoothSettings(SystemCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Bluetooth settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    // ==================== Flashlight Controls ====================

    /**
     * Turn flashlight on
     */
    public void enableFlashlight(SystemCallback callback) {
        try {
            if (cameraId != null && cameraManager != null) {
                cameraManager.setTorchMode(cameraId, true);
                isTorchOn = true;
                callback.onSuccess("Flashlight jal gaya!");
            } else {
                callback.onError("Flashlight support nahi hai");
            }
        } catch (CameraAccessException e) {
            callback.onError("Flashlight nahi jal pa raha: " + e.getMessage());
        }
    }

    /**
     * Turn flashlight off
     */
    public void disableFlashlight(SystemCallback callback) {
        try {
            if (cameraId != null && cameraManager != null) {
                cameraManager.setTorchMode(cameraId, false);
                isTorchOn = false;
                callback.onSuccess("Flashlight band ho gaya!");
            }
        } catch (CameraAccessException e) {
            callback.onError("Flashlight nahi band ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Toggle flashlight
     */
    public void toggleFlashlight(SystemCallback callback) {
        if (isTorchOn) {
            disableFlashlight(callback);
        } else {
            enableFlashlight(callback);
        }
    }

    /**
     * Check flashlight status
     */
    public boolean isFlashlightOn() {
        return isTorchOn;
    }

    // ==================== Brightness Controls ====================

    /**
     * Set brightness (0-255)
     */
    public void setBrightness(int brightness, SystemCallback callback) {
        try {
            // Ensure brightness is in valid range
            brightness = Math.max(0, Math.min(255, brightness));
            
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.getContentResolver(), 
                    Settings.System.SCREEN_BRIGHTNESS, brightness);
                callback.onSuccess("Brightness " + ((brightness * 100) / 255) + "% ho gayi!");
            } else {
                // Request write permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Permission lene ke baad brightness change karein");
            }
        } catch (Exception e) {
            callback.onError("Brightness change nahi ho pa rahi: " + e.getMessage());
        }
    }

    /**
     * Set brightness percentage (0-100)
     */
    public void setBrightnessPercent(int percent, SystemCallback callback) {
        int brightness = (percent * 255) / 100;
        setBrightness(brightness, callback);
    }

    /**
     * Increase brightness
     */
    public void increaseBrightness(SystemCallback callback) {
        int current = getBrightness();
        int newBrightness = Math.min(255, current + 25);
        setBrightness(newBrightness, callback);
    }

    /**
     * Decrease brightness
     */
    public void decreaseBrightness(SystemCallback callback) {
        int current = getBrightness();
        int newBrightness = Math.max(0, current - 25);
        setBrightness(newBrightness, callback);
    }

    /**
     * Get current brightness
     */
    public int getBrightness() {
        try {
            return Settings.System.getInt(context.getContentResolver(), 
                Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return 128; // Default mid value
        }
    }

    /**
     * Set auto brightness
     */
    public void setAutoBrightness(boolean enabled, SystemCallback callback) {
        try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    enabled ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC :
                              Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                callback.onSuccess("Auto brightness " + (enabled ? "on" : "off") + " ho gayi!");
            } else {
                callback.onError("Write permission chahiye");
            }
        } catch (Exception e) {
            callback.onError("Auto brightness change nahi ho pa rahi: " + e.getMessage());
        }
    }

    // ==================== Volume Controls ====================

    /**
     * Set media volume (0-100)
     */
    public void setVolume(int percent, SystemCallback callback) {
        try {
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int volume = (percent * max) / 100;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Volume " + percent + "% ho gaya!");
        } catch (Exception e) {
            callback.onError("Volume change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Increase volume
     */
    public void increaseVolume(SystemCallback callback) {
        try {
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Volume badh gaya!");
        } catch (Exception e) {
            callback.onError("Volume up nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Decrease volume
     */
    public void decreaseVolume(SystemCallback callback) {
        try {
            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Volume kam ho gaya!");
        } catch (Exception e) {
            callback.onError("Volume down nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set silent mode
     */
    public void setSilentMode(SystemCallback callback) {
        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            callback.onSuccess("Silent mode on ho gaya!");
        } catch (Exception e) {
            callback.onError("Silent mode nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set vibrate mode
     */
    public void setVibrateMode(SystemCallback callback) {
        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            callback.onSuccess("Vibrate mode on ho gaya!");
        } catch (Exception e) {
            callback.onError("Vibrate mode nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set normal mode
     */
    public void setNormalMode(SystemCallback callback) {
        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            callback.onSuccess("Normal mode on ho gaya!");
        } catch (Exception e) {
            callback.onError("Normal mode nahi ho pa raha: " + e.getMessage());
        }
    }

    // ==================== Airplane Mode ====================

    /**
     * Toggle airplane mode
     */
    @SuppressLint("MissingPermission")
    public void toggleAirplaneMode(SystemCallback callback) {
        try {
            boolean isEnabled = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.putInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
            } else {
                Settings.System.putInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
            }

            // Broadcast to notify system
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", !isEnabled);
            context.sendBroadcast(intent);

            callback.onSuccess("Airplane mode " + (isEnabled ? "band" : "on") + " ho gaya!");
        } catch (Exception e) {
            callback.onError("Airplane mode change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Check airplane mode status
     */
    public boolean isAirplaneModeEnabled() {
        return Settings.Global.getInt(context.getContentResolver(),
            Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    // ==================== Do Not Disturb ====================

    /**
     * Enable Do Not Disturb
     */
    public void enableDND(SystemCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted()) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                    callback.onSuccess("Do Not Disturb on ho gaya!");
                } else {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    callback.onSuccess("Permission lene ke baad DND enable karein");
                }
            } else {
                callback.onError("DND feature available nahi hai");
            }
        } catch (Exception e) {
            callback.onError("DND enable nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Disable Do Not Disturb
     */
    public void disableDND(SystemCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                callback.onSuccess("Do Not Disturb band ho gaya!");
            }
        } catch (Exception e) {
            callback.onError("DND disable nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Toggle DND
     */
    public void toggleDND(SystemCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                disableDND(callback);
            } else {
                enableDND(callback);
            }
        }
    }

    // ==================== Mobile Data ====================

    /**
     * Toggle mobile data (requires special permission)
     */
    public void toggleMobileData(SystemCallback callback) {
        try {
            // Open settings as direct toggle requires system app permission
            Intent intent = new Intent(Settings.ACTION_NETWORK_AND_INTERNET_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Network settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Mobile data toggle nahi ho pa raha: " + e.getMessage());
        }
    }

    // ==================== Hotspot ====================

    /**
     * Open hotspot settings
     */
    public void openHotspotSettings(SystemCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_HOTSPOT_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Hotspot settings khul gayi!");
        } catch (Exception e) {
            // Fallback
            try {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Wireless settings khul gayi!");
            } catch (Exception e2) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        }
    }

    // ==================== Screen Controls ====================

    /**
     * Lock screen
     */
    public void lockScreen(SystemCallback callback) {
        try {
            // Requires Device Admin permission
            callback.onSuccess("Lock screen ke liye Device Admin enable karein");
        } catch (Exception e) {
            callback.onError("Lock nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Take screenshot
     */
    public void takeScreenshot(SystemCallback callback) {
        try {
            // Android 9+ screenshot API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Requires MediaProjection API
                callback.onSuccess("Screenshot ke liye permission chahiye");
            } else {
                // Hardware key simulation
                callback.onSuccess("Volume Down + Power button dabayein screenshot ke liye");
            }
        } catch (Exception e) {
            callback.onError("Screenshot nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set screen timeout
     */
    public void setScreenTimeout(int seconds, SystemCallback callback) {
        try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, seconds * 1000);
                callback.onSuccess("Screen timeout " + seconds + " seconds set ho gaya!");
            } else {
                callback.onError("Write permission chahiye");
            }
        } catch (Exception e) {
            callback.onError("Timeout change nahi ho pa raha: " + e.getMessage());
        }
    }

    // ==================== Rotation ====================

    /**
     * Enable auto rotation
     */
    public void enableAutoRotation(SystemCallback callback) {
        try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 1);
                callback.onSuccess("Auto rotation on ho gaya!");
            } else {
                callback.onError("Write permission chahiye");
            }
        } catch (Exception e) {
            callback.onError("Rotation change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Disable auto rotation
     */
    public void disableAutoRotation(SystemCallback callback) {
        try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 0);
                callback.onSuccess("Auto rotation band ho gaya!");
            } else {
                callback.onError("Write permission chahiye");
            }
        } catch (Exception e) {
            callback.onError("Rotation change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Toggle auto rotation
     */
    public void toggleAutoRotation(SystemCallback callback) {
        try {
            int current = Settings.System.getInt(context.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION);
            if (current == 1) {
                disableAutoRotation(callback);
            } else {
                enableAutoRotation(callback);
            }
        } catch (Exception e) {
            callback.onError("Rotation toggle nahi ho pa raha: " + e.getMessage());
        }
    }

    // ==================== Battery Saver ====================

    /**
     * Enable battery saver
     */
    public void enableBatterySaver(SystemCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Open battery settings
                Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Battery saver settings khul gayi!");
            }
        } catch (Exception e) {
            callback.onError("Battery saver enable nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Check power save mode
     */
    public boolean isPowerSaveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return powerManager.isPowerSaveMode();
        }
        return false;
    }

    // ==================== Display Settings ====================

    /**
     * Open display settings
     */
    public void openDisplaySettings(SystemCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Display settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    /**
     * Open sound settings
     */
    public void openSoundSettings(SystemCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Sound settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    // ==================== System Info ====================

    /**
     * Get system status
     */
    public SystemStatus getSystemStatus() {
        SystemStatus status = new SystemStatus();
        
        status.wifiEnabled = isWiFiEnabled();
        status.bluetoothEnabled = isBluetoothEnabled();
        status.torchOn = isTorchOn;
        status.brightness = getBrightness();
        status.volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        status.airplaneMode = isAirplaneModeEnabled();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            status.dndEnabled = notificationManager.getCurrentInterruptionFilter() 
                == NotificationManager.INTERRUPTION_FILTER_PRIORITY;
            status.powerSaveMode = powerManager.isPowerSaveMode();
        }

        // Battery level
        Intent batteryIntent = context.registerReceiver(null, 
            new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            status.batteryLevel = (int) ((level * 100) / (float) scale);
        }
        
        return status;
    }

    /**
     * Open settings
     */
    public void openSettings(SystemCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }
}
