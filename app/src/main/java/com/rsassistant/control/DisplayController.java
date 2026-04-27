package com.rsassistant.control;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

/**
 * DisplayController - Display and Screen Control Manager
 * 
 * Supported Features:
 * - Brightness control by voice
 * - Screen timeout settings
 * - Night mode toggle
 * - Blue light filter
 * - Reading mode
 */
public class DisplayController {

    private static final String TAG = "DisplayController";
    private final Context context;
    private final ContentResolver contentResolver;
    private final WindowManager windowManager;
    private final DisplayManager displayManager;

    // Brightness range
    private static final int MIN_BRIGHTNESS = 0;
    private static final int MAX_BRIGHTNESS = 255;

    public interface DisplayCallback {
        void onSuccess(String message);
        void onError(String error);
        void onStatus(DisplayStatus status);
    }

    public static class DisplayStatus {
        public int brightness;
        public int brightnessPercent;
        public boolean autoBrightness;
        public int screenTimeout;
        public boolean nightMode;
        public boolean adaptiveBrightness;
    }

    public DisplayController(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    }

    // ==================== Brightness Controls ====================

    /**
     * Set brightness (0-255)
     */
    public void setBrightness(int brightness, DisplayCallback callback) {
        try {
            // Ensure brightness is in valid range
            brightness = Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, brightness));
            
