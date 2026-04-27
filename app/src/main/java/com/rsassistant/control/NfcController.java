package com.rsassistant.control;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * NfcController - NFC Control and Tag Detection Manager
 * 
 * Supported Features:
 * - NFC toggle control
 * - NFC tag detection
 * - Quick NFC actions (read/write)
 */
public class NfcController {

    private static final String TAG = "NfcController";
    private final Context context;
    private final NfcAdapter nfcAdapter;
    private final NfcManager nfcManager;
    
    private boolean isTagDetected = false;
    private Tag detectedTag;
    private NfcCallback currentCallback;

    public interface NfcCallback {
        void onSuccess(String message);
        void onError(String error);
        void onTagDetected(TagInfo tagInfo);
    }

    public static class TagInfo {
        public String id;
        public String[] techList;
        public String ndefMessage;
        public boolean writable;
        public int maxSize;
    }

    public NfcController(Context context) {
        this.context = context;
        this.nfcManager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        this.nfcAdapter = nfcManager != null ? nfcManager.getDefaultAdapter() : null;
    }

    // ==================== NFC Toggle Controls ====================

    /**
     * Check if NFC is available on device
     */
    public boolean isNfcAvailable() {
        return nfcAdapter != null;
    }

    /**
     * Check if NFC is enabled
     */
    public boolean isNfcEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    /**
     * Enable NFC - Opens settings (cannot enable programmatically)
     */
    public void enableNFC(NfcCallback callback) {
        if (nfcAdapter == null) {
            callback.onError("NFC hardware available nahi hai");
            return;
        }

        if (nfcAdapter.isEnabled()) {
            callback.onSuccess("NFC already on hai!");
            return;
        }

        try {
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("NFC settings khul gayi. Please enable karein.");
        } catch (Exception e) {
            callback.onError("NFC settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    /**
     * Disable NFC - Opens settings (cannot disable programmatically)
     */
    public void disableNFC(NfcCallback callback) {
        if (nfcAdapter == null) {
            callback.onError("NFC hardware available nahi hai");
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            callback.onSuccess("NFC already band hai!");
            return;
        }

        try {
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("NFC settings khul gayi. Please disable karein.");
        } catch (Exception e) {
            callback.onError("NFC settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    /**
     * Toggle NFC - Opens settings
     */
    public void toggleNFC(NfcCallback callback) {
        if (isNfcEnabled()) {
            disableNFC(callback);
        } else {
            enableNFC(callback);
        }
    }

    /**
     * Open NFC settings
     */
    public void openNFCSettings(NfcCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("NFC settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    // ==================== NFC Tag Detection ====================

    /**
     * Enable foreground dispatch for tag detection
     * Call this in Activity's onResume()
     */
    public void enableForegroundDispatch(Activity activity) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            return;
        }

        try {
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity(activity, 0, intent, 
                    PendingIntent.FLAG_MUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
            }

            IntentFilter[] intentFilters = new IntentFilter[] {
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            };

            String[][] techLists = new String[][] {
                new String[] { Ndef.class.getName() }
            };

            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techLists);
            Log.d(TAG, "Foreground dispatch enabled");
        } catch (Exception e) {
            Log.e(TAG, "Error enabling foreground dispatch: " + e.getMessage());
        }
    }

    /**
     * Disable foreground dispatch
     * Call this in Activity's onPause()
     */
    public void disableForegroundDispatch(Activity activity) {
        if (nfcAdapter == null) {
            return;
        }

        try {
            nfcAdapter.disableForegroundDispatch(activity);
            Log.d(TAG, "Foreground dispatch disabled");
        } catch (Exception e) {
            Log.e(TAG, "Error disabling foreground dispatch: " + e.getMessage());
        }
    }

    /**
     * Handle intent from NFC tag detection
     */
    public void handleIntent(Intent intent, NfcCallback callback) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        this.currentCallback = callback;

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                this.detectedTag = tag;
                this.isTagDetected = true;
                processTag(tag, intent, callback);
            }
        }
    }

    /**
     * Process detected NFC tag
     */
    private void processTag(Tag tag, Intent intent, NfcCallback callback) {
        TagInfo tagInfo = new TagInfo();
        
        // Get tag ID
        byte[] id = tag.getId();
        if (id != null) {
            tagInfo.id = bytesToHexString(id);
        }

        // Get supported technologies
        tagInfo.techList = tag.getTechList();

        // Try to read NDEF message
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs != null && rawMsgs.length > 0) {
            NdefMessage ndefMessage = (NdefMessage) rawMsgs[0];
            tagInfo.ndefMessage = extractTextFromNdefMessage(ndefMessage);
        } else {
            // Try to read from tag directly
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                try {
                    ndef.connect();
                    NdefMessage ndefMessage = ndef.getNdefMessage();
                    if (ndefMessage != null) {
                        tagInfo.ndefMessage = extractTextFromNdefMessage(ndefMessage);
                    }
                    tagInfo.writable = ndef.isWritable();
                    tagInfo.maxSize = ndef.getMaxSize();
                    ndef.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error reading NDEF: " + e.getMessage());
                }
            }
        }

        callback.onTagDetected(tagInfo);
    }

