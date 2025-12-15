package com.example.ecoswap.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.models.Post;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {
    
    private ImageView ivPostImage;
    private TextView tvPostTitle, tvPostAuthor, tvPostDate, tvPostContent;
    private RecyclerView recyclerViewComments;
    private String postId;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private List<CommentsAdapter.Comment> commentList;
    private CommentsAdapter commentsAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        
        postId = getIntent().getStringExtra("post_id");
        if (TextUtils.isEmpty(postId)) {
            Toast.makeText(this, "Missing post id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        initDataProviders();
        loadPostDetail();
        loadComments();
    }
    
    private void initViews() {
        ivPostImage = findViewById(R.id.ivPostImage);
        tvPostTitle = findViewById(R.id.tvPostTitle);
        tvPostAuthor = findViewById(R.id.tvPostAuthor);
        tvPostDate = findViewById(R.id.tvPostDate);
        tvPostContent = findViewById(R.id.tvPostContent);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        commentsAdapter = new CommentsAdapter(commentList);
        recyclerViewComments.setAdapter(commentsAdapter);
    }

    private void initDataProviders() {
        sessionManager = SessionManager.getInstance(this);
        supabaseClient = SupabaseClient.getInstance(this);
        if (supabaseClient != null) {
            supabaseClient.hydrateSession(
                sessionManager.getAccessToken(),
                sessionManager.getRefreshToken(),
                sessionManager.getAccessTokenExpiry(),
                sessionManager.getUserId()
            );
        }
    }
    
    private void loadPostDetail() {
        if (supabaseClient == null) {
            Toast.makeText(this, "Supabase not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String endpoint = String.format(Locale.US,
                "/rest/v1/posts?id=eq.%s&select=title,description,image_url,created_at,profiles(name)",
                postId);

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() == 0) {
                        Toast.makeText(PostDetailActivity.this, "Post not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    JSONObject obj = array.getJSONObject(0);
                    String title = obj.optString("title", "Post");
                    String content = obj.optString("description", "");
                    String createdAt = obj.optString("created_at", "");
                    JSONObject profile = obj.optJSONObject("profiles");
                    String author = profile != null ? profile.optString("name", "Author") : "Author";
                    String imageUrl = obj.optString("image_url", null);

                    tvPostTitle.setText(title);
                    tvPostAuthor.setText(author);
                    tvPostDate.setText(formatRelativeTime(createdAt));
                    tvPostContent.setText(!TextUtils.isEmpty(content) ? content : "No content provided.");

                    if (!TextUtils.isEmpty(imageUrl)) {
                        Glide.with(PostDetailActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(ivPostImage);
                    }
                } catch (Exception e) {
                    Toast.makeText(PostDetailActivity.this, "Failed to load post", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PostDetailActivity.this, "Could not load post: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadComments() {
        if (supabaseClient == null) {
            return;
        }

        String endpoint = String.format(Locale.US,
                "/rest/v1/comments?select=*,profiles(name)&post_id=eq.%s&order=created_at.desc",
                postId);

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    commentList.clear();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String id = obj.optString("id");
                        String content = obj.optString("content", "");
                        String createdAt = obj.optString("created_at", "");
                        JSONObject profile = obj.optJSONObject("profiles");
                        String author = profile != null ? profile.optString("name", "User") : "User";
                        commentList.add(new CommentsAdapter.Comment(id, author, content, formatRelativeTime(createdAt)));
                    }
                    commentsAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Toast.makeText(PostDetailActivity.this, "Failed to parse comments", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PostDetailActivity.this, "Could not load comments: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatRelativeTime(String iso) {
        if (TextUtils.isEmpty(iso)) {
            return getString(R.string.just_now);
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
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
}
