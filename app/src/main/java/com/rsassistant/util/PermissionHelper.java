package com.rsassistant.util;

import android.Manifest;
import android.os.Build;

public class PermissionHelper {

    public static String[] getRequiredPermissions() {
        java.util.ArrayList<String> permissions = new java.util.ArrayList<>();

        // Audio & Voice
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        
        // Network
        permissions.add(Manifest.permission.INTERNET);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        
        // Camera
        permissions.add(Manifest.permission.CAMERA);
        
        // Overlay
        permissions.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
        
        // Contacts & Calls
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.WRITE_CONTACTS);
        permissions.add(Manifest.permission.CALL_PHONE);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        
        // SMS
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.RECEIVE_SMS);
        
        // Storage
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        
        // Location
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        
        // Bluetooth
        permissions.add(Manifest.permission.BLUETOOTH);
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        
        // Vibration
        permissions.add(Manifest.permission.VIBRATE);
        
        // Wake Lock
        permissions.add(Manifest.permission.WAKE_LOCK);

        // API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(Manifest.permission.ANSWER_PHONE_CALLS);
            permissions.add(Manifest.permission.READ_CALL_LOG);
        }

        // API 28+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        // API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.READ_CALL_LOG);
        }

        // API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE);
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA);
        }

        // API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        // API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
        }

        return permissions.toArray(new String[0]);
    }

    public static String[] getBasicPermissions() {
        return new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA
        };
    }
}
