package com.example.ecoswap.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
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

public class VerifyEmailActivity extends AppCompatActivity {
    
    private static final String TAG = "VerifyEmailActivity";
    
    private EditText etCode1, etCode2, etCode3, etCode4, etCode5, etCode6;
    private Button btnVerify;
    private TextView tvVerifyEmail, tvResendCode, tvResendTimer, tvBack;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    private String email;
    private String password;
    private String name;
    private CountDownTimer resendTimer;
    private boolean canResend = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);
        
        // Initialize Supabase client and session manager
        supabaseClient = SupabaseClient.getInstance(this);
        sessionManager = SessionManager.getInstance(this);
        
        // Get email from intent
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");
        name = getIntent().getStringExtra("name");
        
        if (email == null) {
            Toast.makeText(this, "Error: Email not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupCodeInputs();
        setupListeners();
        startResendTimer();
    }
    
    private void initViews() {
        etCode1 = findViewById(R.id.etCode1);
        etCode2 = findViewById(R.id.etCode2);
        etCode3 = findViewById(R.id.etCode3);
        etCode4 = findViewById(R.id.etCode4);
        etCode5 = findViewById(R.id.etCode5);
        etCode6 = findViewById(R.id.etCode6);
        btnVerify = findViewById(R.id.btnVerify);
        tvVerifyEmail = findViewById(R.id.tvVerifyEmail);
        tvResendCode = findViewById(R.id.tvResendCode);
        tvResendTimer = findViewById(R.id.tvResendTimer);
        tvBack = findViewById(R.id.tvBack);
        
        tvVerifyEmail.setText(email);
    }
    
    private void setupCodeInputs() {
        EditText[] codeFields = {etCode1, etCode2, etCode3, etCode4, etCode5, etCode6};
        
        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            final EditText currentField = codeFields[i];
            final EditText nextField = (i < codeFields.length - 1) ? codeFields[i + 1] : null;
            final EditText prevField = (i > 0) ? codeFields[i - 1] : null;
            
            // Auto-focus next field
            currentField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && nextField != null) {
                        nextField.requestFocus();
                    }
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
            
            // Handle backspace
            currentField.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (currentField.getText().toString().isEmpty() && prevField != null) {
                        prevField.requestFocus();
                        prevField.setText("");
                    }
                }
                return false;
            });
        }
        
        // Auto-focus first field
        etCode1.requestFocus();
    }
    
    private void setupListeners() {
        btnVerify.setOnClickListener(v -> verifyCode());
        
        tvResendCode.setOnClickListener(v -> {
            if (canResend) {
                resendCode();
            }
        });
        
        tvBack.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void startResendTimer() {
        canResend = false;
        tvResendCode.setEnabled(false);
        tvResendCode.setAlpha(0.5f);
        tvResendTimer.setVisibility(View.VISIBLE);
        
        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendTimer.setText("Resend code in " + (millisUntilFinished / 1000) + "s");
            }
            
            @Override
            public void onFinish() {
                canResend = true;
                tvResendCode.setEnabled(true);
                tvResendCode.setAlpha(1.0f);
                tvResendTimer.setVisibility(View.GONE);
            }
        }.start();
    }
    
    private void verifyCode() {
        String code = etCode1.getText().toString() +
                      etCode2.getText().toString() +
                      etCode3.getText().toString() +
                      etCode4.getText().toString() +
                      etCode5.getText().toString() +
                      etCode6.getText().toString();
        
        if (code.length() != 6) {
            Toast.makeText(this, "Please enter the 6-digit code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable button
        btnVerify.setEnabled(false);
        btnVerify.setText("Verifying...");
        
        Log.d(TAG, "Verifying code: " + code + " for email: " + email);
        
        // Verify OTP with Supabase
        supabaseClient.verifyOTP(email, code, new SupabaseClient.OnAuthCallback() {
            @Override
            public void onSuccess(String userId) {
                Log.d(TAG, "Verification successful! User ID: " + userId);
                
                // Save session
                sessionManager.saveUserId(userId);
                sessionManager.saveUserEmail(email);
                sessionManager.saveUserName(name);
                sessionManager.setLoggedIn(true);
                
                Toast.makeText(VerifyEmailActivity.this, 
                    "✅ Email verified successfully!", 
                    Toast.LENGTH_LONG).show();
                
                // Navigate to dashboard
                Intent intent = new Intent(VerifyEmailActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Verification failed: " + error);
                
                // Re-enable button
                btnVerify.setEnabled(true);
                btnVerify.setText("Verify Code");
                
                // Show error message
                String errorMsg = "Verification failed";
                if (error.contains("invalid") || error.contains("expired")) {
                    errorMsg = "Invalid or expired code";
                } else if (error.contains("attempts")) {
                    errorMsg = "Too many attempts. Please request a new code";
                }
                
                Toast.makeText(VerifyEmailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                
                // Clear code fields
                etCode1.setText("");
                etCode2.setText("");
                etCode3.setText("");
                etCode4.setText("");
                etCode5.setText("");
                etCode6.setText("");
                etCode1.requestFocus();
            }
        });
    }
    
    private void resendCode() {
        Toast.makeText(this, "Sending new code...", Toast.LENGTH_SHORT).show();
        
        // Resend signup request (this will send a new OTP)
        supabaseClient.signUp(email, password, new SupabaseClient.OnAuthCallback() {
            @Override
            public void onSuccess(String response) {
                Toast.makeText(VerifyEmailActivity.this, 
                    "✅ New code sent to " + email, 
                    Toast.LENGTH_LONG).show();
                startResendTimer();
            }
            
            @Override
            public void onError(String error) {
                if (error.contains("over_email_send_rate_limit")) {
                    Toast.makeText(VerifyEmailActivity.this, 
                        "Please wait before requesting another code", 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(VerifyEmailActivity.this, 
                        "Failed to send code. Please try again", 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
