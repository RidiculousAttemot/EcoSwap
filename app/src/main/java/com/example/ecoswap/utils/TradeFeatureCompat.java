package com.example.ecoswap.utils;

import android.text.TextUtils;
import java.util.Locale;

/**
 * Feature switch utilities for trade-related optional columns.
 */
public final class TradeFeatureCompat {

    private static volatile boolean proofPhotoSupported = true;

    private TradeFeatureCompat() {
        // Utility class
    }

    public static boolean isProofPhotoSupported() {
        return proofPhotoSupported;
    }

    public static void disableProofPhoto() {
        proofPhotoSupported = false;
    }

    public static void setProofPhotoSupported(boolean supported) {
        proofPhotoSupported = supported;
    }

    public static boolean isProofPhotoError(String error) {
        if (TextUtils.isEmpty(error)) {
            return false;
        }
        String lower = error.toLowerCase(Locale.US);
        return lower.contains("42703") || lower.contains("proof_photo_url");
    }
}
