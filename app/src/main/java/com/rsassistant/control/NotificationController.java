package com.rsassistant.control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationController - Notification Control and Management
 * 
 * Supported Features:
 * - Read notifications aloud
 * - Clear all notifications
 * - Notification listener service
 */
public class NotificationController {

    private static final String TAG = "NotificationController";
    private static final String LISTENER_SERVICE_CLASS = "com.rsassistant.control.NotificationController$NotificationListener";
    
    private final Context context;
    private final NotificationManager notificationManager;
    private static NotificationController instance;
    private static List<NotificationInfo> cachedNotifications = new ArrayList<>();
    private static NotificationListener notificationListenerInstance;

    public interface NotificationCallback {
        void onSuccess(String message);
        void onError(String error);
        void onNotifications(List<NotificationInfo> notifications);
        void onNotificationPosted(NotificationInfo notification);
        void onNotificationRemoved(NotificationInfo notification);
    }

    public static class NotificationInfo {
        public String packageName;
        public String appName;
        public String title;
        public String text;
        public String bigText;
        public String summaryText;
        public String subText;
        public long postTime;
        public int id;
        public String tag;
        public boolean ongoing;
        public boolean clearable;
        public int priority;
        public String category;
    }

    public NotificationController(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        instance = this;
    }

    /**
     * Get singleton instance
     */
    public static NotificationController getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationController(context);
        }
        return instance;
    }

    // ==================== Notification Listener Service ====================

    /**
     * Check if notification listener permission is granted
     */
    public boolean isNotificationListenerEnabled() {
        String packageName = context.getPackageName();
        String flat = Settings.Secure.getString(context.getContentResolver(), 
            "enabled_notification_listeners");
        
        if (!TextUtils.isEmpty(flat)) {
            String[] names = flat.split(":");
            for (String name : names) {
                ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (packageName.equals(cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Request notification listener permission
     */
    public void requestNotificationListenerPermission(NotificationCallback callback) {
        if (isNotificationListenerEnabled()) {
            callback.onSuccess("Notification listener already enabled hai!");
            return;
        }
        
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Notification access settings khul gayi. Enable karein.");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    /**
     * Enable notification listener service
     */
    public void enableNotificationListener(NotificationCallback callback) {
        if (isNotificationListenerEnabled()) {
            callback.onSuccess("Notification listener enabled hai!");
            return;
        }
        
        requestNotificationListenerPermission(callback);
    }

    /**
     * Open notification settings
     */
    public void openNotificationSettings(NotificationCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Notification settings khul gayi!");
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("App settings khul gayi!");
            } catch (Exception e2) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        }
    }

    // ==================== Read Notifications ====================

    /**
     * Check if can read notifications
     */
    public boolean canReadNotifications() {
        return isNotificationListenerEnabled();
    }

    /**
     * Get all active notifications
     */
    public List<NotificationInfo> getActiveNotifications() {
        if (!isNotificationListenerEnabled()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(cachedNotifications);
    }

    /**
     * Read notifications aloud - returns text to speak
     */
    public void getNotificationsToRead(NotificationCallback callback) {
        if (!isNotificationListenerEnabled()) {
            callback.onError("Notification access enabled nahi hai");
            return;
        }
        
        List<NotificationInfo> notifications = getActiveNotifications();
        
        if (notifications.isEmpty()) {
            callback.onSuccess("Koi notification nahi hai");
            return;
        }
        
        callback.onNotifications(notifications);
    }

    /**
     * Get notification summary text for TTS
     */
    public String getNotificationSummaryText() {
        List<NotificationInfo> notifications = getActiveNotifications();
        
        if (notifications.isEmpty()) {
            return "Koi notification nahi hai";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Apke paas ").append(notifications.size()).append(" notifications hain. ");
        
        // Group by app
        int count = 0;
        for (NotificationInfo info : notifications) {
            if (count >= 5) { // Limit to 5 to avoid too long speech
                int remaining = notifications.size() - count;
                sb.append("Aur ").append(remaining).append(" aur notifications hain. ");
                break;
            }
            
            if (info.appName != null) {
                sb.append(info.appName);
            }
            if (info.title != null && !info.title.isEmpty()) {
                sb.append(": ").append(info.title);
            }
            if (info.text != null && !info.text.isEmpty()) {
                sb.append(" - ").append(info.text);
            }
            sb.append(". ");
            count++;
        }
        
        return sb.toString();
    }

    /**
     * Read notifications from a specific app
     */
    public List<NotificationInfo> getNotificationsFromPackage(String packageName) {
        List<NotificationInfo> result = new ArrayList<>();
        
        for (NotificationInfo info : cachedNotifications) {
            if (packageName.equals(info.packageName)) {
                result.add(info);
            }
        }
        
        return result;
    }

    /**
     * Get unread notifications count
     */
    public int getUnreadNotificationCount() {
        return cachedNotifications.size();
    }

    // ==================== Clear Notifications ====================

    /**
     * Clear all notifications
     */
    public void clearAllNotifications(NotificationCallback callback) {
        if (!isNotificationListenerEnabled()) {
            callback.onError("Notification access enabled nahi hai");
            return;
        }
        
        if (notificationListenerInstance != null) {
            notificationListenerInstance.clearAllNotifications();
            cachedNotifications.clear();
            callback.onSuccess("Sab notifications clear ho gayi!");
        } else {
            // Try to clear via notification manager (requires special permission)
            try {
                notificationManager.cancelAll();
                callback.onSuccess("Notifications clear ki gayi!");
            } catch (Exception e) {
                callback.onError("Notifications clear nahi ho pa rahi: " + e.getMessage());
            }
        }
    }

    /**
     * Clear notification from specific app
     */
    public void clearNotificationsFromPackage(String packageName, NotificationCallback callback) {
        if (!isNotificationListenerEnabled()) {
            callback.onError("Notification access enabled nahi hai");
            return;
        }
        
        int cleared = 0;
        
        if (notificationListenerInstance != null) {
            StatusBarNotification[] sbns = notificationListenerInstance.getActiveNotifications();
            for (StatusBarNotification sbn : sbns) {
                if (packageName.equals(sbn.getPackageName())) {
                    notificationListenerInstance.cancelNotification(sbn.getKey());
                    cleared++;
                }
            }
        }
        
        // Remove from cache
        cachedNotifications.removeIf(info -> packageName.equals(info.packageName));
        
        if (cleared > 0) {
            callback.onSuccess(cleared + " notifications clear ho gayi!");
        } else {
            callback.onSuccess("Koi notification clear nahi hui");
        }
    }

    /**
     * Clear specific notification
     */
    public void clearNotification(int id, String packageName, NotificationCallback callback) {
        if (!isNotificationListenerEnabled()) {
            callback.onError("Notification access enabled nahi hai");
            return;
        }
        
        if (notificationListenerInstance != null) {
            StatusBarNotification[] sbns = notificationListenerInstance.getActiveNotifications();
            for (StatusBarNotification sbn : sbns) {
                if (sbn.getId() == id && packageName.equals(sbn.getPackageName())) {
                    notificationListenerInstance.cancelNotification(sbn.getKey());
                    break;
                }
            }
        }
        
        // Remove from cache
        cachedNotifications.removeIf(info -> info.id == id && packageName.equals(info.packageName));
        
        callback.onSuccess("Notification clear ho gayi!");
    }

    // ==================== Notification Channels ====================

    /**
     * Get notification channels for app
     */
    public List<NotificationChannel> getNotificationChannels() {
        List<NotificationChannel> channels = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channels.addAll(notificationManager.getNotificationChannels());
        }
        
        return channels;
    }

    /**
     * Open channel settings
     */
    public void openChannelSettings(String channelId, NotificationCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Channel settings khul gayi!");
            } catch (Exception e) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        } else {
            openNotificationSettings(callback);
        }
    }

    // ==================== DND and Priority ====================

    /**
     * Check if notifications are blocked for app
     */
    public boolean areNotificationsBlocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return !notificationManager.areNotificationsEnabled();
        }
        return false;
    }

    /**
     * Check if notifications are enabled
     */
    public boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Request notification permission
     */
    public void requestNotificationPermission(NotificationCallback callback) {
        if (areNotificationsEnabled()) {
            callback.onSuccess("Notifications already enabled hain!");
            return;
        }
        
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Notification settings khul gayi. Enable karein.");
        } catch (Exception e) {
            openNotificationSettings(callback);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Get app name from package
     */
    private String getAppName(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    /**
     * Parse StatusBarNotification to NotificationInfo
     */
    private NotificationInfo parseNotification(StatusBarNotification sbn) {
        NotificationInfo info = new NotificationInfo();
        
        info.packageName = sbn.getPackageName();
        info.appName = getAppName(sbn.getPackageName());
        info.id = sbn.getId();
        info.tag = sbn.getTag();
        info.postTime = sbn.getPostTime();
        info.ongoing = sbn.isOngoing();
        info.clearable = sbn.isClearable();
        
        Notification notification = sbn.getNotification();
        if (notification != null) {
            info.priority = notification.priority;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                info.category = notification.category;
            }
            
            Bundle extras = notification.extras;
            if (extras != null) {
                CharSequence titleSeq = extras.getCharSequence(Notification.EXTRA_TITLE);
                if (titleSeq != null) {
                    info.title = titleSeq.toString();
                }
                
                CharSequence textSeq = extras.getCharSequence(Notification.EXTRA_TEXT);
                if (textSeq != null) {
                    info.text = textSeq.toString();
                }
                
                CharSequence bigTextSeq = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
                if (bigTextSeq != null) {
                    info.bigText = bigTextSeq.toString();
                }
                
                CharSequence summarySeq = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
                if (summarySeq != null) {
                    info.summaryText = summarySeq.toString();
                }
                
                CharSequence subSeq = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
                if (subSeq != null) {
                    info.subText = subSeq.toString();
                }
            }
        }
        
        return info;
    }

    /**
     * Update cached notifications
     */
    public static void updateCachedNotifications(List<NotificationInfo> notifications) {
        cachedNotifications.clear();
        cachedNotifications.addAll(notifications);
    }

    /**
     * Add notification to cache
     */
    public static void addNotificationToCache(NotificationInfo info) {
        cachedNotifications.add(info);
    }

    /**
     * Remove notification from cache
     */
    public static void removeNotificationFromCache(String key) {
        // Remove by matching id and package
        cachedNotifications.removeIf(info -> 
            (info.packageName + ":" + info.id).equals(key));
    }

    // ==================== Notification Listener Service ====================

    /**
     * Notification Listener Service Implementation
     */
    public static class NotificationListener extends NotificationListenerService {
        
        private static final String TAG = "NotificationListener";
        private NotificationCallback callback;
        
        @Override
        public void onCreate() {
            super.onCreate();
            notificationListenerInstance = this;
            Log.d(TAG, "Notification Listener Service created");
        }
        
        @Override
        public void onListenerConnected() {
            super.onListenerConnected();
            Log.d(TAG, "Notification Listener connected");
            
            // Cache initial notifications
            updateCachedNotifications();
        }
        
        @Override
        public void onNotificationPosted(StatusBarNotification sbn) {
            Log.d(TAG, "Notification posted: " + sbn.getPackageName());
            
            NotificationInfo info = instance != null ? 
                instance.parseNotification(sbn) : parseNotificationStatic(sbn);
            
            addNotificationToCache(info);
            
            if (callback != null && instance != null) {
                callback.onNotificationPosted(info);
            }
        }
        
        @Override
        public void onNotificationRemoved(StatusBarNotification sbn) {
            Log.d(TAG, "Notification removed: " + sbn.getPackageName());
            
            String key = sbn.getKey();
            removeNotificationFromCache(key);
            
            NotificationInfo info = instance != null ? 
                instance.parseNotification(sbn) : parseNotificationStatic(sbn);
            
            if (callback != null && instance != null) {
                callback.onNotificationRemoved(info);
            }
        }
        
        @Override
        public void onListenerDisconnected() {
            super.onListenerDisconnected();
            Log.d(TAG, "Notification Listener disconnected");
            notificationListenerInstance = null;
        }
        
        @Override
        public void onDestroy() {
            super.onDestroy();
            notificationListenerInstance = null;
        }
        
        /**
         * Update cached notifications
         */
        private void updateCachedNotifications() {
            StatusBarNotification[] sbns = getActiveNotifications();
            List<NotificationInfo> notifications = new ArrayList<>();
            
            for (StatusBarNotification sbn : sbns) {
                NotificationInfo info = instance != null ? 
                    instance.parseNotification(sbn) : parseNotificationStatic(sbn);
                notifications.add(info);
            }
            
            NotificationController.updateCachedNotifications(notifications);
        }
        
        /**
         * Parse notification statically
         */
        private static NotificationInfo parseNotificationStatic(StatusBarNotification sbn) {
            NotificationInfo info = new NotificationInfo();
            info.packageName = sbn.getPackageName();
            info.id = sbn.getId();
            info.tag = sbn.getTag();
            info.postTime = sbn.getPostTime();
            info.ongoing = sbn.isOngoing();
            info.clearable = sbn.isClearable();
            return info;
        }
        
        /**
         * Set callback for notification events
         */
        public void setCallback(NotificationCallback callback) {
            this.callback = callback;
        }
        
        /**
         * Clear all notifications
         */
        public void clearAllNotifications() {
            cancelAllNotifications();
        }
    }

    /**
     * Set notification callback
     */
    public void setNotificationCallback(NotificationCallback callback) {
        if (notificationListenerInstance != null) {
            notificationListenerInstance.setCallback(callback);
        }
    }

    /**
     * Check if notification listener service is running
     */
    public boolean isNotificationListenerServiceRunning() {
        return notificationListenerInstance != null;
    }

    /**
     * Refresh notification cache
     */
    public void refreshNotificationCache(NotificationCallback callback) {
        if (!isNotificationListenerEnabled()) {
            callback.onError("Notification access enabled nahi hai");
            return;
        }
        
        if (notificationListenerInstance != null) {
            StatusBarNotification[] sbns = notificationListenerInstance.getActiveNotifications();
            List<NotificationInfo> notifications = new ArrayList<>();
            
            for (StatusBarNotification sbn : sbns) {
                notifications.add(parseNotification(sbn));
            }
            
            updateCachedNotifications(notifications);
            callback.onNotifications(notifications);
        } else {
            callback.onError("Notification listener service running nahi hai");
        }
    }

    /**
     * Get notification importance level
     */
    public int getNotificationImportance(String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<NotificationChannel> channels = notificationManager.getNotificationChannels();
            for (NotificationChannel channel : channels) {
                // This is a simplified check
                return channel.getImportance();
            }
        }
        return NotificationManager.IMPORTANCE_DEFAULT;
    }
}
