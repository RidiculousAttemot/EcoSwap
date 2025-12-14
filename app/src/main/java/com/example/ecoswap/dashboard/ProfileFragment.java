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
import com.bumptech.glide.Glide;
import com.example.ecoswap.auth.LoginActivity;
import com.example.ecoswap.R;
import com.example.ecoswap.dashboard.listings.MyListingsFragment;
import com.example.ecoswap.utils.ProfileImageUploader;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

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
    private ProgressBar progressLevel;
    
    private SessionManager sessionManager;
    private SupabaseClient supabaseClient;
    private Gson gson;
    
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
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

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

            String ecoIcon = profile.has("eco_icon") && !profile.get("eco_icon").isJsonNull()
                ? profile.get("eco_icon").getAsString()
                : "🌱";
            String ecoLevel = profile.has("eco_level") && !profile.get("eco_level").isJsonNull()
                ? profile.get("eco_level").getAsString()
                : "Beginner EcoSaver";

            int totalSwaps = profile.has("total_swaps") && !profile.get("total_swaps").isJsonNull()
                ? profile.get("total_swaps").getAsInt()
                : 0;
            tvSwapsCount.setText(String.valueOf(totalSwaps));

            int totalDonations = profile.has("total_donations") && !profile.get("total_donations").isJsonNull()
                ? profile.get("total_donations").getAsInt()
                : 0;
            tvDonatedCount.setText(String.valueOf(totalDonations));

            int impactScore = profile.has("impact_score") && !profile.get("impact_score").isJsonNull()
                ? profile.get("impact_score").getAsInt()
                : 0;
            tvImpact.setText(String.valueOf(impactScore));

            Double ratingValue = profile.has("rating") && !profile.get("rating").isJsonNull()
                ? profile.get("rating").getAsDouble()
                : null;
            Integer reviewCountValue = profile.has("review_count") && !profile.get("review_count").isJsonNull()
                ? profile.get("review_count").getAsInt()
                : null;

            updateRatingSection(ratingValue, reviewCountValue, totalSwaps, impactScore);
            updateEcoLevelSection(ecoIcon, ecoLevel, impactScore);
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
            } else {
                tvReviewCount.setText("No reviews yet");
            }
        } else if (totalSwaps > 0) {
            tvReviewCount.setText(totalSwaps + " swap" + (totalSwaps == 1 ? "" : "s"));
        } else {
            tvReviewCount.setText("No reviews yet");
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
