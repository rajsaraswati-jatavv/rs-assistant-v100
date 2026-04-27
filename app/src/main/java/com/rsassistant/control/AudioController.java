package com.rsassistant.control;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * AudioController - Audio and Sound Control Manager
 * 
 * Supported Features:
 * - Volume control (media, ringtone, alarm, call)
 * - Silent/vibrate/normal mode
 * - Do Not Disturb mode
 * - Audio profiles
 */
public class AudioController {

    private static final String TAG = "AudioController";
    private final Context context;
    private final AudioManager audioManager;
    private final NotificationManager notificationManager;
    
    private ToneGenerator toneGenerator;
    private MediaPlayer mediaPlayer;

    public interface AudioCallback {
        void onSuccess(String message);
        void onError(String error);
        void onVolumeChanged(int volume, int maxVolume, int streamType);
        void onProfileChanged(AudioProfile profile);
    }

    public static class AudioProfile {
        public String name;
        public int mediaVolume;
        public int ringVolume;
        public int alarmVolume;
        public int callVolume;
        public int ringerMode;
        public boolean dndEnabled;
    }

    public AudioController(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    // ==================== Volume Controls ====================

    /**
     * Set media volume (0-100)
     */
    public void setMediaVolume(int percent, AudioCallback callback) {
        try {
            percent = Math.max(0, Math.min(100, percent));
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int volume = (percent * max) / 100;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Media volume " + percent + "% set ho gaya!");
            callback.onVolumeChanged(volume, max, AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
            callback.onError("Volume change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set ringtone volume (0-100)
     */
    public void setRingtoneVolume(int percent, AudioCallback callback) {
        try {
            percent = Math.max(0, Math.min(100, percent));
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            int volume = (percent * max) / 100;
            audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Ringtone volume " + percent + "% set ho gaya!");
            callback.onVolumeChanged(volume, max, AudioManager.STREAM_RING);
        } catch (Exception e) {
            callback.onError("Ringtone volume change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set alarm volume (0-100)
     */
    public void setAlarmVolume(int percent, AudioCallback callback) {
        try {
            percent = Math.max(0, Math.min(100, percent));
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int volume = (percent * max) / 100;
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Alarm volume " + percent + "% set ho gaya!");
            callback.onVolumeChanged(volume, max, AudioManager.STREAM_ALARM);
        } catch (Exception e) {
            callback.onError("Alarm volume change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set call volume (0-100)
     */
    public void setCallVolume(int percent, AudioCallback callback) {
        try {
            percent = Math.max(0, Math.min(100, percent));
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
            int volume = (percent * max) / 100;
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Call volume " + percent + "% set ho gaya!");
            callback.onVolumeChanged(volume, max, AudioManager.STREAM_VOICE_CALL);
        } catch (Exception e) {
            callback.onError("Call volume change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set notification volume (0-100)
     */
    public void setNotificationVolume(int percent, AudioCallback callback) {
        try {
            percent = Math.max(0, Math.min(100, percent));
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            int volume = (percent * max) / 100;
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("Notification volume " + percent + "% set ho gaya!");
            callback.onVolumeChanged(volume, max, AudioManager.STREAM_NOTIFICATION);
        } catch (Exception e) {
            callback.onError("Notification volume change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set system volume (0-100)
     */
    public void setSystemVolume(int percent, AudioCallback callback) {
        try {
            percent = Math.max(0, Math.min(100, percent));
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
            int volume = (percent * max) / 100;
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, volume, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess("System volume " + percent + "% set ho gaya!");
            callback.onVolumeChanged(volume, max, AudioManager.STREAM_SYSTEM);
        } catch (Exception e) {
            callback.onError("System volume change nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set all volumes to same percentage
     */
    public void setAllVolumes(int percent, AudioCallback callback) {
        setMediaVolume(percent, new AudioCallback() {
            @Override public void onSuccess(String message) {}
            @Override public void onError(String error) {}
            @Override public void onVolumeChanged(int volume, int maxVolume, int streamType) {}
            @Override public void onProfileChanged(AudioProfile profile) {}
        });
        setRingtoneVolume(percent, new AudioCallback() {
            @Override public void onSuccess(String message) {}
            @Override public void onError(String error) {}
            @Override public void onVolumeChanged(int volume, int maxVolume, int streamType) {}
            @Override public void onProfileChanged(AudioProfile profile) {}
        });
        setAlarmVolume(percent, new AudioCallback() {
            @Override public void onSuccess(String message) {}
            @Override public void onError(String error) {}
            @Override public void onVolumeChanged(int volume, int maxVolume, int streamType) {}
            @Override public void onProfileChanged(AudioProfile profile) {}
        });
        callback.onSuccess("All volumes " + percent + "% set ho gaye!");
    }

    // ==================== Volume Up/Down ====================

    /**
     * Increase media volume
     */
    public void increaseMediaVolume(AudioCallback callback) {
        try {
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            
            if (current < max) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, 
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                int newVolume = Math.min(max, current + 1);
                int percent = (newVolume * 100) / max;
                callback.onSuccess("Media volume " + percent + "% ho gaya!");
            } else {
                callback.onSuccess("Media volume already maximum hai!");
            }
        } catch (Exception e) {
            callback.onError("Volume up nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Decrease media volume
     */
    public void decreaseMediaVolume(AudioCallback callback) {
        try {
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            
            if (current > 0) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, 
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int newVolume = Math.max(0, current - 1);
                int percent = (newVolume * 100) / max;
                callback.onSuccess("Media volume " + percent + "% ho gaya!");
            } else {
                callback.onSuccess("Media volume already minimum hai!");
            }
        } catch (Exception e) {
            callback.onError("Volume down nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Increase volume for specified stream
     */
    public void increaseVolume(int streamType, AudioCallback callback) {
        try {
            int current = audioManager.getStreamVolume(streamType);
            int max = audioManager.getStreamMaxVolume(streamType);
            
            if (current < max) {
                audioManager.adjustStreamVolume(streamType, 
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                callback.onSuccess("Volume badh gaya!");
            } else {
                callback.onSuccess("Volume already maximum hai!");
            }
        } catch (Exception e) {
            callback.onError("Volume up nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Decrease volume for specified stream
     */
    public void decreaseVolume(int streamType, AudioCallback callback) {
        try {
            int current = audioManager.getStreamVolume(streamType);
            
            if (current > 0) {
                audioManager.adjustStreamVolume(streamType, 
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                callback.onSuccess("Volume kam ho gaya!");
            } else {
                callback.onSuccess("Volume already minimum hai!");
            }
        } catch (Exception e) {
            callback.onError("Volume down nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set volume to maximum
     */
    public void setMaxVolume(AudioCallback callback) {
        setMediaVolume(100, callback);
    }

    /**
     * Set volume to minimum
     */
    public void setMinVolume(AudioCallback callback) {
        setMediaVolume(0, callback);
    }

    // ==================== Ringer Mode Controls ====================

    /**
     * Set silent mode
     */
    public void setSilentMode(AudioCallback callback) {
        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            callback.onSuccess("Silent mode on ho gaya!");
        } catch (Exception e) {
            callback.onError("Silent mode nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set vibrate mode
     */
    public void setVibrateMode(AudioCallback callback) {
        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            callback.onSuccess("Vibrate mode on ho gaya!");
        } catch (Exception e) {
            callback.onError("Vibrate mode nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Set normal mode
     */
    public void setNormalMode(AudioCallback callback) {
        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            callback.onSuccess("Normal mode on ho gaya!");
        } catch (Exception e) {
            callback.onError("Normal mode nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Get current ringer mode
     */
    public int getRingerMode() {
        return audioManager.getRingerMode();
    }

    /**
     * Get ringer mode name
     */
    public String getRingerModeName() {
        int mode = audioManager.getRingerMode();
        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                return "Silent";
            case AudioManager.RINGER_MODE_VIBRATE:
                return "Vibrate";
            case AudioManager.RINGER_MODE_NORMAL:
                return "Normal";
            default:
                return "Unknown";
        }
    }

    /**
     * Toggle between silent, vibrate, and normal
     */
    public void toggleRingerMode(AudioCallback callback) {
        int currentMode = audioManager.getRingerMode();
        
        switch (currentMode) {
            case AudioManager.RINGER_MODE_NORMAL:
                setSilentMode(callback);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                setVibrateMode(callback);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                setNormalMode(callback);
                break;
            default:
                setNormalMode(callback);
        }
    }

    // ==================== Do Not Disturb ====================

    /**
     * Check if DND access is granted
     */
    public boolean hasDndAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager.isNotificationPolicyAccessGranted();
        }
        return true;
    }

    /**
     * Enable Do Not Disturb
     */
    public void enableDND(AudioCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasDndAccess()) {
                try {
                    notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                    callback.onSuccess("Do Not Disturb on ho gaya!");
                } catch (Exception e) {
                    callback.onError("DND enable nahi ho pa raha: " + e.getMessage());
                }
            } else {
                openDndSettings(callback);
            }
        } else {
            setSilentMode(callback);
        }
    }

    /**
     * Disable Do Not Disturb
     */
    public void disableDND(AudioCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasDndAccess()) {
                try {
                    notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_ALL);
                    callback.onSuccess("Do Not Disturb band ho gaya!");
                } catch (Exception e) {
                    callback.onError("DND disable nahi ho pa raha: " + e.getMessage());
                }
            } else {
                openDndSettings(callback);
            }
        } else {
            setNormalMode(callback);
        }
    }

    /**
     * Toggle Do Not Disturb
     */
    public void toggleDND(AudioCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int filter = notificationManager.getCurrentInterruptionFilter();
            if (filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                filter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
                disableDND(callback);
            } else {
                enableDND(callback);
            }
        }
    }

    /**
     * Check if DND is enabled
     */
    public boolean isDndEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int filter = notificationManager.getCurrentInterruptionFilter();
            return filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                   filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                   filter == NotificationManager.INTERRUPTION_FILTER_ALARMS;
        }
        return audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
    }

    /**
     * Set DND to Alarms Only
     */
    public void setDndAlarmsOnly(AudioCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasDndAccess()) {
            try {
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_ALARMS);
                callback.onSuccess("Alarms only mode on ho gaya!");
            } catch (Exception e) {
                callback.onError("Alarms only mode nahi ho pa raha");
            }
        } else {
            openDndSettings(callback);
        }
    }

    /**
     * Set DND to Total Silence
     */
    public void setDndTotalSilence(AudioCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasDndAccess()) {
            try {
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_NONE);
                callback.onSuccess("Total silence mode on ho gaya!");
            } catch (Exception e) {
                callback.onError("Total silence mode nahi ho pa raha");
            }
        } else {
            openDndSettings(callback);
        }
    }

    /**
     * Open DND settings
     */
    public void openDndSettings(AudioCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("DND settings khul gayi. Permission enable karein.");
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_ZEN_MODE_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Zen mode settings khul gayi!");
            } catch (Exception e2) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        }
    }

    // ==================== Audio Profiles ====================

    /**
     * Create audio profile
     */
    public AudioProfile createProfile(String name, int mediaVolume, int ringVolume, 
            int alarmVolume, int ringerMode, boolean dndEnabled) {
        AudioProfile profile = new AudioProfile();
        profile.name = name;
        profile.mediaVolume = mediaVolume;
        profile.ringVolume = ringVolume;
        profile.alarmVolume = alarmVolume;
        profile.ringerMode = ringerMode;
        profile.dndEnabled = dndEnabled;
        return profile;
    }

    /**
     * Apply audio profile
     */
    public void applyProfile(AudioProfile profile, AudioCallback callback) {
        // Set volumes
        setMediaVolume(profile.mediaVolume, new AudioCallback() {
            @Override public void onSuccess(String message) {}
            @Override public void onError(String error) {}
            @Override public void onVolumeChanged(int volume, int maxVolume, int streamType) {}
            @Override public void onProfileChanged(AudioProfile p) {}
        });
        setRingtoneVolume(profile.ringVolume, new AudioCallback() {
            @Override public void onSuccess(String message) {}
            @Override public void onError(String error) {}
            @Override public void onVolumeChanged(int volume, int maxVolume, int streamType) {}
            @Override public void onProfileChanged(AudioProfile p) {}
        });
        setAlarmVolume(profile.alarmVolume, new AudioCallback() {
            @Override public void onSuccess(String message) {}
            @Override public void onError(String error) {}
            @Override public void onVolumeChanged(int volume, int maxVolume, int streamType) {}
            @Override public void onProfileChanged(AudioProfile p) {}
        });
        
        // Set ringer mode
        audioManager.setRingerMode(profile.ringerMode);
        
        // Set DND
        if (profile.dndEnabled) {
            enableDND(new AudioCallback() {
                @Override public void onSuccess(String message) {}
                @Override public void onError(String error) {}
                @Override public void onVolumeChanged(int volume, int maxVolume, int streamType) {}
                @Override public void onProfileChanged(AudioProfile p) {}
            });
        } else {
            disableDND(new AudioCallback() {
                @Override public void onSuccess(String message) {}
                @Override public void onError(String error) {}
                @Override public void onVolumeChanged(int volume, int maxVolume, int streamType) {}
                @Override public void onProfileChanged(AudioProfile p) {}
            });
        }
        
        callback.onSuccess("Profile '" + profile.name + "' apply ho gaya!");
        callback.onProfileChanged(profile);
    }

    /**
     * Get preset profiles
     */
    public Map<String, AudioProfile> getPresetProfiles() {
        Map<String, AudioProfile> profiles = new HashMap<>();
        
        // Normal profile
        profiles.put("normal", createProfile("Normal", 70, 70, 70, 
            AudioManager.RINGER_MODE_NORMAL, false));
        
        // Silent profile
        profiles.put("silent", createProfile("Silent", 50, 0, 70, 
            AudioManager.RINGER_MODE_SILENT, false));
        
        // Vibrate profile
        profiles.put("vibrate", createProfile("Vibrate", 50, 50, 70, 
            AudioManager.RINGER_MODE_VIBRATE, false));
        
        // Meeting profile
        profiles.put("meeting", createProfile("Meeting", 20, 0, 50, 
            AudioManager.RINGER_MODE_VIBRATE, true));
        
        // Night profile
        profiles.put("night", createProfile("Night", 30, 10, 40, 
            AudioManager.RINGER_MODE_SILENT, true));
        
        // Outdoor profile
        profiles.put("outdoor", createProfile("Outdoor", 100, 100, 100, 
            AudioManager.RINGER_MODE_NORMAL, false));
        
        return profiles;
    }

    /**
     * Apply preset profile by name
     */
    public void applyPresetProfile(String profileName, AudioCallback callback) {
        Map<String, AudioProfile> profiles = getPresetProfiles();
        AudioProfile profile = profiles.get(profileName.toLowerCase());
        
        if (profile != null) {
            applyProfile(profile, callback);
        } else {
            callback.onError("Profile '" + profileName + "' nahi mila");
        }
    }

    /**
     * Get current audio profile
     */
    public AudioProfile getCurrentProfile() {
        AudioProfile profile = new AudioProfile();
        profile.name = "Current";
        
        // Get volumes
        int maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        
        profile.mediaVolume = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxMedia;
        profile.ringVolume = (audioManager.getStreamVolume(AudioManager.STREAM_RING) * 100) / maxRing;
        profile.alarmVolume = (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) * 100) / maxAlarm;
        
        profile.ringerMode = audioManager.getRingerMode();
        profile.dndEnabled = isDndEnabled();
        
        return profile;
    }

    // ==================== Audio Device Info ====================

    /**
     * Check if headphones are connected
     */
    public boolean isHeadphonesConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if Bluetooth audio is connected
     */
    public boolean isBluetoothAudioConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    return true;
                }
            }
        }
        return audioManager.isBluetoothScoAvailableOffCall();
    }

    /**
     * Get connected audio devices
     */
    public String[] getConnectedAudioDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            String[] deviceNames = new String[devices.length];
            
            for (int i = 0; i < devices.length; i++) {
                deviceNames[i] = getAudioDeviceName(devices[i].getType());
            }
            
            return deviceNames;
        }
        return new String[0];
    }

    private String getAudioDeviceName(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "Built-in Speaker";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "Earpiece";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "Wired Headphones";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "Wired Headset";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "Bluetooth SCO";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "Bluetooth A2DP";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "USB Audio";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "USB Accessory";
            case AudioDeviceInfo.TYPE_DOCK:
                return "Dock";
            case AudioDeviceInfo.TYPE_FM:
                return "FM";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "Built-in Mic";
            case AudioDeviceInfo.TYPE_FM_TUNER:
                return "FM Tuner";
            case AudioDeviceInfo.TYPE_TV_TUNER:
                return "TV Tuner";
            case AudioDeviceInfo.TYPE_TELEPHONY:
                return "Telephony";
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return "Aux Line";
            case AudioDeviceInfo.TYPE_IP:
                return "Network";
            case AudioDeviceInfo.TYPE_BUS:
                return "Bus";
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "USB Headset";
            case AudioDeviceInfo.TYPE_HEARING_AID:
                return "Hearing Aid";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE:
                return "Built-in Speaker Safe";
            case AudioDeviceInfo.TYPE_REMOTE_SUBMIX:
                return "Remote Submix";
            default:
                return "Unknown";
        }
    }

    // ==================== Audio Info ====================

    /**
     * Get audio status summary
     */
    public String getAudioStatusSummary() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Ringer Mode: ").append(getRingerModeName()).append("\n");
        sb.append("DND: ").append(isDndEnabled() ? "On" : "Off").append("\n");
        sb.append("\nVolumes:\n");
        
        int maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        
        int mediaVol = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxMedia;
        int ringVol = (audioManager.getStreamVolume(AudioManager.STREAM_RING) * 100) / maxRing;
        int alarmVol = (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) * 100) / maxAlarm;
        
        sb.append("Media: ").append(mediaVol).append("%\n");
        sb.append("Ringtone: ").append(ringVol).append("%\n");
        sb.append("Alarm: ").append(alarmVol).append("%\n");
        
        sb.append("Headphones: ").append(isHeadphonesConnected() ? "Connected" : "Not Connected");
        
        return sb.toString();
    }

    /**
     * Play test tone
     */
    public void playTestTone(AudioCallback callback) {
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            }
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500);
            callback.onSuccess("Test tone play ho gaya!");
        } catch (Exception e) {
            callback.onError("Test tone play nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Release resources
     */
    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * Open sound settings
     */
    public void openSoundSettings(AudioCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Sound settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }
}
