package com.rsassistant.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskManager - Voice task management system
 * 
 * Features:
 * - Voice task creation
 * - Shopping list
 * - Todo list
 * - Notes
 * - Calendar events
 * - Voice reminders
 * 
 * @author RS Assistant Team
 * @version 2.0
 */
public class TaskManager {

    private static final String TAG = "TaskManager";
    private static final String PREFS_NAME = "task_manager_prefs";
    
    private final Context context;
    private final SharedPreferences prefs;
    
    public TaskManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // ==================== TASK OPERATIONS ====================
    
    /**
     * Add a new task
     */
    public String addTask(String taskName) {
        List<Task> tasks = getTasks();
        
        Task task = new Task();
        task.id = System.currentTimeMillis();
        task.name = taskName;
        task.completed = false;
        task.createdAt = System.currentTimeMillis();
        
        tasks.add(task);
        saveTasks(tasks);
        
        return "✅ Task added: " + taskName + "\n\nTotal tasks: " + tasks.size();
    }
    
    /**
     * Complete a task
     */
    public String completeTask(String taskName) {
        List<Task> tasks = getTasks();
        
        for (Task task : tasks) {
            if (task.name.toLowerCase().contains(taskName.toLowerCase())) {
                task.completed = true;
                saveTasks(tasks);
                return "🎉 Task completed: " + task.name + "\n\nGreat job! 💪";
            }
        }
        
        return "🤔 Task nahi mila: " + taskName + "\n\nDekhein list mein hai ya nahi?";
    }
    
