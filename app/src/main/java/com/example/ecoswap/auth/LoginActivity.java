package com.example.ecoswap.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ecoswap.R;
import com.example.ecoswap.dashboard.DashboardActivity;
import com.example.ecoswap.utils.SupabaseClient;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.NetworkUtils;

public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";
    
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize Supabase client and session manager
        supabaseClient = SupabaseClient.getInstance(this);
        sessionManager = SessionManager.getInstance(this);
        
        initViews();
        setupListeners();
        
        // Check if coming from registration with confirmation message
        Intent intent = getIntent();
        if (intent.hasExtra("email")) {
            String email = intent.getStringExtra("email");
            etEmail.setText(email);
        }
        if (intent.hasExtra("message")) {
            String message = intent.getStringExtra("message");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
    
    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
    }
    
    private void setupListeners() {
        btnLogin.setOnClickListener(v -> performLogin());
        
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
    
    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Connectivity check
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "No internet connection. Please check Wi‑Fi/Data.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Disable button
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");
        
        // Call Supabase signin
        Log.d(TAG, "Starting login for email: " + email);
        
        supabaseClient.signIn(email, password, new SupabaseClient.OnAuthCallback() {
            @Override
            public void onSuccess(String userId) {
                Log.d(TAG, "Login successful! User ID: " + userId);
                
                // Save session
                sessionManager.saveUserId(userId);
                sessionManager.saveUserEmail(email);
                sessionManager.saveAccessToken(supabaseClient.getAccessToken());
                sessionManager.saveRefreshToken(supabaseClient.getRefreshToken());
                sessionManager.saveAccessTokenExpiry(supabaseClient.getAccessTokenExpiry());
                sessionManager.setLoggedIn(true);
                
                Toast.makeText(LoginActivity.this, 
                    "Login successful! Welcome back!", 
                    Toast.LENGTH_SHORT).show();
                
                // Navigate to dashboard
                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Login failed: " + error);
                
                // Re-enable button
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                
                // Show error message
                String lower = error != null ? error.toLowerCase() : "";
                String errorMsg;
                if (lower.contains("unable to resolve host") || lower.contains("failed to connect") || lower.contains("network error")) {
                    errorMsg = "Network error. Check your connection and try again.";
                } else if (lower.contains("invalid login credentials") || lower.contains("invalid")) {
                    errorMsg = "Invalid email or password.";
                } else if (lower.contains("email not confirmed")) {
                    errorMsg = "Please confirm your email first.";
                } else {
                    errorMsg = error != null ? error : "Login failed";
                }
                
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
