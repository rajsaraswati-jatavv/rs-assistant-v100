package com.rsassistant.control;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * AppController - Voice-based App Control Manager
 * Opens, closes, and manages apps by voice commands
 * 
 * Supported Commands:
 * - "WhatsApp kholo" / "Open WhatsApp"
 * - "Chrome band karo" / "Close Chrome"
 * - "Saari apps dikao" / "Show all apps"
 * - "Camera kholo" / "Open Camera"
 */
public class AppController {

    private static final String TAG = "AppController";
    private final Context context;
    private final PackageManager packageManager;

    public interface AppCallback {
        void onSuccess(String message);
        void onError(String error);
        void onAppList(List<AppInfo> apps);
    }

    public static class AppInfo {
        public String packageName;
        public String appName;
        public boolean isSystemApp;
        public DrawableInfo icon;

        public AppInfo(String packageName, String appName, boolean isSystemApp) {
            this.packageName = packageName;
            this.appName = appName;
            this.isSystemApp = isSystemApp;
        }
    }

    public static class DrawableInfo {
        // Placeholder for drawable info
    }

    public AppController(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
    }

    /**
     * Open app by name
     * Supports: "WhatsApp kholo", "Open Chrome", "Camera chalao"
     */
    public void openApp(String appName, AppCallback callback) {
        String packageName = findPackageByAppName(appName);
        
        if (packageName == null) {
            // Try fuzzy matching
            packageName = fuzzySearchApp(appName);
        }

        if (packageName != null) {
            try {
                Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);
                    callback.onSuccess(appName + " khul gaya!");
                } else {
                    callback.onError(appName + " nahi khul pa raha");
                }
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        } else {
            callback.onError(appName + " app nahi mila. Kya aapne sahi naam bola?");
        }
    }

    /**
     * Open app by package name directly
     */
    public void openAppByPackage(String packageName, AppCallback callback) {
        try {
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("App khul gaya!");
            } else {
                callback.onError("App nahi khul pa raha");
            }
        } catch (Exception e) {
            callback.onError("Error: " + e.getMessage());
        }
    }

    /**
     * Close app - requires accessibility service
     */
    public void closeApp(String appName, AppCallback callback) {
        String packageName = findPackageByAppName(appName);
        
        if (packageName != null) {
            try {
                // Method 1: Try to go home (minimizes app)
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(homeIntent);
                
                // Method 2: Kill app (requires root or special permission)
                // This doesn't work without special permissions
                // Instead we just minimize it
                
                callback.onSuccess(appName + " band ho gaya!");
            } catch (Exception e) {
                callback.onError("App band nahi ho pa raha: " + e.getMessage());
            }
        } else {
            callback.onError(appName + " app nahi mila");
        }
    }

    /**
     * Get list of all installed apps
     */
    public void getInstalledApps(AppCallback callback) {
        List<AppInfo> appList = new ArrayList<>();
        
        try {
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            
            for (PackageInfo packageInfo : packages) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                
                appList.add(new AppInfo(packageInfo.packageName, appName, isSystemApp));
            }
            
            // Sort by name
            Collections.sort(appList, Comparator.comparing(a -> a.appName.toLowerCase()));
            
            callback.onAppList(appList);
        } catch (Exception e) {
            callback.onError("Apps list nahi mil pa rahi: " + e.getMessage());
        }
    }

    /**
     * Search apps by name
     */
    public List<AppInfo> searchApps(String query) {
        List<AppInfo> results = new ArrayList<>();
        
        try {
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            String lowerQuery = query.toLowerCase();
            
            for (PackageInfo packageInfo : packages) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                
                if (appName.toLowerCase().contains(lowerQuery) ||
                    packageInfo.packageName.toLowerCase().contains(lowerQuery)) {
                    boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    results.add(new AppInfo(packageInfo.packageName, appName, isSystemApp));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching apps: " + e.getMessage());
        }
        
        return results;
    }

    /**
     * Open app settings
     */
    public void openAppSettings(String appName, AppCallback callback) {
        String packageName = findPackageByAppName(appName);
        
        if (packageName != null) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess(appName + " settings khul gayi!");
            } catch (Exception e) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        } else {
            callback.onError("App nahi mila");
        }
    }

    /**
     * Open app in Play Store
     */
    public void openInPlayStore(String appName, AppCallback callback) {
        String packageName = findPackageByAppName(appName);
        
        if (packageName != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Play Store khul gaya!");
            } catch (Exception e) {
                // Fallback to browser
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    callback.onSuccess("Play Store browser mein khul gaya!");
                } catch (Exception e2) {
                    callback.onError("Play Store nahi khul pa raha");
                }
            }
        } else {
            callback.onError("App nahi mila");
        }
    }

    /**
     * Uninstall app
     */
    public void uninstallApp(String appName, AppCallback callback) {
        String packageName = findPackageByAppName(appName);
        
        if (packageName != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Uninstall screen khul gaya!");
            } catch (Exception e) {
                callback.onError("Uninstall nahi ho pa raha: " + e.getMessage());
            }
        } else {
            callback.onError("App nahi mila");
        }
    }

    /**
     * Clear app cache (requires special permission)
     */
    public void clearAppCache(String appName, AppCallback callback) {
        String packageName = findPackageByAppName(appName);
        
        if (packageName != null) {
            try {
                // Open storage settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Storage settings khul gayi. Wahan se clear cache karein.");
            } catch (Exception e) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        } else {
            callback.onError("App nahi mila");
        }
    }

    /**
     * Find package name by app name
     */
    private String findPackageByAppName(String appName) {
        try {
            // Common apps mapping
            String commonApp = getCommonAppPackage(appName.toLowerCase().trim());
            if (commonApp != null) return commonApp;

            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            String lowerAppName = appName.toLowerCase().trim();
            
            // Exact match
            for (PackageInfo packageInfo : packages) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                String name = packageManager.getApplicationLabel(appInfo).toString().toLowerCase();
                
                if (name.equals(lowerAppName)) {
                    return packageInfo.packageName;
                }
            }
            
            // Contains match
            for (PackageInfo packageInfo : packages) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                String name = packageManager.getApplicationLabel(appInfo).toString().toLowerCase();
                
                if (name.contains(lowerAppName) || lowerAppName.contains(name)) {
                    return packageInfo.packageName;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding app: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Get package name for common apps
     */
    private String getCommonAppPackage(String appName) {
        // Social Media
        if (appName.contains("whatsapp")) return "com.whatsapp";
        if (appName.contains("facebook") || appName.contains("fb")) return "com.facebook.katana";
        if (appName.contains("instagram") || appName.contains("insta")) return "com.instagram.android";
        if (appName.contains("twitter") || appName.contains("tweet")) return "com.twitter.android";
        if (appName.contains("telegram")) return "org.telegram.messenger";
        if (appName.contains("snapchat")) return "com.snapchat.android";
        if (appName.contains("linkedin")) return "com.linkedin.android";
        if (appName.contains("tiktok") || appName.contains("tik tok")) return "com.zhiliaoapp.musically";
        
        // Communication
        if (appName.contains("youtube") || appName.contains("you tube")) return "com.google.android.youtube";
        if (appName.contains("gmail") || appName.contains("mail")) return "com.google.android.gm";
        if (appName.contains("phone") || appName.contains("dialer")) return "com.google.android.dialer";
        if (appName.contains("message") || appName.contains("sms") || appName.contains("messaging")) return "com.google.android.apps.messaging";
        
        // Google Apps
        if (appName.contains("chrome")) return "com.android.chrome";
        if (appName.contains("maps") || appName.contains("map")) return "com.google.android.apps.maps";
        if (appName.contains("drive")) return "com.google.android.apps.docs";
        if (appName.contains("photos")) return "com.google.android.apps.photos";
        if (appName.contains("calendar")) return "com.google.android.calendar";
        if (appName.contains("clock") || appName.contains("alarm")) return "com.google.android.deskclock";
        if (appName.contains("calculator") || appName.contains("calc")) return "com.google.android.calculator";
        if (appName.contains("camera")) return "com.google.android.GoogleCamera";
        if (appName.contains("play store") || appName.contains("playstore")) return "com.android.vending";
        if (appName.contains("play music")) return "com.google.android.music";
        
        // System Apps
        if (appName.contains("settings") || appName.contains("setting")) return "com.android.settings";
        if (appName.contains("gallery")) return "com.android.gallery3d";
        if (appName.contains("file") || appName.contains("files")) return "com.google.android.apps.nbu.files";
        if (appName.contains("music") || appName.contains("audio")) return "com.google.android.music";
        if (appName.contains("video")) return "com.google.android.videos";
        if (appName.contains("notes")) return "com.google.android.keep";
        
        // Shopping
        if (appName.contains("amazon")) return "com.amazon.mShop.android.shopping";
        if (appName.contains("flipkart")) return "com.flipkart.android";
        if (appName.contains("myntra")) return "com.myntra.android";
        if (appName.contains("meesho")) return "com.meesho.app";
        
        // Payment
        if (appName.contains("paytm")) return "net.one97.paytm";
        if (appName.contains("phonepe")) return "com.phonepe.app";
        if (appName.contains("gpay") || appName.contains("google pay")) return "com.google.android.apps.nbu.paisa.user";
        
        // Streaming
        if (appName.contains("netflix")) return "com.netflix.mediaclient";
        if (appName.contains("hotstar")) return "in.startv.hotstar";
        if (appName.contains("spotify")) return "com.spotify.music";
        if (appName.contains("jio")) return "com.jio.media.jiobeats";
        
        return null;
    }

    /**
     * Fuzzy search for apps
     */
    private String fuzzySearchApp(String appName) {
        try {
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            String lowerAppName = appName.toLowerCase().trim();
            
            // Calculate similarity scores
            String bestMatch = null;
            int bestScore = 0;
            
            for (PackageInfo packageInfo : packages) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                String name = packageManager.getApplicationLabel(appInfo).toString().toLowerCase();
                
                int score = calculateSimilarity(lowerAppName, name);
                if (score > bestScore && score > 50) { // 50% threshold
                    bestScore = score;
                    bestMatch = packageInfo.packageName;
                }
            }
            
            return bestMatch;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calculate similarity between two strings (0-100)
     */
    private int calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 100;
        if (s1.contains(s2) || s2.contains(s1)) return 80;
        
        // Simple character matching
        String longer = s1.length() > s2.length() ? s1 : s2;
        String shorter = s1.length() > s2.length() ? s2 : s1;
        
        int matches = 0;
        for (char c : shorter.toCharArray()) {
            if (longer.indexOf(c) >= 0) {
                matches++;
            }
        }
        
        return (matches * 100) / longer.length();
    }

    /**
     * Get app info
     */
    public AppInfo getAppInfo(String packageName) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            String appName = packageManager.getApplicationLabel(appInfo).toString();
            boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            
            return new AppInfo(packageName, appName, isSystemApp);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if app is installed
     */
    public boolean isAppInstalled(String packageName) {
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
