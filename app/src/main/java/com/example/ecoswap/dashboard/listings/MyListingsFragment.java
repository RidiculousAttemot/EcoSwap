package com.example.ecoswap.dashboard.listings;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.ecoswap.R;
import com.example.ecoswap.dashboard.ListingPreviewBottomSheet;
import com.example.ecoswap.dashboard.MarketplaceFragment;
import com.example.ecoswap.dashboard.CreateListingFragment;
import com.example.ecoswap.dashboard.trades.TradeHistoryAdapter;
import com.example.ecoswap.dashboard.trades.TradeRecord;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.example.ecoswap.utils.TradeFeatureCompat;
import com.example.ecoswap.utils.TradeProofUploader;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MyListingsFragment extends Fragment
    implements TradeHistoryAdapter.TradeActionListener, ActiveListingsAdapter.OnListingClickListener {

    private static final String TAG = "MyListingsFragment";
    private static final String ARG_FOCUS_LISTING_ID = "focus_listing_id";
    private static final String ARG_INITIAL_TAB = "initial_tab";
    private static final int TAB_ACTIVE = 0;
    private static final int TAB_COMPLETED = 1;

    private MaterialButtonToggleGroup toggleGroup;
    private SwipeRefreshLayout swipeActive;
    private SwipeRefreshLayout swipeCompleted;
    private RecyclerView rvActive;
    private RecyclerView rvCompleted;
    private ProgressBar progressBar;
    private TextView tvEmptyActive;
    private TextView tvEmptyCompleted;

    private ActiveListingsAdapter activeAdapter;
    private TradeHistoryAdapter completedAdapter;

    private final List<ActiveListingsAdapter.ActiveListing> activeListings = new ArrayList<>();
    private final List<TradeRecord> tradeRecords = new ArrayList<>();
    private final List<TradeRecord> pendingTradeRecords = new ArrayList<>();
    private final Set<String> pendingProfileLookups = new HashSet<>();
    private final Map<String, String> userNameCache = new HashMap<>();

    private SessionManager sessionManager;
    private SupabaseClient supabaseClient;
    private String userId;

    private boolean loadingActiveListings = false;
    private int pendingTradeLoads = 0;
    private boolean uploadingProof = false;

    private String focusListingId;
    private int initialTab = TAB_ACTIVE;

    private ActivityResultLauncher<String> proofPickerLauncher;
    private TradeRecord recordAwaitingProof;

    public static MyListingsFragment newInstance(@Nullable String focusListingId, boolean openCompleted) {
        Bundle args = new Bundle();
        args.putString(ARG_FOCUS_LISTING_ID, focusListingId);
        args.putInt(ARG_INITIAL_TAB, openCompleted ? TAB_COMPLETED : TAB_ACTIVE);
        MyListingsFragment fragment = new MyListingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());
        supabaseClient = SupabaseClient.getInstance(requireContext());
        supabaseClient.hydrateSession(
                sessionManager.getAccessToken(),
                sessionManager.getRefreshToken(),
                sessionManager.getAccessTokenExpiry(),
                sessionManager.getUserId());
        userId = sessionManager.getUserId();

        if (getArguments() != null) {
            focusListingId = getArguments().getString(ARG_FOCUS_LISTING_ID);
            initialTab = getArguments().getInt(ARG_INITIAL_TAB, TAB_ACTIVE);
        }

        proofPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        beginProofUpload(uri);
                    } else {
                        recordAwaitingProof = null;
                    }
                });

        getParentFragmentManager().setFragmentResultListener("listing_updates", this, (requestKey, result) -> {
            if (result.getBoolean("refresh")) {
                loadActiveListings();
                loadCompletedTrades();
                if (result.getBoolean("openCompleted", false)) {
                    toggleGroup.check(R.id.btnToggleCompleted);
                    showCompletedTab();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_listings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupToolbar(view);
        setupToggle();
        setupLists();
        loadActiveListings();
        loadCompletedTrades();
    }

    private void initViews(@NonNull View root) {
        toggleGroup = root.findViewById(R.id.toggleListings);
        swipeActive = root.findViewById(R.id.swipeActiveListings);
        swipeCompleted = root.findViewById(R.id.swipeCompletedListings);
        rvActive = root.findViewById(R.id.rvActiveListings);
        rvCompleted = root.findViewById(R.id.rvCompletedTrades);
        progressBar = root.findViewById(R.id.progressListings);
        tvEmptyActive = root.findViewById(R.id.tvEmptyActive);
        tvEmptyCompleted = root.findViewById(R.id.tvEmptyCompleted);

        swipeActive.setColorSchemeResources(R.color.primary_green, R.color.primary_blue);
        swipeCompleted.setColorSchemeResources(R.color.primary_green, R.color.primary_blue);
        swipeActive.setOnRefreshListener(this::loadActiveListings);
        swipeCompleted.setOnRefreshListener(this::loadCompletedTrades);
    }

    private void setupToolbar(@NonNull View root) {
        MaterialToolbar toolbar = root.findViewById(R.id.myListingsToolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupToggle() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btnToggleActive) {
                showActiveTab();
            } else if (checkedId == R.id.btnToggleCompleted) {
                showCompletedTab();
            }
        });
        int initialButton = initialTab == TAB_COMPLETED ? R.id.btnToggleCompleted : R.id.btnToggleActive;
        toggleGroup.check(initialButton);
    }

    private void setupLists() {
        if (getContext() == null) {
            return;
        }
        activeAdapter = new ActiveListingsAdapter(requireContext(), this);
        rvActive.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActive.setAdapter(activeAdapter);
        completedAdapter = new TradeHistoryAdapter(requireContext(), new ArrayList<>(), this);
        rvCompleted.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCompleted.setAdapter(completedAdapter);
        showActiveTab();
    }

    private void showActiveTab() {
        swipeActive.setVisibility(View.VISIBLE);
        swipeCompleted.setVisibility(View.GONE);
        tvEmptyActive.setVisibility(activeListings.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyCompleted.setVisibility(View.GONE);
    }

    private void showCompletedTab() {
        swipeActive.setVisibility(View.GONE);
        swipeCompleted.setVisibility(View.VISIBLE);
        tvEmptyCompleted.setVisibility(tradeRecords.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyActive.setVisibility(View.GONE);
    }

    private void loadActiveListings() {
        if (TextUtils.isEmpty(userId) || supabaseClient == null) {
            swipeActive.setRefreshing(false);
            Toast.makeText(getContext(), R.string.require_login_message, Toast.LENGTH_SHORT).show();
            return;
        }
        loadingActiveListings = true;
        updateGlobalLoadingIndicator();
        String endpoint = String.format(Locale.US,
            "/rest/v1/posts?select=id,title,description,image_url,status,category,listing_type,condition,location,updated_at&user_id=eq.%s&listing_type=not.is.null&listing_type=neq.community&category=not.ilike.community&order=updated_at.desc",
            userId);
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                parseActiveListings(data);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load active listings: " + error);
                Toast.makeText(getContext(), R.string.messages_error_generic, Toast.LENGTH_SHORT).show();
                swipeActive.setRefreshing(false);
                loadingActiveListings = false;
                updateGlobalLoadingIndicator();
            }
        });
    }

    private void parseActiveListings(Object data) {
        List<ActiveListingsAdapter.ActiveListing> parsed = new ArrayList<>();
        if (data != null) {
            try {
                JSONArray array = new JSONArray(data.toString());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject post = array.getJSONObject(i);
                    String status = post.optString("status", "available");
                    if (!isActiveStatus(status)) {
                        continue;
                    }
                    String id = post.optString("id");
                    String title = post.optString("title", getString(R.string.app_name));
                    String description = post.optString("description", "");
                    String imageUrl = normalizeImageUrl(post.optString("image_url", ""));
                    String location = post.optString("location", null);
                    String rawCategory = post.optString("category", null);
                    if (!TextUtils.isEmpty(rawCategory) && "community".equalsIgnoreCase(rawCategory)) {
                        continue;
                    }
                    String listingType = post.optString("listing_type", "").trim();
                    if ("community".equalsIgnoreCase(listingType)) {
                        continue;
                    }
                    if (TextUtils.isEmpty(listingType)) {
                        listingType = "swap";
                    }
                    String condition = post.optString("condition", "used");
                    long updatedAt = parseTimestamp(post.optString("updated_at"));
                    parsed.add(new ActiveListingsAdapter.ActiveListing(
                            id,
                            title,
                            description,
                            imageUrl,
                            location,
                            rawCategory,
                            mapCategoryLabel(rawCategory),
                            listingType,
                            condition,
                            status,
                            mapStatusLabel(status),
                            updatedAt));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse active listings", e);
            }
        }
        activeListings.clear();
        activeListings.addAll(parsed);
        activeAdapter.replaceData(activeListings);
        swipeActive.setRefreshing(false);
        loadingActiveListings = false;
        updateGlobalLoadingIndicator();
        updateActiveEmptyState();
        maybeScrollToFocusActive();
    }

    private void updateActiveEmptyState() {
        if (toggleGroup.getCheckedButtonId() == R.id.btnToggleActive) {
            tvEmptyActive.setVisibility(activeListings.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void maybeScrollToFocusActive() {
        if (TextUtils.isEmpty(focusListingId)) {
            return;
        }
        for (int i = 0; i < activeListings.size(); i++) {
            if (focusListingId.equals(activeListings.get(i).getId())) {
                rvActive.scrollToPosition(i);
                focusListingId = null;
                break;
            }
        }
    }

    private boolean isActiveStatus(@Nullable String status) {
        if (TextUtils.isEmpty(status)) {
            return true;
        }
        String normalized = status.toLowerCase(Locale.US);
        return normalized.equals("available") || normalized.equals("pending");
    }

    private boolean isCompletedStatus(@NonNull String normalizedStatus) {
        switch (normalizedStatus) {
            case "completed":
            case "swapped":
            case "donated":
                return true;
            default:
                return false;
        }
    }

    private String mapCategoryLabel(@Nullable String rawCategory) {
        if (TextUtils.isEmpty(rawCategory)) {
            return getString(R.string.category_other);
        }
        switch (rawCategory.toLowerCase(Locale.US)) {
            case "electronics":
                return getString(R.string.category_electronics);
            case "clothing":
                return getString(R.string.category_clothing);
            case "books":
                return getString(R.string.category_books);
            case "furniture":
                return getString(R.string.category_furniture);
            case "donation":
                return getString(R.string.category_donation);
            case "swap":
                return getString(R.string.category_swap);
            default:
                return rawCategory.length() > 1
                        ? Character.toUpperCase(rawCategory.charAt(0)) + rawCategory.substring(1)
                        : rawCategory.toUpperCase(Locale.US);
        }
    }

    private String normalizeImageUrl(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            String trimmed = part != null ? part.trim() : null;
            if (!TextUtils.isEmpty(trimmed) && !"null".equalsIgnoreCase(trimmed)) {
                return trimmed;
            }
        }
        return null;
    }

    private String mapStatusLabel(@Nullable String rawStatus) {
        if (TextUtils.isEmpty(rawStatus)) {
            return getString(R.string.my_listings_status_available);
        }
        switch (rawStatus.toLowerCase(Locale.US)) {
            case "available":
                return getString(R.string.my_listings_status_available);
            case "pending":
                return getString(R.string.my_listings_status_pending);
            case "completed":
                return getString(R.string.my_listings_status_completed);
            case "swapped":
                return getString(R.string.my_listings_status_completed);
            case "donated":
                return getString(R.string.my_listings_status_completed);
            default:
                return rawStatus.length() > 1
                        ? Character.toUpperCase(rawStatus.charAt(0)) + rawStatus.substring(1)
                        : rawStatus.toUpperCase(Locale.US);
        }
    }

    private void loadCompletedTrades() {
        if (TextUtils.isEmpty(userId) || supabaseClient == null) {
            swipeCompleted.setRefreshing(false);
            Toast.makeText(getContext(), R.string.require_login_message, Toast.LENGTH_SHORT).show();
            return;
        }
        pendingTradeRecords.clear();
        pendingProfileLookups.clear();
        pendingTradeLoads = 3;
        updateGlobalLoadingIndicator();
        fetchSwaps();
        fetchDonations();
        fetchCompletedPosts();
    }

    private void fetchSwaps() {
        String orClause = String.format(Locale.US, "(user1_id.eq.%s,user2_id.eq.%s)", userId, userId);
        StringBuilder select = new StringBuilder("id,status,created_at,completed_at,user1_id,user2_id");
        if (TradeFeatureCompat.isProofPhotoSupported()) {
            select.append(",proof_photo_url");
        }
        select.append(",post1:posts!swaps_post1_id_fkey(id,title,image_url,user_id),post2:posts!swaps_post2_id_fkey(id,title,image_url,user_id)");

        String endpoint = String.format(Locale.US,
                "/rest/v1/swaps?select=%s&or=%s&order=created_at.desc",
                select.toString(),
                Uri.encode(orClause));
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                parseSwaps(data);
                onTradesPortionLoaded();
            }

            @Override
            public void onError(String error) {
                if (TradeFeatureCompat.isProofPhotoSupported() && TradeFeatureCompat.isProofPhotoError(error)) {
                    TradeFeatureCompat.disableProofPhoto();
                    fetchSwaps();
                    return;
                }
                Log.e(TAG, "Failed to load swaps: " + error);
                onTradesPortionLoaded();
            }
        });
    }

    private void fetchDonations() {
        StringBuilder select = new StringBuilder("id,status,created_at,completed_at,receiver_name,pickup_location,donor_id,receiver_id");
        if (TradeFeatureCompat.isProofPhotoSupported()) {
            select.append(",proof_photo_url");
        }
        select.append(",post:posts!donations_post_id_fkey(id,title,image_url,user_id)");
        String orClause = String.format(Locale.US, "(donor_id.eq.%s,receiver_id.eq.%s)", userId, userId);
        String endpoint = String.format(Locale.US,
                "/rest/v1/donations?select=%s&or=%s&order=created_at.desc",
                select.toString(),
                Uri.encode(orClause));
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                parseDonations(data);
                onTradesPortionLoaded();
            }

            @Override
            public void onError(String error) {
                if (TradeFeatureCompat.isProofPhotoSupported() && TradeFeatureCompat.isProofPhotoError(error)) {
                    TradeFeatureCompat.disableProofPhoto();
                    fetchDonations();
                    return;
                }
                Log.e(TAG, "Failed to load donations: " + error);
                onTradesPortionLoaded();
            }
        });
    }

    private void fetchCompletedPosts() {
        String endpoint = String.format(Locale.US,
            "/rest/v1/posts?select=id,status,created_at,title,image_url,user_id&user_id=eq.%s&status=in.(swapped,donated,completed)&listing_type=not.is.null&listing_type=neq.community&category=not.ilike.community&order=created_at.desc",
                userId);
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                parseCompletedPosts(data);
                onTradesPortionLoaded();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load completed posts: " + error);
                onTradesPortionLoaded();
            }
        });
    }

    private void parseSwaps(Object data) {
        if (data == null) {
            return;
        }
        try {
            JSONArray swaps = new JSONArray(data.toString());
            for (int i = 0; i < swaps.length(); i++) {
                JSONObject swap = swaps.getJSONObject(i);
                String id = swap.optString("id");
                TradeRecord record = new TradeRecord(id, TradeRecord.TradeType.SWAP, parseTimestamp(swap.optString("created_at")));
                record.setStatus(swap.optString("status", "pending"));
                record.setCompletedAtEpochMs(parseOptionalTimestamp(swap.optString("completed_at")));
                record.setProofPhotoUrl(swap.optString("proof_photo_url", null));
                record.setProofUploadSupported(TradeFeatureCompat.isProofPhotoSupported());

                String user1 = swap.optString("user1_id");
                String user2 = swap.optString("user2_id");
                boolean currentIsUser1 = userId != null && userId.equals(user1);

                JSONObject post1 = swap.optJSONObject("post1");
                JSONObject post2 = swap.optJSONObject("post2");
                if (currentIsUser1) {
                    record.setPrimaryItem(buildTradeItem(post1));
                    record.setSecondaryItem(buildTradeItem(post2));
                    record.setCounterpartyId(user2);
                } else {
                    record.setPrimaryItem(buildTradeItem(post2));
                    record.setSecondaryItem(buildTradeItem(post1));
                    record.setCounterpartyId(user1);
                }

                if (!TextUtils.isEmpty(record.getCounterpartyId()) && !userNameCache.containsKey(record.getCounterpartyId())) {
                    pendingProfileLookups.add(record.getCounterpartyId());
                }
                pendingTradeRecords.add(record);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse swaps", e);
        }
    }

    private void parseDonations(Object data) {
        if (data == null) {
            return;
        }
        try {
            JSONArray donations = new JSONArray(data.toString());
            for (int i = 0; i < donations.length(); i++) {
                JSONObject donation = donations.getJSONObject(i);
                TradeRecord record = new TradeRecord(donation.optString("id"), TradeRecord.TradeType.DONATION, parseTimestamp(donation.optString("created_at")));
                record.setStatus(donation.optString("status", "pending"));
                record.setCompletedAtEpochMs(parseOptionalTimestamp(donation.optString("completed_at")));
                record.setProofPhotoUrl(donation.optString("proof_photo_url", null));
                record.setProofUploadSupported(TradeFeatureCompat.isProofPhotoSupported());
                record.setReceiverName(donation.optString("receiver_name", null));
                record.setPickupLocation(donation.optString("pickup_location", null));
                record.setPrimaryItem(buildTradeItem(donation.optJSONObject("post")));

                String donorId = donation.optString("donor_id");
                String receiverId = donation.optString("receiver_id");
                boolean currentIsDonor = userId != null && userId.equals(donorId);
                String counterpartyId = currentIsDonor ? receiverId : donorId;
                record.setCounterpartyId(counterpartyId);
                if (!TextUtils.isEmpty(counterpartyId) && !userNameCache.containsKey(counterpartyId)) {
                    pendingProfileLookups.add(counterpartyId);
                }

                pendingTradeRecords.add(record);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse donations", e);
        }
    }

    private void parseCompletedPosts(Object data) {
        if (data == null) {
            return;
        }
        try {
            JSONArray posts = new JSONArray(data.toString());
            for (int i = 0; i < posts.length(); i++) {
                JSONObject post = posts.getJSONObject(i);
                String status = post.optString("status", "swapped");
                TradeRecord.TradeType type = "donated".equalsIgnoreCase(status)
                        ? TradeRecord.TradeType.DONATION
                        : TradeRecord.TradeType.SWAP;
                TradeRecord record = new TradeRecord(post.optString("id"), type, parseTimestamp(post.optString("created_at")));
                record.setStatus(status);
                record.setPrimaryItem(buildTradeItem(post));
                record.setProofUploadSupported(false);
                pendingTradeRecords.add(record);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse completed posts", e);
        }
    }

    private TradeRecord.TradeItem buildTradeItem(@Nullable JSONObject post) {
        if (post == null) {
            return null;
        }
        return new TradeRecord.TradeItem(
                post.optString("id"),
                post.optString("title", getString(R.string.app_name)),
                post.optString("image_url", null),
                post.optString("user_id", null));
    }

    private void onTradesPortionLoaded() {
        pendingTradeLoads--;
        if (pendingTradeLoads > 0) {
            return;
        }
        if (!pendingProfileLookups.isEmpty()) {
            fetchProfileNames();
        } else {
            publishTradeRecords();
        }
    }

    private void fetchProfileNames() {
        List<String> ids = new ArrayList<>(pendingProfileLookups);
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            params.append("\"").append(ids.get(i)).append("\"");
            if (i < ids.size() - 1) {
                params.append(",");
            }
        }
        String encoded = Uri.encode("(" + params + ")");
        String endpoint = "/rest/v1/profiles?select=id,name&id=in." + encoded;
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject profile = array.getJSONObject(i);
                        String id = profile.optString("id");
                        String name = profile.optString("name");
                        if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(name)) {
                            userNameCache.put(id, name);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse profile names", e);
                }
                publishTradeRecords();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to fetch profile names: " + error);
                publishTradeRecords();
            }
        });
    }

    private void publishTradeRecords() {
        for (TradeRecord record : pendingTradeRecords) {
            if (record.getCounterpartyId() != null) {
                record.setCounterpartyName(userNameCache.get(record.getCounterpartyId()));
            }
        }
        Collections.sort(pendingTradeRecords, (first, second) -> Long.compare(second.getCreatedAtEpochMs(), first.getCreatedAtEpochMs()));
        tradeRecords.clear();
        tradeRecords.addAll(pendingTradeRecords);
        completedAdapter.replaceData(tradeRecords);
        swipeCompleted.setRefreshing(false);
        updateGlobalLoadingIndicator();
        updateCompletedEmptyState();
        maybeScrollToFocusCompleted();
    }

    private void updateCompletedEmptyState() {
        if (toggleGroup.getCheckedButtonId() == R.id.btnToggleCompleted) {
            tvEmptyCompleted.setVisibility(tradeRecords.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void maybeScrollToFocusCompleted() {
        if (TextUtils.isEmpty(focusListingId)) {
            return;
        }
        for (int i = 0; i < tradeRecords.size(); i++) {
            TradeRecord.TradeItem item = tradeRecords.get(i).getPrimaryItem();
            if (item != null && focusListingId.equals(item.getPostId())) {
                rvCompleted.scrollToPosition(i);
                focusListingId = null;
                break;
            }
        }
    }

    private void updateGlobalLoadingIndicator() {
        boolean loading = loadingActiveListings || pendingTradeLoads > 0 || uploadingProof;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (!loading) {
            swipeActive.setRefreshing(false);
            swipeCompleted.setRefreshing(false);
        }
    }

    private long parseTimestamp(@Nullable String isoValue) {
        if (TextUtils.isEmpty(isoValue)) {
            return System.currentTimeMillis();
        }
        try {
            return Instant.parse(isoValue).toEpochMilli();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    @Nullable
    private Long parseOptionalTimestamp(@Nullable String isoValue) {
        if (TextUtils.isEmpty(isoValue) || "null".equalsIgnoreCase(isoValue)) {
            return null;
        }
        try {
            return Instant.parse(isoValue).toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onConfirmTrade(@NonNull TradeRecord record) {
        if (supabaseClient == null) {
            return;
        }
        uploadingProof = true;
        updateGlobalLoadingIndicator();
        JsonObject payload = new JsonObject();
        payload.addProperty("status", "completed");
        payload.addProperty("completed_at", nowIsoUtc());
        String table = record.getType() == TradeRecord.TradeType.SWAP ? "swaps" : "donations";
        supabaseClient.update(table, record.getId(), payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                uploadingProof = false;
                Toast.makeText(getContext(), R.string.trade_history_confirm_success, Toast.LENGTH_SHORT).show();
                updateImpactForInvolvedUsers(record);
                loadCompletedTrades();
            }

            @Override
            public void onError(String error) {
                uploadingProof = false;
                updateGlobalLoadingIndicator();
                Toast.makeText(getContext(), R.string.trade_history_confirm_error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to update trade: " + error);
            }
        });
    }

    @Override
    public void onUploadProof(@NonNull TradeRecord record) {
        if (!record.canUploadProof()) {
            Toast.makeText(getContext(), R.string.trade_history_proof_locked, Toast.LENGTH_SHORT).show();
            return;
        }
        recordAwaitingProof = record;
        proofPickerLauncher.launch("image/*");
    }

    @Override
    public void onTradeClicked(@NonNull TradeRecord record) {
        showTradeDetailsDialog(record);
    }

    private void beginProofUpload(@NonNull Uri uri) {
        if (recordAwaitingProof == null || supabaseClient == null || TextUtils.isEmpty(userId)) {
            recordAwaitingProof = null;
            return;
        }
        uploadingProof = true;
        updateGlobalLoadingIndicator();
        TradeProofUploader.upload(requireContext(), supabaseClient, userId, recordAwaitingProof.getId(), uri,
                new TradeProofUploader.Callback() {
                    @Override
                    public void onUploadSuccess(@NonNull String publicUrl) {
                        persistProofUrl(publicUrl);
                    }

                    @Override
                    public void onUploadError(@NonNull String message) {
                        uploadingProof = false;
                        updateGlobalLoadingIndicator();
                        recordAwaitingProof = null;
                        Toast.makeText(getContext(), R.string.trade_history_proof_upload_error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Proof upload failed: " + message);
                    }
                });
    }

    private void persistProofUrl(@NonNull String photoUrl) {
        if (recordAwaitingProof == null) {
            uploadingProof = false;
            updateGlobalLoadingIndicator();
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("proof_photo_url", photoUrl);
        String table = recordAwaitingProof.getType() == TradeRecord.TradeType.SWAP ? "swaps" : "donations";
        supabaseClient.update(table, recordAwaitingProof.getId(), payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                uploadingProof = false;
                recordAwaitingProof = null;
                Toast.makeText(getContext(), R.string.trade_history_proof_upload_success, Toast.LENGTH_SHORT).show();
                loadCompletedTrades();
            }

            @Override
            public void onError(String error) {
                uploadingProof = false;
                recordAwaitingProof = null;
                updateGlobalLoadingIndicator();
                Toast.makeText(getContext(), R.string.trade_history_proof_upload_error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to save proof url: " + error);
            }
        });
    }

    private void showTradeDetailsDialog(@NonNull TradeRecord record) {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault());
        String createdLabel = fmt.format(new java.util.Date(record.getCreatedAtEpochMs()));
        String completedLabel = record.getCompletedAtEpochMs() != null
                ? fmt.format(new java.util.Date(record.getCompletedAtEpochMs()))
                : getString(R.string.trade_history_pending_label);

        StringBuilder message = new StringBuilder();
        if (record.getPrimaryItem() != null) {
            message.append(getString(R.string.listing_preview_details_header)).append(": ")
                    .append(record.getPrimaryItem().getTitle()).append("\n");
        }
        if (record.getType() == TradeRecord.TradeType.SWAP && record.getSecondaryItem() != null) {
            message.append(getString(R.string.trade_history_swap_with, record.getCounterpartyName() != null ? record.getCounterpartyName() : getString(R.string.trade_history_partner_unknown)))
                    .append("\n");
        } else if (record.getType() == TradeRecord.TradeType.DONATION) {
            String donationPartner = !TextUtils.isEmpty(record.getCounterpartyName())
                    ? record.getCounterpartyName()
                    : (!TextUtils.isEmpty(record.getReceiverName())
                        ? record.getReceiverName()
                        : getString(R.string.trade_history_receiver_placeholder));
            message.append(getString(R.string.trade_history_donation_with, donationPartner)).append("\n");
            if (!TextUtils.isEmpty(record.getPickupLocation())) {
                message.append(getString(R.string.pickup_location_label, record.getPickupLocation())).append("\n");
            }
        }

        message.append("Created: ").append(createdLabel).append("\n");
        message.append("Completed: ").append(completedLabel).append("\n");

        boolean hasProof = !TextUtils.isEmpty(record.getProofPhotoUrl());
        if (hasProof) {
            message.append(getString(R.string.trade_history_proof_uploaded)).append("\n");
        } else if (record.canUploadProof()) {
            message.append(getString(R.string.trade_history_proof_missing)).append("\n");
        } else {
            message.append(getString(R.string.trade_history_proof_locked)).append("\n");
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(record.getType() == TradeRecord.TradeType.SWAP ? R.string.trade_history_swap_with : R.string.trade_history_donation_with)
                .setMessage(message.toString())
                .setPositiveButton(android.R.string.ok, null);

        if (hasProof) {
            builder.setNeutralButton("View proof", (d, which) -> {
                try {
                    startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(record.getProofPhotoUrl())));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Unable to open proof", Toast.LENGTH_SHORT).show();
                }
            });
        }

        builder.show();
    }

    private void updateImpactForInvolvedUsers(@NonNull TradeRecord record) {
        String impactStatus = mapImpactStatus(record);
        if (TextUtils.isEmpty(impactStatus) || supabaseClient == null) {
            return;
        }
        List<String> targetUsers = new ArrayList<>();
        if (!TextUtils.isEmpty(userId)) {
            targetUsers.add(userId);
        }
        if (!TextUtils.isEmpty(record.getCounterpartyId()) && !record.getCounterpartyId().equals(userId)) {
            targetUsers.add(record.getCounterpartyId());
        }
        for (String target : targetUsers) {
            updateImpactForUser(target, impactStatus);
        }
    }

    @Nullable
    private String mapImpactStatus(@NonNull TradeRecord record) {
        if (record.getType() == TradeRecord.TradeType.SWAP) {
            return "swapped";
        }
        if (record.getType() == TradeRecord.TradeType.DONATION) {
            return "donated";
        }
        return null;
    }

    private void updateImpactForUser(String targetUserId, String status) {
        if (TextUtils.isEmpty(targetUserId) || supabaseClient == null) return;
        String profileQuery = "/rest/v1/profiles?id=eq." + targetUserId + "&select=total_swaps,total_donations,total_purchases";
        supabaseClient.query(profileQuery, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() == 0) return;
                    JSONObject profile = array.getJSONObject(0);
                    int swaps = profile.optInt("total_swaps", 0);
                    int donations = profile.optInt("total_donations", 0);
                    int purchases = profile.optInt("total_purchases", 0);

                    if ("swapped".equals(status)) {
                        swaps++;
                    } else if ("donated".equals(status)) {
                        donations++;
                    }

                    int newScore = (swaps * 2) + (donations * 3) + (purchases * 1);
                    String newLevel = "Beginner EcoSaver";
                        String newIcon = "🌱";
                    if (newScore >= 100) {
                        newLevel = "Planet Pioneer";
                            newIcon = "🌞";
                    } else if (newScore >= 50) {
                        newLevel = "Eco Guardian";
                            newIcon = "🦋";
                    } else if (newScore >= 25) {
                        newLevel = "Sustainable Hero";
                            newIcon = "🌍";
                    } else if (newScore >= 10) {
                        newLevel = "Rising Recycler";
                            newIcon = "♻️";
                    }

                    JsonObject updatePayload = new JsonObject();
                    updatePayload.addProperty("total_swaps", swaps);
                    updatePayload.addProperty("total_donations", donations);
                    updatePayload.addProperty("impact_score", newScore);
                    updatePayload.addProperty("eco_level", newLevel);
                    updatePayload.addProperty("eco_icon", newIcon);

                    supabaseClient.update("profiles", targetUserId, updatePayload, new SupabaseClient.OnDatabaseCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            updateEcoSavings(targetUserId, status);
                        }

                        @Override
                        public void onError(String error) {
                            // best-effort
                        }
                    });
                } catch (JSONException ignored) {
                    // best-effort
                }
            }

            @Override
            public void onError(String error) {
                // best-effort
            }
        });
    }

    private void updateEcoSavings(String targetUserId, String status) {
        if (supabaseClient == null || TextUtils.isEmpty(targetUserId)) return;
        String savingsQuery = "/rest/v1/eco_savings?user_id=eq." + targetUserId + "&select=*";
        supabaseClient.query(savingsQuery, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() == 0) return;
                    JSONObject savings = array.getJSONObject(0);
                    double co2 = savings.optDouble("co2_saved", 0);
                    double water = savings.optDouble("water_saved", 0);
                    double waste = savings.optDouble("waste_diverted", 0);
                    double energy = savings.optDouble("energy_saved", 0);
                    int itemsSwapped = savings.optInt("items_swapped", 0);
                    int itemsDonated = savings.optInt("items_donated", 0);

                    if ("swapped".equals(status)) {
                        co2 += 5;
                        water += 100;
                        waste += 2;
                        energy += 10;
                        itemsSwapped++;
                    } else if ("donated".equals(status)) {
                        co2 += 7;
                        water += 150;
                        waste += 3;
                        energy += 15;
                        itemsDonated++;
                    }

                    JsonObject updatePayload = new JsonObject();
                    updatePayload.addProperty("co2_saved", co2);
                    updatePayload.addProperty("water_saved", water);
                    updatePayload.addProperty("waste_diverted", waste);
                    updatePayload.addProperty("energy_saved", energy);
                    updatePayload.addProperty("items_swapped", itemsSwapped);
                    updatePayload.addProperty("items_donated", itemsDonated);

                    supabaseClient.update("eco_savings", savings.optString("id"), updatePayload, new SupabaseClient.OnDatabaseCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            // success
                        }

                        @Override
                        public void onError(String error) {
                            // best-effort
                        }
                    });
                } catch (JSONException ignored) {
                    // best-effort
                }
            }

            @Override
            public void onError(String error) {
                // best-effort
            }
        });
    }

    @Override
    public void onListingClicked(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        openPreview(listing);
    }

    @Override
    public void onEditClicked(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        openEdit(listing.getId());
    }

    @Override
    public void onMarkCompleteClicked(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        confirmMarkComplete(listing);
    }

    @Override
    public void onDeleteClicked(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        confirmDeleteListing(listing);
    }

    private void openPreview(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        MarketplaceFragment.MarketplaceItem item = toMarketplaceItem(listing);
        ListingPreviewBottomSheet bottomSheet = ListingPreviewBottomSheet.newInstance(item);
        bottomSheet.show(getParentFragmentManager(), "ListingPreview");
    }

    private void openEdit(@NonNull String listingId) {
        CreateListingFragment editFragment = CreateListingFragment.newInstanceForEdit(listingId);
        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_down)
                .replace(R.id.fragmentContainer, editFragment)
                .addToBackStack("edit_listing_from_profile")
                .commit();
    }

    private void confirmMarkComplete(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.chat_mark_complete_title)
                .setMessage(R.string.chat_mark_complete_message)
                .setPositiveButton(R.string.chat_mark_complete_title, (d, w) -> markListingComplete(listing))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void markListingComplete(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        if (supabaseClient == null || TextUtils.isEmpty(listing.getId())) {
            return;
        }
        String targetStatus = "donation".equalsIgnoreCase(listing.getListingType()) ? "donated" : "swapped";
        JsonObject payload = new JsonObject();
        payload.addProperty("status", targetStatus);
        payload.addProperty("completed_at", nowIsoUtc());
        progressBar.setVisibility(View.VISIBLE);
        supabaseClient.update("posts", listing.getId(), payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                Toast.makeText(getContext(), R.string.chat_mark_complete_success, Toast.LENGTH_SHORT).show();
                loadActiveListings();
                loadCompletedTrades();
                toggleGroup.check(R.id.btnToggleCompleted);
                showCompletedTab();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), R.string.chat_mark_complete_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteListing(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_listing_title)
                .setMessage(R.string.delete_listing_message)
                .setPositiveButton(R.string.delete_listing_title, (d, w) -> deleteListing(listing))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteListing(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        if (supabaseClient == null || TextUtils.isEmpty(listing.getId())) {
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        supabaseClient.delete("posts", listing.getId(), new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                Toast.makeText(getContext(), R.string.listing_delete_success, Toast.LENGTH_SHORT).show();
                loadActiveListings();
                loadCompletedTrades();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), R.string.listing_delete_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private MarketplaceFragment.MarketplaceItem toMarketplaceItem(@NonNull ActiveListingsAdapter.ActiveListing listing) {
        MarketplaceFragment.MarketplaceItem item = new MarketplaceFragment.MarketplaceItem();
        item.setId(listing.getId());
        item.setTitle(listing.getTitle());
        item.setDescription(listing.getDescription());
        item.setImageUrl(listing.getImageUrl());
        item.setLocation(listing.getLocation());
        item.setPostedBy("You");
        item.setOwnerId(userId);
        item.setRawCategory(listing.getRawCategory());
        item.setListingType(listing.getListingType());
        item.setDisplayCategory(listing.getDisplayCategory());
        item.setRawCondition(listing.getCondition());
        item.setDisplayCondition(listing.getCondition());
        return item;
    }

    private String nowIsoUtc() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(System.currentTimeMillis()));
    }
}
