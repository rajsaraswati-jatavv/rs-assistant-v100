package com.rsassistant.control;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * NavigationController - Voice-based Navigation and Location Manager
 * 
 * Supported Commands:
 * - "Maps kholo" / "Open maps"
 * - "Delhi ka route batao" / "Navigate to Delhi"
 * - "Mera location batao" / "Where am I"
 * - "Nearby restaurant dhoondo" / "Find nearby restaurants"
 */
public class NavigationController {

    private static final String TAG = "NavigationController";
    private final Context context;
    private final LocationManager locationManager;
    private final Geocoder geocoder;

    public interface NavigationCallback {
        void onSuccess(String message);
        void onError(String error);
        void onLocation(LocationInfo location);
        void onPlaces(List<PlaceInfo> places);
    }

    public static class LocationInfo {
        public double latitude;
        public double longitude;
        public String address;
        public String city;
        public String country;

        public LocationInfo(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static class PlaceInfo {
        public String name;
        public String address;
        public double latitude;
        public double longitude;
        public String type;
        public float rating;

        public PlaceInfo(String name, String address, double lat, double lng) {
            this.name = name;
            this.address = address;
            this.latitude = lat;
            this.longitude = lng;
        }
    }

    public NavigationController(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }

    /**
     * Open Google Maps
     */
    public void openMaps(NavigationCallback callback) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onSuccess("Google Maps khul gaya!");
            } else {
                callback.onError("Google Maps installed nahi hai");
            }
        } catch (Exception e) {
            callback.onError("Maps nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Navigate to a location by name
     */
    public void navigateTo(String destination, NavigationCallback callback) {
        try {
            String uri = "google.navigation:q=" + Uri.encode(destination);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess(destination + " ka navigation shuru ho gaya!");
        } catch (Exception e) {
            // Fallback to browser
            try {
                String url = "https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
                callback.onSuccess("Maps browser mein khul gaya!");
            } catch (Exception e2) {
                callback.onError("Navigation nahi ho pa raha: " + e.getMessage());
            }
        }
    }

    /**
     * Navigate to specific coordinates
     */
    public void navigateToCoordinates(double latitude, double longitude, String label, NavigationCallback callback) {
        try {
            String uri = "google.navigation:q=" + latitude + "," + longitude;
            if (label != null && !label.isEmpty()) {
                uri += "(" + Uri.encode(label) + ")";
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Navigation shuru ho gaya!");
        } catch (Exception e) {
            callback.onError("Navigation nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Search location on map
     */
    public void searchLocation(String query, NavigationCallback callback) {
        try {
            String uri = "geo:0,0?q=" + Uri.encode(query);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Location search ho raha hai!");
        } catch (Exception e) {
            callback.onError("Search nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Get current location
     */
    public void getCurrentLocation(NavigationCallback callback) {
        try {
            Location location = null;
            
            // Try GPS first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            
            // Fallback to network
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                LocationInfo info = new LocationInfo(location.getLatitude(), location.getLongitude());
                
                // Get address
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        info.address = address.getAddressLine(0);
                        info.city = address.getLocality();
                        info.country = address.getCountryName();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoding failed: " + e.getMessage());
                }
                
                callback.onLocation(info);
            } else {
                callback.onError("Location nahi mil pa raha. GPS on karein.");
            }
        } catch (SecurityException e) {
            callback.onError("Location permission denied");
        } catch (Exception e) {
            callback.onError("Location error: " + e.getMessage());
        }
    }

    /**
     * Find nearby places
     */
    public void findNearby(String placeType, NavigationCallback callback) {
        try {
            // Get current location first
            Location location = null;
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (location == null) {
                callback.onError("Pehle location on karein");
                return;
            }
            
            // Open Google Maps with nearby search
            String uri = "geo:" + location.getLatitude() + "," + location.getLongitude() + 
                        "?q=" + Uri.encode(placeType);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Nearby " + placeType + " dhoond rahe hain!");
        } catch (Exception e) {
            callback.onError("Search nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Find nearby restaurants
     */
    public void findNearbyRestaurants(NavigationCallback callback) {
        findNearby("restaurant", callback);
    }

    /**
     * Find nearby gas stations
     */
    public void findNearbyGasStations(NavigationCallback callback) {
        findNearby("gas station", callback);
    }

    /**
     * Find nearby hospitals
     */
    public void findNearbyHospitals(NavigationCallback callback) {
        findNearby("hospital", callback);
    }

    /**
     * Find nearby ATMs
     */
    public void findNearbyATMs(NavigationCallback callback) {
        findNearby("ATM", callback);
    }

    /**
     * Find nearby hotels
     */
    public void findNearbyHotels(NavigationCallback callback) {
        findNearby("hotel", callback);
    }

    /**
     * Share current location
     */
    public void shareLocation(NavigationCallback callback) {
        try {
            Location location = null;
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                String locationUrl = "https://maps.google.com/?q=" + 
                    location.getLatitude() + "," + location.getLongitude();
                
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Mera location: " + locationUrl);
                
                Intent chooser = Intent.createChooser(shareIntent, "Location share karein");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
                
                callback.onSuccess("Location share karne ka option aa gaya!");
            } else {
                callback.onError("Location nahi mil pa raha");
            }
        } catch (Exception e) {
            callback.onError("Share nahi ho pa raha: " + e.getMessage());
        }
    }

    /**
     * Open Street View
     */
    public void openStreetView(double latitude, double longitude, NavigationCallback callback) {
        try {
            String uri = "google.streetview:cbll=" + latitude + "," + longitude + "&cbp=1,0,,0,1.0&mz=20";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Street View khul gaya!");
        } catch (Exception e) {
            callback.onError("Street View nahi khul pa raha: " + e.getMessage());
        }
    }

    /**
     * Check if GPS is enabled
     */
    public boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Open location settings
     */
    public void openLocationSettings(NavigationCallback callback) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onSuccess("Location settings khul gayi!");
        } catch (Exception e) {
            callback.onError("Settings nahi khul pa rahi: " + e.getMessage());
        }
    }

    /**
     * Get distance between two points
     */
    public float getDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0]; // in meters
    }

    /**
     * Calculate ETA (simple estimation)
     */
    public String calculateETA(float distanceMeters, float speedKmh) {
        if (speedKmh <= 0) speedKmh = 30; // Default 30 km/h
        
        float distanceKm = distanceMeters / 1000;
        float timeHours = distanceKm / speedKmh;
        int minutes = (int) (timeHours * 60);
        
        if (minutes < 60) {
            return minutes + " minutes";
        } else {
            int hours = minutes / 60;
            int mins = minutes % 60;
            return hours + " hours " + mins + " minutes";
        }
    }
}