            if (Settings.System.canWrite(context)) {
                // Disable auto brightness first
                setAutoBrightnessEnabled(false);
                
                Settings.System.putInt(contentResolver, 
                    Settings.System.SCREEN_BRIGHTNESS, brightness);
                
                int percent = (brightness * 100) / MAX_BRIGHTNESS;
                callback.onSuccess("Brightness " + percent + "% set ho gayi!");
            } else {
                requestWritePermission(callback);
            }
        } catch (Exception e) {
            callback.onError("Brightness change nahi ho pa rahi: " + e.getMessage());
        }
    }

    /**
     * Set brightness percentage (0-100)
     */
    public void setBrightnessPercent(int percent, DisplayCallback callback) {
        percent = Math.max(0, Math.min(100, percent));
        int brightness = (percent * MAX_BRIGHTNESS) / 100;
        setBrightness(brightness, callback);
    }

    /**
     * Increase brightness by 10%
     */
    public void increaseBrightness(DisplayCallback callback) {
        int current = getBrightness();
        int newBrightness = Math.min(MAX_BRIGHTNESS, current + 25);
        setBrightness(newBrightness, callback);
    }

    /**
     * Decrease brightness by 10%
     */
    public void decreaseBrightness(DisplayCallback callback) {
        int current = getBrightness();
        int newBrightness = Math.max(MIN_BRIGHTNESS, current - 25);
        setBrightness(newBrightness, callback);
    }

    /**
     * Set brightness to maximum
     */
    public void setMaxBrightness(DisplayCallback callback) {
        setBrightness(MAX_BRIGHTNESS, callback);
    }

    /**
     * Set brightness to minimum
     */
    public void setMinBrightness(DisplayCallback callback) {
        setBrightness(MIN_BRIGHTNESS + 10, callback); // Slightly above 0 for visibility
    }

    /**
     * Get current brightness
     */
    public int getBrightness() {
        try {
            return Settings.System.getInt(contentResolver, 
                Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return 128; // Default mid value
        }
    }

    /**
     * Get brightness as percentage
     */
    public int getBrightnessPercent() {
        return (getBrightness() * 100) / MAX_BRIGHTNESS;
    }

    /**
     * Set auto brightness
     */
    public void setAutoBrightness(boolean enabled, DisplayCallback callback) {
        try {
            if (Settings.System.canWrite(context)) {
                setAutoBrightnessEnabled(enabled);
                callback.onSuccess("Auto brightness " + (enabled ? "on" : "off") + " ho gaya!");
            } else {
                requestWritePermission(callback);
            }
        } catch (Exception e) {
            callback.onError("Auto brightness change nahi ho pa rahi: " + e.getMessage());
        }
    }

    /**
     * Toggle auto brightness
     */
    public void toggleAutoBrightness(DisplayCallback callback) {
        boolean current = isAutoBrightnessEnabled();
        setAutoBrightness(!current, callback);
    }

    /**
     * Check if auto brightness is enabled
     */
    public boolean isAutoBrightnessEnabled() {
        try {
            int mode = Settings.System.getInt(contentResolver, 
                Settings.System.SCREEN_BRIGHTNESS_MODE);
            return mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    private void setAutoBrightnessEnabled(boolean enabled) {
        Settings.System.putInt(contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            enabled ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC :
                      Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    // ==================== Screen Timeout Controls ====================

    /**
     * Set screen timeout in seconds
     */
    public void setScreenTimeout(int seconds, DisplayCallback callback) {
        try {
            if (Settings.System.canWrite(context)) {
                int milliseconds = seconds * 1000;
                Settings.System.putInt(contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT, milliseconds);
                callback.onSuccess("Screen timeout " + seconds + " seconds set ho gaya!");
            } else {
                requestWritePermission(callback);
            }
        } catch (Exception e) {
            callback.onError("Screen timeout change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set screen timeout to preset values
     */
    public void setScreenTimeoutPreset(String preset, DisplayCallback callback) {
        int seconds;
        switch (preset.toLowerCase()) {
            case "15s":
            case "15 seconds":
                seconds = 15;
                break;
            case "30s":
            case "30 seconds":
                seconds = 30;
                break;
            case "1m":
            case "1 minute":
            case "60s":
            case "60 seconds":
                seconds = 60;
                break;
            case "2m":
            case "2 minutes":
            case "120s":
            case "120 seconds":
                seconds = 120;
                break;
            case "5m":
            case "5 minutes":
            case "300s":
            case "300 seconds":
                seconds = 300;
                break;
            case "10m":
            case "10 minutes":
            case "600s":
            case "600 seconds":
                seconds = 600;
                break;
            case "30m":
            case "30 minutes":
                seconds = 1800;
                break;
            case "never":
            case "always on":
                seconds = Integer.MAX_VALUE / 1000;
                break;
            default:
                seconds = 30;
        }
        setScreenTimeout(seconds, callback);
    }

    /**
     * Get screen timeout in seconds
     */
    public int getScreenTimeout() {
        try {
            return Settings.System.getInt(contentResolver, 
                Settings.System.SCREEN_OFF_TIMEOUT) / 1000;
        } catch (Settings.SettingNotFoundException e) {
            return 30; // Default
        }
    }

    /**
     * Get screen timeout in milliseconds
     */
    public int getScreenTimeoutMillis() {
        try {
            return Settings.System.getInt(contentResolver, 
                Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            return 30000; // Default
        }
    }

    // ==================== Night Mode Controls ====================

    /**
     * Enable night mode
     */
    public void enableNightMode(DisplayCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
            setNightMode(Settings.System.NIGHT_MODE_YES, callback);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31
            setUiNightMode(callback);
        } else {
            // Try vendor-specific night mode
            setVendorNightMode(true, callback);
        }
    }

    /**
     * Disable night mode
     */
    public void disableNightMode(DisplayCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setNightMode(Settings.System.NIGHT_MODE_NO, callback);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            clearUiNightMode(callback);
        } else {
            setVendorNightMode(false, callback);
        }
    }

    /**
     * Toggle night mode
     */
    public void toggleNightMode(DisplayCallback callback) {
        if (isNightModeEnabled()) {
            disableNightMode(callback);
        } else {
            enableNightMode(callback);
        }
    }

    /**
     * Set auto night mode
     */
    public void setAutoNightMode(DisplayCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setNightMode(Settings.System.NIGHT_MODE_AUTO, callback);
        } else {
            callback.onSuccess("Auto night mode setting open karein");
        }
    }

    /**
     * Check if night mode is enabled
     */
    public boolean isNightModeEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int mode = context.getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                int nightMode = Settings.System.getInt(contentResolver, 
                    Settings.System.NIGHT_MODE);
                return nightMode == Settings.System.NIGHT_MODE_YES;
            } catch (Settings.SettingNotFoundException e) {
                return false;
            }
        }
        
        return false;
    }

    private void setNightMode(int mode, DisplayCallback callback) {
        try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(contentResolver, 
                    Settings.System.NIGHT_MODE, mode);
                callback.onSuccess("Night mode " + 
                    (mode == Settings.System.NIGHT_MODE_YES ? "on" : 
                     mode == Settings.System.NIGHT_MODE_NO ? "off" : "auto") + 
                    " ho gaya!");
            } else {
                requestWritePermission(callback);
            }
        } catch (Exception e) {
            callback.onError("Night mode change nahi ho pa raha: " + e.getMessage());
        }
    }

    private void setUiNightMode(DisplayCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Display settings khul gayi. Dark mode enable karein.");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    private void clearUiNightMode(DisplayCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Display settings khul gayi. Dark mode disable karein.");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    private void setVendorNightMode(boolean enabled, DisplayCallback callback) {
        // Try Samsung
        try {
            Settings.System.putInt(contentResolver, "night_mode", enabled ? 1 : 0);
            callback.onSuccess("Night mode " + (enabled ? "on" : "off") + " ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Try Xiaomi
        try {
            Settings.System.putInt(contentResolver, "screen_paper_mode_enabled", enabled ? 1 : 0);
            callback.onSuccess("Reading mode " + (enabled ? "on" : "off") + " ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Fallback to settings
        openDisplaySettings(callback);
    }

    // ==================== Blue Light Filter ====================

    /**
     * Enable blue light filter / eye comfort mode
     */
    public void enableBlueLightFilter(DisplayCallback callback) {
        // Try different vendor implementations
        
        // Samsung Eye Comfort Shield
        try {
            Settings.System.putInt(contentResolver, "eye_comfort_mode", 1);
            callback.onSuccess("Eye comfort mode on ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Xiaomi Reading Mode
        try {
            Settings.System.putInt(contentResolver, "screen_paper_mode_enabled", 1);
            callback.onSuccess("Reading mode on ho gaya!");
            return;
        } catch (Exception ignored) {}

        // OnePlus Night Mode
        try {
            Settings.System.putInt(contentResolver, "oem_night_mode", 1);
            callback.onSuccess("Night mode on ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Huawei Eye Comfort
        try {
            Settings.System.putInt(contentResolver, "hw_screen_color_temp_enable", 1);
            callback.onSuccess("Eye comfort on ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Stock Android night light (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            openDisplaySettings(new DisplayCallback() {
                @Override
                public void onSuccess(String message) {
                    callback.onSuccess("Display settings khul gayi. Night Light enable karein.");
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }

                @Override
                public void onStatus(DisplayStatus status) {}
            });
        } else {
            callback.onError("Blue light filter device par available nahi hai");
        }
    }

    /**
     * Disable blue light filter
     */
    public void disableBlueLightFilter(DisplayCallback callback) {
        // Samsung
        try {
            Settings.System.putInt(contentResolver, "eye_comfort_mode", 0);
            callback.onSuccess("Eye comfort mode band ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Xiaomi
        try {
            Settings.System.putInt(contentResolver, "screen_paper_mode_enabled", 0);
            callback.onSuccess("Reading mode band ho gaya!");
            return;
        } catch (Exception ignored) {}

        // OnePlus
        try {
            Settings.System.putInt(contentResolver, "oem_night_mode", 0);
            callback.onSuccess("Night mode band ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Huawei
        try {
            Settings.System.putInt(contentResolver, "hw_screen_color_temp_enable", 0);
            callback.onSuccess("Eye comfort band ho gaya!");
            return;
        } catch (Exception ignored) {}

        openDisplaySettings(callback);
    }

    /**
     * Toggle blue light filter
     */
    public void toggleBlueLightFilter(DisplayCallback callback) {
        // Since detection is vendor-specific, just toggle
        enableBlueLightFilter(callback);
    }

    // ==================== Reading Mode ====================

    /**
     * Enable reading mode
     */
    public void enableReadingMode(DisplayCallback callback) {
        // Try Xiaomi reading mode
        try {
            Settings.System.putInt(contentResolver, "screen_paper_mode_enabled", 1);
            Settings.System.putInt(contentResolver, "reading_mode", 1);
            callback.onSuccess("Reading mode on ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Try Samsung reading mode
        try {
            Settings.System.putInt(contentResolver, "reading_mode_enabled", 1);
            callback.onSuccess("Reading mode on ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Set optimal brightness and color for reading
        setBrightnessPercent(40, new DisplayCallback() {
            @Override
            public void onSuccess(String message) {
                enableBlueLightFilter(callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onStatus(DisplayStatus status) {}
        });
    }

    /**
     * Disable reading mode
     */
    public void disableReadingMode(DisplayCallback callback) {
        // Try Xiaomi
        try {
            Settings.System.putInt(contentResolver, "screen_paper_mode_enabled", 0);
            Settings.System.putInt(contentResolver, "reading_mode", 0);
            callback.onSuccess("Reading mode band ho gaya!");
            return;
        } catch (Exception ignored) {}

        // Try Samsung
        try {
            Settings.System.putInt(contentResolver, "reading_mode_enabled", 0);
            callback.onSuccess("Reading mode band ho gaya!");
            return;
        } catch (Exception ignored) {}

        disableBlueLightFilter(callback);
    }

    // ==================== Adaptive Brightness ====================

    /**
     * Enable adaptive brightness
     */
    public void enableAdaptiveBrightness(DisplayCallback callback) {
        setAutoBrightness(true, callback);
    }

    /**
     * Disable adaptive brightness
     */
    public void disableAdaptiveBrightness(DisplayCallback callback) {
        setAutoBrightness(false, callback);
    }

    // ==================== Screen Rotation ====================

    /**
     * Enable auto rotation
     */
    public void enableAutoRotation(DisplayCallback callback) {
        try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION, 1);
                callback.onSuccess("Auto rotation on ho gaya!");
            } else {
                requestWritePermission(callback);
            }
        } catch (Exception e) {
            callback.onError("Rotation change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Disable auto rotation
     */
    public void disableAutoRotation(DisplayCallback callback) {
        try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION, 0);
                callback.onSuccess("Auto rotation band ho gaya!");
            } else {
                requestWritePermission(callback);
            }
        } catch (Exception e) {
            callback.onError("Rotation change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Toggle auto rotation
     */
    public void toggleAutoRotation(DisplayCallback callback) {
        if (isAutoRotationEnabled()) {
            disableAutoRotation(callback);
        } else {
            enableAutoRotation(callback);
        }
    }

    /**
     * Check if auto rotation is enabled
     */
    public boolean isAutoRotationEnabled() {
        try {
            return Settings.System.getInt(contentResolver, 
                Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    // ==================== Display Settings ====================

    /**
     * Open display settings
     */
    public void openDisplaySettings(DisplayCallback callback) {
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
     * Get display status
     */
    public DisplayStatus getDisplayStatus() {
        DisplayStatus status = new DisplayStatus();
        status.brightness = getBrightness();
        status.brightnessPercent = getBrightnessPercent();
        status.autoBrightness = isAutoBrightnessEnabled();
        status.screenTimeout = getScreenTimeout();
        status.nightMode = isNightModeEnabled();
        status.adaptiveBrightness = isAutoBrightnessEnabled();
        return status;
    }

    // ==================== Utility Methods ====================

    /**
     * Request write settings permission
     */
    private void requestWritePermission(DisplayCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Permission lene ke baad setting change karein");
        } catch (Exception e) {
            callback.onError("Permission request nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Check if can write system settings
     */
    public boolean canWriteSettings() {
        return Settings.System.canWrite(context);
    }

    /**
     * Get screen width
     */
    public int getScreenWidth() {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * Get screen height
     */
    public int getScreenHeight() {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * Get screen density
     */
    public float getScreenDensity() {
        return context.getResources().getDisplayMetrics().density;
    }

    /**
     * Check if device is in landscape mode
     */
    public boolean isLandscape() {
        int orientation = context.getResources().getConfiguration().orientation;
        return orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Wake up screen
     */
    public void wakeUpScreen(DisplayCallback callback) {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) 
                context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isInteractive()) {
                android.os.PowerManager.WakeLock wl = pm.newWakeLock(
                    android.os.PowerManager.FULL_WAKE_LOCK | 
                    android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP | 
                    android.os.PowerManager.ON_AFTER_RELEASE, 
                    "RSAssistant:WakeLock");
                wl.acquire(5000);
                wl.release();
                callback.onSuccess("Screen on ho gaya!");
            } else {
                callback.onSuccess("Screen already on hai");
            }
        } catch (Exception e) {
            callback.onError("Screen wake nahi ho pa raha: " + e.getMessage());
        }
    }
}
