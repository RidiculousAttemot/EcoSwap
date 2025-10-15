package com.example.ecoswap.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ecoswap.R;
import com.example.ecoswap.auth.LoginActivity;
import com.example.ecoswap.auth.UserProfileActivity;

public class SettingsActivity extends AppCompatActivity {
    
    private Switch switchDarkMode, switchNotifications;
    private Button btnEditProfile, btnAbout, btnPrivacy, btnTerms, btnLogout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        initViews();
        setupListeners();
        loadSettings();
    }
    
    private void initViews() {
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnAbout = findViewById(R.id.btnAbout);
        btnPrivacy = findViewById(R.id.btnPrivacy);
        btnTerms = findViewById(R.id.btnTerms);
        btnLogout = findViewById(R.id.btnLogout);
    }
    
    private void setupListeners() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            saveThemePreference(isChecked);
        });
        
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference(isChecked);
            Toast.makeText(this, "Notifications " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            startActivity(intent);
        });
        
        btnAbout.setOnClickListener(v -> {
            Toast.makeText(this, "EcoSwap v1.0.0\nSustainable Item Exchange Platform", Toast.LENGTH_LONG).show();
        });
        
        btnPrivacy.setOnClickListener(v -> {
            Toast.makeText(this, "Privacy Policy to be implemented", Toast.LENGTH_SHORT).show();
        });
        
        btnTerms.setOnClickListener(v -> {
            Toast.makeText(this, "Terms & Conditions to be implemented", Toast.LENGTH_SHORT).show();
        });
        
        btnLogout.setOnClickListener(v -> {
            performLogout();
        });
    }
    
    private void loadSettings() {
        // TODO: Load settings from SharedPreferences
        switchDarkMode.setChecked(false);
        switchNotifications.setChecked(true);
    }
    
    private void saveThemePreference(boolean isDarkMode) {
        // TODO: Save to SharedPreferences
    }
    
    private void saveNotificationPreference(boolean isEnabled) {
        // TODO: Save to SharedPreferences
    }
    
    private void performLogout() {
        // TODO: Clear session and user data
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
