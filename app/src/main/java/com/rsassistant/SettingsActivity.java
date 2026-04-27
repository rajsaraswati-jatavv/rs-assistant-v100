package com.rsassistant;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.rsassistant.auth.OAuthManager;
import com.rsassistant.service.VoiceRecognitionService;
import com.rsassistant.util.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchWakeWord, switchBackgroundService, switchGestureControl;
    private RadioGroup languageGroup;
    private RadioButton radioEnglish, radioHindi, radioHinglish;
    private Button btnEnableAccessibility, btnLogout;
    private TextView loginStatus;

    private PreferenceManager prefManager;
    private OAuthManager oauthManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefManager = new PreferenceManager(this);
        oauthManager = new OAuthManager(this);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        switchWakeWord = findViewById(R.id.switchWakeWord);
        switchBackgroundService = findViewById(R.id.switchBackgroundService);
        switchGestureControl = findViewById(R.id.switchGestureControl);
        languageGroup = findViewById(R.id.languageGroup);
        radioEnglish = findViewById(R.id.radioEnglish);
        radioHindi = findViewById(R.id.radioHindi);
        radioHinglish = findViewById(R.id.radioHinglish);
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility);
        btnLogout = findViewById(R.id.btnLogout);
        loginStatus = findViewById(R.id.loginStatus);
    }

    private void loadSettings() {
        switchWakeWord.setChecked(prefManager.isWakeWordEnabled());
        switchBackgroundService.setChecked(prefManager.isBackgroundServiceEnabled());
        switchGestureControl.setChecked(prefManager.isGestureControlEnabled());

        String language = prefManager.getLanguage();
        if ("hindi".equals(language)) {
            radioHindi.setChecked(true);
        } else if ("hinglish".equals(language)) {
            radioHinglish.setChecked(true);
        } else {
            radioEnglish.setChecked(true);
        }

        updateLoginStatus();
    }

    private void setupListeners() {
        switchWakeWord.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setWakeWordEnabled(isChecked);
        });

        switchBackgroundService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setBackgroundServiceEnabled(isChecked);
            if (isChecked) {
                startVoiceService();
            } else {
                stopVoiceService();
            }
        });

        switchGestureControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setGestureControlEnabled(isChecked);
        });

        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioEnglish) {
                prefManager.setLanguage("english");
            } else if (checkedId == R.id.radioHindi) {
                prefManager.setLanguage("hindi");
            } else if (checkedId == R.id.radioHinglish) {
                prefManager.setLanguage("hinglish");
            }
        });

        btnEnableAccessibility.setOnClickListener(v -> openAccessibilitySettings());

        btnLogout.setOnClickListener(v -> {
            oauthManager.logout();
            updateLoginStatus();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private void startVoiceService() {
        Intent intent = new Intent(this, VoiceRecognitionService.class);
        ContextCompat.startForegroundService(this, intent);
    }

    private void stopVoiceService() {
        Intent intent = new Intent(this, VoiceRecognitionService.class);
        stopService(intent);
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    private void updateLoginStatus() {
        if (oauthManager.isLoggedIn()) {
            loginStatus.setText("Status: Logged In");
            btnLogout.setEnabled(true);
        } else {
            loginStatus.setText("Status: Not Logged In");
            btnLogout.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLoginStatus();
    }
}
