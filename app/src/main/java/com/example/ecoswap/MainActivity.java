package com.example.ecoswap;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ecoswap.auth.LoginActivity;
import com.example.ecoswap.dashboard.DashboardActivity;
import com.example.ecoswap.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is logged in
        SessionManager sessionManager = SessionManager.getInstance(this);
        
        Intent intent;
        if (sessionManager.isLoggedIn()) {
            // User is logged in, go to Dashboard
            intent = new Intent(this, DashboardActivity.class);
        } else {
            // User not logged in, go to Login
            intent = new Intent(this, LoginActivity.class);
        }
        
        startActivity(intent);
        finish(); // Close MainActivity so user can't go back to it
    }
}