package com.rsassistant.memory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * MemoryManager - Self-Learning Memory System
 * Saves all commands, features, and user preferences
 * Auto-learns from user behavior
 */
public class MemoryManager extends SQLiteOpenHelper {

    private static final String DB_NAME = "rs_assistant_memory.db";
    private static final int DB_VERSION = 1;

    // Tables
    private static final String TABLE_COMMANDS = "command_history";
    private static final String TABLE_FEATURES = "feature_usage";
    private static final String TABLE_PATTERNS = "user_patterns";
    private static final String TABLE_SHORTCUTS = "voice_shortcuts";

    // Command History Table
    private static final String CREATE_COMMANDS = "CREATE TABLE " + TABLE_COMMANDS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "command TEXT NOT NULL, " +
            "response TEXT, " +
            "timestamp LONG, " +
            "success INTEGER, " +
            "category TEXT)";

    // Feature Usage Table
    private static final String CREATE_FEATURES = "CREATE TABLE " + TABLE_FEATURES + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "feature_name TEXT NOT NULL, " +
            "use_count INTEGER DEFAULT 1, " +
            "last_used LONG, " +
            "is_favorite INTEGER DEFAULT 0)";

    // User Patterns Table
    private static final String CREATE_PATTERNS = "CREATE TABLE " + TABLE_PATTERNS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "pattern_type TEXT NOT NULL, " +
            "pattern_data TEXT, " +
            "time_of_day INTEGER, " +
            "day_of_week INTEGER, " +
            "frequency INTEGER DEFAULT 1)";

    // Voice Shortcuts Table
    private static final String CREATE_SHORTCUTS = "CREATE TABLE " + TABLE_SHORTCUTS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "trigger_phrase TEXT NOT NULL UNIQUE, " +
            "action_type TEXT, " +
            "action_data TEXT, " +
            "created_at LONG)";

    private static MemoryManager instance;
    private final Context context;

