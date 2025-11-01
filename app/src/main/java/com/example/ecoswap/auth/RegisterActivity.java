package com.example.ecoswap.auth;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ecoswap.R;
import com.example.ecoswap.dashboard.DashboardActivity;
import com.example.ecoswap.utils.SupabaseClient;
import com.example.ecoswap.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {
    
    private static final String TAG = "RegisterActivity";
    
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialize Supabase client and session manager
        supabaseClient = SupabaseClient.getInstance(this);
        sessionManager = SessionManager.getInstance(this);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
    }
    
    private void setupListeners() {
        btnRegister.setOnClickListener(v -> performRegister());
        
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void performRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        // Validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable button to prevent double-click
        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");
        
        // Call Supabase signup
        Log.d(TAG, "Starting signup for email: " + email);
        
        supabaseClient.signUp(email, password, new SupabaseClient.OnAuthCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Signup successful! Response: " + response);
                
                // Re-enable button
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");
                
                // Check if email confirmation is required
                if (response.startsWith("CONFIRMATION_REQUIRED:")) {
                    String[] parts = response.split(":");
                    String userId = parts.length > 1 ? parts[1] : "";
                    String userEmail = parts.length > 2 ? parts[2] : email;
                    
                    // Navigate to verification screen
                    Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    intent.putExtra("name", name);
                    startActivity(intent);
                } else {
                    // Email confirmation not required, proceed to dashboard
                    String userId = response;
                    
                    // Save session
                    sessionManager.saveUserId(userId);
                    sessionManager.saveUserEmail(email);
                    sessionManager.saveUserName(name);
                    sessionManager.setLoggedIn(true);
                    
                    Toast.makeText(RegisterActivity.this, 
                        "✅ Registration successful! Welcome!", 
                        Toast.LENGTH_LONG).show();
                    
                    // Navigate to dashboard
                    Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Signup failed: " + error);
                
                // Re-enable button
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");
                
                // Show error message
                String errorMsg = "Registration failed";
                if (error.contains("already registered") || error.contains("User already registered")) {
                    errorMsg = "This email is already registered";
                } else if (error.contains("Invalid email")) {
                    errorMsg = "Invalid email format";
                } else if (error.contains("Password") || error.contains("password")) {
                    errorMsg = "Password is too weak (min 6 characters)";
                } else if (error.contains("over_email_send_rate_limit")) {
                    errorMsg = "Please wait a moment before trying again";
                }
                
                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * Show modern email confirmation dialog
     */
    private void showEmailConfirmationDialog(String email) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_email_confirmation);
        dialog.setCancelable(false);
        
        // Set email in dialog
        TextView tvConfirmEmail = dialog.findViewById(R.id.tvConfirmEmail);
        tvConfirmEmail.setText(email);
        
        // OK button - navigate to login
        Button btnOk = dialog.findViewById(R.id.btnConfirmationOk);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            
            // Navigate back to login screen
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("message", "Please check your email to confirm your account");
            startActivity(intent);
            finish();
        });
        
        dialog.show();
    }
}
