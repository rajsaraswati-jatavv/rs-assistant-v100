package com.rsassistant.control;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AppLauncher - Voice-based App Launcher with Fuzzy Matching
 * 
 * Launches apps by voice commands with intelligent name matching.
 * Supports Hindi-English bilingual app names and fuzzy matching.
 * 
 * Supported Commands:
 * - "WhatsApp kholo" / "Open WhatsApp"
 * - "Instagram chalao" / "Start Instagram"
 * - "YouTube on karo" / "Launch YouTube"
 * - "Camera kholo" / "Open Camera"
 * 
 * Features:
 * - Comprehensive app database with package names
 * - Fuzzy matching for misspelled names
 * - Hindi and English app name support
 * - Multiple app suggestions for ambiguous names
 */
public class AppLauncher {

    private static final String TAG = "AppLauncher";
    private final Context context;
    private final PackageManager packageManager;
    
    // Static map of popular apps with multiple name variations
    private static final Map<String, AppInfo> POPULAR_APPS = new HashMap<>();
    
    static {
        // Social Media Apps
        POPULAR_APPS.put("whatsapp", new AppInfo("com.whatsapp", 
            "WhatsApp", Arrays.asList("whatsapp", "व्हाट्सएप", "वाट्सएप", "wa", "whats app")));
        POPULAR_APPS.put("instagram", new AppInfo("com.instagram.android", 
            "Instagram", Arrays.asList("instagram", "insta", "इंस्टाग्राम", "instgram", "ig")));
        POPULAR_APPS.put("facebook", new AppInfo("com.facebook.katana", 
            "Facebook", Arrays.asList("facebook", "fb", "फेसबुक", "face book")));
        POPULAR_APPS.put("twitter", new AppInfo("com.twitter.android", 
            "Twitter", Arrays.asList("twitter", "tweet", "ट्विटर", "x")));
        POPULAR_APPS.put("telegram", new AppInfo("org.telegram.messenger", 
            "Telegram", Arrays.asList("telegram", "tele", "टेलीग्राम", "tg")));
        POPULAR_APPS.put("snapchat", new AppInfo("com.snapchat.android", 
            "Snapchat", Arrays.asList("snapchat", "snap", "स्नैपचैट")));
        POPULAR_APPS.put("linkedin", new AppInfo("com.linkedin.android", 
            "LinkedIn", Arrays.asList("linkedin", "linked in", "लिंक्डइन")));
        POPULAR_APPS.put("tiktok", new AppInfo("com.zhiliaoapp.musically", 
            "TikTok", Arrays.asList("tiktok", "tik tok", "टिकटॉक", "tik-tok")));
        POPULAR_APPS.put("threads", new AppInfo("com.instagram.barcelona", 
            "Threads", Arrays.asList("threads", "थ्रेड्स", "thread")));
        
        // Communication Apps
        POPULAR_APPS.put("youtube", new AppInfo("com.google.android.youtube", 
            "YouTube", Arrays.asList("youtube", "you tube", "यूट्यूब", "yt")));
        POPULAR_APPS.put("gmail", new AppInfo("com.google.android.gm", 
            "Gmail", Arrays.asList("gmail", "google mail", "जीमेल", "mail", "email")));
        POPULAR_APPS.put("phone", new AppInfo("com.google.android.dialer", 
            "Phone", Arrays.asList("phone", "dialer", "dialpad", "फोन", "caller")));
        POPULAR_APPS.put("messages", new AppInfo("com.google.android.apps.messaging", 
            "Messages", Arrays.asList("messages", "sms", "message", "मैसेज", "text")));
        
        // Google Apps
        POPULAR_APPS.put("chrome", new AppInfo("com.android.chrome", 
            "Chrome", Arrays.asList("chrome", "google chrome", "क्रोम", "browser")));
        POPULAR_APPS.put("maps", new AppInfo("com.google.android.apps.maps", 
            "Google Maps", Arrays.asList("maps", "google maps", "मैप्स", "map", "navigation")));
        POPULAR_APPS.put("drive", new AppInfo("com.google.android.apps.docs", 
            "Google Drive", Arrays.asList("drive", "google drive", "ड्राइव")));
        POPULAR_APPS.put("photos", new AppInfo("com.google.android.apps.photos", 
            "Google Photos", Arrays.asList("photos", "gallery", "फोटो", "pictures", "images")));
        POPULAR_APPS.put("calendar", new AppInfo("com.google.android.calendar", 
            "Google Calendar", Arrays.asList("calendar", "कैलेंडर", "kalendar")));
        POPULAR_APPS.put("clock", new AppInfo("com.google.android.deskclock", 
            "Clock", Arrays.asList("clock", "alarm", "घड़ी", "timer", "stopwatch")));
        POPULAR_APPS.put("calculator", new AppInfo("com.google.android.calculator", 
            "Calculator", Arrays.asList("calculator", "calc", "कैलकुलेटर")));
        POPULAR_APPS.put("camera", new AppInfo("com.google.android.GoogleCamera", 
            "Camera", Arrays.asList("camera", "कैमरा", "photo", "selfie", "picture")));
        POPULAR_APPS.put("playstore", new AppInfo("com.android.vending", 
            "Play Store", Arrays.asList("playstore", "play store", "प्ले स्टोर", "store")));
        POPULAR_APPS.put("translate", new AppInfo("com.google.android.apps.translate", 
            "Google Translate", Arrays.asList("translate", "translation", "अनुवाद")));
        POPULAR_APPS.put("assistant", new AppInfo("com.google.android.googlequicksearchbox", 
            "Google Assistant", Arrays.asList("assistant", "google assistant", "गूगल असिस्टेंट")));
        POPULAR_APPS.put("contacts", new AppInfo("com.google.android.contacts", 
            "Contacts", Arrays.asList("contacts", "people", "संपर्क", "contact")));
        POPULAR_APPS.put("keep", new AppInfo("com.google.android.keep", 
            "Google Keep", Arrays.asList("keep", "notes", "google keep", "नोट्स")));
        POPULAR_APPS.put("meet", new AppInfo("com.google.android.apps.meetings", 
            "Google Meet", Arrays.asList("meet", "google meet", "meeting")));
        POPULAR_APPS.put("duo", new AppInfo("com.google.android.apps.tachyon", 
            "Google Duo", Arrays.asList("duo", "google duo", "video call")));
        POPULAR_APPS.put("files", new AppInfo("com.google.android.apps.nbu.files", 
            "Google Files", Arrays.asList("files", "file manager", "फाइल्स", "my files")));
        POPULAR_APPS.put("news", new AppInfo("com.google.android.apps.magazines", 
            "Google News", Arrays.asList("news", "google news", "समाचार", "akhbar")));
        
        // Entertainment Apps
        POPULAR_APPS.put("spotify", new AppInfo("com.spotify.music", 
            "Spotify", Arrays.asList("spotify", "स्पॉटिफाई", "music app")));
        POPULAR_APPS.put("netflix", new AppInfo("com.netflix.mediaclient", 
            "Netflix", Arrays.asList("netflix", "नेटफ्लिक्स")));
        POPULAR_APPS.put("primevideo", new AppInfo("com.amazon.avod.thirdpartyclient", 
            "Amazon Prime Video", Arrays.asList("prime video", "amazon prime", "प्राइम वीडियो", "prime")));
        POPULAR_APPS.put("hotstar", new AppInfo("in.startv.hotstar", 
            "Disney+ Hotstar", Arrays.asList("hotstar", "disney", "हॉटस्टार", "disney hotstar")));
        POPULAR_APPS.put("jiohotstar", new AppInfo("com.jio.media.jiobeats", 
            "JioHotstar", Arrays.asList("jio cinema", "jiocinema", "जियो सिनेमा")));
        POPULAR_APPS.put("gaana", new AppInfo("com.gaana", 
            "Gaana", Arrays.asList("gaana", "गाना", "gaana app")));
        POPULAR_APPS.put("saavn", new AppInfo("com.saavn.android", 
            "JioSaavn", Arrays.asList("saavn", "jiosaavn", "सावन")));
        POPULAR_APPS.put("wynk", new AppInfo("com.bsbportal.music", 
            "Wynk Music", Arrays.asList("wynk", "wynk music", "विंक")));
        POPULAR_APPS.put("amazonmusic", new AppInfo("com.amazon.mp3", 
            "Amazon Music", Arrays.asList("amazon music", "अमेज़न म्यूज़िक")));
        POPULAR_APPS.put("youtubemusic", new AppInfo("com.google.android.apps.youtube.music", 
            "YouTube Music", Arrays.asList("youtube music", "yt music", "यूट्यूब म्यूज़िक")));
        
        // Shopping Apps
        POPULAR_APPS.put("amazon", new AppInfo("com.amazon.mShop.android.shopping", 
            "Amazon", Arrays.asList("amazon", "अमेज़न", "amazon shopping")));
        POPULAR_APPS.put("flipkart", new AppInfo("com.flipkart.android", 
            "Flipkart", Arrays.asList("flipkart", "फ्लिपकार्ट")));
        POPULAR_APPS.put("myntra", new AppInfo("com.myntra.android", 
            "Myntra", Arrays.asList("myntra", "माइंत्रा")));
        POPULAR_APPS.put("meesho", new AppInfo("com.meesho.app", 
            "Meesho", Arrays.asList("meesho", "मीशो")));
        POPULAR_APPS.put("ajio", new AppInfo("com.ril.ajio", 
            "AJIO", Arrays.asList("ajio", "अजीओ")));
        POPULAR_APPS.put("snapdeal", new AppInfo("com.snapdeal.main", 
            "Snapdeal", Arrays.asList("snapdeal", "स्नैपडील")));
        
        // Payment Apps
        POPULAR_APPS.put("paytm", new AppInfo("net.one97.paytm", 
            "Paytm", Arrays.asList("paytm", "पेटीएम")));
        POPULAR_APPS.put("phonepe", new AppInfo("com.phonepe.app", 
            "PhonePe", Arrays.asList("phonepe", "phone pe", "फोनपे")));
        POPULAR_APPS.put("gpay", new AppInfo("com.google.android.apps.nbu.paisa.user", 
            "Google Pay", Arrays.asList("gpay", "google pay", "गूगल पे", "tez")));
        POPULAR_APPS.put("bhim", new AppInfo("in.org.npci.upiapp", 
            "BHIM", Arrays.asList("bhim", "भीम", "upi")));
        POPULAR_APPS.put("amazonpay", new AppInfo("in.amazon.mShop.android.shopping", 
            "Amazon Pay", Arrays.asList("amazon pay", "अमेज़न पे")));
        
        // Food Delivery Apps
        POPULAR_APPS.put("zomato", new AppInfo("com.application.zomato", 
            "Zomato", Arrays.asList("zomato", "जोमाटो")));
        POPULAR_APPS.put("swiggy", new AppInfo("in.swiggy.android", 
            "Swiggy", Arrays.asList("swiggy", "स्विगी")));
        POPULAR_APPS.put("dominos", new AppInfo("dominos.uk.co", 
            "Domino's", Arrays.asList("dominos", "domino's", "डोमिनोज")));
        
        // Travel Apps
        POPULAR_APPS.put("uber", new AppInfo("com.ubercab", 
            "Uber", Arrays.asList("uber", "ऊबर")));
        POPULAR_APPS.put("ola", new AppInfo("com.olacabs.customer", 
            "Ola", Arrays.asList("ola", "ओला", "ola cabs")));
        POPULAR_APPS.put("rapido", new AppInfo("com.rapido.passenger", 
            "Rapido", Arrays.asList("rapido", "रैपिडो")));
        POPULAR_APPS.put("makemytrip", new AppInfo("com.makemytrip", 
            "MakeMyTrip", Arrays.asList("makemytrip", "make my trip", "मेकमायट्रिप")));
        POPULAR_APPS.put("goibibo", new AppInfo("com.goibibo", 
            "Goibibo", Arrays.asList("goibibo", "गोइबिबो")));
        POPULAR_APPS.put("redbus", new AppInfo("in.redbus.android", 
            "redBus", Arrays.asList("redbus", "red bus", "रेडबस")));
        POPULAR_APPS.put("irctc", new AppInfo("cris.org.in.prs.ima", 
            "IRCTC", Arrays.asList("irctc", "railway", "train", "आईआरसीटीसी")));
        
        // Gaming Apps
        POPULAR_APPS.put("freefire", new AppInfo("com.dts.freefireth", 
            "Free Fire", Arrays.asList("freefire", "free fire", "फ्री फायर")));
        POPULAR_APPS.put("pubg", new AppInfo("com.pubg.imobile", 
            "BGMI", Arrays.asList("pubg", "bgmi", "pubg mobile")));
        POPULAR_APPS.put("codmobile", new AppInfo("com.activision.callofduty.shooter"), 
            "COD Mobile", Arrays.asList("cod", "call of duty", "cod mobile")));
        POPULAR_APPS.put("candycrush", new AppInfo("com.king.candycrushsaga", 
            "Candy Crush", Arrays.asList("candy crush", "candycrush", "कैंडी क्रश")));
        POPULAR_APPS.put("subway", new AppInfo("com.kiloo.subwaysurf", 
            "Subway Surfers", Arrays.asList("subway surfers", "subway", "सबवे")));
        POPULAR_APPS.put("ludo", new AppInfo("com.ludo.king", 
            "Ludo King", Arrays.asList("ludo", "ludo king", "लूडो")));
        
