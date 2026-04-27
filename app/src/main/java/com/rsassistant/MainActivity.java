package com.rsassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.rsassistant.ai.SmartAssistantManager;
import com.rsassistant.ai.ZAIChatManager;
import com.rsassistant.auth.OAuthManager;
import com.rsassistant.gesture.ShakeDetector;
import com.rsassistant.memory.MemoryManager;
import com.rsassistant.service.VoiceRecognitionService;
import com.rsassistant.util.CommandProcessor;
import com.rsassistant.util.DeviceControlManager;
import com.rsassistant.util.PermissionHelper;
import com.rsassistant.util.SystemLevelManager;
import com.rsassistant.worker.UpdateReminderWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, OAuthManager.OAuthCallback {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_VOICE = 101;
    private static final int REQUEST_DEVICE_ADMIN = 102;
    private static final int REQUEST_BATTERY_OPTIMIZATION = 103;
    private static final int REQUEST_OVERLAY = 104;

    private ImageButton voiceButton;
    private TextView statusText, resultText;
    private Button settingsButton, cameraButton, loginButton, enableAdminButton, enableBatteryButton, featuresButton;

    private TextToSpeech textToSpeech;
    private OAuthManager oauthManager;
    private ZAIChatManager chatManager;
    private SmartAssistantManager smartManager;
    private MemoryManager memoryManager;
    private CommandProcessor commandProcessor;
    private DeviceControlManager deviceControl;
    private SystemLevelManager systemManager;
    private ShakeDetector shakeDetector;
    
    private boolean isListening = false;
    private boolean ttsInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initServices();
        initShakeDetector();
        checkAllPermissions();
        startBackgroundService();
        initUpdateReminders();
        
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            // Intent handled
        }
    }

    private void initViews() {
        voiceButton = findViewById(R.id.voiceButton);
        statusText = findViewById(R.id.statusText);
        resultText = findViewById(R.id.resultText);
        settingsButton = findViewById(R.id.settingsButton);
        cameraButton = findViewById(R.id.cameraButton);
        loginButton = findViewById(R.id.loginButton);
        enableAdminButton = findViewById(R.id.enableAdminButton);
        enableBatteryButton = findViewById(R.id.enableBatteryButton);
        
        // Features Button (new)
        featuresButton = findViewById(R.id.featuresButton);

        voiceButton.setOnClickListener(v -> toggleVoiceRecognition());
        settingsButton.setOnClickListener(v -> openSettings());
        cameraButton.setOnClickListener(v -> openCamera());
        loginButton.setOnClickListener(v -> handleLogin());
        
        if (featuresButton != null) {
            featuresButton.setOnClickListener(v -> openFeatures());
        }
        
        if (enableAdminButton != null) {
            enableAdminButton.setOnClickListener(v -> requestDeviceAdmin());
        }
        if (enableBatteryButton != null) {
            enableBatteryButton.setOnClickListener(v -> requestBatteryOptimization());
        }
    }

    private void initServices() {
        // Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Memory Manager
        memoryManager = MemoryManager.getInstance(this);
        
        // System Level Manager
        systemManager = SystemLevelManager.getInstance(this);
        
        // OAuth Manager
        oauthManager = new OAuthManager(this);
        oauthManager.setCallback(this);
        
        // AI Chat Manager
        chatManager = new ZAIChatManager(this);
        chatManager.testConnection(new ZAIChatManager.ConnectionCallback() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> showToast("AI Connected!"));
            }

            @Override
            public void onDisconnected(String reason) {
                runOnUiThread(() -> showToast("AI Offline Mode"));
            }
        });
        
        // Smart Assistant Manager
        smartManager = new SmartAssistantManager(this);
        
        // Command Processor
        commandProcessor = new CommandProcessor(this);
        
        // Device Control
        deviceControl = new DeviceControlManager(this);

        updateLoginStatus();
        updateAdminStatus();
        updateBatteryStatus();
    }
    
    private void initShakeDetector() {
        shakeDetector = new ShakeDetector(this);
        shakeDetector.setListener(new ShakeDetector.ShakeListener() {
            @Override
            public void onShakeDetected() {
                runOnUiThread(() -> {
                    showToast("📳 Shake detected! Torch toggled");
                });
            }

            @Override
            public void onTorchStateChanged(boolean isOn) {
                runOnUiThread(() -> {
                    showToast("💡 Torch: " + (isOn ? "ON" : "OFF"));
                });
            }
        });
        
        // Auto-start if enabled
        if (shakeDetector.isEnabled()) {
            shakeDetector.startDetection();
        }
    }
    
    private void initUpdateReminders() {
        UpdateReminderWorker.scheduleHourlyReminders(this);
    }

    private void checkAllPermissions() {
        String[] permissions = PermissionHelper.getRequiredPermissions();
        ArrayList<String> needed = new ArrayList<>();

        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), REQUEST_PERMISSIONS);
        }

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
        }

        checkBatteryOptimization();

        if (!deviceControl.isAdminActive()) {
            requestDeviceAdmin();
        }
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                requestBatteryOptimization();
            }
        }
    }

    private void requestDeviceAdmin() {
        if (!deviceControl.isAdminActive()) {
            Intent intent = deviceControl.getDeviceAdminIntent();
            startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
        }
    }

    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                }
            }
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, VoiceRecognitionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                showToast("Some permissions are required for full functionality");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String command = results.get(0);
                processCommand(command);
            }
            stopVoiceRecognition();
        } else if (requestCode == REQUEST_DEVICE_ADMIN) {
            updateAdminStatus();
            if (deviceControl.isAdminActive()) {
                showToast("Device Admin enabled! Now you can use lock screen, power off commands.");
            }
        } else if (requestCode == REQUEST_BATTERY_OPTIMIZATION) {
            updateBatteryStatus();
        } else {
            stopVoiceRecognition();
        }
    }

    private void toggleVoiceRecognition() {
        if (!isListening) {
            startVoiceRecognition();
        } else {
            stopVoiceRecognition();
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, REQUEST_VOICE);
            isListening = true;
            statusText.setText(R.string.listening);
            voiceButton.setBackgroundColor(getResources().getColor(R.color.listening_active));
        } catch (Exception e) {
            showToast("Voice recognition not available");
        }
    }

    private void stopVoiceRecognition() {
        isListening = false;
        statusText.setText(R.string.speak_now);
        voiceButton.setBackgroundColor(getResources().getColor(R.color.primary));
    }

    private void processCommand(String command) {
        resultText.setText("\"" + command + "\"");
        
        // Save to memory
        memoryManager.saveCommand(command, "", true, "voice");
        
        // Check for memory commands
        String lower = command.toLowerCase();
        
        if (lower.contains("show feature") || lower.contains("सभी features") || 
            lower.contains("all features") || lower.contains("memory दिखा")) {
            openFeatures();
            return;
        }
        
        if (lower.contains("shake on") || lower.contains("shake enable")) {
            shakeDetector.setEnabled(true);
            shakeDetector.startDetection();
            showToast("📳 Shake detection enabled!");
            speak("Shake detection enabled! Shake to toggle torch.");
            return;
        }
        
        if (lower.contains("shake off") || lower.contains("shake disable")) {
            shakeDetector.setEnabled(false);
            shakeDetector.stopDetection();
            showToast("Shake detection disabled");
            speak("Shake detection disabled");
            return;
        }
        
        if (lower.contains("torch on") || lower.contains("flashlight on") || 
            lower.contains("टॉर्च जला")) {
            systemManager.setTorch(true);
            showToast("💡 Torch ON");
            speak("Torch is now on");
            memoryManager.recordFeatureUse("Flashlight");
            return;
        }
        
        if (lower.contains("torch off") || lower.contains("flashlight off") || 
            lower.contains("टॉर्च बुझा")) {
            systemManager.setTorch(false);
            showToast("💡 Torch OFF");
            speak("Torch is now off");
            return;
        }
        
        // Check for SOS command
        if (lower.contains("sos") || lower.contains("emergency") || lower.contains("मदद")) {
            triggerSOS();
            return;
        }
        
        // Check for shortcut
        MemoryManager.ShortcutRecord shortcut = memoryManager.getShortcut(command);
        if (shortcut != null) {
            resultText.setText("🎯 Shortcut: " + shortcut.actionData);
            speak("Running shortcut");
            return;
        }
        
        // Check for custom commands
        String customAction = smartManager.getCustomCommandAction(command);
        if (customAction != null) {
            resultText.setText("Custom: " + customAction);
            speak(customAction);
            return;
        }

        // Process command locally first
        String response = commandProcessor.process(command);
        
        if (response != null && !response.contains("not understand")) {
            resultText.setText(response);
            if (ttsInitialized) {
                speak(response);
            }
            smartManager.recordAction(command);
            
            // Record feature usage
            if (lower.contains("volume")) memoryManager.recordFeatureUse("Volume Control");
            else if (lower.contains("camera")) memoryManager.recordFeatureUse("Camera");
            else if (lower.contains("wifi")) memoryManager.recordFeatureUse("WiFi");
            else if (lower.contains("lock")) memoryManager.recordFeatureUse("Lock Screen");
        } else {
            // Send to AI for conversation
            sendToAI(command);
        }
    }
    
    private void sendToAI(String message) {
        chatManager.sendMessage(message, new ZAIChatManager.ChatCallback() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    resultText.setText(response);
                    if (ttsInitialized) {
                        speak(response);
                    }
                    // Save to memory
                    memoryManager.saveCommand(message, response, true, "ai_chat");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    resultText.setText("Error: " + error);
                    memoryManager.saveCommand(message, error, false, "error");
                });
            }
        });
    }
    
    private void triggerSOS() {
        smartManager.triggerSOS();
        resultText.setText("🆘 SOS TRIGGERED! Emergency contact notified.");
        speak("Emergency SOS activated!");
        memoryManager.recordFeatureUse("SOS");
    }

    private void speak(String text) {
        if (textToSpeech != null && ttsInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void openCamera() {
        startActivity(new Intent(this, CameraActivity.class));
    }
    
    private void openFeatures() {
        startActivity(new Intent(this, FeaturesScreenActivity.class));
    }

    private void handleLogin() {
        if (oauthManager.isLoggedIn()) {
            oauthManager.logout();
        } else {
            oauthManager.startLogin();
        }
        updateLoginStatus();
    }

    private void updateLoginStatus() {
        if (oauthManager.isLoggedIn()) {
            loginButton.setText("✓ Connected to Z.AI");
            loginButton.setBackgroundColor(getResources().getColor(R.color.listening_active));
        } else {
            loginButton.setText(R.string.oauth_login);
            loginButton.setBackgroundColor(getResources().getColor(R.color.accent));
        }
    }

    private void updateAdminStatus() {
        if (enableAdminButton != null) {
            if (deviceControl.isAdminActive()) {
                enableAdminButton.setText("✓ Device Admin Active");
                enableAdminButton.setEnabled(false);
            } else {
                enableAdminButton.setText("Enable Device Admin");
                enableAdminButton.setEnabled(true);
            }
        }
    }

    private void updateBatteryStatus() {
        if (enableBatteryButton != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                enableBatteryButton.setText("✓ Battery Optimization Disabled");
                enableBatteryButton.setEnabled(false);
            } else {
                enableBatteryButton.setText("Disable Battery Optimization");
                enableBatteryButton.setEnabled(true);
            }
        }
    }

    // OAuth Callbacks
    @Override
    public void onLoginSuccess() {
        showToast("Successfully connected to Z.AI!");
        updateLoginStatus();
        if (ttsInitialized) {
            speak("Connected to Z.AI!");
        }
    }

    @Override
    public void onLoginError(String error) {
        showToast("Login failed: " + error);
        updateLoginStatus();
    }

    @Override
    public void onLogout() {
        showToast("Logged out from Z.AI");
        updateLoginStatus();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            ttsInitialized = (result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLoginStatus();
        updateAdminStatus();
        updateBatteryStatus();
        
        // Show predicted commands from memory
        List<String> predictions = memoryManager.getPredictedCommands();
        if (!predictions.isEmpty()) {
            statusText.setText("💡 " + predictions.get(0) + "?");
        } else {
            // Show smart suggestions
            List<String> suggestions = smartManager.getSmartSuggestions();
            if (!suggestions.isEmpty()) {
                statusText.setText(suggestions.get(0));
            }
        }
        
        // Start shake detection if enabled
        if (shakeDetector.isEnabled() && !shakeDetector.isDetecting()) {
            shakeDetector.startDetection();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep shake detection running in background if enabled
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
