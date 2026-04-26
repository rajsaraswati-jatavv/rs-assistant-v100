package com.rsassistant.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rsassistant.util.PreferenceManager;

public class KeepAliveWorker extends Worker {

    public KeepAliveWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        PreferenceManager prefs = new PreferenceManager(getApplicationContext());

        if (prefs.isAlwaysOnEnabled()) {
            // Ensure service is running
            Intent intent = new Intent(getApplicationContext(), CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(intent);
            } else {
                getApplicationContext().startService(intent);
            }
        }

        return Result.success();
    }
}
