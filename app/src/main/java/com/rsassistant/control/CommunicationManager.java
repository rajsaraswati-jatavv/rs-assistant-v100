package com.rsassistant.control;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CommunicationManager - Voice-based Communication Manager
 * 
 * Handles all communication-related voice commands:
 * - Voice calling with contact matching
 * - SMS sending by voice
 * - WhatsApp message sending
 * - Email sending
 * - Read last messages
 * 
 * Supports Hindi-English bilingual commands
 */
public class CommunicationManager {

    private static final String TAG = "CommunicationManager";
    private final Context context;
    private final ContentResolver contentResolver;

    public interface CommunicationCallback {
        void onSuccess(String message);
        void onError(String error);
        void onContactFound(String name, String identifier);
    }

    /**
     * Contact information class
     */
    public static class ContactData {
        public String id;
        public String name;
        public String phoneNumber;
        public String email;
        public String photoUri;
        public int matchScore;
        
        public ContactData(String id, String name, String phoneNumber) {
            this.id = id;
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.matchScore = 0;
        }
    }

    public CommunicationManager(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    // ==================== CALLING ====================

    /**
     * Make a call to a contact or phone number
     * Supports: "Raj ko call karo", "Call mom", "9876543210 ko phone karo"
     * 
     * @param nameOrNumber Contact name or phone number
     * @param callback Result callback
     */
    public void makeCall(String nameOrNumber, CommunicationCallback callback) {
        if (nameOrNumber == null || nameOrNumber.trim().isEmpty()) {
            callback.onError("Kaun ko call karna hai? Naam boliye.");
            return;
        }

        String input = nameOrNumber.trim();
        
        // Check if it's a phone number
        if (isPhoneNumber(input)) {
            makeCallToNumber(input, callback);
        } else {
            // It's a contact name - search and call
            makeCallToContact(input, callback);
        }
    }

    /**
     * Make call to a phone number directly
     */
    private void makeCallToNumber(String phoneNumber, CommunicationCallback callback) {
        if (!hasCallPermission()) {
            callback.onError("Call permission denied. Settings mein jao.");
            return;
        }

        try {
            String cleanNumber = cleanPhoneNumber(phoneNumber);
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + cleanNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
            callback.onSuccess("Call ho raha hai " + cleanNumber + " pe...");
        } catch (Exception e) {
            // Fallback to dialer
            try {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + phoneNumber));
                dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dialIntent);
                callback.onSuccess("Dialer khul gaya. Call button dabayein.");
            } catch (Exception e2) {
                callback.onError("Call nahi ho pa raha: " + e2.getMessage());
            }
        }
    }

    /**
     * Make call to contact by name
     */
    private void makeCallToContact(String contactName, CommunicationCallback callback) {
        if (!hasContactsPermission()) {
            callback.onError("Contacts permission denied");
            return;
        }

        ContactData contact = searchContact(contactName);
        
        if (contact != null) {
            callback.onContactFound(contact.name, contact.phoneNumber);
            makeCallToNumber(contact.phoneNumber, callback);
        } else {
            // No contact found - try to open dialer with name
            callback.onError("'" + contactName + "' contact nahi mila. Naam sahi se boliye.");
        }
    }

    // ==================== SMS ====================

    /**
     * Send SMS to contact or phone number
     * Supports: "Raj ko message bhejo hello", "Send SMS to mom saying hi"
     * 
     * @param nameOrNumber Contact name or phone number
     * @param message Message to send
     * @param callback Result callback
     */
    public void sendSMS(String nameOrNumber, String message, CommunicationCallback callback) {
        if (nameOrNumber == null || nameOrNumber.trim().isEmpty()) {
            callback.onError("Kaun ko message bhejna hai?");
            return;
        }

        String input = nameOrNumber.trim();
        
        if (isPhoneNumber(input)) {
            sendSMSToNumber(input, message, callback);
        } else {
            sendSMSToContact(input, message, callback);
        }
    }

    /**
     * Send SMS to phone number
     */
    private void sendSMSToNumber(String phoneNumber, String message, CommunicationCallback callback) {
        if (!hasSMSPermission()) {
            callback.onError("SMS permission denied");
            return;
        }

        try {
            String cleanNumber = cleanPhoneNumber(phoneNumber);
            
            // Open SMS app with pre-filled data
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + cleanNumber));
            if (message != null && !message.isEmpty()) {
                smsIntent.putExtra("sms_body", message);
            }
            smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(smsIntent);
            
            if (message != null && !message.isEmpty()) {
                callback.onSuccess("Message app khul gaya. Send button dabayein.");
            } else {
                callback.onSuccess("Message app khul gaya. Message likhelein.");
            }
        } catch (Exception e) {
            callback.onError("SMS app nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Send SMS to contact by name
     */
    private void sendSMSToContact(String contactName, String message, CommunicationCallback callback) {
        if (!hasContactsPermission()) {
            callback.onError("Contacts permission denied");
            return;
        }

        ContactData contact = searchContact(contactName);
        
        if (contact != null) {
            callback.onContactFound(contact.name, contact.phoneNumber);
            sendSMSToNumber(contact.phoneNumber, message, callback);
        } else {
            callback.onError("'" + contactName + "' contact nahi mila");
        }
    }

    /**
     * Send SMS directly (without opening SMS app)
     */
    public void sendSMSDirect(String phoneNumber, String message, CommunicationCallback callback) {
        if (!hasSMSPermission()) {
            callback.onError("SMS permission denied");
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // Split message if too long
            ArrayList<String> parts = smsManager.divideMessage(message);
            
            if (parts.size() > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }
            
            callback.onSuccess("SMS bhej diya!");
        } catch (Exception e) {
            callback.onError("SMS nahi bhej pa raha: " + e.getMessage());
        }
    }

    // ==================== WHATSAPP ====================

    /**
     * Send WhatsApp message
     * Supports: "Raj ko WhatsApp bhejo hello", "WhatsApp message to mom"
     * 
     * @param nameOrNumber Contact name or phone number
     * @param message Message to send
     * @param callback Result callback
     */
    public void sendWhatsApp(String nameOrNumber, String message, CommunicationCallback callback) {
        if (nameOrNumber == null || nameOrNumber.trim().isEmpty()) {
            callback.onError("Kaun ko WhatsApp bhejna hai?");
            return;
        }

        String input = nameOrNumber.trim();
        String phoneNumber = null;
        String contactName = null;
        
        if (isPhoneNumber(input)) {
            phoneNumber = input;
        } else {
            // Search contact
            if (hasContactsPermission()) {
                ContactData contact = searchContact(input);
                if (contact != null) {
                    phoneNumber = contact.phoneNumber;
                    contactName = contact.name;
                    callback.onContactFound(contactName, phoneNumber);
                }
            }
        }
        
        if (phoneNumber != null) {
            openWhatsAppWithNumber(phoneNumber, message, callback);
        } else {
            // Just open WhatsApp
            openWhatsApp(callback);
        }
    }

    /**
     * Open WhatsApp with specific number
     */
    private void openWhatsAppWithNumber(String phoneNumber, String message, CommunicationCallback callback) {
        try {
            // Clean phone number - remove spaces, dashes, etc.
            String cleanNumber = cleanPhoneNumber(phoneNumber);
            // Remove country code if present for WhatsApp API
            if (cleanNumber.startsWith("91") && cleanNumber.length() > 10) {
                cleanNumber = cleanNumber.substring(2);
            }
            
            // Open WhatsApp chat
            Uri uri = Uri.parse("https://wa.me/" + cleanNumber + 
                (message != null && !message.isEmpty() ? "?text=" + Uri.encode(message) : ""));
            
            Intent waIntent = new Intent(Intent.ACTION_VIEW, uri);
            waIntent.setPackage("com.whatsapp");
            waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(waIntent);
            
            callback.onSuccess("WhatsApp chat khul gaya!");
        } catch (Exception e) {
            // WhatsApp not installed - open Play Store
            try {
                Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
                playStoreIntent.setData(Uri.parse("market://details?id=com.whatsapp"));
                playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(playStoreIntent);
                callback.onError("WhatsApp installed nahi hai. Play Store khul gaya.");
            } catch (Exception e2) {
                callback.onError("WhatsApp nahi khul pa raha: " + e.getMessage());
            }
        }
    }

    /**
     * Open WhatsApp app
     */
    public void openWhatsApp(CommunicationCallback callback) {
        try {
            Intent waIntent = context.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
            if (waIntent != null) {
                waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(waIntent);
                callback.onSuccess("WhatsApp khul gaya!");
            } else {
                callback.onError("WhatsApp installed nahi hai");
            }
        } catch (Exception e) {
            callback.onError("WhatsApp nahi khul pa raha: " + e.getMessage());
        }
    }

    // ==================== EMAIL ====================

    /**
     * Send email
     * Supports: "Raj ko email bhejo", "Send email to boss about meeting"
     * 
     * @param nameOrEmail Contact name or email address
     * @param subject Email subject
     * @param body Email body
     * @param callback Result callback
     */
    public void sendEmail(String nameOrEmail, String subject, String body, CommunicationCallback callback) {
        if (nameOrEmail == null || nameOrEmail.trim().isEmpty()) {
            callback.onError("Kaun ko email bhejna hai?");
            return;
        }

        String input = nameOrEmail.trim();
        String emailAddress = null;
        String contactName = null;
        
        // Check if it's an email address
        if (isEmailAddress(input)) {
            emailAddress = input;
        } else {
            // Search contact for email
            if (hasContactsPermission()) {
                ContactData contact = searchContactByEmail(input);
                if (contact != null) {
                    emailAddress = contact.email;
                    contactName = contact.name;
                    callback.onContactFound(contactName, emailAddress);
                }
            }
        }
        
        if (emailAddress != null) {
            openEmailApp(emailAddress, subject, body, callback);
        } else {
            // Just open email app
            openEmailApp(callback);
        }
    }

    /**
     * Open email app with pre-filled data
     */
    private void openEmailApp(String email, String subject, String body, CommunicationCallback callback) {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + email));
            
            if (subject != null && !subject.isEmpty()) {
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            }
            if (body != null && !body.isEmpty()) {
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
            }
            
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(emailIntent, "Email bhejein..."));
            callback.onSuccess("Email app khul gaya!");
        } catch (Exception e) {
            callback.onError("Email app nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Open email app
     */
    public void openEmailApp(CommunicationCallback callback) {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_MAIN);
            emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(emailIntent);
            callback.onSuccess("Email app khul gaya!");
        } catch (Exception e) {
            // Try Gmail specifically
            try {
                Intent gmailIntent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
                if (gmailIntent != null) {
                    gmailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(gmailIntent);
                    callback.onSuccess("Gmail khul gaya!");
                } else {
                    callback.onError("Email app nahi mila");
                }
            } catch (Exception e2) {
                callback.onError("Email app nahi khul pa raha");
            }
        }
    }

    // ==================== READ MESSAGES ====================

    /**
     * Read last messages/notifications
     * Supports: "Last message padho", "Notifications padho"
     */
    public void readLastMessages(CommunicationCallback callback) {
        if (!hasSMSPermission()) {
            callback.onError("SMS permission chahiye messages padhne ke liye");
            return;
        }

        try {
            Uri smsUri = Uri.parse("content://sms/inbox");
            Cursor cursor = contentResolver.query(smsUri, null, null, null, "date DESC LIMIT 5");
            
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder sb = new StringBuilder("Paanch latest messages:\n\n");
                int count = 0;
                
                do {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    String dateStr = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    
                    // Try to get contact name
                    String senderName = getContactNameByNumber(address);
                    String displaySender = senderName != null ? senderName : address;
                    
                    sb.append(++count).append(". ").append(displaySender).append(": ");
                    sb.append(body.length() > 100 ? body.substring(0, 100) + "..." : body);
                    sb.append("\n\n");
                    
                } while (cursor.moveToNext() && count < 5);
                
                cursor.close();
                callback.onSuccess(sb.toString());
            } else {
                callback.onSuccess("Koi message nahi hai");
            }
        } catch (Exception e) {
            callback.onError("Messages padhne mein error: " + e.getMessage());
        }
    }

    /**
     * Get unread SMS count
     */
    public int getUnreadSMSCount() {
        if (!hasSMSPermission()) return 0;
        
        try {
            Uri smsUri = Uri.parse("content://sms/inbox");
            Cursor cursor = contentResolver.query(smsUri, null, "read = 0", null, null);
            int count = cursor != null ? cursor.getCount() : 0;
            if (cursor != null) cursor.close();
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== CONTACT SEARCH ====================

    /**
     * Search contact by name with fuzzy matching
     * 
     * @param name Contact name to search
     * @return Best matching contact or null
     */
    public ContactData searchContact(String name) {
        if (!hasContactsPermission()) return null;
        
        List<ContactData> contacts = searchContacts(name, 1);
        return contacts.isEmpty() ? null : contacts.get(0);
    }

    /**
     * Search contacts by name with fuzzy matching
     * 
     * @param name Contact name to search
     * @param limit Maximum results to return
     * @return List of matching contacts
     */
    public List<ContactData> searchContacts(String name, int limit) {
        List<ContactData> results = new ArrayList<>();
        
        if (!hasContactsPermission()) return results;
        
        String searchName = name.toLowerCase().trim();
        
        try {
            Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null, null,
                null
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    String contactName = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                    
                    // Calculate match score
                    int score = calculateNameSimilarity(searchName, contactName.toLowerCase());
                    
                    if (score >= 50) { // 50% threshold
                        ContactData contact = new ContactData(id, contactName, phoneNumber);
                        contact.matchScore = score;
                        
                        // Get email
                        contact.email = getContactEmail(id);
                        
                        results.add(contact);
                    }
                }
                cursor.close();
            }
            
            // Sort by match score
            results.sort((a, b) -> Integer.compare(b.matchScore, a.matchScore));
            
            // Limit results
            if (results.size() > limit) {
                results = new ArrayList<>(results.subList(0, limit));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error searching contacts: " + e.getMessage());
        }
        
        return results;
    }

    /**
     * Search contact by email
     */
    public ContactData searchContactByEmail(String name) {
        if (!hasContactsPermission()) return null;
        
        String searchName = name.toLowerCase().trim();
        
        try {
            Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                null, null,
                null
            );
            
            ContactData bestMatch = null;
            int bestScore = 0;
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String contactId = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID));
                    String contactName = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME));
                    String email = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Email.ADDRESS));
                    
                    int score = calculateNameSimilarity(searchName, contactName.toLowerCase());
                    
                    if (score > bestScore && score >= 50) {
                        bestScore = score;
                        bestMatch = new ContactData(contactId, contactName, null);
                        bestMatch.email = email;
                        bestMatch.matchScore = score;
                    }
                }
                cursor.close();
            }
            
            return bestMatch;
            
        } catch (Exception e) {
            Log.e(TAG, "Error searching contact by email: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get contact name by phone number
     */
    private String getContactNameByNumber(String phoneNumber) {
        if (!hasContactsPermission()) return null;
        
        try {
            Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            );
            
            Cursor cursor = contentResolver.query(uri, 
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, 
                null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                cursor.close();
                return name;
            }
            
            if (cursor != null) cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact name: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Get contact email by ID
     */
    private String getContactEmail(String contactId) {
        try {
            Cursor emailCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                new String[]{contactId},
                null
            );
            
            if (emailCursor != null && emailCursor.moveToFirst()) {
                String email = emailCursor.getString(emailCursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Email.ADDRESS));
                emailCursor.close();
                return email;
            }
            
            if (emailCursor != null) emailCursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact email: " + e.getMessage());
        }
        
        return null;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if string is a phone number
     */
    private boolean isPhoneNumber(String input) {
        // Remove common characters
        String cleaned = input.replaceAll("[\\s\\-\\(\\)\\+]", "");
        // Check if it's mostly digits
        int digitCount = 0;
        for (char c : cleaned.toCharArray()) {
            if (Character.isDigit(c)) digitCount++;
        }
        return digitCount >= 8 && (digitCount * 100 / cleaned.length()) >= 70;
    }

    /**
     * Check if string is an email address
     */
    private boolean isEmailAddress(String input) {
        return input.contains("@") && input.contains(".");
    }

    /**
     * Clean phone number for calling
     */
    private String cleanPhoneNumber(String number) {
        return number.replaceAll("[^0-9\\+]", "");
    }

    /**
     * Calculate name similarity (0-100)
     */
    private int calculateNameSimilarity(String name1, String name2) {
        if (name1.equals(name2)) return 100;
        if (name1.contains(name2) || name2.contains(name1)) return 90;
        
        // Check first name match
        String[] parts1 = name1.split("\\s+");
        String[] parts2 = name2.split("\\s+");
        
        for (String p1 : parts1) {
            for (String p2 : parts2) {
                if (p1.equals(p2)) return 95;
                if (p1.contains(p2) || p2.contains(p1)) return 80;
                if (calculateSimilarity(p1, p2) >= 70) return 70;
            }
        }
        
        // Levenshtein-based similarity
        return calculateSimilarity(name1, name2);
    }

    /**
     * Calculate string similarity using Levenshtein distance
     */
    private int calculateSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 100;
        return ((maxLength - distance) * 100) / maxLength;
    }

    /**
     * Calculate Levenshtein distance
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    // ==================== PERMISSION HELPERS ====================

    private boolean hasContactsPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCallPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasSMSPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            == PackageManager.PERMISSION_GRANTED;
    }
}