    /**
     * Remove a task
     */
    public String removeTask(String taskName) {
        List<Task> tasks = getTasks();
        
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).name.toLowerCase().contains(taskName.toLowerCase())) {
                String removed = tasks.remove(i).name;
                saveTasks(tasks);
                return "🗑️ Task removed: " + removed + "\n\nRemaining tasks: " + tasks.size();
            }
        }
        
        return "🤔 Task nahi mila: " + taskName;
    }
    
    /**
     * List all tasks
     */
    public String listTasks() {
        List<Task> tasks = getTasks();
        
        if (tasks.isEmpty()) {
            return "📝 Task list empty hai!\n\n'New task add karo' boliye task ke liye.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("📝 Your Tasks:\n\n");
        
        int count = 1;
        int completed = 0;
        
        for (Task task : tasks) {
            String status = task.completed ? "✅" : "⬜";
            sb.append(count).append(". ").append(status).append(" ").append(task.name).append("\n");
            if (task.completed) completed++;
            count++;
        }
        
        sb.append("\n📊 Progress: ").append(completed).append("/").append(tasks.size()).append(" completed");
        
        return sb.toString();
    }
    
    /**
     * Clear all tasks
     */
    public String clearAllTasks() {
        saveTasks(new ArrayList<>());
        return "🗑️ All tasks cleared!\n\nFresh start! 🌟";
    }
    
    /**
     * Clear completed tasks
     */
    public String clearCompleted() {
        List<Task> tasks = getTasks();
        List<Task> remaining = new ArrayList<>();
        
        for (Task task : tasks) {
            if (!task.completed) {
                remaining.add(task);
            }
        }
        
        int removed = tasks.size() - remaining.size();
        saveTasks(remaining);
        
        return "🗑️ Cleared " + removed + " completed tasks!\n\nRemaining: " + remaining.size();
    }
    
    // ==================== SHOPPING LIST ====================
    
    /**
     * Add item to shopping list
     */
    public String addShoppingItem(String item) {
        List<String> items = getShoppingList();
        items.add(item);
        saveShoppingList(items);
        
        return "🛒 Added to shopping list: " + item + "\n\nTotal items: " + items.size();
    }
    
    /**
     * Remove item from shopping list
     */
    public String removeShoppingItem(String item) {
        List<String> items = getShoppingList();
        
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).toLowerCase().contains(item.toLowerCase())) {
                String removed = items.remove(i);
                saveShoppingList(items);
                return "🗑️ Removed: " + removed + "\n\nRemaining: " + items.size() + " items";
            }
        }
        
        return "🤔 Item nahi mila: " + item;
    }
    
    /**
     * List shopping items
     */
    public String listShopping() {
        List<String> items = getShoppingList();
        
        if (items.isEmpty()) {
            return "🛒 Shopping list empty hai!\n\n'Shopping list mein dalo [item]' boliye.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 Shopping List:\n\n");
        
        for (int i = 0; i < items.size(); i++) {
            sb.append("□ ").append(items.get(i)).append("\n");
        }
        
        sb.append("\n📝 Total: ").append(items.size()).append(" items");
        
        return sb.toString();
    }
    
    /**
     * Clear shopping list
     */
    public String clearShoppingList() {
        saveShoppingList(new ArrayList<>());
        return "🗑️ Shopping list cleared!\n\nReady for new shopping! 🛒";
    }
    
    // ==================== NOTES ====================
    
    /**
     * Add a note
     */
    public String addNote(String title, String content) {
        List<Note> notes = getNotes();
        
        Note note = new Note();
        note.id = System.currentTimeMillis();
        note.title = title;
        note.content = content;
        note.createdAt = System.currentTimeMillis();
        
        notes.add(note);
        saveNotes(notes);
        
        return "📝 Note saved: " + title + "\n\nTotal notes: " + notes.size();
    }
    
    /**
     * Get a note
     */
    public String getNote(String title) {
        List<Note> notes = getNotes();
        
        for (Note note : notes) {
            if (note.title.toLowerCase().contains(title.toLowerCase())) {
                return "📝 " + note.title + ":\n\n" + note.content;
            }
        }
        
        return "🤔 Note nahi mila: " + title;
    }
    
    /**
     * Delete a note
     */
    public String deleteNote(String title) {
        List<Note> notes = getNotes();
        
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).title.toLowerCase().contains(title.toLowerCase())) {
                String removed = notes.remove(i).title;
                saveNotes(notes);
                return "🗑️ Note deleted: " + removed;
            }
        }
        
        return "🤔 Note nahi mila: " + title;
    }
    
    /**
     * List all notes
     */
    public String listNotes() {
        List<Note> notes = getNotes();
        
        if (notes.isEmpty()) {
            return "📝 Koi notes nahi hai!\n\n'Note save karo [title]' boliye.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("📝 Your Notes:\n\n");
        
        for (int i = 0; i < notes.size(); i++) {
            sb.append((i + 1)).append(". 📄 ").append(notes.get(i).title).append("\n");
        }
        
        return sb.toString();
    }
    
    // ==================== REMINDERS ====================
    
    /**
     * Set a reminder
     */
    public String setReminder(String message, int minutesFromNow) {
        List<Reminder> reminders = getReminders();
        
        Reminder reminder = new Reminder();
        reminder.id = System.currentTimeMillis();
        reminder.message = message;
        reminder.time = System.currentTimeMillis() + (minutesFromNow * 60 * 1000);
        reminder.active = true;
        
        reminders.add(reminder);
        saveReminders(reminders);
        
        return "⏰ Reminder set: " + message + "\n\n" + minutesFromNow + " minute baad notify karungi!";
    }
    
    /**
     * List reminders
     */
    public String listReminders() {
        List<Reminder> reminders = getReminders();
        
        if (reminders.isEmpty()) {
            return "⏰ Koi reminders nahi hai!\n\n'Mujhe yaad dilao [message]' boliye.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ Your Reminders:\n\n");
        
        for (Reminder reminder : reminders) {
            if (reminder.active) {
                sb.append("🔔 ").append(reminder.message).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Cancel reminder
     */
    public String cancelReminder(String message) {
        List<Reminder> reminders = getReminders();
        
        for (Reminder reminder : reminders) {
            if (reminder.message.toLowerCase().contains(message.toLowerCase())) {
                reminder.active = false;
                saveReminders(reminders);
                return "❌ Reminder cancelled: " + reminder.message;
            }
        }
        
        return "🤔 Reminder nahi mila: " + message;
    }
    
    // ==================== STORAGE HELPERS ====================
    
    private List<Task> getTasks() {
        String json = prefs.getString("tasks", "[]");
        return parseTasks(json);
    }
    
    private void saveTasks(List<Task> tasks) {
        JSONArray array = new JSONArray();
        for (Task task : tasks) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", task.id);
                obj.put("name", task.name);
                obj.put("completed", task.completed);
                obj.put("createdAt", task.createdAt);
                array.put(obj);
            } catch (JSONException e) {
                Log.e(TAG, "Error saving task: " + e.getMessage());
            }
        }
        prefs.edit().putString("tasks", array.toString()).apply();
    }
    
    private List<Task> parseTasks(String json) {
        List<Task> tasks = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Task task = new Task();
                task.id = obj.optLong("id", System.currentTimeMillis());
                task.name = obj.optString("name", "");
                task.completed = obj.optBoolean("completed", false);
                task.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
                tasks.add(task);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing tasks: " + e.getMessage());
        }
        return tasks;
    }
    
    private List<String> getShoppingList() {
        String json = prefs.getString("shopping_list", "[]");
        List<String> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                items.add(array.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing shopping list: " + e.getMessage());
        }
        return items;
    }
    
    private void saveShoppingList(List<String> items) {
        JSONArray array = new JSONArray(items);
        prefs.edit().putString("shopping_list", array.toString()).apply();
    }
    
    private List<Note> getNotes() {
        String json = prefs.getString("notes", "[]");
        return parseNotes(json);
    }
    
    private void saveNotes(List<Note> notes) {
        JSONArray array = new JSONArray();
        for (Note note : notes) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", note.id);
                obj.put("title", note.title);
                obj.put("content", note.content);
                obj.put("createdAt", note.createdAt);
                array.put(obj);
            } catch (JSONException e) {
                Log.e(TAG, "Error saving note: " + e.getMessage());
            }
        }
        prefs.edit().putString("notes", array.toString()).apply();
    }
    
    private List<Note> parseNotes(String json) {
        List<Note> notes = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Note note = new Note();
                note.id = obj.optLong("id", System.currentTimeMillis());
                note.title = obj.optString("title", "");
                note.content = obj.optString("content", "");
                note.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
                notes.add(note);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing notes: " + e.getMessage());
        }
        return notes;
    }
    
    private List<Reminder> getReminders() {
        String json = prefs.getString("reminders", "[]");
        return parseReminders(json);
    }
    
    private void saveReminders(List<Reminder> reminders) {
        JSONArray array = new JSONArray();
        for (Reminder reminder : reminders) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", reminder.id);
                obj.put("message", reminder.message);
                obj.put("time", reminder.time);
                obj.put("active", reminder.active);
                array.put(obj);
            } catch (JSONException e) {
                Log.e(TAG, "Error saving reminder: " + e.getMessage());
            }
        }
        prefs.edit().putString("reminders", array.toString()).apply();
    }
    
    private List<Reminder> parseReminders(String json) {
        List<Reminder> reminders = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Reminder reminder = new Reminder();
                reminder.id = obj.optLong("id", System.currentTimeMillis());
                reminder.message = obj.optString("message", "");
                reminder.time = obj.optLong("time", System.currentTimeMillis());
                reminder.active = obj.optBoolean("active", true);
                reminders.add(reminder);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing reminders: " + e.getMessage());
        }
        return reminders;
    }
    
    // ==================== DATA CLASSES ====================
    
    public static class Task {
        public long id;
        public String name;
        public boolean completed;
        public long createdAt;
    }
    
    public static class Note {
        public long id;
        public String title;
        public String content;
        public long createdAt;
    }
    
    public static class Reminder {
        public long id;
        public String message;
        public long time;
        public boolean active;
    }
}
