package com.example.ecoswap.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.SupabaseClient;
import com.example.ecoswap.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.gson.JsonObject;
import java.time.Instant;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.util.List;

public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.PostViewHolder> {

    private List<CommunityFragment.CommunityPost> posts;
    private Context context;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;

    public CommunityPostAdapter(List<CommunityFragment.CommunityPost> posts, Context context) {
        this.posts = posts;
        this.context = context;
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        CommunityFragment.CommunityPost post = posts.get(position);

        holder.tvUserName.setText(post.getUserName());
        holder.tvPostTime.setText(post.getPostTime());
        holder.tvPostContent.setText(post.getContent());
        holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));
        holder.chipCategory.setText(post.getCategory());

        // Load User Avatar (photo or initials fallback)
        holder.ivUserAvatar.setVisibility(View.GONE);
        holder.tvUserAvatar.setVisibility(View.VISIBLE);
        String avatarUrl = post.getUserAvatar();
        if (!TextUtils.isEmpty(avatarUrl)) {
            holder.tvUserAvatar.setVisibility(View.INVISIBLE);
            holder.ivUserAvatar.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(avatarUrl)
                    .circleCrop()
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            holder.ivUserAvatar.setVisibility(View.GONE);
                            holder.tvUserAvatar.setVisibility(View.VISIBLE);
                            holder.tvUserAvatar.setText(getInitials(post.getUserName()));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(holder.ivUserAvatar);
        }
        if (holder.ivUserAvatar.getVisibility() != View.VISIBLE) {
            holder.tvUserAvatar.setText(getInitials(post.getUserName()));
        }

        // Set chip color based on category
        int chipColorRes;
        String category = post.getCategory() != null ? post.getCategory() : "";
        switch (category) {
            case "Swap Story":
                chipColorRes = R.color.chip_electronics;
                break;
            case "Donations":
                chipColorRes = R.color.chip_clothing;
                break;
            case "Eco Tips":
                chipColorRes = R.color.chip_furniture;
                break;
            case "Questions":
                chipColorRes = R.color.chip_books;
                break;
            default:
                chipColorRes = R.color.chip_electronics;
        }
        holder.chipCategory.setChipBackgroundColorResource(chipColorRes);

        // Load Post Image only when present; hide otherwise
        holder.ivPostImage.setVisibility(View.GONE);
        String imageUrl = post.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            holder.ivPostImage.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(holder.ivPostImage);
        }

        // Like button click with server-backed update
        holder.layoutLike.setOnClickListener(v -> {
            int current = Math.max(0, post.getLikeCount());
            int newCount = current + 1;
            holder.tvLikeCount.setText(String.valueOf(newCount));
            post.setLikeCount(newCount);

            JsonObject payload = new JsonObject();
            payload.addProperty("likes", newCount);
            supabaseClient.update("posts", post.getId(), payload, new SupabaseClient.OnDatabaseCallback() {
                @Override
                public void onSuccess(Object data) {
                    sendNotification("like", "liked your post", post.getOwnerId(), post.getId(), null);
                }

                @Override
                public void onError(String error) {
                    // revert to previous count on failure
                    post.setLikeCount(current);
                    holder.tvLikeCount.setText(String.valueOf(current));
                    Toast.makeText(context, "Unable to like right now", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Comment button click
        holder.layoutComment.setOnClickListener(v -> {
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                CommentsBottomSheet bottomSheet = CommentsBottomSheet.newInstance(post.getId(), post.getOwnerId());
                bottomSheet.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "CommentsBottomSheet");
            } else {
                Toast.makeText(context, "Cannot open comments", Toast.LENGTH_SHORT).show();
            }
        });

        // More options (three dots)
        holder.btnMoreOptions.setOnClickListener(v -> showMoreOptions(v, post));
    }
    
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserAvatar, tvUserName, tvPostTime, tvPostContent;
        TextView tvLikeCount, tvCommentCount;
        Chip chipCategory;
        LinearLayout layoutLike, layoutComment;
        ImageView ivPostImage, ivUserAvatar;
        TextView btnMoreOptions;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserAvatar = itemView.findViewById(R.id.tvUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            layoutLike = itemView.findViewById(R.id.layoutLike);
            layoutComment = itemView.findViewById(R.id.layoutComment);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }

    private void showMoreOptions(View anchor, CommunityFragment.CommunityPost post) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 1, 0, "Copy text");
        popup.getMenu().add(0, 3, 1, "Report");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Community Post", post.getContent());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (id == 3) {
                Toast.makeText(context, "Reported. Thank you for the feedback.", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void sendNotification(String type, String message, String recipientId, String postId, @Nullable String commentId) {
        String actorId = sessionManager != null ? sessionManager.getUserId() : null;
        if (supabaseClient == null || TextUtils.isEmpty(actorId) || TextUtils.isEmpty(recipientId) || actorId.equals(recipientId)) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("type", type);
        payload.addProperty("message", message);
        payload.addProperty("user_id", recipientId);
        payload.addProperty("actor_id", actorId);
        payload.addProperty("is_read", false);
        payload.addProperty("created_at", Instant.now().toString());
        if (!TextUtils.isEmpty(postId)) {
            payload.addProperty("post_id", postId);
        }
        if (!TextUtils.isEmpty(commentId)) {
            payload.addProperty("comment_id", commentId);
        }
        supabaseClient.insert("notifications", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                // no-op
            }

            @Override
            public void onError(String error) {
                // silently ignore notification failures
            }
        });
    }
}
