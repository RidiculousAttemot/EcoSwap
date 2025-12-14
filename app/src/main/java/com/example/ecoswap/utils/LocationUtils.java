package com.example.ecoswap.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Shared helpers for geocoding user-entered locations and computing distances between coordinates.
 */
public final class LocationUtils {

    private static final String TAG = "LocationUtils";

    private LocationUtils() {
        // Utility class
    }

    /**
     * Geocode a human readable location string into latitude/longitude coordinates.
     * Returns null if the lookup fails or the device does not have geocoder data available.
     */
    @Nullable
    public static Coordinates geocodeLocation(@NonNull Context context, @Nullable String locationQuery) {
        if (TextUtils.isEmpty(locationQuery)) {
            return null;
        }

        try {
            Geocoder geocoder = new Geocoder(context.getApplicationContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(locationQuery, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return new Coordinates(address.getLatitude(), address.getLongitude());
            }
        } catch (IOException | IllegalArgumentException geocodeError) {
            Log.w(TAG, "Failed to geocode location: " + locationQuery, geocodeError);
        }
        return null;
    }

    /**
     * Calculate the straight-line distance (in kilometers) between two coordinates.
     */
    public static double calculateDistanceKm(double startLat, double startLon, double endLat, double endLon) {
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLon, endLat, endLon, results);
        return results[0] / 1000d;
    }

    /**
     * Human friendly distance label such as "2.3 km away" or "<1 km away".
     */
    public static String formatDistanceLabel(double distanceKm) {
        if (distanceKm < 1d) {
            return "<1 km away";
        }
        if (distanceKm < 10d) {
            return String.format(Locale.getDefault(), "%.1f km away", distanceKm);
        }
        return String.format(Locale.getDefault(), "%.0f km away", distanceKm);
    }

    /**
     * Reverse geocode coordinates to a descriptive label that can prefill a text field.
     */
    @Nullable
    public static String reverseGeocodeCoordinates(@NonNull Context context, double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(context.getApplicationContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                if (!TextUtils.isEmpty(fullAddress)) {
                    return fullAddress;
                }
                String locality = address.getLocality();
                String adminArea = address.getAdminArea();
                if (!TextUtils.isEmpty(locality) && !TextUtils.isEmpty(adminArea)) {
                    return locality + ", " + adminArea;
                }
                if (!TextUtils.isEmpty(locality)) {
                    return locality;
                }
                if (!TextUtils.isEmpty(adminArea)) {
                    return adminArea;
                }
            }
        } catch (IOException | IllegalArgumentException geocodeError) {
            Log.w(TAG, "Failed to reverse geocode coordinates", geocodeError);
        }
        return null;
    }

    public static final class Coordinates {
        private final double latitude;
        private final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}
