package com.example.ecoswap.dashboard;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.ecoswap.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileFragment extends Fragment {
    
    private FloatingActionButton btnEditProfilePicture;
    private ImageView btnEditName;
    private TextView tvUserName;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // TODO: Upload image to Supabase Storage and update profile
                        Toast.makeText(getContext(), "Profile picture selected! Upload functionality coming soon.", Toast.LENGTH_SHORT).show();
                        // For now, just show success message
                        // Later: uploadProfilePicture(selectedImageUri);
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
        
        btnEditProfilePicture.setOnClickListener(v -> {
            checkPermissionAndOpenPicker();
        });
        
        btnEditName.setOnClickListener(v -> {
            showEditNameDialog();
        });
        
        return view;
    }
    
    private void showEditNameDialog() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_name, null);
        dialog.setContentView(dialogView);
        
        // Configure dialog as full screen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Get views
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
        etEmail.setText("user@example.com"); // TODO: Get from SessionManager
        etPhone.setText(""); // TODO: Get from database
        etLocation.setText(extractLocationFromText()); // Extract from profile
        etBio.setText(""); // TODO: Get from database
        
        // Copy avatar text
        TextView tvUserAvatar = getView().findViewById(R.id.tvAvatar);
        if (tvUserAvatar != null) {
            tvAvatarPreview.setText(tvUserAvatar.getText());
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
            if (validateAndSaveProfile(
                etDisplayName.getText().toString().trim(),
                etPhone.getText().toString().trim(),
                etLocation.getText().toString().trim(),
                etBio.getText().toString().trim(),
                dialog
            )) {
                // Success - dialog will be dismissed in validateAndSaveProfile
            }
        });
        
        dialog.show();
    }
    
    private String extractLocationFromText() {
        TextView tvLocation = getView().findViewById(R.id.tvLocation);
        if (tvLocation != null) {
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
        
        // Validate location
        if (TextUtils.isEmpty(location)) {
            Toast.makeText(requireContext(), "Location cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Validate bio (optional, but check max length)
        if (!TextUtils.isEmpty(bio) && bio.length() > 150) {
            Toast.makeText(requireContext(), "Bio is too long (max 150 characters)", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Update UI
        tvUserName.setText(name);
        TextView tvLocation = getView().findViewById(R.id.tvLocation);
        if (tvLocation != null) {
            tvLocation.setText("📍 " + location);
        }
        
        // TODO: Update profile in Supabase database
        // updateProfileInDatabase(name, phone, location, bio);
        
        Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
        dialog.dismiss();
        return true;
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
    
    // TODO: Implement this method to upload to Supabase
    private void uploadProfilePicture(Uri imageUri) {
        // 1. Upload image to Supabase Storage bucket (e.g., "profile-pictures")
        // 2. Get the public URL
        // 3. Update user profile in database with the new image URL
        // 4. Update UI to show the new image
    }
}
