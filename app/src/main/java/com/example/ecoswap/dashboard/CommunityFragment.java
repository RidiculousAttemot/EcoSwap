package com.example.ecoswap.dashboard;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.ecoswap.R;
import com.example.ecoswap.BuildConfig;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CommunityFragment extends Fragment {
    
    private static final String TAG = "CommunityFragment";
    
    private RecyclerView rvCommunityPosts;
    private FloatingActionButton fabCreatePost;
    private EditText etSearchPosts;
    private ImageButton btnNotifications;
    private TextView tvNotificationBadge;
    private Chip chipAllPosts, chipSwapStories, chipDonations, chipTips, chipQuestions;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private CommunityPostAdapter postAdapter;
    private List<CommunityPost> postList;
    private String currentFilter = "All Posts";
    
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private String userId;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private TextView addPhotoLabelInDialog;
    private ImageView selectedPhotoPreview;
    private TextView uploadErrorText;
    private TextView uploadStatusText;
    private ProgressBar uploadProgressBar;
    private boolean scrollToTopAfterLoad = false;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());
        supabaseClient = SupabaseClient.getInstance(requireContext());
        userId = sessionManager.getUserId();
        initImagePicker();
        getParentFragmentManager().setFragmentResultListener("notifications_refresh", this, (requestKey, result) -> loadNotificationBadge());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotificationBadge();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);
        
        initViews(view);
        setupCategoryFilters();
        setupRecyclerView();
        loadPosts();
        setupFAB();
        loadNotificationBadge();
        
        return view;
    }
    
    private void initViews(View view) {
        rvCommunityPosts = view.findViewById(R.id.rvCommunityPosts);
        fabCreatePost = view.findViewById(R.id.fabCreatePost);
        etSearchPosts = view.findViewById(R.id.etSearchPosts);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout); // Assuming you add this to layout
        
        chipAllPosts = view.findViewById(R.id.chipAllPosts);
        chipSwapStories = view.findViewById(R.id.chipSwapStories);
        chipDonations = view.findViewById(R.id.chipDonations);
        chipTips = view.findViewById(R.id.chipTips);
        chipQuestions = view.findViewById(R.id.chipQuestions);
        
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadPosts);
        }

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> NotificationsBottomSheet.newInstance()
                    .show(getParentFragmentManager(), "notifications_sheet"));
        }
    }
    
    private void setupCategoryFilters() {
        View.OnClickListener chipClickListener = v -> {
            // Uncheck all chips
            chipAllPosts.setChecked(false);
            chipSwapStories.setChecked(false);
            chipDonations.setChecked(false);
            chipTips.setChecked(false);
            chipQuestions.setChecked(false);
            
            // Check the clicked chip
            Chip clickedChip = (Chip) v;
            clickedChip.setChecked(true);
            currentFilter = clickedChip.getText().toString();
            
            // Filter posts
            filterPosts(currentFilter);
        };
        
        chipAllPosts.setOnClickListener(chipClickListener);
        chipSwapStories.setOnClickListener(chipClickListener);
        chipDonations.setOnClickListener(chipClickListener);
        chipTips.setOnClickListener(chipClickListener);
        chipQuestions.setOnClickListener(chipClickListener);
    }
    
    private void setupRecyclerView() {
        postList = new ArrayList<>();
        postAdapter = new CommunityPostAdapter(postList, getContext());
        rvCommunityPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCommunityPosts.setAdapter(postAdapter);
    }

    private void loadNotificationBadge() {
        if (supabaseClient == null || TextUtils.isEmpty(userId) || tvNotificationBadge == null) {
            return;
        }
        String endpoint = String.format(Locale.US,
            "/rest/v1/notifications?select=id&user_id=eq.%s&is_read=eq.false",
            userId);
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    int unread = array.length();
                    if (isAdded() && tvNotificationBadge != null) {
                        requireActivity().runOnUiThread(() -> {
                            if (unread > 0) {
                                tvNotificationBadge.setText(String.valueOf(unread));
                                tvNotificationBadge.setVisibility(View.VISIBLE);
                            } else {
                                tvNotificationBadge.setVisibility(View.GONE);
                            }
                        });
                    }
                } catch (JSONException ignored) {
                }
            }

            @Override
            public void onError(String error) {
                // ignore badge errors
            }
        });
    }
    
    private void loadPosts() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        
        String query = "/rest/v1/posts?select=*,profiles(name,profile_image_url),comments(count)&category=eq.community&order=created_at.desc";
        
        supabaseClient.query(query, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    List<CommunityPost> newPosts = new ArrayList<>();
                    
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String id = obj.optString("id");
                        String title = obj.optString("title"); // Using title as sub-category
                        String content = obj.optString("description");
                        String imageUrl = obj.isNull("image_url") ? null : obj.optString("image_url", null);
                        if (imageUrl != null && imageUrl.trim().isEmpty()) {
                            imageUrl = null;
                        }
                        int likes = obj.optInt("likes", 0);
                        int commentCount = 0;
                        JSONArray comments = obj.optJSONArray("comments");
                        if (comments != null && comments.length() > 0) {
                            commentCount = comments.optJSONObject(0).optInt("count", 0);
                        }
                        String createdAt = obj.optString("created_at");
                        String ownerId = obj.optString("user_id", "");
                        
                        JSONObject profile = obj.optJSONObject("profiles");
                        String userName = "Unknown User";
                        String userAvatar = null;

                        if (profile != null) {
                            userName = profile.optString("name", "Unknown User");
                            userAvatar = profile.optString("profile_image_url");
                        }
                        
                        String timeAgo = formatRelative(createdAt);
                        
                        newPosts.add(new CommunityPost(
                            id, ownerId, userName, userAvatar, timeAgo, title, content, likes, commentCount, imageUrl, createdAt
                        ));
                    }
                    
                    requireActivity().runOnUiThread(() -> {
                        postList.clear();
                        postList.addAll(newPosts);
                        postAdapter.notifyDataSetChanged();
                        if (scrollToTopAfterLoad && rvCommunityPosts != null) {
                            rvCommunityPosts.scrollToPosition(0);
                            scrollToTopAfterLoad = false;
                        }
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing posts", e);
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading posts: " + error);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private String formatRelative(@Nullable String iso) {
        if (TextUtils.isEmpty(iso)) {
            return getString(R.string.just_now);
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(iso);
            if (date == null) return iso;
            long delta = System.currentTimeMillis() - date.getTime();
            long minutes = Math.max(1, delta / 60000);
            if (minutes < 60) return minutes + "m ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + "h ago";
            long days = hours / 24;
            return days + "d ago";
        } catch (Exception e) {
            return iso;
        }
    }
    
    private void filterPosts(String category) {
        if (category.equals("All Posts")) {
            loadPosts();
            return;
        }
        
        // Filter locally for now or query DB
        // For simplicity, let's query DB with title filter
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        
        String query = "/rest/v1/posts?select=*,profiles(name,profile_image_url),comments(count)&category=eq.community&title=eq." + category + "&order=created_at.desc";
        supabaseClient.query(query, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                // Same parsing logic... reusing loadPosts logic would be better but for now:
                try {
                    JSONArray array = new JSONArray(data.toString());
                    List<CommunityPost> newPosts = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String id = obj.optString("id");
                        String title = obj.optString("title");
                        String content = obj.optString("description");
                        String imageUrl = obj.isNull("image_url") ? null : obj.optString("image_url", null);
                        if (imageUrl != null && imageUrl.trim().isEmpty()) {
                            imageUrl = null;
                        }
                        int likes = obj.optInt("likes", 0);
                        int commentCount = 0;
                        JSONArray comments = obj.optJSONArray("comments");
                        if (comments != null && comments.length() > 0) {
                            commentCount = comments.optJSONObject(0).optInt("count", 0);
                        }
                        String createdAt = obj.optString("created_at");
                        String ownerId = obj.optString("user_id", "");
                        JSONObject profile = obj.optJSONObject("profiles");
                        String userName = profile != null ? profile.optString("name", "Unknown") : "Unknown";
                        String userAvatar = profile != null ? profile.optString("profile_image_url") : null;
                        
                        newPosts.add(new CommunityPost(
                            id, ownerId, userName, userAvatar, formatRelative(createdAt), title, content, likes, commentCount, imageUrl, createdAt
                        ));
                    }
                    requireActivity().runOnUiThread(() -> {
                        postList.clear();
                        postList.addAll(newPosts);
                        postAdapter.notifyDataSetChanged();
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    });
                } catch (JSONException e) {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                }
            }
            
            @Override
            public void onError(String error) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
    
    private void setupFAB() {
        fabCreatePost.setOnClickListener(v -> showCreatePostDialog());
    }

    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                    // Some providers do not support persistable permissions; best-effort only
                }

                if (addPhotoLabelInDialog != null) {
                    addPhotoLabelInDialog.setText("Photo selected");
                }
                if (selectedPhotoPreview != null) {
                    selectedPhotoPreview.setVisibility(View.VISIBLE);
                    Glide.with(requireContext()).load(selectedImageUri).centerCrop().into(selectedPhotoPreview);
                }
                Toast.makeText(getContext(), "Photo selected", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showCreatePostDialog() {
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_post);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        Spinner spinnerCategory = dialog.findViewById(R.id.spinnerPostCategory);
        EditText etPostContent = dialog.findViewById(R.id.etPostContent);
        LinearLayout layoutAddPhoto = dialog.findViewById(R.id.layoutAddPhoto);
        TextView tvAddPhoto = layoutAddPhoto.findViewById(R.id.tvAddPhotoLabel);
        ImageView ivSelectedPhoto = layoutAddPhoto.findViewById(R.id.ivSelectedPhoto);
        TextView tvUploadError = dialog.findViewById(R.id.tvUploadError);
        TextView tvUploadStatus = dialog.findViewById(R.id.tvUploadStatus);
        ProgressBar progressUpload = dialog.findViewById(R.id.progressUpload);
        Button btnCancel = dialog.findViewById(R.id.btnCancelPost);
        Button btnPublish = dialog.findViewById(R.id.btnPublishPost);

        selectedImageUri = null;
        addPhotoLabelInDialog = tvAddPhoto;
        selectedPhotoPreview = ivSelectedPhoto;
        uploadErrorText = tvUploadError;
        uploadStatusText = tvUploadStatus;
        uploadProgressBar = progressUpload;
        if (tvAddPhoto != null) {
            tvAddPhoto.setText("Add Photo (Optional)");
        }
        if (ivSelectedPhoto != null) {
            ivSelectedPhoto.setVisibility(View.GONE);
        }
        hideInlineError();
        setProgress(false, null);
        
        String[] categories = {"Swap Story", "Donations", "Eco Tips", "Questions"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        
        layoutAddPhoto.setOnClickListener(v -> openImagePicker());
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnPublish.setOnClickListener(v -> {
            String content = etPostContent.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            
            if (content.isEmpty()) {
                etPostContent.setError("Please write something");
                return;
            }
            
            if (TextUtils.isEmpty(userId)) {
                Toast.makeText(getContext(), "Please login to post", Toast.LENGTH_SHORT).show();
                return;
            }
            
            btnPublish.setEnabled(false);
            hideInlineError();
            setProgress(true, selectedImageUri != null ? "Uploading photo..." : "Publishing post...");

            if (selectedImageUri != null) {
                uploadImageAndPublish(dialog, content, category, tvAddPhoto, btnPublish);
            } else {
                publishPost(dialog, content, category, null, tvAddPhoto, btnPublish);
            }
        });
        
        dialog.show();
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void uploadImageAndPublish(Dialog dialog, String content, String category, @Nullable TextView tvAddPhoto, Button btnPublish) {
        if (selectedImageUri == null) {
            publishPost(dialog, content, category, null, tvAddPhoto, btnPublish);
            return;
        }
        try {
            byte[] bytes = compressImage(selectedImageUri);
            if (bytes == null || bytes.length == 0) {
                requireActivity().runOnUiThread(() -> {
                    showInlineError("Could not read photo");
                    resetPublishState(tvAddPhoto, btnPublish);
                });
                return;
            }
            String path = "community/" + userId + "/" + System.currentTimeMillis() + ".jpg";
            List<String> bucketPriority = buildBucketPriorityList();
            attemptUploadToBuckets(bucketPriority, 0, path, bytes, dialog, content, category, tvAddPhoto, btnPublish);
        } catch (Exception e) {
            Log.e(TAG, "Failed to read image", e);
            requireActivity().runOnUiThread(() -> {
                showInlineError("Could not read photo");
                resetPublishState(tvAddPhoto, btnPublish);
            });
        }
    }

    private void publishPost(Dialog dialog, String content, String category, @Nullable String imageUrl, @Nullable TextView tvAddPhoto, Button btnPublish) {
        JsonObject payload = new JsonObject();
        payload.addProperty("user_id", userId);
        payload.addProperty("title", category); // Sub-category
        payload.addProperty("description", content);
        payload.addProperty("category", "community");
        payload.addProperty("status", "available");
        if (!TextUtils.isEmpty(imageUrl)) {
            payload.addProperty("image_url", imageUrl);
        }

        supabaseClient.insert("posts", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Post published! 🎉", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    addPhotoLabelInDialog = null;
                    scrollToTopAfterLoad = true;
                    loadPosts();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Failed to publish: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });

        requireActivity().runOnUiThread(() -> {
            resetPublishState(tvAddPhoto, btnPublish);
            selectedImageUri = null;
            addPhotoLabelInDialog = null;
        });
    }

    private void resetPublishState(@Nullable TextView tvAddPhoto, Button btnPublish) {
        btnPublish.setEnabled(true);
        if (tvAddPhoto != null) {
            tvAddPhoto.setText("Add Photo (Optional)");
        }
        if (selectedPhotoPreview != null) {
            selectedPhotoPreview.setVisibility(View.GONE);
            selectedPhotoPreview.setImageDrawable(null);
        }
        setProgress(false, null);
    }

    private String resolveCommunityBucket() {
        String bucket = null;
        try {
            bucket = BuildConfig.SUPABASE_COMMUNITY_BUCKET;
        } catch (Exception ignored) {
            // field may not exist; fallback below
        }
        if (bucket == null || bucket.trim().isEmpty()) {
            bucket = "community-photos"; // new dedicated bucket for community posts
        }
        return bucket.trim();
    }

    private String resolveListingsBucket() {
        String bucket = null;
        try {
            bucket = BuildConfig.SUPABASE_LISTINGS_BUCKET;
        } catch (Exception ignored) {
            // field may not exist; fallback below
        }
        if (bucket == null || bucket.trim().isEmpty()) {
            bucket = "listing-photos";
        }
        return bucket.trim();
    }

    private String resolveDefaultStorageBucket() {
        String bucket = null;
        try {
            bucket = BuildConfig.SUPABASE_STORAGE_BUCKET;
        } catch (Exception ignored) {
            // field may not exist; fallback below
        }
        if (bucket == null || bucket.trim().isEmpty()) {
            bucket = "ecoswap-images";
        }
        return bucket.trim();
    }

    private List<String> buildBucketPriorityList() {
        List<String> buckets = new ArrayList<>();
        addUniqueBucket(buckets, resolveCommunityBucket());
        addUniqueBucket(buckets, resolveListingsBucket());
        addUniqueBucket(buckets, resolveDefaultStorageBucket());
        addUniqueBucket(buckets, "post-images"); // legacy bucket fallback if configured in Supabase
        return buckets;
    }

    private void addUniqueBucket(List<String> buckets, String bucket) {
        if (bucket == null) {
            return;
        }
        String trimmed = bucket.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (!buckets.contains(trimmed)) {
            buckets.add(trimmed);
        }
    }

    private void attemptUploadToBuckets(List<String> buckets,
                                        int index,
                                        String path,
                                        byte[] bytes,
                                        Dialog dialog,
                                        String content,
                                        String category,
                                        @Nullable TextView tvAddPhoto,
                                        Button btnPublish) {
        if (buckets == null || buckets.isEmpty() || index >= buckets.size()) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Upload failed: no buckets available", Toast.LENGTH_SHORT).show();
                resetPublishState(tvAddPhoto, btnPublish);
            });
            return;
        }

        String bucket = buckets.get(index);
        supabaseClient.uploadFile(bucket, path, bytes, new SupabaseClient.OnStorageCallback() {
            @Override
            public void onSuccess(String url) {
                setProgress(true, "Publishing post...");
                publishPost(dialog, content, category, url, tvAddPhoto, btnPublish);
            }

            @Override
            public void onError(String error) {
                int nextIndex = index + 1;
                if (nextIndex < buckets.size()) {
                    String nextBucket = buckets.get(nextIndex);
                    Log.w(TAG, "Upload failed on bucket " + bucket + ": " + error + "; retrying with " + nextBucket);
                    attemptUploadToBuckets(buckets, nextIndex, path, bytes, dialog, content, category, tvAddPhoto, btnPublish);
                } else {
                    requireActivity().runOnUiThread(() -> {
                        showInlineError("Upload failed (" + bucket + ")");
                        resetPublishState(tvAddPhoto, btnPublish);
                    });
                }
            }
        });
    }

    private byte[] compressImage(Uri uri) throws IOException {
        final int MAX_DIMENSION = 1080;
        final int JPEG_QUALITY = 82;

        InputStream boundsStream = requireContext().getContentResolver().openInputStream(uri);
        if (boundsStream == null) {
            return null;
        }
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeStream(boundsStream, null, options);
        boundsStream.close();

        int sampleSize = 1;
        while ((options.outWidth / sampleSize) > MAX_DIMENSION || (options.outHeight / sampleSize) > MAX_DIMENSION) {
            sampleSize *= 2;
        }

        android.graphics.BitmapFactory.Options decodeOptions = new android.graphics.BitmapFactory.Options();
        decodeOptions.inSampleSize = sampleSize;
        decodeOptions.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;

        InputStream decodeStream = requireContext().getContentResolver().openInputStream(uri);
        if (decodeStream == null) {
            return null;
        }
        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
        decodeStream.close();
        if (bitmap == null) {
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
        bitmap.recycle();
        return outputStream.toByteArray();
    }

    private void showInlineError(String message) {
        if (uploadErrorText != null) {
            uploadErrorText.setText(message);
            uploadErrorText.setVisibility(View.VISIBLE);
        }
    }

    private void hideInlineError() {
        if (uploadErrorText != null) {
            uploadErrorText.setText("");
            uploadErrorText.setVisibility(View.GONE);
        }
    }

    private void setProgress(boolean visible, @Nullable String status) {
        if (uploadProgressBar != null) {
            uploadProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (uploadStatusText != null) {
            if (visible && status != null) {
                uploadStatusText.setText(status);
            }
            uploadStatusText.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    // Inner class for Community Post model
    public static class CommunityPost {
        private String id;
        private String ownerId;
        private String userName;
        private String userAvatar;
        private String postTime;
        private String category;
        private String content;
        private int likeCount;
        private int commentCount;
        private String imageUrl;
        private String createdAtRaw;
        
        public CommunityPost(String id, String ownerId, String userName, String userAvatar, String postTime, 
                           String category, String content, int likeCount, int commentCount, String imageUrl, String createdAtRaw) {
            this.id = id;
            this.ownerId = ownerId;
            this.userName = userName;
            this.userAvatar = userAvatar;
            this.postTime = postTime;
            this.category = category;
            this.content = content;
            this.likeCount = likeCount;
            this.commentCount = commentCount;
            this.imageUrl = imageUrl;
            this.createdAtRaw = createdAtRaw;
        }
        
        public String getId() { return id; }
        public String getOwnerId() { return ownerId; }
        public String getUserName() { return userName; }
        public String getUserAvatar() { return userAvatar; }
        public String getPostTime() { return postTime; }
        public String getCategory() { return category; }
        public String getContent() { return content; }
        public int getLikeCount() { return likeCount; }
        public int getCommentCount() { return commentCount; }
        public String getImageUrl() { return imageUrl; }
        public String getCreatedAtRaw() { return createdAtRaw; }
        
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    }
}
