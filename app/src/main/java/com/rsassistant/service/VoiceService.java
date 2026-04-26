package com.rsassistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.rsassistant.MainActivity;
import com.rsassistant.R;
import com.rsassistant.util.CommandProcessor;
import com.rsassistant.util.PreferenceManager;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceService extends Service implements TextToSpeech.OnInitListener {

    private static final String CHANNEL_ID = "voice_service_channel";
    private static final int NOTIFICATION_ID = 1002;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private CommandProcessor commandProcessor;
    private PreferenceManager prefs;
    private Handler handler = new Handler(Looper.getMainExecutor());

    private boolean isListening = false;
    private boolean ttsReady = false;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new PreferenceManager(this);
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
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Locale locale = "hindi".equals(prefs.getLanguage()) ?
                    new Locale("hi", "IN") : Locale.ENGLISH;
            int result = textToSpeech.setLanguage(locale);
            ttsReady = (result != TextToSpeech.LANG_MISSING_DATA);
        }
    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { isListening = true; }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { isListening = false; }
            @Override public void onError(int error) {
                isListening = false;
                handler.postDelayed(() -> startListening(), 1000);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String response = commandProcessor.process(matches.get(0));
                    if (ttsReady) {
                        textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
                handler.postDelayed(() -> startListening(), 500);
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (speechRecognizer != null && !isListening) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            try {
                speechRecognizer.startListening(intent);
                isListening = true;
            } catch (Exception e) {
                isListening = false;
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Voice Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void startForegroundWithNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Service Active")
                .setContentText("Listening...")
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, flags);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        isListening = false;
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); }
        super.onDestroy();
    }
}
