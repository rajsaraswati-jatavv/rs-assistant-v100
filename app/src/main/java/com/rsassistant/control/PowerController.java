package com.rsassistant.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * PowerController - Battery and Power Management Controller
 * 
 * Supported Features:
 * - Battery percentage announcement
 * - Battery saver mode
 * - Charging status
 * - Power usage stats
 */
public class PowerController {

    private static final String TAG = "PowerController";
    private final Context context;
    private final BatteryManager batteryManager;
    private final PowerManager powerManager;
    
    private BroadcastReceiver batteryReceiver;
    private PowerCallback currentCallback;

    public interface PowerCallback {
        void onSuccess(String message);
        void onError(String error);
        void onBatteryStatus(BatteryStatus status);
    }

    public static class BatteryStatus {
        public int level;
        public int scale;
        public int percent;
        public int status;
        public int health;
        public int plugType;
        public int temperature;
        public int voltage;
        public String technology;
        public boolean isCharging;
        public boolean isFull;
        public boolean isPowerSaving;
        public long remainingTimeMinutes;
        public long remainingTimeHours;
    }

    public PowerController(Context context) {
        this.context = context;
        this.batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    // ==================== Battery Percentage ====================

    /**
     * Get battery percentage
     */
    public int getBatteryPercentage() {
        if (batteryManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        
        // Fallback
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);
        
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            
            if (level >= 0 && scale > 0) {
                return (int) ((level * 100.0) / scale);
            }
        }
        
        return -1;
    }

    /**
     * Get battery percentage announcement
     */
    public String getBatteryAnnouncement() {
        int percent = getBatteryPercentage();
        if (percent < 0) {
            return "Battery status nahi mil pa raha";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Battery ").append(percent).append(" percent hai. ");
        
        if (isCharging()) {
            sb.append("Charging ho rahi hai. ");
            if (percent >= 90) {
                sb.append("Jaldi full ho jayegi.");
            } else if (percent >= 50) {
                sb.append("Aur charging hogi.");
            }
        } else {
            if (percent <= 10) {
                sb.append("Battery bahut kam hai. Please charge karein.");
            } else if (percent <= 20) {
                sb.append("Battery kam hai. Charger laga lein.");
            } else if (percent >= 80) {
                sb.append("Battery achhi hai.");
            }
        }

        return sb.toString();
    }

    /**
     * Announce battery status
     */
    public void announceBatteryStatus(PowerCallback callback) {
        String announcement = getBatteryAnnouncement();
        callback.onSuccess(announcement);
    }

    /**
     * Get detailed battery status
     */
    public BatteryStatus getDetailedBatteryStatus() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        BatteryStatus status = new BatteryStatus();
        
        if (intent != null) {
            status.level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            status.scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            status.status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            status.health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            status.plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            status.temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            status.voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            status.technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
            
            if (status.level >= 0 && status.scale > 0) {
                status.percent = (int) ((status.level * 100.0) / status.scale);
            }
            
            status.isCharging = status.status == BatteryManager.BATTERY_STATUS_CHARGING ||
                               status.status == BatteryManager.BATTERY_STATUS_FULL;
            status.isFull = status.status == BatteryManager.BATTERY_STATUS_FULL;
        }
        
        // Power save mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            status.isPowerSaving = powerManager != null && powerManager.isPowerSaveMode();
        }
        
