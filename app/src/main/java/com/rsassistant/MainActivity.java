package com.rsassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
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

import com.rsassistant.auth.OAuthManager;
import com.rsassistant.service.VoiceRecognitionService;
import com.rsassistant.util.CommandProcessor;
import com.rsassistant.util.PermissionHelper;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_VOICE = 101;

    private ImageButton voiceButton;
    private TextView statusText, resultText;
    private Button settingsButton, cameraButton, loginButton;

    private TextToSpeech textToSpeech;
    private OAuthManager oauthManager;
    private CommandProcessor commandProcessor;
    private boolean isListening = false;
    private boolean ttsInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initServices();
        checkPermissions();
    }

    private void initViews() {
        voiceButton = findViewById(R.id.voiceButton);
        statusText = findViewById(R.id.statusText);
        resultText = findViewById(R.id.resultText);
        settingsButton = findViewById(R.id.settingsButton);
        cameraButton = findViewById(R.id.cameraButton);
        loginButton = findViewById(R.id.loginButton);

        voiceButton.setOnClickListener(v -> toggleVoiceRecognition());
        settingsButton.setOnClickListener(v -> openSettings());
        cameraButton.setOnClickListener(v -> openCamera());
        loginButton.setOnClickListener(v -> handleLogin());
    }

    private void initServices() {
        textToSpeech = new TextToSpeech(this, this);
        oauthManager = new OAuthManager(this);
        commandProcessor = new CommandProcessor(this);

        updateLoginStatus();
    }

    private void checkPermissions() {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String command = results.get(0);
                processCommand(command);
            }
        }

        stopVoiceRecognition();
    }

    private void processCommand(String command) {
        resultText.setText("\"" + command + "\"");

        String response = commandProcessor.process(command);
        resultText.setText(response);

        if (ttsInitialized) {
            speak(response);
        }
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

    private void handleLogin() {
        if (oauthManager.isLoggedIn()) {
            oauthManager.logout();
            showToast("Logged out");
        } else {
            oauthManager.startLogin();
        }
        updateLoginStatus();
    }

    private void updateLoginStatus() {
        if (oauthManager.isLoggedIn()) {
            loginButton.setText("Logout from Z.AI");
        } else {
            loginButton.setText(R.string.oauth_login);
        }
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
