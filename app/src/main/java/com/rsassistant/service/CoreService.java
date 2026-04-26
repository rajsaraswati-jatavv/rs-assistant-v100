package com.rsassistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.rsassistant.MainActivity;
import com.rsassistant.R;
import com.rsassistant.receiver.ServiceRestartReceiver;
import com.rsassistant.util.CommandProcessor;
import com.rsassistant.util.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class CoreService extends Service implements TextToSpeech.OnInitListener {

    private static final String CHANNEL_ID = "core_service_channel";
    private static final int NOTIFICATION_ID = 1001;

    private static CoreService instance;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private CommandProcessor commandProcessor;
    private PreferenceManager prefs;
    private PowerManager.WakeLock wakeLock;

    private boolean isListening = false;
    private boolean ttsReady = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    private Runnable restartRunnable = this::startListening;

    public static CoreService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        prefs = new PreferenceManager(this);
        commandProcessor = new CommandProcessor(this);

        acquireWakeLock();
        initTTS();
        initSpeechRecognizer();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundWithNotification();

        if (!isListening) {
            startListening();
        }

        // START_STICKY ensures service restarts if killed
        return START_STICKY;
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "RSAssistant::CoreService"
            );
            wakeLock.acquire(10 * 60 * 1000L); // 10 minutes, will be re-acquired
        }
    }

    private void initTTS() {
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if ("wake_response".equals(utteranceId)) {
                    handler.postDelayed(restartRunnable, 500);
                }
            }

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

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
            }

            @Override
            public void onError(int error) {
                isListening = false;
                // Auto restart on error
                if (prefs.isAlwaysOnEnabled()) {
                    handler.postDelayed(restartRunnable, 1000);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase();
                    processCommand(command);
                }
                // Auto restart
                if (prefs.isAlwaysOnEnabled()) {
                    handler.postDelayed(restartRunnable, 500);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (speechRecognizer != null && !isListening) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            try {
                speechRecognizer.startListening(intent);
                isListening = true;
            } catch (Exception e) {
                isListening = false;
                handler.postDelayed(restartRunnable, 2000);
            }
        }
    }

    private void processCommand(String command) {
        // Check for wake word if enabled
        if (prefs.isWakeWordEnabled()) {
            String wakeWord = prefs.getWakeWordPhrase().toLowerCase();
            if (!command.toLowerCase().contains(wakeWord)) {
                return; // Ignore commands without wake word
            }
            // Remove wake word from command
            command = command.toLowerCase().replace(wakeWord, "").trim();
            if (command.isEmpty()) {
                speak("Yes, how can I help?");
                return;
            }
        }

        String response = commandProcessor.process(command);

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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "RS Assistant Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Always-on voice assistant service");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundWithNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RS Assistant Active")
                .setContentText("Always listening for voice commands...")
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            flags = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE |
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA |
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, flags);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isListening = false;
        handler.removeCallbacks(restartRunnable);

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Send broadcast to restart service
        if (prefs.isAlwaysOnEnabled()) {
            Intent restartIntent = new Intent(this, ServiceRestartReceiver.class);
            sendBroadcast(restartIntent);
        }

        instance = null;
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Restart service when task is removed
        if (prefs.isAlwaysOnEnabled()) {
            Intent restartIntent = new Intent(this, ServiceRestartReceiver.class);
            sendBroadcast(restartIntent);
        }
        super.onTaskRemoved(rootIntent);
    }
}