    /**
     * Extract text from NDEF message
     */
    private String extractTextFromNdefMessage(NdefMessage message) {
        if (message == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        NdefRecord[] records = message.getRecords();
        
        for (NdefRecord record : records) {
            if (record != null) {
                try {
                    String text = parseNdefRecord(record);
                    if (text != null && !text.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(text);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing record: " + e.getMessage());
                }
            }
        }

        return sb.toString();
    }

    /**
     * Parse NDEF record
     */
    private String parseNdefRecord(NdefRecord record) {
        if (record == null) {
            return null;
        }

        // Check for text record
        if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
            if (java.util.Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                return parseTextRecord(record);
            } else if (java.util.Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                return parseUriRecord(record);
            }
        }

        // Check for URI record
        if (record.getTnf() == NdefRecord.TNF_ABSOLUTE_URI ||
            record.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
            byte[] payload = record.getPayload();
            if (payload != null) {
                return new String(payload, Charset.forName("UTF-8"));
            }
        }

        // Check for MIME type
        if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA) {
            byte[] payload = record.getPayload();
            if (payload != null) {
                return new String(payload, Charset.forName("UTF-8"));
            }
        }

        return null;
    }

    /**
     * Parse text record
     */
    private String parseTextRecord(NdefRecord record) {
        byte[] payload = record.getPayload();
        if (payload == null || payload.length == 0) {
            return "";
        }

        // Get text encoding
        String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
        
        // Get language code length
        int languageCodeLength = payload[0] & 0x3F;
        
        // Get text
        try {
            return new String(payload, languageCodeLength + 1, 
                payload.length - languageCodeLength - 1, textEncoding);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing text: " + e.getMessage());
            return "";
        }
    }

    /**
     * Parse URI record
     */
    private String parseUriRecord(NdefRecord record) {
        byte[] payload = record.getPayload();
        if (payload == null || payload.length < 2) {
            return "";
        }

        // Get URI prefix
        String prefix = getUriPrefix(payload[0]);
        
        // Get URI
        String uri = new String(payload, 1, payload.length - 1, Charset.forName("UTF-8"));
        
        return prefix + uri;
    }

    /**
     * Get URI prefix based on identifier
     */
    private String getUriPrefix(byte identifier) {
        switch (identifier) {
            case 0x00: return "";
            case 0x01: return "http://www.";
            case 0x02: return "https://www.";
            case 0x03: return "http://";
            case 0x04: return "https://";
            case 0x05: return "tel:";
            case 0x06: return "mailto:";
            case 0x07: return "ftp://anonymous:anonymous@";
            case 0x08: return "ftp://ftp.";
            case 0x09: return "ftps://";
            case 0x0A: return "sftp://";
            case 0x0B: return "smb://";
            case 0x0C: return "nfs://";
            case 0x0D: return "ftp://";
            case 0x0E: return "dav://";
            case 0x0F: return "news:";
            case 0x10: return "telnet://";
            case 0x11: return "imap:";
            case 0x12: return "rtsp://";
            case 0x13: return "urn:";
            case 0x14: return "pop:";
            case 0x15: return "sip:";
            case 0x16: return "sips:";
            case 0x17: return "tftp:";
            case 0x18: return "btspp://";
            case 0x19: return "btl2cap://";
            case 0x1A: return "btgoep://";
            case 0x1B: return "tcpobex://";
            case 0x1C: return "irdaobex://";
            case 0x1D: return "file://";
            case 0x1E: return "urn:epc:id:";
            case 0x1F: return "urn:epc:tag:";
            default: return "";
        }
    }

    // ==================== Quick NFC Actions ====================

