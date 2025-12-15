package com.example.ecoswap.dashboard;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.chat.ChatFragment;
import com.example.ecoswap.utils.ConversationMetadataStore;
import com.example.ecoswap.utils.LocationUtils;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ListingPreviewBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_LISTING_ID = "arg_listing_id";
    private static final String ARG_OWNER_ID = "arg_owner_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_IMAGE_URL = "arg_image_url";
    private static final String ARG_LOCATION = "arg_location";
    private static final String ARG_POSTED_BY = "arg_posted_by";
    private static final String ARG_CATEGORY = "arg_category";
    private static final String ARG_CATEGORY_LABEL = "arg_category_label";
    private static final String ARG_LISTING_TYPE = "arg_listing_type";
    private static final String ARG_CONDITION_LABEL = "arg_condition_label";
    private static final String ARG_DISTANCE_KM = "arg_distance_km";
    private static final String ARG_HAS_DISTANCE = "arg_has_distance";
    private static final String ARG_NEAR_USER = "arg_near_user";
    private static final String ARG_OWNER_AVATAR = "arg_owner_avatar";

    private String listingId;
    private String ownerId;
    private String title;
    private String description;
    private String imageUrl;
    private String location;
    private String postedBy;
    private String rawCategory;
    private String listingType;
    private String displayCategory;
    private String displayCondition;
    private Double distanceKm;
    private boolean isNearUser;
    private String ownerAvatarUrl;
    private ConversationMetadataStore conversationMetadataStore;
    private SessionManager sessionManager;
    private SupabaseClient supabaseClient;
    private String currentUserId;

    public static ListingPreviewBottomSheet newInstance(@NonNull MarketplaceFragment.MarketplaceItem item) {
        ListingPreviewBottomSheet sheet = new ListingPreviewBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, item.getId());
        args.putString(ARG_OWNER_ID, item.getOwnerId());
        args.putString(ARG_TITLE, item.getTitle());
        args.putString(ARG_DESCRIPTION, item.getDescription());
        args.putString(ARG_IMAGE_URL, item.getImageUrl());
        args.putString(ARG_LOCATION, item.getLocation());
        args.putString(ARG_POSTED_BY, item.getPostedBy());
        args.putString(ARG_CATEGORY, item.getRawCategory());
        args.putString(ARG_LISTING_TYPE, item.getListingType());
        args.putString(ARG_CATEGORY_LABEL, item.getDisplayCategory());
        args.putString(ARG_CONDITION_LABEL, item.getDisplayCondition());
        args.putString(ARG_OWNER_AVATAR, item.getOwnerProfileImageUrl());
        if (item.getDistanceKm() != null) {
            args.putBoolean(ARG_HAS_DISTANCE, true);
            args.putDouble(ARG_DISTANCE_KM, item.getDistanceKm());
        }
        args.putBoolean(ARG_NEAR_USER, item.isNearUser());
        sheet.setArguments(args);
        return sheet;
    }

    private String buildInterestedMessage() {
        String resolvedTitle = !TextUtils.isEmpty(title) ? title : "this item";
        return "Hi, I'm interested in " + resolvedTitle + ". Is it still available?";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_listing_preview, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            listingId = args.getString(ARG_LISTING_ID);
            ownerId = args.getString(ARG_OWNER_ID);
            title = args.getString(ARG_TITLE);
            description = args.getString(ARG_DESCRIPTION);
            imageUrl = args.getString(ARG_IMAGE_URL);
            location = args.getString(ARG_LOCATION);
            postedBy = args.getString(ARG_POSTED_BY);
            rawCategory = args.getString(ARG_CATEGORY);
            listingType = args.getString(ARG_LISTING_TYPE);
            displayCategory = args.getString(ARG_CATEGORY_LABEL);
            displayCondition = args.getString(ARG_CONDITION_LABEL);
            ownerAvatarUrl = args.getString(ARG_OWNER_AVATAR);
            if (args.getBoolean(ARG_HAS_DISTANCE, false)) {
                distanceKm = args.getDouble(ARG_DISTANCE_KM);
            }
            isNearUser = args.getBoolean(ARG_NEAR_USER, false);
        }
        if (getContext() != null) {
            conversationMetadataStore = new ConversationMetadataStore(requireContext());
            sessionManager = SessionManager.getInstance(requireContext());
            supabaseClient = SupabaseClient.getInstance(requireContext());
            currentUserId = sessionManager != null ? sessionManager.getUserId() : null;
            persistListingContext();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewPager2 vpImageSlider = view.findViewById(R.id.vpImageSlider);
        TabLayout tabLayoutIndicator = view.findViewById(R.id.tabLayoutIndicator);
        TextView tvPreviewTitle = view.findViewById(R.id.tvPreviewTitle);
        TextView tvAvatar = view.findViewById(R.id.tvAvatar);
        ImageView ivAvatar = view.findViewById(R.id.ivAvatar);
        TextView tvPreviewPostedBy = view.findViewById(R.id.tvPreviewPostedBy);
        TextView tvPreviewLocation = view.findViewById(R.id.tvPreviewLocation);
        TextView tvPreviewDistance = view.findViewById(R.id.tvPreviewDistance);
        TextView tvPreviewDescription = view.findViewById(R.id.tvPreviewDescription);
        TextView tvCategoryLabel = view.findViewById(R.id.tvCategoryLabel);
        TextView tvListingTypeBadge = view.findViewById(R.id.tvListingTypeBadge);
        TextView tvConditionBadge = view.findViewById(R.id.tvConditionBadge);
        ExtendedFloatingActionButton btnStartChat = view.findViewById(R.id.btnStartChat);
        View layoutProfile = view.findViewById(R.id.layoutProfile);
        
        LinearLayout layoutOwnerActions = view.findViewById(R.id.layoutOwnerActions);
        MaterialButton btnMarkSuccessful = view.findViewById(R.id.btnMarkSuccessful);
        MaterialButton btnEditListing = view.findViewById(R.id.btnEditListing);
        MaterialButton btnDeleteListing = view.findViewById(R.id.btnDeleteListing);

        String resolvedTitle = !TextUtils.isEmpty(title) ? title : getString(R.string.app_name);
        String resolvedPoster = !TextUtils.isEmpty(postedBy) ? postedBy : getString(R.string.app_name);
        String resolvedCategory = !TextUtils.isEmpty(displayCategory) ? displayCategory : getString(R.string.category_other);
        String resolvedCondition = !TextUtils.isEmpty(displayCondition) ? displayCondition : getString(R.string.condition_good);

        // Check ownership
        String currentUserId = sessionManager != null ? sessionManager.getUserId() : null;
        boolean isOwner = !TextUtils.isEmpty(currentUserId) && !TextUtils.isEmpty(ownerId) && currentUserId.equals(ownerId);

        if (isOwner) {
            btnStartChat.setVisibility(View.GONE);
            layoutOwnerActions.setVisibility(View.VISIBLE);
            
            btnMarkSuccessful.setOnClickListener(v -> showMarkSuccessfulDialog());
            btnEditListing.setOnClickListener(v -> showEditListing());
            btnDeleteListing.setOnClickListener(v -> showDeleteConfirmation());
        } else {
            btnStartChat.setVisibility(View.VISIBLE);
            layoutOwnerActions.setVisibility(View.GONE);
            
            btnStartChat.setOnClickListener(v -> {
                openChat();
                dismissAllowingStateLoss();
            });
        }

        tvPreviewTitle.setText(resolvedTitle);
        tvPreviewPostedBy.setText(resolvedPoster);
        tvPreviewLocation.setText(!TextUtils.isEmpty(location) ? location : getString(R.string.location_unknown));
        
        if (ivAvatar != null && !TextUtils.isEmpty(ownerAvatarUrl)) {
            ivAvatar.setVisibility(View.VISIBLE);
            if (tvAvatar != null) tvAvatar.setVisibility(View.GONE);
            Glide.with(this)
                .load(ownerAvatarUrl)
                .circleCrop()
                .placeholder(R.drawable.bg_circle_placeholder)
                .into(ivAvatar);
        } else if (tvAvatar != null) {
            tvAvatar.setVisibility(View.VISIBLE);
            if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);
            tvAvatar.setText(getInitials(resolvedPoster));
        }

        if (distanceKm != null) {
            tvPreviewDistance.setText(LocationUtils.formatDistanceLabel(distanceKm));
            tvPreviewDistance.setVisibility(View.VISIBLE);
        } else if (isNearUser) {
            tvPreviewDistance.setText(getString(R.string.listing_preview_nearby));
            tvPreviewDistance.setVisibility(View.VISIBLE);
        } else {
            tvPreviewDistance.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(description)) {
            tvPreviewDescription.setText(description);
        } else {
            tvPreviewDescription.setText(R.string.listing_preview_description_empty);
        }

        tvCategoryLabel.setText(resolvedCategory.toUpperCase());
        applyCategoryStyle(tvCategoryLabel, rawCategory);
        
        if ("donation".equalsIgnoreCase(listingType)) {
            tvListingTypeBadge.setText("Donation");
            tvListingTypeBadge.setBackgroundResource(R.drawable.bg_chip_green);
        } else {
            tvListingTypeBadge.setText("Swap");
            tvListingTypeBadge.setBackgroundResource(R.drawable.bg_chip_blue);
        }

        tvConditionBadge.setText(resolvedCondition);

        List<String> images = new ArrayList<>();
        if (!TextUtils.isEmpty(imageUrl)) {
            if (imageUrl.contains(",")) {
                String[] parts = imageUrl.split(",");
                for (String part : parts) {
                    if (!TextUtils.isEmpty(part.trim())) {
                        images.add(part.trim());
                    }
                }
            } else {
                images.add(imageUrl);
            }
        }

        if (images.isEmpty()) {
            // Add a placeholder or handle empty state
            // For now, maybe add a null to show placeholder in adapter?
            // Or just hide slider?
            // Let's show placeholder in adapter if list is empty?
            // Actually, adapter expects strings.
            // Let's add a dummy empty string if list is empty to show placeholder
            // But better to just handle it in adapter or here.
        }

        ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(requireContext(), images);
        vpImageSlider.setAdapter(sliderAdapter);

        if (images.size() > 1) {
            new TabLayoutMediator(tabLayoutIndicator, vpImageSlider, (tab, position) -> {
                // No text, just dots
            }).attach();
            tabLayoutIndicator.setVisibility(View.VISIBLE);
        } else {
            tabLayoutIndicator.setVisibility(View.GONE);
        }

        if (layoutProfile != null) {
            layoutProfile.setOnClickListener(v -> {
                UserProfileBottomSheet profileSheet = UserProfileBottomSheet.newInstance(ownerId);
                profileSheet.show(getParentFragmentManager(), "user_profile_sheet");
            });
        }
    }

    private void showMarkSuccessfulDialog() {
        boolean isDonation = "donation".equalsIgnoreCase(listingType);
        String actionLabel = isDonation ? "Mark as Donated" : "Mark as Swapped";
        String message = isDonation
                ? "Mark this donation as completed? We'll update your impact stats."
                : "Mark this swap as completed? We'll update your impact stats.";
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mark as Successful")
            .setMessage(message)
            .setPositiveButton(actionLabel, (dialog, which) -> markAsComplete())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void markAsComplete() {
        if (supabaseClient == null || TextUtils.isEmpty(listingId)) return;

        boolean isDonation = "donation".equalsIgnoreCase(listingType);
        String targetStatus = isDonation ? "donated" : "swapped";

        JsonObject payload = new JsonObject();
        payload.addProperty("status", targetStatus);

            supabaseClient.update("posts", listingId, payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                persistTradeRecord(targetStatus);
                Toast.makeText(getContext(), "Listing marked as " + targetStatus, Toast.LENGTH_SHORT).show();
                Bundle result = new Bundle();
                result.putBoolean("refresh", true);
                result.putBoolean("openCompleted", true);
                getParentFragmentManager().setFragmentResult("listing_updates", result);
                dismissAllowingStateLoss();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to update listing: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void persistTradeRecord(String status) {
        if (supabaseClient == null || TextUtils.isEmpty(listingId)) {
            updateImpactForInvolvedUsers(status);
            return;
        }
        boolean isDonation = "donated".equals(status);
        if (isDonation) {
            createDonationRecord(status);
        } else {
            createSwapRecord(status);
        }
    }

    private void createSwapRecord(String status) {
        if (TextUtils.isEmpty(ownerId) || TextUtils.isEmpty(currentUserId)) {
            updateImpactForInvolvedUsers(status);
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("post1_id", listingId);
        payload.addProperty("user1_id", ownerId);
        payload.addProperty("user2_id", currentUserId);
        payload.addProperty("status", "completed");
        payload.addProperty("completed_at", nowIsoUtc());
        supabaseClient.insert("swaps", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                updateImpactForInvolvedUsers(status);
            }

            @Override
            public void onError(String error) {
                updateImpactForInvolvedUsers(status);
            }
        });
    }

    private void createDonationRecord(String status) {
        if (TextUtils.isEmpty(ownerId) || TextUtils.isEmpty(currentUserId)) {
            updateImpactForInvolvedUsers(status);
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("post_id", listingId);
        payload.addProperty("donor_id", ownerId);
        payload.addProperty("receiver_name", "Peer");
        payload.addProperty("status", "completed");
        payload.addProperty("completed_at", nowIsoUtc());
        if (!TextUtils.isEmpty(currentUserId) && !currentUserId.equals(ownerId)) {
            payload.addProperty("receiver_id", currentUserId);
        }

        supabaseClient.insert("donations", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                updateImpactForInvolvedUsers(status);
            }

            @Override
            public void onError(String error) {
                updateImpactForInvolvedUsers(status);
            }
        });
    }

    private void updateImpactForInvolvedUsers(String status) {
        if (supabaseClient == null) return;
        List<String> targetUsers = new ArrayList<>();
        if (!TextUtils.isEmpty(ownerId)) {
            targetUsers.add(ownerId);
        }
        if (!TextUtils.isEmpty(currentUserId) && !currentUserId.equals(ownerId)) {
            targetUsers.add(currentUserId);
        }
        for (String user : targetUsers) {
            updateImpactForUser(user, status);
        }
    }

    private void updateImpactForUser(String userId, String status) {
        if (TextUtils.isEmpty(userId)) return;

        String profileQuery = "/rest/v1/profiles?id=eq." + userId + "&select=total_swaps,total_donations,total_purchases";
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

                    supabaseClient.update("profiles", userId, updatePayload, new SupabaseClient.OnDatabaseCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            Log.d("ImpactUpdate", "Profile stats updated for user " + userId);
                            updateEcoSavings(userId, status);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("ImpactUpdate", "Failed to update profile for user " + userId + ": " + error);
                        }
                    });
                } catch (JSONException e) {
                    Log.e("ImpactUpdate", "Error parsing profile data for user " + userId, e);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ImpactUpdate", "Failed to fetch profile stats for user " + userId + ": " + error);
            }
        });
    }

    private void updateEcoSavings(String userId, String status) {
        String savingsQuery = "/rest/v1/eco_savings?user_id=eq." + userId + "&select=*";
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

                    supabaseClient.update("eco_savings", "user_id", userId, updatePayload, new SupabaseClient.OnDatabaseCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            Log.d("ImpactUpdate", "Eco savings updated for user " + userId);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("ImpactUpdate", "Failed to update eco savings for user " + userId + ": " + error);
                        }
                    });
                } catch (JSONException e) {
                    Log.e("ImpactUpdate", "Error parsing eco savings for user " + userId, e);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ImpactUpdate", "Failed to fetch eco savings for user " + userId + ": " + error);
            }
        });
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete this listing? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteListing())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteListing() {
        if (supabaseClient == null || TextUtils.isEmpty(listingId)) return;

            supabaseClient.delete("posts", listingId, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                Toast.makeText(getContext(), "Listing deleted", Toast.LENGTH_SHORT).show();
                Bundle result = new Bundle();
                result.putBoolean("refresh", true);
                getParentFragmentManager().setFragmentResult("listing_updates", result);
                dismissAllowingStateLoss();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to delete listing: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditListing() {
        CreateListingFragment editFragment = CreateListingFragment.newInstanceForEdit(listingId);
        getParentFragmentManager()
            .beginTransaction()
            .setCustomAnimations(R.anim.slide_in_up, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_down)
            .replace(R.id.fragmentContainer, editFragment)
            .addToBackStack("edit_listing")
            .commit();
        dismissAllowingStateLoss();
    }

    private void openChat() {
        if (getParentFragmentManager() == null || getActivity() == null) {
            return;
        }
        persistListingContext();
        ChatFragment fragment = ChatFragment.newInstance(ownerId, listingId, title, imageUrl);
        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_up,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out_down)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("chat_fragment")
                .commit();
    }

    private void applyCategoryStyle(@NonNull TextView textView, @Nullable String rawCategory) {
        if (getContext() == null) {
            return;
        }
        int textColor = R.color.primary_blue;
        String categoryKey = rawCategory != null ? rawCategory.toLowerCase() : "";
        switch (categoryKey) {
            case "electronics":
                textColor = R.color.primary_blue;
                break;
            case "clothing":
                textColor = R.color.error_red;
                break;
            case "books":
                textColor = R.color.warning_orange;
                break;
            case "furniture":
                textColor = R.color.success_green;
                break;
            default:
                textColor = R.color.primary_green;
                break;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        textView.setTextColor(ContextCompat.getColor(context, textColor));
    }

    private void persistListingContext() {
        if (conversationMetadataStore == null
                || TextUtils.isEmpty(ownerId)) {
            return;
        }
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(imageUrl) && TextUtils.isEmpty(listingId)) {
            return;
        }
        conversationMetadataStore.saveListingContext(ownerId, listingId, title, imageUrl);
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }

    private String nowIsoUtc() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(System.currentTimeMillis()));
    }
}
