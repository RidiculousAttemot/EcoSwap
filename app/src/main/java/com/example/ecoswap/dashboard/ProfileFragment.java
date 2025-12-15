package com.example.ecoswap.dashboard;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.ecoswap.auth.LoginActivity;
import com.example.ecoswap.R;
import com.example.ecoswap.dashboard.listings.MyListingsFragment;
import com.example.ecoswap.utils.ProfileImageUploader;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {
    
    private static final String TAG = "ProfileFragment";
    private static final String BIO_PLACEHOLDER = "Add a short bio to introduce yourself.";
    
    private FloatingActionButton btnEditProfilePicture;
    private ImageView btnEditName;
    private TextView tvUserName;
    private TextView tvAvatar;
    private ShapeableImageView imgAvatarPhoto;
    private TextView tvBio;
    private TextView tvLocation;
    private TextView tvSwapsCount;
    private TextView tvDonatedCount;
    private TextView tvImpact;
    private TextView tvEcoLevel;
    private TextView tvEcoLevelDescription;
    private TextView tvLevelProgress;
    private TextView tvRatingValue;
    private TextView tvReviewCount;
    private MaterialCardView cardBio;
    private LinearLayout btnLogout;
    private LinearLayout btnMyListings;
    private LinearLayout btnNotifications;
    private LinearLayout btnMyImpact;
    private ProgressBar progressLevel;
    
    private SessionManager sessionManager;
    private SupabaseClient supabaseClient;
    private Gson gson;
    private final List<ReviewRow> reviewRows = new ArrayList<>();
    
    private String currentUserId;
    private String currentUserEmail;
    private String currentBio;
    private String currentPhone;
    private String currentProfileImageUrl;
    private Uri pendingAvatarUri;
    private boolean isUploadingPhoto;
    private String activeUploadToken;
    private String lastSubmittedBio;
    private boolean awaitingBioSync;
    private int impactScoreValue;
    private int totalSwapsValue;
    private int totalDonationsValue;
    private String ecoLevelValue = "Beginner EcoSaver";
    private String ecoIconValue = "🌱";
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private static class ReviewRow {
        final String name;
        final String avatarUrl;
        final int rating;
        final String createdAt;
        final String comment;

        ReviewRow(String name, String avatarUrl, int rating, String createdAt, String comment) {
            this.name = name;
            this.avatarUrl = avatarUrl;
            this.rating = rating;
            this.createdAt = createdAt;
            this.comment = comment;
        }
    }

    private static final LevelThreshold[] LEVEL_THRESHOLDS = new LevelThreshold[]{
        new LevelThreshold("Beginner EcoSaver", 0, 10, "Rising Recycler"),
        new LevelThreshold("Rising Recycler", 10, 25, "Sustainable Hero"),
        new LevelThreshold("Sustainable Hero", 25, 50, "Eco Guardian"),
        new LevelThreshold("Eco Guardian", 50, 100, "Planet Pioneer"),
        new LevelThreshold("Planet Pioneer", 100, Integer.MAX_VALUE, null)
    };
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "=========== ProfileFragment onCreate() called ===========");
        
        // Initialize managers
        sessionManager = SessionManager.getInstance(requireContext());
        supabaseClient = SupabaseClient.getInstance(requireContext());
        gson = new Gson();
        
        // Get current user ID
        currentUserId = sessionManager.getUserId();
        currentUserEmail = sessionManager.getUserEmail();

        supabaseClient.hydrateSession(
            sessionManager.getAccessToken(),
            sessionManager.getRefreshToken(),
            sessionManager.getAccessTokenExpiry(),
            currentUserId
        );
        
        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadProfilePicture(selectedImageUri);
                    }
                }
            }
        );
        
        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    Toast.makeText(getContext(), "Permission denied. Cannot access photos.", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        btnEditProfilePicture = view.findViewById(R.id.btnEditProfilePicture);
        btnEditName = view.findViewById(R.id.btnEditName);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvAvatar = view.findViewById(R.id.tvAvatar);
        imgAvatarPhoto = view.findViewById(R.id.imgAvatarPhoto);
        tvBio = view.findViewById(R.id.tvBio);
        tvLocation = view.findViewById(R.id.tvLocation);
    cardBio = view.findViewById(R.id.cardBio);
        tvSwapsCount = view.findViewById(R.id.tvSwapsCount);
        tvDonatedCount = view.findViewById(R.id.tvDonatedCount);
        tvImpact = view.findViewById(R.id.tvImpact);
        tvEcoLevel = view.findViewById(R.id.tvEcoLevel);
        tvEcoLevelDescription = view.findViewById(R.id.tvEcoLevelDescription);
        tvLevelProgress = view.findViewById(R.id.tvLevelProgress);
        tvRatingValue = view.findViewById(R.id.tvRating);
        tvReviewCount = view.findViewById(R.id.tvReviewCount);
        progressLevel = view.findViewById(R.id.progressLevel);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnMyListings = view.findViewById(R.id.btnMyListings);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnMyImpact = view.findViewById(R.id.btnMyImpact);
        
        btnEditProfilePicture.setOnClickListener(v -> {
            checkPermissionAndOpenPicker();
        });
        
        btnEditName.setOnClickListener(v -> {
            showEditProfileDialog();
        });
        
        btnLogout.setOnClickListener(v -> {
            performLogout();
        });

        if (btnMyListings != null) {
            btnMyListings.setOnClickListener(v -> openMyListings());
        }

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> openNotifications());
        }

        if (btnMyImpact != null) {
            btnMyImpact.setOnClickListener(v -> openImpactDetails());
        }

        if (tvReviewCount != null) {
            tvReviewCount.setOnClickListener(v -> openReviewsDialog());
        }
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "=========== ProfileFragment onViewCreated() called ===========");
        
        // Load user profile from database
        loadUserProfile();
    }
    
    private void loadUserProfile() {
        Log.d(TAG, "loadUserProfile() called");
        
        if (currentUserId == null) {
            Log.e(TAG, "currentUserId is null - user not logged in");
            Toast.makeText(requireContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Loading profile for user ID: " + currentUserId);
        String endpoint = "/rest/v1/profiles?id=eq." + currentUserId + "&select=*";
        Log.d(TAG, "Endpoint: " + endpoint);
        
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                Log.d(TAG, "Database query successful, data received: " + (data != null ? data.toString() : "null"));
                try {
                    JsonArray result = gson.fromJson(data.toString(), JsonArray.class);
                    Log.d(TAG, "Parsed JsonArray, size: " + (result != null ? result.size() : "null"));
                    
                    if (result != null && result.size() > 0) {
                        JsonObject profile = result.get(0).getAsJsonObject();
                        Log.d(TAG, "Profile object retrieved: " + profile.toString());
                        displayProfileData(profile);
                    } else {
                        Log.e(TAG, "Profile not found for user: " + currentUserId);
                        // Show basic info from session
                        displayFallbackData();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing profile data", e);
                    Toast.makeText(requireContext(), 
                        "Error loading profile: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    displayFallbackData();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading profile from database: " + error);
                Toast.makeText(requireContext(), 
                    "Error loading profile: " + error, 
                    Toast.LENGTH_SHORT).show();
                displayFallbackData();
            }
        });
    }
    
    private void displayProfileData(JsonObject profile) {
        Log.d(TAG, "displayProfileData() called with profile: " + profile.toString());
        
        // Ensure we're on the UI thread
        if (!isAdded() || getActivity() == null) {
            Log.e(TAG, "Fragment not attached, cannot update UI");
            return;
        }
        
        requireActivity().runOnUiThread(() -> {
            String name = profile.has("name") && !profile.get("name").isJsonNull()
                ? profile.get("name").getAsString()
                : "Anonymous User";
            Log.d(TAG, "Setting name: " + name);
            tvUserName.setText(name);

            currentUserEmail = profile.has("email") && !profile.get("email").isJsonNull()
                ? profile.get("email").getAsString()
                : currentUserEmail;

            String remoteBio = profile.has("bio") && !profile.get("bio").isJsonNull()
                ? profile.get("bio").getAsString()
                : null;
            currentBio = resolveBioValue(remoteBio);

            currentPhone = profile.has("contact_number") && !profile.get("contact_number").isJsonNull()
                ? profile.get("contact_number").getAsString()
                : "";

            currentProfileImageUrl = profile.has("profile_image_url") && !profile.get("profile_image_url").isJsonNull()
                ? profile.get("profile_image_url").getAsString()
                : null;

            String initials = getInitials(name);
            tvAvatar.setText(initials);
            if (!isUploadingPhoto) {
                applyProfileImage();
            }

            updateBioSection();

            if (profile.has("location") && !profile.get("location").isJsonNull()) {
                String location = profile.get("location").getAsString();
                if (!location.trim().isEmpty()) {
                    tvLocation.setText("📍 " + location);
                    tvLocation.setVisibility(View.VISIBLE);
                } else {
                    tvLocation.setVisibility(View.GONE);
                }
            } else {
                tvLocation.setVisibility(View.GONE);
            }

            ecoIconValue = profile.has("eco_icon") && !profile.get("eco_icon").isJsonNull()
                ? profile.get("eco_icon").getAsString()
                : "🌱";
            ecoLevelValue = profile.has("eco_level") && !profile.get("eco_level").isJsonNull()
                ? profile.get("eco_level").getAsString()
                : "Beginner EcoSaver";

            totalSwapsValue = profile.has("total_swaps") && !profile.get("total_swaps").isJsonNull()
                ? profile.get("total_swaps").getAsInt()
                : 0;
            tvSwapsCount.setText(String.valueOf(totalSwapsValue));

            totalDonationsValue = profile.has("total_donations") && !profile.get("total_donations").isJsonNull()
                ? profile.get("total_donations").getAsInt()
                : 0;
            tvDonatedCount.setText(String.valueOf(totalDonationsValue));

            impactScoreValue = profile.has("impact_score") && !profile.get("impact_score").isJsonNull()
                ? profile.get("impact_score").getAsInt()
                : 0;
            tvImpact.setText(String.valueOf(impactScoreValue));

            Double ratingValue = profile.has("rating") && !profile.get("rating").isJsonNull()
                ? profile.get("rating").getAsDouble()
                : null;
            Integer reviewCountValue = profile.has("review_count") && !profile.get("review_count").isJsonNull()
                ? profile.get("review_count").getAsInt()
                : null;

            updateRatingSection(ratingValue, reviewCountValue, totalSwapsValue, impactScoreValue);
            updateEcoLevelSection(ecoIconValue, ecoLevelValue, impactScoreValue);
        });
    }
    
    private void displayFallbackData() {
        // If database fails, show data from SessionManager
        String name = sessionManager.getUserName();
        if (name != null && !name.isEmpty()) {
            tvUserName.setText(name);
            tvAvatar.setText(getInitials(name));
        } else {
            tvUserName.setText("User");
            tvAvatar.setText("U");
        }
        
    tvLocation.setVisibility(View.GONE);
    currentBio = "";
    awaitingBioSync = false;
    lastSubmittedBio = null;
    updateBioSection();
        currentProfileImageUrl = null;
        pendingAvatarUri = null;
        isUploadingPhoto = false;
        applyProfileImage();
        tvSwapsCount.setText("0");
        tvDonatedCount.setText("0");
        tvImpact.setText("0");
        if (tvRatingValue != null) {
            tvRatingValue.setText("New EcoSaver");
        }
        if (tvReviewCount != null) {
            tvReviewCount.setText("No reviews yet");
        }
    }
    
    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }

        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else if (parts.length == 1 && parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase();
        } else {
            return parts[0].substring(0, 1).toUpperCase();
        }
    }
    
    private String formatMemberSince(String timestamp) {
        try {
            // Parse ISO 8601 timestamp from Supabase
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM yyyy", Locale.US);
            Date date = inputFormat.parse(timestamp.substring(0, 19));
            return outputFormat.format(date);
        } catch (Exception e) {
            return "Recently";
        }
    }
    
    private void performLogout() {
        // Clear session
        sessionManager.logout();
        
        // Clear any other app preferences
        SharedPreferences prefs = requireContext().getSharedPreferences("EcoSwapPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        // Show toast
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to login screen
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        // Finish current activity
        requireActivity().finish();
    }

    private void openMyListings() {
        if (!isAdded()) {
            return;
        }
        MyListingsFragment fragment = MyListingsFragment.newInstance(null, false);
        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_up,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out_down)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("my_listings")
                .commit();
    }

    private void openNotifications() {
        if (!isAdded()) {
            return;
        }
        NotificationsBottomSheet.newInstance()
                .show(getParentFragmentManager(), "notifications_sheet");
    }

    private void openImpactDetails() {
        if (!isAdded()) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append(ecoIconValue).append(" ").append(ecoLevelValue)
                .append(" • Impact score ").append(impactScoreValue).append("\n\n");
        message.append("How you earn impact:\n");
        message.append("• Swaps completed: ").append(totalSwapsValue).append(" (each swap adds +2)\n");
        message.append("• Donations made: ").append(totalDonationsValue).append(" (each donation adds +3)\n\n");
        message.append("Why it matters:\n");
        message.append("Higher impact means more trust in the community, better visibility, and progress toward eco badges. Keep swapping or donating to level up and help more items find a new life.");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("My Impact")
                .setMessage(message.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openReviewsDialog() {
        if (TextUtils.isEmpty(currentUserId) || supabaseClient == null) {
            Toast.makeText(getContext(), R.string.require_login_message, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isAdded()) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View content = inflater.inflate(R.layout.dialog_profile_reviews, null);
        RecyclerView rv = content.findViewById(R.id.rvReviews);
        TextView tvEmpty = content.findViewById(R.id.tvReviewsEmpty);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        ReviewAdapter adapter = new ReviewAdapter(reviewRows);
        rv.setAdapter(adapter);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setPositiveButton(android.R.string.ok, null);
        Dialog dialog = builder.create();
        dialog.show();

        fetchReviews(adapter, tvEmpty);
    }

    private void fetchReviews(@NonNull ReviewAdapter adapter, @NonNull TextView tvEmpty) {
        String endpoint = "/rest/v1/reviews?select=rating,comment,created_at,rater:profiles(name,profile_image_url)&ratee_id=eq." + currentUserId + "&order=created_at.desc&limit=50";
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    reviewRows.clear();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject row = array.getJSONObject(i);
                        int rating = row.optInt("rating", 0);
                        String comment = row.optString("comment", "");
                        String createdAt = row.optString("created_at", "");
                        JSONObject rater = row.optJSONObject("rater");
                        String raterName = rater != null ? rater.optString("name", "EcoSwap member") : "EcoSwap member";
                        String raterAvatar = rater != null ? rater.optString("profile_image_url", null) : null;
                        reviewRows.add(new ReviewRow(raterName, raterAvatar, rating, createdAt, comment));
                    }
                    requireActivity().runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(reviewRows.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(R.string.profile_reviews_load_error);
                    });
                }
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.profile_reviews_load_error);
                });
            }
        });
    }

    private static class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
        private final List<ReviewRow> rows;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        ReviewAdapter(List<ReviewRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_review, parent, false);
            return new ReviewViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
            ReviewRow row = rows.get(position);
            holder.tvName.setText(row.name);
            holder.ratingBar.setRating(row.rating);

            String dateLabel = row.createdAt;
            try {
                // Basic ISO parse fallback
                Date parsed = javax.xml.bind.DatatypeConverter.parseDateTime(row.createdAt).getTime();
                dateLabel = dateFormat.format(parsed);
            } catch (Exception ignored) { }
            holder.tvDate.setText(dateLabel);

            if (!TextUtils.isEmpty(row.comment)) {
                holder.tvComment.setVisibility(View.VISIBLE);
                holder.tvComment.setText(row.comment);
            } else {
                holder.tvComment.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(row.avatarUrl)) {
                holder.tvInitials.setVisibility(View.GONE);
                holder.ivAvatar.setVisibility(View.VISIBLE);
                Glide.with(holder.ivAvatar.getContext())
                        .load(row.avatarUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setVisibility(View.GONE);
                holder.tvInitials.setVisibility(View.VISIBLE);
                holder.tvInitials.setText(extractInitials(row.name));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        private static String extractInitials(String name) {
            if (TextUtils.isEmpty(name)) return "?";
            String[] parts = name.trim().split(" ");
            if (parts.length >= 2) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.getDefault());
            }
            return name.substring(0, 1).toUpperCase(Locale.getDefault());
        }

        static class ReviewViewHolder extends RecyclerView.ViewHolder {
            final TextView tvName;
            final TextView tvDate;
            final TextView tvComment;
            final TextView tvInitials;
            final ImageView ivAvatar;
            final RatingBar ratingBar;

            ReviewViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvReviewerName);
                tvDate = itemView.findViewById(R.id.tvReviewDate);
                tvComment = itemView.findViewById(R.id.tvReviewComment);
                tvInitials = itemView.findViewById(R.id.tvAvatar);
                ivAvatar = itemView.findViewById(R.id.ivReviewer);
                ratingBar = itemView.findViewById(R.id.reviewRatingBar);
            }
        }
    }
    
    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_name, null);
        dialog.setContentView(dialogView);
        
        // Configure dialog as full screen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Get views
        ShapeableImageView imgAvatarPreview = dialogView.findViewById(R.id.imgAvatarPreview);
        TextView tvAvatarPreview = dialogView.findViewById(R.id.tvAvatarPreview);
        FloatingActionButton btnChangePhoto = dialogView.findViewById(R.id.btnChangePhoto);
        TextInputEditText etDisplayName = dialogView.findViewById(R.id.etDisplayName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etPhone);
        TextInputEditText etLocation = dialogView.findViewById(R.id.etLocation);
        TextInputEditText etBio = dialogView.findViewById(R.id.etBio);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        
        // Pre-fill current data
        etDisplayName.setText(tvUserName.getText().toString());
        etEmail.setText(currentUserEmail != null ? currentUserEmail : "");
        etPhone.setText(currentPhone != null ? currentPhone : "");
        etLocation.setText(extractLocationFromText());
        etBio.setText(currentBio != null ? currentBio : "");
        
        // Email is read-only
        etEmail.setEnabled(false);
        etEmail.setAlpha(0.6f);
        
        if (isUploadingPhoto && pendingAvatarUri != null) {
            imgAvatarPreview.setVisibility(View.VISIBLE);
            Glide.with(this)
                .load(pendingAvatarUri)
                .placeholder(R.drawable.bg_circle_white)
                .error(R.drawable.bg_circle_white)
                .into(imgAvatarPreview);
            tvAvatarPreview.setVisibility(View.GONE);
        } else if (currentProfileImageUrl != null && !currentProfileImageUrl.trim().isEmpty()) {
            imgAvatarPreview.setVisibility(View.VISIBLE);
            Glide.with(this)
                .load(currentProfileImageUrl)
                .placeholder(R.drawable.bg_circle_white)
                .error(R.drawable.bg_circle_white)
                .into(imgAvatarPreview);
            tvAvatarPreview.setVisibility(View.GONE);
        } else {
            imgAvatarPreview.setVisibility(View.GONE);
            tvAvatarPreview.setVisibility(View.VISIBLE);
            if (tvAvatar != null) {
                tvAvatarPreview.setText(tvAvatar.getText());
            }
        }
        
        // Change photo button
        btnChangePhoto.setOnClickListener(v -> {
            dialog.dismiss();
            checkPermissionAndOpenPicker();
        });
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Save button
        btnSave.setOnClickListener(v -> {
            String name = etDisplayName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String bio = etBio.getText().toString().trim();
            
            if (validateAndSaveProfile(name, phone, location, bio, dialog)) {
                // Success - dialog will be dismissed in validateAndSaveProfile
            }
        });
        
        dialog.show();
    }
    
    private String extractLocationFromText() {
        if (tvLocation != null && tvLocation.getVisibility() == View.VISIBLE) {
            String fullText = tvLocation.getText().toString();
            // Remove the emoji and extract just the location
            return fullText.replace("📍 ", "").trim();
        }
        return "";
    }
    
    private boolean validateAndSaveProfile(String name, String phone, String location, String bio, Dialog dialog) {
        // Validate name
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (name.length() < 2) {
            Toast.makeText(requireContext(), "Name must be at least 2 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (name.length() > 50) {
            Toast.makeText(requireContext(), "Name is too long (max 50 characters)", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Validate phone (optional)
        if (!TextUtils.isEmpty(phone)) {
            // Remove spaces and dashes for validation
            String cleanPhone = phone.replaceAll("[\\s-]", "");
            if (cleanPhone.length() < 10) {
                Toast.makeText(requireContext(), "Phone number must be at least 10 digits", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        // Validate location (optional now)
        if (!TextUtils.isEmpty(location) && location.length() > 100) {
            Toast.makeText(requireContext(), "Location is too long (max 100 characters)", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Validate bio (optional, but check max length)
        if (!TextUtils.isEmpty(bio) && bio.length() > 150) {
            Toast.makeText(requireContext(), "Bio is too long (max 150 characters)", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Save to database
        updateProfileInDatabase(name, phone, location, bio, dialog);
        
        return true;
    }
    
    private void updateProfileInDatabase(String name, String phone, String location, String bio, Dialog dialog) {
        // Disable save button
        Button btnSave = dialog.findViewById(R.id.btnSave);
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");
        }
        
        // Prepare data for update
        JsonObject profileData = new JsonObject();
        profileData.addProperty("name", name);
        if (phone.isEmpty()) {
            profileData.add("contact_number", com.google.gson.JsonNull.INSTANCE);
        } else {
            profileData.addProperty("contact_number", phone);
        }
        if (location.isEmpty()) {
            profileData.add("location", com.google.gson.JsonNull.INSTANCE);
        } else {
            profileData.addProperty("location", location);
        }
        if (bio.isEmpty()) {
            profileData.add("bio", com.google.gson.JsonNull.INSTANCE);
        } else {
            profileData.addProperty("bio", bio);
        }
        
        // Add timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        String timestamp = sdf.format(new Date());
        profileData.addProperty("updated_at", timestamp);
        
        // Update in Supabase
        String endpoint = "/rest/v1/profiles?id=eq." + currentUserId;
        
        supabaseClient.updateRecord(endpoint, profileData, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                // Update SessionManager
                sessionManager.saveUserName(name);
                
                // Update UI
                tvUserName.setText(name);
                tvAvatar.setText(getInitials(name));
                
                if (!location.isEmpty()) {
                    tvLocation.setText("📍 " + location);
                    tvLocation.setVisibility(View.VISIBLE);
                } else {
                    tvLocation.setVisibility(View.GONE);
                }
                
                // Update cached values
                currentPhone = phone;
                currentBio = bio;
                lastSubmittedBio = bio;
                awaitingBioSync = true;
                // Update bio UI immediately
                updateBioSection();
                
                // Refresh the profile from Supabase to sync computed fields
                loadUserProfile();

                Toast.makeText(requireContext(), "Profile updated successfully! 🎉", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error updating profile: " + error);
                Toast.makeText(requireContext(), 
                    "Error updating profile: " + error, 
                    Toast.LENGTH_SHORT).show();
                
                // Re-enable save button
                if (btnSave != null) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                }
            }
        });
    }
    
    private void checkPermissionAndOpenPicker() {
        // For Android 13+ (API 33+), we need READ_MEDIA_IMAGES permission
        // For older versions, we need READ_EXTERNAL_STORAGE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
    
    private void uploadProfilePicture(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "Cannot upload photo without a user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String uploadToken = UUID.randomUUID().toString();
        activeUploadToken = uploadToken;
        pendingAvatarUri = imageUri;
        isUploadingPhoto = true;
        applyProfileImage();
        if (btnEditProfilePicture != null) {
            btnEditProfilePicture.setEnabled(false);
        }
        Toast.makeText(requireContext(), "Uploading profile photo...", Toast.LENGTH_SHORT).show();

        ProfileImageUploader.upload(requireContext(), supabaseClient, currentUserId, imageUri, new ProfileImageUploader.Callback() {
            @Override
            public void onUploadSuccess(@NonNull String publicUrl) {
                if (!uploadToken.equals(activeUploadToken)) {
                    return;
                }
                persistProfileImage(publicUrl, uploadToken);
            }

            @Override
            public void onUploadError(@NonNull String message) {
                if (!uploadToken.equals(activeUploadToken)) {
                    return;
                }
                isUploadingPhoto = false;
                activeUploadToken = null;
                pendingAvatarUri = null;
                applyProfileImage();
                if (btnEditProfilePicture != null) {
                    btnEditProfilePicture.setEnabled(true);
                }
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Photo upload failed: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void persistProfileImage(String publicUrl, String uploadToken) {
        JsonObject update = new JsonObject();
        update.addProperty("profile_image_url", publicUrl);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        update.addProperty("updated_at", sdf.format(new Date()));

        String endpoint = "/rest/v1/profiles?id=eq." + currentUserId;

        supabaseClient.updateRecord(endpoint, update, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!uploadToken.equals(activeUploadToken)) {
                    return;
                }
                if (!isAdded()) {
                    return;
                }
                currentProfileImageUrl = publicUrl;
                pendingAvatarUri = null;
                isUploadingPhoto = false;
                activeUploadToken = null;
                applyProfileImage();
                if (btnEditProfilePicture != null) {
                    btnEditProfilePicture.setEnabled(true);
                }
                Toast.makeText(requireContext(), "Profile photo updated!", Toast.LENGTH_SHORT).show();
                loadUserProfile();
            }

            @Override
            public void onError(String error) {
                if (!uploadToken.equals(activeUploadToken)) {
                    return;
                }
                if (!isAdded()) {
                    return;
                }
                pendingAvatarUri = null;
                isUploadingPhoto = false;
                activeUploadToken = null;
                applyProfileImage();
                if (btnEditProfilePicture != null) {
                    btnEditProfilePicture.setEnabled(true);
                }
                String message = error != null ? error : "Unable to update profile photo";
                Toast.makeText(requireContext(), "Failed to save photo: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateEcoLevelSection(String ecoIcon, String ecoLevel, int impactScore) {
        if (tvEcoLevel != null) {
            tvEcoLevel.setText(ecoIcon + " " + ecoLevel);
        }

        EcoProgress progress = calculateEcoProgress(impactScore, ecoLevel);

        if (tvEcoLevelDescription != null) {
            tvEcoLevelDescription.setText(progress.description);
        }

        if (tvLevelProgress != null) {
            tvLevelProgress.setText(progress.progressLabel);
        }

        if (progressLevel != null) {
            progressLevel.setMax(100);
            animateProgress(progress.percentage);
        }
    }

    private EcoProgress calculateEcoProgress(int impactScore, String currentLevel) {
        LevelThreshold matched = LEVEL_THRESHOLDS[0];

        if (currentLevel != null) {
            for (LevelThreshold threshold : LEVEL_THRESHOLDS) {
                if (currentLevel.equalsIgnoreCase(threshold.level)) {
                    matched = threshold;
                    break;
                }
            }
        }

        for (LevelThreshold threshold : LEVEL_THRESHOLDS) {
            boolean withinRange = impactScore >= threshold.minScore
                && impactScore < threshold.nextStart;
            boolean isFinalLevel = threshold.nextLevel == null
                && impactScore >= threshold.minScore;
            if (withinRange || isFinalLevel) {
                matched = threshold;
                break;
            }
        }

        EcoProgress progress = new EcoProgress();
        progress.percentage = 100;

        if (matched.nextLevel == null || matched.nextStart <= matched.minScore) {
            progress.description = String.format(Locale.getDefault(),
                "Impact score %d • %s", impactScore, matched.level);
            progress.progressLabel = "Level complete";
            return progress;
        }

        int span = matched.nextStart - matched.minScore;
        if (span <= 0) {
            progress.description = String.format(Locale.getDefault(),
                "Impact score %d • %s", impactScore, matched.level);
            progress.progressLabel = "Level complete";
            return progress;
        }

        int gained = Math.max(0, impactScore - matched.minScore);
        int percent = Math.min(100, Math.round((gained * 100f) / span));
        int remaining = Math.max(0, matched.nextStart - impactScore);

        progress.percentage = percent;
        progress.description = String.format(Locale.getDefault(),
            "Impact score %d • %d pts to %s",
            impactScore,
            remaining,
            matched.nextLevel);
        progress.progressLabel = String.format(Locale.getDefault(),
            "%d%% towards %s",
            percent,
            matched.nextLevel);

        return progress;
    }

    private static class LevelThreshold {
        final String level;
        final int minScore;
        final int nextStart;
        final String nextLevel;

        LevelThreshold(String level, int minScore, int nextStart, String nextLevel) {
            this.level = level;
            this.minScore = minScore;
            this.nextStart = nextStart;
            this.nextLevel = nextLevel;
        }
    }

    private static class EcoProgress {
        int percentage;
        String description;
        String progressLabel;
    }

    private String resolveBioValue(String remoteBio) {
        String trimmedRemote = remoteBio != null ? remoteBio.trim() : "";
        String trimmedSubmitted = lastSubmittedBio != null ? lastSubmittedBio.trim() : "";

        if (awaitingBioSync) {
            if (!trimmedRemote.isEmpty()) {
                if (!trimmedSubmitted.isEmpty()
                    && !trimmedRemote.equals(trimmedSubmitted)
                    && !trimmedRemote.equalsIgnoreCase(trimmedSubmitted)) {
                    return lastSubmittedBio != null ? lastSubmittedBio : trimmedSubmitted;
                }

                if (trimmedRemote.equals(trimmedSubmitted)
                    || trimmedRemote.equalsIgnoreCase(trimmedSubmitted)) {
                    lastSubmittedBio = null;
                }
                awaitingBioSync = false;
                return remoteBio;
            }

            if (!trimmedSubmitted.isEmpty()) {
                return lastSubmittedBio;
            }

            awaitingBioSync = false;
            lastSubmittedBio = null;
            return "";
        }

        if (!trimmedRemote.isEmpty()) {
            return remoteBio;
        }

        return "";
    }

    private void updateBioSection() {
        if (tvBio == null) {
            return;
        }

        String trimmedBio = currentBio != null ? currentBio.trim() : "";
        boolean hasBio = !trimmedBio.isEmpty();
        Typeface baseTypeface = tvBio.getTypeface() != null
            ? tvBio.getTypeface()
            : Typeface.DEFAULT;

        if (hasBio) {
            tvBio.setText(trimmedBio);
            tvBio.setAlpha(1f);
            tvBio.setTypeface(Typeface.create(baseTypeface, Typeface.NORMAL));
        } else {
            tvBio.setText(BIO_PLACEHOLDER);
            tvBio.setAlpha(0.75f);
            tvBio.setTypeface(Typeface.create(baseTypeface, Typeface.ITALIC));
        }

        tvBio.setVisibility(View.VISIBLE);
        if (cardBio != null) {
            cardBio.setVisibility(View.VISIBLE);
        }
    }

    private void updateRatingSection(Double ratingValue, Integer reviewCountValue, int totalSwaps, int impactScore) {
        if (tvRatingValue == null || tvReviewCount == null) {
            return;
        }

        boolean hasBackendRating = ratingValue != null && ratingValue > 0;
        double resolvedRating = 0d;

        if (hasBackendRating) {
            resolvedRating = Math.max(0d, Math.min(5d, ratingValue));
        } else if (impactScore > 0) {
            resolvedRating = Math.min(5d, impactScore / 20d);
        } else if (totalSwaps > 0) {
            resolvedRating = Math.min(5d, totalSwaps * 1d);
        }

        if (resolvedRating > 0d) {
            tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f", resolvedRating));
        } else {
            tvRatingValue.setText("New EcoSaver");
        }

        if (reviewCountValue != null) {
            int count = Math.max(0, reviewCountValue);
            if (count > 0) {
                tvReviewCount.setText(count + " review" + (count == 1 ? "" : "s"));
                tvReviewCount.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0);
            } else {
                tvReviewCount.setText("No reviews yet");
                tvReviewCount.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        } else if (totalSwaps > 0) {
            tvReviewCount.setText(totalSwaps + " swap" + (totalSwaps == 1 ? "" : "s"));
            tvReviewCount.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        } else {
            tvReviewCount.setText("No reviews yet");
            tvReviewCount.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    private void applyProfileImage() {
        if (imgAvatarPhoto == null || tvAvatar == null) {
            return;
        }

        if (!isAdded()) {
            return;
        }

        boolean showPhoto = false;

        if (pendingAvatarUri != null) {
            showPhoto = true;
            Glide.with(this)
                .load(pendingAvatarUri)
                .placeholder(R.drawable.bg_circle_white)
                .error(R.drawable.bg_circle_white)
                .into(imgAvatarPhoto);
        } else {
            String trimmedUrl = currentProfileImageUrl != null
                ? currentProfileImageUrl.trim()
                : "";
            if (!trimmedUrl.isEmpty()) {
                showPhoto = true;
                Glide.with(this)
                    .load(trimmedUrl)
                    .placeholder(R.drawable.bg_circle_white)
                    .error(R.drawable.bg_circle_white)
                    .into(imgAvatarPhoto);
            }
        }

        if (showPhoto) {
            imgAvatarPhoto.setVisibility(View.VISIBLE);
            tvAvatar.setVisibility(View.GONE);
        } else {
            Glide.with(this).clear(imgAvatarPhoto);
            imgAvatarPhoto.setVisibility(View.GONE);
            tvAvatar.setVisibility(View.VISIBLE);
        }

        if (btnEditProfilePicture != null) {
            btnEditProfilePicture.setEnabled(!isUploadingPhoto);
        }
    }

    private void animateProgress(int targetProgress) {
        if (progressLevel == null) {
            return;
        }

        int current = progressLevel.getProgress();
        if (targetProgress == current) {
            progressLevel.setProgress(targetProgress);
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(current, targetProgress);
        animator.setDuration(500);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            progressLevel.setProgress(value);
        });
        animator.start();
    }
}
