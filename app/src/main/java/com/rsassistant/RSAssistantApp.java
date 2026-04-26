package com.rsassistant;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.rsassistant.service.CoreService;
import com.rsassistant.service.KeepAliveWorker;
import com.rsassistant.util.PreferenceManager;

import java.util.concurrent.TimeUnit;

public class RSAssistantApp extends Application {

    private static final String CHANNEL_ID = "rs_assistant_channel";
    private static final String CHANNEL_SERVICE = "rs_service_channel";

    private static RSAssistantApp instance;

    public static RSAssistantApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        createNotificationChannels();
        startKeepAliveWorker();

        // Auto start core service
        PreferenceManager prefs = new PreferenceManager(this);
        if (prefs.isAlwaysOnEnabled()) {
            startCoreService();
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Main notification channel
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "RS Assistant",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("RS Assistant notifications");
            manager.createNotificationChannel(channel);

            // Service notification channel
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_SERVICE,
                    "Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Always-on background service");
            serviceChannel.setShowBadge(false);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void startKeepAliveWorker() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                KeepAliveWorker.class,
                15, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "keep_alive",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    public void startCoreService() {
        Intent intent = new Intent(this, CoreService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    public void stopCoreService() {
        Intent intent = new Intent(this, CoreService.class);
        stopService(intent);
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}
