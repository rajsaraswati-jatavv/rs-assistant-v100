package com.rsassistant.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.rsassistant.service.CoreService;
import com.rsassistant.util.PreferenceManager;

public class KeepAliveReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PreferenceManager prefs = new PreferenceManager(context);

        if (prefs.isAlwaysOnEnabled()) {
            Intent serviceIntent = new Intent(context, CoreService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
