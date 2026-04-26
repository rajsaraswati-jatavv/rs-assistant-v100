package com.rsassistant.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.rsassistant.util.PreferenceManager;

public class ServiceConnector {

    public static void startAllServices(Context context) {
        PreferenceManager prefs = new PreferenceManager(context);

        // Start Core Service
        Intent coreIntent = new Intent(context, CoreService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(coreIntent);
        } else {
            context.startService(coreIntent);
        }
    }

    public static void stopAllServices(Context context) {
        context.stopService(new Intent(context, CoreService.class));
        context.stopService(new Intent(context, VoiceService.class));
        context.stopService(new Intent(context, CameraService.class));
        context.stopService(new Intent(context, LocationService.class));
    }
}
