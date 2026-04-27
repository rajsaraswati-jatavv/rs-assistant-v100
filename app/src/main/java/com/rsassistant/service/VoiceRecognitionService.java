package com.rsassistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.core.app.NotificationCompat;

import com.rsassistant.MainActivity;
import com.rsassistant.R;
import com.rsassistant.util.CommandProcessor;
import com.rsassistant.util.PreferenceManager;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceRecognitionService extends Service implements TextToSpeech.OnInitListener {

    private static final String CHANNEL_ID = "voice_service_channel";
    private static final int NOTIFICATION_ID = 1001;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private CommandProcessor commandProcessor;
    private PreferenceManager prefManager;
    private boolean isListening = false;
    private boolean ttsReady = false;

    @Override
    public void onCreate() {
        super.onCreate();
        prefManager = new PreferenceManager(this);
        commandProcessor = new CommandProcessor(this);
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
        return START_STICKY;
    }

    private void initTTS() {
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if ("wake_response".equals(utteranceId)) {
                    startListening();
                }
            }

            @Override
            public void onError(String utteranceId) {}
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            String lang = prefManager.getLanguage();
            Locale locale = Locale.ENGLISH;
            if ("hindi".equals(lang)) {
                locale = new Locale("hi", "IN");
            } else if ("hinglish".equals(lang)) {
                locale = new Locale("en", "IN");
            }
            int result = textToSpeech.setLanguage(locale);
            ttsReady = (result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED);
        }
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
                if (prefManager.isBackgroundServiceEnabled()) {
                    restartListening();
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase();
                    processCommand(command);
                }
                if (prefManager.isBackgroundServiceEnabled()) {
                    restartListening();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

            try {
                speechRecognizer.startListening(intent);
                isListening = true;
            } catch (Exception e) {
                isListening = false;
            }
        }
    }

    private void restartListening() {
        new android.os.Handler().postDelayed(this::startListening, 500);
    }

    private void processCommand(String command) {
        String response = commandProcessor.process(command);

        if (ttsReady && textToSpeech != null) {
            textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, "command_response");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Voice Recognition Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("RS Assistant Voice Service");
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
                .setContentText("Listening for voice commands...")
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
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
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
