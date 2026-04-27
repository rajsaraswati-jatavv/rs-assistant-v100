package com.rsassistant.gesture;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;

/**
 * ShakeDetector - Detects Shake Gestures
 * Controls Flashlight/Torch with shake gestures
 * System-level capability once set up
 */
public class ShakeDetector implements SensorEventListener {

    private static final String PREF_NAME = "shake_detector_prefs";
    private static final String KEY_ENABLED = "shake_enabled";
    private static final String KEY_SENSITIVITY = "shake_sensitivity";
    private static final String KEY_ACTION = "shake_action";
    private static final String KEY_TORCH_STATE = "torch_state";

    // Sensor settings
    private static final float DEFAULT_SENSITIVITY = 12.0f;
    private static final int SHAKE_TIMEOUT = 500; // ms between shakes
    private static final int SHAKE_COUNT = 2; // Number of shakes needed
    private static final long VIBRATE_DURATION = 100;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;
    private SharedPreferences prefs;
    private Context context;

    private float sensitivity;
    private int shakeCount = 0;
    private long lastShakeTime = 0;
    private boolean isDetecting = false;
    private boolean torchState = false;

    private ShakeListener listener;

    public interface ShakeListener {
        void onShakeDetected();
        void onTorchStateChanged(boolean isOn);
    }

    public ShakeDetector(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        
        this.sensitivity = prefs.getFloat(KEY_SENSITIVITY, DEFAULT_SENSITIVITY);
        this.torchState = prefs.getBoolean(KEY_TORCH_STATE, false);
    }

    public void setListener(ShakeListener listener) {
        this.listener = listener;
    }

    public void startDetection() {
        if (isDetecting || !isEnabled()) return;
        
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            isDetecting = true;
        }
    }

    public void stopDetection() {
        if (!isDetecting) return;
        
        sensorManager.unregisterListener(this);
        isDetecting = false;
    }

    public boolean isDetecting() {
        return isDetecting;
    }

    // ==================== SETTINGS ====================

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        
        if (enabled && !isDetecting) {
            startDetection();
        } else if (!enabled && isDetecting) {
            stopDetection();
        }
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
        prefs.edit().putFloat(KEY_SENSITIVITY, sensitivity).apply();
    }

    public float getSensitivity() {
        return sensitivity;
    }

    public void setShakeAction(String action) {
        prefs.edit().putString(KEY_ACTION, action).apply();
    }

    public String getShakeAction() {
        return prefs.getString(KEY_ACTION, "toggle_torch");
    }

    // ==================== TORCH STATE ====================

    public void setTorchState(boolean isOn) {
        this.torchState = isOn;
        prefs.edit().putBoolean(KEY_TORCH_STATE, isOn).apply();
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                listener.onTorchStateChanged(isOn)
            );
        }
    }

    public boolean getTorchState() {
        return torchState;
    }

    public void toggleTorch() {
        boolean newState = !torchState;
        setTorchState(newState);
        
        // Broadcast to system
        android.content.Intent intent = new android.content.Intent("com.rsassistant.TORCH_TOGGLE");
        intent.putExtra("state", newState);
        context.sendBroadcast(intent);
    }

    // ==================== SENSOR EVENTS ====================

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        // Calculate acceleration magnitude
        float acceleration = (float) Math.sqrt(x * x + y * y + z * z);
        
        // Detect shake
        if (acceleration > sensitivity) {
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastShakeTime < SHAKE_TIMEOUT) {
                shakeCount++;
            } else {
                shakeCount = 1;
            }
            
            lastShakeTime = currentTime;
            
            // Check if we have enough shakes
            if (shakeCount >= SHAKE_COUNT) {
                onShakeDetected();
                shakeCount = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private void onShakeDetected() {
        // Vibrate feedback
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(
                    VIBRATE_DURATION, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(VIBRATE_DURATION);
            }
        }
        
        // Get action
        String action = getShakeAction();
        
        switch (action) {
            case "toggle_torch":
                toggleTorch();
                break;
            case "torch_on":
                setTorchState(true);
                break;
            case "torch_off":
                setTorchState(false);
                break;
            case "sos":
                // Trigger SOS
                android.content.Intent sosIntent = new android.content.Intent("com.rsassistant.TRIGGER_SOS");
                context.sendBroadcast(sosIntent);
                break;
            case "volume_mute":
                android.content.Intent muteIntent = new android.content.Intent("com.rsassistant.VOLUME_MUTE");
                context.sendBroadcast(muteIntent);
                break;
        }
        
        // Notify listener
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                listener.onShakeDetected()
            );
        }
    }

    // ==================== UTILITY ====================

    public String getStatusInfo() {
        return "📳 Shake Detector:\n" +
               "• Status: " + (isDetecting ? "Active ✓" : "Inactive") + "\n" +
               "• Sensitivity: " + sensitivity + "\n" +
               "• Action: " + getShakeAction() + "\n" +
               "• Torch: " + (torchState ? "ON 💡" : "OFF");
    }

    public static String[] getAvailableActions() {
        return new String[]{
            "toggle_torch - Toggle flashlight on/off",
            "torch_on - Turn flashlight ON",
            "torch_off - Turn flashlight OFF",
            "sos - Trigger emergency SOS",
            "volume_mute - Mute volume"
        };
    }
}
