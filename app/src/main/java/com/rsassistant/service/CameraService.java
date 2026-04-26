package com.rsassistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.rsassistant.R;

public class CameraService extends Service {

    private static final String CHANNEL_ID = "camera_service_channel";
    private static final int NOTIFICATION_ID = 1002;

    private CameraManager cameraManager;
    private String cameraId;
    private boolean flashEnabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundWithNotification();
        return START_STICKY;
    }

    public void toggleFlash() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                flashEnabled = !flashEnabled;
                cameraManager.setTorchMode(cameraId, flashEnabled);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void setFlash(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                flashEnabled = enabled;
                cameraManager.setTorchMode(cameraId, enabled);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isFlashEnabled() {
        return flashEnabled;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Camera Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundWithNotification() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RS Assistant Camera")
                .setContentText("Camera service active")
                .setSmallIcon(R.drawable.ic_camera)
                .setOngoing(true)
                .build();

        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            flags = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, flags);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        setFlash(false);
        super.onDestroy();
    }
}
