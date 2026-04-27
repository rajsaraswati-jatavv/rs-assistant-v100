package com.rsassistant.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.SmsManager;

import androidx.core.app.NotificationCompat;

import com.rsassistant.MainActivity;
import com.rsassistant.R;
import com.rsassistant.ai.SmartAssistantManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * SOSReceiver - Handles Emergency SOS broadcasts
 * Triggered by voice command "SOS" or power button presses
 */
public class SOSReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "sos_emergency_channel";
    private static final int NOTIFICATION_ID = 9999;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if ("com.rsassistant.TRIGGER_SOS".equals(action) ||
            "android.intent.action.SOS".equals(action)) {
            triggerEmergencySOS(context);
        }
    }

    private void triggerEmergencySOS(Context context) {
        SmartAssistantManager smartManager = new SmartAssistantManager(context);
        String emergencyContact = smartManager.getEmergencyContact();

        if (emergencyContact.isEmpty()) {
            sendNotification(context, "SOS Failed", "No emergency contact set. Go to Settings to add one.");
            return;
        }

        // Get location
        String locationInfo = getLocationString(context);

        // Build SOS message
        String sosMessage = buildSOSMessage(locationInfo);

        // Send SMS
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(sosMessage);
            smsManager.sendMultipartTextMessage(emergencyContact, null, parts, null, null);
            sendNotification(context, "🆘 SOS Sent", "Emergency message sent to " + emergencyContact);
        } catch (Exception e) {
            sendNotification(context, "SOS Failed", "Could not send SMS: " + e.getMessage());
        }
    }

    private String getLocationString(Context context) {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (location != null) {
                    String lat = String.format("%.6f", location.getLatitude());
                    String lng = String.format("%.6f", location.getLongitude());
                    return "https://maps.google.com/?q=" + lat + "," + lng;
                }
            }
        } catch (SecurityException e) {
            // Location permission not granted
        }
        return "Location unavailable";
    }

    private String buildSOSMessage(String locationInfo) {
        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        return "🆘 EMERGENCY SOS 🆘\n\n" +
               "मुझे मदद चाहिए!\n" +
               "I need help!\n\n" +
               "📍 Location: " + locationInfo + "\n" +
               "🕐 Time: " + time + "\n\n" +
               "- Sent via RS Assistant";
    }

    private void sendNotification(Context context, String title, String message) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SOS Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Emergency SOS notifications");
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
