package com.example.ecoswap.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ecoswap.R;
import com.example.ecoswap.auth.LoginActivity;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;

public class UserProfileActivity extends AppCompatActivity {
    
    private ImageView ivProfilePicture;
    private TextView tvUserName, tvUserEmail, tvUserBio;
    private Button btnEditProfile, btnLogout;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private String userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_user_profile);
        
        sessionManager = SessionManager.getInstance(this);
        supabaseClient = SupabaseClient.getInstance(this);
        userId = sessionManager.getUserId();
        if (supabaseClient != null) {
            supabaseClient.hydrateSession(
                sessionManager.getAccessToken(),
                sessionManager.getRefreshToken(),
                sessionManager.getAccessTokenExpiry(),
                userId
            );
        }

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
            Toast.makeText(this, "Edit profile from the Profile tab", Toast.LENGTH_SHORT).show();
        });
        
        btnLogout.setOnClickListener(v -> {
            performLogout();
        });
    }
    
    private void loadUserProfile() {
        if (TextUtils.isEmpty(userId) || supabaseClient == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        String endpoint = "/rest/v1/profiles?select=name,email,bio,profile_image_url&id=eq." + userId + "&limit=1";

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() == 0) {
                        Toast.makeText(UserProfileActivity.this, "Profile not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONObject obj = array.getJSONObject(0);
                    String name = obj.optString("name", "EcoSwap Member");
                    String email = obj.optString("email", "");
                    String bio = obj.optString("bio", "");
                    String avatar = obj.optString("profile_image_url", null);

                    tvUserName.setText(name);
                    tvUserEmail.setText(email);
                    tvUserBio.setText(!TextUtils.isEmpty(bio) ? bio : "Tell the community about yourself");

                    if (!TextUtils.isEmpty(avatar)) {
                        Glide.with(UserProfileActivity.this)
                                .load(avatar)
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(ivProfilePicture);
                    }
                } catch (Exception e) {
                    Toast.makeText(UserProfileActivity.this, "Failed to parse profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(UserProfileActivity.this, "Could not load profile: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogout() {
        if (supabaseClient == null) {
            finish();
            return;
        }
        btnLogout.setEnabled(false);
        supabaseClient.signOut(new SupabaseClient.OnAuthCallback() {
            @Override
            public void onSuccess(String userId) {
                clearSessionAndExit();
            }

            @Override
            public void onError(String error) {
                clearSessionAndExit();
            }
        });
    }

    private void clearSessionAndExit() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
