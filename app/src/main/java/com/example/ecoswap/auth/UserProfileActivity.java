package com.example.ecoswap.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ecoswap.R;

public class UserProfileActivity extends AppCompatActivity {
    
    private ImageView ivProfilePicture;
    private TextView tvUserName, tvUserEmail, tvUserBio;
    private Button btnEditProfile, btnLogout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        
        initViews();
        setupListeners();
        loadUserProfile();
    }
    
    private void initViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserBio = findViewById(R.id.tvUserBio);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
    }
    
    private void setupListeners() {
        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Edit profile functionality to be implemented", Toast.LENGTH_SHORT).show();
        });
        
        btnLogout.setOnClickListener(v -> {
            // TODO: Implement logout
            Toast.makeText(this, "Logout functionality to be implemented", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    
    private void loadUserProfile() {
        // TODO: Load user profile from Supabase
        tvUserName.setText("User Name");
        tvUserEmail.setText("user@example.com");
        tvUserBio.setText("Eco-conscious individual passionate about sustainability");
    }
}
