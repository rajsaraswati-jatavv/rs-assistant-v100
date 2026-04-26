package com.rsassistant;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.rsassistant.service.CoreService;
import com.rsassistant.util.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchWakeWord, switchAlwaysOn, switchVoiceResponse;
    private SwitchCompat switchGestureControl, switchFaceDetection;
    private RadioGroup languageGroup;
    private RadioButton radioEnglish, radioHindi, radioHinglish;
    private Button btnEnableAccessibility, btnBatteryOptimization, btnOverlayPermission, btnLoginLogout;
    private TextView loginStatus;

    private PreferenceManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PreferenceManager(this);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        switchWakeWord = findViewById(R.id.switchWakeWord);
        switchAlwaysOn = findViewById(R.id.switchAlwaysOn);
        switchVoiceResponse = findViewById(R.id.switchVoiceResponse);
        switchGestureControl = findViewById(R.id.switchGestureControl);
        switchFaceDetection = findViewById(R.id.switchFaceDetection);
        languageGroup = findViewById(R.id.languageGroup);
        radioEnglish = findViewById(R.id.radioEnglish);
        radioHindi = findViewById(R.id.radioHindi);
        radioHinglish = findViewById(R.id.radioHinglish);
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility);
        btnBatteryOptimization = findViewById(R.id.btnBatteryOptimization);
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission);
        btnLoginLogout = findViewById(R.id.btnLoginLogout);
        loginStatus = findViewById(R.id.loginStatus);
    }

    private void loadSettings() {
        switchWakeWord.setChecked(prefs.isWakeWordEnabled());
        switchAlwaysOn.setChecked(prefs.isAlwaysOnEnabled());
        switchVoiceResponse.setChecked(prefs.isVoiceResponseEnabled());
        switchGestureControl.setChecked(prefs.isGestureControlEnabled());
        switchFaceDetection.setChecked(prefs.isFaceDetectionEnabled());

        String language = prefs.getLanguage();
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
        switchWakeWord.setOnCheckedChangeListener((v, isChecked) -> prefs.setWakeWordEnabled(isChecked));

        switchAlwaysOn.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.setAlwaysOnEnabled(isChecked);
            if (isChecked) {
                startCoreService();
            } else {
                stopCoreService();
            }
        });

        switchVoiceResponse.setOnCheckedChangeListener((v, isChecked) -> prefs.setVoiceResponseEnabled(isChecked));
        switchGestureControl.setOnCheckedChangeListener((v, isChecked) -> prefs.setGestureControlEnabled(isChecked));
        switchFaceDetection.setOnCheckedChangeListener((v, isChecked) -> prefs.setFaceDetectionEnabled(isChecked));

        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioEnglish) {
                prefs.setLanguage("english");
            } else if (checkedId == R.id.radioHindi) {
                prefs.setLanguage("hindi");
            } else if (checkedId == R.id.radioHinglish) {
                prefs.setLanguage("hinglish");
            }
        });

        btnEnableAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnBatteryOptimization.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        btnOverlayPermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        btnLoginLogout.setOnClickListener(v -> {
            if (prefs.isLoggedIn()) {
                prefs.setLoggedIn(false);
                prefs.setAccessToken(null);
                updateLoginStatus();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            } else {
                // Start OAuth login
                Toast.makeText(this, "Login functionality - integrate with chat.z.ai", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCoreService() {
        Intent intent = new Intent(this, CoreService.class);
        ContextCompat.startForegroundService(this, intent);
    }

    private void stopCoreService() {
        Intent intent = new Intent(this, CoreService.class);
        stopService(intent);
    }

    private void updateLoginStatus() {
        if (prefs.isLoggedIn()) {
            loginStatus.setText("Status: Logged In");
            btnLoginLogout.setText("Logout");
        } else {
            loginStatus.setText("Status: Not Logged In");
            btnLoginLogout.setText("Login with Z.AI");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLoginStatus();

        // Check battery optimization status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                btnBatteryOptimization.setText("Disable Battery Optimization (Recommended)");
            } else {
                btnBatteryOptimization.setText("Battery Optimization Disabled ✓");
            }
        }

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                btnOverlayPermission.setText("Overlay Permission Granted ✓");
            } else {
                btnOverlayPermission.setText("Grant Overlay Permission");
            }
        }
    }
}
