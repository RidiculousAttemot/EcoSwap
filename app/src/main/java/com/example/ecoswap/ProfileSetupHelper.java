package com.example.ecoswap;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.ecoswap.utils.ProfileImageUploader;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.example.ecoswap.utils.SupabaseClient;
import com.example.ecoswap.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class ProfileSetupHelper {

    private Context context;
    private Dialog dialog;
    private String userId;
    private String userEmail;
    private Uri selectedPhotoUri;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    // UI Components
    private ShapeableImageView imgAvatarPreview;
    private TextView tvAvatar;
    private FloatingActionButton fabCamera;
    private TextInputEditText etDisplayName;
    private TextInputEditText etLocation;
    private TextInputEditText etBio;

    private Button btnCompleteSetup;
    private Button btnSkip;

    private boolean isUploadingPhoto;
    private String uploadedPhotoUrl;
    private String activeUploadToken;

    public ProfileSetupHelper(Context context) {
        this.context = context;
        this.sessionManager = SessionManager.getInstance(context);
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.supabaseClient.hydrateSession(
            sessionManager.getAccessToken(),
            sessionManager.getRefreshToken(),
            sessionManager.getAccessTokenExpiry(),
            sessionManager.getUserId()
        );
    }

    public void showProfileSetupDialog(String userId, String userEmail, 
                                       ActivityResultLauncher<Intent> photoPickerLauncher) {
        this.userId = userId;
        this.userEmail = userEmail;

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_profile_setup);
        dialog.setCancelable(false);

        // Initialize views
        initializeViews();

        // Setup listeners
        setupListeners(photoPickerLauncher);

        // Set default avatar (first letter of email)
        if (userEmail != null && !userEmail.isEmpty()) {
            tvAvatar.setText(userEmail.substring(0, 1).toUpperCase());
        }

        dialog.show();
    }

    private void initializeViews() {
        imgAvatarPreview = dialog.findViewById(R.id.imgAvatarPreview);
        tvAvatar = dialog.findViewById(R.id.tvAvatarPreview);
        fabCamera = dialog.findViewById(R.id.btnAddPhoto);
        etDisplayName = dialog.findViewById(R.id.etDisplayName);
        etLocation = dialog.findViewById(R.id.etLocation);
        etBio = dialog.findViewById(R.id.etBio);
        // Note: Bio counter is inline in TextInputLayout, can add later if needed
        btnCompleteSetup = dialog.findViewById(R.id.btnComplete);
        btnSkip = dialog.findViewById(R.id.btnSkip);
    }

    private void setupListeners(ActivityResultLauncher<Intent> photoPickerLauncher) {
        // Photo selection
        fabCamera.setOnClickListener(v -> {
            if (photoPickerLauncher != null) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                photoPickerLauncher.launch(intent);
            } else {
                Toast.makeText(context, "Photo picker not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Bio character counter - handled by TextInputLayout counter

        // Complete setup button
        btnCompleteSetup.setOnClickListener(v -> {
            String displayName = etDisplayName.getText() != null 
                ? etDisplayName.getText().toString().trim() 
                : "";
            String location = etLocation.getText() != null 
                ? etLocation.getText().toString().trim() 
                : "";
            String bio = etBio.getText() != null 
                ? etBio.getText().toString().trim() 
                : "";

            // Validate display name
            if (displayName.isEmpty()) {
                etDisplayName.setError("Display name is required");
                etDisplayName.requestFocus();
                return;
            }

            if (displayName.length() < 2 || displayName.length() > 50) {
                etDisplayName.setError("Display name must be 2-50 characters");
                etDisplayName.requestFocus();
                return;
            }

            // Validate bio length
            if (bio.length() > 150) {
                etBio.setError("Bio must be 150 characters or less");
                etBio.requestFocus();
                return;
            }

            // Save profile
            saveProfile(displayName, location, bio);
        });

        // Skip button
        btnSkip.setOnClickListener(v -> {
            // Just close dialog and proceed to dashboard
            dialog.dismiss();
            navigateToDashboard();
        });
    }

    public void handlePhotoSelected(Uri uri) {
        selectedPhotoUri = uri;
        if (dialog == null || !dialog.isShowing()) {
            return;
        }

        if (uri != null) {
            showAvatarPreview(uri);
        }

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(context, "Unable to upload photo: missing user id", Toast.LENGTH_SHORT).show();
            return;
        }

        startPhotoUpload(uri);
    }

    private void showAvatarPreview(Uri uri) {
        if (imgAvatarPreview != null) {
            imgAvatarPreview.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.bg_circle_white)
                .error(R.drawable.bg_circle_white)
                .into(imgAvatarPreview);
        }

        if (tvAvatar != null) {
            tvAvatar.setVisibility(View.GONE);
        }
    }

    private void restoreAvatarFallback() {
        if (imgAvatarPreview != null) {
            imgAvatarPreview.setVisibility(View.GONE);
        }

        if (tvAvatar != null) {
            tvAvatar.setVisibility(View.VISIBLE);
            if (etDisplayName.getText() != null && !etDisplayName.getText().toString().trim().isEmpty()) {
                tvAvatar.setText(getInitials(etDisplayName.getText().toString().trim()));
            } else if (userEmail != null && !userEmail.isEmpty()) {
                tvAvatar.setText(userEmail.substring(0, 1).toUpperCase());
            } else {
                tvAvatar.setText("📸");
            }
        }
    }

    private void startPhotoUpload(Uri uri) {
        if (uri == null) {
            restoreAvatarFallback();
            return;
        }

        String uploadToken = UUID.randomUUID().toString();
        activeUploadToken = uploadToken;
        isUploadingPhoto = true;
        if (btnCompleteSetup != null) {
            btnCompleteSetup.setEnabled(false);
            btnCompleteSetup.setText("Uploading photo...");
        }

        ProfileImageUploader.upload(context, supabaseClient, userId, uri, new ProfileImageUploader.Callback() {
            @Override
            public void onUploadSuccess(@NonNull String publicUrl) {
                if (!uploadToken.equals(activeUploadToken)) {
                    return;
                }
                uploadedPhotoUrl = publicUrl;
                isUploadingPhoto = false;
                activeUploadToken = null;
                if (btnCompleteSetup != null) {
                    btnCompleteSetup.setEnabled(true);
                    btnCompleteSetup.setText("Complete Setup");
                }
                Toast.makeText(context, "Photo ready!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUploadError(@NonNull String message) {
                if (!uploadToken.equals(activeUploadToken)) {
                    return;
                }
                uploadedPhotoUrl = null;
                isUploadingPhoto = false;
                activeUploadToken = null;
                restoreAvatarFallback();
                if (btnCompleteSetup != null) {
                    btnCompleteSetup.setEnabled(true);
                    btnCompleteSetup.setText("Complete Setup");
                }
                Toast.makeText(context, "Photo upload failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveProfile(String displayName, String location, String bio) {
        btnCompleteSetup.setEnabled(false);
        btnCompleteSetup.setText("Saving...");

        // Create profile update JSON
        JsonObject profileData = new JsonObject();
        profileData.addProperty("name", displayName);
        if (!location.isEmpty()) {
            profileData.addProperty("location", location);
        } else {
            profileData.add("location", com.google.gson.JsonNull.INSTANCE);
        }
        if (!bio.isEmpty()) {
            profileData.addProperty("bio", bio);
        } else {
            profileData.add("bio", com.google.gson.JsonNull.INSTANCE);
        }
        if (uploadedPhotoUrl != null && !uploadedPhotoUrl.trim().isEmpty()) {
            profileData.addProperty("profile_image_url", uploadedPhotoUrl);
        }
        profileData.addProperty("updated_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(new java.util.Date()));

        // Update profile in Supabase using PATCH request with filter
        String endpoint = "/rest/v1/profiles?id=eq." + userId;

        supabaseClient.updateRecord(endpoint, profileData, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                // Save display name to SessionManager for quick access
                try {
                    android.content.SharedPreferences prefs = context.getSharedPreferences("EcoSwapPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putString("user_name", displayName).apply();
                } catch (Exception e) {
                    // Ignore session save errors
                }

                Toast.makeText(context, 
                    "Profile setup complete! 🎉", 
                    Toast.LENGTH_SHORT).show();
                
                dialog.dismiss();
                navigateToDashboard();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, 
                    "Error saving profile: " + error, 
                    Toast.LENGTH_LONG).show();
                btnCompleteSetup.setEnabled(true);
                btnCompleteSetup.setText("Complete Setup");
            }
        });
    }

    private void navigateToDashboard() {
        try {
            Class<?> dashboardClass = Class.forName("com.example.ecoswap.dashboard.DashboardActivity");
            Intent intent = new Intent(context, dashboardClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(context, "Error navigating to dashboard", Toast.LENGTH_SHORT).show();
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

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
