package com.rsassistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.rsassistant.MainActivity;
import com.rsassistant.R;

public class CameraService extends Service {

    private static final String CHANNEL_ID = "camera_service_channel";
    private static final int NOTIFICATION_ID = 1003;

    private CameraManager cameraManager;
    private String cameraId;
    private boolean flashEnabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String[] ids = cameraManager.getCameraIdList();
            if (ids.length > 0) cameraId = ids[0];
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraId != null) {
            try {
                flashEnabled = !flashEnabled;
                cameraManager.setTorchMode(cameraId, flashEnabled);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void setFlash(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraId != null) {
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
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Camera Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void startForegroundWithNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Camera Service Active")
                .setContentText("Camera ready")
                .setSmallIcon(R.drawable.ic_camera)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA : 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, flags);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        setFlash(false);
        super.onDestroy();
    }
}
