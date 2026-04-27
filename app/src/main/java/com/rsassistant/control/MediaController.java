package com.rsassistant.control;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.util.Log;

/**
 * MediaController - Voice-based Media Control Manager
 * 
 * Supported Commands:
 * - "Music play karo" / "Play music"
 * - "Volume badhao" / "Volume up"
 * - "Next song" / "Previous song"
 * - "Pause karo" / "Pause"
 */
public class MediaController {

    private static final String TAG = "MediaController";
    private final Context context;
    private final AudioManager audioManager;

    public interface MediaCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public MediaController(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Play/Pause music
     */
    public void playPause(MediaCallback callback) {
        try {
            sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            callback.onSuccess("Music play/pause ho gaya!");
        } catch (Exception e) {
            callback.onError("Media control nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Play music
     */
    public void play(MediaCallback callback) {
        try {
            sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
            callback.onSuccess("Music shuru ho gaya!");
        } catch (Exception e) {
            callback.onError("Play nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Pause music
     */
    public void pause(MediaCallback callback) {
        try {
            sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PAUSE);
            callback.onSuccess("Music ruk gaya!");
        } catch (Exception e) {
            callback.onError("Pause nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Next track
     */
    public void nextTrack(MediaCallback callback) {
        try {
            sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
            callback.onSuccess("Next song chal raha hai!");
        } catch (Exception e) {
            callback.onError("Next track nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Previous track
     */
    public void previousTrack(MediaCallback callback) {
        try {
            sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            callback.onSuccess("Previous song chal raha hai!");
        } catch (Exception e) {
            callback.onError("Previous track nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Stop music
     */
    public void stop(MediaCallback callback) {
        try {
            sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_STOP);
            callback.onSuccess("Music band ho gaya!");
        } catch (Exception e) {
            callback.onError("Stop nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Fast forward
     */
    public void fastForward(MediaCallback callback) {
        try {
            sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
            callback.onSuccess("Fast forward ho raha hai!");
        } catch (Exception e) {
            callback.onError("Fast forward nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Rewind
     */
    public void rewind(MediaCallback callback) {
        try {
            sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_REWIND);
            callback.onSuccess("Rewind ho raha hai!");
        } catch (Exception e) {
            callback.onError("Rewind nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Volume up
     */
    public void volumeUp(MediaCallback callback) {
        try {
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int percentage = (currentVolume * 100) / maxVolume;
            callback.onSuccess("Volume " + percentage + "% ho gaya!");
        } catch (Exception e) {
            callback.onError("Volume up nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Volume down
     */
    public void volumeDown(MediaCallback callback) {
        try {
            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int percentage = (currentVolume * 100) / maxVolume;
            callback.onSuccess("Volume " + percentage + "% ho gaya!");
        } catch (Exception e) {
            callback.onError("Volume down nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set volume to specific level (0-100)
     */
    public void setVolume(int percentage, MediaCallback callback) {
        try {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int volume = (percentage * maxVolume) / 100;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Volume " + percentage + "% set ho gaya!");
        } catch (Exception e) {
            callback.onError("Volume set nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Mute/Unmute
     */
    public void toggleMute(MediaCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int direction = audioManager.isMicrophoneMute() ? 
                    AudioManager.ADJUST_UNMUTE : AudioManager.ADJUST_MUTE;
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
            } else {
                AudioManager am = audioManager;
                am.setStreamMute(AudioManager.STREAM_MUSIC, !am.isMicrophoneMute());
            }
            callback.onSuccess("Mute toggle ho gaya!");
        } catch (Exception e) {
            callback.onError("Mute nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set media volume to max
     */
    public void setMaxVolume(MediaCallback callback) {
        try {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Volume maximum ho gaya!");
        } catch (Exception e) {
            callback.onError("Max volume nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set media volume to zero
     */
    public void setMinVolume(MediaCallback callback) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Volume zero ho gaya!");
        } catch (Exception e) {
            callback.onError("Min volume nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Open music player
     */
    public void openMusicPlayer(MediaCallback callback) {
        try {
            Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Music player khul gaya!");
        } catch (Exception e) {
            // Try to open Google Play Music / YouTube Music
            try {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.youtube.music");
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    callback.onSuccess("YouTube Music khul gaya!");
                } else {
                    callback.onError("Music player nahi mila");
                }
            } catch (Exception e2) {
                callback.onError("Music player nahi khul pa raha: " + e.getMessage());
            }
        }
    }

    /**
     * Open YouTube
     */
    public void openYouTube(MediaCallback callback) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("YouTube khul gaya!");
            } else {
                // Open in browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com"));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
                callback.onSuccess("YouTube browser mein khul gaya!");
            }
        } catch (Exception e) {
            callback.onError("YouTube nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Open YouTube with search
     */
    public void searchYouTube(String query, MediaCallback callback) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage("com.google.android.youtube");
            intent.putExtra("query", query);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("YouTube mein search ho raha hai!");
        } catch (Exception e) {
            // Fallback to browser
            try {
                String url = "https://www.youtube.com/results?search_query=" + Uri.encode(query);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
                callback.onSuccess("YouTube search browser mein khul gaya!");
            } catch (Exception e2) {
                callback.onError("YouTube search nahi ho pa raha: " + e.getMessage());
            }
        }
    }

    /**
     * Open Spotify
     */
    public void openSpotify(MediaCallback callback) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.spotify.music");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Spotify khul gaya!");
            } else {
                callback.onError("Spotify installed nahi hai");
            }
        } catch (Exception e) {
            callback.onError("Spotify nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Play specific song on Spotify
     */
    public void playOnSpotify(String songName, MediaCallback callback) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("spotify:search:" + songName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Spotify mein '" + songName + "' search ho raha hai!");
        } catch (Exception e) {
            callback.onError("Spotify search nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Open video player
     */
    public void openVideoPlayer(MediaCallback callback) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("video/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "Video Player"));
            callback.onSuccess("Video player khul gaya!");
        } catch (Exception e) {
            callback.onError("Video player nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Get current volume level (0-100)
     */
    public int getCurrentVolume() {
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return (currentVolume * 100) / maxVolume;
    }

    /**
     * Check if music is playing
     */
    public boolean isMusicPlaying() {
        return audioManager.isMusicActive();
    }

    /**
     * Send media button event
     */
    private void sendMediaButtonEvent(int keyCode) {
        long eventTime = System.currentTimeMillis();
        
        KeyEvent downEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);
        audioManager.dispatchMediaKeyEvent(downEvent);
        
        KeyEvent upEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0);
        audioManager.dispatchMediaKeyEvent(upEvent);
    }
}
