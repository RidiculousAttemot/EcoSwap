package com.example.ecoswap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.example.ecoswap.R;
import com.example.ecoswap.dashboard.DashboardActivity;
import com.example.ecoswap.utils.SupabaseClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private String userId;
    private String userName;
    private String userEmail;
    private SupabaseClient supabaseClient;
    
    // UI Components
    private ImageView btnBack;
    private TextView tvAvatar;
    private TextView tvUserName;
    private TextView tvLocation;
    private TextView tvMemberSince;
    private TextView tvSwapsCount;
    private TextView tvRating;
    private TextView tvReviewCount;
    private CardView bioCard;
    private TextView tvBio;
    private Button btnMessage;
    private Button btnViewListings;
    private TextView tvRecentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Initialize Supabase client
        supabaseClient = SupabaseClient.getInstance(this);

        // Get user ID from intent
        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        loadUserProfile();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvAvatar = findViewById(R.id.tvAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvLocation = findViewById(R.id.tvLocation);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        tvSwapsCount = findViewById(R.id.tvSwapsCount);
        tvRating = findViewById(R.id.tvRating);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        bioCard = findViewById(R.id.bioCard);
        tvBio = findViewById(R.id.tvBio);
        btnMessage = findViewById(R.id.btnMessage);
        btnViewListings = findViewById(R.id.btnViewListings);
        tvRecentActivity = findViewById(R.id.tvRecentActivity);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnMessage.setOnClickListener(v -> {
            // TODO: Navigate to chat with this user
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("TARGET_FRAGMENT", "messages");
            intent.putExtra("CHAT_USER_ID", userId);
            intent.putExtra("CHAT_USER_NAME", userName);
            startActivity(intent);
        });

        btnViewListings.setOnClickListener(v -> {
            // TODO: Navigate to marketplace filtered by this user
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("TARGET_FRAGMENT", "marketplace");
            intent.putExtra("FILTER_USER_ID", userId);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        // Query profiles table for the specific user
        String endpoint = "/rest/v1/profiles?id=eq." + userId + "&select=*";

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    JsonArray result = gson.fromJson(data.toString(), JsonArray.class);
                    
                    if (result != null && result.size() > 0) {
                        JsonObject profile = result.get(0).getAsJsonObject();
                        displayProfileData(profile);
                        loadUserStats();
                    } else {
                        Toast.makeText(UserProfileActivity.this, 
                            "Profile not found", 
                            Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Toast.makeText(UserProfileActivity.this, 
                        "Error parsing profile data: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    android.util.Log.e("UserProfileActivity", "Error parsing profile", e);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(UserProfileActivity.this, 
                    "Error loading profile: " + error, 
                    Toast.LENGTH_SHORT).show();
                android.util.Log.e("UserProfileActivity", "Error loading profile: " + error);
            }
        });
    }

    private void displayProfileData(JsonObject profile) {
        // Display name
        userName = profile.has("name") && !profile.get("name").isJsonNull() 
            ? profile.get("name").getAsString() 
            : "Anonymous User";
        tvUserName.setText(userName);

        // Display email
        userEmail = profile.has("email") && !profile.get("email").isJsonNull()
            ? profile.get("email").getAsString()
            : "";

        // Display avatar (first letters of name)
        String initials = getInitials(userName);
        tvAvatar.setText(initials);

        // Display location
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

        // Display member since
        if (profile.has("created_at") && !profile.get("created_at").isJsonNull()) {
            String createdAt = profile.get("created_at").getAsString();
            String memberSince = formatMemberSince(createdAt);
            tvMemberSince.setText("Member since " + memberSince);
        } else {
            tvMemberSince.setText("New member");
        }

        // Display bio
        if (profile.has("bio") && !profile.get("bio").isJsonNull()) {
            String bio = profile.get("bio").getAsString();
            if (!bio.trim().isEmpty()) {
                tvBio.setText(bio);
                bioCard.setVisibility(View.VISIBLE);
            } else {
                bioCard.setVisibility(View.GONE);
            }
        } else {
            bioCard.setVisibility(View.GONE);
        }

        // Display rating - using impact_score or total_swaps as a rating proxy
        // Since the schema doesn't have a rating field, we'll calculate it
        if (profile.has("impact_score") && !profile.get("impact_score").isJsonNull()) {
            int impactScore = profile.get("impact_score").getAsInt();
            // Convert impact score to 0-5 rating (divide by 20)
            double rating = Math.min(5.0, impactScore / 20.0);
            tvRating.setText(String.format(Locale.US, "%.1f", rating));
        } else if (profile.has("total_swaps") && !profile.get("total_swaps").isJsonNull()) {
            int totalSwaps = profile.get("total_swaps").getAsInt();
            // Convert swaps to rating (1 star per swap, max 5)
            double rating = Math.min(5.0, totalSwaps * 1.0);
            tvRating.setText(String.format(Locale.US, "%.1f", rating));
        } else {
            tvRating.setText("—");
        }

        // Display review count - use total swaps as proxy since schema doesn't have reviews table
        if (profile.has("total_swaps") && !profile.get("total_swaps").isJsonNull()) {
            int totalSwaps = profile.get("total_swaps").getAsInt();
            tvReviewCount.setText(totalSwaps + " swap" + (totalSwaps != 1 ? "s" : ""));
        } else {
            tvReviewCount.setText("No swaps yet");
        }
    }

    private void loadUserStats() {
        // Load swaps count from posts table (completed swaps)
        String endpoint = "/rest/v1/posts?user_id=eq." + userId + "&status=eq.swapped&select=id";

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    JsonArray result = gson.fromJson(data.toString(), JsonArray.class);
                    
                    int swapsCount = result != null ? result.size() : 0;
                    tvSwapsCount.setText(String.valueOf(swapsCount));
                } catch (Exception e) {
                    tvSwapsCount.setText("0");
                    android.util.Log.e("UserProfileActivity", "Error loading swaps count", e);
                }
            }

            @Override
            public void onError(String error) {
                tvSwapsCount.setText("0");
                android.util.Log.e("UserProfileActivity", "Error querying swaps: " + error);
            }
        });

        // Load recent activity
        loadRecentActivity();
    }

    private void loadRecentActivity() {
        // TODO: Implement loading recent listings, swaps, or community posts
        // For now, show placeholder
        tvRecentActivity.setText("No recent activity to display");
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
}