        // System Apps
        POPULAR_APPS.put("settings", new AppInfo("com.android.settings", 
            "Settings", Arrays.asList("settings", "setting", "सेटिंग्स", "config")));
        POPULAR_APPS.put("gallery", new AppInfo("com.android.gallery3d", 
            "Gallery", Arrays.asList("gallery", "गैलरी", "images")));
        POPULAR_APPS.put("music", new AppInfo("com.google.android.music", 
            "Music", Arrays.asList("music", "म्यूज़िक", "audio", "songs")));
        POPULAR_APPS.put("video", new AppInfo("com.google.android.videos", 
            "Videos", Arrays.asList("video", "videos", "वीडियो")));
        POPULAR_APPS.put("notes", new AppInfo("com.google.android.keep", 
            "Notes", Arrays.asList("notes", "नोट्स", "note")));
        POPULAR_APPS.put("recorder", new AppInfo("com.google.android.apps.recorder", 
            "Recorder", Arrays.asList("recorder", "voice recorder", "रिकॉर्डर")));
        POPULAR_APPS.put("filemanager", new AppInfo("com.google.android.apps.nbu.files", 
            "Files", Arrays.asList("files", "file manager", "फाइल मैनेजर")));
        POPULAR_APPS.put("downloads", new AppInfo("com.android.providers.downloads.ui", 
            "Downloads", Arrays.asList("downloads", "डाउनलोड")));
        
