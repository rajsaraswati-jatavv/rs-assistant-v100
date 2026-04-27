package com.rsassistant.control;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * ConnectivityController - Network and Connectivity Control Manager
 * 
 * Supported Features:
 * - Mobile data toggle
 * - Hotspot toggle
 * - Airplane mode toggle
 * - VPN toggle
 * - WiFi direct
 */
public class ConnectivityController {

    private static final String TAG = "ConnectivityController";
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final WifiManager wifiManager;
    private final TelephonyManager telephonyManager;
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel wifiP2pChannel;
    
    private ConnectivityManager.NetworkCallback networkCallback;

    public interface ConnectivityCallback {
        void onSuccess(String message);
        void onError(String error);
        void onStatus(ConnectivityStatus status);
        void onPeersDiscovered(List<WifiP2pDevice> peers);
    }

    public static class ConnectivityStatus {
        public boolean mobileDataEnabled;
        public boolean wifiEnabled;
        public boolean hotspotEnabled;
        public boolean airplaneModeEnabled;
        public boolean vpnConnected;
        public boolean wifiDirectEnabled;
        public String networkType;
        public int signalStrength;
        public String ssid;
    }

    public ConnectivityController(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        
        // WiFi P2P
        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager != null) {
            this.wifiP2pChannel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        } else {
            this.wifiP2pChannel = null;
        }
    }

    // ==================== Mobile Data Controls ====================

    /**
     * Check if mobile data is enabled
     */
    @SuppressLint("MissingPermission")
    public boolean isMobileDataEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    return tm.isDataEnabled();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking mobile data: " + e.getMessage());
            }
        }
        
        // Fallback using reflection
        try {
            Method method = connectivityManager.getClass().getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(connectivityManager);
        } catch (Exception e) {
            Log.e(TAG, "Reflection error: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Toggle mobile data
     */
    public void toggleMobileData(ConnectivityCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, direct toggle is not allowed
            openMobileDataSettings(callback);
        } else {
            try {
                @SuppressLint("DiscouragedPrivateApi") Method method = connectivityManager.getClass()
                    .getDeclaredMethod("setMobileDataEnabled", boolean.class);
                method.setAccessible(true);
                method.invoke(connectivityManager, !isMobileDataEnabled());
                callback.onSuccess("Mobile data " + (isMobileDataEnabled() ? "band" : "on") + " ho gaya!");
            } catch (Exception e) {
                openMobileDataSettings(callback);
            }
        }
    }

    /**
     * Enable mobile data
     */
    public void enableMobileData(ConnectivityCallback callback) {
        if (isMobileDataEnabled()) {
            callback.onSuccess("Mobile data already on hai!");
            return;
        }
        toggleMobileData(callback);
    }

    /**
     * Disable mobile data
     */
    public void disableMobileData(ConnectivityCallback callback) {
        if (!isMobileDataEnabled()) {
            callback.onSuccess("Mobile data already band hai!");
            return;
        }
        toggleMobileData(callback);
    }

    /**
     * Open mobile data settings
     */
    public void openMobileDataSettings(ConnectivityCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_NETWORK_AND_INTERNET_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Network settings khul gayi. Mobile data toggle karein.");
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Settings khul gayi!");
            } catch (Exception e2) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        }
    }

    // ==================== Hotspot Controls ====================

    /**
     * Check if WiFi hotspot is enabled
     */
    public boolean isHotspotEnabled() {
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            Log.e(TAG, "Error checking hotspot: " + e.getMessage());
            return false;
        }
    }

    /**
     * Toggle WiFi hotspot
     */
    public void toggleHotspot(ConnectivityCallback callback) {
        try {
            boolean currentState = isHotspotEnabled();
            
            Method method = wifiManager.getClass().getDeclaredMethod("setWifiApEnabled", 
                WifiConfiguration.class, boolean.class);
            method.setAccessible(true);
            method.invoke(wifiManager, null, !currentState);
            
            callback.onSuccess("Hotspot " + (currentState ? "band" : "on") + " ho gaya!");
        } catch (Exception e) {
            openHotspotSettings(callback);
        }
    }

    /**
     * Enable WiFi hotspot
     */
    public void enableHotspot(ConnectivityCallback callback) {
        if (isHotspotEnabled()) {
            callback.onSuccess("Hotspot already on hai!");
            return;
        }
        toggleHotspot(callback);
    }

    /**
     * Disable WiFi hotspot
     */
    public void disableHotspot(ConnectivityCallback callback) {
        if (!isHotspotEnabled()) {
            callback.onSuccess("Hotspot already band hai!");
            return;
        }
        toggleHotspot(callback);
    }

    /**
     * Open hotspot settings
     */
    public void openHotspotSettings(ConnectivityCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_HOTSPOT_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Hotspot settings khul gayi!");
        } catch (Exception e) {
            // Fallback
            try {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Wireless settings khul gayi!");
            } catch (Exception e2) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        }
    }

    /**
     * Get hotspot configuration
     */
    public WifiConfiguration getHotspotConfiguration() {
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("getWifiApConfiguration");
            method.setAccessible(true);
            return (WifiConfiguration) method.invoke(wifiManager);
        } catch (Exception e) {
            Log.e(TAG, "Error getting hotspot config: " + e.getMessage());
            return null;
        }
    }

    /**
     * Set hotspot configuration
     */
    public void setHotspotConfiguration(String ssid, String password, ConnectivityCallback callback) {
        try {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = ssid;
            config.preSharedKey = password;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            
            Method method = wifiManager.getClass().getDeclaredMethod("setWifiApConfiguration", 
                WifiConfiguration.class);
            method.setAccessible(true);
            boolean result = (Boolean) method.invoke(wifiManager, config);
            
            if (result) {
                callback.onSuccess("Hotspot configuration set ho gayi!");
            } else {
                callback.onError("Configuration set nahi ho pa rahi");
            }
        } catch (Exception e) {
            openHotspotSettings(callback);
        }
    }

    // ==================== Airplane Mode Controls ====================

    /**
     * Check if airplane mode is enabled
     */
    public boolean isAirplaneModeEnabled() {
        return Settings.Global.getInt(context.getContentResolver(), 
            Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    /**
     * Toggle airplane mode
     */
    @SuppressLint("MissingPermission")
    public void toggleAirplaneMode(ConnectivityCallback callback) {
        try {
            boolean isEnabled = isAirplaneModeEnabled();
            
            // Toggle airplane mode
            Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
            
            // Broadcast the change
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", !isEnabled);
            context.sendBroadcast(intent);
            
            callback.onSuccess("Airplane mode " + (isEnabled ? "band" : "on") + " ho gaya!");
        } catch (Exception e) {
            openAirplaneModeSettings(callback);
        }
    }

    /**
     * Enable airplane mode
     */
    public void enableAirplaneMode(ConnectivityCallback callback) {
        if (isAirplaneModeEnabled()) {
            callback.onSuccess("Airplane mode already on hai!");
            return;
        }
        toggleAirplaneMode(callback);
    }

    /**
     * Disable airplane mode
     */
    public void disableAirplaneMode(ConnectivityCallback callback) {
        if (!isAirplaneModeEnabled()) {
            callback.onSuccess("Airplane mode already band hai!");
            return;
        }
        toggleAirplaneMode(callback);
    }

    /**
     * Open airplane mode settings
     */
    public void openAirplaneModeSettings(ConnectivityCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Airplane mode settings khul gayi!");
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Wireless settings khul gayi!");
            } catch (Exception e2) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        }
    }

    // ==================== VPN Controls ====================

    /**
     * Check if VPN is connected
     */
    @SuppressLint("MissingPermission")
    public boolean isVpnConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (caps != null) {
                    return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
                }
            }
        }
        
        // Fallback for older versions
        try {
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking VPN: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Open VPN settings
     */
    public void openVpnSettings(ConnectivityCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_VPN_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("VPN settings khul gayi!");
        } catch (Exception e) {
            callback.onError("VPN settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    /**
     * Toggle VPN (opens settings)
     */
    public void toggleVpn(ConnectivityCallback callback) {
        openVpnSettings(callback);
    }

    /**
     * Disconnect VPN
     */
    public void disconnectVpn(ConnectivityCallback callback) {
        // Apps cannot disconnect VPN directly for security reasons
        openVpnSettings(callback);
    }

    // ==================== WiFi Direct Controls ====================

    /**
     * Check if WiFi P2P is available
     */
    public boolean isWifiP2pAvailable() {
        return wifiP2pManager != null && wifiP2pChannel != null;
    }

    /**
     * Enable WiFi P2P discovery
     */
    public void startWifiP2pDiscovery(final ConnectivityCallback callback) {
        if (!isWifiP2pAvailable()) {
            callback.onError("WiFi Direct available nahi hai");
            return;
        }

        // First enable WiFi if not enabled
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        wifiP2pManager.discoverPeers(wifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onSuccess("WiFi Direct discovery shuru ho gayi!");
            }

            @Override
            public void onFailure(int reason) {
                String errorMsg;
                switch (reason) {
                    case WifiP2pManager.ERROR:
                        errorMsg = "Internal error";
                        break;
                    case WifiP2pManager.P2P_UNSUPPORTED:
                        errorMsg = "WiFi Direct unsupported";
                        break;
                    case WifiP2pManager.BUSY:
                        errorMsg = "WiFi Direct busy hai";
                        break;
                    default:
                        errorMsg = "Unknown error: " + reason;
                }
                callback.onError("Discovery failed: " + errorMsg);
            }
        });
    }

    /**
     * Stop WiFi P2P discovery
     */
    public void stopWifiP2pDiscovery(ConnectivityCallback callback) {
        if (!isWifiP2pAvailable()) {
            callback.onError("WiFi Direct available nahi hai");
            return;
        }

        wifiP2pManager.stopPeerDiscovery(wifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onSuccess("WiFi Direct discovery band ho gayi!");
            }

            @Override
            public void onFailure(int reason) {
                callback.onError("Discovery stop nahi ho pa rahi");
            }
        });
    }

    /**
     * Request WiFi P2P peers
     */
    public void requestWifiP2pPeers(final ConnectivityCallback callback) {
        if (!isWifiP2pAvailable()) {
            callback.onError("WiFi Direct available nahi hai");
            return;
        }

        wifiP2pManager.requestPeers(wifiP2pChannel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                List<WifiP2pDevice> deviceList = new ArrayList<>();
                deviceList.addAll(peers.getDeviceList());
                callback.onPeersDiscovered(deviceList);
            }
        });
    }

    /**
     * Connect to WiFi P2P device
     */
    public void connectToWifiP2pDevice(WifiP2pDevice device, ConnectivityCallback callback) {
        if (!isWifiP2pAvailable()) {
            callback.onError("WiFi Direct available nahi hai");
            return;
        }

        WifiP2pManager.ActionListener listener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onSuccess("Connection request bhej di gayi!");
            }

            @Override
            public void onFailure(int reason) {
                callback.onError("Connection failed: " + reason);
            }
        };

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        wifiP2pManager.connect(wifiP2pChannel, config, listener);
    }

    /**
     * Disconnect WiFi P2P
     */
    public void disconnectWifiP2p(ConnectivityCallback callback) {
        if (!isWifiP2pAvailable()) {
            callback.onError("WiFi Direct available nahi hai");
            return;
        }

        wifiP2pManager.removeGroup(wifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onSuccess("WiFi Direct disconnect ho gaya!");
            }

            @Override
            public void onFailure(int reason) {
                callback.onError("Disconnect failed: " + reason);
            }
        });
    }

    /**
     * Open WiFi Direct settings
     */
    public void openWifiP2pSettings(ConnectivityCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("WiFi settings khul gayi. WiFi Direct option dhundein.");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    // Inner class for WifiP2pConfig since we need it
    private static class WifiP2pConfig {
        String deviceAddress;
    }

    // ==================== WiFi Controls ====================

    /**
     * Check if WiFi is enabled
     */
    public boolean isWifiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    /**
     * Enable WiFi
     */
    public void enableWifi(ConnectivityCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("WiFi settings khul gayi. WiFi enable karein.");
            } catch (Exception e) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        } else {
            if (wifiManager.setWifiEnabled(true)) {
                callback.onSuccess("WiFi on ho gaya!");
            } else {
                callback.onError("WiFi on nahi ho pa raha");
            }
        }
    }

    /**
     * Disable WiFi
     */
    public void disableWifi(ConnectivityCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("WiFi settings khul gayi. WiFi disable karein.");
            } catch (Exception e) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        } else {
            if (wifiManager.setWifiEnabled(false)) {
                callback.onSuccess("WiFi band ho gaya!");
            } else {
                callback.onError("WiFi band nahi ho pa raha");
            }
        }
    }

    /**
     * Toggle WiFi
     */
    public void toggleWifi(ConnectivityCallback callback) {
        if (isWifiEnabled()) {
            disableWifi(callback);
        } else {
            enableWifi(callback);
        }
    }

    // ==================== Network Status ====================

    /**
     * Get current network type
     */
    @SuppressLint("MissingPermission")
    public String getNetworkType() {
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
                    if (caps != null) {
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            return "WiFi";
                        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            return "Mobile Data";
                        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                            return "Ethernet";
                        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                            return "VPN";
                        }
                    }
                }
            } else {
                android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    switch (activeNetwork.getType()) {
                        case ConnectivityManager.TYPE_WIFI:
                            return "WiFi";
                        case ConnectivityManager.TYPE_MOBILE:
                            return "Mobile Data";
                        case ConnectivityManager.TYPE_ETHERNET:
                            return "Ethernet";
                        case ConnectivityManager.TYPE_VPN:
                            return "VPN";
                    }
                }
            }
        }
        return "No Connection";
    }

    /**
     * Check if internet is available
     */
    public boolean isInternetAvailable() {
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
                    return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                }
            } else {
                android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        }
        return false;
    }

    /**
     * Get connectivity status
     */
    public ConnectivityStatus getConnectivityStatus() {
        ConnectivityStatus status = new ConnectivityStatus();
        
        status.wifiEnabled = isWifiEnabled();
        status.mobileDataEnabled = isMobileDataEnabled();
        status.hotspotEnabled = isHotspotEnabled();
        status.airplaneModeEnabled = isAirplaneModeEnabled();
        status.vpnConnected = isVpnConnected();
        status.networkType = getNetworkType();
        
        // Get WiFi signal strength
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                status.signalStrength = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
                String ssid = wifiInfo.getSSID();
                if (ssid != null && !ssid.contains("unknown")) {
                    status.ssid = ssid.replace("\"", "");
                }
            }
        }
        
        status.wifiDirectEnabled = isWifiP2pAvailable();
        
        return status;
    }

    /**
     * Get connectivity summary
     */
    public String getConnectivitySummary() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Network: ").append(getNetworkType()).append("\n");
        sb.append("WiFi: ").append(isWifiEnabled() ? "On" : "Off").append("\n");
        sb.append("Mobile Data: ").append(isMobileDataEnabled() ? "On" : "Off").append("\n");
        sb.append("Airplane Mode: ").append(isAirplaneModeEnabled() ? "On" : "Off").append("\n");
        sb.append("Hotspot: ").append(isHotspotEnabled() ? "On" : "Off").append("\n");
        sb.append("VPN: ").append(isVpnConnected() ? "Connected" : "Not Connected");
        
        return sb.toString();
    }

    // ==================== Network Monitoring ====================

    /**
     * Start network monitoring
     */
    public void startNetworkMonitoring(final ConnectivityCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
            
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    callback.onSuccess("Network available: " + getNetworkType());
                }
                
                @Override
                public void onLost(Network network) {
                    callback.onError("Network connection lost");
                }
                
                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                    ConnectivityStatus status = getConnectivityStatus();
                    callback.onStatus(status);
                }
            };
            
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    /**
     * Stop network monitoring
     */
    public void stopNetworkMonitoring() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    /**
     * Open network settings
     */
    public void openNetworkSettings(ConnectivityCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_NETWORK_AND_INTERNET_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Network settings khul gayi!");
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Settings khul gayi!");
            } catch (Exception e2) {
                callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
            }
        }
    }

    // ==================== Data Usage ====================

    /**
     * Open data usage settings
     */
    public void openDataUsageSettings(ConnectivityCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_DATA_USAGE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Data usage settings khul gayi!");
        } catch (Exception e) {
            openMobileDataSettings(callback);
        }
    }
}
