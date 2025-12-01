package com.example.ecoswap.utils;

/**
 * Tracks whether the backend supports latitude/longitude columns so the UI can
 * gracefully fall back when those columns are missing.
 */
public final class LocationFeatureCompat {

    private static volatile boolean coordinatesSupported = true;

    private LocationFeatureCompat() {
        // No instances
    }

    public static boolean areCoordinatesSupported() {
        return coordinatesSupported;
    }

    public static void markCoordinatesUnsupported() {
        coordinatesSupported = false;
    }

    public static void setCoordinatesSupported(boolean supported) {
        coordinatesSupported = supported;
    }

    public static void reset() {
        coordinatesSupported = true;
    }
}
