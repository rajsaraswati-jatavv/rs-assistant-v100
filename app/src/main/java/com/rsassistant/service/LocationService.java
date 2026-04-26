package com.rsassistant.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.rsassistant.MainActivity;
import com.rsassistant.R;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 1004;

    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        initLocationCallback();
    }

    private void initLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    currentLocation = locationResult.getLastLocation();
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundWithNotification();
        startLocationUpdates();
        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        locationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public String getLocationString() {
        if (currentLocation != null) {
            return String.format("Lat: %.4f, Lng: %.4f",
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        }
        return "Location unknown";
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void startForegroundWithNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service Active")
                .setContentText("Tracking location")
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, flags);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }
}
