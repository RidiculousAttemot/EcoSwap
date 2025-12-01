package com.example.ecoswap.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import com.example.ecoswap.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Pure Java Supabase client using OkHttp for REST API calls
 * Provides authentication, database, and storage operations
 */
public class SupabaseClient {
    private static SupabaseClient instance;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Handler mainHandler;
    private final SessionManager sessionManager;
    private final Context appContext;

    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 30L;
    
    private String accessToken = null;
    private String refreshToken = null;
    private long accessTokenExpiry = 0L;
    private String userId = null;
    
    private SupabaseClient(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = SessionManager.getInstance(this.appContext);
        this.supabaseUrl = BuildConfig.SUPABASE_URL;
        this.supabaseKey = BuildConfig.SUPABASE_ANON_KEY;
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Configure OkHttp client
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        hydrateSession(
            sessionManager.getAccessToken(),
            sessionManager.getRefreshToken(),
            sessionManager.getAccessTokenExpiry(),
            sessionManager.getUserId()
        );
    }
    
    public static synchronized SupabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseClient(context.getApplicationContext());
        }
        return instance;
    }
    
    public String getSupabaseUrl() {
        return supabaseUrl;
    }
    
    public String getSupabaseKey() {
        return supabaseKey;
    }
    
    public String getCurrentUserId() {
        return userId;
    }
    
    public boolean isLoggedIn() {
        return accessToken != null && userId != null;
    }

    public void hydrateSession(String accessToken, String refreshToken, long expiryEpochSeconds, String userId) {
        this.accessToken = (accessToken != null && !accessToken.trim().isEmpty()) ? accessToken : null;
        this.refreshToken = (refreshToken != null && !refreshToken.trim().isEmpty()) ? refreshToken : null;
        this.accessTokenExpiry = expiryEpochSeconds;
        this.userId = (userId != null && !userId.trim().isEmpty()) ? userId : null;
    }

    public void hydrateSession(String accessToken, String userId) {
        this.accessToken = (accessToken != null && !accessToken.trim().isEmpty()) ? accessToken : null;
        this.userId = (userId != null && !userId.trim().isEmpty()) ? userId : null;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getAccessTokenExpiry() {
        return accessTokenExpiry;
    }
    
    // ========== Authentication Methods ==========
    
    /**
     * Sign up new user with email and password
     */
    public void signUp(String email, String password, OnAuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        
        RequestBody body = RequestBody.create(
            gson.toJson(json),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
                .url(supabaseUrl + "/auth/v1/signup")
                .post(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                android.util.Log.d("SupabaseClient", "Signup response code: " + response.code());
                android.util.Log.d("SupabaseClient", "Signup response body: " + responseBody);
                
                if (response.isSuccessful()) {
                    try {
                        JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                        
                        // Check if this is a user object (Supabase returns user data at root level for signup)
                        if (result.has("id") && result.has("email")) {
                            // This IS the user object
                            String uid = result.get("id").getAsString();
                            String userEmail = result.has("email") ? result.get("email").getAsString() : email;
                            
                            // Check if email confirmation is required
                            boolean needsConfirmation = false;
                            
                            // Check if confirmation_sent_at exists (means OTP was sent)
                            if (result.has("confirmation_sent_at") && !result.get("confirmation_sent_at").isJsonNull()) {
                                needsConfirmation = true;
                                android.util.Log.d("SupabaseClient", "OTP sent, confirmation required");
                            }
                            
                            // Check user_metadata for email_verified
                            if (result.has("user_metadata") && !result.get("user_metadata").isJsonNull()) {
                                JsonObject userMetadata = result.getAsJsonObject("user_metadata");
                                if (userMetadata.has("email_verified") && !userMetadata.get("email_verified").getAsBoolean()) {
                                    needsConfirmation = true;
                                    android.util.Log.d("SupabaseClient", "Email not verified, confirmation required");
                                }
                            }
                            
                            // Pass success with email confirmation status
                            String successMessage = needsConfirmation 
                                ? "CONFIRMATION_REQUIRED:" + uid + ":" + userEmail
                                : uid;
                            
                            android.util.Log.d("SupabaseClient", "Success message: " + successMessage);
                            mainHandler.post(() -> callback.onSuccess(successMessage));
                        } else if (result.has("user") && !result.get("user").isJsonNull()) {
                            // Alternative format with user object (for older Supabase versions)
                            JsonObject user = result.getAsJsonObject("user");
                            String uid = user.get("id").getAsString();
                            String userEmail = user.has("email") ? user.get("email").getAsString() : email;
                            
                            boolean needsConfirmation = false;
                            
                            if (!result.has("access_token") || result.get("access_token").isJsonNull() 
                                || result.get("access_token").getAsString().isEmpty()) {
                                needsConfirmation = true;
                            }
                            
                            String successMessage = needsConfirmation 
                                ? "CONFIRMATION_REQUIRED:" + uid + ":" + userEmail
                                : uid;
                            
                            android.util.Log.d("SupabaseClient", "Success message: " + successMessage);
                            mainHandler.post(() -> callback.onSuccess(successMessage));
                        } else {
                            android.util.Log.e("SupabaseClient", "User object missing or null in response");
                            mainHandler.post(() -> callback.onError("Invalid response from server"));
                        }
                    } catch (Exception e) {
                        android.util.Log.e("SupabaseClient", "Parse error: " + e.getMessage(), e);
                        mainHandler.post(() -> callback.onError("Parse error: " + e.getMessage()));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Sign up failed: " + responseBody));
                }
            }
        });
    }
    
    /**
     * Verify OTP (One-Time Password) for email confirmation
     */
    public void verifyOTP(String email, String token, OnAuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("token", token);
        json.addProperty("type", "signup");
        
        RequestBody body = RequestBody.create(
            gson.toJson(json),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
                .url(supabaseUrl + "/auth/v1/verify")
                .post(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                android.util.Log.d("SupabaseClient", "Verify OTP response code: " + response.code());
                android.util.Log.d("SupabaseClient", "Verify OTP response body: " + responseBody);
                
                if (response.isSuccessful()) {
                    try {
                        JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                        
                        if (result.has("user") && !result.get("user").isJsonNull()) {
                            JsonObject user = result.getAsJsonObject("user");
                            String uid = user.get("id").getAsString();
                            
                            // Store access token
                            if (result.has("access_token") && !result.get("access_token").isJsonNull()) {
                                accessToken = result.get("access_token").getAsString();
                                accessTokenExpiry = extractExpiry(accessToken);
                                userId = uid;
                            }
                            if (result.has("refresh_token") && !result.get("refresh_token").isJsonNull()) {
                                refreshToken = result.get("refresh_token").getAsString();
                            }
                            persistSession();
                            
                            mainHandler.post(() -> callback.onSuccess(uid));
                        } else {
                            mainHandler.post(() -> callback.onError("Invalid verification response"));
                        }
                    } catch (Exception e) {
                        android.util.Log.e("SupabaseClient", "Parse error: " + e.getMessage(), e);
                        mainHandler.post(() -> callback.onError("Parse error: " + e.getMessage()));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Verification failed: " + responseBody));
                }
            }
        });
    }
    
    /**
     * Sign in existing user with email and password
     */
    public void signIn(String email, String password, OnAuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        
        RequestBody body = RequestBody.create(
            gson.toJson(json),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
                .url(supabaseUrl + "/auth/v1/token?grant_type=password")
                .post(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                
                if (response.isSuccessful()) {
                    try {
                        JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                        if (result.has("access_token")) {
                            accessToken = result.get("access_token").getAsString();
                            accessTokenExpiry = extractExpiry(accessToken);
                        }
                        if (result.has("refresh_token") && !result.get("refresh_token").isJsonNull()) {
                            refreshToken = result.get("refresh_token").getAsString();
                        }
                        if (result.has("user") && result.getAsJsonObject("user").has("id")) {
                            userId = result.getAsJsonObject("user").get("id").getAsString();
                            persistSession();
                            mainHandler.post(() -> callback.onSuccess(userId));
                        } else {
                            mainHandler.post(() -> callback.onError("Invalid response from server"));
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("Parse error: " + e.getMessage()));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Sign in failed: " + responseBody));
                }
            }
        });
    }
    
    /**
     * Sign out current user
     */
    public void signOut(OnAuthCallback callback) {
        if (accessToken == null) {
            mainHandler.post(() -> callback.onError("No user logged in"));
            return;
        }
        
        Request request = new Request.Builder()
                .url(supabaseUrl + "/auth/v1/logout")
                .post(RequestBody.create("", null))
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Clear local session anyway
                accessToken = null;
                refreshToken = null;
                accessTokenExpiry = 0L;
                userId = null;
                sessionManager.logout();
                mainHandler.post(() -> callback.onSuccess(""));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                accessToken = null;
                refreshToken = null;
                accessTokenExpiry = 0L;
                userId = null;
                sessionManager.logout();
                mainHandler.post(() -> callback.onSuccess(""));
            }
        });
    }
    
    // ========== Database Methods ==========
    
    /**
     * Insert data into a table
     */
    public void insert(String table, Object data, OnDatabaseCallback callback) {
        Runnable requestRunnable = () -> {
            String jsonData = gson.toJson(data);

            RequestBody body = RequestBody.create(
                jsonData,
                MediaType.parse("application/json")
            );

            Request.Builder requestBuilder = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/" + table)
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            if (accessToken != null) {
                requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
            }

            httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(responseBody));
                    } else {
                        mainHandler.post(() -> callback.onError("Insert failed: " + responseBody));
                    }
                }
            });
        };

        runWithSession(
            requestRunnable,
            () -> mainHandler.post(() -> callback.onError("Session expired. Please log in again."))
        );
    }
    
    /**
     * Select data from a table
     */
    public void select(String table, String query, OnDatabaseCallback callback) {
        Runnable requestRunnable = () -> {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(supabaseUrl + "/rest/v1/" + table).newBuilder();
            urlBuilder.addQueryParameter("select", "*");

            if (query != null && !query.isEmpty()) {
                urlBuilder.addEncodedQueryParameter("", query);
            }

            Request.Builder requestBuilder = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Content-Type", "application/json");

            if (accessToken != null) {
                requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
            }

            httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(responseBody));
                    } else {
                        mainHandler.post(() -> callback.onError("Select failed: " + responseBody));
                    }
                }
            });
        };

        runWithSession(
            requestRunnable,
            () -> mainHandler.post(() -> callback.onError("Session expired. Please log in again."))
        );
    }
    
    /**
     * Update data in a table
     */
    public void update(String table, String id, Object data, OnDatabaseCallback callback) {
        Runnable requestRunnable = () -> {
            String jsonData = gson.toJson(data);

            RequestBody body = RequestBody.create(
                jsonData,
                MediaType.parse("application/json")
            );

            Request.Builder requestBuilder = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/" + table + "?id=eq." + id)
                    .patch(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            if (accessToken != null) {
                requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
            }

            httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(responseBody));
                    } else {
                        mainHandler.post(() -> callback.onError("Update failed: " + responseBody));
                    }
                }
            });
        };

        runWithSession(
            requestRunnable,
            () -> mainHandler.post(() -> callback.onError("Session expired. Please log in again."))
        );
    }
    
    /**
     * Delete data from a table
     */
    public void delete(String table, String id, OnDatabaseCallback callback) {
        Runnable requestRunnable = () -> {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/" + table + "?id=eq." + id)
                    .delete()
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Content-Type", "application/json");

            if (accessToken != null) {
                requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
            }

            httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess("Deleted successfully"));
                    } else {
                        String responseBody = response.body().string();
                        mainHandler.post(() -> callback.onError("Delete failed: " + responseBody));
                    }
                }
            });
        };

        runWithSession(
            requestRunnable,
            () -> mainHandler.post(() -> callback.onError("Session expired. Please log in again."))
        );
    }
    
    // ========== Storage Methods ==========
    
    /**
     * Upload file to storage bucket
     */
    public void uploadFile(String bucket, String path, byte[] data, OnStorageCallback callback) {
        Runnable requestRunnable = () -> {
            RequestBody body = RequestBody.create(data, MediaType.parse("application/octet-stream"));
            android.util.Log.d("SupabaseClient", "uploadFile bucket=" + bucket
                + " path=" + path
                + " bytes=" + (data != null ? data.length : 0)
                + " hasToken=" + (accessToken != null));

            Request.Builder requestBuilder = new Request.Builder()
                .url(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path)
                .post(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/octet-stream");

            String authToken = (accessToken != null && !accessToken.isEmpty()) ? accessToken : supabaseKey;
            requestBuilder.addHeader("Authorization", "Bearer " + authToken);

            httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    android.util.Log.e("SupabaseClient", "uploadFile network failure", e);
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    android.util.Log.d("SupabaseClient", "uploadFile response code=" + response.code()
                        + " body=" + responseBody);
                    if (response.isSuccessful()) {
                        String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
                        mainHandler.post(() -> callback.onSuccess(publicUrl));
                    } else {
                        mainHandler.post(() -> callback.onError("Upload failed: " + responseBody));
                    }
                }
            });
        };

        runWithSession(
            requestRunnable,
            () -> mainHandler.post(() -> callback.onError("Session expired. Please log in again."))
        );
    }
    
    /**
     * Download file from storage bucket
     */
    public void downloadFile(String bucket, String path, OnStorageCallback callback) {
        Runnable requestRunnable = () -> {
            Request.Builder requestBuilder = new Request.Builder()
                .url(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path)
                .get()
                .addHeader("apikey", supabaseKey);

            String authToken = (accessToken != null && !accessToken.isEmpty()) ? accessToken : supabaseKey;
            requestBuilder.addHeader("Authorization", "Bearer " + authToken);

            httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String url = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
                        mainHandler.post(() -> callback.onSuccess(url));
                    } else {
                        String responseBody = response.body().string();
                        mainHandler.post(() -> callback.onError("Download failed: " + responseBody));
                    }
                }
            });
        };

        runWithSession(
            requestRunnable,
            () -> mainHandler.post(() -> callback.onError("Session expired. Please log in again."))
        );
    }
    
    /**
     * Get public URL for a storage file
     */
    public String getPublicUrl(String bucket, String path) {
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
    
    // ========== Generic Database Query Methods ==========
    
    /**
     * Generic GET request to Supabase REST API
     */
    public void query(String endpoint, OnDatabaseCallback callback) {
        Runnable requestRunnable = () -> {
            Request.Builder requestBuilder = new Request.Builder()
                .url(supabaseUrl + endpoint)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json");

            String authToken = (accessToken != null && !accessToken.isEmpty()) ? accessToken : supabaseKey;
            requestBuilder.addHeader("Authorization", "Bearer " + authToken);

            Request request = requestBuilder.build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(responseBody));
                    } else {
                        mainHandler.post(() -> callback.onError("Query failed: " + responseBody));
                    }
                }
            });
        };

        runWithSession(
            requestRunnable,
            () -> mainHandler.post(() -> callback.onError("Session expired. Please log in again."))
        );
    }
    
    /**
     * Generic PATCH request to Supabase REST API
     */
    public void updateRecord(String endpoint, JsonObject data, OnDatabaseCallback callback) {
        Runnable requestRunnable = () -> {
            RequestBody body = RequestBody.create(
                gson.toJson(data),
                MediaType.parse("application/json")
            );

            Request.Builder requestBuilder = new Request.Builder()
                .url(supabaseUrl + endpoint)
                .patch(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal");

            String authToken = (accessToken != null && !accessToken.isEmpty()) ? accessToken : supabaseKey;
            requestBuilder.addHeader("Authorization", "Bearer " + authToken);

            Request request = requestBuilder.build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(responseBody));
                    } else {
                        mainHandler.post(() -> callback.onError("Update failed: " + responseBody));
                    }
                }
            });
        };

        runWithSession(
            requestRunnable,
            () -> mainHandler.post(() -> callback.onError("Session expired. Please log in again."))
        );
    }

    private boolean isAccessTokenExpired() {
        if (accessToken == null) {
            return false;
        }
        if (accessTokenExpiry == 0L) {
            return false;
        }
        long currentSeconds = System.currentTimeMillis() / 1000L;
        return currentSeconds >= (accessTokenExpiry - TOKEN_EXPIRY_BUFFER_SECONDS);
    }

    private long extractExpiry(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return 0L;
            }
            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            JsonObject payloadJson = gson.fromJson(payload, JsonObject.class);
            if (payloadJson != null && payloadJson.has("exp")) {
                return payloadJson.get("exp").getAsLong();
            }
        } catch (Exception e) {
            android.util.Log.w("SupabaseClient", "Unable to parse JWT expiry", e);
        }
        return 0L;
    }

    private void runWithSession(Runnable onReady, Runnable onAuthFailure) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            onReady.run();
            return;
        }

        if (accessToken == null) {
            refreshAccessToken(onReady, onAuthFailure);
            return;
        }

        if (!isAccessTokenExpired()) {
            onReady.run();
            return;
        }

        refreshAccessToken(onReady, onAuthFailure);
    }

    private void refreshAccessToken(Runnable onSuccess, Runnable onFailure) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("refresh_token", refreshToken);

        RequestBody body = RequestBody.create(
            gson.toJson(payload),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url(supabaseUrl + "/auth/v1/token?grant_type=refresh_token")
            .post(body)
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (onFailure != null) {
                    mainHandler.post(onFailure);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                        if (result != null) {
                            if (result.has("access_token")) {
                                accessToken = result.get("access_token").getAsString();
                                accessTokenExpiry = extractExpiry(accessToken);
                            }
                            if (result.has("refresh_token") && !result.get("refresh_token").isJsonNull()) {
                                refreshToken = result.get("refresh_token").getAsString();
                            }
                            if (result.has("user") && !result.get("user").isJsonNull()) {
                                JsonObject user = result.getAsJsonObject("user");
                                if (user.has("id") && !user.get("id").isJsonNull()) {
                                    userId = user.get("id").getAsString();
                                }
                            }
                            persistSession();
                        }
                        if (onSuccess != null) {
                            mainHandler.post(onSuccess);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("SupabaseClient", "Refresh parse error", e);
                        if (onFailure != null) {
                            mainHandler.post(onFailure);
                        }
                    }
                } else {
                    android.util.Log.e("SupabaseClient", "Refresh failed: " + responseBody);
                    if (onFailure != null) {
                        mainHandler.post(onFailure);
                    }
                }
            }
        });
    }

    private void persistSession() {
        sessionManager.saveAccessToken(accessToken);
        sessionManager.saveAccessTokenExpiry(accessTokenExpiry);
        sessionManager.saveRefreshToken(refreshToken);
        if (userId != null) {
            sessionManager.saveUserId(userId);
        }
        sessionManager.setLoggedIn(accessToken != null && userId != null);
    }
    
    // ========== Callback Interfaces ==========
    
    public interface OnAuthCallback {
        void onSuccess(String userId);
        void onError(String error);
    }
    
    public interface OnDatabaseCallback {
        void onSuccess(Object data);
        void onError(String error);
    }
    
    public interface OnStorageCallback {
        void onSuccess(String url);
        void onError(String error);
    }
}