        // Health & Fitness
        POPULAR_APPS.put("fitbit", new AppInfo("com.fitbit.FitbitMobile", 
            "Fitbit", Arrays.asList("fitbit", "फिटबिट")));
        POPULAR_APPS.put("strava", new AppInfo("com.strava", 
            "Strava", Arrays.asList("strava", "स्ट्रावा")));
        POPULAR_APPS.put("googlefit", new AppInfo("com.google.android.apps.fitness", 
            "Google Fit", Arrays.asList("google fit", "fit", "गूगल फिट")));
        POPULAR_APPS.put("healthifyme", new AppInfo("com.healthifyme.basic", 
            "HealthifyMe", Arrays.asList("healthifyme", "healthify", "हेल्थीफाईमी")));
        
        // Education Apps
        POPULAR_APPS.put("byju", new AppInfo("com.byjus.thelearningapp", 
            "BYJU'S", Arrays.asList("byju", "byjus", "बायजू")));
        POPULAR_APPS.put("unacademy", new AppInfo("com.unacademyapp", 
            "Unacademy", Arrays.asList("unacademy", "यूनकैडमी")));
        POPULAR_APPS.put("vedantu", new AppInfo("com.vedantu.app", 
            "Vedantu", Arrays.asList("vedantu", "वेदांतू")));
        POPULAR_APPS.put("coursera", new AppInfo("org.coursera.android", 
            "Coursera", Arrays.asList("coursera", "कौरसेरा")));
        POPULAR_APPS.put("udemy", new AppInfo("com.udemy.android", 
            "Udemy", Arrays.asList("udemy", "यूडेमी")));
        POPULAR_APPS.put("khanacademy", new AppInfo("org.khanacademy.android", 
            "Khan Academy", Arrays.asList("khan academy", "खान अकादमी")));
        POPULAR_APPS.put("duolingo", new AppInfo("com.duolingo", 
            "Duolingo", Arrays.asList("duolingo", "डुओलिंगो")));
        
