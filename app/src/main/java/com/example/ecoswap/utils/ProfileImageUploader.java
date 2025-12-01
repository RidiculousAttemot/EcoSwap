package com.example.ecoswap.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.ecoswap.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles compression and upload of profile images to Supabase Storage.
 */
public final class ProfileImageUploader {

    private static final String TAG = "ProfileImageUploader";
    private static final String DEFAULT_BUCKET = "ecoswap-images";
    private static final int MAX_DIMENSION = 720;
    private static final int JPEG_QUALITY = 85;

    private ProfileImageUploader() {
        // Utility class
    }

    public interface Callback {
        void onUploadSuccess(@NonNull String publicUrl);
        void onUploadError(@NonNull String message);
    }

    public static void upload(Context context,
                              SupabaseClient supabaseClient,
                              String userId,
                              Uri imageUri,
                              Callback callback) {
        Objects.requireNonNull(context, "Context must not be null");
        Objects.requireNonNull(supabaseClient, "SupabaseClient must not be null");
        Objects.requireNonNull(callback, "Callback must not be null");

        if (userId == null || userId.isEmpty()) {
            callback.onUploadError("Missing user id");
            return;
        }

        if (imageUri == null) {
            callback.onUploadError("No image selected");
            return;
        }

        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                byte[] imageBytes = decodeAndCompress(context.getContentResolver(), imageUri);
                if (imageBytes == null || imageBytes.length == 0) {
                    throw new IOException("Unable to process selected image");
                }

                String objectPath = userId + "/profile_" + UUID.randomUUID() + ".jpg";
                String bucketName = resolveBucketName();
                Log.d(TAG, "Uploading avatar for user=" + userId
                        + " bucket=" + bucketName
                        + " path=" + objectPath
                        + " bytes=" + imageBytes.length);
                supabaseClient.uploadFile(bucketName, objectPath, imageBytes, new SupabaseClient.OnStorageCallback() {
                    @Override
                    public void onSuccess(String url) {
                        Log.d(TAG, "Upload success for user=" + userId + " url=" + url);
                        callback.onUploadSuccess(url);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Upload failed for user=" + userId + ": " + error);
                        callback.onUploadError(error != null ? error : "Upload failed");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Image upload failed", e);
                String message = e.getMessage() != null ? e.getMessage() : "Unable to upload image";
                mainHandler.post(() -> callback.onUploadError(message));
            }
        }).start();
    }

    private static String resolveBucketName() {
        String configuredBucket = BuildConfig.SUPABASE_STORAGE_BUCKET;
        if (configuredBucket == null || configuredBucket.trim().isEmpty()) {
            Log.w(TAG, "SUPABASE_STORAGE_BUCKET missing. Falling back to " + DEFAULT_BUCKET);
            return DEFAULT_BUCKET;
        }
        return configuredBucket.trim();
    }

    private static byte[] decodeAndCompress(ContentResolver resolver, Uri uri) throws IOException {
        Bitmap bitmap = null;
        try {
            bitmap = decodeScaledBitmap(resolver, uri, MAX_DIMENSION);
            if (bitmap == null) {
                throw new IOException("Failed to decode image");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
            return outputStream.toByteArray();
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private static Bitmap decodeScaledBitmap(ContentResolver resolver, Uri uri, int maxDimension) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try (InputStream boundsStream = resolver.openInputStream(uri)) {
            if (boundsStream == null) {
                throw new IOException("Unable to read image bounds");
            }
            BitmapFactory.decodeStream(boundsStream, null, options);
        }

        int sampleSize = 1;
        while ((options.outWidth / sampleSize) > maxDimension || (options.outHeight / sampleSize) > maxDimension) {
            sampleSize *= 2;
        }

        options.inSampleSize = sampleSize;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try (InputStream decodeStream = resolver.openInputStream(uri)) {
            if (decodeStream == null) {
                throw new IOException("Unable to open image for decoding");
            }
            return BitmapFactory.decodeStream(decodeStream, null, options);
        }
    }
}
