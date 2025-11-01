package com.example.ecoswap.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "EcoSwapSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    
    private static SessionManager instance;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;
    
    private SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }
    
    public void createLoginSession(String userId, String email, String name, String accessToken) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.apply();
    }
    
    // Individual setters
    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }
    
    public void saveUserId(String userId) {
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }
    
    public void saveUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }
    
    public void saveUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }
    
    public void saveAccessToken(String token) {
        editor.putString(KEY_ACCESS_TOKEN, token);
        editor.apply();
    }
    
    // Getters
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }
    
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }
    
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