        // Productivity Apps
        POPULAR_APPS.put("notion", new AppInfo("notion.id", 
            "Notion", Arrays.asList("notion", "नोशन")));
        POPULAR_APPS.put("evernote", new AppInfo("com.evernote", 
            "Evernote", Arrays.asList("evernote", "एवरनोट")));
        POPULAR_APPS.put("todoist", new AppInfo("com.todoist", 
            "Todoist", Arrays.asList("todoist", "टोडोइस्ट")));
        POPULAR_APPS.put("trello", new AppInfo("com.trello", 
            "Trello", Arrays.asList("trello", "ट्रेलो")));
        POPULAR_APPS.put("slack", new AppInfo("com.Slack", 
            "Slack", Arrays.asList("slack", "स्लैक")));
        POPULAR_APPS.put("teams", new AppInfo("com.microsoft.teams", 
            "Microsoft Teams", Arrays.asList("teams", "microsoft teams", "टीम्स")));
        POPULAR_APPS.put("zoom", new AppInfo("us.zoom.videomeetings", 
            "Zoom", Arrays.asList("zoom", "ज़ूम")));
        POPULAR_APPS.put("skype", new AppInfo("com.skype.raider", 
            "Skype", Arrays.asList("skype", "स्काइप")));
        
        // News Apps
        POPULAR_APPS.put("inshorts", new AppInfo("com.nis.app", 
            "Inshorts", Arrays.asList("inshorts", "इनशॉर्ट्स")));
        POPULAR_APPS.put("toi", new AppInfo("com.toi.reader.activities", 
            "Times of India", Arrays.asList("times of india", "toi", "टाइम्स ऑफ इंडिया")));
        POPULAR_APPS.put("bhasha", new AppInfo("in.jamunabhasha", 
            "Bhasha", Arrays.asList("bhasha", "भाषा")));
        POPULAR_APPS.put("dainik", new AppInfo("com.dainik.bhaskar", 
            "Dainik Bhaskar", Arrays.asList("dainik bhaskar", "दैनिक भास्कर")));
        
