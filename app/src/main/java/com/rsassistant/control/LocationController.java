package com.rsassistant.control;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * LocationController - GPS and Location Control Manager
 * 
 * Supported Features:
 * - GPS toggle
 * - Location sharing
 * - Current location announcement
 * - Navigation to places
 */
public class LocationController implements LocationListener {

    private static final String TAG = "LocationController";
    private final Context context;
    private final LocationManager locationManager;
    
    private Location currentLocation;
    private LocationCallback currentCallback;
    private boolean isListeningForLocation = false;

    public interface LocationCallback {
        void onSuccess(String message);
        void onError(String error);
        void onLocationReceived(LocationInfo locationInfo);
    }

    public static class LocationInfo {
        public double latitude;
        public double longitude;
        public double altitude;
        public float accuracy;
        public float speed;
        public float bearing;
        public String address;
        public String city;
        public String country;
        public String postalCode;
        public long timestamp;
    }

    public LocationController(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    // ==================== GPS Toggle Controls ====================

    /**
     * Check if location permission is granted
     */
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, 
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, 
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if GPS is enabled
     */
    public boolean isGpsEnabled() {
        if (locationManager == null) {
            return false;
        }
        
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "Error checking GPS status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if network location is enabled
     */
    public boolean isNetworkLocationEnabled() {
        if (locationManager == null) {
            return false;
        }
        
        try {
            return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "Error checking network location status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if any location provider is enabled
     */
    public boolean isLocationEnabled() {
        return isGpsEnabled() || isNetworkLocationEnabled();
    }

    /**
     * Enable GPS - Opens location settings
     */
    public void enableGPS(LocationCallback callback) {
        if (isGpsEnabled()) {
            callback.onSuccess("GPS already on hai!");
            return;
        }

        try {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Location settings khul gayi. GPS enable karein.");
        } catch (Exception e) {
            callback.onError("Location settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    /**
     * Open location settings
     */
    public void openLocationSettings(LocationCallback callback) {
        try {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Location settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    // ==================== Current Location ====================

    /**
     * Get current location
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onError("Location permission chahiye");
            return;
        }

        if (!isLocationEnabled()) {
            callback.onError("Location service band hai. Please enable karein.");
            return;
        }

        this.currentCallback = callback;

        try {
            // Try GPS first (more accurate)
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            // Use the most recent location
            if (gpsLocation != null && networkLocation != null) {
                currentLocation = gpsLocation.getTime() > networkLocation.getTime() ? 
                    gpsLocation : networkLocation;
            } else if (gpsLocation != null) {
                currentLocation = gpsLocation;
            } else if (networkLocation != null) {
                currentLocation = networkLocation;
            }

            if (currentLocation != null && 
                System.currentTimeMillis() - currentLocation.getTime() < 60000) {
                // Location is recent (within 1 minute)
                LocationInfo info = locationToInfo(currentLocation);
                callback.onLocationReceived(info);
            } else {
                // Request fresh location
                requestFreshLocation(callback);
            }
        } catch (Exception e) {
            callback.onError("Location fetch error: " + e.getMessage());
        }
    }

    /**
     * Request fresh location update
     */
    @SuppressLint("MissingPermission")
    private void requestFreshLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onError("Location permission chahiye");
            return;
        }

        isListeningForLocation = true;

        try {
            // Request from both providers for faster response
            if (isGpsEnabled()) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, 
                    this, Looper.getMainLooper());
            }
            
            if (isNetworkLocationEnabled()) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, 
                    this, Looper.getMainLooper());
            }

            // Set timeout for location request
            new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isListeningForLocation) {
                    stopLocationUpdates();
                    if (currentLocation != null) {
                        LocationInfo info = locationToInfo(currentLocation);
                        callback.onLocationReceived(info);
                    } else {
                        callback.onError("Location timeout. Please try again.");
                    }
                }
            }, 30000); // 30 second timeout

        } catch (Exception e) {
            callback.onError("Location request error: " + e.getMessage());
        }
    }

    /**
     * Start continuous location updates
     */
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(long minTime, float minDistance, LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onError("Location permission chahiye");
            return;
        }

        if (!isLocationEnabled()) {
            callback.onError("Location service band hai");
            return;
        }