        // Remaining time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (batteryManager != null) {
                long remainingTime = batteryManager.computeChargeTimeRemaining();
                if (remainingTime > 0) {
                    status.remainingTimeMinutes = remainingTime / (1000 * 60);
                    status.remainingTimeHours = status.remainingTimeMinutes / 60;
                }
            }
        }
        
        return status;
    }

    /**
     * Get battery status with callback
     */
    public void getBatteryStatus(PowerCallback callback) {
        BatteryStatus status = getDetailedBatteryStatus();
        callback.onBatteryStatus(status);
    }

    // ==================== Charging Status ====================

    /**
     * Check if device is charging
     */
    public boolean isCharging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && batteryManager != null) {
            return batteryManager.isCharging();
        }
        
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                   status == BatteryManager.BATTERY_STATUS_FULL;
        }
        
        return false;
    }

    /**
     * Check if charging via USB
     */
    public boolean isUsbCharging() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            return plugType == BatteryManager.BATTERY_PLUGGED_USB;
        }
        
        return false;
    }

    /**
     * Check if charging via AC
     */
    public boolean isAcCharging() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            return plugType == BatteryManager.BATTERY_PLUGGED_AC;
        }
        
        return false;
    }

    /**
     * Check if charging wirelessly
     */
    public boolean isWirelessCharging() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            return plugType == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        }
        
        return false;
    }

    /**
     * Get charging type
     */
    public String getChargingType() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            
            switch (plugType) {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    return "AC Charger";
                case BatteryManager.BATTERY_PLUGGED_USB:
                    return "USB";
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    return "Wireless";
                default:
                    return "Unknown";
            }
        }
        
        return "Not Charging";
    }

    /**
     * Check if battery is full
     */
    public boolean isBatteryFull() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return status == BatteryManager.BATTERY_STATUS_FULL;
        }
        
        return false;
    }

    /**
     * Get charging announcement
     */
    public String getChargingAnnouncement() {
        if (!isCharging()) {
            int percent = getBatteryPercentage();
            return "Device charge nahi ho raha. Battery " + percent + " percent hai.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Device charge ho raha hai. ");
        
        String chargingType = getChargingType();
        sb.append("Charging type: ").append(chargingType).append(". ");
        
        int percent = getBatteryPercentage();
        sb.append("Battery ").append(percent).append(" percent hai. ");
        
        if (isBatteryFull()) {
            sb.append("Battery full ho gayi hai!");
        } else if (percent >= 90) {
            sb.append("Jaldi full ho jayegi.");
        }
        
        return sb.toString();
    }

    // ==================== Battery Saver Mode ====================

    /**
     * Check if battery saver is enabled
     */
    public boolean isBatterySaverEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return powerManager != null && powerManager.isPowerSaveMode();
        }
        return false;
    }

    /**
     * Enable battery saver mode
     */
    public void enableBatterySaver(PowerCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (isBatterySaverEnabled()) {
                callback.onSuccess("Battery saver already on hai!");
                return;
            }
            
            try {
                Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Battery saver settings khul gayi. Enable karein.");
            } catch (Exception e) {
                callback.onError("Battery saver settings nahi khul pa rahi: " + e.getMessage());
            }
        } else {
            callback.onError("Battery saver feature available nahi hai");
        }
    }

    /**
     * Disable battery saver mode
     */
    public void disableBatterySaver(PowerCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!isBatterySaverEnabled()) {
                callback.onSuccess("Battery saver already band hai!");
                return;
            }
            
            try {
                Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Battery saver settings khul gayi. Disable karein.");
            } catch (Exception e) {
                callback.onError("Battery saver settings nahi khul pa rahi: " + e.getMessage());
            }
        } else {
            callback.onError("Battery saver feature available nahi hai");
        }
    }

    /**
     * Toggle battery saver mode
     */
    public void toggleBatterySaver(PowerCallback callback) {
        if (isBatterySaverEnabled()) {
            disableBatterySaver(callback);
        } else {
            enableBatterySaver(callback);
        }
    }

    // ==================== Battery Health ====================

    /**
     * Get battery health status
     */
    public String getBatteryHealth() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    return "Good";
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    return "Overheat";
                case BatteryManager.BATTERY_HEALTH_DEAD:
                    return "Dead";
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                    return "Over Voltage";
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    return "Unspecified Failure";
                case BatteryManager.BATTERY_HEALTH_COLD:
                    return "Cold";
                default:
                    return "Unknown";
            }
        }
        
        return "Unknown";
    }

    /**
     * Get battery health announcement
     */
    public String getBatteryHealthAnnouncement() {
        String health = getBatteryHealth();
        int percent = getBatteryPercentage();
        
        StringBuilder sb = new StringBuilder();
        sb.append("Battery health: ").append(health).append(". ");
        
        switch (health) {
            case "Good":
                sb.append("Battery condition achhi hai.");
                break;
            case "Overheat":
                sb.append("Battery garam ho rahi hai. Please device thanda karein.");
                break;
            case "Dead":
                sb.append("Battery dead hai. Replacement ki zaroorat hai.");
                break;
            case "Over Voltage":
                sb.append("Over voltage detected. Charger check karein.");
                break;
            case "Cold":
                sb.append("Battery bahut thandi hai. Device garam karein.");
                break;
            default:
                sb.append("Health status detect nahi ho pa raha.");
        }
        
        return sb.toString();
    }

    // ==================== Temperature & Voltage ====================

    /**
     * Get battery temperature in Celsius
     */
    public float getBatteryTemperature() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            return temp / 10.0f; // Convert from tenths of degree
        }
        
        return -1;
    }

    /**
     * Get battery voltage in millivolts
     */
    public int getBatteryVoltage() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            return intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        }
        
        return -1;
    }

    /**
     * Get battery technology
     */
    public String getBatteryTechnology() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
        
        if (intent != null) {
            return intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        }
        
        return "Unknown";
    }

    /**
     * Check if battery is overheating
     */
    public boolean isBatteryOverheating() {
        float temp = getBatteryTemperature();
        return temp > 45; // Over 45°C is considered overheating
    }

    /**
     * Get temperature announcement
     */
    public String getTemperatureAnnouncement() {
        float temp = getBatteryTemperature();
        
        if (temp < 0) {
            return "Temperature nahi mil pa raha";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Battery temperature ").append(String.format("%.1f", temp)).append(" degree Celsius hai. ");
        
        if (temp < 20) {
            sb.append("Battery thandi hai.");
        } else if (temp < 35) {
            sb.append("Temperature normal hai.");
        } else if (temp < 45) {
            sb.append("Battery thodi garam hai.");
        } else {
            sb.append("Battery bahut garam hai! Please device rest karein.");
        }
        
        return sb.toString();
    }

    // ==================== Power Usage Stats ====================

    /**
     * Get estimated remaining battery time
     */
    public String getRemainingBatteryTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && batteryManager != null) {
            long remainingTime = batteryManager.computeChargeTimeRemaining();
            
            if (remainingTime > 0) {
                long hours = remainingTime / (1000 * 60 * 60);
                long minutes = (remainingTime / (1000 * 60)) % 60;
                
                return String.format("%d hours %d minutes remaining", hours, minutes);
            }
        }
        
        // Estimate based on percentage
        int percent = getBatteryPercentage();
        if (percent > 0) {
            // Rough estimate: 1% = ~15 minutes (varies greatly)
            long estimatedMinutes = (long)(percent * 15.0);
            long hours = estimatedMinutes / 60;
            long minutes = estimatedMinutes % 60;
            
            return String.format("Approximately %d hours %d minutes remaining", hours, minutes);
        }
        
        return "Remaining time calculate nahi ho pa raha";
    }

    /**
     * Get estimated charge time
     */
    public String getEstimatedChargeTime() {
        if (!isCharging()) {
            return "Device charge nahi ho raha";
        }
        
        int percent = getBatteryPercentage();
        int remaining = 100 - percent;
        
        // Rough estimate based on charging type
        int minutesPerPercent;
        if (isAcCharging()) {
            minutesPerPercent = 1; // Fast charging
        } else if (isWirelessCharging()) {
            minutesPerPercent = 3; // Slower wireless
        } else {
            minutesPerPercent = 2; // USB charging
        }
        
        long totalMinutes = (long)(remaining * minutesPerPercent * 1.2); // 20% buffer
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        
        return String.format("%d hours %d minutes to full charge", hours, minutes);
    }

    /**
     * Get power usage summary
     */
    public String getPowerUsageSummary() {
        StringBuilder sb = new StringBuilder();
        
        // Battery level
        int percent = getBatteryPercentage();
        sb.append("Battery: ").append(percent).append("%\n");
        
        // Charging status
        sb.append("Status: ");
        if (isCharging()) {
            sb.append("Charging (").append(getChargingType()).append(")\n");
            sb.append("Time to full: ").append(getEstimatedChargeTime()).append("\n");
        } else {
            sb.append("Discharging\n");
            sb.append("Estimated time: ").append(getRemainingBatteryTime()).append("\n");
        }
        
        // Battery saver
        sb.append("Battery Saver: ").append(isBatterySaverEnabled() ? "On" : "Off").append("\n");
        
        // Health
        sb.append("Health: ").append(getBatteryHealth()).append("\n");
        
        // Temperature
        float temp = getBatteryTemperature();
        if (temp > 0) {
            sb.append("Temperature: ").append(String.format("%.1f", temp)).append("°C");
        }
        
        return sb.toString();
    }

    // ==================== Battery Monitoring ====================

    /**
     * Start monitoring battery changes
     */
    public void startBatteryMonitoring(PowerCallback callback) {
        this.currentCallback = callback;
        
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (currentCallback != null) {
                    BatteryStatus status = getDetailedBatteryStatus();
                    currentCallback.onBatteryStatus(status);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryReceiver, filter);
        
        callback.onSuccess("Battery monitoring shuru ho gayi!");
    }

    /**
     * Stop monitoring battery changes
     */
    public void stopBatteryMonitoring() {
        if (batteryReceiver != null) {
            try {
                context.unregisterReceiver(batteryReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }
            batteryReceiver = null;
        }
        currentCallback = null;
    }

    // ==================== Screen & Power ====================

    /**
     * Check if screen is on
     */
    public boolean isScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return powerManager != null && powerManager.isInteractive();
        }
        return false;
    }

    /**
     * Open battery settings
     */
    public void openBatterySettings(PowerCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Battery settings khul gayi!");
        } catch (Exception e) {
            // Fallback
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Settings khul gayi!");
            } catch (Exception e2) {
                callback.onError("Settings nahi khul pa rahi: " + e2.getMessage());
            }
        }
    }

    /**
     * Get battery capacity (if available)
     */
    public long getBatteryCapacity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && batteryManager != null) {
            return batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        }
        return -1;
    }

    /**
     * Get current now (microamperes)
     */
    public long getCurrentNow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && batteryManager != null) {
            return batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        }
        return -1;
    }

    /**
     * Get power profile summary
     */
    public String getPowerProfile() {
        StringBuilder sb = new StringBuilder();
        
        // Current
        long current = getCurrentNow();
        if (current > 0) {
            sb.append("Current: ").append(current / 1000).append(" mA\n");
        }
        
        // Voltage
        int voltage = getBatteryVoltage();
        if (voltage > 0) {
            sb.append("Voltage: ").append(voltage).append(" mV\n");
        }
        
        // Technology
        String tech = getBatteryTechnology();
        if (tech != null && !tech.isEmpty()) {
            sb.append("Technology: ").append(tech);
        }
        
        return sb.length() > 0 ? sb.toString() : "Power profile available nahi hai";
    }
}