        // Utility Apps
        POPULAR_APPS.put("camscanner", new AppInfo("com.intsig.camscanner", 
            "CamScanner", Arrays.asList("camscanner", "कैमस्कैनर", "scanner")));
        POPULAR_APPS.put("shareit", new AppInfo("com.lenovo.anyshare.gps", 
            "SHAREit", Arrays.asList("shareit", "share it", "शेयरइट")));
        POPULAR_APPS.put("xender", new AppInfo("cn.xender", 
            "Xender", Arrays.asList("xender", "ज़ेन्डर")));
        POPULAR_APPS.put("mxplayer", new AppInfo("com.mxtech.videoplayer.ad", 
            "MX Player", Arrays.asList("mx player", "mxplayer", "एमएक्स प्लेयर")));
        POPULAR_APPS.put("vlc", new AppInfo("org.videolan.vlc", 
            "VLC", Arrays.asList("vlc", "vlc player", "वीएलसी")));
        POPULAR_APPS.put("truecaller", new AppInfo("com.truecaller", 
            "Truecaller", Arrays.asList("truecaller", "ट्रूकॉलर")));
        POPULAR_APPS.put("gdrive", new AppInfo("com.google.android.apps.docs", 
            "Google Drive", Arrays.asList("drive", "gdrive", "google drive", "ड्राइव")));
        POPULAR_APPS.put("dropbox", new AppInfo("com.dropbox.android", 
            "Dropbox", Arrays.asList("dropbox", "ड्रॉपबॉक्स")));
    }

    public interface LaunchCallback {
        void onSuccess(String message);
        void onError(String error);
        void onMultipleMatches(List<String> matches);
    }

    /**
     * AppInfo class to store app details
     */
    public static class AppInfo {
        public final String packageName;
        public final String displayName;
        public final List<String> aliases;
        
        public AppInfo(String packageName, String displayName, List<String> aliases) {
            this.packageName = packageName;
            this.displayName = displayName;
            this.aliases = aliases != null ? aliases : new ArrayList<>();
        }
        
        public AppInfo(String packageName, String displayName) {
            this(packageName, displayName, null);
        }
    }

    public AppLauncher(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
    }

    /**
     * Launch app by voice command
     * @param voiceInput The app name spoken by user
     * @param callback Callback for result
     */
    public void launchApp(String voiceInput, LaunchCallback callback) {
        if (voiceInput == null || voiceInput.trim().isEmpty()) {
            callback.onError("App naam bolo");
            return;
        }
        
        String appName = voiceInput.toLowerCase().trim();
        
        // Remove common suffixes
        appName = removeCommandSuffixes(appName);
        
        // Step 1: Check popular apps map for exact match
        AppInfo appInfo = POPULAR_APPS.get(appName);
        if (appInfo != null) {
            launchByPackageName(appInfo.packageName, appInfo.displayName, callback);
            return;
        }
        
        // Step 2: Check aliases in popular apps
        for (AppInfo info : POPULAR_APPS.values()) {
            if (info.aliases.contains(appName)) {
                launchByPackageName(info.packageName, info.displayName, callback);
                return;
            }
        }
        
        // Step 3: Fuzzy search in popular apps
        List<AppInfo> fuzzyMatches = fuzzySearchPopularApps(appName);
        if (!fuzzyMatches.isEmpty()) {
            if (fuzzyMatches.size() == 1) {
                AppInfo match = fuzzyMatches.get(0);
                launchByPackageName(match.packageName, match.displayName, callback);
            } else {
                // Multiple matches - ask user
                List<String> matchNames = new ArrayList<>();
                for (AppInfo match : fuzzyMatches.subList(0, Math.min(3, fuzzyMatches.size()))) {
                    matchNames.add(match.displayName);
                }
                callback.onMultipleMatches(matchNames);
            }
            return;
        }
        
        // Step 4: Search installed apps
        searchAndLaunchInstalledApp(appName, callback);
    }

    /**
     * Remove common command suffixes from app name
     */
    private String removeCommandSuffixes(String input) {
        String[] suffixes = {
            " kholo", " chalao", " on karo", " start karo", " run karo",
            " open", " launch", " start", " run", " karo", " jalao",
            " ko kholo", " app kholo"
        };
        
        for (String suffix : suffixes) {
            if (input.endsWith(suffix)) {
                return input.substring(0, input.length() - suffix.length()).trim();
            }
        }
        
        // Remove prefix commands
        String[] prefixes = {
            "open ", "launch ", "start ", "run ", "kholo ", "chalao "
        };
        
        for (String prefix : prefixes) {
            if (input.startsWith(prefix)) {
                return input.substring(prefix.length()).trim();
            }
        }
        
        return input;
    }

    /**
     * Fuzzy search in popular apps
     */
    private List<AppInfo> fuzzySearchPopularApps(String query) {
        List<ScoredApp> scoredApps = new ArrayList<>();
        
        for (AppInfo info : POPULAR_APPS.values()) {
            int score = calculateSimilarity(query, info.displayName);
            
            // Check aliases too
            for (String alias : info.aliases) {
                int aliasScore = calculateSimilarity(query, alias);
                score = Math.max(score, aliasScore);
            }
            
            if (score >= 60) { // 60% threshold
                scoredApps.add(new ScoredApp(info, score));
            }
        }
        
        // Sort by score descending
        scoredApps.sort((a, b) -> Integer.compare(b.score, a.score));
        
        List<AppInfo> result = new ArrayList<>();
        for (ScoredApp sa : scoredApps) {
            result.add(sa.appInfo);
        }
        
        return result;
    }

    /**
     * Search and launch installed app
     */
    private void searchAndLaunchInstalledApp(String query, LaunchCallback callback) {
        try {
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            List<ScoredApp> scoredApps = new ArrayList<>();
            
            for (PackageInfo pkg : packages) {
                ApplicationInfo appInfo = pkg.applicationInfo;
                String appName = packageManager.getApplicationLabel(appInfo).toString().toLowerCase();
                
                int score = calculateSimilarity(query, appName);
                
                // Also check package name
                int pkgScore = calculateSimilarity(query, pkg.packageName.toLowerCase());
                score = Math.max(score, pkgScore);
                
                if (score >= 60) {
                    scoredApps.add(new ScoredApp(
                        new AppInfo(pkg.packageName, packageManager.getApplicationLabel(appInfo).toString()),
                        score
                    ));
                }
            }
            
            if (!scoredApps.isEmpty()) {
                scoredApps.sort((a, b) -> Integer.compare(b.score, a.score));
                AppInfo bestMatch = scoredApps.get(0).appInfo;
                launchByPackageName(bestMatch.packageName, bestMatch.displayName, callback);
            } else {
                callback.onError("'" + query + "' app nahi mila. Kya aapne sahi naam bola?");
            }
        } catch (Exception e) {
            callback.onError("App search mein error: " + e.getMessage());
        }
    }

    /**
     * Launch app by package name
     */
    private void launchByPackageName(String packageName, String displayName, LaunchCallback callback) {
        try {
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
                callback.onSuccess(displayName + " khul gaya!");
            } else {
                // App might be disabled or not launchable
                callback.onError(displayName + " khul nahi pa raha. App enabled hai?");
            }
        } catch (Exception e) {
            // App not installed - offer to open Play Store
            openPlayStoreForApp(packageName, displayName, callback);
        }
    }

    /**
     * Open Play Store for app
     */
    private void openPlayStoreForApp(String packageName, String displayName, LaunchCallback callback) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess(displayName + " installed nahi hai. Play Store khul gaya!");
        } catch (Exception e) {
            // Fallback to browser
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess(displayName + " ke liye Play Store browser mein khul gaya!");
            } catch (Exception e2) {
                callback.onError(displayName + " app nahi mila");
            }
        }
    }

    /**
     * Calculate similarity between two strings (0-100)
     */
    private int calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        if (s1.equals(s2)) return 100;
        
        String str1 = s1.toLowerCase().trim();
        String str2 = s2.toLowerCase().trim();
        
        if (str1.equals(str2)) return 100;
        if (str1.contains(str2)) return 90;
        if (str2.contains(str1)) return 85;
        
        // Levenshtein-based similarity
        int distance = levenshteinDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        
        if (maxLength == 0) return 100;
        
        return ((maxLength - distance) * 100) / maxLength;
    }

    /**
     * Calculate Levenshtein distance
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
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

    /**
     * Get list of popular apps
     */
    public List<AppInfo> getPopularAppsList() {
        return new ArrayList<>(POPULAR_APPS.values());
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

    /**
     * Get app info by package name
     */
    @Nullable
    public AppInfo getInstalledAppInfo(String packageName) {
        try {
            PackageInfo pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            String displayName = packageManager.getApplicationLabel(pkg.applicationInfo).toString();
            return new AppInfo(packageName, displayName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper class for scoring apps
     */
    private static class ScoredApp {
        final AppInfo appInfo;
        final int score;
        
        ScoredApp(AppInfo appInfo, int score) {
            this.appInfo = appInfo;
            this.score = score;
        }
    }
}