    public static synchronized MemoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new MemoryManager(context.getApplicationContext());
        }
        return instance;
    }

    private MemoryManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_COMMANDS);
        db.execSQL(CREATE_FEATURES);
        db.execSQL(CREATE_PATTERNS);
        db.execSQL(CREATE_SHORTCUTS);
        
        // Insert default features
        insertDefaultFeatures(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMANDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEATURES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATTERNS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHORTCUTS);
        onCreate(db);
    }

    private void insertDefaultFeatures(SQLiteDatabase db) {
        String[] features = {
            "Volume Control", "Flashlight", "Camera", "WiFi", "Bluetooth",
            "Lock Screen", "Power Off", "Silent Mode", "Vibrate Mode",
            "Screenshot", "Media Control", "Calls", "SMS", "Alarms",
            "Reminders", "Time", "Date", "Weather", "Jokes", "SOS",
            "Auto Reply", "Custom Commands", "Shake Torch", "Quick Actions"
        };
        
        for (String feature : features) {
            ContentValues values = new ContentValues();
            values.put("feature_name", feature);
            values.put("use_count", 0);
            values.put("last_used", 0);
            values.put("is_favorite", 0);
            db.insert(TABLE_FEATURES, null, values);
        }
    }

    // ==================== COMMAND HISTORY ====================

    public void saveCommand(String command, String response, boolean success, String category) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("command", command);
        values.put("response", response);
        values.put("timestamp", System.currentTimeMillis());
        values.put("success", success ? 1 : 0);
        values.put("category", category);
        db.insert(TABLE_COMMANDS, null, values);
        
        // Learn from this command
        learnPattern(command, category);
    }

    public List<CommandRecord> getCommandHistory(int limit) {
        List<CommandRecord> history = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_COMMANDS, null, null, null, null, null, 
                "timestamp DESC", String.valueOf(limit));
        
        if (cursor.moveToFirst()) {
            do {
                CommandRecord record = new CommandRecord();
                record.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                record.command = cursor.getString(cursor.getColumnIndexOrThrow("command"));
                record.response = cursor.getString(cursor.getColumnIndexOrThrow("response"));
                record.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                record.success = cursor.getInt(cursor.getColumnIndexOrThrow("success")) == 1;
                record.category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                history.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return history;
    }

    public List<CommandRecord> getCommandsByCategory(String category) {
        List<CommandRecord> commands = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_COMMANDS, null, "category = ?", 
                new String[]{category}, null, null, "timestamp DESC");
        
        if (cursor.moveToFirst()) {
            do {
                CommandRecord record = new CommandRecord();
                record.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                record.command = cursor.getString(cursor.getColumnIndexOrThrow("command"));
                record.response = cursor.getString(cursor.getColumnIndexOrThrow("response"));
                record.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                record.success = cursor.getInt(cursor.getColumnIndexOrThrow("success")) == 1;
                record.category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                commands.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return commands;
    }

    public List<String> getMostUsedCommands(int limit) {
        List<String> commands = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        String query = "SELECT command, COUNT(*) as count FROM " + TABLE_COMMANDS + 
                       " GROUP BY command ORDER BY count DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit)});
        
        if (cursor.moveToFirst()) {
            do {
                commands.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return commands;
    }

    // ==================== FEATURE USAGE ====================

    public void recordFeatureUse(String featureName) {
        SQLiteDatabase db = getWritableDatabase();
        
        // Check if feature exists
        Cursor cursor = db.query(TABLE_FEATURES, null, "feature_name = ?", 
                new String[]{featureName}, null, null, null);
        
        if (cursor.moveToFirst()) {
            // Update existing
            ContentValues values = new ContentValues();
            values.put("use_count", cursor.getInt(cursor.getColumnIndexOrThrow("use_count")) + 1);
            values.put("last_used", System.currentTimeMillis());
            db.update(TABLE_FEATURES, values, "feature_name = ?", new String[]{featureName});
        } else {
            // Insert new
            ContentValues values = new ContentValues();
            values.put("feature_name", featureName);
            values.put("use_count", 1);
            values.put("last_used", System.currentTimeMillis());
            db.insert(TABLE_FEATURES, null, values);
        }
        cursor.close();
    }

    public List<FeatureRecord> getAllFeatures() {
        List<FeatureRecord> features = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_FEATURES, null, null, null, null, null, 
                "use_count DESC");
        
        if (cursor.moveToFirst()) {
            do {
                FeatureRecord record = new FeatureRecord();
                record.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                record.featureName = cursor.getString(cursor.getColumnIndexOrThrow("feature_name"));
                record.useCount = cursor.getInt(cursor.getColumnIndexOrThrow("use_count"));
                record.lastUsed = cursor.getLong(cursor.getColumnIndexOrThrow("last_used"));
                record.isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")) == 1;
                features.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return features;
    }

    public List<String> getTopFeatures(int limit) {
        List<String> topFeatures = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_FEATURES, new String[]{"feature_name"}, 
                null, null, null, null, "use_count DESC", String.valueOf(limit));
        
        if (cursor.moveToFirst()) {
            do {
                topFeatures.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return topFeatures;
    }

    public void setFavorite(String featureName, boolean favorite) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_favorite", favorite ? 1 : 0);
        db.update(TABLE_FEATURES, values, "feature_name = ?", new String[]{featureName});
    }

    // ==================== PATTERN LEARNING ====================

    private void learnPattern(String command, String category) {
        SQLiteDatabase db = getWritableDatabase();
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hourOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
        
        // Check if similar pattern exists
        Cursor cursor = db.query(TABLE_PATTERNS, null, 
                "pattern_type = ? AND pattern_data = ? AND time_of_day = ? AND day_of_week = ?",
                new String[]{"command", command, String.valueOf(hourOfDay), String.valueOf(dayOfWeek)},
                null, null, null);
        
        if (cursor.moveToFirst()) {
            // Update frequency
            ContentValues values = new ContentValues();
            values.put("frequency", cursor.getInt(cursor.getColumnIndexOrThrow("frequency")) + 1);
            db.update(TABLE_PATTERNS, values, "id = ?", 
                    new String[]{String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("id")))});
        } else {
            // Insert new pattern
            ContentValues values = new ContentValues();
            values.put("pattern_type", "command");
            values.put("pattern_data", command);
            values.put("time_of_day", hourOfDay);
            values.put("day_of_week", dayOfWeek);
            values.put("frequency", 1);
            db.insert(TABLE_PATTERNS, null, values);
        }
        cursor.close();
    }

    public List<String> getPredictedCommands() {
        List<String> predictions = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hourOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
        
        // Find patterns for current time and day
        Cursor cursor = db.query(TABLE_PATTERNS, new String[]{"pattern_data", "frequency"}, 
                "time_of_day BETWEEN ? AND ? AND day_of_week = ?",
                new String[]{String.valueOf(hourOfDay - 1), String.valueOf(hourOfDay + 1), String.valueOf(dayOfWeek)},
                null, null, "frequency DESC", "5");
        
        if (cursor.moveToFirst()) {
            do {
                predictions.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return predictions;
    }

    // ==================== VOICE SHORTCUTS ====================

    public boolean addShortcut(String triggerPhrase, String actionType, String actionData) {
        SQLiteDatabase db = getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("trigger_phrase", triggerPhrase.toLowerCase());
        values.put("action_type", actionType);
        values.put("action_data", actionData);
        values.put("created_at", System.currentTimeMillis());
        
        try {
            db.insertOrThrow(TABLE_SHORTCUTS, null, values);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public ShortcutRecord getShortcut(String phrase) {
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_SHORTCUTS, null, "trigger_phrase = ?", 
                new String[]{phrase.toLowerCase()}, null, null, null);
        
        ShortcutRecord shortcut = null;
        if (cursor.moveToFirst()) {
            shortcut = new ShortcutRecord();
            shortcut.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            shortcut.triggerPhrase = cursor.getString(cursor.getColumnIndexOrThrow("trigger_phrase"));
            shortcut.actionType = cursor.getString(cursor.getColumnIndexOrThrow("action_type"));
            shortcut.actionData = cursor.getString(cursor.getColumnIndexOrThrow("action_data"));
            shortcut.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        }
        cursor.close();
        
        return shortcut;
    }

    public List<ShortcutRecord> getAllShortcuts() {
        List<ShortcutRecord> shortcuts = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_SHORTCUTS, null, null, null, null, null, 
                "created_at DESC");
        
        if (cursor.moveToFirst()) {
            do {
                ShortcutRecord shortcut = new ShortcutRecord();
                shortcut.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                shortcut.triggerPhrase = cursor.getString(cursor.getColumnIndexOrThrow("trigger_phrase"));
                shortcut.actionType = cursor.getString(cursor.getColumnIndexOrThrow("action_type"));
                shortcut.actionData = cursor.getString(cursor.getColumnIndexOrThrow("action_data"));
                shortcut.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                shortcuts.add(shortcut);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return shortcuts;
    }

    public boolean deleteShortcut(String triggerPhrase) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_SHORTCUTS, "trigger_phrase = ?", 
                new String[]{triggerPhrase.toLowerCase()}) > 0;
    }

    // ==================== STATISTICS ====================

    public String getStatistics() {
        SQLiteDatabase db = getReadableDatabase();
        
        int totalCommands = 0;
        int totalFeatures = 0;
        int favoriteFeatures = 0;
        int shortcuts = 0;
        
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_COMMANDS, null);
        if (cursor.moveToFirst()) totalCommands = cursor.getInt(0);
        cursor.close();
        
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FEATURES, null);
        if (cursor.moveToFirst()) totalFeatures = cursor.getInt(0);
        cursor.close();
        
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FEATURES + " WHERE is_favorite = 1", null);
        if (cursor.moveToFirst()) favoriteFeatures = cursor.getInt(0);
        cursor.close();
        
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SHORTCUTS, null);
        if (cursor.moveToFirst()) shortcuts = cursor.getInt(0);
        cursor.close();
        
        return "📊 Memory Statistics:\n" +
               "• Total Commands: " + totalCommands + "\n" +
               "• Features Available: " + totalFeatures + "\n" +
               "• Favorite Features: " + favoriteFeatures + "\n" +
               "• Custom Shortcuts: " + shortcuts;
    }

    // ==================== EXPORT/IMPORT ====================

    public String exportMemory() {
        try {
            JSONObject export = new JSONObject();
            
            // Export commands
            JSONArray commands = new JSONArray();
            for (CommandRecord cmd : getCommandHistory(100)) {
                JSONObject cmdJson = new JSONObject();
                cmdJson.put("command", cmd.command);
                cmdJson.put("response", cmd.response);
                cmdJson.put("timestamp", cmd.timestamp);
                cmdJson.put("category", cmd.category);
                commands.put(cmdJson);
            }
            export.put("commands", commands);
            
            // Export shortcuts
            JSONArray shortcuts = new JSONArray();
            for (ShortcutRecord shortcut : getAllShortcuts()) {
                JSONObject scJson = new JSONObject();
                scJson.put("trigger", shortcut.triggerPhrase);
                scJson.put("action", shortcut.actionData);
                shortcuts.put(scJson);
            }
            export.put("shortcuts", shortcuts);
            
            return export.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== RECORD CLASSES ====================

    public static class CommandRecord {
        public int id;
        public String command;
        public String response;
        public long timestamp;
        public boolean success;
        public String category;
    }

    public static class FeatureRecord {
        public int id;
        public String featureName;
        public int useCount;
        public long lastUsed;
        public boolean isFavorite;
    }

    public static class ShortcutRecord {
        public int id;
        public String triggerPhrase;
        public String actionType;
        public String actionData;
        public long createdAt;
    }
}
