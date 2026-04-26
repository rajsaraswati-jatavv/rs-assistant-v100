package com.rsassistant.service;

import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationListenerService extends NotificationListenerService {

    private static NotificationListenerService instance;

    public static NotificationListenerService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Handle new notification
        String packageName = sbn.getPackageName();
        Bundle extras = sbn.getNotification().extras;

        String title = extras.getString("android.title");
        CharSequence text = extras.getCharSequence("android.text");

        // Can speak notification if needed
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removed
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
    }

    @Override
    public void onListenerDisconnected() {
        instance = null;
        super.onListenerDisconnected();
    }

    public void dismissNotification(String key) {
        cancelNotification(key);
    }

    public void dismissAllNotifications() {
        cancelAllNotifications();
    }

    public StatusBarNotification[] getActiveNotifications() {
        return super.getActiveNotifications();
    }
}
