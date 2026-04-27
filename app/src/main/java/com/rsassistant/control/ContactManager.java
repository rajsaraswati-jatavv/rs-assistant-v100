package com.rsassistant.control;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ContactManager - Voice-based Contact and Communication Manager
 * 
 * Supported Commands:
 * - "Raj ko call karo" / "Call Raj"
 * - "Mummy ko SMS bhejo" / "Send SMS to Mom"
 * - "Contacts dikao" / "Show contacts"
 * - "Naya contact add karo" / "Add new contact"
 */
public class ContactManager {

    private static final String TAG = "ContactManager";
    private final Context context;

    public interface ContactCallback {
        void onSuccess(String message);
        void onError(String error);
        void onContactList(List<ContactInfo> contacts);
        void onContactFound(ContactInfo contact);
    }

    public static class ContactInfo {
        public String id;
        public String name;
        public String phoneNumber;
        public String email;
        public String photoUri;

        public ContactInfo(String id, String name, String phoneNumber) {
            this.id = id;
            this.name = name;
            this.phoneNumber = phoneNumber;
        }
    }

    public ContactManager(Context context) {
        this.context = context;
    }

    /**
     * Get all contacts
     */
    public void getAllContacts(ContactCallback callback) {
        if (!hasContactsPermission()) {
            callback.onError("Contacts permission denied");
            return;
        }

        List<ContactInfo> contacts = new ArrayList<>();
        
        try {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                    
                    // Get phone number
                    String phoneNumber = null;
                    Cursor phoneCursor = resolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id},
                        null
                    );
                    
                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phoneCursor.close();
                    }

                    if (phoneNumber != null && !phoneNumber.isEmpty()) {
                        ContactInfo contact = new ContactInfo(id, name, phoneNumber);
                        
                        // Get email
                        Cursor emailCursor = resolver.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            new String[]{id},
                            null
                        );
                        
                        if (emailCursor != null && emailCursor.moveToFirst()) {
                            contact.email = emailCursor.getString(emailCursor.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Email.ADDRESS));
                            emailCursor.close();
                        }
                        
                        // Get photo URI
                        String photoUri = cursor.getString(cursor.getColumnIndexOrThrow(
                            ContactsContract.Contacts.PHOTO_URI));
                        contact.photoUri = photoUri;
                        
                        contacts.add(contact);
                    }
                }
                cursor.close();
            }

            callback.onContactList(contacts);
        } catch (Exception e) {
            callback.onError("Contacts load nahi ho pa rahe: " + e.getMessage());
        }
    }

    /**
     * Search contact by name
     */
    public void searchContact(String name, ContactCallback callback) {
        if (!hasContactsPermission()) {
            callback.onError("Contacts permission denied");
            return;
        }

        try {
            ContentResolver resolver = context.getContentResolver();
            String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = new String[]{"%" + name + "%"};

            Cursor cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null
            );

            if (cursor != null && cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                String contactName = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER));

                cursor.close();
                
                ContactInfo contact = new ContactInfo(id, contactName, phoneNumber);
                callback.onContactFound(contact);
            } else {
                callback.onError(name + " contact nahi mila");
            }
        } catch (Exception e) {
            callback.onError("Contact search error: " + e.getMessage());
        }
    }

    /**
     * Make a call to contact
     */
    public void callContact(String name, ContactCallback callback) {
        searchContact(name, new ContactCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onContactList(List<ContactInfo> contacts) {}

            @Override
            public void onContactFound(ContactInfo contact) {
                // Make the call
                callPhoneNumber(contact.phoneNumber, callback);
            }
        });
    }

    /**
     * Make a call to phone number directly
     */
    public void callPhoneNumber(String phoneNumber, ContactCallback callback) {
        if (!hasCallPermission()) {
            callback.onError("Call permission denied");
            return;
        }

        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Call ho raha hai...");
        } catch (Exception e) {
            callback.onError("Call nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Send SMS to contact
     */
    public void sendSMS(String contactName, String message, ContactCallback callback) {
        searchContact(contactName, new ContactCallback() {
            @Override
            public void onSuccess(String msg) {}

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onContactList(List<ContactInfo> contacts) {}

            @Override
            public void onContactFound(ContactInfo contact) {
                sendSMSToNumber(contact.phoneNumber, message, callback);
            }
        });
    }

    /**
     * Send SMS to phone number directly
     */
    public void sendSMSToNumber(String phoneNumber, String message, ContactCallback callback) {
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

    /**
     * Open SMS app with pre-filled number and message
     */
    public void openSMSApp(String phoneNumber, String message, ContactCallback callback) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phoneNumber));
            if (message != null && !message.isEmpty()) {
                intent.putExtra("sms_body", message);
            }
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("SMS app khul gaya!");
        } catch (Exception e) {
            callback.onError("SMS app nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Add new contact
     */
    public void addContact(String name, String phoneNumber, String email, ContactCallback callback) {
        if (!hasWriteContactsPermission()) {
            callback.onError("Write contacts permission denied");
            return;
        }

        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            // Insert raw contact
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

            // Insert name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());

            // Insert phone
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());
            }

            // Insert email
            if (email != null && !email.isEmpty()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    .build());
            }

            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            callback.onSuccess(name + " contact add ho gaya!");
        } catch (Exception e) {
            callback.onError("Contact add nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Open add contact screen
     */
    public void openAddContactScreen(ContactCallback callback) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_INSERT);
            intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Add contact screen khul gaya!");
        } catch (Exception e) {
            callback.onError("Screen nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Delete contact
     */
    public void deleteContact(String name, ContactCallback callback) {
        if (!hasWriteContactsPermission()) {
            callback.onError("Write contacts permission denied");
            return;
        }

        searchContact(name, new ContactCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onContactList(List<ContactInfo> contacts) {}

            @Override
            public void onContactFound(ContactInfo contact) {
                try {
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id);
                    context.getContentResolver().delete(uri, null, null);
                    callback.onSuccess(contact.name + " delete ho gaya!");
                } catch (Exception e) {
                    callback.onError("Delete nahi ho pa raha: " + e.getMessage());
                }
            }
        });
    }

    // Permission helpers
    private boolean hasContactsPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasWriteContactsPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS)
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

    /**
     * Get frequent/recent contacts
     */
    public List<ContactInfo> getFrequentContacts(int limit) {
        List<ContactInfo> contacts = new ArrayList<>();
        
        if (!hasContactsPermission()) {
            return contacts;
        }

        try {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                ContactsContract.Contacts.TIMES_CONTACTED + " > 0",
                null,
                ContactsContract.Contacts.TIMES_CONTACTED + " DESC LIMIT " + limit
            );

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                    
                    // Get phone
                    Cursor phoneCursor = resolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id},
                        null
                    );
                    
                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        String phone = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                        contacts.add(new ContactInfo(id, name, phone));
                        phoneCursor.close();
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting frequent contacts: " + e.getMessage());
        }

        return contacts;
    }
}
