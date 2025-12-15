package com.example.ecoswap.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Persists lightweight conversation metadata (listing id/title/image) so that
 * chat and inbox screens can display context even if the backend omits it.
 */
public class ConversationMetadataStore {

    private static final String PREFS = "conversation_metadata_store";
    private static final String KEY_TEMPLATE = "%s|%s";
    private static final String KEY_ARCHIVED = "archived_conversations";
    private static final String KEY_BLOCKED = "blocked_conversations";

    private final SharedPreferences prefs;

    public ConversationMetadataStore(@Nullable Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveListingContext(String userId, @Nullable String listingId, @Nullable String title, @Nullable String imageUrl) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        String normalizedListingId = normalize(listingId);
        String normalizedTitle = normalize(title);
        String normalizedImage = normalize(imageUrl);
        if (TextUtils.isEmpty(normalizedListingId)
                && TextUtils.isEmpty(normalizedTitle)
                && TextUtils.isEmpty(normalizedImage)) {
            return;
        }
        JSONObject payload = new JSONObject();
        try {
            if (!TextUtils.isEmpty(normalizedListingId)) {
                payload.put("listingId", normalizedListingId);
            }
            if (!TextUtils.isEmpty(normalizedTitle)) {
                payload.put("title", normalizedTitle);
            }
            if (!TextUtils.isEmpty(normalizedImage)) {
                payload.put("imageUrl", normalizedImage);
            }
        } catch (JSONException ignored) {
            // If we cannot encode, skip persisting.
            return;
        }
        String key = buildKey(userId, normalizedListingId);
        prefs.edit().putString(key, payload.toString()).apply();
        if (!TextUtils.isEmpty(normalizedListingId)) {
            // Store under the "direct" key as a fallback for chats where listing_id was omitted.
            prefs.edit().putString(buildKey(userId, null), payload.toString()).apply();
        }
    }

    public void setArchived(String userId, @Nullable String listingId, boolean archived) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        String key = buildKey(userId, listingId);
        Set<String> archivedSet = getArchivedSet();
        if (archived) {
            archivedSet.add(key);
        } else {
            archivedSet.remove(key);
        }
        prefs.edit().putStringSet(KEY_ARCHIVED, archivedSet).apply();
    }

    public boolean isArchived(String userId, @Nullable String listingId) {
        if (TextUtils.isEmpty(userId)) {
            return false;
        }
        return getArchivedSet().contains(buildKey(userId, listingId));
    }

    public void setBlocked(String userId, @Nullable String listingId, boolean blocked) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        String key = buildKey(userId, listingId);
        Set<String> blockedSet = getBlockedSet();
        if (blocked) {
            blockedSet.add(key);
        } else {
            blockedSet.remove(key);
        }
        prefs.edit().putStringSet(KEY_BLOCKED, blockedSet).apply();
    }

    public boolean isBlocked(String userId, @Nullable String listingId) {
        if (TextUtils.isEmpty(userId)) {
            return false;
        }
        return getBlockedSet().contains(buildKey(userId, listingId));
    }

    private Set<String> getArchivedSet() {
        Set<String> stored = prefs.getStringSet(KEY_ARCHIVED, null);
        if (stored == null) {
            return new java.util.HashSet<>();
        }
        return new java.util.HashSet<>(stored);
    }

    private Set<String> getBlockedSet() {
        Set<String> stored = prefs.getStringSet(KEY_BLOCKED, null);
        if (stored == null) {
            return new java.util.HashSet<>();
        }
        return new java.util.HashSet<>(stored);
    }

    @Nullable
    public ListingContext getListingContext(String userId, @Nullable String listingId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        String encoded = prefs.getString(buildKey(userId, listingId), null);
        if (encoded == null && !TextUtils.isEmpty(listingId)) {
            encoded = prefs.getString(buildKey(userId, null), null);
        }
        if (TextUtils.isEmpty(encoded)) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(encoded);
            String resolvedListingId = normalize(object.optString("listingId", listingId));
            String resolvedTitle = normalize(object.optString("title", null));
            String resolvedImage = normalize(object.optString("imageUrl", null));
            if (TextUtils.isEmpty(resolvedListingId)) {
                resolvedListingId = listingId;
            }
            return new ListingContext(resolvedListingId, resolvedTitle, resolvedImage);
        } catch (JSONException e) {
            return null;
        }
    }

    private String buildKey(String userId, @Nullable String listingId) {
        String resolvedListingId = !TextUtils.isEmpty(listingId) ? listingId : "direct";
        return String.format(Locale.US, KEY_TEMPLATE, userId, resolvedListingId);
    }

    @Nullable
    private String normalize(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("null".equalsIgnoreCase(trimmed) || "undefined".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    public static class ListingContext {
        public final String listingId;
        public final String title;
        public final String imageUrl;

        public ListingContext(@Nullable String listingId, @Nullable String title, @Nullable String imageUrl) {
            this.listingId = listingId;
            this.title = title;
            this.imageUrl = imageUrl;
        }
    }
}
