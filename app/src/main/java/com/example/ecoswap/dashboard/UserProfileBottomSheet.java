package com.example.ecoswap.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.chat.ChatFragment;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class UserProfileBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_USER_ID = "arg_user_id";
    private String userId;
    private SupabaseClient supabaseClient;

    private TextView tvProfileAvatar;
    private ImageView imgProfileAvatar;
    private TextView tvProfileName;
    private TextView tvProfileLocation;
    private TextView tvProfileSwaps;
    private TextView tvProfileDonated;
    private TextView tvProfileBio;
    private RecyclerView rvProfileListings;
    private ProgressBar progressBarListings;
    private TextView tvNoListings;
    private View btnMessageUser;

    public static UserProfileBottomSheet newInstance(String userId) {
        UserProfileBottomSheet fragment = new UserProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
        supabaseClient = SupabaseClient.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvProfileAvatar = view.findViewById(R.id.tvProfileAvatar);
        imgProfileAvatar = view.findViewById(R.id.imgProfileAvatar);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileLocation = view.findViewById(R.id.tvProfileLocation);
        tvProfileSwaps = view.findViewById(R.id.tvProfileSwaps);
        tvProfileDonated = view.findViewById(R.id.tvProfileDonated);
        tvProfileBio = view.findViewById(R.id.tvProfileBio);
        rvProfileListings = view.findViewById(R.id.rvProfileListings);
        progressBarListings = view.findViewById(R.id.progressBarListings);
        tvNoListings = view.findViewById(R.id.tvNoListings);
        btnMessageUser = view.findViewById(R.id.btnMessageUser);

        rvProfileListings.setLayoutManager(new LinearLayoutManager(getContext()));

        loadUserProfile();
        loadUserListings();

        btnMessageUser.setOnClickListener(v -> {
            if (userId != null) {
                ChatFragment chatFragment = ChatFragment.newInstance(userId, null, null, null, null, null);
                getParentFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_down)
                    .replace(R.id.fragmentContainer, chatFragment)
                    .addToBackStack("chat_from_profile")
                    .commit();
                dismiss();
            }
        });
    }

    private void loadUserProfile() {
        String endpoint = "/rest/v1/profiles?id=eq." + userId + "&select=*";
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    JsonArray result = gson.fromJson(data.toString(), JsonArray.class);
                    if (result != null && result.size() > 0) {
                        JsonObject profile = result.get(0).getAsJsonObject();
                        
                        String name = profile.has("name") && !profile.get("name").isJsonNull() 
                                ? profile.get("name").getAsString() : "User";
                        tvProfileName.setText(name);
                        tvProfileAvatar.setText(getInitials(name));

                        String location = profile.has("location") && !profile.get("location").isJsonNull()
                                ? profile.get("location").getAsString() : "Unknown Location";
                        tvProfileLocation.setText(location);

                        // Load stats if available
                        if (profile.has("total_swaps")) {
                            tvProfileSwaps.setText(String.valueOf(profile.get("total_swaps").getAsInt()));
                        }
                        if (profile.has("total_donations")) {
                            tvProfileDonated.setText(String.valueOf(profile.get("total_donations").getAsInt()));
                        }

                        if (profile.has("bio") && !profile.get("bio").isJsonNull()) {
                            String bio = profile.get("bio").getAsString();
                            if (!bio.isEmpty()) {
                                tvProfileBio.setText(bio);
                                tvProfileBio.setVisibility(View.VISIBLE);
                            }
                        }
                        
                        // Check for profile_image_url first (used in ProfileFragment)
                        String avatarUrl = null;
                        if (profile.has("profile_image_url") && !profile.get("profile_image_url").isJsonNull()) {
                            avatarUrl = profile.get("profile_image_url").getAsString();
                        } else if (profile.has("avatar_url") && !profile.get("avatar_url").isJsonNull()) {
                            avatarUrl = profile.get("avatar_url").getAsString();
                        }

                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(requireContext())
                                .load(avatarUrl)
                                .centerCrop()
                                .placeholder(R.drawable.bg_circle_white)
                                .into(imgProfileAvatar);
                            imgProfileAvatar.setVisibility(View.VISIBLE);
                            tvProfileAvatar.setVisibility(View.GONE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                // Handle error
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load profile: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadUserListings() {
        progressBarListings.setVisibility(View.VISIBLE);
        // Query active listings for this user, including profile name and image
        String endpoint = "/rest/v1/posts?user_id=eq." + userId + "&status=eq.available&select=*,profiles(name,profile_image_url)&order=created_at.desc";
        
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                progressBarListings.setVisibility(View.GONE);
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    JsonArray result = gson.fromJson(data.toString(), JsonArray.class);
                    
                    if (result != null && result.size() > 0) {
                        List<MarketplaceFragment.MarketplaceItem> items = new ArrayList<>();
                        for (int i = 0; i < result.size(); i++) {
                            JsonObject post = result.get(i).getAsJsonObject();
                            // Map JsonObject to MarketplaceItem
                            String id = post.get("id").getAsString();
                            String title = post.get("title").getAsString();
                            String description = post.has("description") && !post.get("description").isJsonNull() ? post.get("description").getAsString() : "";
                            String imageUrl = post.has("image_url") && !post.get("image_url").isJsonNull() ? post.get("image_url").getAsString() : "";
                            String location = post.has("location") && !post.get("location").isJsonNull() ? post.get("location").getAsString() : "";
                            String category = post.has("category") && !post.get("category").isJsonNull() ? post.get("category").getAsString() : "Other";
                            String type = post.has("listing_type") && !post.get("listing_type").isJsonNull() ? post.get("listing_type").getAsString() : "swap";
                            
                            // Extract user name and image from joined profiles table
                            String postedBy = "User";
                            String ownerImage = null;
                            if (post.has("profiles") && !post.get("profiles").isJsonNull()) {
                                JsonObject profile = post.get("profiles").getAsJsonObject();
                                if (profile.has("name") && !profile.get("name").isJsonNull()) {
                                    postedBy = profile.get("name").getAsString();
                                }
                                if (profile.has("profile_image_url") && !profile.get("profile_image_url").isJsonNull()) {
                                    ownerImage = profile.get("profile_image_url").getAsString();
                                }
                            }

                            MarketplaceFragment.MarketplaceItem item = new MarketplaceFragment.MarketplaceItem();
                            item.setId(id);
                            item.setTitle(title);
                            item.setDescription(description);
                            item.setImageUrl(imageUrl);
                            item.setLocation(location);
                            item.setOwnerId(userId);
                            item.setPostedBy(postedBy);
                            item.setOwnerProfileImageUrl(ownerImage);
                            item.setRawCategory(category);
                            item.setListingType(type);
                            item.setRawCondition("good");
                            item.setDisplayCategory(category);
                            item.setDisplayCondition("Good");
                            items.add(item);
                        }
                        
                        // Setup adapter
                        MarketplaceAdapter adapter = new MarketplaceAdapter(items, getContext(), item -> {
                            // Handle click on listing in profile
                            ListingPreviewBottomSheet preview = ListingPreviewBottomSheet.newInstance(item);
                            preview.show(getParentFragmentManager(), "listing_preview");
                        });
                        rvProfileListings.setAdapter(adapter);
                        rvProfileListings.setVisibility(View.VISIBLE);
                        tvNoListings.setVisibility(View.GONE);
                    } else {
                        tvNoListings.setVisibility(View.VISIBLE);
                        rvProfileListings.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    tvNoListings.setText("Error loading listings");
                    tvNoListings.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                progressBarListings.setVisibility(View.GONE);
                tvNoListings.setText("Error loading listings");
                tvNoListings.setVisibility(View.VISIBLE);
            }
        });
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
}
