package com.example.cse476;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class to handle all location-related operations
 * Separates location logic from the main activity for cleaner code
 */
public class LocationHelper {
    // for permissions
    private final Context context;
    // System service that provides location data
    private final LocationManager locationManager;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Constructor - initializes the location manager
    public LocationHelper(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Check if the app has location permissions
     * @return true if the user granted location permissions
     */
    public boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request location permissions from the user
     * @param activity - this will handle the permission result
     */
    public void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * Check if device location services are enabled
     * @return true if GPS or network location is enabled
     */
    public boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Main method to open directions to club location
     * Includes permissions, location services, and opening maps
     * @param activity - the calling activity
     * @param clubLocation - the destination address
     */
    public void openDirections(Activity activity, String clubLocation) {
        // First check if location services are enabled
        if (!isLocationEnabled()) {
            Toast.makeText(activity, "Please enable location services to get directions", Toast.LENGTH_LONG).show();
            // Open location settings so user can enable it
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(intent);
            return;
        }

        // Next check if we have location permissions
        if (!checkLocationPermissions()) {
            requestLocationPermissions(activity);
            return;
        }

        // Step 3: Open maps with the destination
        openMapDirections(activity, clubLocation);
    }

    /**
     * Open maps app with navigation to destination
     * Has multiple fallbacks in case Google Maps isn't available
     * @param activity - the calling activity
     * @param destination - where to navigate to
     */
    private void openMapDirections(Activity activity, String destination) {
        try {
            // First try Google Maps navigation
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(destination));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(mapIntent);
            } else {
                // Fallback: Any map app that can handle geo coordinates
                Uri fallbackUri = Uri.parse("geo:0,0?q=" + Uri.encode(destination));
                Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, fallbackUri);
                activity.startActivity(fallbackIntent);
            }
        } catch (Exception e) {
            // Open in web browser if all else fails
            Uri webIntentUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(destination));
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webIntentUri);
            activity.startActivity(webIntent);
        }
    }
}