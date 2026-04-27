package com.rsassistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.rsassistant.gesture.ShakeDetector;
import com.rsassistant.memory.MemoryManager;
import com.rsassistant.util.SystemLevelManager;

import java.util.List;

/**
 * FeaturesScreenActivity - Shows All Features with Memory
 * Displays feature usage, favorites, and shortcuts
 */
public class FeaturesScreenActivity extends AppCompatActivity {

    private ListView featuresList;
    private ListView shortcutsList;
    private ListView historyList;
    private TextView statsText;
    
    private MemoryManager memoryManager;
    private SystemLevelManager systemManager;
    private ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_features);

        initViews();
        initManagers();
        loadFeatures();
        loadShortcuts();
        loadHistory();
        loadStats();
    }

    private void initViews() {
        featuresList = findViewById(R.id.featuresList);
        shortcutsList = findViewById(R.id.shortcutsList);
        historyList = findViewById(R.id.historyList);
        statsText = findViewById(R.id.statsText);
    }

    private void initManagers() {
        memoryManager = MemoryManager.getInstance(this);
        systemManager = SystemLevelManager.getInstance(this);
        shakeDetector = new ShakeDetector(this);
    }

    private void loadFeatures() {
        List<MemoryManager.FeatureRecord> features = memoryManager.getAllFeatures();
        
        String[] featureNames = new String[features.size()];
        String[] featureUsage = new String[features.size()];
        
        for (int i = 0; i < features.size(); i++) {
            MemoryManager.FeatureRecord f = features.get(i);
            featureNames[i] = (f.isFavorite ? "⭐ " : "   ") + f.featureName;
            featureUsage[i] = "Used: " + f.useCount + " times";
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_2, android.R.id.text1, featureNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                text1.setText(featureNames[position]);
                text2.setText(featureUsage[position]);
                return view;
            }
        };
        
        featuresList.setAdapter(adapter);
        
        featuresList.setOnItemClickListener((parent, view, position, id) -> {
            MemoryManager.FeatureRecord feature = features.get(position);
            // Toggle favorite
            memoryManager.setFavorite(feature.featureName, !feature.isFavorite);
            loadFeatures();
        });
    }

    private void loadShortcuts() {
        List<MemoryManager.ShortcutRecord> shortcuts = memoryManager.getAllShortcuts();
        
        String[] items = new String[shortcuts.size()];
        for (int i = 0; i < shortcuts.size(); i++) {
            MemoryManager.ShortcutRecord s = shortcuts.get(i);
            items[i] = "🎯 \"" + s.triggerPhrase + "\" → " + s.actionData;
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, items);
        shortcutsList.setAdapter(adapter);
    }

    private void loadHistory() {
        List<MemoryManager.CommandRecord> history = memoryManager.getCommandHistory(20);
        
        String[] items = new String[history.size()];
        for (int i = 0; i < history.size(); i++) {
            MemoryManager.CommandRecord cmd = history.get(i);
            items[i] = (cmd.success ? "✓ " : "✗ ") + cmd.command;
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, items);
        historyList.setAdapter(adapter);
    }

    private void loadStats() {
        String stats = memoryManager.getStatistics();
        stats += "\n\n" + systemManager.getSystemStatus();
        stats += "\n\n" + shakeDetector.getStatusInfo();
        statsText.setText(stats);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFeatures();
        loadShortcuts();
        loadHistory();
        loadStats();
    }
}