        try {
            this.currentCallback = callback;
            isListeningForLocation = true;

            if (isGpsEnabled()) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
                    minTime, minDistance, this);
            }
            
            if (isNetworkLocationEnabled()) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
                    minTime, minDistance, this);
            }

            callback.onSuccess("Location updates shuru ho gaye!");
        } catch (Exception e) {
            callback.onError("Location updates error: " + e.getMessage());
        }
    }

    /**
     * Stop location updates
     */
    public void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        isListeningForLocation = false;
    }

    // ==================== Location Listener Implementation ====================

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        
        if (currentCallback != null) {
            LocationInfo info = locationToInfo(location);
            currentCallback.onLocationReceived(info);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Deprecated in API 29, but still needed for older versions
        Log.d(TAG, "Provider status changed: " + provider + " status: " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Provider disabled: " + provider);
    }

    // ==================== Location Sharing ====================

    /**
     * Share current location
     */
    @SuppressLint("MissingPermission")
    public void shareLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onError("Location permission chahiye");
            return;
        }

        getCurrentLocation(new LocationCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onLocationReceived(LocationInfo locationInfo) {
                try {
                    // Create Google Maps URI
                    String geoUri = "geo:" + locationInfo.latitude + "," + locationInfo.longitude;
                    String mapsUrl = "https://maps.google.com/?q=" + locationInfo.latitude + "," + locationInfo.longitude;
                    
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mera Location");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, 
                        "Mera current location: " + mapsUrl + 
                        "\n\nShared via RS Assistant");
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    Intent chooser = Intent.createChooser(shareIntent, "Location Share Karo");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(chooser);
                    
                    callback.onSuccess("Location share ke liye ready!");
                } catch (Exception e) {
                    callback.onError("Location share error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Share location as SMS
     */
    @SuppressLint("MissingPermission")
    public void shareLocationViaSms(String phoneNumber, LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onError("Location permission chahiye");
            return;
        }

        getCurrentLocation(new LocationCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onLocationReceived(LocationInfo locationInfo) {
                try {
                    String mapsUrl = "https://maps.google.com/?q=" + 
                        locationInfo.latitude + "," + locationInfo.longitude;
                    
                    String message = "Mera location: " + mapsUrl;
                    
                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                    smsIntent.setData(Uri.parse("smsto:" + phoneNumber));
                    smsIntent.putExtra("sms_body", message);
                    smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(smsIntent);
                    
                    callback.onSuccess("Location SMS ready!");
                } catch (Exception e) {
                    callback.onError("SMS error: " + e.getMessage());
                }
            }
        });
    }

    // ==================== Navigation ====================

    /**
     * Navigate to a place
     */
    public void navigateTo(String destination, LocationCallback callback) {
        try {
            // First geocode the destination
            geocodeDestination(destination, new GeocodeCallback() {
                @Override
                public void onResult(double latitude, double longitude, String address) {
                    openNavigation(latitude, longitude, destination, callback);
                }

                @Override
                public void onError(String error) {
                    // Try direct search in Google Maps
                    openNavigationSearch(destination, callback);
                }
            });
        } catch (Exception e) {
            callback.onError("Navigation error: " + e.getMessage());
        }
    }

    /**
     * Navigate to coordinates
     */
    public void navigateToCoordinates(double latitude, double longitude, LocationCallback callback) {
        openNavigation(latitude, longitude, "Destination", callback);
    }

    /**
     * Open navigation to specific coordinates
     */
    private void openNavigation(double latitude, double longitude, String label, LocationCallback callback) {
        try {
            // Open in Google Maps for navigation
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
                callback.onSuccess("Navigation shuru ho rahi hai " + label + " tak!");
            } else {
                // Fallback to browser
                String url = "https://maps.google.com/maps?daddr=" + latitude + "," + longitude;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
                callback.onSuccess("Maps browser mein khul gaya!");
            }
        } catch (Exception e) {
            callback.onError("Navigation nahi khul pa rahi: " + e.getMessage());
        }
    }

    /**
     * Open navigation with search query
     */
    private void openNavigationSearch(String query, LocationCallback callback) {
        try {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(query));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
                callback.onSuccess("Navigation " + query + " ke liye shuru ho rahi hai!");
            } else {
                String url = "https://maps.google.com/maps?daddr=" + Uri.encode(query);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
                callback.onSuccess("Maps browser mein khul gaya!");
            }
        } catch (Exception e) {
            callback.onError("Navigation error: " + e.getMessage());
        }
    }

    /**
     * Open Google Maps
     */
    public void openGoogleMaps(LocationCallback callback) {
        try {
            Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage("com.google.android.apps.maps");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Google Maps khul gaya!");
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com"));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
                callback.onSuccess("Maps browser mein khul gaya!");
            }
        } catch (Exception e) {
            callback.onError("Maps nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Search nearby places
     */
    public void searchNearby(String query, LocationCallback callback) {
        try {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
                callback.onSuccess("Nearby " + query + " search ho rahe hain!");
            } else {
                String url = "https://maps.google.com/maps?q=" + Uri.encode(query);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
                callback.onSuccess("Browser mein search ho raha hai!");
            }
        } catch (Exception e) {
            callback.onError("Nearby search error: " + e.getMessage());
        }
    }

    // ==================== Location Announcement ====================

    /**
     * Get current location announcement text
     */
    @SuppressLint("MissingPermission")
    public void getLocationAnnouncement(LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onError("Location permission chahiye");
            return;
        }

        getCurrentLocation(new LocationCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onLocationReceived(LocationInfo locationInfo) {
                StringBuilder announcement = new StringBuilder();
                announcement.append("Apka current location hai. ");
                
                if (locationInfo.address != null && !locationInfo.address.isEmpty()) {
                    announcement.append(locationInfo.address);
                } else {
                    announcement.append("Latitude: ").append(String.format("%.4f", locationInfo.latitude));
                    announcement.append(", Longitude: ").append(String.format("%.4f", locationInfo.longitude));
                }
                
                if (locationInfo.city != null) {
                    announcement.append(", ").append(locationInfo.city);
                }
                
                if (locationInfo.accuracy > 0) {
                    announcement.append(". Accuracy: ").append((int)locationInfo.accuracy).append(" meters");
                }

                callback.onSuccess(announcement.toString());
            }
        });
    }

    // ==================== Geocoding ====================

    private interface GeocodeCallback {
        void onResult(double latitude, double longitude, String address);
        void onError(String error);
    }

    /**
     * Geocode destination string to coordinates
     */
    private void geocodeDestination(String destination, GeocodeCallback callback) {
        if (Geocoder.isPresent()) {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(destination, 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    callback.onResult(address.getLatitude(), address.getLongitude(), 
                        address.getAddressLine(0));
                } else {
                    callback.onError("Location nahi mila");
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding error: " + e.getMessage());
                callback.onError("Geocoding error: " + e.getMessage());
            }
        } else {
            callback.onError("Geocoder available nahi hai");
        }
    }

    /**
     * Reverse geocode coordinates to address
     */
    private String reverseGeocode(double latitude, double longitude) {
        if (Geocoder.isPresent()) {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(address.getAddressLine(i));
                    }
                    
                    return sb.toString();
                }
            } catch (IOException e) {
                Log.e(TAG, "Reverse geocoding error: " + e.getMessage());
            }
        }
        return null;
    }

    // ==================== Utility Methods ====================

    /**
     * Convert Location to LocationInfo
     */
    private LocationInfo locationToInfo(Location location) {
        LocationInfo info = new LocationInfo();
        info.latitude = location.getLatitude();
        info.longitude = location.getLongitude();
        info.altitude = location.hasAltitude() ? location.getAltitude() : 0;
        info.accuracy = location.hasAccuracy() ? location.getAccuracy() : 0;
        info.speed = location.hasSpeed() ? location.getSpeed() : 0;
        info.bearing = location.hasBearing() ? location.getBearing() : 0;
        info.timestamp = location.getTime();

        // Get address
        String address = reverseGeocode(info.latitude, info.longitude);
        if (address != null) {
            info.address = address;
            
            // Extract city and country
            if (Geocoder.isPresent()) {
                try {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(info.latitude, info.longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        info.city = addr.getLocality();
                        info.country = addr.getCountryName();
                        info.postalCode = addr.getPostalCode();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting address details: " + e.getMessage());
                }
            }
        }

        return info;
    }

    /**
     * Calculate distance between two points
     */
    public static float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    /**
     * Format distance for display
     */
    public static String formatDistance(float meters) {
        if (meters < 1000) {
            return String.format(Locale.getDefault(), "%.0f meters", meters);
        } else {
            return String.format(Locale.getDefault(), "%.1f km", meters / 1000);
        }
    }

    /**
     * Get last known location
     */
    @SuppressLint("MissingPermission")
    public Location getLastKnownLocation() {
        if (!hasLocationPermission()) {
            return null;
        }

        try {
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (gpsLocation != null && networkLocation != null) {
                return gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
            } else if (gpsLocation != null) {
                return gpsLocation;
            } else {
                return networkLocation;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last known location: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if listening for location updates
     */
    public boolean isListeningForLocation() {
        return isListeningForLocation;
    }

    /**
     * Get current cached location
     */
    public Location getCurrentCachedLocation() {
        return currentLocation;
    }
}
