package com.rsassistant.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rsassistant.MainActivity;
import com.rsassistant.R;

import java.util.concurrent.TimeUnit;

/**
 * UpdateReminderWorker - Sends hourly notifications for app updates
 */
public class UpdateReminderWorker extends Worker {

    private static final String CHANNEL_ID = "update_reminder_channel";
    private static final String WORK_NAME = "hourly_update_reminder";
    private static final String PREF_NAME = "update_reminder_prefs";
    private static final String KEY_REMINDER_COUNT = "reminder_count";

    private static final String[] UPGRADE_MESSAGES = {
        "🚀 RS Assistant Update! New: SOS Emergency, Smart Features, Better AI",
        "⚡ Upgrade Available! Features: Auto-Reply, Custom Commands, Location Share",
        "🎯 New Update! Improved: Emergency SOS, Voice Recognition, Hindi Support",
        "🔥 v104 Ready! New: Smart Suggestions, Reminder System, Performance Info",
        "💡 Update Now! Features: Volume Control, Lock Screen, Power Management",
        "🌟 Upgrade Time! Enhanced: AI Chat, Context Memory, Offline Mode",
        "🔔 New Version! Improved: Battery Saver, Background Service, Wake Word",
        "📢 Update Available! New: Multi-language AI, Quick Actions, SOS Button"
    };

    public UpdateReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        createNotificationChannel(context);

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_REMINDER_COUNT, 0);

        String message = UPGRADE_MESSAGES[count % UPGRADE_MESSAGES.length];
        showUpdateNotification(context, message, count + 1);

        prefs.edit().putInt(KEY_REMINDER_COUNT, count + 1).apply();

        return Result.success();
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Update Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Hourly reminders for RS Assistant updates");
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showUpdateNotification(Context context, String message, int reminderNumber) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("RS Assistant Update #" + reminderNumber)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(new long[]{0, 500, 200, 500});

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    public static void scheduleHourlyReminders(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
            UpdateReminderWorker.class,
            1, TimeUnit.HOURS
        ).setInitialDelay(1, TimeUnit.HOURS).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        );
    }

    public static void cancelReminders(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
}
