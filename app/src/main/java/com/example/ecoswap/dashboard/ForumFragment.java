package com.example.ecoswap.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.adapters.PostAdapter;
import com.example.ecoswap.models.Post;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.gson.JsonObject;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ForumFragment extends Fragment {
    
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private FloatingActionButton fabCreatePost;
    private List<Post> postList;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private String userId;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum, container, false);
        
        sessionManager = SessionManager.getInstance(requireContext());
        supabaseClient = SupabaseClient.getInstance(requireContext());
        userId = sessionManager.getUserId();
        if (supabaseClient != null) {
            supabaseClient.hydrateSession(
                sessionManager.getAccessToken(),
                sessionManager.getRefreshToken(),
                sessionManager.getAccessTokenExpiry(),
                userId
            );
        }

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadPosts();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts);
        fabCreatePost = view.findViewById(R.id.fabCreatePost);
        postList = new ArrayList<>();
    }
    
    private void setupRecyclerView() {
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(postList, post -> {
            Intent intent = new Intent(getActivity(), PostDetailActivity.class);
            intent.putExtra("post_id", post.getId());
            startActivity(intent);
        });
        recyclerViewPosts.setAdapter(postAdapter);
    }
    
    private void setupListeners() {
        fabCreatePost.setOnClickListener(v -> {
            showCreatePostDialog();
        });
    }
    
    private void loadPosts() {
        if (supabaseClient == null) {
            Toast.makeText(getContext(), "Supabase not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String query = "/rest/v1/posts?select=*,profiles(name)&category=eq.community&order=created_at.desc&limit=50";

        supabaseClient.query(query, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    List<Post> newPosts = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String id = obj.optString("id");
                        String title = obj.optString("title", "");
                        String content = obj.optString("description", "");
                        String authorId = obj.optString("user_id", "");
                        String imageUrl = obj.optString("image_url", null);
                        String createdAt = obj.optString("created_at", "");
                        JSONObject profile = obj.optJSONObject("profiles");
                        String author = profile != null ? profile.optString("name", "Author") : "Author";

                        Post post = new Post(id, title, content, author, authorId, imageUrl, formatRelativeTime(createdAt), 0, 0);
                        newPosts.add(post);
                    }

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            postList.clear();
                            postList.addAll(newPosts);
                            postAdapter.notifyDataSetChanged();
                        });
                    }
                } catch (Exception e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed to parse posts", Toast.LENGTH_SHORT).show());
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Could not load posts: " + error, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showCreatePostDialog() {
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(getContext(), "Please login to post", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_forum_post, null);
        EditText etTitle = dialogView.findViewById(R.id.etForumTitle);
        EditText etContent = dialogView.findViewById(R.id.etForumContent);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("New Forum Post")
                .setView(dialogView)
                .setPositiveButton("Publish", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    etTitle.setError("Title required");
                    return;
                }
                if (TextUtils.isEmpty(content)) {
                    etContent.setError("Content required");
                    return;
                }
                publishPost(dialog, title, content);
            });
        });

        dialog.show();
    }

    private void publishPost(AlertDialog dialog, String title, String content) {
        if (supabaseClient == null) {
            Toast.makeText(getContext(), "Supabase not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("user_id", userId);
        payload.addProperty("title", title);
        payload.addProperty("description", content);
        payload.addProperty("category", "community");
        payload.addProperty("status", "available");

        supabaseClient.insert("posts", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Posted", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadPosts();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed to post: " + error, Toast.LENGTH_SHORT).show());
                }
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
