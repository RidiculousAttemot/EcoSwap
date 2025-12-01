package com.example.ecoswap.dashboard;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.LocationFeatureCompat;
import com.example.ecoswap.utils.LocationUtils;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.chip.Chip;
import okhttp3.HttpUrl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MarketplaceFragment extends Fragment {

    private static final String TAG = "MarketplaceFragment";
    private EditText etSearch;
    private Chip chipAll, chipElectronics, chipClothing, chipBooks, chipFurniture;
    private RecyclerView rvItems;
    private ProgressBar progressListings;
    private TextView tvEmptyState;
    private MarketplaceAdapter adapter;
    private final List<MarketplaceItem> allItems = new ArrayList<>();
    private final List<MarketplaceItem> filteredItems = new ArrayList<>();
    private String selectedCategory = "All";
    private String userLocation;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private Double userLatitude;
    private Double userLongitude;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_marketplace, container, false);

        initViews(view);
        initDataProviders();
        setupRecyclerView();
        setupSearchListener();
        setupCategoryFilters();
        fetchCurrentUserLocation();
        loadListingsFromSupabase();

        return view;
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        chipAll = view.findViewById(R.id.chipAll);
        chipElectronics = view.findViewById(R.id.chipElectronics);
        chipClothing = view.findViewById(R.id.chipClothing);
        chipBooks = view.findViewById(R.id.chipBooks);
        chipFurniture = view.findViewById(R.id.chipFurniture);
        rvItems = view.findViewById(R.id.rvItems);
        progressListings = view.findViewById(R.id.progressListings);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void initDataProviders() {
        if (getContext() == null) {
            return;
        }
        sessionManager = SessionManager.getInstance(requireContext());
        supabaseClient = SupabaseClient.getInstance(requireContext());
        supabaseClient.hydrateSession(
            sessionManager.getAccessToken(),
            sessionManager.getRefreshToken(),
            sessionManager.getAccessTokenExpiry(),
            sessionManager.getUserId()
        );
    }

    private void setupRecyclerView() {
        adapter = new MarketplaceAdapter(new ArrayList<>(), requireContext(), this::openListingPreview);
        rvItems.setLayoutManager(new LinearLayoutManager(getContext()));
        rvItems.setAdapter(adapter);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryFilters() {
        View.OnClickListener categoryListener = v -> {
            resetChipStyles();

            Chip clickedChip = (Chip) v;
            clickedChip.setChipBackgroundColorResource(R.color.primary_green);
            clickedChip.setTextColor(getResources().getColor(R.color.text_white, null));

            selectedCategory = clickedChip.getText().toString();
            applyFilters();
        };

        chipAll.setOnClickListener(categoryListener);
        chipElectronics.setOnClickListener(categoryListener);
        chipClothing.setOnClickListener(categoryListener);
        chipBooks.setOnClickListener(categoryListener);
        chipFurniture.setOnClickListener(categoryListener);
    }

    private void resetChipStyles() {
        Chip[] chips = {chipAll, chipElectronics, chipClothing, chipBooks, chipFurniture};
        for (Chip chip : chips) {
            chip.setChipBackgroundColorResource(R.color.background_white);
            chip.setTextColor(getResources().getColor(R.color.text_primary, null));
        }
    }

    private void fetchCurrentUserLocation() {
        if (sessionManager == null || supabaseClient == null) {
            return;
        }
        String userId = sessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            return;
        }

        String endpoint = "/rest/v1/profiles?select=location&id=eq." + userId + "&limit=1";
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) {
                    return;
                }
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() > 0) {
                        JSONObject profile = array.getJSONObject(0);
                        String locationValue = profile.optString("location", null);
                        if (!TextUtils.isEmpty(locationValue)) {
                            userLocation = locationValue;
                            applyFilters();
                            geocodeUserCoordinates(locationValue);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse profile location", e);
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Unable to fetch user location: " + error);
            }
        });
    }

    private void geocodeUserCoordinates(String locationValue) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        geocodeExecutor.execute(() -> {
            LocationUtils.Coordinates coordinates = LocationUtils.geocodeLocation(appContext, locationValue);
            if (coordinates != null) {
                userLatitude = coordinates.getLatitude();
                userLongitude = coordinates.getLongitude();
            } else {
                userLatitude = null;
                userLongitude = null;
            }
            mainHandler.post(this::applyFilters);
        });
    }

    private void loadListingsFromSupabase() {
        if (supabaseClient == null) {
            Log.e(TAG, "Supabase client not initialized");
            return;
        }
        showLoading(true);
        boolean includeCoordinates = LocationFeatureCompat.areCoordinatesSupported();
        String endpoint = buildListingsEndpoint(includeCoordinates);

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) {
                    return;
                }
                showLoading(false);
                parseListings(data);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                if (LocationFeatureCompat.areCoordinatesSupported() && isMissingCoordinateColumnError(error)) {
                    Log.w(TAG, "Supabase is missing latitude/longitude columns; reloading without distance data.");
                    LocationFeatureCompat.markCoordinatesUnsupported();
                    userLatitude = null;
                    userLongitude = null;
                    loadListingsFromSupabase();
                    return;
                }
                showLoading(false);
                handleListingsError(error);
            }
        });
    }

    private String buildListingsEndpoint(boolean includeCoordinates) {
        if (supabaseClient == null || TextUtils.isEmpty(supabaseClient.getSupabaseUrl())) {
            return "/rest/v1/posts";
        }
        HttpUrl baseUrl = HttpUrl.parse(supabaseClient.getSupabaseUrl());
        if (baseUrl == null) {
            return "/rest/v1/posts";
        }

        String selectFields = "id,title,description,category,condition,location,image_url,user_id,status";
        if (includeCoordinates) {
            selectFields += ",latitude,longitude";
        }
        selectFields += ",profiles(name,location)";

        HttpUrl url = baseUrl.newBuilder()
                .addPathSegments("rest/v1/posts")
                .addQueryParameter("select", selectFields)
                .addQueryParameter("status", "eq.available")
                .addQueryParameter("order", "created_at.desc")
                .addQueryParameter("limit", "50")
                .build();

        return url.toString().replace(supabaseClient.getSupabaseUrl(), "");
    }

    private void parseListings(Object data) {
        try {
            JSONArray listings = new JSONArray(data.toString());
            allItems.clear();
            for (int i = 0; i < listings.length(); i++) {
                JSONObject listing = listings.getJSONObject(i);
                MarketplaceItem item = mapListing(listing);
                if (item != null) {
                    allItems.add(item);
                }
            }
            applyFilters();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse listings", e);
            handleListingsError("Unable to load listings right now.");
        }
    }

    private MarketplaceItem mapListing(JSONObject listing) {
        MarketplaceItem item = new MarketplaceItem();
        item.setId(listing.optString("id"));
        item.setTitle(listing.optString("title", "Untitled listing"));
        item.setDescription(listing.optString("description"));
        item.setRawCategory(listing.optString("category", "other"));
        item.setDisplayCategory(formatCategoryLabel(item.getRawCategory()));
        item.setRawCondition(listing.optString("condition", "good"));
        item.setDisplayCondition(formatConditionLabel(item.getRawCondition()));
        item.setImageUrl(listing.optString("image_url", null));
        item.setOwnerId(listing.optString("user_id"));
        if (listing.has("latitude") && !listing.isNull("latitude")) {
            item.setLatitude(listing.optDouble("latitude"));
        }
        if (listing.has("longitude") && !listing.isNull("longitude")) {
            item.setLongitude(listing.optDouble("longitude"));
        }

        String listingLocation = listing.optString("location", null);
        JSONObject profile = listing.optJSONObject("profiles");
        if (profile != null) {
            String posterName = profile.optString("name", null);
            if (!TextUtils.isEmpty(posterName)) {
                item.setPostedBy(posterName);
            }
            if (TextUtils.isEmpty(listingLocation)) {
                listingLocation = profile.optString("location", null);
            }
        }

        item.setPostedBy(!TextUtils.isEmpty(item.getPostedBy()) ? item.getPostedBy() : getString(R.string.app_name));
        item.setLocation(!TextUtils.isEmpty(listingLocation) ? listingLocation : getString(R.string.location_unknown));

        return item;
    }

    private void openListingPreview(@NonNull MarketplaceItem item) {
        if (!isAdded()) {
            return;
        }
        ListingPreviewBottomSheet.newInstance(item)
                .show(getParentFragmentManager(), "listing_preview_sheet");
    }

    private void applyFilters() {
        if (adapter == null) {
            return;
        }
        filteredItems.clear();
        String searchQuery = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase(Locale.US) : "";
        String categoryKey = normalizeCategory(selectedCategory);
        String normalizedLocation = !TextUtils.isEmpty(userLocation) ? userLocation.toLowerCase(Locale.US) : null;
        boolean hasUserCoordinates = userLatitude != null && userLongitude != null;

        for (MarketplaceItem item : allItems) {
            boolean matchesCategory = TextUtils.isEmpty(categoryKey) || categoryKey.equalsIgnoreCase(item.getRawCategory());
            boolean matchesQuery = searchQuery.isEmpty() || matchesQuery(item, searchQuery);

            if (matchesCategory && matchesQuery) {
                Double computedDistance = null;
                if (hasUserCoordinates && item.hasCoordinates()) {
                    computedDistance = LocationUtils.calculateDistanceKm(
                            userLatitude,
                            userLongitude,
                            item.getLatitude(),
                            item.getLongitude()
                    );
                }

                item.setDistanceKm(computedDistance);

                boolean isNearUser;
                if (computedDistance != null) {
                    isNearUser = computedDistance <= 10d;
                } else {
                    isNearUser = !TextUtils.isEmpty(normalizedLocation) && item.getLocation() != null
                            && item.getLocation().toLowerCase(Locale.US).contains(normalizedLocation);
                }
                item.setNearUser(isNearUser);
                filteredItems.add(item);
            }
        }

        if (hasUserCoordinates) {
            Collections.sort(filteredItems, (first, second) -> {
                Double firstDistance = first.getDistanceKm();
                Double secondDistance = second.getDistanceKm();
                if (firstDistance == null && secondDistance == null) {
                    return 0;
                }
                if (firstDistance == null) {
                    return 1;
                }
                if (secondDistance == null) {
                    return -1;
                }
                return Double.compare(firstDistance, secondDistance);
            });
        } else if (!TextUtils.isEmpty(normalizedLocation)) {
            Collections.sort(filteredItems, (first, second) -> {
                if (first.isNearUser() == second.isNearUser()) {
                    return 0;
                }
                return first.isNearUser() ? -1 : 1;
            });
        }

        adapter.updateItems(filteredItems);
        updateEmptyState();
    }

    private boolean matchesQuery(MarketplaceItem item, String query) {
        return (item.getTitle() != null && item.getTitle().toLowerCase(Locale.US).contains(query))
                || (item.getDescription() != null && item.getDescription().toLowerCase(Locale.US).contains(query))
                || (item.getLocation() != null && item.getLocation().toLowerCase(Locale.US).contains(query))
                || (item.getDisplayCategory() != null && item.getDisplayCategory().toLowerCase(Locale.US).contains(query));
    }

    private void updateEmptyState() {
        boolean hasItems = adapter.getItemCount() > 0;
        rvItems.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        tvEmptyState.setVisibility(hasItems ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        if (progressListings != null) {
            progressListings.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void handleListingsError(String message) {
        String displayMessage = !TextUtils.isEmpty(message) ? message : getString(R.string.error_loading_listings);
        tvEmptyState.setText(displayMessage);
        tvEmptyState.setVisibility(View.VISIBLE);
        rvItems.setVisibility(View.GONE);
        if (getContext() != null) {
            Toast.makeText(getContext(), displayMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizeCategory(String label) {
        if (TextUtils.isEmpty(label) || "All".equalsIgnoreCase(label)) {
            return null;
        }
        switch (label.toLowerCase(Locale.US)) {
            case "electronics":
                return "electronics";
            case "clothing":
                return "clothing";
            case "books":
                return "books";
            case "furniture":
                return "furniture";
            default:
                return label.toLowerCase(Locale.US);
        }
    }

    private String formatCategoryLabel(String rawCategory) {
        if (TextUtils.isEmpty(rawCategory)) {
            return getString(R.string.category_other);
        }
        String lowered = rawCategory.toLowerCase(Locale.US);
        switch (lowered) {
            case "electronics":
                return getString(R.string.category_electronics);
            case "clothing":
                return getString(R.string.category_clothing);
            case "books":
                return getString(R.string.category_books);
            case "furniture":
                return getString(R.string.category_furniture);
            case "swap":
                return getString(R.string.category_swap);
            case "donation":
                return getString(R.string.category_donation);
            default:
                return capitalize(lowered);
        }
    }

    private String formatConditionLabel(String rawCondition) {
        if (TextUtils.isEmpty(rawCondition)) {
            return getString(R.string.condition_good);
        }
        switch (rawCondition.toLowerCase(Locale.US)) {
            case "new":
                return getString(R.string.condition_new);
            case "like_new":
                return getString(R.string.condition_like_new);
            case "good":
                return getString(R.string.condition_good);
            case "fair":
                return getString(R.string.condition_fair);
            case "poor":
                return getString(R.string.condition_poor);
            default:
                return capitalize(rawCondition);
        }
    }

    private String capitalize(String value) {
        if (TextUtils.isEmpty(value)) {
            return value;
        }
        String lower = value.toLowerCase(Locale.US);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private boolean isMissingCoordinateColumnError(String error) {
        if (TextUtils.isEmpty(error)) {
            return false;
        }
        String lower = error.toLowerCase(Locale.US);
        return lower.contains("42703") || (lower.contains("column") && (lower.contains("latitude") || lower.contains("longitude")));
    }

    // Inner class for marketplace items
    public static class MarketplaceItem {
        private String id;
        private String title;
        private String description;
        private String location;
        private String postedBy;
        private String rawCategory;
        private String displayCategory;
        private String rawCondition;
        private String displayCondition;
        private String imageUrl;
        private String ownerId;
        private boolean nearUser;
        private Double latitude;
        private Double longitude;
        private Double distanceKm;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getPostedBy() { return postedBy; }
        public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

        public String getRawCategory() { return rawCategory; }
        public void setRawCategory(String rawCategory) { this.rawCategory = rawCategory; }

        public String getDisplayCategory() { return displayCategory; }
        public void setDisplayCategory(String displayCategory) { this.displayCategory = displayCategory; }

        public String getRawCondition() { return rawCondition; }
        public void setRawCondition(String rawCondition) { this.rawCondition = rawCondition; }

        public String getDisplayCondition() { return displayCondition; }
        public void setDisplayCondition(String displayCondition) { this.displayCondition = displayCondition; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getOwnerId() { return ownerId; }
        public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

        public boolean isNearUser() { return nearUser; }
        public void setNearUser(boolean nearUser) { this.nearUser = nearUser; }

        public boolean hasCoordinates() {
            return latitude != null && longitude != null;
        }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public Double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    }
}