    /**
     * Create NDEF text record
     */
    public NdefRecord createTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);
        
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);
        
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, 
            new byte[0], data);
    }

    /**
     * Create NDEF URI record
     */
    public NdefRecord createUriRecord(String uri) {
        return NdefRecord.createUri(uri);
    }

    /**
     * Create NDEF message with text
     */
    public NdefMessage createTextMessage(String text) {
        NdefRecord record = createTextRecord(text, Locale.getDefault(), true);
        return new NdefMessage(new NdefRecord[] { record });
    }

    /**
     * Create NDEF message with URI
     */
    public NdefMessage createUriMessage(String uri) {
        NdefRecord record = createUriRecord(uri);
        return new NdefMessage(new NdefRecord[] { record });
    }

    /**
     * Write text to detected tag
     */
    public void writeTextToTag(String text, NfcCallback callback) {
        if (detectedTag == null) {
            callback.onError("Koi tag detect nahi hui");
            return;
        }

        Ndef ndef = Ndef.get(detectedTag);
        if (ndef == null) {
            callback.onError("Ye tag NDEF support nahi karti");
            return;
        }

        try {
            ndef.connect();
            
            if (!ndef.isWritable()) {
                ndef.close();
                callback.onError("Ye tag read-only hai");
                return;
            }

            NdefMessage message = createTextMessage(text);
            int size = message.toByteArray().length;
            
            if (size > ndef.getMaxSize()) {
                ndef.close();
                callback.onError("Message tag se bada hai");
                return;
            }

            ndef.writeNdefMessage(message);
            ndef.close();
            
            callback.onSuccess("Tag successfully likh di gayi!");
        } catch (Exception e) {
            callback.onError("Tag likhne mein error: " + e.getMessage());
        }
    }

    /**
     * Write URI to detected tag
     */
    public void writeUriToTag(String uri, NfcCallback callback) {
        if (detectedTag == null) {
            callback.onError("Koi tag detect nahi hui");
            return;
        }

        Ndef ndef = Ndef.get(detectedTag);
        if (ndef == null) {
            callback.onError("Ye tag NDEF support nahi karti");
            return;
        }

        try {
            ndef.connect();
            
            if (!ndef.isWritable()) {
                ndef.close();
                callback.onError("Ye tag read-only hai");
                return;
            }

            NdefMessage message = createUriMessage(uri);
            int size = message.toByteArray().length;
            
            if (size > ndef.getMaxSize()) {
                ndef.close();
                callback.onError("Message tag se bada hai");
                return;
            }

            ndef.writeNdefMessage(message);
            ndef.close();
            
            callback.onSuccess("URI successfully likh di gayi!");
        } catch (Exception e) {
            callback.onError("Tag likhne mein error: " + e.getMessage());
        }
    }

    /**
     * Format tag to NDEF (erases existing content)
     */
    public void formatTag(NfcCallback callback) {
        if (detectedTag == null) {
            callback.onError("Koi tag detect nahi hui");
            return;
        }

        // Note: Formatting requires NdefFormatable class
        callback.onError("Tag formatting ke liye NdefFormatable support chahiye");
    }

    /**
     * Make tag read-only
     */
    public void makeTagReadOnly(NfcCallback callback) {
        if (detectedTag == null) {
            callback.onError("Koi tag detect nahi hui");
            return;
        }

        Ndef ndef = Ndef.get(detectedTag);
        if (ndef == null) {
            callback.onError("Ye tag NDEF support nahi karti");
            return;
        }

        try {
            ndef.connect();
            boolean success = ndef.makeReadOnly();
            ndef.close();
            
            if (success) {
                callback.onSuccess("Tag ab read-only hai!");
            } else {
                callback.onError("Tag read-only nahi ban pa rahi");
            }
        } catch (Exception e) {
            callback.onError("Error making tag read-only: " + e.getMessage());
        }
    }

    /**
     * Check if tag is detected
     */
    public boolean isTagDetected() {
        return isTagDetected;
    }

    /**
     * Get detected tag
     */
    public Tag getDetectedTag() {
        return detectedTag;
    }

    /**
     * Clear detected tag
     */
    public void clearDetectedTag() {
        detectedTag = null;
        isTagDetected = false;
    }

    /**
     * Share content via Android Beam (deprecated in API 29)
     */
    @SuppressWarnings("deprecation")
    public void shareViaBeam(Activity activity, String text, NfcCallback callback) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            callback.onError("NFC enabled nahi hai");
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            NdefMessage message = createTextMessage(text);
            nfcAdapter.setNdefPushMessage(message, activity);
            callback.onSuccess("Beam ke liye ready! Do phones ko peeche se lagaayein");
        } else {
            callback.onError("Android Beam deprecated hai. Alternative sharing method use karein");
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Convert bytes to hex string
     */
    private String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Get NFC adapter for advanced operations
     */
    public NfcAdapter getNfcAdapter() {
        return nfcAdapter;
    }

    /**
     * Check if NFC is supported
     */
    public boolean isNfcSupported() {
        return context.getPackageManager().hasSystemFeature("android.hardware.nfc");
    }
}
