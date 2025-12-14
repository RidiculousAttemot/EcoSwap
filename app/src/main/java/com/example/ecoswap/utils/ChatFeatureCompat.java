package com.example.ecoswap.utils;

import android.text.TextUtils;
import java.util.Locale;

/**
 * Centralized feature switches for chat-related optional columns.
 */
public final class ChatFeatureCompat {

    private static volatile boolean listingMetadataSupported = true;

    private ChatFeatureCompat() {
        // Utility class
    }

    public static boolean isListingMetadataSupported() {
        return listingMetadataSupported;
    }

    public static void disableListingMetadata() {
        listingMetadataSupported = false;
    }

    public static void setListingMetadataSupported(boolean supported) {
        listingMetadataSupported = supported;
    }

    public static boolean isListingMetadataError(String error) {
        if (TextUtils.isEmpty(error)) {
            return false;
        }
        String lower = error.toLowerCase(Locale.US);
        return lower.contains("42703")
            || lower.contains("listing_id")
            || lower.contains("chats_listing_id_fkey")
            || lower.contains("listing_title_snapshot")
            || lower.contains("listing_image_url_snapshot");
    }
}
