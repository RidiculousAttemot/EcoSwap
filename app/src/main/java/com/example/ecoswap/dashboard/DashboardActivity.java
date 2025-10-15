package com.example.ecoswap.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ecoswap.R;
import com.example.ecoswap.market.SwapActivity;
import com.example.ecoswap.market.DonationActivity;
import com.example.ecoswap.market.BiddingActivity;
import com.example.ecoswap.tracker.EcoTrackerActivity;
import com.example.ecoswap.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {
    
    private FrameLayout fragmentContainer;
    private BottomNavigationView bottomNavigationView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        initViews();
        setupBottomNavigation();
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new ForumFragment());
        }
    }
    
    private void initViews() {
        fragmentContainer = findViewById(R.id.fragmentContainer);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_forum) {
                loadFragment(new ForumFragment());
                return true;
            } else if (itemId == R.id.nav_community) {
                loadFragment(new CommunityFragment());
                return true;
            } else if (itemId == R.id.nav_swap) {
                startActivity(new Intent(this, SwapActivity.class));
                return true;
            } else if (itemId == R.id.nav_tracker) {
                startActivity(new Intent(this, EcoTrackerActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            
            return false;
        });
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
