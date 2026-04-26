package com.rsassistant;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.rsassistant.service.CoreService;
import com.rsassistant.service.RSAccessibilityService;
import com.rsassistant.util.CommandProcessor;
import com.rsassistant.util.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_VOICE = 101;
    private static final int REQUEST_OVERLAY = 102;
    private static final int REQUEST_BATTERY = 103;

    private ImageButton voiceButton;
    private TextView statusText, resultText;
    private Button settingsButton, cameraButton, serviceButton, permissionButton;

    private TextToSpeech textToSpeech;
    private CommandProcessor commandProcessor;
    private PreferenceManager prefs;
    private boolean isListening = false;
    private boolean ttsReady = false;
    private boolean serviceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PreferenceManager(this);
        commandProcessor = new CommandProcessor(this);

        initViews();
        initTTS();
        checkPermissions();
        updateServiceStatus();
    }

    private void initViews() {
        voiceButton = findViewById(R.id.voiceButton);
        statusText = findViewById(R.id.statusText);
        resultText = findViewById(R.id.resultText);
        settingsButton = findViewById(R.id.settingsButton);
        cameraButton = findViewById(R.id.cameraButton);
        serviceButton = findViewById(R.id.serviceButton);
        permissionButton = findViewById(R.id.permissionButton);

        voiceButton.setOnClickListener(v -> toggleVoice());
        settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        cameraButton.setOnClickListener(v -> startActivity(new Intent(this, CameraActivity.class)));
        serviceButton.setOnClickListener(v -> toggleService());
        permissionButton.setOnClickListener(v -> requestAllPermissions());
    }

    private void initTTS() {
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {}

            @Override
            public void onError(String utteranceId) {}
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            setTTSLanguage();
            ttsReady = true;
        }
    }

    private void setTTSLanguage() {
        String lang = prefs.getLanguage();
        Locale locale = Locale.ENGLISH;
        if ("hindi".equals(lang)) {
            locale = new Locale("hi", "IN");
        } else if ("hinglish".equals(lang)) {
            locale = new Locale("en", "IN");
        }
        int result = textToSpeech.setLanguage(locale);
        ttsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED);
    }

    private void checkPermissions() {
        String[] permissions = getRequiredPermissions();
        ArrayList<String> needed = new ArrayList<>();

        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQUEST_PERMISSIONS);
        }

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            resultText.setText("Grant Overlay Permission for full functionality");
        }

        // Check battery optimization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                // Request to disable battery optimization
            }
        }
    }

    private String[] getRequiredPermissions() {
        ArrayList<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.RECORD_AUDIO);
        perms.add(Manifest.permission.INTERNET);
        perms.add(Manifest.permission.CAMERA);
        perms.add(Manifest.permission.READ_CONTACTS);
        perms.add(Manifest.permission.CALL_PHONE);
        perms.add(Manifest.permission.SEND_SMS);
        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        perms.add(Manifest.permission.BLUETOOTH);
        perms.add(Manifest.permission.READ_PHONE_STATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            perms.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perms.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE);
            perms.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            perms.add(Manifest.permission.ANSWER_PHONE_CALLS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_AUDIO);
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.READ_MEDIA_VIDEO);
        }

        return perms.toArray(new String[0]);
    }

    private void requestAllPermissions() {
        // Request runtime permissions
        String[] permissions = getRequiredPermissions();
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);

        // Request overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
        }

        // Request battery optimization exemption
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_BATTERY);
            }
        }

        // Open accessibility settings
        Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(accessibilityIntent);

        showToast("Grant all permissions for full functionality");
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
            if (allGranted) {
                showToast("All permissions granted!");
            } else {
                showToast("Some permissions denied. Features may be limited.");
            }
        }
    }

    private void toggleVoice() {
        if (!isListening) {
            startVoiceRecognition();
        } else {
            stopVoiceRecognition();
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                processCommand(results.get(0));
            }
        }

        stopVoiceRecognition();
        updateServiceStatus();
    }

    private void processCommand(String command) {
        resultText.setText("\"" + command + "\"");

        String response = commandProcessor.process(command);
        resultText.setText(response);

        if (ttsReady && prefs.isVoiceResponseEnabled()) {
            speak(response);
        }
    }

    private void speak(String text) {
        if (textToSpeech != null && ttsReady) {
            String utteranceId = "tts_" + System.currentTimeMillis();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
            }
        }
    }

    private void toggleService() {
        if (serviceRunning) {
            stopService(new Intent(this, CoreService.class));
            serviceRunning = false;
            serviceButton.setText("Start Always-On Service");
            serviceButton.setBackgroundColor(getResources().getColor(R.color.success_green));
        } else {
            Intent intent = new Intent(this, CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            serviceRunning = true;
            serviceButton.setText("Stop Service");
            serviceButton.setBackgroundColor(getResources().getColor(R.color.error_red));
        }
        prefs.setAlwaysOnEnabled(serviceRunning);
    }

    private void updateServiceStatus() {
        // Check if service is running
        serviceRunning = prefs.isAlwaysOnEnabled();
        if (serviceRunning) {
            serviceButton.setText("Stop Service");
            serviceButton.setBackgroundColor(getResources().getColor(R.color.error_red));
        } else {
            serviceButton.setText("Start Always-On Service");
            serviceButton.setBackgroundColor(getResources().getColor(R.color.success_green));
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTTSLanguage();
        updateServiceStatus();
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
