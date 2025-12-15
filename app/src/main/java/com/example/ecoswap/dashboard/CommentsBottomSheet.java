package com.example.ecoswap.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CommentsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_POST_ID = "arg_post_id";
    private static final String ARG_POST_OWNER_ID = "arg_post_owner_id";
    private String postId;
    private String postOwnerId;
    
    private RecyclerView rvComments;
    private EditText etCommentInput;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private TextView tvNoComments;
    
    private CommentsAdapter adapter;
    private List<CommentsAdapter.Comment> commentList;
    
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private String userId;

    public static CommentsBottomSheet newInstance(String postId, @Nullable String postOwnerId) {
        CommentsBottomSheet fragment = new CommentsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        if (!TextUtils.isEmpty(postOwnerId)) {
            args.putString(ARG_POST_OWNER_ID, postOwnerId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString(ARG_POST_ID);
            postOwnerId = getArguments().getString(ARG_POST_OWNER_ID);
        }
        sessionManager = SessionManager.getInstance(requireContext());
        supabaseClient = SupabaseClient.getInstance(requireContext());
        userId = sessionManager.getUserId();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rvComments = view.findViewById(R.id.rvComments);
        etCommentInput = view.findViewById(R.id.etCommentInput);
        btnSend = view.findViewById(R.id.btnSendComment);
        progressBar = view.findViewById(R.id.progressBarComments);
        tvNoComments = view.findViewById(R.id.tvNoComments);
        
        commentList = new ArrayList<>();
        adapter = new CommentsAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(adapter);
        
        btnSend.setOnClickListener(v -> postComment());
        
        loadComments();
    }

    private void loadComments() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoComments.setVisibility(View.GONE);
        
        String query = "/rest/v1/comments?select=*,profiles(name,profile_image_url)&post_id=eq." + postId + "&order=created_at.desc";
        
        supabaseClient.query(query, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    List<CommentsAdapter.Comment> newComments = new ArrayList<>();
                    
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String id = obj.optString("id");
                        String content = obj.optString("content");
                        String createdAt = obj.optString("created_at", "");
                        
                        JSONObject profile = obj.optJSONObject("profiles");
                        String authorName = profile != null ? profile.optString("name", "Unknown") : "Unknown";
                        
                        String timeAgo = formatRelativeTime(createdAt);
                        
                        newComments.add(new CommentsAdapter.Comment(id, authorName, content, timeAgo));
                    }
                    
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            commentList.clear();
                            commentList.addAll(newComments);
                            adapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE);
                            
                            if (commentList.isEmpty()) {
                                tvNoComments.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                } catch (JSONException e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to load comments", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void postComment() {
        String content = etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;
        
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(getContext(), "Please login to comment", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnSend.setEnabled(false);
        
        JsonObject payload = new JsonObject();
        payload.addProperty("post_id", postId);
        payload.addProperty("author_id", userId);
        payload.addProperty("content", content);

        // Optimistic insert
        String provisionalId = "temp_" + System.currentTimeMillis();
        CommentsAdapter.Comment provisional = new CommentsAdapter.Comment(provisionalId, "You", content, getString(R.string.just_now));
        commentList.add(0, provisional);
        adapter.notifyDataSetChanged();
        tvNoComments.setVisibility(View.GONE);

        supabaseClient.insert("comments", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                String newId = provisionalId;
                String createdAt = null;
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() > 0) {
                        JSONObject obj = array.getJSONObject(0);
                        newId = obj.optString("id", provisionalId);
                        createdAt = obj.optString("created_at", null);
                    }
                } catch (JSONException ignored) { }

                String displayTime = formatRelativeTime(createdAt);
                final String finalNewId = newId;
                final String finalDisplayTime = displayTime;
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        etCommentInput.setText("");
                        btnSend.setEnabled(true);
                        replaceProvisional(provisionalId, finalNewId, finalDisplayTime);
                        sendNotification("comment", "commented on your post", postOwnerId, postId, finalNewId);
                        Toast.makeText(getContext(), "Comment posted", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        removeProvisional(provisionalId);
                        Toast.makeText(getContext(), "Failed to post comment: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void replaceProvisional(String provisionalId, String newId, String displayTime) {
        for (int i = 0; i < commentList.size(); i++) {
            CommentsAdapter.Comment c = commentList.get(i);
            if (provisionalId.equals(c.id)) {
                commentList.set(i, new CommentsAdapter.Comment(newId, c.authorName, c.content, displayTime));
                adapter.notifyDataSetChanged();
                return;
            }
        }
    }

    private void removeProvisional(String provisionalId) {
        for (int i = 0; i < commentList.size(); i++) {
            if (provisionalId.equals(commentList.get(i).id)) {
                commentList.remove(i);
                adapter.notifyDataSetChanged();
                return;
            }
        }
    }

    private String formatRelativeTime(@Nullable String iso) {
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

    private void sendNotification(String type, String message, @Nullable String recipientId, String postId, @Nullable String commentId) {
        if (supabaseClient == null || TextUtils.isEmpty(recipientId) || TextUtils.isEmpty(userId) || userId.equals(recipientId)) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("type", type);
        payload.addProperty("message", message);
        payload.addProperty("user_id", recipientId);
        payload.addProperty("actor_id", userId);
        payload.addProperty("post_id", postId);
        if (!TextUtils.isEmpty(commentId)) {
            payload.addProperty("comment_id", commentId);
        }
        supabaseClient.insert("notifications", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) { }

            @Override
            public void onError(String error) { }
        });
    }
}