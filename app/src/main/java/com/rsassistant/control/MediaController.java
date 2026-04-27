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

    // ==================== YouTube Music Integration ====================

    /**
     * Open YouTube Music app
     * Supports: "YouTube Music kholo", "Play YouTube Music"
     */
    public void openYouTubeMusic(MediaCallback callback) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.youtube.music");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("YouTube Music khul gaya!");
            } else {
                // Fallback to YouTube
                openYouTube(callback);
            }
        } catch (Exception e) {
            callback.onError("YouTube Music nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Search and play on YouTube Music
     * Supports: "YouTube Music pe Arijit Singh bajao"
     */
    public void searchYouTubeMusic(String query, MediaCallback callback) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(query)));
            intent.setPackage("com.google.android.apps.youtube.music");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("YouTube Music mein '" + query + "' search ho raha hai!");
        } catch (Exception e) {
            // Fallback to YouTube search
            searchYouTube(query, callback);
        }
    }

    // ==================== Gaana Integration ====================

    /**
     * Open Gaana app
     * Supports: "Gaana kholo", "Gaana app chalao"
     */
    public void openGaana(MediaCallback callback) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.gaana");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Gaana khul gaya!");
            } else {
                callback.onError("Gaana installed nahi hai");
            }
        } catch (Exception e) {
            callback.onError("Gaana nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Play on Gaana
     * Supports: "Gaana pe Arijit Singh bajao"
     */
    public void playOnGaana(String query, MediaCallback callback) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("gaana://search?query=" + Uri.encode(query)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Gaana mein '" + query + "' search ho raha hai!");
        } catch (Exception e) {
            // App might not be installed
            openGaana(callback);
        }
    }

    // ==================== JioSaavn Integration ====================

    /**
     * Open JioSaavn app
     * Supports: "Saavn kholo", "JioSaavn chalao"
     */
    public void openJioSaavn(MediaCallback callback) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.saavn.android");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("JioSaavn khul gaya!");
            } else {
                callback.onError("JioSaavn installed nahi hai");
            }
        } catch (Exception e) {
            callback.onError("JioSaavn nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Play on JioSaavn
     * Supports: "Saavn pe gaana bajao"
     */
    public void playOnJioSaavn(String query, MediaCallback callback) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("saavn://search?q=" + Uri.encode(query)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("JioSaavn mein '" + query + "' search ho raha hai!");
        } catch (Exception e) {
            openJioSaavn(callback);
        }
    }

    // ==================== Wynk Music Integration ====================

    /**
     * Open Wynk Music app
     */
    public void openWynk(MediaCallback callback) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.bsbportal.music");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Wynk Music khul gaya!");
            } else {
                callback.onError("Wynk installed nahi hai");
            }
        } catch (Exception e) {
            callback.onError("Wynk nahi khul pa raha: " + e.getMessage());
        }
    }

    // ==================== Smart Music Commands ====================

    /**
     * Smart play - tries multiple music apps
     * Supports: "Gaana bajao", "Play music", "Play Arijit Singh"
     * 
     * @param query Song/artist name or null for just play
     */
    public void smartPlay(String query, MediaCallback callback) {
        if (query == null || query.isEmpty() || query.equals("music") || query.equals("gaana")) {
            // Just play current music
            play(callback);
            return;
        }

        // Try apps in order of preference
        String[] musicPackages = {
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.gaana",
            "com.saavn.android",
            "com.bsbportal.music"
        };

        for (String pkg : musicPackages) {
            try {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                if (intent != null) {
                    // App is installed, use it
                    if (pkg.contains("spotify")) {
                        playOnSpotify(query, callback);
                    } else if (pkg.contains("youtube.music")) {
                        searchYouTubeMusic(query, callback);
                    } else if (pkg.contains("gaana")) {
                        playOnGaana(query, callback);
                    } else if (pkg.contains("saavn")) {
                        playOnJioSaavn(query, callback);
                    } else {
                        // Just open the app
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        callback.onSuccess("Music app khul gaya!");
                    }
                    return;
                }
            } catch (Exception e) {
                // Try next app
            }
        }

        // No music app found - use YouTube as fallback
        searchYouTube(query, callback);
    }

    /**
     * Play specific artist
     * Supports: "Play Arijit Singh", "Arijit Singh ke gaane bajao"
     */
    public void playArtist(String artistName, MediaCallback callback) {
        smartPlay(artistName, callback);
    }

    /**
     * Play specific song
     * Supports: "Play Tum Hi Ho", "Tum Hi Ho gaana bajao"
     */
    public void playSong(String songName, MediaCallback callback) {
        smartPlay(songName, callback);
    }

    /**
     * Play playlist
     * Supports: "Play my playlist", "Playlist chalao"
     */
    public void playPlaylist(String playlistName, MediaCallback callback) {
        // Open default music app
        openMusicPlayer(callback);
    }

    /**
     * Play random/shuffle
     * Supports: "Shuffle karo", "Random gaane bajao"
     */
    public void playShuffle(MediaCallback callback) {
        try {
            sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
            callback.onSuccess("Music shuffle mode mein chal raha hai!");
        } catch (Exception e) {
            callback.onError("Shuffle nahi ho pa raha: " + e.getMessage());
        }
    }

    // ==================== Volume Profile ====================

    /**
     * Set volume profile
     * Supports: "Volume full karo", "Volume 50 percent set karo"
     */
    public void setVolumeProfile(String profile, MediaCallback callback) {
        int volume = 50; // default
        
        switch (profile.toLowerCase()) {
            case "full":
            case "max":
            case "maximum":
            case "high":
            case "100":
                volume = 100;
                break;
            case "medium":
            case "mid":
            case "50":
                volume = 50;
                break;
            case "low":
            case "min":
            case "minimum":
            case "25":
                volume = 25;
                break;
            case "mute":
            case "silent":
            case "zero":
            case "0":
                volume = 0;
                break;
        }
        
        setVolume(volume, callback);
    }

    // ==================== Audio Info ====================

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
     * Get volume status summary
     */
    public String getVolumeStatus() {
        int current = getCurrentVolume();
        boolean playing = isMusicPlaying();
        return "Volume " + current + "% hai" + (playing ? ". Music chal raha hai." : ".");
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
